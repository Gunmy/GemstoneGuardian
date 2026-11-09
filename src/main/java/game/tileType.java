package game;

import java.util.function.Consumer;

public class tileType {

    protected String name;
    protected int x, y, width, height, id;
    protected boolean block;
    protected Consumer<character> pressurePlate;

    public tileType (String name, int x, int y, int width, int height, int id, boolean block, Consumer<character> pressurePlate) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.id = id;
        this.block = block;
        this.pressurePlate = pressurePlate;
    }

    public boolean isBlocked () {return this.block;}

    public int getId () {return this.id;}

    protected boolean insideScreen (gameHandler handler, double x, double y) {
        return -handler.getUnitWidth()/handler.getTileCount() <= x
        && x <= handler.getWIDTH()
        && -handler.getUnitHeight()/handler.getTileCount() <= y
        && y <= handler.getHEIGHT();
    }

    //Draws the part of the tile that is on the given z-level. The levels are drawn one at a time:
    //0 is the ground, 1 is whatever stands on it (trees, flowers, most characters...),
    //2 and up is above that again. A plain tile is only ground
    public void drawLayer (gameHandler handler, double realx, double realy, int x, int y, int z) {
        if (z == 0 && insideScreen(handler, realx, realy)) {
            drawSpecial(handler, realx, realy, x, y);
        }
    }

    public int getMaxZ () {return 0;}

    protected void drawSpecial (gameHandler handler, double realx, double realy, int x, int y) {
        int correctionVal =0;
        if (handler.getTempCanvas().getOpacity() == 1) {
            correctionVal = 1;
        }

        handler.getTempGc().drawImage(

        handler.getImages().getImage(0), 
        this.x+1, 
        this.y+1, 
        this.width-2, 
        this.height-2, 
        (int) realx,   
        (int) realy, 
        (int) (handler.getUnitWidth()/handler.getTileCount()+correctionVal), 
        (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal)
        
        );
    }

    public void doFunction (character character) {
        if (this.pressurePlate != null) {
            pressurePlate.accept(character);

        }
    }

    public String getName () {
        return this.name;
    }
}
