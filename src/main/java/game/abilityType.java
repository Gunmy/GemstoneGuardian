package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

//What kind of thing an ability does. Each type has an icon in the icon row of tiles.png (guiSkin.ICON_Y),
//drawn grey-scale and tinted with the type's colour, shown on the ability buttons in battle
public enum abilityType implements picture {
    ATTACK   (0,   "#E8E8E8"),  //plain damage
    HEAL     (100, "#FF4060"),  //heals or heals over time
    FIRE     (200, "#FF7020"),  //sets on fire
    POISON   (300, "#60D040"),  //poisons
    CONFUSE  (400, "#D060FF"),  //confuses, lowers hit chance
    LIGHTNING(500, "#FFE040"),  //damage by share of max health
    DRAIN    (600, "#C03060"),  //hurts them and heals you
    CLEANSE  (700, "#60C0FF"),  //removes effects
    RESTORE  (800, "#FFFFFF");  //restores ability uses

    private final int cellX;
    private final String tint;

    abilityType (int cellX, String tint) {
        this.cellX = cellX;
        this.tint = tint;
    }

    public Image getIcon (imgDict images) {
        return images.getTinted(cellX, guiSkin.ICON_Y, tint);
    }

    @Override
    public void draw (GraphicsContext gc, gui gui, double x, double y, double size) {
        picture.of(getIcon(gui.getHandler().getImages())).draw(gc, gui, x, y, size);
    }
}
