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

    //Where the camera looks, with this frame's shake on top (a big hit in battle, see battleAnimation.quake)
    public double getX () {
        return this.x + shakeX;
    }

    //Shaken this frame by so much (world units); cleared every frame before the animations play (gameHandler.next)
    private double shakeX, shakeY;
    public void shake (double dx, double dy) {shakeX += dx; shakeY += dy;}
    public void clearShake () {shakeX = 0; shakeY = 0;}

    public void setX (double x) {
        this.x = x;
    }

    public void changeX (double change) {
        this.x += change;
    }

    public double getY () {
        return this.y + shakeY;
    }

    public void setY (double y) {
        this.y = y;
    }

    public void changeY (double change) {
        this.y += change;
    }

    

    //How far is lit around what the camera follows, in chunks. In a dark world a character's goggles see further
    public double getRenderDistance () {
        chunks world = handler.getWorld(getBound().getWorld());
        double distance = world.getRenderDistance();
        if (world.isDark() && getBound() instanceof character) distance += ((character) getBound()).getSightBonus()/(double) handler.getTileCount();
        return distance;
    }

    public double inRendered (double rendx, double rendy) {
        return Math.pow((getRenderDistance()*handler.getFadeUnit()*2-getDistance(rendx, rendy))/(getRenderDistance()*handler.getFadeUnit()), 5);

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

        //From where it really is, not where the shake shows it
        double xDiff = getBound().getX()+0.5/tc-this.x;
        double yDiff = getBound().getY()+0.5/tc-this.y;
        
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
