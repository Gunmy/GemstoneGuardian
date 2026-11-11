package game;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The menu opened with escape. A page is a title and a column of buttons; the button texts are
//refreshed every frame so toggles always show their current state
public class guiSettings extends guiElement {

    private enum page {MAIN, SECRETS, VISUALS}
    private page current = page.MAIN;

    private List<guiButton> mainButtons = new ArrayList<guiButton>();
    private List<guiButton> secretButtons = new ArrayList<guiButton>();
    private List<guiButton> visualButtons = new ArrayList<guiButton>();

    private guiButton collidersButton, bordersButton, darknessButton, sizeButton, fluidsButton;
    //The player's level with a - and a + on each side of it
    private guiButton levelButton, levelDown, levelUp;
    //Small square buttons drawn at the left and right end of a button's row (the button itself gets narrower)
    private java.util.Map<guiButton, guiButton[]> sideButtons = new java.util.HashMap<guiButton, guiButton[]>();
    public static final int MAX_LEVEL = 100;
    //The player's walking speed, as a multiplier with a - and a + on each side of it
    private guiButton speedButton, speedDown, speedUp;
    private static final double[] SPEEDS = {1, 1.5, 2, 3, 4, 6};
    //The floor (world) the player is on, with a - (down) and a + (up) to move there at the same spot
    private guiButton floorButton, floorDown, floorUp;

    public guiSettings (gui gui) {
        super(gui);
        visible = false;
        gameHandler handler = gui.getHandler();

        mainButtons.add(new guiButton(gui, "Back to game", () -> gui.closeSettings()));
        mainButtons.add(new guiButton(gui, "Hidden secrets", () -> current = page.SECRETS));
        mainButtons.add(new guiButton(gui, "Visuals", () -> current = page.VISUALS));
        mainButtons.add(new guiButton(gui, "Save and quit to menu", () -> {
            gui.closeSettings();
            handler.quitToMenu();
        }));

        collidersButton = new guiButton(gui, "", () -> handler.Cpress());
        bordersButton = new guiButton(gui, "", () -> handler.Mpress());
        secretButtons.add(collidersButton);
        secretButtons.add(bordersButton);
        darknessButton = new guiButton(gui, "", () -> handler.Hpress());
        secretButtons.add(darknessButton);
        secretButtons.add(new guiButton(gui, "Teleports", () -> {
            gui.closeSettings();
            gui.openTeleports(handler.getAllTeleportTargets(), "Teleports", () -> gui.openSecrets());
        }));
        secretButtons.add(new guiButton(gui, "Give item", () -> {
            gui.closeSettings();
            gui.openGiveItems();
        }));
        levelButton = new guiButton(gui, "", () -> {});
        levelButton.setEnabled(false);
        levelDown = new guiButton(gui, "-", () -> changeLevel(-1));
        levelUp = new guiButton(gui, "+", () -> changeLevel(1));
        sideButtons.put(levelButton, new guiButton[] {levelDown, levelUp});
        secretButtons.add(levelButton);
        speedButton = new guiButton(gui, "", () -> {});
        speedButton.setEnabled(false);
        speedDown = new guiButton(gui, "-", () -> changeSpeed(-1));
        speedUp = new guiButton(gui, "+", () -> changeSpeed(1));
        sideButtons.put(speedButton, new guiButton[] {speedDown, speedUp});
        secretButtons.add(speedButton);
        floorButton = new guiButton(gui, "", () -> {});
        floorButton.setEnabled(false);
        floorDown = new guiButton(gui, "-", () -> handler.changeFloor(-1));
        floorUp = new guiButton(gui, "+", () -> handler.changeFloor(1));
        sideButtons.put(floorButton, new guiButton[] {floorDown, floorUp});
        secretButtons.add(floorButton);
        secretButtons.add(new guiButton(gui, "Back", () -> current = page.MAIN));

        sizeButton = new guiButton(gui, "", () -> handler.getSettings().cycleGuiSize());
        fluidsButton = new guiButton(gui, "", () -> handler.getSettings().toggleSimpleFluids());
        visualButtons.add(sizeButton);
        visualButtons.add(fluidsButton);
        visualButtons.add(new guiButton(gui, "Back", () -> {
            if (visualsOnly) gui.closeSettings();
            else current = page.MAIN;
        }));
    }

    //Opened from the main menu: only the visuals page, and back closes it
    private boolean visualsOnly = false;

    @Override
    public void setVisible (boolean visible) {
        super.setVisible(visible);
        if (visible) current = page.MAIN;
        visualsOnly = false;
    }

    //The visuals page on its own, for the main menu
    public void openVisuals () {
        setVisible(true);
        current = page.VISUALS;
        visualsOnly = true;
    }

    //Opens the menu straight on the hidden secrets page, for menus opened from there to go back to
    public void openSecrets () {
        setVisible(true);
        current = page.SECRETS;
    }

    private void changeLevel (int by) {
        character boy = gui.getHandler().getMainCharacter();
        boy.setLevel(Math.max(0, Math.min(MAX_LEVEL, boy.getLevel() + by)));
    }

    //The step in SPEEDS the player walks at now (the closest one)
    private int speedStep () {
        double now = gui.getHandler().getMainCharacter().getSpeedMultiplier();
        int best = 0;
        for (int i = 1; i < SPEEDS.length; i++) if (Math.abs(SPEEDS[i] - now) < Math.abs(SPEEDS[best] - now)) best = i;
        return best;
    }

    private void changeSpeed (int by) {
        int step = Math.max(0, Math.min(SPEEDS.length - 1, speedStep() + by));
        gui.getHandler().getMainCharacter().setSpeedMultiplier(SPEEDS[step]);
    }

    //The page's buttons and the side buttons of its rows: everything to draw, click and hover
    private List<guiButton> allButtons () {
        List<guiButton> all = new ArrayList<guiButton>(currentButtons());
        for (guiButton button : currentButtons()) {
            guiButton[] sides = sideButtons.get(button);
            if (sides != null) for (guiButton side : sides) all.add(side);
        }
        return all;
    }

    private List<guiButton> currentButtons () {
        switch (current) {
            case SECRETS: return secretButtons;
            case VISUALS: return visualButtons;
            default:      return mainButtons;
        }
    }

    private String title () {
        switch (current) {
            case SECRETS: return "Hidden secrets";
            case VISUALS: return "Visuals";
            default:      return "Settings";
        }
    }

    private void updateTexts () {
        gameHandler handler = gui.getHandler();
        collidersButton.setText("Phase through blocks: " + (handler.collidersOn() ? "Off" : "On"));
        bordersButton.setText("Show borders: " + (handler.infoOn() ? "On" : "Off"));
        darknessButton.setText("Darkness in caves: " + (handler.darknessOff() ? "Off" : "On"));
        sizeButton.setText("GUI size: " + handler.getSettings().getGuiSize().label);
        fluidsButton.setText("Fluids: " + (handler.getSettings().simpleFluids() ? "Simple" : "Shader"));
        int level = handler.getMainCharacter().getLevel();
        levelButton.setText("Level: " + level);
        levelDown.setEnabled(level > 0);
        levelUp.setEnabled(level < MAX_LEVEL);
        int step = speedStep();
        double speed = SPEEDS[step];
        speedButton.setText("Speed: x" + (speed == Math.floor(speed) ? String.valueOf((int) speed) : String.valueOf(speed)));
        speedDown.setEnabled(step > 0);
        speedUp.setEnabled(step < SPEEDS.length - 1);
        int world = handler.getMainCharacter().getWorld(), floor = handler.floorOf(world);
        floorButton.setText("Floor: " + handler.getWorld(world).getName());
        floorDown.setEnabled(floor > 0);
        floorUp.setEnabled(floor >= 0 && floor < gameHandler.FLOOR_ORDER.length - 1);
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        double s = gui.s();
        double pad = gui.getSkin().getBorder() + 8*s;
        double buttonWidth = 320*s, buttonHeight = 40*s, gap = 8*s, titleHeight = 30*s;
        List<guiButton> buttons = currentButtons();

        width = buttonWidth + 2*pad;
        height = 2*pad + titleHeight + buttons.size()*buttonHeight + (buttons.size()-1)*gap;
        x = Math.round((screenWidth - width)/2);
        y = Math.round((screenHeight - height)/2);

        for (int i = 0; i < buttons.size(); i++) {
            double by = Math.round(y + pad + titleHeight + i*(buttonHeight+gap));
            guiButton[] sides = sideButtons.get(buttons.get(i));
            if (sides == null) {
                buttons.get(i).setBounds(x + pad, by, buttonWidth, buttonHeight);
                continue;
            }
            //Square buttons at both ends, the button between them
            sides[0].setBounds(x + pad, by, buttonHeight, buttonHeight);
            sides[1].setBounds(x + pad + buttonWidth - buttonHeight, by, buttonHeight, buttonHeight);
            buttons.get(i).setBounds(x + pad + buttonHeight + gap, by, buttonWidth - 2*(buttonHeight + gap), buttonHeight);
        }
        updateTexts();
    }

    @Override
    public void draw (GraphicsContext gc) {
        double s = gui.s();
        gui.getSkin().drawPanel(gc, x, y, width, height);
        guiText.draw(gc, title(), x + width/2, y + gui.getSkin().getBorder() + 4*s, 16*s, Color.BLACK, TextAlignment.CENTER);
        for (guiButton button : allButtons()) button.draw(gc);

    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        for (guiButton button : allButtons()) {
            if (button.contains(mx, my)) return button.click(mx, my, rightClick);
        }
        //The menu is modal, nothing behind it gets clicked
        return true;
    }

    @Override
    public void hover (double mx, double my) {
        for (guiButton button : allButtons()) button.hover(mx, my);
    }
}
