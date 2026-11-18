package game;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//The open sky of a sky world (chunks.sky()): the background is its blue, and clouds drift across it. The tiles of a
//sky world that are not ground are open sky (tile 184, drawn as nothing), so the clouds show there.
//The clouds are drawn in code, no textures (drawCloud): simple squares of different sizes joined into one, with a
//shaded underside, in the art's whites and light greys.
//They lie in layers at different depths: a layer moves DEPTH times as much as the ground when the camera moves, so far
//ones move less (parallax), and each layer drifts slowly on its own. Within a layer the clouds have fixed places in a
//grid of CELL sized squares (which cells have a cloud and what it looks like comes from the cell's own seed), so they
//only get bigger or smaller when zooming, never jump. drawBack draws the layers behind the ground, drawFront the one
//that now and then passes in front of everything, drawWindow the sky as seen through a sky portal (fixed to the screen)
public class skyLayer {

    public static final String SKY = "#8EDCF2";
    private static final Color TOP = Color.web("#FFFFFF"), BODY = Color.web("#F0F0F4"), UNDER = Color.web("#D8DCE4");

    //depth, drift (chunks per second), chance of a cloud per cell, how see-through, size factor
    private static final double[][] BACK = {{0.25, 0.03, 0.45, 1, 0.8}, {0.5, 0.05, 0.4, 1, 1}};
    private static final double[] FRONT = {1.4, 0.08, 0.12, 0.8, 1.3};
    //High clouds over an open world seen from far out (drawHigh), from the lowest level to the highest: above the
    //ground, so they move more than it (depth over 1, more the higher), bigger the higher, a little see-through. Two
    //more numbers each: from how many chunks across the screen the level fades in, and where it is fully there, so
    //zooming out the low ones show first (a few already close in) and more and more join
    private static final double[][] HIGH = {
        {1.12, 0.03, 0.06, 0.85, 2.2, 1.6, 2.4},
        {1.3, 0.04, 0.12, 0.85, 4.0, 2.4, 3.5},
        {1.55, 0.06, 0.16, 0.8, 6.5, 3.5, 5.5},
        {1.9, 0.08, 0.22, 0.75, 10, 5.5, 8}};
    //One grid square of a layer, in chunks
    public static final double CELL = 0.8;

    private gameHandler handler;

    public skyLayer (gameHandler handler) {
        this.handler = handler;
    }

    //Draws a cloud: simple squares of different sizes joined into one, standing on a common bottom line and
    //overlapping their neighbours, the biggest ones near the middle. A darker copy a little lower is its shaded
    //underside. seed picks the squares; (x, y) is the top left of its w x h box on the screen
    static void drawCloud (GraphicsContext gc, long seed, double x, double y, double w, double h) {
        double[][] squares = cloudSquares(seed, x, y, h);
        for (double[] q : squares) {
            q[0] = Math.round(q[0]);
            q[1] = Math.round(q[1]);
            q[2] = Math.round(q[2]);
        }
        double shade = Math.max(1, Math.round(h*0.08));
        gc.setFill(UNDER);
        for (double[] q : squares) gc.fillRect(q[0], q[1] + shade, q[2], q[2]);
        gc.setFill(BODY);
        for (double[] q : squares) gc.fillRect(q[0], q[1], q[2], q[2]);
    }

    //A see-through layer is drawn solid into an image first (at SOLID_SCALE of the screen, the clouds are blocky
    //anyway) and that image goes on the screen once, see-through: squares and clouds that overlap are then no more
    //solid than one alone, instead of their see-throughness adding up
    public static final double SOLID_SCALE = 0.5;
    private javafx.scene.canvas.Canvas solid;
    private javafx.scene.image.WritableImage solidImage;
    private final javafx.scene.SnapshotParameters solidParams = new javafx.scene.SnapshotParameters();
    {solidParams.setFill(Color.TRANSPARENT);}

    private void drawLayerEvenly (GraphicsContext gc, double[] layer, int index, double alpha) {
        double a = alpha*layer[3];
        if (a <= 0) return;
        if (a >= 0.999) {
            drawLayer(gc, layer, index, true, 1/layer[3]);
            return;
        }
        double w = handler.getTempCanvas().getWidth(), h = handler.getTempCanvas().getHeight();
        int sw = Math.max(1, (int) Math.ceil(w*SOLID_SCALE)), sh = Math.max(1, (int) Math.ceil(h*SOLID_SCALE));
        if (solid == null || solid.getWidth() != sw || solid.getHeight() != sh) {
            solid = new javafx.scene.canvas.Canvas(sw, sh);
            solidImage = new javafx.scene.image.WritableImage(sw, sh);
        }
        GraphicsContext s = solid.getGraphicsContext2D();
        s.setTransform(1, 0, 0, 1, 0, 0);
        s.clearRect(0, 0, sw, sh);
        s.setTransform(SOLID_SCALE, 0, 0, SOLID_SCALE, 0, 0);
        s.setImageSmoothing(false);
        //Solid: alpha 1 for this layer
        drawLayer(s, layer, index, true, 1/layer[3]);
        s.setTransform(1, 0, 0, 1, 0, 0);
        solid.snapshot(solidParams, solidImage);
        gc.save();
        gc.setGlobalAlpha(a);
        gc.setImageSmoothing(false);
        gc.drawImage(solidImage, 0, 0, sw, sh, 0, 0, sw/SOLID_SCALE, sh/SOLID_SCALE);
        gc.restore();
    }

    //The squares of a cloud (x, y, size each), unrounded: the shape drawCloud draws. Its width follows from h
    static double[][] cloudSquares (long seed, double x, double y, double h) {
        Random rnd = new Random(seed);
        int n = 3 + rnd.nextInt(4);
        double bottom = y + h*0.9;
        double[][] squares = new double[n][];
        double left = x;
        for (int i = 0; i < n; i++) {
            double middle = 1 - Math.abs(i - (n - 1)/2.0)/((n - 1)/2.0 + 0.001)*0.55;
            double size = Math.max(h*0.25, h*0.85*middle*(0.75 + rnd.nextDouble()*0.25));
            //Each square overlaps the one before it, so they join up
            double sx = i == 0 ? left : left - size*(0.25 + rnd.nextDouble()*0.2);
            squares[i] = new double[] {sx, bottom - size, size};
            left = sx + size;
        }
        return squares;
    }

    //How wide a cloud of height h is, from its left edge to its right. Every square reaches down to the line h*0.65
    //below its top (a square is at least a quarter of h, on a bottom line at h*0.9), and they overlap, so along that
    //line it is solid cloud the whole way across
    static double cloudWidth (long seed, double h) {
        double right = 0;
        for (double[] q : cloudSquares(seed, 0, 0, h)) right = Math.max(right, q[0] + q[2]);
        return right;
    }
    public static final double CLOUD_SOLID_LINE = 0.75;

    //Draws one layer. withCamera false: the layer as seen through a portal, not moving with the camera
    private void drawLayer (GraphicsContext gc, double[] layer, int index, boolean withCamera, double alpha) {
        double unitW = handler.getUnitWidth(), unitH = handler.getUnitHeight();
        double midW = handler.getMidWIDTH(), midH = handler.getMidHEIGHT();
        double time = System.nanoTime()/1e9;
        //Where the middle of the screen is in this layer, in chunks
        double cx = (withCamera ? handler.getCamera().getX()*layer[0] : 0) - time*layer[1];
        double cy = withCamera ? handler.getCamera().getY()*layer[0] : 0;
        double tile = unitW/handler.getTileCount();
        //The cells on screen, with room for a cloud reaching in from the side (up to 5 tiles wide times its size)
        int reach = 2 + (int) Math.ceil(5*layer[4]*1.15/handler.getTileCount()/CELL);
        int x0 = (int) Math.floor((cx - midW/unitW)/CELL) - reach, x1 = (int) Math.ceil((cx + midW/unitW)/CELL) + 1;
        int reachY = 2 + (int) Math.ceil(2*layer[4]/handler.getTileCount()/CELL);
        int y0 = (int) Math.floor((cy - midH/unitH)/CELL) - reachY, y1 = (int) Math.ceil((cy + midH/unitH)/CELL) + 1;
        gc.setGlobalAlpha(alpha*layer[3]);
        for (int j = y0; j <= y1; j++) {
            for (int i = x0; i <= x1; i++) {
                long seed = ((long) i*73856093L) ^ ((long) j*19349663L) ^ (index*83492791L);
                Random rnd = new Random(seed);
                if (rnd.nextDouble() >= layer[2]) continue;
                int w = 2 + rnd.nextInt(4), h = 1 + (rnd.nextDouble() < 0.35 ? 1 : 0);
                double size = layer[4]*(0.85 + rnd.nextDouble()*0.3);
                double wx = (i + rnd.nextDouble())*CELL, wy = (j + rnd.nextDouble())*CELL;
                double sx = Math.round(midW + (wx - cx)*unitW), sy = Math.round(midH + (wy - cy)*unitH);
                drawCloud(gc, rnd.nextLong(), sx, sy, w*tile*size, h*tile*size);
            }
        }
    }

    //Behind the ground: called before the tiles are drawn
    public void drawBack (GraphicsContext gc) {
        gc.save();
        gc.setImageSmoothing(false);
        for (int i = 0; i < BACK.length; i++) drawLayer(gc, BACK[i], i, true, 1);
        gc.restore();
    }

    //In front of everything in the world, a little see-through, only now and then
    public void drawFront (GraphicsContext gc) {
        gc.save();
        gc.setImageSmoothing(false);
        drawLayerEvenly(gc, FRONT, BACK.length, 1);
        gc.restore();
    }

    //Clouds high over an open world (chunks.cloudsFromAfar), seen when zoomed out: in front of everything, the low
    //levels first, each fading in at its own zoom, all at alpha on top (gameHandler.highCloudAlpha fades them away
    //far out)
    public void drawHigh (GraphicsContext gc, double alpha) {
        if (alpha <= 0) return;
        double across = handler.chunksAcross();
        gc.save();
        gc.setImageSmoothing(false);
        for (int i = 0; i < HIGH.length; i++) {
            double in = Math.max(0, Math.min(1, (across - HIGH[i][5])/(HIGH[i][6] - HIGH[i][5])));
            if (in > 0) drawLayerEvenly(gc, HIGH[i], BACK.length + 1 + i, alpha*in);
        }
        gc.restore();
    }

    //The sky through a sky portal tile (x, y, w, h on the screen): blue, with the far clouds drifting by, fixed to the
    //screen, at alpha (the tile's own fading)
    public void drawWindow (GraphicsContext gc, double x, double y, double w, double h, double alpha) {
        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.web(SKY));
        gc.fillRect(x, y, w, h);
        gui.clipTo(gc, x, y, w, h);
        gc.setImageSmoothing(false);
        for (int i = 0; i < BACK.length; i++) drawLayer(gc, BACK[i], i, false, alpha);
        gc.restore();
    }
}
