package game;

import java.util.function.Consumer;

public class fluidType extends tileType {
    private double speed;

    private double greenWeight, blueWeight, redWeight;
    private shader shader;
    private boolean inverse;

    public fluidType(String name, int x, int y, int width, int height, int id, boolean block,
            Consumer<character> pressurePlate, double speed, double redWeight, double greenWeight, double blueWeight, shader shader, boolean inverse) {
        super(name, x, y, width, height, id, block, pressurePlate);
        this.speed = speed;
        this.greenWeight = greenWeight;
        this.redWeight = redWeight;
        this.blueWeight = blueWeight;
        this.shader = shader;
        this.inverse = inverse;
    }

    protected void drawSpecial (gameHandler handler, double realx, double realy, int x, int y) {

        
        //double time = System.nanoTime()/1_000_000_000.0;

        int correctionVal =0;
        if (handler.getTempCanvas().getOpacity() == 1) {
            correctionVal = 1;
        }

        /*
        int val = (int) (1/speed) + 1;

        handler.getTempGc().drawImage(handler.getImages().getImage(0), 
        this.x+this.width*(1-(time%val)/val), 
        this.y+this.height*(1-(time%val)/val), 
        this.width, this.height, 
         (int) realx,   (int) realy, 
         (int) (handler.getUnitWidth()/handler.getTileCount()+correctionVal), 
         (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal));
        */
        shader.drawShader(x, y, realx, realy, (int) (handler.getUnitWidth()/handler.getTileCount()+correctionVal), (int) (handler.getUnitHeight()/handler.getTileCount()+correctionVal), 0, redWeight, greenWeight, blueWeight, speed, inverse);
    }    
}
