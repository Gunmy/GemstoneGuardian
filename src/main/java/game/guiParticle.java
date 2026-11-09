package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//A little square that lives on the GUI layer, in screen pixels. Fades out over its lifetime
public class guiParticle {

    private double x, y, vx, vy, life, age, size;
    private Color color;

    public guiParticle (double x, double y, double vx, double vy, double life, double size, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.size = size;
        this.color = color;
    }

    public void next (double deltaTime) {
        age += deltaTime;
        x += vx*deltaTime;
        y += vy*deltaTime;
    }

    public boolean isDead () {return age >= life;}

    public void draw (GraphicsContext gc) {
        double opacity = Math.max(0, 1 - age/life);
        gc.setGlobalAlpha(opacity);
        gc.setFill(color);
        gc.fillRect(Math.round(x - size/2), Math.round(y - size/2), Math.ceil(size), Math.ceil(size));
        gc.setGlobalAlpha(1);
    }
}
