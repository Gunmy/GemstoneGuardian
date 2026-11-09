package game;

import java.util.function.Consumer;

public class waveType extends tileType {
    private boolean wave;
    private tileType underlay;
    private int z;
    private double tall;
    private double scatter;

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
        if (wave) {
            heightOffset += Math.sin(time/division)*handler.getUnitHeight()/handler.getTileCount()*0.09;
        }

        if (scatter > 0) {
            realx += scatterValue(x, y, 0)*scatter*handler.getUnitWidth()/handler.getTileCount();
            realy += scatterValue(x, y, 1)*scatter*handler.getUnitHeight()/handler.getTileCount();
        }

        handler.getTempGc().drawImage(handler.getImages().getImage(0),
        this.x+1, this.y+1, this.width-2, this.height-2,
         (int) realx,   (int) realy-heightOffset,
         (int) (handler.getUnitWidth()/handler.getTileCount()+correctionVal),
         (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal)+heightOffset);
    }
}
