package game;

public class camera {
    private double x = 10000;
    private double y = 10000;
    private gameHandler handler;
    private cordinate bind;

    private long time = System.nanoTime();


    public camera (gameHandler handler, cordinate object) {
        this.handler = handler;
        this.bind = object;
    }   

    public double getX () {
        return this.x;
    }

    public void setX (double x) {
        this.x = x;
    }

    public void changeX (double change) {
        this.x += change;
    }

    public double getY () {
        return this.y;
    }

    public void setY (double y) {
        this.y = y;
    }

    public void changeY (double change) {
        this.y += change;
    }

    

    public double getRenderDistance () {
        return handler.getWorld(getBound().getWorld()).getRenderDistance();
    }

    public double inRendered (double rendx, double rendy) {
        return Math.pow((getRenderDistance()*handler.getUnitHeight()*2-getDistance(rendx, rendy))/(getRenderDistance()*handler.getUnitHeight()), 5);

    }

    public double getDistance (double rendx, double rendy) {
        return Math.sqrt(Math.pow(rendx-handler.getMidWIDTH(), 2)+Math.pow(rendy-handler.getMidHEIGHT(), 2));
    }

    public boolean bindTo (cordinate object) {
        if (bindTimeOver()) {
            this.bind = object;
            return true;
        }
        return false;
    }

    public boolean bindTimeOver () {
        return System.nanoTime() > time;
    }
    public cordinate getBound () {return this.bind;}
    public boolean isBound () {return this.bind != null;}

    public void setBindTime (double seconds) {
        time = System.nanoTime() + (long) seconds * 1_000_000_000;
    }

    public void move (Double deltaTime) {

        int tc = handler.getTileCount();

        double xDiff = getBound().getX()+0.5/tc-getX();
        double yDiff = getBound().getY()+0.5/tc-getY();
        
        double perc = 0.1;

        double maxMov = 0.2;

        double xMove = xDiff*perc;
        if (xMove > maxMov) {xMove = maxMov;}
        else if (xMove < -maxMov) {xMove = -maxMov;}

        double yMove = yDiff*perc;
        if (yMove > maxMov) {yMove = maxMov;}
        else if (yMove < -maxMov) {yMove = -maxMov;}

        changeX(xMove);
        changeY(yMove);
    }


}
