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
    //One grid square of a layer, in chunks
    public static final double CELL = 0.8;

    private gameHandler handler;

    public skyLayer (gameHandler handler) {
        this.handler = handler;
    }

    //Draws a cloud: simple squares of different sizes joined into one, standing on a common bottom line and
    //overlapping their neighbours, the biggest ones near the middle. A darker copy a little lower is its shaded
    //underside. seed picks the squares; (x, y) is the top left of its w x h box on the screen
    private void drawCloud (GraphicsContext gc, long seed, double x, double y, double w, double h) {
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
            squares[i] = new double[] {Math.round(sx), Math.round(bottom - size), Math.round(size)};
            left = sx + size;
        }
        double shade = Math.max(1, Math.round(h*0.08));
        gc.setFill(UNDER);
        for (double[] q : squares) gc.fillRect(q[0], q[1] + shade, q[2], q[2]);
        gc.setFill(BODY);
        for (double[] q : squares) gc.fillRect(q[0], q[1], q[2], q[2]);
    }

    //Draws one layer. withCamera false: the layer as seen through a portal, not moving with the camera
    private void drawLayer (GraphicsContext gc, double[] layer, int index, boolean withCamera, double alpha) {
        double unitW = handler.getUnitWidth(), unitH = handler.getUnitHeight();
        double midW = handler.getMidWIDTH(), midH = handler.getMidHEIGHT();
        double time = System.nanoTime()/1e9;
        //Where the middle of the screen is in this layer, in chunks
        double cx = (withCamera ? handler.getCamera().getX()*layer[0] : 0) - time*layer[1];
        double cy = withCamera ? handler.getCamera().getY()*layer[0] : 0;
        double tile = unitW/handler.getTileCount();
        //The cells on screen, with room for a cloud reaching in from the side
        int x0 = (int) Math.floor((cx - midW/unitW)/CELL) - 2, x1 = (int) Math.ceil((cx + midW/unitW)/CELL) + 1;
        int y0 = (int) Math.floor((cy - midH/unitH)/CELL) - 2, y1 = (int) Math.ceil((cy + midH/unitH)/CELL) + 1;
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
        drawLayer(gc, FRONT, BACK.length, true, 1);
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
