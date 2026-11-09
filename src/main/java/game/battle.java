package game;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

//The turn based fight. Holds the state only, the GUI (guiBattle) draws it and calls back in here
public class battle {

    private boolean activeBattle = false;

    private character you, them;

    private event youWin, theyWin;

    private boolean opponentDoneMove = true;

    //True while the player is choosing what to do
    private boolean waitingForPlayer = false;
    //True while the rocks and items are shown instead of the abilities
    private boolean moreView = false;

    //Health bars that slide towards the real health
    private double yourBar, theirBar;

    //To save the event in case player lose
    private double x, y, r;
    private int world;

    public battle () {
        resetBattle();
    }

    public character getTurnHaver () {
        if (opponentDoneMove) return you;
        return them;
    }

    public character getNotTurnHaver () {
        if (!opponentDoneMove) return you;
        return them;
    }

    public character getYou () {return this.you;}
    public character getThem () {return this.them;}
    public double getYourBar () {return this.yourBar;}
    public double getTheirBar () {return this.theirBar;}

    public boolean isActive () {return this.activeBattle;}
    public boolean isWaitingForPlayer () {return this.waitingForPlayer;}
    public boolean inMoreView () {return this.moreView;}

    public void newBattle (character you, character them, event youWin, event theyWin, double x, double y, double r, int world) {
        if (you == them) throw new IllegalArgumentException("Kan ikke slåss mot seg selv");
        activeBattle = true;
        this.you = you;
        this.them = them;
        this.youWin = youWin;
        this.theyWin = theyWin;
        this.x = x;
        this.y = y;
        this.r = r;
        this.world = world;
        this.yourBar = you.getHealthPercentage();
        this.theirBar = them.getHealthPercentage();
    }

    public void showMore () {moreView = true;}
    public void showActions () {moreView = false;}

    private void endTurn () {
        waitingForPlayer = false;
        moreView = false;
        opponentDoneMove = false;
    }

    //n is 0, 1 or 2
    public void useAbility (int n) {
        if (!waitingForPlayer) return;
        gemstone stone = you.getGemstone();
        if (stone == null) return;

        if (n == 0 && stone.ability1usesLeft()) {stone.ability1(you, them); endTurn();}
        if (n == 1 && stone.ability2usesLeft()) {stone.ability2(you, them); endTurn();}
        if (n == 2 && stone.ability3usesLeft()) {stone.ability3(you, them); endTurn();}
    }

    //Using a rock equips it, using anything else does what the item does. Both take the turn
    public void useItem (item item) {
        if (!waitingForPlayer) return;
        if (item instanceof gemstone && you.getGemstone() == item) return;

        item.use(you, them);
        if (item.isConsumable()) you.consume(item);
        endTurn();
    }

    public void giveUp () {
        if (!waitingForPlayer) return;
        you.kill();
        endTurn();
    }

    //Hands the player what the loser dropped, one message per kind of item
    private void giveDrops (gameHandler handler) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        Map<String, item> examples = new HashMap<String, item>();
        for (item drop : them.rollDrops()) {
            you.give(drop);
            counts.merge(drop.getName(), 1, Integer::sum);
            examples.put(drop.getName(), drop);
        }
        for (String name : counts.keySet()) {
            String text = counts.get(name) == 1 ? "You got " + name : "You got " + counts.get(name) + " x " + name;
            handler.getDiealoguecontainer().add(new dialogue("Game", text).highlight(dialogue.ITEM_BORDER, examples.get(name)));
        }
    }

    private void opponentMove () {
        them.autoMaticMove(you);
        opponentDoneMove = true;
    }

    private void resetBattle () {
        activeBattle = false;
        waitingForPlayer = false;
        moreView = false;
        this.you = null;
        this.them = null;
        this.youWin = null;
        this.theyWin = null;
        this.x = 0;
        this.y = 0;
        this.r = 0;
        this.world = 0;
        opponentDoneMove = true;
    }

    private boolean checkForDead (gameHandler handler) {
        if (you.getHealth() <= 0.5 || them.getHealth() <= 0.5) {

            if (you.getHealth() <= 0.5) {
                handler.getDiealoguecontainer().add(new dialogue("Game", you.getName() + " lost"));

                if (theyWin != null) {
                    theyWin.trigger();
                }
                you.restore();
                handler.getDiealoguecontainer().add(new dialogue("Game", "Restoring winner..."));
                them.restore();

                event battleEvent = new battleEvent(you, them, this, youWin, theyWin, x, y, r, world);
                handler.addEventListener(new eventListener(handler, x, y, r, world, battleEvent));
            } else {
                handler.getDiealoguecontainer().add(new dialogue("Game", them.getName() + " lost"));
                if (youWin != null) {
                    this.youWin.trigger();
                }
                you.giveXp(Math.pow(them.getLevel()+1, 2));
                giveDrops(handler);

            }

            resetBattle();
            return true;
        }
        return false;
    }

    public void carryOn (gameHandler handler) {

        if (!checkForDead(handler) && !handler.getDiealoguecontainer().isActive() && !waitingForPlayer) {
            if (opponentDoneMove) {
                waitingForPlayer = true;
            } else {
                opponentMove();
                if (!checkForDead(handler)) {
                    you.updateEffects();
                    them.updateEffects();
                }
            }
        }

        if (you != null) {
            yourBar += (you.getHealthPercentage()-yourBar)*0.1;
            theirBar += (them.getHealthPercentage()-theirBar)*0.1;
        }
    }

}
