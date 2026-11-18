package game;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//Everything shown during a battle: the two health bars and the action panel at the bottom.
//The panel shows the three abilities and a "more" button, or the rocks and items with "go back" and "give up"
public class guiBattle extends guiElement {

    private battle battle;

    private guiButton[] abilityButtons = new guiButton[3];
    private guiButton moreButton, backButton, giveUpButton;
    private List<guiButton> buttons = new ArrayList<guiButton>();

    private slotGrid rockGrid = new slotGrid(inventory.slotKind.ROCK, inventory.ACTIVE, 1);
    private slotGrid itemGrid = new slotGrid(inventory.slotKind.ITEM, inventory.ACTIVE, 1);
    //Tiny copies of the two rows, drawn on the "more" button as a peek
    private slotGrid miniRockGrid = new slotGrid(inventory.slotKind.ROCK, inventory.ACTIVE, 1);
    private slotGrid miniItemGrid = new slotGrid(inventory.slotKind.ITEM, inventory.ACTIVE, 1);

    private double slotSize = 48, gap = 4, miniSize = 12, miniGap = 2;
    private double barWidth = 280, barHeight = 56;

    //The action panel's box
    private double panelX, panelY, panelWidth, panelHeight;

    //Popups: what is hovered slides up out from under it in POPUP_TIME seconds, and back down when the mouse leaves.
    //Each value is how far up that popup is, 0 to 1.
    //A move's box sits right on top of the move. A rock or item in the More view gets the inventory's info panel,
    //always in the same spot: centred on top of the action panel. It is one popup: moving from slot to slot while it
    //is up only changes what it shows
    public static final double POPUP_TIME = 0.25;
    private double[] popup = new double[3];
    private int hoveredMove = -1;
    private double[] slotPopup = new double[1];
    //The item is kept so the popup still has something to show while it goes down
    private item slotPopupItem, hoveredItem;
    private double slotInfoWidth = 260;
    private long lastDraw = 0;

    public guiBattle (gui gui, battle battle) {
        super(gui);
        this.battle = battle;

        for (int i = 0; i < 3; i++) {
            final int n = i;
            abilityButtons[i] = new guiButton(gui, "", () -> battle.useAbility(n));
            buttons.add(abilityButtons[i]);
        }
        moreButton = new guiButton(gui, "", () -> battle.showMore());
        moreButton.setFace(gc -> drawMoreFace(gc));
        backButton = new guiButton(gui, "Go back", () -> battle.showActions());
        giveUpButton = new guiButton(gui, "Give up", () -> battle.giveUp());
        buttons.add(moreButton);
        buttons.add(backButton);
        buttons.add(giveUpButton);
    }

    private boolean panelShown () {
        return battle.isActive() && battle.isWaitingForPlayer();
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        //The element covers the whole screen, its parts are placed inside
        setBounds(0, 0, screenWidth, screenHeight);

        double s = gui.s();
        double slotSize = this.slotSize*s, gap = this.gap*s, miniSize = this.miniSize*s, miniGap = this.miniGap*s;
        double margin = 20*s;
        double pad = gui.getSkin().getBorder() + 4*s;
        boolean more = battle.inMoreView();

        panelWidth = screenWidth - 2*margin;
        panelHeight = more ? 2*pad + 2*slotSize + gap + 12*s + 36*s : 2*pad + 64*s;
        panelX = margin;
        panelY = screenHeight - panelHeight - 12*s;

        for (guiButton button : buttons) button.setVisible(panelShown());

        if (more) {
            for (guiButton button : abilityButtons) button.setVisible(false);
            moreButton.setVisible(false);

            double gridsX = Math.round(panelX + (panelWidth - rockGrid.getWidth())/2);
            rockGrid.setBounds(gridsX, panelY + pad, slotSize, gap);
            itemGrid.setBounds(gridsX, panelY + pad + slotSize + gap, slotSize, gap);

            double buttonWidth = 160*s, buttonHeight = 36*s;
            double buttonsY = panelY + panelHeight - pad - buttonHeight;
            backButton.setBounds(Math.round(panelX + panelWidth/2 - buttonWidth - gap), buttonsY, buttonWidth, buttonHeight);
            giveUpButton.setBounds(Math.round(panelX + panelWidth/2 + gap), buttonsY, buttonWidth, buttonHeight);
        } else {
            backButton.setVisible(false);
            giveUpButton.setVisible(false);

            double buttonWidth = (panelWidth - 2*pad - 3*gap)/4;
            double buttonHeight = panelHeight - 2*pad;
            for (int i = 0; i < 3; i++) {
                abilityButtons[i].setBounds(Math.round(panelX + pad + i*(buttonWidth+gap)), panelY + pad, Math.round(buttonWidth), buttonHeight);
            }
            moreButton.setBounds(Math.round(panelX + pad + 3*(buttonWidth+gap)), panelY + pad, Math.round(buttonWidth), buttonHeight);

            double miniX = Math.round(moreButton.x + (moreButton.width - miniRockGrid.getWidth())/2);
            miniRockGrid.setBounds(miniX, moreButton.y + 26*s, miniSize, miniGap);
            miniItemGrid.setBounds(miniX, moreButton.y + 26*s + miniSize + miniGap, miniSize, miniGap);
        }

        updateButtons();
    }

    private void updateButtons () {
        if (battle.getYou() == null) return;
        gemstone stone = battle.getYou().getGemstone();

        if (stone == null) {
            for (guiButton button : abilityButtons) {
                button.setText("-");
                button.setEnabled(false);
                button.setIcon(null);
            }
            return;
        }

        String[] names = {stone.getAbility1name(), stone.getAbility2name(), stone.getAbility3name()};
        List<List<Integer>> uses = List.of(stone.usesOf(0, battle.getYou()), stone.usesOf(1, battle.getYou()), stone.usesOf(2, battle.getYou()));
        List<Double> chances = stone.chances(battle.getYou());
        abilityType[] types = {stone.getAbility1type(), stone.getAbility2type(), stone.getAbility3type()};

        for (int i = 0; i < 3; i++) {
            abilityButtons[i].setText(names[i] + " (" + uses.get(i).get(0) + "/" + uses.get(i).get(1) + ") [" + guiText.num(chances.get(i)*100) + "%]");
            abilityButtons[i].setEnabled(uses.get(i).get(0) > 0);
            abilityButtons[i].setIcon(types[i].getIcon(gui.getHandler().getImages()));
        }
    }

    private void drawMoreFace (GraphicsContext gc) {
        double s = gui.s();
        guiText.draw(gc, "More", moreButton.x + moreButton.width/2, moreButton.y + 8*s, 13*s, Color.BLACK, TextAlignment.CENTER);
        inventory inv = battle.getYou().getInventory();
        miniRockGrid.draw(gc, gui, inv, battle.getYou().getGemstone(), false);
        miniItemGrid.draw(gc, gui, inv, battle.getYou().getGemstone(), false);
    }

    @Override
    public void draw (GraphicsContext gc) {
        if (!battle.isActive()) {
            //The next fight's panel comes up from below again
            panelUp = 0;
            lastDraw = 0;
            return;
        }
        //Cheat mode's scores start being worked out as soon as it is the player's turn, before anything is hovered
        battle.getHints(gui.getHandler());

        double s = gui.s();
        drawHealth(gc, battle.getYou(), battle.getYourBar(), 20*s, 20*s);
        drawHealth(gc, battle.getThem(), battle.getTheirBar(), width - barWidth*s - 20*s, 20*s);
        if (battle.getDuel() != null && battle.getDuel().isAgainstComputer()) drawStreak(gc, battle.getDuel().getStreak(), s);

        if (battle.waitingForOther(gui.getHandler())) {
            //A 1v1 waiting for the other player: a small panel where the moves would be, with the waiting sign
            double w = 300*s, h = 64*s;
            double px = Math.round((width - w)/2), py = Math.round(height - h - 12*s);
            gui.getSkin().drawPanel(gc, px, py, w, h);
            guiWaiting.draw(gc, gui, px + 34*s, py + h/2, 36*s);
            guiText.draw(gc, "Waiting for " + battle.getDuel().getTheirName(), px + 62*s, py + h/2 - guiText.height(12*s)/2, 12*s, Color.BLACK);
        }

        long now = System.nanoTime();
        double dt = lastDraw == 0 ? 0 : Math.min(0.1, (now - lastDraw)/1e9);
        lastDraw = now;
        //The panel comes up from the bottom when it is the player's turn and goes back down when a move is picked
        panelUp = panelShown() ? Math.min(1, panelUp + dt/SLIDE_TIME) : Math.max(0, panelUp - dt/SLIDE_TIME);

        if (!panelShown()) {
            //Picking a move hides the panel; the popups start from hidden when it comes back
            java.util.Arrays.fill(popup, 0);
            java.util.Arrays.fill(slotPopup, 0);
            if (panelUp <= 0) {
                lastDraw = 0;
                //The rocks and items keep acting on each other (singularity charging healite) while the panel is
                //down, as if on the More button, below the screen
                if (battle.getYou() != null) {
                    inventory inv = battle.getYou().getInventory();
                    miniRockGrid.interactOnly(gui, inv.group(inventory.slotKind.ROCK), height);
                    miniItemGrid.interactOnly(gui, inv.group(inventory.slotKind.ITEM), height);
                }
                return;
            }
        }

        boolean movesShown = !battle.inMoreView() && battle.getYou().getGemstone() != null;
        slide(popup, panelShown() && movesShown ? hoveredMove : -1, dt);
        slide(slotPopup, panelShown() && battle.inMoreView() && hoveredItem != null ? 0 : -1, dt);

        gc.save();
        gc.translate(0, panelOffset());
        gui.getSkin().drawPanel(gc, panelX, panelY, panelWidth, panelHeight);

        if (battle.inMoreView()) {
            inventory inv = battle.getYou().getInventory();
            rockGrid.draw(gc, gui, inv, battle.getYou().getGemstone(), true);
            itemGrid.draw(gc, gui, inv, battle.getYou().getGemstone(), true);
        }

        //Going down the buttons are hidden already (layout), so the ones of the page shown are drawn
        boolean more = battle.inMoreView();
        for (guiButton button : buttons) {
            boolean onPage = more ? button == backButton || button == giveUpButton : button != backButton && button != giveUpButton;
            if (button.isVisible() || (!panelShown() && onPage)) button.draw(gc);
        }
        gc.restore();
    }

    //How far up the action panel is, 0 (below the screen) to 1 (in place), and how far down that draws it
    private double panelUp = 0;
    private double panelOffset () {
        double t = panelUp*panelUp*(3 - 2*panelUp);
        return Math.round((1 - t)*(height - panelY + 4));
    }

    //The popups (a move's box, a rock's or item's info) are drawn by gui after everything else, glows, particles and
    //lightning included, so nothing is ever drawn over them. Each is still cut off at the edge it slides out of
    public void drawPopups (GraphicsContext gc) {
        if (!battle.isActive() || !panelShown()) return;
        //With the panel, while it slides up
        gc.save();
        gc.translate(0, panelOffset());
        if (!battle.inMoreView() && battle.getYou().getGemstone() != null) drawMovePopups(gc);
        drawSlotPopups(gc);
        gc.restore();
    }

    //Moves every popup towards up (the hovered one) or down by dt seconds' worth
    private static void slide (double[] popups, int hovered, double dt) {
        double step = dt/POPUP_TIME;
        for (int i = 0; i < popups.length; i++) {
            popups[i] = i == hovered ? Math.min(1, popups[i] + step) : Math.max(0, popups[i] - step);
        }
    }

    //How far a popup has come up, eased out: fast at first, settling at the top
    private static double eased (double popup) {return 1 - Math.pow(1 - popup, 3);}

    //A random battle: the wins in a row so far and the best, in a little panel at the top between the health bars
    private void drawStreak (GraphicsContext gc, int streak, double s) {
        String line = "Win streak: " + streak;
        String best = "Best: " + duelBuild.library.shared().bestStreak;
        double pad = gui.getSkin().getBorder()*0.75 + 4*s;
        double w = Math.max(guiText.width(line, 14*s), guiText.width(best, 10*s)) + 2*pad + 12*s;
        double h = 2*pad + guiText.height(14*s) + 3*s + guiText.height(10*s);
        double px = Math.round((width - w)/2), py = Math.round(20*s);
        gui.getSkin().drawPanel(gc, px, py, w, h);
        guiText.draw(gc, line, width/2, py + pad, 14*s, streak > 0 ? Color.web("#A07800") : Color.BLACK, TextAlignment.CENTER);
        guiText.draw(gc, best, width/2, py + pad + guiText.height(14*s) + 3*s, 10*s, Color.rgb(60, 60, 60), TextAlignment.CENTER);
    }

    private void drawHealth (GraphicsContext gc, character c, double progress, double bx, double by) {
        double s = gui.s();
        double barWidth = this.barWidth*s, barHeight = this.barHeight*s;
        gui.getSkin().drawPanel(gc, bx, by, barWidth, healthBoxHeight(c));
        double pad = gui.getSkin().getBorder()*0.75;

        //The character's picture at the right end, the name and bar to the left of it
        double portrait = barHeight - 2*pad*0.6;
        gc.drawImage(c.getIcon(), 0, 0, guiSkin.CELL, guiSkin.CELL,
            Math.round(bx + barWidth - pad - portrait), Math.round(by + (barHeight - portrait)/2), Math.round(portrait), Math.round(portrait));
        double innerWidth = barWidth - 3*pad - portrait;

        String name = c.getLevel() == 0 ? c.getName() : c.getName() + " (Lvl " + c.getLevel() + ")";
        guiText.draw(gc, name, bx + pad, by + pad - 2*s, 11*s, Color.BLACK);

        //The health in numbers at the right end of the name line, next to the picture
        String hp = guiText.num(c.getShownHealth()) + "/" + guiText.num(c.getMaxHealth());
        guiText.draw(gc, hp, bx + pad + innerWidth, by + pad - 2*s, 11*s, Color.BLACK, TextAlignment.RIGHT);

        double barY = by + barHeight - pad - 12*s;
        gc.setFill(Color.rgb(0, 0, 0, 0.2));
        gc.fillRect(bx + pad, barY, innerWidth, 12*s);
        gc.setFill(Color.RED);
        gc.fillRect(bx + pad, barY, innerWidth*Math.max(0, Math.min(1, progress)), 12*s);

        //Under the bar: the equipped rock, then each effect on one line: how strong with its icon, and the turns left
        //with the rounds icon
        double lineSize = 10*s, lineHeight = Math.max(guiText.height(lineSize), 14*s);
        double lineY = by + barHeight - pad*0.5;
        gemstone stone = c.getGemstone();
        //Only the icon, so the rock gives off no particles or lightning from up here
        if (stone != null) stone.drawIcon(gc, bx + pad, lineY + (lineHeight - 12*s)/2, 12*s);
        guiText.draw(gc, "Rock: " + (stone != null ? stone.getName() : "None"), bx + pad + 16*s, lineY + (lineHeight - guiText.height(lineSize))/2, lineSize, Color.BLACK);
        lineY += lineHeight;
        for (infoBox effect : c.getEffectBoxes()) {
            for (iconText line : effect.getLines()) line.draw(gc, gui, bx + pad, lineY + (lineHeight - line.height(lineSize))/2, lineSize, innerWidth);
            lineY += lineHeight;
        }
    }

    //The move boxes, each as wide as its move and ending on its top edge when fully up. Cut off at that edge, so they
    //come up from behind the move
    private void drawMovePopups (GraphicsContext gc) {
        List<infoBox> moves = battle.getYou().getGemstone().getInfoBoxes(battle.getYou(), battle.getThem());
        battleAI.hints hints = battle.getHints(gui.getHandler());
        for (int i = 0; i < popup.length; i++) {
            if (popup[i] <= 0) continue;
            //Cheat mode: how good the move is now, the best action in green
            String[] hint = hintText(hints, battleSim.MOVE, i);
            if (hint != null) moves.get(i).row(hint[0], hint[1], hints.isBest(battleSim.MOVE, i) ? HINT_BEST : Color.BLACK);
            guiButton button = abilityButtons[i];
            double h = moves.get(i).draw(gc, gui, 0, 0, button.width, false);
            double y = button.y - h*eased(popup[i]);
            gc.save();
            gui.clipTo(gc, 0, 0, width, button.y);
            moves.get(i).draw(gc, gui, button.x, Math.round(y), button.width, true);
            gc.restore();
        }
    }

    //The info panel of the hovered rock or item in the More view, as in the inventory. Centred on top of the action
    //panel when fully up. Cut off at the panel's top edge, so it comes up from behind it
    private void drawSlotPopups (GraphicsContext gc) {
        if (slotPopup[0] <= 0 || slotPopupItem == null) return;
        double s = gui.s();
        double w = slotInfoWidth*s, pad = gui.getSkin().getBorder() + 4*s;
        //Cheat mode: a line at the bottom with how good using it is now (switching to a rock, drinking a potion)
        battleAI.hints hints = battle.getHints(gui.getHandler());
        int[] action = slotAction(slotPopupItem);
        String[] hint = action == null ? null : hintText(hints, action[0], action[1]);
        double hintSize = 11*s, hintHeight = hint == null ? 0 : guiText.height(hintSize) + 6*s;
        double details = guiInventory.drawItemDetails(gc, gui, slotPopupItem, 0, 0, w - 2*pad, false);
        double h = details + hintHeight + 2*pad;
        double x = Math.round(panelX + (panelWidth - w)/2);
        double y = Math.round(panelY - h*eased(slotPopup[0]));
        gc.save();
        gui.clipTo(gc, 0, 0, width, panelY);
        gui.getSkin().drawPanel(gc, x, y, w, h);
        guiInventory.drawItemDetails(gc, gui, slotPopupItem, x + pad, y + pad, w - 2*pad, true);
        if (hint != null) {
            Color color = hints.isBest(action[0], action[1]) ? HINT_BEST : Color.BLACK;
            double hy = y + pad + details + 6*s;
            guiText.draw(gc, hint[0], x + pad, hy, hintSize, color);
            guiText.draw(gc, hint[1], x + w - pad, hy, hintSize, color, TextAlignment.RIGHT);
        }
        gc.restore();
    }

    //Cheat mode's best action is written in this green
    private static final Color HINT_BEST = Color.rgb(30, 140, 30);

    //"Win chance" and "62%" (the trained judge), or "Score" and "0.62" (the calculated one, which is no true chance);
    //null when there is nothing to show for that action
    private static String[] hintText (battleAI.hints hints, int kind, int index) {
        if (hints == null) return null;
        double v = hints.of(kind, index);
        if (Double.isNaN(v)) return null;
        if (hints.chance) return new String[] {"Win chance", Math.round(v*100) + "%"};
        return new String[] {"Score", String.format(java.util.Locale.ENGLISH, "%.2f", v)};
    }

    //The battleSim action that using this rock or item is: switching to it, or using the item. Null for anything that
    //is no action (the held rock, an item with no effect in a fight)
    private int[] slotAction (item shown) {
        character you = battle.getYou();
        int k = battleSim.rockOrder(you).indexOf(shown);
        if (k >= 0) return new int[] {battleSim.SWITCH, k};
        k = battleSim.itemOrder(you).indexOf(shown);
        if (k >= 0) return new int[] {battleSim.ITEM, k};
        return null;
    }

    //How tall a health box is: the bar part, a line for the equipped rock and a line more for each effect on the character
    private double healthBoxHeight (character c) {
        double s = gui.s();
        int lines = 1 + c.getEffectBoxes().size();
        double lineHeight = Math.max(guiText.height(10*s), 14*s);
        return barHeight*s + lines*lineHeight + gui.getSkin().getBorder()*0.25;
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        if (!panelShown()) return false;

        for (guiButton button : buttons) {
            if (button.contains(mx, my)) return button.click(mx, my, rightClick);
        }

        if (battle.inMoreView()) {
            inventory inv = battle.getYou().getInventory();
            for (slotGrid grid : new slotGrid[] {rockGrid, itemGrid}) {
                int index = grid.indexAt(mx, my, inv);
                if (index >= 0) {
                    itemStack stack = inv.get(grid.getKind(), index);
                    if (stack != null) battle.useItem(stack.getItem());
                    return true;
                }
            }
        }

        //The panel itself swallows clicks, the rest of the screen does not
        return panelX <= mx && mx < panelX+panelWidth && panelY <= my && my < panelY+panelHeight;
    }

    @Override
    public void hover (double mx, double my) {
        for (guiButton button : buttons) button.hover(mx, my);

        //The hovered move shows what it does in a box that pops up above it (see drawMovePopups), the same box as
        //in the inventory's info panel
        hoveredMove = -1;
        if (panelShown() && !battle.inMoreView() && battle.getYou().getGemstone() != null) {
            for (int i = 0; i < abilityButtons.length; i++) {
                if (abilityButtons[i].isVisible() && abilityButtons[i].contains(mx, my)) hoveredMove = i;
            }
        }

        //A hovered rock or item shows its info panel, popping up above the action panel (see drawSlotPopups)
        hoveredItem = null;
        if (panelShown() && battle.inMoreView()) {
            inventory inv = battle.getYou().getInventory();
            for (slotGrid grid : new slotGrid[] {rockGrid, itemGrid}) {
                int index = grid.indexAt(mx, my, inv);
                if (index >= 0 && inv.get(grid.getKind(), index) != null) hoveredItem = inv.get(grid.getKind(), index).getItem();
            }
        }
        if (hoveredItem != null) slotPopupItem = hoveredItem;
    }
}
