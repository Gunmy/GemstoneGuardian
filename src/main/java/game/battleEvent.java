package game;

public class battleEvent implements event {

    private character you, them;
    private battle battleHandler;
    private event youWin;
    private event theyWin;

    private double x, y, r;
    private int world;

    public battleEvent (character you, character them, battle battleHandler, event youWin, event theyWin, double x, double y, double r, int world) {
        if (you == them) throw new IllegalArgumentException("Kan ikke slåss mot seg selv");
        this.you = you;
        this.them = them;
        this.battleHandler = battleHandler;
        this.youWin = youWin;
        this.theyWin = theyWin;
        this.x = x;
        this.y = y;
        this.r = r;
        this.world = world;
    }

    @Override
    public void trigger() {
        battleHandler.newBattle(you, them, youWin, theyWin, x, y, r, world);
    }
    
}
