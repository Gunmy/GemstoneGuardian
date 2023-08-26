package game;

import javafx.scene.transform.Rotate;

public abstract class baseParticle implements cordinate, entity {

    protected double x, y, lifeTime, spinSpeed, startWidth, startHeight, growthConstant;
    private int world;

    private double moveX, moveY;
    private double spawn = System.nanoTime();
    protected gameHandler handler;

    public baseParticle (double x, double y, int world, double lifeTime, double spinSpeed, double startWidth, double startHeight, double growthConstant, gameHandler handler, double moveX, double moveY) {
        if (handler.getWorld(world) == null) throw new IllegalArgumentException("Verden finnes ikke");
        this.x = x;
        this.y = y;
        this.world = world;
        this.spinSpeed = spinSpeed;
        this.lifeTime = lifeTime;
        this.startWidth = startWidth;
        this.startHeight = startHeight;
        this.growthConstant = growthConstant;
        this.handler = handler;
        this.moveX = moveX;
        this.moveY = moveY;

    }

    private double getTimeAlive () {
        return (System.nanoTime()-spawn)/1_000_000_000;
    }

    public void move (double deltaTime) {
        x += moveX/lifeTime*deltaTime;
        y += moveY/lifeTime*deltaTime;
    }

    protected double getCurrentGrowth () {
        return 1 + growthConstant*getTimeAlive()/lifeTime;
    }

    public boolean isDead () {
        return lifeTime - getTimeAlive() <= 0;
    }

    public void draw () {
        if (this.world == handler.getCurrentWorld()) {
            double unitWidth = handler.getUnitWidth();
            double unitHeight = handler.getUnitHeight();
    
            double rendX = (x-handler.getCamera().getX())*unitWidth+handler.getMidWIDTH();
            double rendY = (y-handler.getCamera().getY())*unitHeight+handler.getMidHEIGHT();
    
            handler.getTempGc().save();
            Rotate r = new Rotate(360/lifeTime*getTimeAlive()*spinSpeed, rendX, rendY);
            
            double opacity = (lifeTime-getTimeAlive())/(lifeTime/2);
            if (opacity > 0.9) {opacity = 0.9;}

            handler.getTempGc().setGlobalAlpha(opacity);

            handler.getTempGc().setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
            
            drawSpecial(rendX, rendY);
    
            handler.getTempGc().restore();
        }

    }

    protected abstract void drawSpecial (double rendX, double rendY);


    @Override
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public int getWorld() {
        return this.world;
    }
    
}
