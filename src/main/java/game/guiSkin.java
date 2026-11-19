package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

//Where the GUI pieces live in tiles.png and how they are drawn. The GUI row (y = 1500) holds, from the left:
//slot, selected slot, panel corner/top edge/left edge/middle, button corner/top edge/left edge/middle, rock, potion, bandage.
//Panels and buttons are 9-sliced: the corner and edges are drawn at a fixed size and flipped for the other sides,
//the middle is stretched
public class guiSkin {

    public static final int CELL = 20;
    public static final int GUI_Y = 300;
    public static final int SLOT_X = 0;
    public static final int SLOT_SELECTED_X = 20;
    public static final int PANEL_X = 40;
    public static final int BUTTON_X = 120;
    public static final int ROCK_X = 200;
    public static final int POTION_X = 220;
    public static final int BANDAGE_X = 240;
    //The heart of the warrior, also the white shape in empty trinket slots
    public static final int HEART_X = 260;

    //The icon row (y = 320): ability type icons first (see abilityType), then message icons
    public static final int ICON_Y = 320;
    public static final int LEVEL_UP_X = 180;

    //How many screen pixels the border pieces of panels and buttons get (a whole 20px cell).
    //The pieces are drawn as 20x20 pixel art, so at 20 one drawn pixel is one screen pixel
    private double border = 20;

    private gui gui;
    private gameHandler handler;

    public guiSkin (gui gui) {
        this.gui = gui;
        this.handler = gui.getHandler();
    }

    public double getBorder () {return border*gui.s();}

    //Draws one cell of the sheet (shrunk by a pixel on each side so the neighbours do not bleed in)
    public void drawCell (GraphicsContext gc, int cellX, int cellY, double x, double y, double w, double h) {
        drawCell(gc, handler.getImages().getImage(0), cellX, cellY, x, y, w, h, false, false);
    }

    public void drawCell (GraphicsContext gc, Image img, int cellX, int cellY, double x, double y, double w, double h, boolean flipX, boolean flipY) {
        //Both edges are rounded and the size taken from them, so pieces that meet (the 9-slice's) share the same pixel
        //edge: rounding the start and the size apart left a 1px gap or overlap by the right and bottom edges
        long left = Math.round(x), top = Math.round(y), right = Math.round(x + w), bottom = Math.round(y + h);
        gc.save();
        gc.translate(flipX ? right : left, flipY ? bottom : top);
        gc.scale(flipX ? -1 : 1, flipY ? -1 : 1);
        gc.drawImage(img, cellX, cellY, CELL, CELL, 0, 0, right - left, bottom - top);
        gc.restore();
    }

    public void drawPanel (GraphicsContext gc, double x, double y, double w, double h) {
        nineSlice(gc, PANEL_X, x, y, w, h);
    }

    public void drawButton (GraphicsContext gc, double x, double y, double w, double h) {
        nineSlice(gc, BUTTON_X, x, y, w, h);
    }

    //A button in a colour: its own cells with their grey levels turned into shades of hex (imgDict.getTinted, lifted so
    //it stays light), border and all
    public void drawButton (GraphicsContext gc, double x, double y, double w, double h, String hex) {
        if (hex == null) {
            drawButton(gc, x, y, w, h);
            return;
        }
        imgDict images = handler.getImages();
        Image corner = images.getTinted(BUTTON_X, GUI_Y, hex, TINT_LIFT), top = images.getTinted(BUTTON_X+CELL, GUI_Y, hex, TINT_LIFT);
        Image left = images.getTinted(BUTTON_X+2*CELL, GUI_Y, hex, TINT_LIFT), middle = images.getTinted(BUTTON_X+3*CELL, GUI_Y, hex, TINT_LIFT);
        double b = Math.min(border, Math.min(w, h)/2);
        drawCell(gc, middle, 0, 0, x+b, y+b, w-2*b, h-2*b, false, false);
        drawCell(gc, top, 0, 0, x+b, y, w-2*b, b, false, false);
        drawCell(gc, top, 0, 0, x+b, y+h-b, w-2*b, b, false, true);
        drawCell(gc, left, 0, 0, x, y+b, b, h-2*b, false, false);
        drawCell(gc, left, 0, 0, x+w-b, y+b, b, h-2*b, true, false);
        drawCell(gc, corner, 0, 0, x, y, b, b, false, false);
        drawCell(gc, corner, 0, 0, x+w-b, y, b, b, true, false);
        drawCell(gc, corner, 0, 0, x, y+h-b, b, b, false, true);
        drawCell(gc, corner, 0, 0, x+w-b, y+h-b, b, b, true, true);
    }
    private static final double TINT_LIFT = 0.35;

    private void nineSlice (GraphicsContext gc, int cellX, double x, double y, double w, double h) {
        Image img = handler.getImages().getImage(0);
        double b = Math.min(border, Math.min(w, h)/2);
        int corner = cellX, top = cellX+CELL, left = cellX+2*CELL, middle = cellX+3*CELL;

        drawCell(gc, img, middle, GUI_Y, x+b, y+b, w-2*b, h-2*b, false, false);

        drawCell(gc, img, top, GUI_Y, x+b, y, w-2*b, b, false, false);
        drawCell(gc, img, top, GUI_Y, x+b, y+h-b, w-2*b, b, false, true);
        drawCell(gc, img, left, GUI_Y, x, y+b, b, h-2*b, false, false);
        drawCell(gc, img, left, GUI_Y, x+w-b, y+b, b, h-2*b, true, false);

        drawCell(gc, img, corner, GUI_Y, x, y, b, b, false, false);
        drawCell(gc, img, corner, GUI_Y, x+w-b, y, b, b, true, false);
        drawCell(gc, img, corner, GUI_Y, x, y+h-b, b, b, false, true);
        drawCell(gc, img, corner, GUI_Y, x+w-b, y+h-b, b, b, true, true);
    }

    public void drawSlot (GraphicsContext gc, double x, double y, double size, boolean selected) {
        drawCell(gc, selected ? SLOT_SELECTED_X : SLOT_X, GUI_Y, x, y, size, size);
    }

    //An empty slot hints at what goes in it with a white shape of that kind of item (not a real item)
    public void drawSlotHint (GraphicsContext gc, inventory.slotKind kind, double x, double y, double size) {
        int cellX;
        switch (kind) {
            case ROCK: cellX = ROCK_X; break;
            case ITEM: cellX = POTION_X; break;
            case TRINKET: cellX = HEART_X; break;
            default:   return;
        }
        Image shape = handler.getImages().getSilhouette(cellX, GUI_Y, "#FFFFFF");
        gc.setGlobalAlpha(hintOpacity);
        gc.drawImage(shape, 0, 0, CELL, CELL, Math.round(x + size*0.1), Math.round(y + size*0.1), Math.round(size*0.8), Math.round(size*0.8));
        gc.setGlobalAlpha(1);
    }

    private double hintOpacity = 0.7;
}
