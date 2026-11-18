package game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

//The trained judge: a small neural network that looks at a fight (a battleSim) and gives side i's chance to win. It is
//trained outside the game (tools/ai, Python), which reads the inputs this class makes (aiTrain writes them) and writes
//the weights back as a text file (game/reading/valueNet.txt). Only this class turns a fight into numbers, so the
//training never has its own copy of the rules.
//
//Everything is level independent: amounts are shares of a max or current health, never health, damage or levels.
//
//How it looks at a fight:
//1. Every move of every rock in either side's active row is one row: the move (what it does, fixed for the fight:
//   moveStatic), how it stands now (uses, whether its rock is held, whether it would finish the target, whether its
//   burn and poison would stick: moveNow) and the situation (both sides' stats, sideStats, from the move owner's view,
//   and who acts next). One small shared net (move1, move2) reads each row.
//2. A rock is the sum of its moves' rows, with whether it has uses left, through the rock layer.
//3. Items the same way: one row each (itemStatic, itemNow, the situation) through their own small shared net.
//4. A side is: its held rock, its other rocks (average and strongest of each number), its items (average and
//   strongest), its stats. Both sides and who acts next go through the head (head1, head2, out) to the chance.
//Running it: the move's fixed part of the first layer is worked out once per fight (battleSim.move.pre), and the
//situation's part once per side, so per move only the changing part and the second layer are left
public class valueNet implements battleSim.judge {

    //Sizes of the inputs: a move's fixed part, a move's changing part, a side's stats; rows per side
    public static final int S = 20, R = 11, F = 16, CTX = 2*F + 2, ROW = S + R + CTX;
    public static final int ROCKS = inventory.ACTIVE, MOVES = 3, ITEMS = inventory.ACTIVE;
    //Sizes of the layers: read from the weights file (load), so nets of different widths all work
    private int MOVE_H, MOVE_OUT, ITEM_H, ITEM_OUT, ROCK_OUT, HEAD_H, SIDE, HEAD_IN;

    //---- The inputs ----

    //What a move does, for its owner me against them: fixed for the fight
    public static double[] moveStatic (battleSim.move m, battleSim.side me, battleSim.side them) {
        double[] x = effectStatic(m.o, me, them, m.chance);
        x[19] = m.max/10.0;
        return x;
    }

    //What an item does: as a move that always hits
    public static double[] itemStatic (moveEffects.outcome o, battleSim.side me, battleSim.side them) {
        return effectStatic(o, me, them, 1);
    }

    private static double[] effectStatic (moveEffects.outcome o, battleSim.side me, battleSim.side them, double chance) {
        double[] x = new double[S];
        x[0] = o.damage/them.max;
        x[1] = o.selfDamage/me.max;
        x[2] = o.heal/me.max;
        x[3] = o.healShare;
        x[4] = o.burn/them.max;
        x[5] = o.burnTurns/10.0;
        x[6] = o.poison/them.max;
        x[7] = o.poisonTurns/10.0;
        x[8] = o.selfPoison/me.max;
        x[9] = o.selfPoisonTurns/10.0;
        x[10] = o.healing/me.max;
        x[11] = o.healingTurns/10.0;
        x[12] = o.confusion;
        x[13] = o.confusionTurns/10.0;
        x[14] = o.cleanse ? 1 : 0;
        x[15] = o.restore ? 1 : 0;
        x[16] = o.reflect ? 1 : 0;
        x[17] = chance;
        x[18] = chance*o.damage/them.max;
        return x;
    }

    //How a move stands now: its uses, whether its rock is held, and what it would do to the target now
    public static double[] moveNow (battleSim.move m, boolean held, battleSim.side me, battleSim.side them) {
        double[] x = new double[R];
        x[0] = m.uses/10.0;
        x[1] = m.max > 0 ? (double) m.uses/m.max : 0;
        x[2] = m.uses > 0 ? 1 : 0;
        x[3] = held ? 1 : 0;
        effectNow(x, m.o, battleSim.hitChance(me, m), me, them);
        return x;
    }

    //How an item stands now: how many are left, and what it would do now
    public static double[] itemNow (moveEffects.outcome o, int count, battleSim.side me, battleSim.side them) {
        double[] x = new double[R];
        x[0] = count/3.0;
        x[1] = count > 0 ? 1 : 0;
        x[2] = count > 0 ? 1 : 0;
        x[3] = 0;
        effectNow(x, o, 1, me, them);
        return x;
    }

    private static void effectNow (double[] x, moveEffects.outcome o, double chance, battleSim.side me, battleSim.side them) {
        double theirHp = Math.max(battleSim.DEAD, them.hp);
        x[4] = Math.min(2, o.damage/theirHp);
        x[5] = o.damage > 0 && them.hp - o.damage <= battleSim.DEAD ? 1 : 0;
        x[6] = Math.min(2, chance*o.damage/theirHp);
        double missing = me.max - me.hp, heal = battleSim.healOf(me, o);
        x[7] = heal > 0 && missing > 0 ? Math.min(1, heal/missing) : 0;
        x[8] = o.burn > 0 && battleSim.sticks(them.fx, 0, o.burn, o.burnTurns) ? 1 : 0;
        x[9] = o.poison > 0 && battleSim.sticks(them.fx, 2, o.poison, o.poisonTurns) ? 1 : 0;
        x[10] = chance;
    }

    //A side's stats, from its own view (them is the other side)
    public static double[] sideStats (battleSim.side me, battleSim.side them) {
        double[] x = new double[F];
        x[0] = Math.max(0, Math.min(1, me.hp/me.max));
        x[1] = me.punch/them.max;
        x[2] = me.fx[0]/me.max;
        x[3] = me.fx[1]/10;
        x[4] = me.fx[2]/me.max;
        x[5] = me.fx[3]/10;
        x[6] = me.fx[4]/me.max;
        x[7] = me.fx[5]/10;
        x[8] = me.fx[7] > 0 ? me.fx[6] : 0;
        x[9] = me.fx[7]/10;
        boolean heldHasUses = me.equipped >= 0 && me.rocks.get(me.equipped).hasMoves();
        x[10] = heldHasUses ? 1 : 0;
        int others = 0;
        for (int k = 0; k < me.rocks.size(); k++) if (k != me.equipped && me.rocks.get(k).hasMoves()) others++;
        x[11] = others/4.0;
        int kinds = 0;
        for (int c : me.counts) if (c > 0) kinds++;
        x[12] = kinds/4.0;
        double myHp = Math.max(battleSim.DEAD, me.hp), theirHp = Math.max(battleSim.DEAD, them.hp);
        //The threat: their best expected hit against my health
        x[13] = Math.min(2, battleSim.bestHit(them, me)/myHp);
        //What all my uses left could still do, against their health
        double left = 0, can = 0;
        for (int k = 0; k < me.rocks.size(); k++) {
            for (battleSim.move m : me.rocks.get(k).moves) {
                if (m == null || m.uses <= 0) continue;
                left += m.uses*battleSim.hitChance(me, m)*(m.o.damage + m.o.burn*m.o.burnTurns*2 + m.o.poison*m.o.poisonTurns);
                if (k == me.equipped && m.o.damage > 0 && them.hp - m.o.damage <= battleSim.DEAD) can = 1;
            }
        }
        x[14] = Math.min(5, left/theirHp)/5;
        //A held move that finishes them if it hits
        x[15] = can;
        return x;
    }

    //The situation every row of side o sees: its stats, the other side's, whether o acts next, and whether the
    //effects tick right after the next action
    public static double[] context (double[] mine, double[] theirs, boolean oNext, boolean tickNext) {
        double[] x = new double[CTX];
        System.arraycopy(mine, 0, x, 0, F);
        System.arraycopy(theirs, 0, x, F, F);
        x[2*F] = oNext ? 1 : 0;
        x[2*F + 1] = tickNext ? 1 : 0;
        return x;
    }

    //---- The net ----

    private final Map<String, double[][]> w = new HashMap<String, double[][]>();
    private final Map<String, double[]> b = new HashMap<String, double[]>();

    private double[][] W (String name) {return w.get(name);}
    private double[] B (String name) {return b.get(name);}

    //Side i's chance to win, with next to act and ender moving last in the round
    @Override
    public double value (battleSim s, int i, int next, int ender) {
        double[] in = new double[HEAD_IN];
        double[][] stats = {sideStats(s.sides[0], s.sides[1]), sideStats(s.sides[1], s.sides[0])};
        for (int k = 0; k < 2; k++) {
            int o = k == 0 ? i : 1 - i;
            double[] ctx = context(stats[o], stats[1 - o], next == o, next == ender);
            sideSummary(s, o, stats[o], ctx, in, k*SIDE);
        }
        in[2*SIDE] = next == i ? 1 : 0;
        in[2*SIDE + 1] = next == ender ? 1 : 0;
        double[] h = relu(affine(W("head1"), B("head1"), in));
        h = relu(affine(W("head2"), B("head2"), h));
        double logit = affine(W("out"), B("out"), h)[0];
        return 1/(1 + Math.exp(-logit));
    }

    //The fixed parts of every move and item, worked out on the position a search starts from: every position in the
    //search is a copy of it and shares them
    @Override
    public void prepare (battleSim root) {
        for (int o = 0; o < 2; o++) {
            battleSim.side me = root.sides[o], them = root.sides[1 - o];
            for (battleSim.rock r : me.rocks) for (battleSim.move m : r.moves) if (m != null) staticOf(m, me, them);
            itemsOf(me, them);
        }
    }

    private double[] staticOf (battleSim.move m, battleSim.side me, battleSim.side them) {
        if (m.pre == null || m.preBy != this) {
            m.pre = staticPart(W("move1"), B("move1"), moveStatic(m, me, them));
            m.preBy = this;
        }
        return m.pre;
    }

    private double[][] itemsOf (battleSim.side me, battleSim.side them) {
        if (me.itemPre == null || me.itemPreBy != this) {
            double[][] pre = new double[me.items.size()][];
            for (int k = 0; k < me.items.size(); k++) pre[k] = staticPart(W("item1"), B("item1"), itemStatic(me.items.get(k), me, them));
            me.itemPre = pre;
            me.itemPreBy = this;
        }
        return me.itemPre;
    }

    //Side o's summary, written into out from at
    private void sideSummary (battleSim s, int o, double[] stats, double[] ctx, double[] out, int at) {
        battleSim.side me = s.sides[o], them = s.sides[1 - o];
        //The situation's part of the first layers, the same for every row of this side
        double[] moveCtx = partial(W("move1"), ctx, S + R), itemCtx = partial(W("item1"), ctx, S + R);

        double[] held = new double[ROCK_OUT], mean = new double[ROCK_OUT], max = new double[ROCK_OUT];
        int others = 0;
        for (int k = 0; k < me.rocks.size() && k < ROCKS; k++) {
            battleSim.rock r = me.rocks.get(k);
            double[] sum = new double[MOVE_OUT + 1];
            for (battleSim.move m : r.moves) {
                if (m == null) continue;
                double[] h = moveCtx.clone();
                add(h, staticOf(m, me, them));
                add(h, partial(W("move1"), moveNow(m, k == me.equipped, me, them), S, R));
                h = relu(affine(W("move2"), B("move2"), relu(h)));
                for (int j = 0; j < MOVE_OUT; j++) sum[j] += h[j];
            }
            sum[MOVE_OUT] = r.hasMoves() ? 1 : 0;
            double[] rock = relu(affine(W("rock"), B("rock"), sum));
            if (k == me.equipped) held = rock;
            else {
                for (int j = 0; j < ROCK_OUT; j++) {
                    mean[j] += rock[j];
                    max[j] = others == 0 ? rock[j] : Math.max(max[j], rock[j]);
                }
                others++;
            }
        }
        if (others > 0) for (int j = 0; j < ROCK_OUT; j++) mean[j] /= others;

        double[][] itemPre = itemsOf(me, them);
        double[] itemMean = new double[ITEM_OUT], itemMax = new double[ITEM_OUT];
        int items = 0;
        for (int k = 0; k < me.items.size() && k < ITEMS; k++) {
            int count = me.counts.get(k);
            if (count <= 0) continue;
            double[] h = itemCtx.clone();
            add(h, itemPre[k]);
            add(h, partial(W("item1"), itemNow(me.items.get(k), count, me, them), S, R));
            h = relu(affine(W("item2"), B("item2"), relu(h)));
            for (int j = 0; j < ITEM_OUT; j++) {
                itemMean[j] += h[j];
                itemMax[j] = items == 0 ? h[j] : Math.max(itemMax[j], h[j]);
            }
            items++;
        }
        if (items > 0) for (int j = 0; j < ITEM_OUT; j++) itemMean[j] /= items;

        int p = at;
        for (double v : held) out[p++] = v;
        for (double v : mean) out[p++] = v;
        for (double v : max) out[p++] = v;
        for (double v : itemMean) out[p++] = v;
        for (double v : itemMax) out[p++] = v;
        for (double v : stats) out[p++] = v;
    }

    //The first layer's part for the fixed inputs (columns 0 to S), with the bias
    private static double[] staticPart (double[][] w, double[] b, double[] x) {
        double[] h = partial(w, x, 0);
        add(h, b);
        return h;
    }

    //w times x, where x fills the columns from col
    private static double[] partial (double[][] w, double[] x, int col) {return partial(w, x, col, x.length);}

    private static double[] partial (double[][] w, double[] x, int col, int n) {
        double[] h = new double[w.length];
        for (int r = 0; r < w.length; r++) {
            double sum = 0;
            double[] row = w[r];
            for (int c = 0; c < n; c++) sum += row[col + c]*x[c];
            h[r] = sum;
        }
        return h;
    }

    private static double[] affine (double[][] w, double[] b, double[] x) {
        double[] h = partial(w, x, 0);
        add(h, b);
        return h;
    }

    private static void add (double[] into, double[] x) {for (int j = 0; j < into.length; j++) into[j] += x[j];}

    private static double[] relu (double[] x) {
        for (int j = 0; j < x.length; j++) if (x[j] < 0) x[j] = 0;
        return x;
    }

    //---- Loading ----
    //The file: "valueNet 1", then for each tensor "<name> <rows> <cols>" and its numbers row by row (a bias has one
    //row). Written by tools/ai/train.py

    public static valueNet load (InputStream in) throws IOException {
        valueNet net = new valueNet();
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
        }
        String[] t = text.toString().trim().split("\\s+");
        int p = 0;
        if (!t[p++].equals("valueNet") || !t[p++].equals("1")) throw new IOException("Not a valueNet file");
        while (p < t.length) {
            String name = t[p++];
            int rows = Integer.parseInt(t[p++]), cols = Integer.parseInt(t[p++]);
            double[][] m = new double[rows][cols];
            for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) m[r][c] = Double.parseDouble(t[p++]);
            if (name.endsWith(".weight")) net.w.put(name.substring(0, name.length() - 7), m);
            else if (name.endsWith(".bias")) net.b.put(name.substring(0, name.length() - 5), m[0]);
        }
        for (String name : new String[] {"move1", "move2", "item1", "item2", "rock", "head1", "head2", "out"}) {
            if (net.w.get(name) == null || net.b.get(name) == null) throw new IOException("valueNet: " + name + " is missing");
        }
        net.MOVE_H = net.w.get("move1").length;
        net.MOVE_OUT = net.w.get("move2").length;
        net.ITEM_H = net.w.get("item1").length;
        net.ITEM_OUT = net.w.get("item2").length;
        net.ROCK_OUT = net.w.get("rock").length;
        net.HEAD_H = net.w.get("head1").length;
        net.SIDE = net.ROCK_OUT*3 + net.ITEM_OUT*2 + F;
        net.HEAD_IN = 2*net.SIDE + 2;
        net.check("move1", net.MOVE_H, ROW);
        net.check("move2", net.MOVE_OUT, net.MOVE_H);
        net.check("item1", net.ITEM_H, ROW);
        net.check("item2", net.ITEM_OUT, net.ITEM_H);
        net.check("rock", net.ROCK_OUT, net.MOVE_OUT + 1);
        net.check("head1", net.HEAD_H, net.HEAD_IN);
        net.check("head2", net.HEAD_H, net.HEAD_H);
        net.check("out", 1, net.HEAD_H);
        return net;
    }

    public static valueNet load (File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return load(in);
        }
    }

    private void check (String name, int rows, int cols) throws IOException {
        double[][] m = w.get(name);
        double[] bias = b.get(name);
        if (m == null || bias == null) throw new IOException("valueNet: " + name + " is missing");
        if (m.length != rows || m[0].length != cols || bias.length != rows) {
            throw new IOException("valueNet: " + name + " is " + m.length + "x" + m[0].length + ", should be " + rows + "x" + cols);
        }
    }

    //The net that comes with the game (game/reading/valueNet.txt), or null when there is none yet
    private static valueNet shared;
    private static boolean sharedLoaded = false;

    public static synchronized valueNet shared () {
        if (!sharedLoaded) {
            sharedLoaded = true;
            try (InputStream in = valueNet.class.getResourceAsStream("/game/reading/valueNet.txt")) {
                if (in != null) shared = load(in);
            } catch (IOException | RuntimeException e) {
                System.out.println("Kunne ikke lese valueNet.txt: " + e);
            }
        }
        return shared;
    }
}
