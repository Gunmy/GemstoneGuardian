package game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//The end of a fight, as a message: it waits in the queue and is gone with space like any other, so what comes after
//it (the event the win triggers, "Restoring winner...") follows as before. But it is not drawn in the speech box:
//guiOutcome covers the screen with it, and while it is up the world plays along (playAnimations, gameHandler):
//- WIN: the screen flashes white, VICTORY slams down and shakes the camera, the loser is knocked back (and stays),
//  the rock that won is shown big and glowing, the xp counts up into the level bar (LEVEL UP when it fills) and the
//  drops pop out one by one
//- LOSE: the camera closes in on the player who falls over, DEFEATED fades in slowly
//- DUEL (a 1v1, either side): VICTORY slams down (or DEFEATED fades in, for the one who lost) with the winner's portrait and name, the loser's greyed out, both
//  builds side by side. The loser falls over
//All show the fight in numbers (stats). Space is ignored for HOLD seconds so mashing through the last hit does not
//skip it; pressed while the screen is still playing out, it skips to the end of it, and only then goes on
public class fightOutcome extends dialogue {

    public enum kind {WIN, LOSE, DUEL}

    //A fight in numbers, counted by battle as it goes. Damage is what each did to the other, effects included
    public static class stats {
        public int turns;
        public double yourDamage, yourBest, theirDamage, theirBest;

        public void yourHit (double damage, boolean counts) {
            if (damage <= 0) return;
            yourDamage += damage;
            if (counts) yourBest = Math.max(yourBest, damage);
        }

        public void theirHit (double damage, boolean counts) {
            if (damage <= 0) return;
            theirDamage += damage;
            if (counts) theirBest = Math.max(theirBest, damage);
        }

        //The other side's view of the same fight (the 1v1 guest gets the host's)
        public stats swapped () {
            stats s = new stats();
            s.turns = turns;
            s.yourDamage = theirDamage;
            s.yourBest = theirBest;
            s.theirDamage = yourDamage;
            s.theirBest = yourBest;
            return s;
        }

        public String encode () {return turns + ";" + yourDamage + ";" + yourBest + ";" + theirDamage + ";" + theirBest;}

        public static stats decode (String field) {
            stats s = new stats();
            String[] f = field.split(";");
            try {
                s.turns = Integer.parseInt(f[0]);
                s.yourDamage = Double.parseDouble(f[1]);
                s.yourBest = Double.parseDouble(f[2]);
                s.theirDamage = Double.parseDouble(f[3]);
                s.theirBest = Double.parseDouble(f[4]);
            } catch (RuntimeException e) {
                //Shown as zeros
            }
            return s;
        }
    }

    //Timings, in seconds from when it comes on screen
    public static final double HOLD = 0.6;
    public static final double FLASH = 0.35, SLAM = 0.25, SHAKE = 0.4, KNOCK = 0.4;
    public static final double REVEAL = 0.45;
    public static final double XP_START = 0.8, XP_TIME = 1.2, DROP_GAP = 0.3;
    public static final double FALL_START = 0.15, FALL_TIME = 0.7;
    public static final double FADE_START = 0.5, FADE_TIME = 1.2;
    //How much closer the camera comes when you lose
    public static final double CLOSER = 0.3;

    public final kind type;
    //Seen from this game's player: you are the one at the bottom left of the fight
    public final character you, them;
    public final boolean youWon;
    public final stats numbers;

    //WIN only: the rock you won with, the xp before and given, and what the loser dropped (one entry per kind of item)
    public gemstone rock;
    public double xpBefore, xpGained;
    public final List<item> drops = new ArrayList<item>();
    public final List<Integer> dropCounts = new ArrayList<Integer>();

    private double skipped = 0;
    private Set<String> fired = new HashSet<String>();

    private fightOutcome (kind kind, character you, character them, boolean youWon, stats numbers) {
        super("Game", youWon ? you.getName() + " won" : them.getName() + " won");
        this.type = kind;
        this.you = you;
        this.them = them;
        this.youWon = youWon;
        this.numbers = numbers;
    }

    public static fightOutcome win (character you, character them, stats numbers, double xpBefore, double xpGained, List<item> dropped) {
        fightOutcome o = new fightOutcome(kind.WIN, you, them, true, numbers);
        o.rock = you.getGemstone();
        o.xpBefore = xpBefore;
        o.xpGained = xpGained;
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        Map<String, item> examples = new LinkedHashMap<String, item>();
        for (item drop : dropped) {
            counts.merge(drop.getName(), 1, Integer::sum);
            examples.putIfAbsent(drop.getName(), drop);
        }
        for (String name : counts.keySet()) {
            o.drops.add(examples.get(name));
            o.dropCounts.add(counts.get(name));
        }
        return o;
    }

    public static fightOutcome lose (character you, character them, stats numbers) {
        return new fightOutcome(kind.LOSE, you, them, false, numbers);
    }

    public static fightOutcome duel (character you, character them, boolean youWon, stats numbers) {
        return new fightOutcome(kind.DUEL, you, them, youWon, numbers);
    }

    //A random battle's streak: wins in a row before and after this fight (-1: not a random battle), and whether it
    //is a new best (duel.countStreak)
    public int streakBefore = -1, streakAfter = -1;
    public boolean newBest = false;
    //Cheat mode was on in this fight: the end screen shows CHEAT! instead of the streak (it did not count)
    public boolean cheated = false;
    public void streak (int before, int after, boolean best) {
        this.streakBefore = before;
        this.streakAfter = after;
        this.newBest = best;
    }

    public character winner () {return youWon ? you : them;}
    public character loser () {return youWon ? them : you;}

    //Seconds into the screen, skips included
    public double time () {return age() + skipped;}

    //When everything on it has played out
    public double done () {
        switch (type) {
            case WIN: return dropTime(drops.size()) + 0.3;
            case LOSE: return FADE_START + FADE_TIME;
            default:
                double shown = youWon ? REVEAL + 0.6 : FADE_START + FADE_TIME;
                //A random battle's streak is slapped on after the panel is in, and the buttons wait for it
                if (streakAfter >= 0) shown = Math.max(shown, streakAt() + STREAK_SLAM + (hasStreakNote() ? STREAK_NOTE + 0.25 : 0.2));
                return shown;
        }
    }

    //When a random battle's streak lands on the 1v1 panel (once the panel has come in: REVEAL plus its 0.35 for a
    //win, the fade for a loss), how long it takes to come down, and how much later the line under it pops in
    public static final double STREAK_SLAM = 0.18, STREAK_NOTE = 0.3;
    public double streakAt () {return (youWon ? REVEAL + 0.35 : FADE_START + FADE_TIME) + 0.2;}
    //NEW BEST! or the streak that was lost, under the number
    public boolean hasStreakNote () {return youWon ? newBest : streakBefore > 0;}

    //When the drop at index i pops out (after the xp has counted up)
    public double dropTime (int i) {return XP_START + XP_TIME + 0.2 + i*DROP_GAP;}

    public boolean finished () {return time() >= done();}

    //True the first time it is asked for that key: for bursts that happen once at a moment of the screen
    public boolean fire (String key) {return fired.add(key);}

    @Override
    public boolean letGo () {
        if (age() < HOLD) return false;
        if (!finished()) {
            skipped = done() - age();
            return false;
        }
        //With a choice (a random battle) it stays up until one of its buttons is clicked: space does nothing then
        if (hasChoice()) return false;
        settle();
        return true;
    }

    //What the fight sets off (the event after a win, the loser's restoring): run once the screen is gone, so nobody
    //walks off or talks while the last hit and the screen are still up. A save runs it at once (settle) so it is
    //never lost
    private Runnable afterwards;
    public fightOutcome afterwards (Runnable afterwards) {
        this.afterwards = afterwards;
        return this;
    }

    public boolean hasPending () {return afterwards != null;}

    public void settle () {
        Runnable run = afterwards;
        afterwards = null;
        if (run != null) run.run();
    }

    //A choice at the end (a random battle: again, or back to the menu), shown as buttons once it has played out
    private Runnable again, back;
    public void offer (Runnable again, Runnable back) {
        this.again = again;
        this.back = back;
    }
    public boolean hasChoice () {return again != null;}
    public void again () {leave(again);}
    public void back () {leave(back);}

    //Picking one first takes the screen away (it slides up, the grey and the zoom go back) in LEAVE_TIME seconds,
    //and only then does what was picked (the zoom out into space)
    public static final double LEAVE_TIME = 0.4;
    private long leftAt = 0;
    private Runnable leaveTo;

    private void leave (Runnable then) {
        if (leftAt != 0 || then == null) return;
        leftAt = System.nanoTime();
        leaveTo = then;
    }

    public boolean isLeaving () {return leftAt != 0;}

    //How far it has gone, 0 to 1
    public double leaving () {return leftAt == 0 ? 0 : Math.min(1, (System.nanoTime() - leftAt)/1e9/LEAVE_TIME);}

    //0 to 1, eased out
    public static double ease (double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }


    //The fight's zoom is multiplied by this: closer in on you when you lose
    public double zoom () {return youWon || isLeaving() ? 1 : 1 + CLOSER*ease(time()/(FALL_START + FALL_TIME + 0.4));}

    //Whether the banner slams down (and shakes) rather than fading in
    public boolean slams () {return youWon;}

    //The world's part, every frame between the nudges being cleared and the characters drawn
    @Override
    public void playAnimations (gameHandler handler, double deltaTime) {
        super.playAnimations(handler, deltaTime);
        double t = time(), tile = 1.0/handler.getTileCount();
        character loser = loser(), winner = winner();
        double away = Math.signum(loser.getX() - winner.getX());
        if (away == 0) away = 1;
        //The campaign's loser takes the last hit and stays where it is
        if (type == kind.WIN && t < KNOCK) {
            double u = t/KNOCK;
            loser.nudge(away*tile*0.3*Math.sin(Math.PI*u)*(1 - 0.5*u), 0);
        }
        //The player who lost falls over, away from the winner
        if (type != kind.WIN) loser.fallOver(away*ease((t - FALL_START)/FALL_TIME)*(1 - ease(leaving())));
        //Gone: now what was picked
        if (leaveTo != null && leaving() >= 1) {
            Runnable then = leaveTo;
            leaveTo = null;
            then.run();
        }
        //The banner landing
        if (slams() && t >= SLAM && t < SLAM + SHAKE) {
            double amp = tile*0.12*(1 - (t - SLAM)/SHAKE);
            java.util.Random rdm = handler.getGui().getRandom();
            handler.getCamera().shake((rdm.nextDouble()*2 - 1)*amp, (rdm.nextDouble()*2 - 1)*amp);
        }
    }
}
