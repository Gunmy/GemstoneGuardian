package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

//A fight as plain numbers, for battleAI to look ahead in (and aiTest and aiTrain to play whole fights in): both
//fighters' health, effects (burn, poison, healing, confusion), the rocks in their active row with each move's outcome
//(moveEffects.outcome, worked out at the start for these two), hit chance and uses, and the items in their active row.
//The rules are the game's own:
//- a move uses one of its uses, hit or miss; it hits with its chance times (1 - the user's confusion)
//- a hit does its outcome: damage (made bigger by what the user wears), damage to the user, healing (a share of the
//  user's health as it is then, for "50% current HP"), burn, poison and confusion on the target, poison and healing
//  over time on the user (each only if stronger than what is on already, character.combine), cleanse (clears
//  everything on the user, healing over time too), reflection (the user's burn, poison and confusion onto the
//  target), restore (every rock's uses back)
//- an item does its effects and is gone; switching rock does nothing else; without a rock with moves, a plain hit
//- a round is the player, then the game's fighter, then everyone's effects tick: burn (it counts down on a coin flip:
//  half a round on average when looking ahead, a real coin when coin is set), poison, healing over time, confusion
//- someone at 0.5 health or less has lost. Both at once is decided as the game does it (doubleKo): the campaign's
//  player loses, the 1v1's player wins; 0.5 (a draw) when nothing is said, as in training
//Only the rocks and items in the active row take part: those are the only ones that can be used in a fight
public class battleSim {

    //---- The state ----

    public static class move {
        moveEffects.outcome o;
        double chance;
        int uses, max;
        //The trained judge's work on the parts of this move that never change in the fight (valueNet), shared by the
        //copies
        double[] pre;
        Object preBy;
        move copy () {
            move m = new move();
            m.o = o;
            m.chance = chance;
            m.uses = uses;
            m.max = max;
            m.pre = pre;
            m.preBy = preBy;
            return m;
        }
    }

    public static class rock {
        move[] moves = new move[3];
        rock copy () {
            rock r = new rock();
            for (int i = 0; i < 3; i++) r.moves[i] = moves[i] == null ? null : moves[i].copy();
            return r;
        }
        boolean hasMoves () {
            for (move m : moves) if (m != null && m.uses > 0) return true;
            return false;
        }
    }

    public static class side {
        double hp, max, punch;
        //burn, burn rounds, poison, poison rounds, healing, healing rounds, confusion, confusion rounds
        double[] fx = new double[8];
        List<rock> rocks = new ArrayList<rock>();
        int equipped = -1;
        //Items: what each does, and how many
        List<moveEffects.outcome> items = new ArrayList<moveEffects.outcome>();
        List<Integer> counts = new ArrayList<Integer>();
        //The trained judge's work on each item's fixed parts (valueNet), shared by the copies
        double[][] itemPre;
        Object itemPreBy;

        side copy () {
            side s = new side();
            s.hp = hp;
            s.max = max;
            s.punch = punch;
            s.fx = fx.clone();
            for (rock r : rocks) s.rocks.add(r.copy());
            s.equipped = equipped;
            s.items = items;
            s.counts = new ArrayList<Integer>(counts);
            s.itemPre = itemPre;
            s.itemPreBy = itemPreBy;
            return s;
        }

        public double getHp () {return hp;}
        public double getMax () {return max;}
    }

    //sides[0] and sides[1]
    public side[] sides = new side[2];

    //What side 0 gets when both fall at once (side 1 gets 1 minus it)
    public double doubleKo = 0.5;

    //Set: burn counts down on a real coin flip, as in the game (whole fights in aiTrain). Unset: by half a round, the
    //average, so looking ahead does not depend on luck
    public Random coin;

    public battleSim copy () {
        battleSim b = new battleSim();
        b.sides[0] = sides[0].copy();
        b.sides[1] = sides[1].copy();
        b.doubleKo = doubleKo;
        b.coin = coin;
        return b;
    }

    //The state of a fight between a and b (a is side 0), with the rocks and items in their active rows (rockOrder,
    //itemOrder)
    public static battleSim of (character a, character b) {
        battleSim s = new battleSim();
        s.sides[0] = sideOf(a, b);
        s.sides[1] = sideOf(b, a);
        return s;
    }

    //The rocks in the active row, in slot order
    public static List<gemstone> rockOrder (character c) {
        List<gemstone> rocks = new ArrayList<gemstone>();
        for (int i = 0; i < inventory.ACTIVE; i++) {
            itemStack stack = c.getInventory().get(inventory.slotKind.ROCK, i);
            if (stack != null && stack.getItem() instanceof gemstone && !rocks.contains(stack.getItem())) rocks.add((gemstone) stack.getItem());
        }
        return rocks;
    }

    //The items in the active row that do something in a fight and are used up, in slot order
    public static List<item> itemOrder (character c) {
        List<item> items = new ArrayList<item>();
        for (int i = 0; i < inventory.ACTIVE; i++) {
            itemStack stack = c.getInventory().get(inventory.slotKind.ITEM, i);
            if (stack == null) continue;
            item held = stack.getItem();
            if (!(held instanceof gemstone) && held.getEffects() != null && held.isConsumable() && !items.contains(held)) items.add(held);
        }
        return items;
    }

    //Whether rock g is the one c holds (or an earlier form of it, see character.getGemstone)
    private static boolean isEquipped (character c, gemstone g) {
        gemstone held = c.getGemstone();
        return held != null && (g == held || g.latest() == held);
    }

    private static side sideOf (character c, character other) {
        side s = new side();
        s.hp = c.getHealth();
        s.max = c.getMaxHealth();
        //A plain hit lands through hitBy, so what the fighter wears makes it bigger too (battleAI.perform)
        s.punch = (c.getLevel()*2 + 5)*c.getDamageMultiplier();
        s.fx = c.effectState();
        for (gemstone g : rockOrder(c)) {
            rock r = new rock();
            //Base hit chance (milestones in), without the user's confusion (that is applied as it changes)
            for (int n = 0; n < 3; n++) {
                moveEffects fx = g.effectsOf(n);
                if (fx == null) continue;
                List<Integer> uses = g.usesOf(n, c);
                if (uses.get(1) <= 0) continue;
                move m = new move();
                m.o = fx.outcome(c, other);
                m.chance = g.baseChance(n, c);
                m.uses = uses.get(0);
                m.max = uses.get(1);
                r.moves[n] = m;
            }
            s.rocks.add(r);
            if (isEquipped(c, g)) s.equipped = s.rocks.size() - 1;
        }
        for (item held : itemOrder(c)) {
            s.items.add(held.getEffects().outcome(c, other));
            //Only the stacks of this very item: two stacks of the same potion made apart are two items here, each with
            //its own count (c.consume takes from this one)
            int count = 0;
            for (int i = 0; i < inventory.ACTIVE; i++) {
                itemStack stack = c.getInventory().get(inventory.slotKind.ITEM, i);
                if (stack != null && stack.getItem() == held) count += stack.getCount();
            }
            s.counts.add(Math.max(1, count));
        }
        return s;
    }

    //---- Actions ----

    public static final int MOVE = 0, SWITCH = 1, ITEM = 2, PUNCH = 3;

    public static class action {
        public final int kind, index;
        public action (int kind, int index) {this.kind = kind; this.index = index;}
        public String toString () {return new String[] {"move", "switch", "item", "punch"}[kind] + " " + index;}
    }

    //What side i can do now
    public List<action> actions (int i) {
        side s = sides[i];
        List<action> out = new ArrayList<action>();
        if (s.equipped >= 0) {
            rock r = s.rocks.get(s.equipped);
            for (int n = 0; n < 3; n++) if (r.moves[n] != null && r.moves[n].uses > 0) out.add(new action(MOVE, n));
        }
        for (int k = 0; k < s.rocks.size(); k++) if (k != s.equipped && s.rocks.get(k).hasMoves()) out.add(new action(SWITCH, k));
        boolean canFight = !out.isEmpty();
        for (int k = 0; k < s.items.size(); k++) if (s.counts.get(k) > 0) out.add(new action(ITEM, k));
        //Out of moves and rocks to switch to, a plain hit is always there, potions or not
        if (!canFight) out.add(new action(PUNCH, 0));
        return out;
    }

    //The chance that action hits (1 for anything that is not a move)
    public double chance (int i, action a) {
        if (a.kind != MOVE) return 1;
        side s = sides[i];
        move m = s.rocks.get(s.equipped).moves[a.index];
        return hitChance(s, m);
    }

    static double hitChance (side s, move m) {
        double confusion = s.fx[7] > 0 ? s.fx[6] : 0;
        return Math.max(0, Math.min(1, m.chance*(1 - confusion)));
    }

    //Side i does a (hit or not)
    public void apply (int i, action a, boolean hit) {
        side me = sides[i], them = sides[1 - i];
        switch (a.kind) {
            case SWITCH: me.equipped = a.index; return;
            case PUNCH: them.hp -= me.punch; return;
            case ITEM:
                me.counts.set(a.index, me.counts.get(a.index) - 1);
                outcome(me, them, me.items.get(a.index));
                return;
            default:
                move m = me.rocks.get(me.equipped).moves[a.index];
                if (hit) outcome(me, them, m.o);
                //The use is taken after, as in the game: a move that restores every rock's uses stays used itself
                m.uses -= 1;
        }
    }

    //What a heal gives: the fixed amount and the share of the health as it is now
    static double healOf (side me, moveEffects.outcome o) {
        return o.heal + o.healShare*Math.max(0, me.hp);
    }

    private static void outcome (side me, side them, moveEffects.outcome o) {
        me.hp -= o.selfDamage;
        them.hp -= o.damage;
        double heal = healOf(me, o);
        if (heal > 0) me.hp = Math.min(me.max, me.hp + heal);
        if (o.burn > 0) add(them.fx, 0, o.burn, o.burnTurns);
        if (o.poison > 0) add(them.fx, 2, o.poison, o.poisonTurns);
        if (o.selfPoison > 0) add(me.fx, 2, o.selfPoison, o.selfPoisonTurns);
        if (o.healing > 0) add(me.fx, 4, o.healing, o.healingTurns);
        if (o.confusion > 0) add(them.fx, 6, o.confusion, o.confusionTurns);
        if (o.reflect) {
            if (me.fx[1] > 0) add(them.fx, 0, me.fx[0], me.fx[1]);
            if (me.fx[3] > 0) add(them.fx, 2, me.fx[2], me.fx[3]);
            if (me.fx[7] > 0) add(them.fx, 6, me.fx[6], me.fx[7]);
            for (int k : new int[] {0, 1, 2, 3, 6, 7}) me.fx[k] = 0;
        }
        if (o.cleanse) java.util.Arrays.fill(me.fx, 0);
        if (o.restore) for (rock r : me.rocks) for (move m : r.moves) if (m != null) m.uses = m.max;
    }

    //Whether an effect put on would stay: the stronger stays, an equal one only if it lasts longer (character.combine)
    static boolean sticks (double[] fx, int at, double strength, double turns) {
        return fx[at + 1] <= 0 || strength > fx[at] || (strength == fx[at] && turns > fx[at + 1]);
    }

    private static void add (double[] fx, int at, double strength, double turns) {
        if (sticks(fx, at, strength, turns)) {
            fx[at] = strength;
            fx[at + 1] = turns;
        }
    }

    //The end of a round: everyone's effects tick
    public void tick () {
        for (side s : sides) {
            double change = 0;
            if (s.fx[1] > 0) {
                change -= s.fx[0];
                if (coin == null) s.fx[1] -= 0.5;
                else if (coin.nextBoolean()) s.fx[1] -= 1;
            }
            if (s.fx[3] > 0) {change -= s.fx[2]; s.fx[3] -= 1;}
            if (s.fx[5] > 0) {change += s.fx[4]; s.fx[5] -= 1;}
            if (s.fx[7] > 0) s.fx[7] -= 1;
            s.hp = Math.min(s.max, s.hp + change);
        }
    }

    //Fallen, as the game decides it (battle.checkForDead)
    public static final double DEAD = 0.5;

    //1 if side i has won, 0 if it has lost, doubleKo (for side 0) when both fell at once, -1 while it goes on
    public double result (int i) {
        boolean meDead = sides[i].hp <= DEAD, themDead = sides[1 - i].hp <= DEAD;
        if (meDead && themDead) return i == 0 ? doubleKo : 1 - doubleKo;
        if (themDead) return 1;
        if (meDead) return 0;
        return -1;
    }

    //---- Looking ahead ----
    //From where side i is about to act: its action, the other side's, its own again (depth actions in all), with
    //everyone's effects ticking whenever the round ends (after the side that moves last in a round: ender). Every
    //action is tried with every way it can hit or miss (by their chances); the other side is taken to play well
    //(OPPONENT_WORST of the time its best answer, else an average one), and side i its best. Where it stops, the
    //position is judged as side i's chance to win by the judge (the calculated estimate or the trained valueNet).
    //Judging by the chance to win also handles risk: behind, a long shot that could turn it around is worth more than
    //a sure small gain; ahead, the safe play is. Positions met twice are judged once (cache)

    //Overridable for testing (aiTest): -DopponentWorst=0.9
    public static final double OPPONENT_WORST = Double.parseDouble(System.getProperty("opponentWorst", "0.7"));

    //How a position is judged: side i's chance to win, with next to act and ender moving last in the round
    public interface judge {
        double value (battleSim s, int i, int next, int ender);
        //Work done once on the position a search starts from, which every position in the search copies
        default void prepare (battleSim root) {}
    }

    //The hand-made estimate (see estimate)
    public static final judge CALCULATED = (s, i, next, ender) -> estimate(s, i);

    private static class search {
        final judge judge;
        final int i, ender;
        final Map<Long, Double> seen = new HashMap<Long, Double>();
        search (judge judge, int i, int ender) {this.judge = judge; this.i = i; this.ender = ender;}
    }

    //The chance to win after each of side i's actions (in the order of actions(i)), looking strength.depth actions
    //ahead. ender: the side that moves last in a round (in the game the fighter it plays, after the player)
    //It looks ahead on a copy without the coin: the lookahead counts burn down by its average, never by the fight's dice
    public double[] values (int i, int ender, aiStrength strength) {
        search run = new search(strength.judge(), i, ender);
        battleSim root = copy();
        root.coin = null;
        run.judge.prepare(root);
        List<action> mine = root.actions(i);
        double[] out = new double[mine.size()];
        for (int k = 0; k < mine.size(); k++) out[k] = chanceNode(run, root, i, mine.get(k), strength.depth);
        return out;
    }

    //actor does a, hitting or missing by its chance; then the next one acts, until plies run out
    private static double chanceNode (search run, battleSim state, int actor, action a, int plies) {
        double p = state.chance(actor, a), total = 0;
        for (int outcome = 0; outcome < 2; outcome++) {
            boolean hit = outcome == 0;
            double weight = hit ? p : 1 - p;
            if (weight <= 0) continue;
            battleSim s = state.copy();
            s.apply(actor, a, hit);
            double v = s.result(run.i);
            if (v < 0 && actor == run.ender) {
                s.tick();
                v = s.result(run.i);
            }
            if (v < 0) v = plies <= 1 ? leaf(run, s, 1 - actor) : choiceNode(run, s, 1 - actor, plies - 1);
            total += weight*v;
        }
        return total;
    }

    private static double leaf (search run, battleSim s, int next) {
        long key = s.key()*31 + next;
        Double known = run.seen.get(key);
        if (known != null) return known;
        double v = run.judge.value(s, run.i, next, run.ender);
        run.seen.put(key, v);
        return v;
    }

    //actor picks: side i its best, the other one mostly its best answer
    private static double choiceNode (search run, battleSim state, int actor, int plies) {
        long key = (state.key()*31 + actor)*31 + plies + 7;
        Double known = run.seen.get(key);
        if (known != null) return known;
        List<action> options = state.actions(actor);
        double best = 0, worst = 1, sum = 0;
        for (action a : options) {
            double v = chanceNode(run, state, actor, a, plies);
            best = Math.max(best, v);
            worst = Math.min(worst, v);
            sum += v;
        }
        double v = actor == run.i ? best : OPPONENT_WORST*worst + (1 - OPPONENT_WORST)*sum/options.size();
        run.seen.put(key, v);
        return v;
    }

    //A fingerprint of everything that changes in a fight, for the search's cache
    long key () {
        long h = 1469598103934665603L;
        for (side s : sides) {
            h = mix(h, Double.doubleToLongBits(s.hp));
            for (double f : s.fx) h = mix(h, Double.doubleToLongBits(f));
            h = mix(h, s.equipped);
            for (rock r : s.rocks) for (move m : r.moves) if (m != null) h = mix(h, m.uses);
            for (int c : s.counts) h = mix(h, c);
        }
        return h;
    }

    private static long mix (long h, long v) {
        h ^= v;
        h *= 0x100000001B3L;
        return h ^ (h >>> 29);
    }

    //---- Judging a position (the calculated estimate) ----
    //Side i's chance to win, from how many turns each side needs to finish the other (its best expected hit a turn
    //against the other's health, with the burn, poison and healing still to come and the potions still to drink),
    //blended with plain health. A race score more than a true chance: battleAI shows it as a score, not a percentage

    public static double estimate (battleSim s, int i) {
        side me = s.sides[i], them = s.sides[1 - i];
        double myHp = effective(me), theirHp = effective(them);
        double myTurns = theirHp/Math.max(1e-6, bestHit(me, them)), theirTurns = myHp/Math.max(1e-6, bestHit(them, me));
        double race = theirTurns/(myTurns + theirTurns);
        double health = 1/(1 + Math.exp(-4*(myHp/me.max - theirHp/them.max)));
        return 0.7*race + 0.3*health;
    }

    //Health counting what is still to come: burn (it lasts twice its rounds on average) and poison off, healing over
    //time and potions on
    private static double effective (side s) {
        double hp = s.hp;
        if (s.fx[1] > 0) hp -= s.fx[0]*s.fx[1]*2;
        if (s.fx[3] > 0) hp -= s.fx[2]*s.fx[3];
        if (s.fx[5] > 0) hp += s.fx[4]*s.fx[5];
        for (int k = 0; k < s.items.size(); k++) hp += healOf(s, s.items.get(k))*s.counts.get(k)*0.8;
        return Math.max(1, Math.min(s.max*1.5, hp));
    }

    //Its best expected damage a turn: the moves of its equipped rock, or a bit less with another rock it can switch to
    static double bestHit (side s, side target) {
        double confusion = s.fx[7] > 0 ? s.fx[6] : 0, best = 0;
        for (int k = 0; k < s.rocks.size(); k++) {
            for (move m : s.rocks.get(k).moves) {
                if (m == null || m.uses <= 0) continue;
                double over = m.o.burn*Math.min(2*m.o.burnTurns, 4) + m.o.poison*Math.min(m.o.poisonTurns, 4);
                double hit = m.chance*(1 - confusion)*(m.o.damage + over/2);
                best = Math.max(best, k == s.equipped ? hit : hit*0.85);
            }
        }
        return Math.max(best, s.punch*0.5);
    }
}
