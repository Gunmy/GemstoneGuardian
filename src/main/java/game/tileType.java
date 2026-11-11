package game;

import java.util.function.Consumer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class tileType implements picture {

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

    //Only set for special tiles that are solid from some sides and open from others.
    //Every other tile is solid from all sides or none, decided by block
    private boolean[] solidFrom;

    //1 if the tile is solid when you come from that side, 0 if not. Same order as the moves in movementEvent
    public void setSolidFrom (int left, int top, int right, int bottom) {
        this.solidFrom = new boolean[] {left != 0, top != 0, right != 0, bottom != 0};
    }

    //xChange and yChange is the step taken to get into the tile
    public boolean isBlocked (double xChange, double yChange) {
        if (solidFrom == null) return this.block;

        if (xChange > 0) return solidFrom[0];
        if (yChange > 0) return solidFrom[1];
        if (xChange < 0) return solidFrom[2];
        if (yChange < 0) return solidFrom[3];
        return this.block;
    }

    //Only set for special tiles that you can stand on, but not leave in every direction
    private boolean[] solidTo;

    //1 if you can not leave the tile in that direction, 0 if you can. Not the same setting as solidFrom:
    //solidFrom is about getting into the tile, solidTo is about getting out of it
    public void setSolidTo (int left, int top, int right, int bottom) {
        this.solidTo = new boolean[] {left != 0, top != 0, right != 0, bottom != 0};
    }

    //xChange and yChange is the step taken to get out of the tile
    public boolean isBlockedOut (double xChange, double yChange) {
        if (solidTo == null) return false;

        if (xChange < 0) return solidTo[0];
        if (yChange < 0) return solidTo[1];
        if (xChange > 0) return solidTo[2];
        if (yChange > 0) return solidTo[3];
        return false;
    }

    public int getId () {return this.id;}

    //The one colour the tile is drawn in when zoomed far out (see gameHandler.flatView, chunks.drawFlat). Worked out
    //from the texture (the colour most of its pixels have, see flatPixels) unless set by hand in tileTypes
    private Color primaryColor;
    private boolean primaryKnown = false;
    //Set by hand: kept as it is when the colours are made unique (tileTypes.assignPrimaryColors)
    private boolean primaryFixed = false;
    private int primaryArgb, primaryColdArgb;

    public void setPrimaryColor (String hex) {
        this.primaryColor = hex == null ? null : Color.web(hex);
        this.primaryKnown = true;
        this.primaryFixed = true;
        cacheArgb();
    }
    public boolean isPrimaryFixed () {return this.primaryFixed;}

    //The colour most of its texture is (or the one set by hand), before assignPrimaryColors moved it away from the
    //others. The chunks drawn as one colour each use this (chunk.getChunkColor), so tiles that look alike count together
    private Color textureColor;
    private boolean textureKnown = false;
    public Color getTextureColor (imgDict images) {
        if (!textureKnown) {
            textureColor = primaryFixed ? primaryColor : imgDict.mostCommon(flatPixels(images));
            textureKnown = true;
        }
        return textureColor;
    }

    //The colour tileTypes.assignPrimaryColors gave it, close to its own but not the same as any other tile's
    public void assignPrimaryColor (Color color) {
        this.primaryColor = color;
        this.primaryKnown = true;
        cacheArgb();
    }

    //Null when the tile is see-through (the open sky)
    public Color getPrimaryColor (imgDict images) {
        if (!primaryKnown) {
            primaryColor = imgDict.mostCommon(flatPixels(images));
            primaryKnown = true;
            cacheArgb();
        }
        return primaryColor;
    }

    //The primary colour as ARGB, in the cold colours of a mirror world if cold. 0 when see-through
    public int getPrimaryArgb (imgDict images, boolean cold) {
        getPrimaryColor(images);
        return cold ? primaryColdArgb : primaryArgb;
    }

    private void cacheArgb () {
        primaryArgb = imgDict.argb(primaryColor);
        primaryColdArgb = primaryColor == null ? 0 : imgDict.argb(imgDict.cold(primaryColor));
    }

    //The tile as it looks lying still, one cell of ARGB pixels, which its primary colour is taken from
    protected int[] flatPixels (imgDict images) {return images.cellPixels(this.x, this.y);}

    //Tiles of a type that is a teleport target (the healing pads) are listed in the teleports menu
    private boolean teleportTarget = false;
    public void setTeleportTarget (boolean teleportTarget) {this.teleportTarget = teleportTarget;}
    public boolean isTeleportTarget () {return this.teleportTarget;}

    //Every tile of a container type is a chest that opens when clicked (see chest)
    private boolean container = false;
    public void setContainer (boolean container) {this.container = container;}
    public boolean isContainer () {return this.container;}

    //The tile flat and still in a small map (the teleports menu): its cell from the sheet in the given rectangle
    public void drawMini (GraphicsContext gc, Image sheet, double x, double y, double width, double height) {
        gc.drawImage(sheet, this.x, this.y, this.width, this.height, x, y, width, height);
    }

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
        this.x, 
        this.y, 
        this.width, 
        this.height, 
        (int) realx,   
        (int) realy, 
        (int) (handler.getUnitWidth()/handler.getTileCount()+correctionVal), 
        (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal)
        
        );
    }

    public void doFunction (character character) {
        if (this.pressurePlate != null) {
            dialogueContainer dialogues = character.getHandler().getDiealoguecontainer();
            dialogues.setSource(this);
            pressurePlate.accept(character);
            dialogues.clearSource();
        }
        if (teleportTarget) {
            character.visitTeleport(character.getStandingTile());
            character.getHandler().teleportTargetVisited(character, character.getStandingTile());
        }
    }

    public String getName () {
        return this.name;
    }

    //In a box the tile is its own cell from the sheet
    @Override
    public void draw (GraphicsContext gc, gui gui, double x, double y, double size) {
        gc.drawImage(gui.getHandler().getImages().getImage(0), this.x, this.y, width, height, Math.round(x), Math.round(y), Math.round(size), Math.round(size));
    }
}
