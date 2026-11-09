package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The layer drawn on top of the game: active bar, battle, inventory and dialogue.
//Mouse events come in here first and only reach the game when no element wanted them
public class gui {

    private gameHandler handler;
    private guiSkin skin;
    private Random rdm = new Random();

    //Drawn in this order, so the last one is on top and gets the clicks first
    private List<guiElement> elements = new ArrayList<guiElement>();
    private guiHud hud;
    private guiBattle battleScreen;
    private guiInventory inventoryScreen;
    private guiDialogue dialogueBox;
    private guiSettings settingsScreen;

    private List<guiParticle> particles = new ArrayList<guiParticle>();

    //The stack being carried between slots
    private itemStack cursor;
    private double mouseX, mouseY;
    private String tooltip;
    private String debugText = "";

    public gui (gameHandler handler) {
        this.handler = handler;
        this.skin = new guiSkin(this);

        hud = new guiHud(this);
        battleScreen = new guiBattle(this, handler.getBattle());
        inventoryScreen = new guiInventory(this);
        dialogueBox = new guiDialogue(this);
        settingsScreen = new guiSettings(this);

        elements.add(hud);
        elements.add(battleScreen);
        elements.add(inventoryScreen);
        elements.add(dialogueBox);
        elements.add(settingsScreen);
    }

    public gameHandler getHandler () {return this.handler;}
    public guiSkin getSkin () {return this.skin;}
    public Random getRandom () {return this.rdm;}

    //The GUI scale: every size in the gui classes is multiplied with this, so the GUI is a share of the screen width
    public double s () {
        return handler.getWIDTH()/handler.getSettings().getReferenceWidth() * handler.getSettings().getGuiSize().factor;
    }

    public itemStack getCursor () {return this.cursor;}
    public void setCursor (itemStack cursor) {this.cursor = cursor;}

    //Shown next to the mouse, set by whatever is under it during hover
    public void setTooltip (String tooltip) {this.tooltip = tooltip;}
    public void setDebugText (String text) {this.debugText = text;}

    public void addParticle (guiParticle particle) {particles.add(particle);}

    public void toggleInventory () {
        if (handler.getBattle().isActive()) return;
        inventoryScreen.setVisible(!inventoryScreen.isVisible());
    }

    public void closeInventory () {
        inventoryScreen.setVisible(false);
    }

    public boolean inventoryOpen () {
        return inventoryScreen.isVisible();
    }

    public void closeSettings () {settingsScreen.setVisible(false);}
    public boolean settingsOpen () {return settingsScreen.isVisible();}

    //Escape backs out of whatever is open, and opens the settings when nothing is
    public void escape () {
        if (settingsOpen()) closeSettings();
        else if (inventoryOpen()) closeInventory();
        else settingsScreen.setVisible(true);
    }

    //While a screen has the player's attention the character stands still
    public boolean blocksMovement () {
        return inventoryOpen() || settingsOpen();
    }

    public void next (double deltaTime) {
        particles.forEach(p -> p.next(deltaTime));
        particles = particles.stream().filter(p -> !p.isDead()).collect(Collectors.toList());
    }

    public void draw (GraphicsContext gc) {
        double screenWidth = handler.getWIDTH();
        double screenHeight = handler.getHEIGHT();

        hud.setVisible(!inventoryOpen() && !handler.getBattle().isActive());
        battleScreen.setVisible(handler.getBattle().isActive());
        dialogueBox.setVisible(handler.getDiealoguecontainer().isActive());

        gc.setGlobalAlpha(1);
        for (guiElement element : elements) {
            if (element.isVisible()) {
                element.layout(screenWidth, screenHeight);
                element.draw(gc);
            }
        }

        particles.forEach(p -> p.draw(gc));

        double s = s();
        if (cursor != null) {
            cursor.getItem().drawIcon(gc, mouseX - 20*s, mouseY - 20*s, 40*s);
            if (cursor.getCount() > 1) {
                guiText.draw(gc, String.valueOf(cursor.getCount()), mouseX + 10*s, mouseY + 6*s, 12*s, Color.BLACK);
            }
        } else if (tooltip != null) {
            double size = 11*s;
            double w = guiText.width(tooltip, size) + 12*s;
            double h = guiText.height(size) + 8*s;
            double tx = Math.min(mouseX + 14*s, screenWidth - w), ty = mouseY - h - 4*s;
            gc.setFill(Color.rgb(0, 0, 0, 0.75));
            gc.fillRect(Math.round(tx), Math.round(ty), Math.round(w), Math.round(h));
            guiText.draw(gc, tooltip, tx + 6*s, ty + 4*s, size, Color.WHITE);
        }

        if (!debugText.isEmpty()) {
            guiText.draw(gc, debugText, 14, 14, 12*s, Color.WHITE);
        }

        //Leave the text settings the way the rest of the game expects them
        gc.setTextBaseline(VPos.BASELINE);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    public boolean click (double mx, double my, boolean rightClick) {
        mouseX = mx;
        mouseY = my;
        for (int i = elements.size()-1; i >= 0; i--) {
            guiElement element = elements.get(i);
            boolean modal = element == inventoryScreen || element == settingsScreen;
            if (element.isVisible() && (element.contains(mx, my) || modal) && element.click(mx, my, rightClick)) return true;
        }
        return false;
    }

    public boolean scroll (double mx, double my, double delta) {
        //An open menu takes all scrolling, so the world does not zoom behind it
        if (settingsOpen()) return true;
        if (inventoryOpen()) return inventoryScreen.scroll(mx, my, delta);

        for (int i = elements.size()-1; i >= 0; i--) {
            guiElement element = elements.get(i);
            if (element.isVisible() && element.contains(mx, my) && element.scroll(mx, my, delta)) return true;
        }
        return false;
    }

    public void hover (double mx, double my) {
        mouseX = mx;
        mouseY = my;
        tooltip = null;
        for (guiElement element : elements) {
            if (element.isVisible()) element.hover(mx, my);
        }
    }
}
