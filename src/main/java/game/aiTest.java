package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

//Tests the fighters' AI (battleAI) without the game: real characters with real rocks and items, made without a game
//running, fight whole fights in battleSim. Run from the project folder after building:
//
//   java --module-path "<javafx jars>;target\classes" -m GemstoneGuardian/game.aiTest [fights]
//
//It prints:
//1. the search AI against the old one (a random move of the equipped rock) and a greedy one (always its biggest
//   expected hit right now), with every rock against every rock, each side once first
//2. the rocks against each other, both played by the search AI: how often each one wins (for balancing)
//3. bosses' loadouts (four rocks and potions) against a player's loadout, search AI against the old one
public class aiTest {

    public static final int LEVEL = 15, MAX_ROUNDS = 80;

    interface policy {
        battleSim.action choose (battleSim s, int i, int ender, Random r);
    }

    //The search AI as campaigns start with it: calculated, -Dplies (3) actions ahead; -Dtrained=true for the trained net
    static final aiStrength STRENGTH = new aiStrength(Boolean.getBoolean("trained"), Integer.parseInt(System.getProperty("plies", "3")));
    static final policy SEARCH = (s, i, ender, r) -> battleAI.choose(s, i, ender, STRENGTH, r);

    //The old AI: a random move of the equipped rock; out of moves, the first rock that has some; else a plain hit
    static final policy RANDOM = (s, i, ender, r) -> {
        List<battleSim.action> moves = new ArrayList<battleSim.action>();
        for (battleSim.action a : s.actions(i)) if (a.kind == battleSim.MOVE) moves.add(a);
        if (!moves.isEmpty()) return moves.get(r.nextInt(moves.size()));
        return s.actions(i).get(0);
    };

    //Greedy: the move with the biggest expected hit now (damage times its chance); out of moves, switches
    static final policy GREEDY = (s, i, ender, r) -> {
        battleSim.action best = null;
        double bestValue = -1;
        for (battleSim.action a : s.actions(i)) {
            if (a.kind != battleSim.MOVE) continue;
            battleSim.side me = s.sides[i];
            moveEffects.outcome o = me.rocks.get(me.equipped).moves[a.index].o;
            double v = s.chance(i, a)*(o.damage + o.burn*o.burnTurns + o.poison*o.poisonTurns);
            if (v > bestValue) {bestValue = v; best = a;}
        }
        return best != null ? best : s.actions(i).get(0);
    };

    //One fight: side 0 moves first in each round. 1 if side 0 wins, 0 if side 1 does, 0.5 for a draw
    static double fight (battleSim start, policy first, policy second, Random r) {
        battleSim s = start.copy();
        policy[] who = {first, second};
        for (int round = 0; round < MAX_ROUNDS; round++) {
            for (int i = 0; i < 2; i++) {
                battleSim.action a = who[i].choose(s, i, 1, r);
                s.apply(i, a, r.nextDouble() < s.chance(i, a));
                double res = s.result(0);
                if (res >= 0) return res;
            }
            s.tick();
            double res = s.result(0);
            if (res >= 0) return res;
        }
        return 0.5;
    }

    static character fighter (String name, int level, List<item> carried) {
        character c = new character(name, 1, 1, 0, 0, 20, 20, 0, null, 3);
        c.setLevel(level);
        c.resetHealth();
        for (item held : carried) c.give(held);
        return c;
    }

    //How often policy a wins against b with these loadouts, each side first half of the time
    static double winRate (Supplier<List<item>> loadA, Supplier<List<item>> loadB, policy a, policy b, int fights, Random r) {
        double won = 0;
        for (int k = 0; k < fights; k++) {
            character ca = fighter("A", LEVEL, loadA.get()), cb = fighter("B", LEVEL, loadB.get());
            if (k % 2 == 0) won += fight(battleSim.of(ca, cb), a, b, r);
            else won += 1 - fight(battleSim.of(cb, ca), b, a, r);
        }
        return won/fights;
    }

    public static void main (String[] args) {
        int fights = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        Random r = new Random(7);
        gemstones gems = new gemstones(null);
        items things = new items(null);
        List<Supplier<item>> rocks = new ArrayList<Supplier<item>>();
        List<String> names = new ArrayList<String>();
        for (Supplier<item> maker : new itemRegistry(gems).getMakers()) {
            item made = maker.get();
            if (!(made instanceof gemstone) || made instanceof elementalRock) continue;
            rocks.add(maker);
            names.add(made.getName());
        }
        long start = System.nanoTime();

        //1. Against the old AI and a greedy one, every rock against every rock
        double vsRandom = 0, vsGreedy = 0;
        int pairs = 0;
        for (int x = 0; x < rocks.size(); x++) {
            for (int y = 0; y < rocks.size(); y++) {
                Supplier<item> rx = rocks.get(x), ry = rocks.get(y);
                vsRandom += winRate(() -> List.of(rx.get()), () -> List.of(ry.get()), SEARCH, RANDOM, Math.max(2, fights/5), r);
                vsGreedy += winRate(() -> List.of(rx.get()), () -> List.of(ry.get()), SEARCH, GREEDY, Math.max(2, fights/5), r);
                pairs++;
            }
        }
        System.out.printf("1. Search AI wins %.1f%% against the old AI and %.1f%% against a greedy one (%d rock pairings, level %d)%n",
            100*vsRandom/pairs, 100*vsGreedy/pairs, pairs, LEVEL);
        //Sanity: against itself it should win half
        double self = 0;
        for (int x = 0; x < rocks.size(); x++) {
            Supplier<item> rx = rocks.get(x), ry = rocks.get((x*7 + 3) % rocks.size());
            self += winRate(() -> List.of(rx.get()), () -> List.of(ry.get()), SEARCH, SEARCH, Math.max(2, fights/5), r)
                + winRate(() -> List.of(ry.get()), () -> List.of(rx.get()), SEARCH, SEARCH, Math.max(2, fights/5), r);
        }
        System.out.printf("   (search AI against itself, both ways round: %.1f%%, should be 50)%n", 100*self/(2*rocks.size()));

        //2. Rocks against each other, both by the search AI
        System.out.println("2. Each rock's wins against every other rock, both played by the search AI:");
        double[] score = new double[rocks.size()];
        for (int x = 0; x < rocks.size(); x++) {
            for (int y = x + 1; y < rocks.size(); y++) {
                Supplier<item> rx = rocks.get(x), ry = rocks.get(y);
                double w = winRate(() -> List.of(rx.get()), () -> List.of(ry.get()), SEARCH, SEARCH, fights, r);
                score[x] += w;
                score[y] += 1 - w;
            }
        }
        List<Integer> order = new ArrayList<Integer>();
        for (int x = 0; x < rocks.size(); x++) order.add(x);
        order.sort((p, q) -> Double.compare(score[q], score[p]));
        for (int x : order) System.out.printf("   %-22s %5.1f%%%n", names.get(x), 100*score[x]/(rocks.size() - 1));

        //3. Loadouts with four rocks and potions, as the bosses have
        Supplier<List<item>> boss = () -> List.of(gems.meteorite(), gems.boltstone(), gems.uranium(), gems.healite(), things.healingPotion(), things.cleansingPotion());
        Supplier<List<item>> player = () -> List.of(gems.lava(), gems.obsidian(), gems.giantBolder(), gems.ice(), things.healingPotion());
        System.out.printf("3. With four rocks and potions: search AI %.1f%% against the old AI, %.1f%% against a greedy one%n",
            100*winRate(boss, player, SEARCH, RANDOM, fights, r), 100*winRate(boss, player, SEARCH, GREEDY, fights, r));
        System.out.printf("   (old AI with the boss loadout against search AI with the player's: %.1f%%)%n",
            100*winRate(boss, player, RANDOM, SEARCH, fights, r));
        System.out.printf("Took %.1f s%n", (System.nanoTime() - start)/1e9);
    }
}
