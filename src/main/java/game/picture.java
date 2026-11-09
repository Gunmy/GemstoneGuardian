package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

//Something that can be shown in a GUI box (dialogue pictures and the like) and behaves like it does in the game:
//characters bounce, items give off their particles, waving tiles wave. Implemented by character, item, tileType and abilityType
public interface picture {

    void draw (GraphicsContext gc, gui gui, double x, double y, double size);

    //A picture that is just a still image of one sheet cell
    static picture of (Image image) {
        return (gc, gui, x, y, size) -> gc.drawImage(image, 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, Math.round(x), Math.round(y), Math.round(size), Math.round(size));
    }
}
