package game;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The first screen, drawn over the campaign's world drifting by (see gameHandler.inMenu). Pages:
//- main: play, settings (the visuals page of guiSettings), edit map (not made yet), quit
//- load: every savefile as a mini map of the 3x3 chunks around where the player was (a mapGrid), with an X to delete,
//  and start new campaign under it
//- new: three steps, one name each: the campaign, yourself, the girl (the first fight and the final boss).
//  Next (enter) goes on and starts the campaign after the last, back (escape) goes a step back, from the first to the list
//- delete: asks before a savefile is deleted
//Starting a game goes through the gameHandler's launcher, which builds a fresh game for it
public class guiMainMenu extends guiElement {

    private enum page {MAIN, LOAD, NEW, DELETE}
    private page current = page.MAIN;

    private List<guiButton> mainButtons = new ArrayList<guiButton>();
    private guiButton editMapButton;

    private mapGrid saves;
    private guiButton newCampaignButton, loadBackButton;

    //The steps of a new campaign: the campaign's name, the player's and the girl's, each with a title and a hint
    private guiTextField nameField, playerField, girlField;
    private List<guiTextField> newFields = new ArrayList<guiTextField>();
    private List<String> newTitles = java.util.Arrays.asList("Name the campaign", "What is your name?", "Who is your rival?");
    private List<String> newHints = java.util.Arrays.asList("Shown in the list of savefiles", "The hero of the story", "Your first fight, and your last");
    private int step = 0;
    private guiButton startButton, newBackButton;

    private saveFile toDelete;
    private guiButton deleteButton, cancelDeleteButton;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public guiMainMenu (gui gui) {
        super(gui);
        //Comes down from the top and goes back up
        visible = gui.getHandler().inMenu();
        slideFrom(edge.TOP);
        gameHandler handler = gui.getHandler();

        mainButtons.add(new guiButton(gui, "Play", () -> openLoad()));
        mainButtons.add(new guiButton(gui, "Settings", () -> gui.openVisuals()));
        //Will edit the campaign file (the map and what is in it). Not made yet
        editMapButton = new guiButton(gui, "Edit map", null);
        editMapButton.setEnabled(false);
        mainButtons.add(editMapButton);
        mainButtons.add(new guiButton(gui, "Quit game", () -> javafx.application.Platform.exit()));

        //At most three campaigns side by side
        saves = new mapGrid(gui).maxColumns(3).scrollable();
        newCampaignButton = new guiButton(gui, "Start new campaign", () -> openNew());
        loadBackButton = new guiButton(gui, "Back", () -> current = page.MAIN);

        nameField = new guiTextField(gui, 24);
        playerField = new guiTextField(gui, 16);
        girlField = new guiTextField(gui, 16);
        newFields.add(nameField);
        newFields.add(playerField);
        newFields.add(girlField);
        for (guiTextField field : newFields) field.onEnter(() -> nextStep());
        startButton = new guiButton(gui, "Next", () -> nextStep());
        newBackButton = new guiButton(gui, "Back", () -> previousStep());

        deleteButton = new guiButton(gui, "Delete", () -> {
            if (toDelete != null) toDelete.delete();
            toDelete = null;
            openLoad();
        });
        cancelDeleteButton = new guiButton(gui, "Cancel", () -> openLoad());
    }

    //The savefiles are read again every time the list is opened
    private void openLoad () {
        gameHandler handler = gui.getHandler();
        saves.clear();
        for (saveFile save : saveFile.list()) {
            int world = save.getPlayerWorld();
            if (handler.getWorld(world) == null) world = 0;
            double px = save.getPlayerX(), py = save.getPlayerY();
            tile standing = handler.getWorld(world).getTile(px, py);
            miniMap view = new miniMap(handler, world, (int) px, (int) py, 1, standing);
            saves.add(view, save.getName(), describe(save), () -> handler.playWithEffect(save));
            saves.addDelete(() -> {
                toDelete = save;
                current = page.DELETE;
            });
        }
        current = page.LOAD;
    }

    //"Lvl 3, 12 min, 10 Nov 2026"
    private String describe (saveFile save) {
        long minutes = Math.round(save.getPlayTime()/60);
        String time = minutes < 60 ? minutes + " min" : minutes/60 + " h " + minutes%60 + " min";
        String date = DATE.format(Instant.ofEpochMilli(save.getLastPlayed()).atZone(ZoneId.systemDefault()));
        return "Lvl " + save.getLevel() + ", " + time + ", " + date;
    }

    private void openNew () {
        nameField.setText("Campaign " + (saveFile.list().size() + 1));
        playerField.setText("Boy");
        girlField.setText("Girl");
        step = 0;
        current = page.NEW;
    }

    private boolean stepFilled () {
        return !newFields.get(step).getText().trim().isEmpty();
    }

    private void nextStep () {
        if (!stepFilled()) return;
        if (step < newFields.size() - 1) step++;
        else startNew();
    }

    private void previousStep () {
        if (step > 0) step--;
        else openLoad();
    }

    private void startNew () {
        saveFile save = saveFile.create(nameField.getText().trim());
        save.set(gameHandler.PLAYER_NAME, playerField.getText().trim());
        save.set(gameHandler.GIRL_NAME, girlField.getText().trim());
        gui.getHandler().playWithEffect(save);
    }

    //Escape goes back a page
    public void back () {
        switch (current) {
            case LOAD:   current = page.MAIN; break;
            case NEW:    previousStep(); break;
            case DELETE: openLoad(); break;
            default:     break;
        }
    }

    private String title () {
        switch (current) {
            case LOAD:   return "Load campaign";
            case NEW:    return newTitles.get(step);
            case DELETE: return "Delete campaign?";
            default:     return "Gemstone Guardian";
        }
    }

    private List<guiElement> currentElements () {
        List<guiElement> list = new ArrayList<guiElement>();
        switch (current) {
            case LOAD:   list.add(newCampaignButton); list.add(loadBackButton); break;
            case NEW:    list.add(newFields.get(step)); list.add(startButton); list.add(newBackButton); break;
            case DELETE: list.add(deleteButton); list.add(cancelDeleteButton); break;
            default:     list.addAll(mainButtons); break;
        }
        return list;
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        double s = gui.s();
        double pad = gui.getSkin().getBorder() + 8*s;
        double buttonWidth = 320*s, buttonHeight = 40*s, gap = 8*s;
        double titleHeight = (current == page.MAIN ? 44 : 30)*s;
        double textHeight = guiText.height(12*s) + 10*s;

        switch (current) {
            case LOAD: {
                saves.measure(screenWidth - 40*s - 2*pad, screenHeight - 40*s - 2*pad - titleHeight - 2*buttonHeight - gap);
                double gridHeight = saves.isEmpty() ? 60*s : saves.getHeight();
                width = Math.max(saves.getWidth(), buttonWidth) + 2*pad;
                height = 2*pad + titleHeight + gridHeight + 2*buttonHeight + gap;
                setPosition(screenWidth, screenHeight);
                saves.place(x + (width - saves.getWidth())/2, y + pad + titleHeight);
                newCampaignButton.setBounds(Math.round(x + (width - buttonWidth)/2), Math.round(y + height - pad - 2*buttonHeight - gap), buttonWidth, buttonHeight);
                loadBackButton.setBounds(Math.round(x + (width - buttonWidth)/2), Math.round(y + height - pad - buttonHeight), buttonWidth, buttonHeight);
                break;
            }
            case NEW: {
                width = buttonWidth + 2*pad;
                height = 2*pad + titleHeight + textHeight + buttonHeight + 2*gap + 2*buttonHeight + gap;
                setPosition(screenWidth, screenHeight);
                double top = y + pad + titleHeight + textHeight;
                newFields.get(step).setBounds(x + pad, Math.round(top), buttonWidth, buttonHeight);
                double buttonsTop = top + buttonHeight + 2*gap;
                startButton.setBounds(x + pad, Math.round(buttonsTop), buttonWidth, buttonHeight);
                newBackButton.setBounds(x + pad, Math.round(buttonsTop + buttonHeight + gap), buttonWidth, buttonHeight);
                startButton.setText(step == newFields.size() - 1 ? "Start" : "Next");
                startButton.setEnabled(stepFilled());
                break;
            }
            case DELETE: {
                width = buttonWidth + 2*pad;
                height = 2*pad + titleHeight + 2*textHeight + 2*buttonHeight + gap;
                setPosition(screenWidth, screenHeight);
                double top = y + pad + titleHeight + 2*textHeight;
                deleteButton.setBounds(x + pad, Math.round(top), buttonWidth, buttonHeight);
                cancelDeleteButton.setBounds(x + pad, Math.round(top + buttonHeight + gap), buttonWidth, buttonHeight);
                break;
            }
            default: {
                width = buttonWidth + 2*pad;
                height = 2*pad + titleHeight + mainButtons.size()*buttonHeight + (mainButtons.size()-1)*gap;
                setPosition(screenWidth, screenHeight);
                for (int i = 0; i < mainButtons.size(); i++) {
                    mainButtons.get(i).setBounds(x + pad, Math.round(y + pad + titleHeight + i*(buttonHeight+gap)), buttonWidth, buttonHeight);
                }
            }
        }
    }

    private void setPosition (double screenWidth, double screenHeight) {
        x = Math.round((screenWidth - width)/2);
        y = Math.round((screenHeight - height)/2);
    }

    @Override
    public void draw (GraphicsContext gc) {
        double s = gui.s();
        double border = gui.getSkin().getBorder();
        double pad = border + 8*s;
        gui.getSkin().drawPanel(gc, x, y, width, height);
        double titleSize = (current == page.MAIN ? 22 : 16)*s;
        guiText.draw(gc, title(), x + width/2, y + border + 4*s, titleSize, Color.BLACK, TextAlignment.CENTER);

        double titleHeight = (current == page.MAIN ? 44 : 30)*s;
        double textY = y + pad + titleHeight;
        if (current == page.LOAD) {
            saves.draw(gc);
            if (saves.isEmpty()) {
                guiText.draw(gc, "No savefiles yet", x + width/2, textY + 20*s, 12*s, Color.BLACK, TextAlignment.CENTER);
            }
        } else if (current == page.NEW) {
            guiText.draw(gc, newHints.get(step), x + width/2, textY, 11*s, Color.rgb(60, 60, 60), TextAlignment.CENTER);
        } else if (current == page.DELETE && toDelete != null) {
            double size = 12*s;
            String name = toDelete.getName();
            while (size > 7 && guiText.width(name, size) > width - 2*pad) size -= 0.5;
            guiText.draw(gc, name, x + width/2, textY, size, Color.BLACK, TextAlignment.CENTER);
            guiText.draw(gc, "will be gone for good", x + width/2, textY + guiText.height(12*s) + 10*s, 12*s, Color.BLACK, TextAlignment.CENTER);
        }

        for (guiElement element : currentElements()) element.draw(gc);
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        for (guiElement element : currentElements()) {
            if (element.contains(mx, my)) {
                return element.click(mx, my, rightClick);
            }
        }
        if (current == page.LOAD) saves.click(mx, my, rightClick);
        //The menu is modal, nothing behind it gets clicked
        return true;
    }

    @Override
    public boolean scroll (double mx, double my, double delta) {
        //The savefile list scrolls; the rest of the menu takes the wheel too, so nothing behind it zooms
        if (current == page.LOAD && saves.scroll(mx, my, delta)) saves.hover(mx, my);
        return true;
    }

    @Override
    public void hover (double mx, double my) {
        for (guiElement element : currentElements()) element.hover(mx, my);
        if (current == page.LOAD) saves.hover(mx, my);
        if (current == page.MAIN && editMapButton.contains(mx, my)) gui.setTooltip("Coming soon");
    }

    //The menu has the keyboard: the name field gets the keys on its page, escape goes back
    @Override
    public boolean keyPressed (KeyCode code) {
        if (code == KeyCode.ESCAPE) {
            back();
            return true;
        }
        if (current == page.NEW) {
            newFields.get(step).keyPressed(code);
        }
        return true;
    }

    @Override
    public boolean keyTyped (String typed) {
        if (current == page.NEW) newFields.get(step).keyTyped(typed);
        return true;
    }
}
