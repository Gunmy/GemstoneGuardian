package game;

public class finishGameEvent implements event {
    gameHandler handler;

    public finishGameEvent (gameHandler handler) {
        this.handler = handler;
    }

    @Override
    public void trigger() {
        handler.finish();
    }
    
}
