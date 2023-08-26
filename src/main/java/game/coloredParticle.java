package game;

import javafx.scene.paint.Color;

public class coloredParticle extends baseParticle {
    private String HEX;
    public coloredParticle(double x, double y, int world, double lifeTime, double spinSpeed, double startWidth,
            double startHeight, double growthConstant, gameHandler handler, double moveX, double moveY, String HEX) {
        super(x, y, world, lifeTime, spinSpeed, startWidth, startHeight, growthConstant, handler, moveX, moveY);
        this.HEX = HEX;
    }

    public void drawSpecial (double rendX, double rendY) {
        double unitWidth = handler.getUnitWidth();
        double unitHeight = handler.getUnitHeight();
        handler.getTempGc().setFill(Color.web(HEX));
        handler.getTempGc().fillRect(
            -startWidth/2*getCurrentGrowth()*unitWidth+rendX, 
            -startHeight/2*getCurrentGrowth()*unitHeight+rendY, 
            unitWidth*startWidth*getCurrentGrowth(), unitHeight*startHeight*getCurrentGrowth()
        );
    }
    
}
