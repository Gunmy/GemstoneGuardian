package game;

//The start of every fight: the two fighters, giant, each on their half of the screen (you on the left), rise up from
//below (RISE), stay a moment with their names and VS between them (HOLD), and slide out, the left one to the left and
//the right one to the right (LEAVE). The fight waits for it (battle.carryOn, the battle screen in gui.updateVisibility).
//guiBattleIntro draws it.
//A 1v1 starts it on the way out of the menu (gameHandler.duelWithEffect): there it stays up through the zoom into
//space and leaves as the fight's game zooms in (untilLanded). The fight's game is another gameHandler, so the intro is
//kept here, shared, and that game's fight puts its own fighters in it (forBattle)
public class battleIntro {

    public static final double RISE = 0.5, HOLD = 1.0, LEAVE = 0.5;
    //A 1v1's intro leaves by then at the latest, in case the fight's game never zooms in
    private static final double LONGEST_WAIT = 12;

    private static battleIntro current;

    private character left, right;
    private final long start = System.nanoTime();
    //Seconds after the start when it begins to leave; NaN while it waits for the zoom in from space
    private double leaveAt;

    private battleIntro (character left, character right, double leaveAt) {
        this.left = left;
        this.right = right;
        this.leaveAt = leaveAt;
    }

    //A 1v1 from the menu: up until the fight's game zooms in from space (landing)
    public static void untilLanded (character left, character right) {
        current = new battleIntro(left, right, Double.NaN);
    }

    //A fight starting: the 1v1 intro already up takes these fighters, anything else plays a new intro
    public static void forBattle (character you, character them) {
        if (current != null && current.waiting()) {
            current.left = you;
            current.right = them;
            return;
        }
        current = new battleIntro(you, them, RISE + HOLD);
    }

    //The intro going on, null when there is none
    public static battleIntro current () {
        if (current != null && current.time() >= current.end()) current = null;
        return current;
    }

    //True while an intro is on screen: the fight waits
    public static boolean playing () {return current() != null;}

    //The fight's game is zooming in from space (or is not in a trip at all): a waiting intro starts to leave
    public void landing () {
        if (waiting()) leaveAt = Math.max(RISE, time());
    }

    public boolean waiting () {return Double.isNaN(leaveAt) && time() < LONGEST_WAIT;}

    public character getLeft () {return this.left;}
    public character getRight () {return this.right;}

    public double time () {return (System.nanoTime() - start)/1e9;}

    private double leaveStart () {return Double.isNaN(leaveAt) ? LONGEST_WAIT : leaveAt;}
    private double end () {return leaveStart() + LEAVE;}

    //0 below the screen, 1 risen
    public double risen () {return ease(time()/RISE);}
    //0 still up, 1 gone out at the sides
    public double gone () {return ease((time() - leaveStart())/LEAVE);}

    private static double ease (double t) {
        t = Math.max(0, Math.min(1, t));
        return t*t*(3 - 2*t);
    }
}
