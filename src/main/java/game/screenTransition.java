package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;

//Blacks out the screen tile by tile around the player, runs something while it is all black, and opens it again.
//The black closes in as a diamond (tiles go black by how many steps away from the player they are) and opens
//as a square (tiles come back by how many tiles away they are in the furthest direction). The black is a night sky:
//twinkling stars fixed to the screen, each shown only while the tile under it is black. Used for teleporting,
//and between the main menu and a game: the screen that is left closes and stays black, then hands over (start with
//an end), and the new game opens out of the black (startFromBlack). Each side has half the black time, and the stars
//are shared by every game, so the two halves look like one teleport
public class screenTransition {

    private enum phase {NONE, CLOSING, BLACK, OPENING}

    //How far a teleport zooms out: the world is drawn this many times its size at the darkest
    private static final double ZOOM_OUT = 0.55;

    //Seconds for each part
    private static final double CLOSE_TIME = 1.0, BLACK_TIME = 1.0, OPEN_TIME = 1.0;

    private gameHandler handler;
    private phase current = phase.NONE;
    private long phaseStart;
    private Runnable middle;
    //Run when the black part is over instead of opening. The screen then stays black, for whatever replaces it
    private Runnable end;
    private boolean holding = false;

    //Run once when this share (0 to 1) of the black part has gone, set with atBlackShare after starting
    private Runnable cue;
    private double cueShare;

    public void atBlackShare (double share, Runnable cue) {
        if (share < 0 || share > 1) throw new IllegalArgumentException("Share må være mellom 0 og 1");
        this.cueShare = share;
        this.cue = cue;
    }
    private double outZoom = ZOOM_OUT, sizeCarry = 1;

    //One star: where on the screen (0 to 1 across and down), how big, how fast it twinkles and where in the twinkle it starts
    private static class star {
        double u, v, size, speed, phase;
        boolean sparkle;
        Color color;
    }
    private static List<star> stars = new ArrayList<star>();
    private static double starsWidth = -1, starsHeight = -1;

    //How long this transition stays black. Half when it is only one side of a switch between games
    private double blackTime = BLACK_TIME;
    //About one star per this many square pixels
    private static final double STAR_SPACING = 1800;
    private static final Color[] STAR_COLORS = {Color.WHITE, Color.WHITE, Color.web("#FFF4C0"), Color.web("#C8E0FF"), Color.web("#B8FFC0")};

    public screenTransition (gameHandler handler) {
        this.handler = handler;
    }

    public boolean isActive () {return current != phase.NONE;}

    //True when this game is being left for another one (the screen will not open again)
    public boolean isLeaving () {return isActive() && (end != null || holding);}

    //middle is run once the screen is all black
    //middle is run once the screen is all black. minZoom is the smallest the zoom may get,
    //as a share of the zoom now (to never zoom out further than the player can, which lags)
    public void start (Runnable middle, double minZoom) {
        start(middle, minZoom, null);
    }

    //Like start, but with end the screen does not open again: end runs when the black part is over
    public void start (Runnable middle, double minZoom, Runnable end) {
        if (isActive()) return;
        this.middle = middle;
        this.end = end;
        this.holding = false;
        this.outZoom = Math.min(1, Math.max(ZOOM_OUT, minZoom));
        this.sizeCarry = 1;
        this.blackTime = end == null ? BLACK_TIME : BLACK_TIME/2;
        this.cue = null;
        enter(phase.CLOSING);
    }

    //Only the second half: all black (zoomed out), then opening around the player (the middle of the screen in the menu)
    public void startFromBlack (double minZoom) {
        if (isActive()) return;
        this.middle = null;
        this.end = null;
        this.holding = false;
        this.outZoom = Math.min(1, Math.max(ZOOM_OUT, minZoom));
        this.sizeCarry = 1;
        this.blackTime = BLACK_TIME/2;
        this.cue = null;
        enter(phase.BLACK);
    }

    //The stars for this screen size, made again when the window size changes
    private static void ensureStars (gameHandler handler) {
        if (handler.getWIDTH() > 0 && (handler.getWIDTH() != starsWidth || handler.getHEIGHT() != starsHeight)) makeStars(handler);
    }

    private static void makeStars (gameHandler handler) {
        stars.clear();
        starsWidth = handler.getWIDTH();
        starsHeight = handler.getHEIGHT();
        Random rdm = new Random();
        int count = (int) (handler.getWIDTH()*handler.getHEIGHT()/STAR_SPACING);
        for (int i = 0; i < count; i++) {
            star s = new star();
            s.u = rdm.nextDouble();
            s.v = rdm.nextDouble();
            //Pixel sizes: mostly 1, some 2, a few 3. Some of the 1 pixel ones sparkle
            double pick = rdm.nextDouble();
            s.size = pick < 0.6 ? 1 : pick < 0.9 ? 2 : 3;
            s.sparkle = s.size == 1 && rdm.nextDouble() < 0.25;
            s.speed = 2 + rdm.nextDouble()*5;
            s.phase = rdm.nextDouble()*2*Math.PI;
            s.color = STAR_COLORS[rdm.nextInt(STAR_COLORS.length)];
            stars.add(s);
        }
    }

    private void enter (phase next) {
        current = next;
        phaseStart = System.nanoTime();
    }

    private double phaseTime () {
        return (System.nanoTime() - phaseStart)/1e9;
    }

    //Moves the transition along. Called once per frame
    public void next () {
        switch (current) {
            case CLOSING:
                if (phaseTime() >= CLOSE_TIME) {
                    enter(phase.BLACK);
                    if (middle != null) middle.run();
                    middle = null;
                }
                break;
            case BLACK:
                if (cue != null && phaseTime() >= cueShare*blackTime) {
                    Runnable run = cue;
                    cue = null;
                    run.run();
                }
                if (holding || phaseTime() < blackTime) break;
                if (end != null) {
                    Runnable run = end;
                    end = null;
                    holding = true;
                    run.run();
                } else {
                    enter(phase.OPENING);
                }
                break;
            case OPENING:
                if (phaseTime() >= OPEN_TIME) current = phase.NONE;
                break;
            default:
                break;
        }
    }

    //Multiplied into the zoom: out while closing, held while black, back in while opening
    //The size at the darkest is outZoom times the size before. When the middle changes the base size (a new
    //level with its own size), sizeCarry makes up for it: the black part keeps the old size and the opening
    //eases from there to the new size, so nothing on screen (like the player) jumps in size
    public double zoomFactor () {
        switch (current) {
            case CLOSING: return 1 - (1 - outZoom)*ease(phaseTime()/CLOSE_TIME);
            case BLACK:   return outZoom*sizeCarry;
            case OPENING: return outZoom*sizeCarry + (1 - outZoom*sizeCarry)*ease(phaseTime()/OPEN_TIME);
            default:      return 1;
        }
    }

    //Old base size divided by new, set by the middle when it changes the base size
    public void setSizeCarry (double sizeCarry) {this.sizeCarry = sizeCarry;}

    //Smooth start and end
    private static double ease (double t) {
        t = Math.max(0, Math.min(1, t));
        return t*t*(3 - 2*t);
    }

    //The stars that fall inside the screen box (x, y, w, h), twinkling, at alpha times their brightness. The stars are
    //fixed to the screen, the same ones as in the transition: portals (portalType) show them through their tiles
    public static void drawStars (gameHandler handler, javafx.scene.canvas.GraphicsContext gc, double x, double y, double w, double h, double alpha) {
        ensureStars(handler);
        double time = System.nanoTime()/1e9;
        for (star s : stars) {
            double sx = s.u*handler.getWIDTH(), sy = s.v*handler.getHEIGHT();
            if (sx < x || sx >= x + w || sy < y || sy >= y + h) continue;
            drawStar(gc, s, sx, sy, time, alpha);
        }
    }

    private static void drawStar (javafx.scene.canvas.GraphicsContext gc, star s, double sx, double sy, double time, double alpha) {
        //Brightness goes up and down, and now and then drops to almost nothing
        double twinkle = 0.5 + 0.5*Math.sin(time*s.speed + s.phase);
        double brightness = 0.15 + 0.85*twinkle*twinkle;
        //Whole screen pixels, never more than 5x5: a dot of 1 to 3 pixels, and sparkling ones
        //(always 1 pixel) grow 1 pixel arms, then 2, when they shine the most
        int size = (int) s.size;
        double left = Math.floor(sx) - (size - 1)/2, top = Math.floor(sy) - (size - 1)/2;
        gc.setGlobalAlpha(alpha*brightness);
        gc.setFill(s.color);
        gc.fillRect(left, top, size, size);
        if (s.sparkle && twinkle > 0.75) {
            int arm = twinkle > 0.92 ? 2 : 1;
            gc.setGlobalAlpha(alpha*brightness*0.6);
            gc.fillRect(left - arm, top, arm, 1);
            gc.fillRect(left + 1, top, arm, 1);
            gc.fillRect(left, top - arm, 1, arm);
            gc.fillRect(left, top + 1, 1, arm);
        }
    }

    //Draws the black tiles over the world, lined up with the world's tiles
    public void draw () {
        if (!isActive()) return;
        //Made here, not when started, since a game started before its first frame may not know the screen size yet
        ensureStars(handler);

        int tC = handler.getTileCount();
        double unitW = handler.getUnitWidth(), unitH = handler.getUnitHeight();
        double camX = handler.getCamera().getX(), camY = handler.getCamera().getY();
        double midW = handler.getMidWIDTH(), midH = handler.getMidHEIGHT();

        character player = handler.getMainCharacter();
        int px = (int) Math.round(player.getX()*tC), py = (int) Math.round(player.getY()*tC);
        //Behind the main menu there is no player on screen, so it closes in on the middle of the screen
        if (handler.inMenu()) {
            px = (int) Math.floor(camX*tC);
            py = (int) Math.floor(camY*tC);
        }

        //The tiles on screen, with one extra on every side
        int minX = (int) Math.floor((camX - midW/unitW)*tC) - 1, maxX = (int) Math.ceil((camX + midW/unitW)*tC) + 1;
        int minY = (int) Math.floor((camY - midH/unitH)*tC) - 1, maxY = (int) Math.ceil((camY + midH/unitH)*tC) + 1;

        int farX = Math.max(Math.abs(minX - px), Math.abs(maxX - px));
        int farY = Math.max(Math.abs(minY - py), Math.abs(maxY - py));

        //A tile is black when its distance is at least the threshold
        boolean diamond = current == phase.CLOSING;
        double threshold;
        switch (current) {
            case CLOSING: threshold = (farX + farY + 1)*(1 - ease(phaseTime()/CLOSE_TIME)); break;
            case OPENING: threshold = (Math.max(farX, farY) + 1)*ease(phaseTime()/OPEN_TIME); break;
            default:      threshold = 0; break;
        }

        handler.getTempGc().setGlobalAlpha(1);
        handler.getTempGc().setFill(Color.BLACK);
        for (int ty = minY; ty <= maxY; ty++) {
            for (int tx = minX; tx <= maxX; tx++) {
                if (!isBlack(tx, ty, px, py, diamond, threshold)) continue;

                //Whole pixels from the tile edges, so neighbouring tiles leave no gaps
                double x0 = Math.floor(midW + (tx/(double) tC - camX)*unitW), x1 = Math.floor(midW + ((tx+1)/(double) tC - camX)*unitW);
                double y0 = Math.floor(midH + (ty/(double) tC - camY)*unitH), y1 = Math.floor(midH + ((ty+1)/(double) tC - camY)*unitH);
                handler.getTempGc().fillRect(x0, y0, x1 - x0, y1 - y0);
            }
        }

        //The stars, only on the tiles that are black right now
        double time = System.nanoTime()/1e9;
        for (star s : stars) {
            double sx = s.u*handler.getWIDTH(), sy = s.v*handler.getHEIGHT();
            int tx = (int) Math.floor((camX + (sx - midW)/unitW)*tC);
            int ty = (int) Math.floor((camY + (sy - midH)/unitH)*tC);
            if (!isBlack(tx, ty, px, py, diamond, threshold)) continue;
            drawStar(handler.getTempGc(), s, sx, sy, time, 1);
        }
        handler.getTempGc().setGlobalAlpha(1);
    }

    //Whether the tile (tx, ty) is black, measured from the player's tile (px, py)
    private static boolean isBlack (int tx, int ty, int px, int py, boolean diamond, double threshold) {
        int dx = Math.abs(tx - px), dy = Math.abs(ty - py);
        double distance = diamond ? dx + dy : Math.max(dx, dy);
        return distance >= threshold;
    }
}
