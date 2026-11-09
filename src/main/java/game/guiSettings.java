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

    private guiButton collidersButton, bordersButton, sizeButton, fluidsButton;

    public guiSettings (gui gui) {
        super(gui);
        visible = false;
        gameHandler handler = gui.getHandler();

        mainButtons.add(new guiButton(gui, "Back to game", () -> gui.closeSettings()));
        mainButtons.add(new guiButton(gui, "Hidden secrets", () -> current = page.SECRETS));
        mainButtons.add(new guiButton(gui, "Visuals", () -> current = page.VISUALS));

        collidersButton = new guiButton(gui, "", () -> handler.Cpress());
        bordersButton = new guiButton(gui, "", () -> handler.Mpress());
        secretButtons.add(collidersButton);
        secretButtons.add(bordersButton);
        secretButtons.add(new guiButton(gui, "Back", () -> current = page.MAIN));

        sizeButton = new guiButton(gui, "", () -> handler.getSettings().cycleGuiSize());
        fluidsButton = new guiButton(gui, "", () -> handler.getSettings().toggleSimpleFluids());
        visualButtons.add(sizeButton);
        visualButtons.add(fluidsButton);
        visualButtons.add(new guiButton(gui, "Back", () -> current = page.MAIN));
    }

    @Override
    public void setVisible (boolean visible) {
        super.setVisible(visible);
        if (visible) current = page.MAIN;
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
        sizeButton.setText("GUI size: " + handler.getSettings().getGuiSize().label);
        fluidsButton.setText("Fluids: " + (handler.getSettings().simpleFluids() ? "Simple" : "Shader"));
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
            buttons.get(i).setBounds(x + pad, Math.round(y + pad + titleHeight + i*(buttonHeight+gap)), buttonWidth, buttonHeight);
        }
        updateTexts();
    }

    @Override
    public void draw (GraphicsContext gc) {
        double s = gui.s();
        gui.getSkin().drawPanel(gc, x, y, width, height);
        guiText.draw(gc, title(), x + width/2, y + gui.getSkin().getBorder() + 4*s, 16*s, Color.BLACK, TextAlignment.CENTER);
        for (guiButton button : currentButtons()) button.draw(gc);
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        for (guiButton button : currentButtons()) {
            if (button.contains(mx, my)) return button.click(mx, my, rightClick);
        }
        //The menu is modal, nothing behind it gets clicked
        return true;
    }

    @Override
    public void hover (double mx, double my) {
        for (guiButton button : currentButtons()) button.hover(mx, my);
    }
}
