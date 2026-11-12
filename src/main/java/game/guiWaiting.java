package game;

import javafx.scene.canvas.GraphicsContext;

//The "waiting" sign: eight little rocks in a ring (the game's own rock icon, tinted), lit up one after another round it.
//Shown while a 1v1 waits for the other player (guiDuelMenu, guiBattle)
public class guiWaiting {

    private static final int ROCKS = 8;
    //One lap of the light round the ring, in seconds
    private static final double LAP = 1.2;
    private static final String[] COLORS = {"#E8E8E8", "#8EDCF2", "#FFE040", "#FF7020", "#60D040", "#D060FF", "#FF4060", "#60C0FF"};

    //size is the ring's width, (cx, cy) its middle
    public static void draw (GraphicsContext gc, gui gui, double cx, double cy, double size) {
        double t = System.nanoTime()/1e9/LAP % 1.0;
        double rock = size*0.26, radius = (size - rock)/2;
        double alpha = gc.getGlobalAlpha();
        for (int i = 0; i < ROCKS; i++) {
            double angle = 2*Math.PI*i/ROCKS - Math.PI/2;
            //How far behind the light this rock is, 0 (lit now) to 1 (a lap behind)
            double behind = ((t*ROCKS - i) % ROCKS + ROCKS) % ROCKS / ROCKS;
            double glow = Math.pow(1 - behind, 2);
            double s = rock*(0.7 + 0.3*glow);
            double x = cx + Math.cos(angle)*radius - s/2, y = cy + Math.sin(angle)*radius - s/2;
            gc.setGlobalAlpha(alpha*(0.25 + 0.75*glow));
            gc.drawImage(gui.getHandler().getImages().getTinted(guiSkin.ROCK_X, guiSkin.GUI_Y, COLORS[i]), 0, 0, guiSkin.CELL, guiSkin.CELL,
                Math.round(x), Math.round(y), Math.round(s), Math.round(s));
        }
        gc.setGlobalAlpha(alpha);
    }
}
