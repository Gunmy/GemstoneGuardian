package game;

import java.util.function.Consumer;

public class waveType extends tileType {
    private boolean wave;
    private tileType underlay;
    public waveType(String name, int x, int y, int width, int height, int id, boolean block,
            Consumer<character> pressurePlate, tileType underlay, boolean wave) {
        super(name, x, y, width, height, id, block, pressurePlate);
        this.underlay = underlay;
        this.wave = wave;
    }
    
    @Override
    protected void drawSpecial (gameHandler handler, double realx, double realy, int x, int y) {
        int correctionVal =0;
        if (handler.getTempCanvas().getOpacity() == 1) {
            correctionVal = 1;
        }

        underlay.draw(handler, realx, realy, x, y);
    
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
