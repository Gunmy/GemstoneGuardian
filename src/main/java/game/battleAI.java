package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//How a computer-controlled fighter picks what to do on its turn. The fight is copied into a battleSim, and every
//action it can take is tried there (each move of the equipped rock, switching to each other rock with moves left,
//each potion or bandage it carries): looking ahead its action, the player's answer and so on (aiStrength.depth
//actions in all), with every way each can hit or miss, and judging where that leads as its chance to win, by the
//calculated estimate or the trained net (aiStrength, battleSim.values). That chance is each action's score. Things a
//fixed scoring would have to guess come out of it: a heal is worth most when it is low and the player could finish
//it; switching rock costs the hit the player gets in meanwhile; a potion is kept while it is still worth more in the
//bag; behind, it takes a gamble that could turn the fight, ahead it plays safe.
//Then one action is picked at random by score, the way a language model picks its next word: the scores become
//chances (softmax at TEMPERATURE), the unlikeliest actions that together make up the last (1 - TOP_P) of the chance
//are dropped, and one of the rest is picked by its chance.
//The looking ahead runs on a thread of its own (plan), so the game keeps drawing while it thinks. The copy of the
//fight is made on the game's thread first, so nothing in the real fight is read while it changes.
//Used by battle for every fighter that is not the player (and not the other side of a 1v1), and for cheat mode's
//scores of the player's own actions (hints); aiTest and aiTrain play whole fights with it
public class battleAI {

    public static final double TOP_P = 0.85, TEMPERATURE = 0.02;

    private static final Random rdm = new Random();

    private static final ExecutorService THINKING = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "battleAI");
        t.setDaemon(true);
        return t;
    });

    //What me does on its turn against them (it moves last in the round, after the player), worked out on a thread
    public static CompletableFuture<battleSim.action> plan (character me, character them, aiStrength strength) {
        battleSim sim = battleSim.of(me, them);
        sim.doubleKo = doubleKoForComputer(me);
        return CompletableFuture.supplyAsync(() -> choose(sim, 0, 0, strength, new Random(rdm.nextLong())), THINKING);
    }

    //Both falling at once: in a 1v1 the player wins, in the campaign the computer does (battle.checkForDead). For the
    //computer as side 0
    private static double doubleKoForComputer (character computer) {
        return computer.getHandler() != null && computer.getHandler().getDuel() != null ? 0 : 1;
    }

    //What side i of sim does, ender moving last in the round
    public static battleSim.action choose (battleSim sim, int i, int ender, aiStrength strength, Random random) {
        List<battleSim.action> options = sim.actions(i);
        if (options.size() == 1) return options.get(0);
        return options.get(pick(sim.values(i, ender, strength), random));
    }

    //Does the action in the real fight. Things may have changed while it thought (an item that changed into another):
    //what can not be done any more is a plain hit
    public static void perform (battleSim.action a, character me, character them) {
        switch (a.kind) {
            case battleSim.MOVE: {
                gemstone held = me.getGemstone();
                if (held != null && held.usesLeft(a.index, me)) {
                    held.useMove(a.index, me, them);
                    return;
                }
                break;
            }
            case battleSim.SWITCH: {
                List<gemstone> rocks = battleSim.rockOrder(me);
                if (a.index < rocks.size()) {
                    rocks.get(a.index).use(me, them);
                    return;
                }
                break;
            }
            case battleSim.ITEM: {
                List<item> items = battleSim.itemOrder(me);
                if (a.index < items.size()) {
                    item held = items.get(a.index);
                    held.use(me, them);
                    me.consume(held);
                    return;
                }
                break;
            }
            default:
                break;
        }
        me.getHandler().getDiealoguecontainer().add(new dialogue(me, "Hit " + them.getName()));
        them.hitBy(me, () -> them.takeDmg(me.getLevel()*2+5));
    }

    //---- Cheat mode ----

    //How good each of the player's actions is right now, by the same AI as the fighters: the player (you) moves first
    //in the round, the computer last. chance: the judge gives a real chance to win (the trained one), else a score
    public static class hints {
        public final List<battleSim.action> actions;
        public final double[] values;
        public final boolean chance;
        public final int best;
        hints (List<battleSim.action> actions, double[] values, boolean chance) {
            this.actions = actions;
            this.values = values;
            this.chance = chance;
            int b = 0;
            for (int k = 1; k < values.length; k++) if (values[k] > values[b]) b = k;
            this.best = b;
        }

        //The value of the action of this kind and index, or NaN when there is no such action now
        public double of (int kind, int index) {
            int k = indexOf(kind, index);
            return k < 0 ? Double.NaN : values[k];
        }

        public boolean isBest (int kind, int index) {return indexOf(kind, index) == best;}

        private int indexOf (int kind, int index) {
            for (int k = 0; k < actions.size(); k++) if (actions.get(k).kind == kind && actions.get(k).index == index) return k;
            return -1;
        }
    }

    public static CompletableFuture<hints> hintsFor (character you, character them, aiStrength strength) {
        battleSim sim = battleSim.of(you, them);
        //Side 0 is the player here: it wins a double fall in a 1v1, loses it in the campaign
        sim.doubleKo = 1 - doubleKoForComputer(them);
        return CompletableFuture.supplyAsync(() -> new hints(sim.actions(0), sim.values(0, 1, strength), strength.givesChance()), THINKING);
    }

    //At random by score: softmax at TEMPERATURE, only the likeliest that together make up TOP_P of the chance
    public static int pick (double[] scores, Random random) {
        double top = Double.NEGATIVE_INFINITY;
        for (double s : scores) top = Math.max(top, s);
        double[] weight = new double[scores.length];
        double total = 0;
        for (int i = 0; i < scores.length; i++) {
            weight[i] = Math.exp((scores[i] - top)/TEMPERATURE);
            total += weight[i];
        }
        List<Integer> order = new ArrayList<Integer>();
        for (int i = 0; i < scores.length; i++) order.add(i);
        order.sort((a, b) -> Double.compare(weight[b], weight[a]));
        List<Integer> kept = new ArrayList<Integer>();
        double covered = 0, keptTotal = 0;
        for (int i : order) {
            kept.add(i);
            keptTotal += weight[i];
            covered += weight[i]/total;
            if (covered >= TOP_P) break;
        }
        double roll = random.nextDouble()*keptTotal;
        for (int i : kept) {
            roll -= weight[i];
            if (roll <= 0) return i;
        }
        return kept.get(kept.size() - 1);
    }
}
