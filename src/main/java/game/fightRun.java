package game;

import java.util.ArrayList;
import java.util.List;

//A run of 1v1 fights against the computer, one after the other, the same build all the way: each stage is an opponent
//(name, look, build, how strong it plays) at a place. A loss ends the run, as does beating the last one. Each fight
//is a game of its own (duel), health and moves full again at the start of each. The daily battles are one (daily).
//While it goes it counts how far it got, the fights in numbers, and how good the player's actions were: each is
//graded by how much of its chance to win it gave up against the best action then (battle, grade). changed runs after
//anything was counted, so a run that is quit (or the game closed) is kept as far as it got.
//encode / decode: the whole run as one line, for the file
public class fightRun {

    public static class stage {
        public final String name, look, placeName;
        public final duelBuild build;
        //Null in a run read back from the file: only shown then, never fought
        public final duel.spot place;
        public final aiStrength strength;
        //How many levels the opponent is above the run's level
        public final int levelsAbove;

        public stage (String name, String look, duelBuild build, duel.spot place, aiStrength strength, int levelsAbove) {
            this.name = name;
            this.look = look;
            this.build = build;
            this.place = place;
            this.placeName = place == null ? "" : place.name;
            this.strength = strength;
            this.levelsAbove = levelsAbove;
        }

        private stage (String name, String look, String placeName, duelBuild build, aiStrength strength, int levelsAbove) {
            this.name = name;
            this.look = look;
            this.build = build;
            this.place = null;
            this.placeName = placeName;
            this.strength = strength;
            this.levelsAbove = levelsAbove;
        }
    }

    //How good an action was: how much of its chance to win it gave up against the best one (most: the share of
    //chance up to which it still counts as this)
    public enum grade {
        BRILLIANT("Brilliant", "#2FB8A8", 0.005), GREAT("Great", "#4C9F3F", 0.03), OK("OK", "#C8A030", 0.10),
        BAD("Bad", "#D07030", 0.25), TERRIBLE("Terrible", "#C03030", Double.POSITIVE_INFINITY);

        public final String label, color;
        public final double most;
        grade (String label, String color, double most) {
            this.label = label;
            this.color = color;
            this.most = most;
        }

        public static grade of (double loss) {
            for (grade g : values()) if (loss <= g.most) return g;
            return TERRIBLE;
        }
    }
    //Turns where the best and the worst action are closer than this are not graded (nothing to choose between)
    public static final double UNGRADED_SPREAD = 0.01;
    //A loss counts as at most this for the accuracy (one blunder does not wipe it out)
    private static final double ACCURACY_CAP = 0.25;

    public final List<stage> stages;
    public final duelBuild build;
    public final String myName, myLook;
    public final int level;

    //Fights won, and whether the run is over (a loss, or the last one beaten)
    private int beaten = 0;
    private boolean over = false;
    private final int[] grades = new int[grade.values().length];
    private double lossSum = 0;
    private final fightOutcome.stats total = new fightOutcome.stats();

    private Runnable changed = () -> {};

    public fightRun (List<stage> stages, duelBuild build, String myName, String myLook, int level) {
        this.stages = stages;
        this.build = build;
        this.myName = myName;
        this.myLook = myLook;
        this.level = level;
    }

    public fightRun onChange (Runnable changed) {
        this.changed = changed;
        return this;
    }

    public int beaten () {return this.beaten;}
    public boolean isOver () {return this.over;}
    public boolean allBeaten () {return beaten >= stages.size();}
    public int count (grade g) {return grades[g.ordinal()];}
    public int graded () {
        int n = 0;
        for (int g : grades) n += g;
        return n;
    }
    public fightOutcome.stats total () {return this.total;}

    //100 when every action was the best, down to 0 when every one gave up ACCURACY_CAP or more of the chance
    public double accuracy () {
        int n = graded();
        return n == 0 ? 0 : 100*(1 - lossSum/n/ACCURACY_CAP);
    }

    //The fight for the stage the run is at (the next one not beaten)
    public duel current () {
        stage s = stages.get(Math.min(beaten, stages.size() - 1));
        duel d = duel.againstComputer(myName, myLook, build, s.name, s.look, s.build, s.place, level);
        d.inRun(this, Math.min(beaten, stages.size() - 1), s.strength, s.levelsAbove);
        return d;
    }

    //One of the player's actions, loss being how much chance it gave up against the best one
    public void graded (double loss) {
        double lost = Math.max(0, loss);
        grades[grade.of(lost).ordinal()]++;
        lossSum += Math.min(lost, ACCURACY_CAP);
        changed.run();
    }

    public void fightOver (boolean won, fightOutcome.stats numbers) {
        if (over) return;
        if (won) beaten++;
        if (!won || allBeaten()) over = true;
        total.turns += numbers.turns;
        total.yourDamage += numbers.yourDamage;
        total.yourBest = Math.max(total.yourBest, numbers.yourBest);
        total.theirDamage += numbers.theirDamage;
        total.theirBest = Math.max(total.theirBest, numbers.theirBest);
        changed.run();
    }

    //---- On file ----

    //Fields joined by |: beaten, over, the grades (;), the loss sum, the numbers, the build, me, then a stage each
    //(name, look, place name, build, judge, depth, levels above)
    public String encode () {
        List<String> parts = new ArrayList<String>();
        parts.add(String.valueOf(beaten));
        parts.add(String.valueOf(over));
        List<String> g = new ArrayList<String>();
        for (int n : grades) g.add(String.valueOf(n));
        parts.add(String.join(";", g));
        parts.add(String.valueOf(lossSum));
        parts.add(total.encode());
        parts.add(duelBuild.enc(build.encode()));
        parts.add(duelBuild.enc(myName) + ";" + duelBuild.enc(myLook) + ";" + level);
        for (stage s : stages) {
            parts.add(duelBuild.enc(s.name) + ";" + duelBuild.enc(s.look) + ";" + duelBuild.enc(s.placeName) + ";"
                + duelBuild.enc(s.build.encode()) + ";" + s.strength.trained + ";" + s.strength.depth + ";" + s.levelsAbove);
        }
        return String.join("|", parts);
    }

    //Null if it can not be read
    public static fightRun decode (String line) {
        if (line == null || line.isEmpty()) return null;
        try {
            String[] p = line.split("\\|", -1);
            String[] me = p[6].split(";", -1);
            List<stage> stages = new ArrayList<stage>();
            for (int i = 7; i < p.length; i++) {
                String[] f = p[i].split(";", -1);
                stages.add(new stage(duelBuild.dec(f[0]), duelBuild.dec(f[1]), duelBuild.dec(f[2]), duelBuild.decode(duelBuild.dec(f[3])),
                    new aiStrength(Boolean.parseBoolean(f[4]), Integer.parseInt(f[5])), f.length > 6 ? Integer.parseInt(f[6]) : 0));
            }
            fightRun run = new fightRun(stages, duelBuild.decode(duelBuild.dec(p[5])), duelBuild.dec(me[0]), duelBuild.dec(me[1]), Integer.parseInt(me[2]));
            run.beaten = Integer.parseInt(p[0]);
            run.over = Boolean.parseBoolean(p[1]);
            String[] g = p[2].split(";");
            for (int i = 0; i < g.length && i < run.grades.length; i++) run.grades[i] = Integer.parseInt(g[i]);
            run.lossSum = Double.parseDouble(p[3]);
            fightOutcome.stats t = fightOutcome.stats.decode(p[4]);
            run.total.turns = t.turns;
            run.total.yourDamage = t.yourDamage;
            run.total.yourBest = t.yourBest;
            run.total.theirDamage = t.theirDamage;
            run.total.theirBest = t.theirBest;
            return run;
        } catch (RuntimeException e) {
            System.out.println("Kunne ikke lese dagens kamper: " + e);
            return null;
        }
    }
}
