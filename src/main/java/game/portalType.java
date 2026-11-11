package game;

import java.util.function.Consumer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//A portal tile: a hole into the starry dark of a teleport. It is black, and the stars showing through it are the
//teleport's own (screenTransition), fixed to the screen: walking past, the world moves and the stars stay where they
//are, so they look far away behind it. Nothing else is drawn under it, so it also fades out cleanly in the dark. Its
//cell in tiles.png is only used for the small maps (teleports menu)
public class portalType extends tileType {

    public portalType (String name, int x, int y, int width, int height, int id, boolean block, Consumer<character> pressurePlate) {
        super(name, x, y, width, height, id, block, pressurePlate);
    }

    //A portal that is off until it is lit (the one behind the girl, lit by gameHandler every frame from portalLit()):
    //until then only black, no stars, and solid like a wall
    private boolean offUntilLit = false, lit = true;
    public portalType offUntilLit () {
        this.offUntilLit = true;
        this.lit = false;
        return this;
    }
    public void setLit (boolean lit) {this.lit = !offUntilLit || lit;}

    @Override
    public boolean isBlocked (double xChange, double yChange) {
        return !lit || super.isBlocked(xChange, yChange);
    }

    @Override
    protected void drawSpecial (gameHandler handler, double realx, double realy, int x, int y) {
        GraphicsContext gc = handler.getTempGc();
        //Whole pixels from the tile edges, so neighbouring portal tiles leave no gaps
        double x0 = Math.floor(realx), y0 = Math.floor(realy);
        double x1 = Math.floor(realx + handler.getUnitWidth()/handler.getTileCount());
        double y1 = Math.floor(realy + handler.getUnitHeight()/handler.getTileCount());
        double alpha = gc.getGlobalAlpha();
        gc.setFill(Color.BLACK);
        gc.fillRect(x0, y0, x1 - x0, y1 - y0);
        if (lit) screenTransition.drawStars(handler, gc, x0, y0, x1 - x0, y1 - y0, alpha);
        gc.setGlobalAlpha(alpha);
    }
}
