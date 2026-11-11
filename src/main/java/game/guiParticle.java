package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//A little square that lives on the GUI layer, in screen pixels. Fades out over its lifetime
public class guiParticle {

    private double x, y, vx, vy, life, age, size;
    private Color color;

    //A throw on top of the particle's own drift (vx, vy): it gets it from the item it came off being swung around,
    //and loses it to air drag, so the particle flies off in the swing and then goes back to drifting
    private double flingX, flingY;
    private static final double FLING_DRAG = 3;

    //Optional physics: gravity pulls it down (pixels per second squared), and it spins (radians per second).
    //Spinning ones are drawn as a turned rectangle, shaped by aspect (height/width)
    private double gravity, spin, angle, aspect = 1;
    //Spinning ones stay solid and only fade over the last part of their life
    private boolean fadeLate = false;

    //A little pixel picture instead of a square: every true cell is a square of size pixels, centred on the particle.
    //Made in code, no texture
    private boolean[][] shape;

    public static final boolean[][] HEART = pixels(
        ".X.X.",
        "XXXXX",
        "XXXXX",
        ".XXX.",
        "..X..");

    //Rows of X (filled) and . (empty), all the same length
    public static boolean[][] pixels (String... rows) {
        boolean[][] cells = new boolean[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            cells[r] = new boolean[rows[r].length()];
            for (int c = 0; c < rows[r].length(); c++) cells[r][c] = rows[r].charAt(c) == 'X';
        }
        return cells;
    }

    public guiParticle shape (boolean[][] shape) {
        this.shape = shape;
        return this;
    }

    public guiParticle physics (double gravity, double spin, double angle, double aspect) {
        this.gravity = gravity;
        this.spin = spin;
        this.angle = angle;
        this.aspect = aspect;
        this.fadeLate = true;
        return this;
    }

    public guiParticle (double x, double y, double vx, double vy, double life, double size, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.size = size;
        this.color = color;
    }

    public guiParticle fling (double flingX, double flingY) {
        this.flingX = flingX;
        this.flingY = flingY;
        return this;
    }

    public void next (double deltaTime) {
        age += deltaTime;
        double drag = Math.exp(-FLING_DRAG*deltaTime);
        flingX *= drag;
        flingY *= drag;
        vy += gravity*deltaTime;
        angle += spin*deltaTime;
        x += (vx + flingX)*deltaTime;
        y += (vy + flingY)*deltaTime;
    }

    public boolean isDead () {return age >= life;}

    public void draw (GraphicsContext gc) {
        double left = 1 - age/life;
        double opacity = Math.max(0, fadeLate ? Math.min(1, left*3) : left);
        gc.setGlobalAlpha(opacity);
        gc.setFill(color);
        if (shape != null) {
            double px = Math.max(1, Math.round(size));
            double left0 = Math.round(x - px*shape[0].length/2), top0 = Math.round(y - px*shape.length/2);
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c]) gc.fillRect(left0 + c*px, top0 + r*px, px, px);
                }
            }
        } else if (spin == 0 && aspect == 1) {
            gc.fillRect(Math.round(x - size/2), Math.round(y - size/2), Math.ceil(size), Math.ceil(size));
        } else {
            gc.save();
            gc.translate(x, y);
            gc.rotate(Math.toDegrees(angle));
            double w = Math.max(1, size), h = Math.max(1, size*aspect);
            gc.fillRect(-w/2, -h/2, w, h);
            gc.restore();
        }
        gc.setGlobalAlpha(1);
    }
}
