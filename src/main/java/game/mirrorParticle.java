package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

//The Mirror Shard's particles (battleAnimation.mirrorCopy), two kinds:
//- a mirror (reflection null): the game's own mirror (the mirror tiles' texture), standing. It opens in at the start
//  of its life and folds away at the end
//- a reflection: a copy of a character's sprite in the mirror's cold blue, turned round left to right as a mirror
//  image is, flying from where it starts to (toX, toY) over its life, and fading out right at the end where it hits
//Both drawn with nearest neighbour, in whole screen pixels
public class mirrorParticle extends baseParticle {

    //The mirror's texture on the sheet (the mirror tiles, tileTypes 194-198)
    private static final int MIRROR_X = 260, MIRROR_Y = 360;

    private final Image reflection;
    private double startX, startY, toX, toY;
    private final double height;
    private double age;

    //A mirror standing on (x, y), size wide and twice as tall (world units)
    public mirrorParticle (gameHandler handler, int world, double x, double y, double size, double life) {
        super(x, y, world, life, 0, size, size, 0, handler, 0, 0);
        this.reflection = null;
        this.startX = x;
        this.startY = y;
        this.toX = x;
        this.toY = y;
        this.height = size*2;
    }

    //A reflection: picture (a character's sprite, tinted) size big, from (x, y) to (toX, toY) over life seconds
    public mirrorParticle (gameHandler handler, int world, double x, double y, double toX, double toY, double size, double life, Image picture) {
        super(x, y, world, life, 0, size, size, 0, handler, 0, 0);
        this.reflection = picture;
        this.startX = x;
        this.startY = y;
        this.toX = toX;
        this.toY = toY;
        this.height = size;
    }

    //In a mirror world its path is turned round too
    @Override
    public void mirror (double axis) {
        super.mirror(axis);
        startX = 2*axis - startX;
        toX = 2*axis - toX;
    }

    @Override
    public void move (double deltaTime) {
        age += deltaTime;
        if (reflection == null) return;
        //Slow out of the mirror, then faster and faster at them
        double f = Math.min(1, age/lifeTime);
        double eased = f*f;
        x = startX + (toX - startX)*eased;
        y = startY + (toY - startY)*eased;
    }

    //Mirrors and reflections are not see-through until their very end
    @Override
    protected double opacity () {
        double left = lifeTime - age;
        return Math.max(0, Math.min(1, left/(lifeTime*0.15)));
    }

    @Override
    protected void drawSpecial (double rendX, double rendY) {
        GraphicsContext gc = handler.getTempGc();
        double unit = handler.getUnitWidth();
        if (reflection != null) {
            double s = Math.round(startWidth*unit);
            //Turned round, as in a mirror, if it flies to the right; facing the way it goes
            boolean flip = toX > startX;
            double left = Math.round(rendX - s/2), top = Math.round(rendY - s/2);
            if (flip) gc.drawImage(reflection, 0, 0, guiSkin.CELL, guiSkin.CELL, left + s, top, -s, s);
            else gc.drawImage(reflection, 0, 0, guiSkin.CELL, guiSkin.CELL, left, top, s, s);
            return;
        }
        //The mirror: the game's own mirror (the tile's texture, MIRROR_X/MIRROR_Y on the sheet), twice its size wide
        //and tall, standing on its spot, opening in and folding away like a door
        double f = Math.min(1, age/lifeTime);
        double open = Math.min(1, f/0.15)*Math.min(1, (1 - f)/0.15);
        double size = Math.round(height*unit), w = Math.round(size*open);
        if (w < 2) return;
        gc.drawImage(handler.getImages().getImage(0), MIRROR_X, MIRROR_Y, guiSkin.CELL, guiSkin.CELL,
            Math.round(rendX - w/2), Math.round(rendY - size), w, size);
    }
}
