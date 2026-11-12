package game;

import javafx.scene.paint.Color;

//A particle of a move's animation in battle (battleAnimation), out in the world. Unlike coloredParticle it has a real
//velocity (world units per second), gravity and air drag, so it can fly in an arc from one fighter and land on the
//other. Drawn as a square, or as a little pixel picture (guiParticle.HEART and the shapes in battleAnimation), made in
//code: no texture
public class battleParticle extends baseParticle {

    private Color color;
    private double vx, vy, gravity, drag;
    private boolean[][] shape;
    //A picture instead (a cell sized image, like a rock's tinted icon), drawn as wide as the particle
    private javafx.scene.image.Image image;
    //Text instead (a damage number), as tall as the particle, with a dark outline
    private String text;
    //Bounces off the ground at this y (NaN: no ground), keeping this share of its speed each time
    private double ground = Double.NaN, keep;

    //size is the particle's width in world units (a tile is 1/9)
    public battleParticle (gameHandler handler, int world, double x, double y, double vx, double vy, double life, double size, String hex) {
        super(x, y, world, life, 0, size, size, 0, handler, 0, 0);
        this.vx = vx;
        this.vy = vy;
        this.color = Color.web(hex);
    }

    public battleParticle gravity (double gravity) {this.gravity = gravity; return this;}
    public battleParticle drag (double drag) {this.drag = drag; return this;}
    public battleParticle shape (boolean[][] shape) {this.shape = shape; return this;}
    public battleParticle image (javafx.scene.image.Image image) {this.image = image; return this;}
    public battleParticle text (String text) {this.text = text; return this;}
    //Made this much bigger (battleAnimation.SIZE_SCALE); text keeps its size
    public battleParticle scale (double factor) {
        if (text == null) {
            startWidth *= factor;
            startHeight *= factor;
        }
        return this;
    }
    public battleParticle bounce (double ground, double keep) {this.ground = ground; this.keep = keep; return this;}
    //Turns this many times over its life
    public battleParticle spin (double turns) {this.spinSpeed = turns; return this;}
    //Grows (or shrinks, below 0) by this share of its size over its life
    public battleParticle grow (double growth) {this.growthConstant = growth; return this;}

    @Override
    public void move (double deltaTime) {
        double slow = Math.exp(-drag*deltaTime);
        vx *= slow;
        vy *= slow;
        vy += gravity*deltaTime;
        x += vx*deltaTime;
        y += vy*deltaTime;
        if (!Double.isNaN(ground) && y > ground && vy > 0) {
            y = ground;
            vy = -vy*keep;
        }
    }

    @Override
    protected void drawSpecial (double rendX, double rendY) {
        double size = startWidth*getCurrentGrowth()*handler.getUnitWidth();
        if (size <= 0) return;
        if (text != null) {
            double top = rendY - size/2;
            javafx.scene.paint.Color outline = javafx.scene.paint.Color.rgb(20, 20, 30);
            double o = Math.max(1, Math.round(size*0.08));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) guiText.draw(handler.getTempGc(), text, rendX + dx*o, top + dy*o, size, outline, javafx.scene.text.TextAlignment.CENTER);
                }
            }
            guiText.draw(handler.getTempGc(), text, rendX, top, size, color, javafx.scene.text.TextAlignment.CENTER);
            return;
        }
        if (image != null) {
            handler.getTempGc().drawImage(image, 0, 0, guiSkin.CELL, guiSkin.CELL, rendX - size/2, rendY - size/2, size, size);
            return;
        }
        handler.getTempGc().setFill(color);
        if (shape == null) {
            handler.getTempGc().fillRect(rendX - size/2, rendY - size/2, size, size);
            return;
        }
        //The picture as wide as the particle, every filled cell a square of it
        int rows = shape.length, cols = shape[0].length;
        double cell = size/cols;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (shape[r][c]) handler.getTempGc().fillRect(rendX - size/2 + c*cell, rendY - rows*cell/2 + r*cell, Math.ceil(cell), Math.ceil(cell));
            }
        }
    }
}
