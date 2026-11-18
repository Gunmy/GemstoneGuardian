package game;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

//The game's side of training the trained judge (valueNet). tools/ai/train.py runs it; it can be run by hand too, from
//the project folder after building:
//
//   java --module-path "<javafx jars>;target\classes" -m GemstoneGuardian/game.aiTrain <command> key=value ...
//
//Commands:
//- selfplay out=<file> fights=N depth=D epsilon=E seed=S [weights=<file>] [calcShare=C] [old=<file,file>] [oldShare=O]
//  [threads=T]
//  Plays whole fights in battleSim with the search AI on both sides, with random loadouts and often a random start
//  (hurt fighters, used-up moves, effects already on), and every so often (epsilon) a random action, so the net also
//  sees the odd positions a person makes. Burn counts down on a real coin. Who plays: without weights, the calculated
//  judge on both sides. With weights, the net against a pool of opponents, a new pick each fight: the calculated judge
//  (calcShare of the fights), one of the older nets in old (oldShare), else itself. The net takes a random side.
//  [varied=V] [invented=I]: each rock (and potion) a fighter carries is, with chance V, a real one with its numbers
//  changed a little (madeUp.vary), with chance I a made-up one (madeUp.invent), else the real one unchanged, so the
//  net learns what effects are worth rather than which rock they belong to. eval and check use only real rocks.
//  Writes every position before an action, with how the fight ended, as valueNet's inputs (the format is below).
//  Fights that reach MAX_ROUNDS are dropped; both falling at once counts as a draw (0.5).
//- eval weights=<file> fights=N depth=D [opponentDepth=3] [seed=S] [threads=T]
//  The net at depth D against the calculated judge at opponentDepth, random loadouts, each loadout played from both
//  sides. Prints "winrate <the net's share of the points>" and how long one of the net's decisions took.
//- check weights=<file> out=<file> values=<file> fights=N
//  As selfplay with the calculated judge, and writes the net's value for both sides of every position to values (one
//  line "v0 v1" each), for train.py to check that its forward pass gives the same.
//
//- rocks rock=<name> levels=1,2,... perLevel=N depth=D weights=<file> [seed=S] [threads=T]   (or: rocks list=true)
//  Scores a rock for balancing: at each level, perLevel fights (half of them with the sides swapped, same dice)
//  between a fighter with that rock and 0 to 3 other random rocks and one with 1 to 4 random rocks without it, both
//  at that level, with 0 to 2 random trinkets, no potions, the same AI on both sides. Prints
//  "RESULT <level> <points> <games>" after each level (points: the rock's side's wins, draws half). list=true prints
//  "ROCK <name>" for every rock that can be scored. tools/ai/score_rocks.py runs it rock by rock.
//
//The data file, little-endian: the int header MAGIC, S, R, F, ROCKS, MOVES, ITEMS, then fight after fight:
//  int positions, float result (side 0: 1 won, 0 lost, 0.5 draw)
//  static, for side 0 then 1: per rock slot and move (ROCKS x MOVES rows) and per item slot (ITEMS rows): present, then
//    S floats (valueNet.moveStatic / itemStatic; zeros when not present)
//  per position: int next (who acts), int ender (who moves last in the round), float search (the value of the
//    position by the search of the net that acts, for side 0; NaN when the calculated judge acts), then for side 0 and 1:
//    ROCKS x MOVES rows of R floats (valueNet.moveNow), ITEMS rows of R (valueNet.itemNow), per rock present, has
//    uses, held (3 floats each), per item 1 if it is there with some left, and F floats (valueNet.sideStats)
public class aiTrain {

    public static final int MAGIC = 0x47475632, MAX_ROUNDS = 80;

    public static void main (String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("aiTrain selfplay|eval|check key=value ...");
            return;
        }
        Map<String, String> opt = new HashMap<String, String>();
        for (int i = 1; i < args.length; i++) {
            int eq = args[i].indexOf('=');
            if (eq > 0) opt.put(args[i].substring(0, eq), args[i].substring(eq + 1));
        }
        catalogue cat = new catalogue();
        switch (args[0]) {
            case "selfplay": selfplay(cat, opt); break;
            case "eval": eval(cat, opt); break;
            case "check": check(cat, opt); break;
            case "rocks": rocks(cat, opt); break;
            default: System.out.println("Unknown command " + args[0]);
        }
        System.exit(0);
    }

    private static int intOf (Map<String, String> opt, String key, int fallback) {return opt.containsKey(key) ? Integer.parseInt(opt.get(key)) : fallback;}
    private static double doubleOf (Map<String, String> opt, String key, double fallback) {return opt.containsKey(key) ? Double.parseDouble(opt.get(key)) : fallback;}

    private static battleSim.judge judgeOf (Map<String, String> opt) throws IOException {
        String weights = opt.get("weights");
        if (weights == null || weights.isEmpty() || weights.equals("none")) return battleSim.CALCULATED;
        return valueNet.load(new File(weights));
    }

    //---- What fighters can carry ----

    static class catalogue {
        final gemstones gems = new gemstones(null);
        final items things = new items(null);
        final List<Supplier<item>> rocks = new ArrayList<Supplier<item>>(), trinkets = new ArrayList<Supplier<item>>(), potions = new ArrayList<Supplier<item>>();

        catalogue () {
            //Every public method of the rock and item lists that makes an item (as itemRegistry finds them), but one by
            //one: some items need a running game to be made, and are left out
            List<Supplier<item>> makers = new ArrayList<Supplier<item>>();
            for (Object list : new Object[] {gems, things}) {
                for (java.lang.reflect.Method m : list.getClass().getMethods()) {
                    if (m.getParameterCount() != 0 || !item.class.isAssignableFrom(m.getReturnType())) continue;
                    makers.add(() -> {
                        try {
                            return (item) m.invoke(list);
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
            for (Supplier<item> maker : makers) {
                item made;
                try {
                    made = maker.get();
                } catch (RuntimeException e) {
                    continue;
                }
                if (made instanceof elementalRock) continue;
                if (made instanceof gemstone) rocks.add(maker);
                else if (made instanceof trinket) trinkets.add(maker);
                else if (made.getEffects() != null && made.isConsumable() && made.isBattleItem()) potions.add(maker);
            }
        }

        //A fighter at level with a random loadout: 1 to 4 rocks (now and then an Elemental Rock with one or two rocks
        //set in it), up to 2 trinkets worn, up to 4 kinds of potions
        character fighter (String name, int level, Random r) {
            character c = new character(name, 1, 1, 0, 0, 20, 20, 0, null, 3);
            c.setLevel(level);
            int rockCount = 1 + r.nextInt(4);
            for (int k = 0; k < rockCount; k++) {
                if (r.nextDouble() < 0.1) {
                    elementalRock e = new elementalRock(null);
                    List<Integer> slots = new ArrayList<Integer>(List.of(0, 1, 2));
                    java.util.Collections.shuffle(slots, r);
                    int inside = 1 + r.nextInt(elementalRock.MAX_ROCKS);
                    for (int n = 0; n < inside; n++) e.slots().set(slots.get(n), new itemStack(rocks.get(r.nextInt(rocks.size())).get()));
                    c.give(e);
                } else {
                    c.give(rocks.get(r.nextInt(rocks.size())).get());
                }
            }
            int worn = trinkets.isEmpty() ? 0 : r.nextInt(3);
            for (int k = 0; k < worn; k++) c.getInventory().set(inventory.slotKind.TRINKET, k, new itemStack(trinkets.get(r.nextInt(trinkets.size())).get()));
            if (!potions.isEmpty() && r.nextDouble() < 0.6) {
                int kinds = 1 + r.nextInt(inventory.ACTIVE);
                for (int k = 0; k < kinds; k++) {
                    Supplier<item> potion = potions.get(r.nextInt(potions.size()));
                    int count = 1 + r.nextInt(3);
                    for (int n = 0; n < count; n++) c.give(potion.get());
                }
            }
            //Which rock is held: any of them
            List<gemstone> held = battleSim.rockOrder(c);
            if (!held.isEmpty()) c.equipGemstone(held.get(r.nextInt(held.size())));
            c.resetHealth();
            return c;
        }

        //The rocks' names, in the catalogue's order
        List<String> rockNames () {
            List<String> names = new ArrayList<String>();
            for (Supplier<item> maker : rocks) names.add(maker.get().getName());
            return names;
        }

        //A fighter at level with exactly these rocks (the held one picked at random) and 0 to 2 random trinkets
        character withRocks (String name, int level, List<Supplier<item>> carried, Random r) {
            character c = new character(name, 1, 1, 0, 0, 20, 20, 0, null, 3);
            c.setLevel(level);
            for (Supplier<item> rock : carried) c.give(rock.get());
            int worn = trinkets.isEmpty() ? 0 : r.nextInt(3);
            for (int k = 0; k < worn; k++) c.getInventory().set(inventory.slotKind.TRINKET, k, new itemStack(trinkets.get(r.nextInt(trinkets.size())).get()));
            List<gemstone> held = battleSim.rockOrder(c);
            if (!held.isEmpty()) c.equipGemstone(held.get(r.nextInt(held.size())));
            c.resetHealth();
            return c;
        }

        //count different random rocks, none of them the rock at index without (-1: any)
        List<Supplier<item>> randomRocks (int count, int without, Random r) {
            List<Integer> pool = new ArrayList<Integer>();
            for (int k = 0; k < rocks.size(); k++) if (k != without) pool.add(k);
            java.util.Collections.shuffle(pool, r);
            List<Supplier<item>> out = new ArrayList<Supplier<item>>();
            for (int k = 0; k < count && k < pool.size(); k++) out.add(rocks.get(pool.get(k)));
            return out;
        }

        //Two fighters close in level (each 3 to 40, at most 6 apart), as side 0 and side 1
        battleSim fight (Random r) {
            int level = 3 + r.nextInt(38);
            int other = Math.max(3, Math.min(40, level + r.nextInt(13) - 6));
            return battleSim.of(fighter("A", level, r), fighter("B", other, r));
        }
    }

    //---- Made-up rocks ----
    //The net never sees a rock's name, only what its moves do. With only the game's rocks it would see only their
    //combinations of numbers, and could learn shortcuts that hold for them alone. So in self-play some rocks are
    //changed a little and some are made up from scratch, within the ranges the game's rocks use
    static class madeUp {
        //Each rock and potion of both sides: varied, invented or left as it is
        static void apply (battleSim s, double varied, double invented, Random r) {
            for (int o = 0; o < 2; o++) {
                battleSim.side me = s.sides[o], them = s.sides[1 - o];
                for (battleSim.rock rock : me.rocks) {
                    double pick = r.nextDouble();
                    if (pick < invented) {
                        int n = 1 + r.nextInt(3);
                        if (n < 3 && r.nextDouble() < 0.6) n = 3;
                        for (int k = 0; k < 3; k++) rock.moves[k] = k < n ? invent(me, them, r) : null;
                    } else if (pick < invented + varied) {
                        for (battleSim.move m : rock.moves) if (m != null) vary(m, r);
                    }
                }
                List<moveEffects.outcome> items = new ArrayList<moveEffects.outcome>(me.items);
                for (int k = 0; k < items.size(); k++) {
                    double pick = r.nextDouble();
                    if (pick < invented) items.set(k, inventItem(me, r));
                    else if (pick < invented + varied) items.set(k, varyOutcome(items.get(k), r));
                }
                me.items = items;
            }
        }

        static double between (Random r, double a, double b) {return a + (b - a)*r.nextDouble();}
        static int turns (Random r, int a, int b) {return a + r.nextInt(b - a + 1);}

        //Every amount times 0.6 to 1.5, rounds and the hit chance a little off, max uses one more or less
        static void vary (battleSim.move m, Random r) {
            m.o = varyOutcome(m.o, r);
            m.chance = Math.max(0.2, Math.min(1, m.chance + between(r, -0.15, 0.15)));
            int max = Math.max(1, m.max + r.nextInt(3) - 1);
            m.uses = Math.max(0, Math.min(max, m.uses + (max - m.max)));
            m.max = max;
        }

        static moveEffects.outcome varyOutcome (moveEffects.outcome a, Random r) {
            moveEffects.outcome o = new moveEffects.outcome();
            o.damage = a.damage*between(r, 0.6, 1.5);
            o.selfDamage = a.selfDamage*between(r, 0.6, 1.5);
            o.heal = a.heal*between(r, 0.6, 1.5);
            o.healShare = Math.min(1, a.healShare*between(r, 0.6, 1.5));
            o.burn = a.burn*between(r, 0.6, 1.5);
            o.poison = a.poison*between(r, 0.6, 1.5);
            o.selfPoison = a.selfPoison*between(r, 0.6, 1.5);
            o.healing = a.healing*between(r, 0.6, 1.5);
            o.confusion = Math.min(0.9, a.confusion*between(r, 0.7, 1.3));
            o.burnTurns = a.burnTurns > 0 ? Math.max(1, a.burnTurns + r.nextInt(3) - 1) : 0;
            o.poisonTurns = a.poisonTurns > 0 ? Math.max(1, a.poisonTurns + r.nextInt(3) - 1) : 0;
            o.selfPoisonTurns = a.selfPoisonTurns > 0 ? Math.max(1, a.selfPoisonTurns + r.nextInt(3) - 1) : 0;
            o.healingTurns = a.healingTurns > 0 ? Math.max(1, a.healingTurns + r.nextInt(3) - 1) : 0;
            o.confusionTurns = a.confusionTurns > 0 ? Math.max(1, a.confusionTurns + r.nextInt(3) - 1) : 0;
            o.cleanse = a.cleanse;
            o.restore = a.restore;
            o.reflect = a.reflect;
            return o;
        }

        //A move from scratch: mostly an attack, sometimes an effect or a support move, with extras now and then.
        //Strong moves get fewer uses and a lower hit chance, like the game's
        static battleSim.move invent (battleSim.side me, battleSim.side them, Random r) {
            moveEffects.outcome o = new moveEffects.outcome();
            double kind = r.nextDouble(), power;
            if (kind < 0.6) {
                power = between(r, 0.05, 0.45);
                o.damage = power*them.max;
            } else if (kind < 0.85) {
                power = between(r, 0.05, 0.3);
                double effect = r.nextDouble();
                if (effect < 0.4) {o.burn = between(r, 0.02, 0.08)*them.max; o.burnTurns = turns(r, 2, 6);}
                else if (effect < 0.8) {o.poison = between(r, 0.02, 0.08)*them.max; o.poisonTurns = turns(r, 3, 9);}
                else {o.confusion = between(r, 0.2, 0.7); o.confusionTurns = turns(r, 1, 3);}
                if (r.nextDouble() < 0.5) o.damage = between(r, 0, 0.12)*them.max;
            } else {
                power = between(r, 0.1, 0.35);
                double effect = r.nextDouble();
                if (effect < 0.4) o.heal = power*me.max;
                else if (effect < 0.55) o.healShare = between(r, 0.3, 0.6);
                else if (effect < 0.8) {o.healing = between(r, 0.02, 0.07)*me.max; o.healingTurns = turns(r, 3, 6);}
                else if (effect < 0.9) o.cleanse = true;
                else if (effect < 0.95) o.restore = true;
                else o.reflect = true;
            }
            //Extras now and then
            if (r.nextDouble() < 0.15 && o.burn == 0) {o.burn = between(r, 0.02, 0.06)*them.max; o.burnTurns = turns(r, 2, 5);}
            if (r.nextDouble() < 0.15 && o.poison == 0) {o.poison = between(r, 0.02, 0.06)*them.max; o.poisonTurns = turns(r, 3, 8);}
            if (r.nextDouble() < 0.08 && o.confusion == 0) {o.confusion = between(r, 0.2, 0.5); o.confusionTurns = turns(r, 1, 3);}
            if (r.nextDouble() < 0.1) o.selfDamage = between(r, 0.05, 0.2)*me.max;
            if (r.nextDouble() < 0.1 && o.heal == 0) o.heal = between(r, 0.03, 0.2)*me.max;
            if (r.nextDouble() < 0.05) {o.selfPoison = between(r, 0.02, 0.05)*me.max; o.selfPoisonTurns = turns(r, 2, 5);}

            battleSim.move m = new battleSim.move();
            m.o = o;
            m.chance = Math.max(0.35, Math.min(1, between(r, 1.05, 1.15) - power*between(r, 0.5, 1.4)));
            m.max = Math.max(1, Math.min(8, (int) Math.round(between(r, 0.5, 1.5)*(1 + (0.45 - power)/0.45*5))));
            m.uses = m.max;
            return m;
        }

        //A potion from scratch: a heal, a share of the health, healing over time or a cleanse
        static moveEffects.outcome inventItem (battleSim.side me, Random r) {
            moveEffects.outcome o = new moveEffects.outcome();
            double kind = r.nextDouble();
            if (kind < 0.5) o.heal = between(r, 0.08, 0.4)*me.max;
            else if (kind < 0.65) o.healShare = between(r, 0.2, 0.6);
            else if (kind < 0.85) {o.healing = between(r, 0.02, 0.08)*me.max; o.healingTurns = turns(r, 2, 6);}
            else o.cleanse = true;
            if (r.nextDouble() < 0.15 && !o.cleanse) o.cleanse = true;
            return o;
        }
    }

    //A start in the middle of a fight: hurt, uses used, effects on
    static void randomStart (battleSim s, Random r) {
        for (battleSim.side side : s.sides) {
            side.hp = side.max*(0.15 + 0.85*r.nextDouble());
            for (battleSim.rock rock : side.rocks) for (battleSim.move m : rock.moves) if (m != null && r.nextDouble() < 0.5) m.uses = r.nextInt(m.max + 1);
            if (r.nextDouble() < 0.25) {side.fx[0] = side.max*(0.02 + 0.08*r.nextDouble()); side.fx[1] = 1 + r.nextInt(8);}
            if (r.nextDouble() < 0.25) {side.fx[2] = side.max*(0.02 + 0.08*r.nextDouble()); side.fx[3] = 1 + r.nextInt(10);}
            if (r.nextDouble() < 0.15) {side.fx[4] = side.max*(0.02 + 0.06*r.nextDouble()); side.fx[5] = 1 + r.nextInt(6);}
            if (r.nextDouble() < 0.15) {side.fx[6] = 0.2 + 0.5*r.nextDouble(); side.fx[7] = 1 + r.nextInt(3);}
            for (int k = 0; k < side.counts.size(); k++) if (r.nextDouble() < 0.3) side.counts.set(k, r.nextInt(side.counts.get(k) + 1));
        }
    }

    //---- Writing positions ----

    static class writer {
        final ByteBuffer buf;
        writer (int floats) {buf = ByteBuffer.allocate(4*floats).order(ByteOrder.LITTLE_ENDIAN);}
        void f (double v) {buf.putFloat((float) v);}
        void i (int v) {buf.putInt(v);}
        void all (double[] x) {for (double v : x) f(v);}
    }

    static final int STATIC_FLOATS = 2*(valueNet.ROCKS*valueNet.MOVES + valueNet.ITEMS)*(1 + valueNet.S);
    static final int POSITION_FLOATS = 3 + 2*((valueNet.ROCKS*valueNet.MOVES + valueNet.ITEMS)*valueNet.R + valueNet.ROCKS*3 + valueNet.ITEMS + valueNet.F);

    static void writeStatic (writer w, battleSim s) {
        for (int o = 0; o < 2; o++) {
            battleSim.side me = s.sides[o], them = s.sides[1 - o];
            for (int k = 0; k < valueNet.ROCKS; k++) {
                for (int n = 0; n < valueNet.MOVES; n++) {
                    battleSim.move m = k < me.rocks.size() ? me.rocks.get(k).moves[n] : null;
                    w.f(m != null ? 1 : 0);
                    w.all(m != null ? valueNet.moveStatic(m, me, them) : new double[valueNet.S]);
                }
            }
            for (int k = 0; k < valueNet.ITEMS; k++) {
                boolean there = k < me.items.size();
                w.f(there ? 1 : 0);
                w.all(there ? valueNet.itemStatic(me.items.get(k), me, them) : new double[valueNet.S]);
            }
        }
    }

    static void writePosition (writer w, battleSim s, int next, int ender, double search) {
        w.i(next);
        w.i(ender);
        w.f(search);
        for (int o = 0; o < 2; o++) {
            battleSim.side me = s.sides[o], them = s.sides[1 - o];
            for (int k = 0; k < valueNet.ROCKS; k++) {
                for (int n = 0; n < valueNet.MOVES; n++) {
                    battleSim.move m = k < me.rocks.size() ? me.rocks.get(k).moves[n] : null;
                    w.all(m != null ? valueNet.moveNow(m, k == me.equipped, me, them) : new double[valueNet.R]);
                }
            }
            for (int k = 0; k < valueNet.ITEMS; k++) {
                boolean there = k < me.items.size() && me.counts.get(k) > 0;
                w.all(there ? valueNet.itemNow(me.items.get(k), me.counts.get(k), me, them) : new double[valueNet.R]);
            }
            for (int k = 0; k < valueNet.ROCKS; k++) {
                boolean there = k < me.rocks.size();
                w.f(there ? 1 : 0);
                w.f(there && me.rocks.get(k).hasMoves() ? 1 : 0);
                w.f(there && k == me.equipped ? 1 : 0);
            }
            for (int k = 0; k < valueNet.ITEMS; k++) w.f(k < me.items.size() && me.counts.get(k) > 0 ? 1 : 0);
            w.all(valueNet.sideStats(me, them));
        }
    }

    //---- Whole fights ----

    static class played {
        byte[] bytes;
        int positions, decisions;
        long thinkNanos;
        double result;
        List<double[]> checkValues;
    }

    //One fight between two policies (side 0 moves first in each round, side 1 last). Null when it reached MAX_ROUNDS.
    //The thinking time is counted for timed's decisions (null: everyone's). The search's value is recorded where
    //searcher acts (the net being trained; older nets and the calculated judge get NaN)
    static played play (battleSim start, aiStrength[] who, double epsilon, Random r, boolean record, valueNet checkNet, aiStrength timed) {
        return play(start, who, epsilon, r, record, checkNet, timed, null);
    }

    static played play (battleSim start, aiStrength[] who, double epsilon, Random r, boolean record, valueNet checkNet, aiStrength timed, aiStrength searcher) {
        battleSim s = start;
        s.coin = r;
        s.doubleKo = 0.5;
        List<writer> positions = new ArrayList<writer>();
        List<double[]> checks = checkNet != null ? new ArrayList<double[]>() : null;
        played p = new played();
        double result = -1;
        outer:
        for (int round = 0; round < MAX_ROUNDS; round++) {
            for (int i = 0; i < 2; i++) {
                List<battleSim.action> options = s.actions(i);
                //A net looks ahead at every turn, even when it then acts at random or has one choice: the best of its
                //values is the search's value of this position, a target for training (train.py)
                boolean byNet = searcher != null && who[i] == searcher;
                double[] values = null;
                long t = System.nanoTime();
                if (byNet || options.size() > 1) values = s.values(i, 1, who[i]);
                if (options.size() > 1 && (timed == null || who[i] == timed)) {
                    p.thinkNanos += System.nanoTime() - t;
                    p.decisions++;
                }
                double search = Double.NaN;
                if (byNet && values != null) {
                    double best = 0;
                    for (double v : values) best = Math.max(best, v);
                    search = i == 0 ? best : 1 - best;
                }
                if (record) {
                    writer w = new writer(POSITION_FLOATS);
                    writePosition(w, s, i, 1, search);
                    positions.add(w);
                }
                if (checks != null) checks.add(new double[] {checkNet.value(s, 0, i, 1), checkNet.value(s, 1, i, 1)});
                battleSim.action a;
                if (options.size() == 1) a = options.get(0);
                else if (r.nextDouble() < epsilon) a = options.get(r.nextInt(options.size()));
                else a = options.get(battleAI.pick(values, r));
                s.apply(i, a, r.nextDouble() < s.chance(i, a));
                result = s.result(0);
                if (result >= 0) break outer;
            }
            s.tick();
            result = s.result(0);
            if (result >= 0) break;
        }
        if (result < 0) return null;
        p.result = result;
        p.positions = positions.size();
        p.checkValues = checks;
        if (record) {
            writer head = new writer(2 + STATIC_FLOATS);
            head.i(positions.size());
            head.f(result);
            writeStatic(head, s);
            ByteBuffer all = ByteBuffer.allocate(head.buf.position() + positions.size()*4*POSITION_FLOATS);
            all.put(head.buf.array(), 0, head.buf.position());
            for (writer w : positions) all.put(w.buf.array(), 0, w.buf.position());
            p.bytes = all.array();
        }
        return p;
    }

    private static void writeHeader (OutputStream out) throws IOException {
        writer w = new writer(7);
        w.i(MAGIC);
        w.i(valueNet.S);
        w.i(valueNet.R);
        w.i(valueNet.F);
        w.i(valueNet.ROCKS);
        w.i(valueNet.MOVES);
        w.i(valueNet.ITEMS);
        out.write(w.buf.array());
    }

    private static int threadsOf (Map<String, String> opt) {
        return intOf(opt, "threads", Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    static void selfplay (catalogue cat, Map<String, String> opt) throws Exception {
        int fights = intOf(opt, "fights", 1000), depth = intOf(opt, "depth", 2), threads = threadsOf(opt);
        double epsilon = doubleOf(opt, "epsilon", 0.1), randomStarts = doubleOf(opt, "randomStart", 0.5);
        double varied = doubleOf(opt, "varied", 0), invented = doubleOf(opt, "invented", 0);
        long seed = Long.parseLong(opt.getOrDefault("seed", String.valueOf(System.nanoTime())));
        battleSim.judge judge = judgeOf(opt);
        aiStrength strength = aiStrength.with(judge, depth), calc = aiStrength.with(battleSim.CALCULATED, depth);
        boolean mixed = judge != battleSim.CALCULATED;
        double calcShare = mixed ? doubleOf(opt, "calcShare", 0) : 0, oldShare = mixed ? doubleOf(opt, "oldShare", 0) : 0;
        List<aiStrength> olds = new ArrayList<aiStrength>();
        if (mixed && opt.containsKey("old") && !opt.get("old").isEmpty()) {
            for (String path : opt.get("old").split(",")) olds.add(aiStrength.with(valueNet.load(new File(path)), depth));
        }
        AtomicInteger[] kinds = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};
        File out = new File(opt.get("out"));
        long started = System.nanoTime();
        AtomicInteger done = new AtomicInteger(), dropped = new AtomicInteger(), positions = new AtomicInteger();
        long[] think = new long[2];
        try (OutputStream file = new BufferedOutputStream(new FileOutputStream(out))) {
            writeHeader(file);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            List<Future<?>> jobs = new ArrayList<Future<?>>();
            for (int t = 0; t < threads; t++) {
                final int share = fights/threads + (t < fights % threads ? 1 : 0);
                final long mySeed = seed*1000003L + t;
                jobs.add(pool.submit(() -> {
                    Random r = rng(mySeed);
                    for (int f = 0; f < share; f++) {
                        battleSim s = cat.fight(r);
                        madeUp.apply(s, varied, invented, r);
                        if (r.nextDouble() < randomStarts) randomStart(s, r);
                        //The opponent: itself, the calculated judge or an older net
                        aiStrength other = strength;
                        double pick = r.nextDouble();
                        int kind = 0;
                        if (pick < calcShare) {other = calc; kind = 1;}
                        else if (pick < calcShare + oldShare && !olds.isEmpty()) {other = olds.get(r.nextInt(olds.size())); kind = 2;}
                        kinds[kind].incrementAndGet();
                        aiStrength[] who = r.nextBoolean() ? new aiStrength[] {strength, other} : new aiStrength[] {other, strength};
                        played p = play(s, who, epsilon, r, true, null, null, judge instanceof valueNet ? strength : null);
                        if (p == null) {dropped.incrementAndGet(); continue;}
                        synchronized (file) {
                            file.write(p.bytes);
                            think[0] += p.thinkNanos;
                            think[1] += p.decisions;
                        }
                        positions.addAndGet(p.positions);
                        int n = done.incrementAndGet();
                        if (n % 200 == 0) System.out.println("PROGRESS " + n + " " + fights);
                    }
                    return null;
                }));
            }
            for (Future<?> job : jobs) job.get();
            pool.shutdown();
        }
        System.out.printf(java.util.Locale.ENGLISH, "OPPONENTS self %d calculated %d older %d%n", kinds[0].get(), kinds[1].get(), kinds[2].get());
        System.out.printf(java.util.Locale.ENGLISH, "DONE fights %d dropped %d positions %d seconds %.1f msPerDecision %.2f%n",
            done.get(), dropped.get(), positions.get(), (System.nanoTime() - started)/1e9, think[1] == 0 ? 0 : think[0]/1e6/think[1]);
    }

    static void eval (catalogue cat, Map<String, String> opt) throws Exception {
        int fights = intOf(opt, "fights", 200), depth = intOf(opt, "depth", 2), opponentDepth = intOf(opt, "opponentDepth", 3), threads = threadsOf(opt);
        long seed = Long.parseLong(opt.getOrDefault("seed", "12345"));
        aiStrength net = aiStrength.with(judgeOf(opt), depth), calc = aiStrength.with(battleSim.CALCULATED, opponentDepth);
        long started = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<double[]>> jobs = new ArrayList<Future<double[]>>();
        int pairs = Math.max(1, fights/2);
        for (int k = 0; k < pairs; k++) {
            final long mySeed = seed*7919L + k;
            jobs.add(pool.submit(() -> {
                //The same loadouts, the net on each side once; the same seed for the dice both times
                battleSim s = cat.fight(rng(mySeed));
                double points = 0, games = 0, netThink = 0, netDecisions = 0;
                for (int side = 0; side < 2; side++) {
                    aiStrength[] who = side == 0 ? new aiStrength[] {net, calc} : new aiStrength[] {calc, net};
                    battleSim start = s.copy();
                    played p = play(start, who, 0, rng(~mySeed), false, null, net);
                    if (p == null) {points += 0.5; games++; continue;}
                    points += side == 0 ? p.result : 1 - p.result;
                    games++;
                    netThink += p.thinkNanos;
                    netDecisions += p.decisions;
                }
                return new double[] {points, games, netThink, netDecisions};
            }));
        }
        double points = 0, games = 0, think = 0, decisions = 0;
        for (Future<double[]> job : jobs) {
            double[] v = job.get();
            points += v[0];
            games += v[1];
            think += v[2];
            decisions += v[3];
        }
        pool.shutdown();
        System.out.printf(java.util.Locale.ENGLISH, "winrate %.4f games %d seconds %.1f msPerDecision %.2f%n",
            points/games, (int) games, (System.nanoTime() - started)/1e9, decisions == 0 ? 0 : think/1e6/decisions);
    }

    //A Random for seed, scrambled so fights with seeds next to each other differ (seeded)
    static Random rng (long seed) {return seeded.random(seed);}

    static void rocks (catalogue cat, Map<String, String> opt) throws Exception {
        List<String> names = cat.rockNames();
        if (opt.containsKey("list")) {
            for (String name : names) System.out.println("ROCK " + name);
            return;
        }
        //show=true levels=...: every rock's moves worked out at each level against a fighter of that level, no
        //trinkets ("MOVE rock level n uses chance damage selfDamage heal healShare burn burnTurns poison poisonTurns
        //selfPoison healing healingTurns confusion confusionTurns"), to check the rocks against a model of them
        if (opt.containsKey("show")) {
            for (String levelText : opt.getOrDefault("levels", "0,10,25").split(",")) {
                int level = Integer.parseInt(levelText.trim());
                for (int k = 0; k < names.size(); k++) {
                    character a = new character("A", 1, 1, 0, 0, 20, 20, 0, null, 3), b = new character("B", 1, 1, 0, 0, 20, 20, 0, null, 3);
                    a.setLevel(level);
                    b.setLevel(level);
                    a.resetHealth();
                    b.resetHealth();
                    gemstone g = (gemstone) cat.rocks.get(k).get();
                    for (int n = 0; n < 3; n++) {
                        moveEffects.outcome o = g.effectsOf(n).outcome(a, b);
                        List<Integer> uses = g.usesOf(n, a);
                        System.out.println("MOVE\t" + names.get(k) + "\t" + level + "\t" + n + "\t" + uses.get(1) + "\t" + g.baseChance(n, a)
                            + "\t" + o.damage + "\t" + o.selfDamage + "\t" + o.heal + "\t" + o.healShare + "\t" + o.burn + "\t" + o.burnTurns
                            + "\t" + o.poison + "\t" + o.poisonTurns + "\t" + o.selfPoison + "\t" + o.healing + "\t" + o.healingTurns
                            + "\t" + o.confusion + "\t" + o.confusionTurns);
                    }
                }
            }
            return;
        }
        int index = names.indexOf(opt.get("rock"));
        if (index < 0) throw new IllegalArgumentException("No rock called " + opt.get("rock"));
        int perLevel = intOf(opt, "perLevel", 400), depth = intOf(opt, "depth", 2), threads = threadsOf(opt);
        long seed = Long.parseLong(opt.getOrDefault("seed", "4242"));
        aiStrength ai = aiStrength.with(judgeOf(opt), depth);
        Supplier<item> scored = cat.rocks.get(index);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (String levelText : opt.get("levels").split(",")) {
            int level = Integer.parseInt(levelText.trim());
            List<Future<double[]>> jobs = new ArrayList<Future<double[]>>();
            for (int k = 0; k < Math.max(1, perLevel/2); k++) {
                final long mySeed = (seed*31 + index)*1000003L + level*7919L + k;
                jobs.add(pool.submit(() -> {
                    Random r = rng(mySeed);
                    //The rock's side: it and 0 to 3 others; the other side: 1 to 4 rocks, never this one
                    List<Supplier<item>> mine = new ArrayList<Supplier<item>>();
                    mine.add(scored);
                    mine.addAll(cat.randomRocks(r.nextInt(4), index, r));
                    List<Supplier<item>> theirs = cat.randomRocks(1 + r.nextInt(4), index, r);
                    character a = cat.withRocks("A", level, mine, r), b = cat.withRocks("B", level, theirs, r);
                    double points = 0, games = 0;
                    long dice = r.nextLong();
                    for (int side = 0; side < 2; side++) {
                        battleSim s = side == 0 ? battleSim.of(a, b) : battleSim.of(b, a);
                        played p = play(s, new aiStrength[] {ai, ai}, 0, new Random(dice), false, null, null);
                        double result = p == null ? 0.5 : p.result;
                        points += side == 0 ? result : 1 - result;
                        games++;
                    }
                    return new double[] {points, games};
                }));
            }
            double points = 0, games = 0;
            for (Future<double[]> job : jobs) {
                double[] v = job.get();
                points += v[0];
                games += v[1];
            }
            System.out.println("RESULT " + level + " " + points + " " + (int) games);
            System.out.flush();
        }
        pool.shutdown();
    }

    static void check (catalogue cat, Map<String, String> opt) throws Exception {
        int fights = intOf(opt, "fights", 5);
        valueNet net = (valueNet) judgeOf(opt);
        Random r = new Random(99);
        aiStrength calc = aiStrength.with(battleSim.CALCULATED, 1);
        try (OutputStream file = new BufferedOutputStream(new FileOutputStream(new File(opt.get("out"))));
             PrintWriter values = new PrintWriter(new File(opt.get("values")), "UTF-8")) {
            writeHeader(file);
            int written = 0;
            while (written < fights) {
                battleSim s = cat.fight(r);
                randomStart(s, r);
                played p = play(s, new aiStrength[] {calc, calc}, 0.3, r, true, net, null);
                if (p == null) continue;
                file.write(p.bytes);
                for (double[] v : p.checkValues) values.println(v[0] + " " + v[1]);
                written++;
            }
        }
        System.out.println("DONE");
    }
}
