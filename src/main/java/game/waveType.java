package game;

import java.util.function.Consumer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class waveType extends tileType {
    private boolean wave;
    private tileType underlay;
    private int z;
    private double tall;
    private double scatter;
    //Plants sway in the wind instead of bobbing: a gust rolls over the map from the top left to the bottom right
    //and bends each plant up and to the right from its base, so neighbours lean one after another
    private boolean sway = false;

    //How far the top of a plant leans in a full gust (in tiles per tile of height) and how much it stretches upwards
    private static final double SWAY_LEAN = 0.09;
    private static final double SWAY_STRETCH = 0.03;
    //How fast the gusts move and how far apart they are (one gust every 2*pi/SWAY_SPACING tiles along the diagonal)
    private static final double SWAY_SPEED = 1.6;
    private static final double SWAY_SPACING = 0.45;

    //z is the level the overlay is drawn on, tall is how many tiles high it is drawn (it grows upwards),
    //scatter is how far (in tiles) the overlay can be moved away from the middle of its tile
    public waveType(String name, int x, int y, int width, int height, int id, boolean block,
            Consumer<character> pressurePlate, tileType underlay, boolean wave, int z, double tall, double scatter) {
        super(name, x, y, width, height, id, block, pressurePlate);
        if (z < 0) throw new IllegalArgumentException("Z kan ikke være under 0");
        if (tall < 1) throw new IllegalArgumentException("Tall kan ikke være under 1");
        if (scatter < 0 || scatter > 0.5) throw new IllegalArgumentException("Scatter må være mellom 0 og 0.5");
        this.underlay = underlay;
        this.wave = wave;
        this.z = z;
        this.tall = tall;
        this.scatter = scatter;
    }

    //A number between -1 and 1 that looks random, but is always the same for the same tile
    private double scatterValue (int x, int y, int seed) {
        long hash = x*73856093L ^ y*19349663L ^ seed*83492791L;
        hash = (hash ^ (hash >>> 13))*1274126177L;
        hash = hash ^ (hash >>> 16);
        return (hash & 0xFFFF)/32767.5 - 1;
    }

    public void setSway (boolean sway) {this.sway = sway;}

    //How hard the wind pushes the plant at (x, y) right now, 0 to 1. There is always a light breeze,
    //and the gusts are sharp peaks of a wave whose crests move towards the bottom right (x + y getting bigger)
    private double gust (int x, int y) {
        double t = System.nanoTime()/1e9;
        double phase = t*SWAY_SPEED - (x + y)*SWAY_SPACING + scatterValue(x, y, 2)*0.4;
        double peak = 0.5 + 0.5*Math.sin(phase);
        return 0.15 + 0.85*peak*peak*peak;
    }

    //Draws the overlay leaning right by the gust g: the bottom edge stays put and the top is pushed right and a bit up.
    //tileWidth is the size of one tile on screen, the lean grows with how tall the overlay is
    private void drawSwaying (GraphicsContext gc, Image sheet, double g, double dx, double dy, double dw, double dh, double tileWidth) {
        double stretch = dh*SWAY_STRETCH*g;
        double top = dy - stretch, height = dh + stretch;
        double bottom = dy + dh;
        //x' = x + shear*(y - bottom), so the bottom stays and the top moves right by -shear*height
        double shear = -SWAY_LEAN*g*tileWidth*tall/height;
        gc.save();
        gc.transform(1, 0, shear, 1, -shear*bottom, 0);
        gc.drawImage(sheet, this.x+1, this.y+1, this.width-2, this.height-2, dx, top, dw, height);
        gc.restore();
    }

    //In a small map the underlay is drawn with the overlay flat on top, inside the tile
    @Override
    public void drawMini (GraphicsContext gc, Image sheet, double x, double y, double width, double height) {
        underlay.drawMini(gc, sheet, x, y, width, height);
        super.drawMini(gc, sheet, x, y, width, height);
    }

    @Override
    public int getMaxZ () {return Math.max(z, underlay.getMaxZ());}

    //The overlay is drawn on its own level, also when the underlay is higher up. That way a flat
    //overlay (like the edge of a hill) on top of a tree ends up on the ground, behind the tree
    @Override
    public void drawLayer (gameHandler handler, double realx, double realy, int x, int y, int z) {
        underlay.drawLayer(handler, realx, realy, x, y, z);
        if (z == this.z && overlayInsideScreen(handler, realx, realy)) {
            drawSpecial(handler, realx, realy, x, y);
        }
    }

    //The overlay can reach into the tiles around (tall or scattered), so it is drawn one tile outside the screen too
    private boolean overlayInsideScreen (gameHandler handler, double realx, double realy) {
        double tileWidth = handler.getUnitWidth()/handler.getTileCount();
        double tileHeight = handler.getUnitHeight()/handler.getTileCount();
        return -2*tileWidth <= realx
        && realx <= handler.getWIDTH()+tileWidth
        && -2*tileHeight <= realy
        && realy <= handler.getHEIGHT()+tileHeight;
    }

    //In a box a waving overlay waves like it does in the world
    @Override
    public void draw (GraphicsContext gc, gui gui, double x, double y, double size) {
        if (sway) {
            drawSwaying(gc, gui.getHandler().getImages().getImage(0), gust(0, 0), Math.round(x), y, Math.round(size), size, size/tall);
            return;
        }
        double heightOffset = wave ? Math.sin(System.nanoTime()/300000000.0)*size*0.09 : 0;
        gc.drawImage(gui.getHandler().getImages().getImage(0), this.x+1, this.y+1, width-2, height-2, Math.round(x), y - heightOffset, Math.round(size), size + heightOffset);
    }

    //Only the overlay, the underlay is drawn by drawLayer
    @Override
    protected void drawSpecial (gameHandler handler, double realx, double realy, int x, int y) {
        int correctionVal =0;
        if (handler.getTempCanvas().getOpacity() == 1) {
            correctionVal = 1;
        }

        double time = (double) System.nanoTime();
        double division = 300000000;

        double heightOffset = handler.getUnitHeight()/handler.getTileCount()*(tall-1);
        if (wave && !sway) {
            heightOffset += Math.sin(time/division)*handler.getUnitHeight()/handler.getTileCount()*0.09;
        }

        if (scatter > 0) {
            realx += scatterValue(x, y, 0)*scatter*handler.getUnitWidth()/handler.getTileCount();
            realy += scatterValue(x, y, 1)*scatter*handler.getUnitHeight()/handler.getTileCount();
        }

        if (sway) {
            double tileWidth = handler.getUnitWidth()/handler.getTileCount();
            drawSwaying(handler.getTempGc(), handler.getImages().getImage(0), gust(x, y),
                (int) realx, (int) realy-heightOffset,
                (int) (tileWidth+correctionVal),
                (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal)+heightOffset, tileWidth);
            return;
        }

        handler.getTempGc().drawImage(handler.getImages().getImage(0),
        this.x+1, this.y+1, this.width-2, this.height-2,
         (int) realx,   (int) realy-heightOffset,
         (int) (handler.getUnitWidth()/handler.getTileCount()+correctionVal),
         (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal)+heightOffset);

        //A chest the player can open right now gets lighter and back, fading in and out: a brightened copy of its own
        //picture is laid over it, so the texture always shows
        double highlight = isContainer() ? handler.chestHighlight(x, y) : 0;
        if (highlight > 0) {
            GraphicsContext gc = handler.getTempGc();
            double alpha = gc.getGlobalAlpha();
            gc.setGlobalAlpha(alpha*highlight);
            gc.drawImage(handler.getImages().getBrightened(this.x, this.y, gameHandler.CHEST_BRIGHTEN),
             1, 1, guiSkin.CELL-2, guiSkin.CELL-2,
             (int) realx,   (int) realy-heightOffset,
             (int) (handler.getUnitWidth()/handler.getTileCount()+correctionVal),
             (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal)+heightOffset);
            gc.setGlobalAlpha(alpha);
        }
    }
}
