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

    //Fading in over its first moment as well as out over its second half, so it never pops into view
    @Override
    protected double opacity () {
        double in = Math.min(0.06, lifeTime*0.2);
        return Math.min(super.opacity(), in <= 0 ? 1 : getTimeAlive()/in);
    }
    //An item instead, drawn as a slot draws it (slotGrid.drawStack: outline, icon, glow), as wide as the particle
    private item item;
    public battleParticle item (item item) {this.item = item; return this;}
    //Gives off no light in the dark (most of a beam's specks: a few lights are enough, see battleAnimation.beam)
    private boolean noLight = false;
    public battleParticle noLight () {this.noLight = true; return this;}
    public battleParticle text (String text) {this.text = text; return this;}
    //Words and numbers (LUCKY!, -12) are drawn after every other effect, so a beam never covers them (gameHandler)
    public boolean isText () {return this.text != null;}
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

    //Its light while a move darkens the world (gameHandler.drawMood): a soft round light in its colour (mixed with
    //white) drawn into the light map lights, which is scale times the size of the screen and blended so the brightest
    //light wins: two lights on the same spot are no brighter than the brighter one. strength (0 to 1) is how much the
    //mood lets the moves shine. Numbers and pictures (the thrown rocks) give off no light, their trails do
    public static final double LIGHT_REACH = 5, LIGHT_MIN_TILES = 0.45, LIGHT_WHITE = 0.45;

    public void light (javafx.scene.canvas.GraphicsContext lights, double scale, double strength) {
        if (noLight || text != null || image != null || item != null || getWorld() != handler.getCurrentWorld()) return;
        //Black (a black hole's core) gives off no light
        if (color.getBrightness() < 0.1) return;
        double a = opacity()*strength;
        if (a <= 0.01) return;
        double tile = handler.getUnitWidth()/handler.getTileCount();
        double size = startWidth*getCurrentGrowth()*handler.getUnitWidth();
        double r = Math.max(LIGHT_MIN_TILES*tile, size*LIGHT_REACH)*scale;
        double cx = screenX()*scale, cy = screenY()*scale;
        if (cx + r < 0 || cy + r < 0 || cx - r > lights.getCanvas().getWidth() || cy - r > lights.getCanvas().getHeight()) return;
        Color shine = color.interpolate(Color.WHITE, LIGHT_WHITE);
        lights.setFill(new javafx.scene.paint.RadialGradient(0, 0, cx, cy, r, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.color(shine.getRed(), shine.getGreen(), shine.getBlue(), a)),
            new javafx.scene.paint.Stop(1, Color.color(shine.getRed(), shine.getGreen(), shine.getBlue(), 0))));
        lights.fillOval(cx - r, cy - r, 2*r, 2*r);
    }

    //Spiralling into a point instead of flying (a black hole's pull, battleAnimation.vortex): round (cx, cy) from
    //radius r0 at angle a0 to the middle over its life, turning turns times, the ring squashed to flat (0 to 1) of its
    //width so it lies on the ground
    private boolean spiral = false;
    private double scx, scy, r0, a0, turns, flat, age;
    public battleParticle spiral (double cx, double cy, double r0, double a0, double turns, double flat) {
        this.spiral = true;
        this.scx = cx;
        this.scy = cy;
        this.r0 = r0;
        this.a0 = a0;
        this.turns = turns;
        this.flat = flat;
        return this;
    }

    @Override
    public void move (double deltaTime) {
        if (spiral) {
            age += deltaTime;
            double f = Math.min(1, age/lifeTime);
            //Faster and faster as it falls in
            double r = r0*(1 - f*f), a = a0 + turns*2*Math.PI*f*f;
            x = scx + Math.cos(a)*r;
            y = scy + Math.sin(a)*r*flat;
            return;
        }
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
                    if (dx != 0 || dy != 0) guiText.draw(handler.getTempGc(), text, rendX + dx*o, top + dy*o, size, outline, javafx.scene.text.TextAlignment.CENTER, true);
                }
            }
            guiText.draw(handler.getTempGc(), text, rendX, top, size, color, javafx.scene.text.TextAlignment.CENTER);
            return;
        }
        if (item != null) {
            javafx.scene.canvas.GraphicsContext gc = handler.getTempGc();
            double x = rendX - size/2, y = rendY - size/2;
            item.drawGlowOutline(gc, x, y, size);
            item.drawIcon(gc, x, y, size);
            item.drawGlowOver(gc, x, y, size);
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
