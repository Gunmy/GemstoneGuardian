package game;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//A menu of places to teleport to: one button per target showing a mini map around it, with the world and chunk
//under it (a mapGrid). Clicking one closes the menu and teleports the player there. Opened by the teleports cheat
//in the settings (every healing pad) and by the teleporter item (the pads the player has stood on)
public class guiTeleports extends guiElement {

    private String title = "";
    private mapGrid grid;
    private guiButton backButton;
    private Runnable back;

    public guiTeleports (gui gui) {
        super(gui);
        visible = false;
        grid = new mapGrid(gui);
        backButton = new guiButton(gui, "Back", () -> {
            setVisible(false);
            if (back != null) back.run();
        });
    }

    //back is run when the back button is pressed, after the menu is closed (null just closes it)
    public void open (List<worldTile> targets, String title, Runnable back) {
        gameHandler handler = gui.getHandler();
        this.title = title;
        this.back = back;
        grid.clear();

        for (worldTile target : targets) {
            miniMap view = new miniMap(handler, target.getWorld(), target.getChunkX(), target.getChunkY(), 1, target.getTile());
            String label = handler.getWorld(target.getWorld()).getName() + " (" + target.getChunkX() + ", " + target.getChunkY() + ")";
            guiButton button = grid.add(view, label, null, () -> {
                setVisible(false);
                handler.teleportWithEffect(target.getWorld(), target.getX(), target.getY());
            });
            //Teleporting out of a fight is not allowed
            button.setEnabled(!handler.getBattle().isActive());
        }
        setVisible(true);
    }

    //The grid as big as fits on the screen, with the back button under it
    @Override
    public void layout (double screenWidth, double screenHeight) {
        double s = gui.s();
        double pad = gui.getSkin().getBorder() + 8*s;
        double buttonWidth = 320*s, buttonHeight = 40*s, titleHeight = 30*s;

        grid.measure(screenWidth - 40*s - 2*pad, screenHeight - 40*s - 2*pad - titleHeight - buttonHeight);
        width = Math.max(grid.getWidth(), buttonWidth) + 2*pad;
        height = 2*pad + titleHeight + grid.getHeight() + buttonHeight;
        x = Math.round((screenWidth - width)/2);
        y = Math.round((screenHeight - height)/2);

        grid.place(x + (width - grid.getWidth())/2, y + pad + titleHeight);
        backButton.setBounds(Math.round(x + (width - buttonWidth)/2), Math.round(y + height - pad - buttonHeight), buttonWidth, buttonHeight);
    }

    @Override
    public void draw (GraphicsContext gc) {
        double s = gui.s();
        gui.getSkin().drawPanel(gc, x, y, width, height);
        guiText.draw(gc, title, x + width/2, y + gui.getSkin().getBorder() + 4*s, 16*s, Color.BLACK, TextAlignment.CENTER);
        grid.draw(gc);
        backButton.draw(gc);
        if (grid.isEmpty()) {
            guiText.draw(gc, "No healing pads yet", x + width/2, y + height/2 - 20*s, 12*s, Color.BLACK, TextAlignment.CENTER);
        }
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        if (backButton.contains(mx, my)) return backButton.click(mx, my, rightClick);
        grid.click(mx, my, rightClick);
        //The menu is modal, nothing behind it gets clicked
        return true;
    }

    @Override
    public void hover (double mx, double my) {
        grid.hover(mx, my);
        backButton.hover(mx, my);
    }
}
