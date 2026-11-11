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

    //Clips everything drawn after it to the rectangle (until the gc.restore() that matches the gc.save() before it).
    //Always clip with this, never with gc.clip() straight: JavaFX's canvas has a bug where a clip made right after the
    //opacity was changed (any setGlobalAlpha, even one set back to 1 or undone by restore) draws everything inside it
    //with the old opacity. The battle's popups came out see-through like that (0.7, from the empty slot hints on the
    //More button). Drawing anything first makes the canvas take up the real opacity, so a see-through dot goes first
    public static void clipTo (GraphicsContext gc, double x, double y, double w, double h) {
        javafx.scene.paint.Paint fill = gc.getFill();
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(x, y, 1, 1);
        gc.setFill(fill);
        gc.beginPath();
        gc.rect(x, y, w, h);
        gc.clip();
    }

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
    private guiTeleports teleportScreen;
    private guiMainMenu mainMenu;

    //A main menu drawn on top of everything without taking the mouse, while switching between the menu and a game
    private guiElement menuOverlay;

    private List<guiParticle> particles = new ArrayList<guiParticle>();

    //The stack being carried between slots
    private itemStack cursor;
    private double mouseX, mouseY;

    //The held item is shaken by moving the mouse (see item.shake): this mouse speed (in gui pixels per second, see s)
    //counts as shaking as hard as it gets. Just holding it keeps it a little stirred, so it glows faintly in the hand
    private double lastMouseX, lastMouseY;
    private static final double FULL_SHAKE_SPEED = 1500;
    private static final double HELD_SHAKE = 0.18;
    private static final double SHAKE_BREAK = 0.5, SHAKE_BREAK_RATE = 1.0;
    //A sudden turn: moving at least TURN_SPEED (gui pixels per second) and, within TURN_WINDOW seconds, going off at
    //least TURN_ANGLE degrees from that way at that speed again. It gives a held item that breaks from shaking
    //TURN_HEAT at once, which is all glass needs to break
    private static final double TURN_SPEED = 600, TURN_WINDOW = 0.25, TURN_ANGLE = 110, TURN_HEAT = 1;
    //The last fast movement, and how long ago it was
    private double fastVx, fastVy, fastAge = Double.MAX_VALUE;
    //The mouse's velocity in screen pixels per second, smoothed a little. Particles off the held item are thrown with it
    private double mouseVx, mouseVy;
    private String tooltip;
    //A bigger tooltip drawn as an info box (a move's description in battle), over the plain one
    private List<infoBox> tooltipBoxes;
    private String debugText = "";

    public gui (gameHandler handler) {
        this.handler = handler;
        this.skin = new guiSkin(this);

        hud = new guiHud(this);
        //Comes up from the bottom once the game has opened, and goes back down when it is hidden
        hud.setVisible(false);
        hud.slideFrom(guiElement.edge.BOTTOM);
        battleScreen = new guiBattle(this, handler.getBattle());
        inventoryScreen = new guiInventory(this);
        dialogueBox = new guiDialogue(this);
        settingsScreen = new guiSettings(this);
        teleportScreen = new guiTeleports(this);
        mainMenu = new guiMainMenu(this);

        elements.add(hud);
        elements.add(battleScreen);
        elements.add(inventoryScreen);
        elements.add(dialogueBox);
        elements.add(settingsScreen);
        elements.add(teleportScreen);
        elements.add(mainMenu);
    }

    public gameHandler getHandler () {return this.handler;}
    public guiSkin getSkin () {return this.skin;}
    public Random getRandom () {return this.rdm;}

    //The GUI scale: every size in the gui classes is multiplied with this, so the GUI is a share of the screen width
    //On short, wide windows the screen height decides instead, so that the tallest screen (see settings.getFitHeight)
    //still fits at the largest GUI size. The smaller sizes shrink along with it, so the sizes always stay apart
    public double s () {
        settings set = handler.getSettings();
        double byWidth = handler.getWIDTH()/set.getReferenceWidth();
        double byHeight = handler.getHEIGHT()/(set.getFitHeight()*settings.guiSize.largest());
        return Math.min(byWidth, byHeight) * set.getGuiSize().factor;
    }

    public itemStack getCursor () {return this.cursor;}
    public void setCursor (itemStack cursor) {this.cursor = cursor;}

    //Shown next to the mouse, set by whatever is under it during hover
    public void setTooltip (String tooltip) {this.tooltip = tooltip;}
    public void setTooltip (infoBox box) {this.tooltipBoxes = List.of(box);}
    //Several boxes stacked (an item with its abilities)
    public void setTooltip (List<infoBox> boxes) {this.tooltipBoxes = boxes;}
    public void setDebugText (String text) {this.debugText = text;}

    public void addParticle (guiParticle particle) {particles.add(particle);}

    //How dark the world behind the open inventory is tinted, fading towards INVENTORY_DIM in DIM_TIME seconds
    private double dim = 0;
    private static final double INVENTORY_DIM = 0.3, DIM_TIME = 0.2;

    //Where the items are on screen and how they charge each other
    private itemInteractions interactions = new itemInteractions(this);
    public itemInteractions getInteractions () {return this.interactions;}

    //Streams left behind items moved on the cursor (item.trail). The one being drawn now is trail, older ones fade out
    private List<guiTrail> trails = new ArrayList<guiTrail>();
    private guiTrail trail;
    private boolean trailPaused = true;
    //Below this mouse speed (gui pixels per second) the stream stops and starts again on the next swing
    private static final double TRAIL_SPEED = 120;

    //Outlines jumping out of items that turn into others, drawn with the particles
    private List<guiOutlinePulse> pulses = new ArrayList<guiOutlinePulse>();
    public void addOutlinePulse (guiOutlinePulse pulse) {pulses.add(pulse);}

    //Lightning strikes from shaken items, drawn with the particles
    private List<guiLightning> lightning = new ArrayList<guiLightning>();
    public void addLightning (guiLightning strike) {lightning.add(strike);}

    public void toggleInventory () {
        if (handler.getBattle().isActive() || handler.inMenu()) return;
        inventoryScreen.setVisible(!inventoryScreen.isVisible());
    }

    //Opens the inventory with a chest (or any other slots) above it, so items can be moved between them
    //Opens or closes an elemental rock's three slots under the inventory
    public void toggleElemental (elementalRock rock) {
        inventoryScreen.toggleElemental(rock);
    }

    public void openContainer (slotGroup container, String title) {
        if (handler.getBattle().isActive()) return;
        inventoryScreen.openWith(container, title);
    }

    //The give item cheat: every registered item in a catalogue above the inventory (Hidden secrets -> Give item, or I)
    public void openGiveItems () {
        openContainer(new itemSource(handler.getItemRegistry().getMakers()), "Give item");
    }

    //I opens the give item catalogue, and closes the inventory when it is open (also with a chest)
    public void toggleGiveItems () {
        if (handler.getBattle().isActive() || handler.inMenu() || settingsOpen() || teleportsOpen()) return;
        if (inventoryOpen()) closeInventory();
        else openGiveItems();
    }

    public void closeInventory () {
        inventoryScreen.setVisible(false);
    }

    public boolean inventoryOpen () {
        return inventoryScreen.isVisible();
    }

    public void closeSettings () {settingsScreen.setVisible(false);}
    public void openSecrets () {settingsScreen.openSecrets();}
    public void openVisuals () {settingsScreen.openVisuals();}

    public guiMainMenu getMainMenu () {return this.mainMenu;}
    //A menu shows up by sliding in (if it is not in place already) and null slides the one shown out
    public void setMenuOverlay (guiElement menu) {
        if (menu != null) {
            menuOverlay = menu;
            menu.setVisible(true);
        } else if (menuOverlay != null) {
            menuOverlay.setVisible(false);
        }
    }

    //Slides the main menu away while the game it starts is still loading in. The new game takes over the slide
    private boolean menuLeaving = false;
    public void menuLeaves () {menuLeaving = true;}

    //A main menu for the game to slide in on top while quitting
    public guiElement makeMenu () {return new guiMainMenu(this);}

    //A menu of places to teleport to. back is run when its back button is pressed (null just closes it)
    public void openTeleports (List<worldTile> targets, String title, Runnable back) {
        teleportScreen.open(targets, title, back);
    }
    public boolean teleportsOpen () {return teleportScreen.isVisible();}
    public boolean settingsOpen () {return settingsScreen.isVisible();}

    //Escape backs out of whatever is open, and opens the settings when nothing is
    public void escape () {
        if (handler.inMenu() && settingsOpen()) closeSettings();
        else if (handler.inMenu()) mainMenu.back();
        else if (teleportsOpen()) teleportScreen.setVisible(false);
        else if (settingsOpen()) closeSettings();
        else if (inventoryOpen()) closeInventory();
        else settingsScreen.setVisible(true);
    }

    //While a screen has the player's attention the character stands still
    public boolean blocksMovement () {
        return inventoryOpen() || settingsOpen() || teleportsOpen() || handler.inMenu();
    }

    //Keys go to the screen on top that wants them (the main menu takes them all)
    public boolean keyPressed (javafx.scene.input.KeyCode code) {
        for (int i = elements.size()-1; i >= 0; i--) {
            if (elements.get(i).isVisible() && elements.get(i).keyPressed(code)) return true;
        }
        //In the main menu only escape reaches the game's keys (it backs out of the settings)
        return handler.inMenu() && code != javafx.scene.input.KeyCode.ESCAPE;
    }

    public boolean keyTyped (String typed) {
        for (int i = elements.size()-1; i >= 0; i--) {
            if (elements.get(i).isVisible() && elements.get(i).keyTyped(typed)) return true;
        }
        return false;
    }

    public void next (double deltaTime) {
        particles.forEach(p -> p.next(deltaTime));
        lightning.forEach(l -> l.next(deltaTime));
        pulses.forEach(p -> p.next(deltaTime));
        pulses.removeIf(guiOutlinePulse::isDead);
        interactions.next(deltaTime);
        double dimTarget = inventoryOpen() ? INVENTORY_DIM : 0;
        double dimStep = INVENTORY_DIM*deltaTime/DIM_TIME;
        dim = dim < dimTarget ? Math.min(dimTarget, dim + dimStep) : Math.max(dimTarget, dim - dimStep);
        lightning = lightning.stream().filter(l -> !l.isDead()).collect(Collectors.toList());
        particles = particles.stream().filter(p -> !p.isDead()).collect(Collectors.toList());

        for (guiElement element : elements) element.slide(deltaTime);

        //The held item may have turned into another (sand into glass)
        if (cursor != null && cursor.getItem().latest() != cursor.getItem()) {
            item became = cursor.getItem().latest();
            cursor = new itemStack(became, Math.min(cursor.getCount(), became.getMaxStack()));
            double size = 50*s();
            interactions.changeBurst(became, mouseX - size/2, mouseY - size/2, size);
        }

        if (deltaTime > 0) {
            double blend = Math.min(1, deltaTime*25);
            mouseVx += ((mouseX - lastMouseX)/deltaTime - mouseVx)*blend;
            mouseVy += ((mouseY - lastMouseY)/deltaTime - mouseVy)*blend;
            double speed = Math.hypot(mouseX - lastMouseX, mouseY - lastMouseY)/deltaTime/Math.max(0.01, s());
            if (cursor != null && cursor.getItem().isShakeable()) {
                cursor.getItem().shake(Math.max(HELD_SHAKE, Math.min(1, speed/FULL_SHAKE_SPEED)), deltaTime);
            }
            //Jerked around hard enough, a held item that breaks from it (glass) works towards breaking. Only shaking
            //above SHAKE_BREAK of full counts, and full violence breaks it in about 1/SHAKE_BREAK_RATE seconds
            if (cursor != null && cursor.getItem().heatsWith(itemInfluence.SHAKE)) {
                double smooth = Math.min(1, Math.hypot(mouseVx, mouseVy)/Math.max(0.01, s())/FULL_SHAKE_SPEED);
                double violence = Math.max(0, (smooth - SHAKE_BREAK)/(1 - SHAKE_BREAK));
                cursor.getItem().expose(itemInfluence.SHAKE, violence*SHAKE_BREAK_RATE*deltaTime);
            }
            if (suddenTurn(deltaTime) && cursor != null && cursor.getItem().heatsWith(itemInfluence.SHAKE)) {
                cursor.getItem().expose(itemInfluence.SHAKE, TURN_HEAT);
            }
        }
        updateTrail(deltaTime);
        //What is under the mouse is looked at again every frame, not only when the mouse moves, so the info panel and
        //tooltips follow items that change under it (sand turning into glass) and screens that open or move
        hover(mouseX, mouseY);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (menuOverlay != null) {
            menuOverlay.slide(deltaTime);
            if (!menuOverlay.isDrawn()) menuOverlay = null;
        }
    }

    //Whether the mouse turned sharply this frame (see TURN_SPEED)
    private boolean suddenTurn (double deltaTime) {
        fastAge += deltaTime;
        double speed = Math.hypot(mouseVx, mouseVy)/Math.max(0.01, s());
        if (speed < TURN_SPEED) return false;
        boolean turned = false;
        if (fastAge < TURN_WINDOW) {
            double cos = (fastVx*mouseVx + fastVy*mouseVy)/(Math.hypot(fastVx, fastVy)*Math.hypot(mouseVx, mouseVy));
            turned = cos < Math.cos(Math.toRadians(TURN_ANGLE));
        }
        fastVx = mouseVx;
        fastVy = mouseVy;
        fastAge = 0;
        return turned;
    }

    private void updateTrail (double deltaTime) {
        trails.forEach(t -> t.next(deltaTime));
        trails.removeIf(t -> t != trail && t.isEmpty());

        item held = cursor == null ? null : cursor.getItem();
        if (held == null || !held.hasTrail()) {
            trail = null;
            return;
        }
        if (trail == null || trail.getItem() != held) {
            double s = s();
            trail = new guiTrail(held, Color.web(held.getTrailColor()), held.getTrailWidth()*s, held.getTrailLife(), 1100*s);
            trails.add(trail);
            trailPaused = true;
        }
        double speed = Math.hypot(mouseVx, mouseVy)/Math.max(0.01, s());
        if (speed < TRAIL_SPEED) {
            trailPaused = true;
            return;
        }
        //From the middle of the held item, with the swing (each bit of the stream keeps its own share of it)
        trail.add(mouseX, mouseY, mouseVx, mouseVy, trailPaused);
        trailPaused = false;
    }

    public void draw (GraphicsContext gc) {
        double screenWidth = handler.getWIDTH();
        double screenHeight = handler.getHEIGHT();

        boolean menu = handler.inMenu();
        //The hotbar waits for a menu sliding away and goes when the game is being left
        hud.setVisible(!menu && menuOverlay == null && !handler.isLeaving() && !inventoryOpen() && !handler.getBattle().isActive());
        battleScreen.setVisible(!menu && handler.getBattle().isActive());
        dialogueBox.setVisible(!menu && handler.getDiealoguecontainer().isActive());
        //The visuals page opened from the main menu is shown in its place
        mainMenu.setVisible(menu && !settingsOpen() && !menuLeaving);

        interactions.newFrame();
        gc.setGlobalAlpha(1);
        for (guiElement element : elements) {
            //The world (and whatever is under the inventory) goes a little darker behind it
            if (element == inventoryScreen && dim > 0) {
                gc.setFill(Color.rgb(0, 0, 0, dim));
                gc.fillRect(0, 0, screenWidth, screenHeight);
            }
            if (element.isDrawn()) element.drawSliding(gc, screenWidth, screenHeight);
        }

        if (menuOverlay != null) menuOverlay.drawSliding(gc, screenWidth, screenHeight);

        double s = s();
        //Streams behind moved items, under the item itself
        trails.forEach(t -> t.draw(gc));

        //Drawn like in a slot, centered on the mouse, and giving off its particles like in a slot
        if (cursor != null) {
            double size = 50*s;
            interactions.place(cursor.getItem(), mouseX - size/2, mouseY - size/2, size, true);
            slotGrid.drawStack(gc, cursor, mouseX - size/2, mouseY - size/2, size);
            //Only shaken items throw their particles along with the swing
            boolean shakes = cursor.getItem().isShakeable();
            cursor.getItem().spawnParticles(this, mouseX - size/2, mouseY - size/2, size, rdm, shakes ? mouseVx*0.7 : 0, shakes ? mouseVy*0.7 : 0);
        }

        //The glow of charged items, after everything so nothing drawn later cuts it off
        interactions.drawGlows(gc);

        //Over the held item too, so the ones it gives off are not hidden under it
        particles.forEach(p -> p.draw(gc));
        lightning.forEach(l -> l.draw(gc));
        pulses.forEach(p -> p.draw(gc));

        //The battle's popups over all of that
        if (battleScreen.isDrawn()) battleScreen.drawPopups(gc);

        if (cursor == null && tooltipBoxes != null && !tooltipBoxes.isEmpty()) {
            //Above the mouse, kept on the screen
            double w = 260*s, gap = 4*s;
            double h = -gap;
            for (infoBox box : tooltipBoxes) h += box.draw(gc, this, 0, 0, w, false) + gap;
            double tx = Math.max(0, Math.min(mouseX + 14*s, screenWidth - w));
            double ty = mouseY - h - 4*s;
            if (ty < 0) ty = Math.max(0, Math.min(mouseY + 20*s, screenHeight - h));
            for (infoBox box : tooltipBoxes) ty += box.draw(gc, this, Math.round(tx), Math.round(ty), w, true) + gap;
        } else if (cursor == null && tooltip != null) {
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
        //Nothing in the menu is clicked while it is switching to a game
        if (handler.inMenu() && handler.inTransition()) return true;
        for (int i = elements.size()-1; i >= 0; i--) {
            guiElement element = elements.get(i);
            boolean modal = element == inventoryScreen || element == settingsScreen || element == teleportScreen || element == mainMenu;
            if (element.isVisible() && (element.contains(mx, my) || modal) && element.click(mx, my, rightClick)) return true;
        }
        return false;
    }

    public boolean scroll (double mx, double my, double delta) {
        //An open menu takes all scrolling, so the world does not zoom behind it
        if (settingsOpen() || teleportsOpen()) return true;
        if (handler.inMenu()) return mainMenu.scroll(mx, my, delta);
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
        tooltipBoxes = null;
        for (guiElement element : elements) {
            if (element.isVisible()) element.hover(mx, my);
        }
    }
}
