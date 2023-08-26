package game;

import java.util.List;
import java.util.Random;
import javafx.scene.paint.Color;

public class eventListener implements cordinate {
    private int world;
    private double x, y;
    private event event;
    private double radius;
    private gameHandler handler;

    private List<dialogue> dialog;

    public eventListener (gameHandler handler, double x, double y, double radius, int world, event event) {
        if (handler.getWorld(world) == null) throw new IllegalArgumentException("Verdenen finnes ikke");
        this.world = world;
        this.x = x;
        this.y = y;
        this.event = event;
        this.radius = radius;
        this.handler = handler;
    }

    public eventListener (gameHandler handler, double x, double y, double radius, int world, event event, List<dialogue> dialog) {
        this(handler, x, y, radius, world, event);
        this.dialog = dialog;
    }

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

    public boolean testForTrigger () {
        character c = handler.getMainCharacter();
        if (c.getWorld() == world && Math.sqrt(Math.pow(c.getX()-getX(), 2)+Math.pow(c.getY()-getY(), 2)) <= radius) {
            if (event != null) event.trigger();
            if (dialog != null && dialog.size() > 0) {
                for (dialogue d : dialog) {
                    this.handler.getDiealoguecontainer().add(d);
                }

                if (!handler.getDiealoguecontainer().isActive()) {
                    handler.getDiealoguecontainer().next();
                }
            }
            return true;
        }

        return false;
    }

    public void draw () {
        if (this.world == handler.getCurrentWorld()) {
            Random rdm = new Random();
            if (handler.infoOn()) {
                camera cam = handler.getCamera();
                double renderx = handler.getMidWIDTH() + ((x-cam.getX()))*handler.getUnitWidth();
                double rendery = handler.getMidHEIGHT() + ((y-cam.getY()))*handler.getUnitHeight();
        
                handler.getTempGc().setGlobalAlpha(0.2);
                handler.getTempGc().setFill(Color.YELLOW);
                handler.getTempGc().fillOval(
                    renderx-radius*handler.getUnitWidth()+handler.getUnitWidth()/handler.getTileCount()/2, 
                    rendery-radius*handler.getUnitHeight()+handler.getUnitHeight()/handler.getTileCount()/2, 
                    2*radius*handler.getUnitWidth(), 
                    2*radius*handler.getUnitHeight());
                handler.getTempGc().setGlobalAlpha(1);
                    handler.getTempGc().strokeOval(
                        renderx-radius*handler.getUnitWidth()+handler.getUnitWidth()/handler.getTileCount()/2-1, 
                        rendery-radius*handler.getUnitHeight()+handler.getUnitHeight()/handler.getTileCount()/2-1, 
                        2*radius*handler.getUnitWidth()+2, 
                        2*radius*handler.getUnitHeight()+2);
            }
        
            if (rdm.nextDouble() < 0.2) {
                coloredParticle particle;
                if (event instanceof giveEvent) {
                    particle = new coloredParticle(getX()+0.055, getY()+0.055, handler.getCurrentWorld(), 1, 1, 0.005, 0.005, 1, handler, (rdm.nextDouble()-0.5)/3, (-rdm.nextDouble())/10, "#A519B5");
                } else if (event instanceof battleEvent) {
                    particle = new coloredParticle(getX()+0.055, getY()+0.055, handler.getCurrentWorld(), 1, 1, 0.005, 0.005, 1, handler, (rdm.nextDouble()-0.5)/3, (rdm.nextDouble()-0.5)/3, "#FF0000");
                } else {
                    particle = new coloredParticle(getX()+0.055, getY()+0.055, handler.getCurrentWorld(), 1, 1, 0.005, 0.005, 1, handler, (rdm.nextDouble()-0.5)/3, (rdm.nextDouble()-0.5)/3, "#FFF600");
                }               
                handler.addFrontParticle(particle);
            }
        }
    }
}
