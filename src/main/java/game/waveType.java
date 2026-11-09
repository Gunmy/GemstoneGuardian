package game;

import java.util.function.Consumer;

public class waveType extends tileType {
    private boolean wave;
    private tileType underlay;
    private int z;

    //z is the level the overlay is drawn on
    public waveType(String name, int x, int y, int width, int height, int id, boolean block,
            Consumer<character> pressurePlate, tileType underlay, boolean wave, int z) {
        super(name, x, y, width, height, id, block, pressurePlate);
        if (z < 0) throw new IllegalArgumentException("Z kan ikke være under 0");
        this.underlay = underlay;
        this.wave = wave;
        this.z = z;
    }

    @Override
    public int getMaxZ () {return Math.max(z, underlay.getMaxZ());}

    //The overlay is drawn on its own level, also when the underlay is higher up. That way a flat
    //overlay (like the edge of a hill) on top of a tree ends up on the ground, behind the tree
    @Override
    public void drawLayer (gameHandler handler, double realx, double realy, int x, int y, int z) {
        underlay.drawLayer(handler, realx, realy, x, y, z);
        if (z == this.z && insideScreen(handler, realx, realy)) {
            drawSpecial(handler, realx, realy, x, y);
        }
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

        double heightOffset = 0;
        if (wave) {
            heightOffset = Math.sin(time/division)*handler.getUnitHeight()/handler.getTileCount()*0.09;
        }

        handler.getTempGc().drawImage(handler.getImages().getImage(0),
        this.x+1, this.y+1, this.width-2, this.height-2,
         (int) realx,   (int) realy-heightOffset,
         (int) (handler.getUnitWidth()/handler.getTileCount()+correctionVal),
         (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal)+heightOffset);
    }
}
