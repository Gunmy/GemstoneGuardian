package game;

public class deltaTime {

    private long lastTime = System.nanoTime();
    private long deltaTime;
    
    public void next () {
        long time = System.nanoTime();
        deltaTime = ((time - lastTime) / 1000000);
        lastTime = time;
    }

    public Double getSeconds () {
        return (((double) deltaTime)/1000);
    }
}
