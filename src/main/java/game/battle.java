package game;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

public class battle {

    private List<Button> buttons = new ArrayList<Button>();
    private boolean activeBattle = false;

    private character you, them;
    private ProgressBar bar1, bar2;
    private boolean buttonsShown = false;

    private event youWin, theyWin;

    private boolean opponentDoneMove = true;

    private ListView<item> inventory;

    //To save the event in case player lose
    private double x, y, r;
    private int world;

    public battle (Button button1, Button button2, Button button3, Button button4, ProgressBar progressBar1, ProgressBar progressBar2, ListView<item> inventory) {
        buttons.add(button1);
        buttons.add(button2);
        buttons.add(button3);
        buttons.add(button4);

        this.bar1 = progressBar1;
        this.bar2 = progressBar2;

        this.inventory = inventory;
    
        resetBattle();

        inventory.setCellFactory(Item -> new ButtonListCell());
    }

    public character getTurnHaver () {
        if (opponentDoneMove) return you;
        return them;
    }

    public character getNotTurnHaver () {
        if (!opponentDoneMove) return you;
        return them;
    }

    public boolean isActive () {return this.activeBattle;}
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
        bar1.setVisible(true);
        bar2.setVisible(true);
        updateButtons();

    }

    private void updateButtons () {
        if (you.getGemstone() != null) {
            gemstone temp = you.getGemstone();
            buttons.get(0).setText(temp.getAbility1name()
            + " (" + temp.howManyAbility1Uses().get(0) + "/" + temp.howManyAbility1Uses().get(1) + ") [" + Math.round(temp.chances(you).get(0)*100) + "%]");
            buttons.get(1).setText(temp.getAbility2name()
            + " (" + temp.howManyAbility2Uses().get(0) + "/" + temp.howManyAbility2Uses().get(1) + ") [" + Math.round(temp.chances(you).get(1)*100) + "%]");
            buttons.get(2).setText(temp.getAbility3name()
            + " (" + temp.howManyAbility3Uses().get(0) + "/" + temp.howManyAbility3Uses().get(1) + ") [" + Math.round(temp.chances(you).get(2)*100) + "%]");
        } else {
            buttons.get(2).setText("Give up");
        }
    }

    private void hideButtons () {
        buttonsShown = false;
        for (Button button : buttons) {
            button.setVisible(false);
        }
    }

    private void showButtons () {
        buttonsShown = true;
        this.inventory.setVisible(false);

        if (you.getGemstone() == null) {
            buttons.get(0).setVisible(false);
            buttons.get(1).setVisible(false);
            buttons.get(2).setVisible(true);
    
            if (you.getInventory().size() == 0) {
                buttons.get(3).setVisible(false);
            } else {
                buttons.get(3).setVisible(true);
            }
        } else {
            for (Button button : buttons) {
                button.setVisible(true);
            }
        }

    }



    public void button1click () {

        if (you.getGemstone().ability1usesLeft()) {
            you.getGemstone().ability1(you, them);

            hideButtons();
            opponentDoneMove = false;
        }
    }
    public void button2click () {
        if (you.getGemstone().ability2usesLeft()) {
            you.getGemstone().ability2(you, them);

            hideButtons();
            opponentDoneMove = false;
        }
    }
    public void button3click () {
        if (you.getGemstone() == null) {
            hideButtons();
            you.kill();
            opponentDoneMove = false;
        } else {
            if (you.getGemstone().ability3usesLeft()) {
                you.getGemstone().ability3(you, them);
                hideButtons();
                opponentDoneMove = false;
            }
        }

    }
    public void button4click () {
        
        inventory.getItems().clear();
        for (item item : you.getInventory()) {
            inventory.getItems().addAll(item);
        }
        inventory.setVisible(true);

        hideButtons();
    }

    public void itemChosen () {
        opponentDoneMove = false;
        inventory.setVisible(false);
    }

    private void opponentMove () {
        them.autoMaticMove(you);
        opponentDoneMove = true;
    }

    private void resetBattle () {
        activeBattle = false;
        bar1.setVisible(false);
        bar2.setVisible(false);
        hideButtons();
        this.you = null;
        this.them = null;
        this.youWin = null;
        this.theyWin = null;
        this.x = 0;
        this.y = 0;
        this.r = 0;
        this.world = 0;
        opponentDoneMove = true;
        this.inventory.setVisible(false);
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

            }

            resetBattle();
            return true;
        }
        return false;
    }

    public void carryOn (gameHandler handler) {

        if (!checkForDead(handler) && !handler.getDiealoguecontainer().isActive() && !buttonsShown) {
            if (opponentDoneMove)  {
                if (!inventory.isVisible()) {
                    updateButtons();
                    showButtons();

                }
            }
            else {opponentMove();
                if (!checkForDead(handler)) {
                    you.updateEffects();
                    them.updateEffects();
                }
            }
        }

        if (you != null) {
            bar1.setProgress(bar1.getProgress() + (you.getHealthPercentage()-bar1.getProgress())*0.1);
            bar2.setProgress(bar2.getProgress() + (them.getHealthPercentage()-bar2.getProgress())*0.1);
        }
    }

}
