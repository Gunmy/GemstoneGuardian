package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

//An item's outline jumping out from it and fading: drawn around (cx, cy), growing from the item's size to `grow`
//times it over its life. A negative start age delays it, for a second ring after the first. Used when an item turns
//into another (itemInteractions.changeBurst)
public class guiOutlinePulse {

    private Image outline;
    private double cx, cy, size, age, life, grow, strength;

    public guiOutlinePulse (Image outline, double cx, double cy, double size, double life, double grow, double strength, double delay) {
        this.outline = outline;
        this.cx = cx;
        this.cy = cy;
        this.size = size;
        this.life = life;
        this.grow = grow;
        this.strength = strength;
        this.age = -delay;
    }

    public void next (double deltaTime) {age += deltaTime;}
    public boolean isDead () {return age >= life;}

    public void draw (GraphicsContext gc) {
        if (age < 0) return;
        double t = age/life;
        //Out fast, then slowing down, while fading
        double eased = 1 - (1 - t)*(1 - t);
        double s = size*(1 + (grow - 1)*eased);
        gc.save();
        gc.setImageSmoothing(false);
        gc.setGlobalAlpha(Math.max(0, strength*(1 - t)));
        gc.drawImage(outline, 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, Math.round(cx - s/2), Math.round(cy - s/2), Math.round(s), Math.round(s));
        gc.restore();
    }
}
