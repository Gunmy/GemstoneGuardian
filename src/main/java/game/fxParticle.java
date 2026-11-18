package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//A move's effect that draws itself (battleAnimation's own kinds: cracks in the ground, growing crystals, shockwaves,
//shadows, pools, chains...): one thing on the screen for its life, drawn each frame by its painter from how far
//through its life it is. It is placed at (x, y) and may reach to (toX, toY), both in the world; in a mirror world
//both are turned round and dir is -1, so a painter that draws along dir or towards (toX, toY) comes out the right
//way round. Drawn in whole screen pixels with the painter's own alpha (no fade of its own). In the dark (a move's
//mood) it can light up round itself (lighter)
public class fxParticle extends baseParticle {

    //Draws it: f is how far through its life (0 to 1), age its seconds alive
    public interface painter {
        void paint (GraphicsContext gc, fxParticle p, double f, double age);
    }
    //Lights the dark round it (see glow), strength 0 to 1 from the mood
    public interface lighter {
        void light (GraphicsContext lights, fxParticle p, double f, double scale, double strength);
    }

    private final painter paint;
    private lighter lighting;
    private double toX, toY, age;
    private int dir = 1;
    //Anything the painter wants to keep (a crack's path, a crystal's points), made when it is
    public final java.util.Random rdm;
    public final long seed;

    public fxParticle (gameHandler handler, int world, double x, double y, double toX, double toY, double life, painter paint) {
        super(x, y, world, life, 0, 0, 0, 0, handler, 0, 0);
        this.toX = toX;
        this.toY = toY;
        this.paint = paint;
        this.seed = new java.util.Random().nextLong();
        this.rdm = new java.util.Random(seed);
    }

    public fxParticle light (lighter lighting) {this.lighting = lighting; return this;}

    //A fighter it stands in for (stretched, crumbling, hidden in a cloud): not drawn itself for as long as this lives,
    //from this frame on (gameHandler veils it again every frame)
    private character veiled;
    public fxParticle veils (character c) {
        this.veiled = c;
        c.veil();
        return this;
    }
    public character getVeiled () {return isDead() ? null : veiled;}

    @Override
    public void mirror (double axis) {
        super.mirror(axis);
        toX = 2*axis - toX;
        dir = -dir;
    }

    @Override
    public void move (double deltaTime) {age += deltaTime*battleFx.SPEED;}

    @Override
    public boolean isDead () {return age >= lifeTime;}

    public double getToX () {return toX;}
    public double getToY () {return toY;}
    public int getDir () {return dir;}
    public double getLife () {return lifeTime;}
    public double getAge () {return age;}
    //Lives on until that age (a stand-in the move keeps alive while it plays, see battleFx.noodle)
    public void liveUntil (double until) {lifeTime = Math.max(lifeTime, until);}
    public gameHandler getHandler () {return handler;}

    //World to screen, in whole pixels
    public double sx (double wx) {return Math.round((wx - handler.getCamera().getX())*handler.getUnitWidth() + handler.getMidWIDTH());}
    public double sy (double wy) {return Math.round((wy - handler.getCamera().getY())*handler.getUnitHeight() + handler.getMidHEIGHT());}
    //A length in the world (tiles are 1/tileCount) to screen pixels
    public double px (double w) {return w*handler.getUnitWidth();}
    //One pixel of the 20 pixel tile art, in screen pixels (at least 1): the size of a drawn pixel
    public double pixel () {return Math.max(1, Math.round(handler.getUnitWidth()/handler.getTileCount()/20));}

    //Nothing pops in or out: it fades in over FADE_IN seconds of its life and out over the last FADE_OUT (less when it
    //is short), times whatever see-through it draws with (alpha). A stand-in for a fighter stays solid, or the fighter
    //would blink see-through
    public static final double FADE_IN = 0.1, FADE_OUT = 0.2;
    public double envelope () {
        if (veiled != null) return 1;
        double in = Math.min(FADE_IN, lifeTime*0.3), out = Math.min(FADE_OUT, lifeTime*0.4);
        return Math.max(0, Math.min(1, Math.min(in <= 0 ? 1 : age/in, out <= 0 ? 1 : (lifeTime - age)/out)));
    }
    //Draws see-through by a (0 to 1) on top of the fade
    public void alpha (GraphicsContext gc, double a) {gc.setGlobalAlpha(Math.max(0, Math.min(1, a))*envelope());}

    @Override
    public void draw () {
        if (getWorld() != handler.getCurrentWorld()) return;
        GraphicsContext gc = handler.getTempGc();
        gc.save();
        gc.setImageSmoothing(false);
        gc.setGlobalAlpha(envelope());
        paint.paint(gc, this, Math.min(1, age/lifeTime), age);
        gc.restore();
    }

    @Override
    protected void drawSpecial (double rendX, double rendY) {}

    public void light (GraphicsContext lights, double scale, double strength) {
        if (lighting == null || getWorld() != handler.getCurrentWorld()) return;
        lighting.light(lights, this, Math.min(1, age/lifeTime), scale, strength);
    }

    //A soft round light at (wx, wy) in the world, radius tiles across, in this colour (mixed a little with white)
    public void glow (GraphicsContext lights, double scale, double wx, double wy, double radius, String hex, double alpha) {
        if (alpha <= 0.01) return;
        double r = radius/handler.getTileCount()*handler.getUnitWidth()*scale;
        double cx = sx(wx)*scale, cy = sy(wy)*scale;
        if (cx + r < 0 || cy + r < 0 || cx - r > lights.getCanvas().getWidth() || cy - r > lights.getCanvas().getHeight()) return;
        Color c = Color.web(hex).interpolate(Color.WHITE, battleParticle.LIGHT_WHITE);
        lights.setFill(new javafx.scene.paint.RadialGradient(0, 0, cx, cy, r, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(1, alpha))),
            new javafx.scene.paint.Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0))));
        lights.fillOval(cx - r, cy - r, 2*r, 2*r);
    }
}
