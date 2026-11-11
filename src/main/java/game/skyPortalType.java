package game;

import java.util.function.Consumer;

//A sky portal: a 1x1 window into the sky (skyLayer.drawWindow: blue, with clouds fixed to the screen drifting by).
//Like the cave passages it sits on the same tile in both worlds: stepping on it takes you to the other world, onto the
//portal there, and stepping off and on again takes you back
public class skyPortalType extends tileType {

    public skyPortalType (String name, int x, int y, int width, int height, int id, boolean block, Consumer<character> pressurePlate) {
        super(name, x, y, width, height, id, block, pressurePlate);
    }

    //A sky portal that is off until it is lit (the ones leading up into the sky ruins, lit by gameHandler every frame
    //from skyPortalLit: once the Desert Nomad is beaten). Until then it is plain sand, and does nothing
    private boolean offUntilLit = false, lit = true;
    private tileType offLook;
    public skyPortalType offUntilLit (tileType offLook) {
        this.offUntilLit = true;
        this.lit = false;
        this.offLook = offLook;
        return this;
    }
    public void setLit (boolean lit) {this.lit = !offUntilLit || lit;}
    public boolean isLit () {return this.lit;}

    @Override
    public void doFunction (character character) {
        if (lit) super.doFunction(character);
    }

    @Override
    protected void drawSpecial (gameHandler handler, double realx, double realy, int x, int y) {
        if (!lit) {
            offLook.drawLayer(handler, realx, realy, x, y, 0);
            return;
        }
        double x0 = Math.floor(realx), y0 = Math.floor(realy);
        double x1 = Math.floor(realx + handler.getUnitWidth()/handler.getTileCount());
        double y1 = Math.floor(realy + handler.getUnitHeight()/handler.getTileCount());
        handler.getSkyLayer().drawWindow(handler.getTempGc(), x0, y0, x1 - x0, y1 - y0, handler.getTempGc().getGlobalAlpha());
    }
}
