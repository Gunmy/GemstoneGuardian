package game;

//A lightning strike out in the world (out of the player), made like the GUI ones (guiLightning) but fixed to a place
//in the world: the bolt is made in screen pixels around (0, 0) when it starts, and drawn moved to wherever that place
//is on screen now, scaled with the zoom, so it stays put while the camera moves
public class worldLightning {

    private int world;
    private double x, y;
    private guiLightning bolt;
    private double unitAtStart;

    //(x, y) in the world is the bolt's (0, 0); unitAtStart is gameHandler.getUnitWidth() when it was made
    public worldLightning (int world, double x, double y, guiLightning bolt, double unitAtStart) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.bolt = bolt;
        this.unitAtStart = unitAtStart;
    }

    public void next (double deltaTime) {bolt.next(deltaTime);}
    public boolean isDead () {return bolt.isDead();}

    public void draw (gameHandler handler) {
        if (handler.getCurrentWorld() != world) return;
        camera cam = handler.getCamera();
        double sx = handler.getMidWIDTH() + (x - cam.getX())*handler.getUnitWidth();
        double sy = handler.getMidHEIGHT() + (y - cam.getY())*handler.getUnitHeight();
        double scale = handler.getUnitWidth()/unitAtStart;
        handler.getTempGc().save();
        handler.getTempGc().translate(sx, sy);
        handler.getTempGc().scale(scale, scale);
        bolt.draw(handler.getTempGc());
        handler.getTempGc().restore();
    }
}
