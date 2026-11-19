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
//- main: play, settings (the visuals page of guiSettings), 1v1, quit
//- load: every savefile as a mini map of the 3x3 chunks around where the player was (a mapGrid), with an X to delete,
//  and start new campaign under it
//- new: three steps, one name each: the campaign, yourself, the girl (the first fight and the final boss), then how
//  strong the computer plays (aiStrength, kept in the savefile for good, like a difficulty).
//  Next (enter) goes on and starts the campaign after the last, back (escape) goes a step back, from the first to the list
//- delete: asks before a savefile is deleted
//- duel: the 1v1 pages, handed to guiDuelMenu
//- daily: the daily battles (their loadout, or how today's try went), handed to guiDailyMenu
//Starting a game goes through the gameHandler's launcher, which builds a fresh game for it
public class guiMainMenu extends guiElement {

    private enum page {MAIN, LOAD, NEW, DELETE, DUEL, DAILY}
    private page current = page.MAIN;

    private List<guiButton> mainButtons = new ArrayList<guiButton>();

    //The 1v1 pages. Set returnToDuel when a 1v1 ends, so the menu opens on them again (gameHandler.quitToMenu)
    private guiDuelMenu duelMenu;
    public static boolean returnToDuel = false;

    //The daily battles' pages. returnToDaily: set when a run of them is left, so the menu opens on how it went
    private guiDailyMenu dailyMenu;
    public static boolean returnToDaily = false;
    //Its button on the first page: "Play daily battles", or the time until the next ones once today's try is used
    private guiButton dailyButton;

    private mapGrid saves;
    private guiButton newCampaignButton, loadBackButton;

    //The steps of a new campaign: the campaign's name, the player's and the girl's, each with a title and a hint
    private guiTextField nameField, playerField, girlField;
    private List<guiTextField> newFields = new ArrayList<guiTextField>();
    private List<String> newTitles = java.util.Arrays.asList("Name the campaign", "What is your name?", "Who is your rival?", "Opponents");
    private List<String> newHints = java.util.Arrays.asList("Shown in the list of savefiles", "The hero of the story", "Your first fight, and your last",
        "How strong they play. Kept for this campaign");
    //The last step: how strong the computer plays, with no text field (newFields only has the name steps)
    private static final int STEPS = 4;
    private aiStrength newStrength = aiStrength.CAMPAIGN;
    private guiStrengthPicker strengthPicker;
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

        dailyMenu = new guiDailyMenu(gui, () -> show(page.MAIN));
        dailyButton = new guiButton(gui, "Play daily battles", () -> openDaily());
        mainButtons.add(dailyButton);
        mainButtons.add(new guiButton(gui, "Play story", () -> openLoad()));
        duelMenu = new guiDuelMenu(gui, () -> show(page.MAIN));
        mainButtons.add(new guiButton(gui, "Play 1v1s", () -> show(page.DUEL)));
        mainButtons.add(new guiButton(gui, "Settings", () -> gui.openVisuals()));
        mainButtons.add(new guiButton(gui, "Quit game", () -> javafx.application.Platform.exit()));

        //At most three campaigns side by side
        saves = new mapGrid(gui).maxColumns(3).scrollable();
        newCampaignButton = new guiButton(gui, "Start new campaign", () -> openNew());
        loadBackButton = new guiButton(gui, "Back", () -> show(page.MAIN));

        nameField = new guiTextField(gui, 24);
        playerField = new guiTextField(gui, 16);
        girlField = new guiTextField(gui, 16);
        newFields.add(nameField);
        newFields.add(playerField);
        newFields.add(girlField);
        for (guiTextField field : newFields) field.onEnter(() -> nextStep());
        //At most CAMPAIGN_MAX_DEPTH ahead, with the difficulty it comes to under it
        strengthPicker = new guiStrengthPicker(gui, () -> newStrength, chosen -> newStrength = chosen, aiStrength.CAMPAIGN_MAX_DEPTH, true);
        startButton = new guiButton(gui, "Next", () -> nextStep());
        newBackButton = new guiButton(gui, "Back", () -> previousStep());

        deleteButton = new guiButton(gui, "Delete", () -> {
            if (toDelete != null) toDelete.delete();
            toDelete = null;
            openLoad();
        });
        cancelDeleteButton = new guiButton(gui, "Cancel", () -> openLoad());

        //Back from a 1v1. Only the menu of the game behind it takes the flag down, the copy that slides down over the
        //ending fight opens on the 1v1 page too
        if (returnToDuel) current = page.DUEL;
        if (handler.inMenu()) returnToDuel = false;
        if (returnToDaily) {
            dailyMenu.open();
            current = page.DAILY;
        }
        if (handler.inMenu()) returnToDaily = false;
    }

    //Straight into the loadout (in the inventory, with Play), or how today's try went once it is used
    private void openDaily () {
        if (!guiDailyMenu.hasResult()) {
            guiDailyMenu.editLoadout(gui);
            return;
        }
        swapPage(() -> {
            dailyMenu.open();
            current = page.DAILY;
        });
    }

    //Another page: the old one goes out at the bottom as the new one comes down (guiElement.swapPage)
    private void show (page next) {swapPage(() -> current = next);}

    //The savefiles are read again every time the list is opened
    private void openLoad () {swapPage(() -> readLoad());}

    private void readLoad () {
        gameHandler handler = gui.getHandler();
        saves.clear();
        for (saveFile save : saveFile.list()) {
            int world = save.getPlayerWorld();
            if (handler.getWorld(world) == null) world = 0;
            double px = save.getPlayerX(), py = save.getPlayerY();
            tile standing = handler.getWorld(world).getTile(px, py);
            miniMap view = new miniMap(handler, world, (int) px, (int) py, 1, standing);
            saves.add(view, save.getName(), describe(save), () -> handler.playWithEffect(save));
            saves.addDelete(() -> swapPage(() -> {
                toDelete = save;
                current = page.DELETE;
            }));
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
        swapPage(() -> {
            nameField.setText("Campaign " + (saveFile.list().size() + 1));
            playerField.setText("Boy");
            girlField.setText("Girl");
            newStrength = aiStrength.CAMPAIGN;
            step = 0;
            current = page.NEW;
        });
    }

    //The steps with a text field: step < newFields.size()
    private boolean nameStep () {return step < newFields.size();}

    private boolean stepFilled () {
        return !nameStep() || !newFields.get(step).getText().trim().isEmpty();
    }

    private void nextStep () {
        if (!stepFilled()) return;
        if (step < STEPS - 1) swapPage(() -> step++);
        else startNew();
    }

    private void previousStep () {
        if (step > 0) swapPage(() -> step--);
        else openLoad();
    }

    private void startNew () {
        saveFile save = saveFile.create(nameField.getText().trim());
        save.set(gameHandler.PLAYER_NAME, playerField.getText().trim());
        save.set(gameHandler.GIRL_NAME, girlField.getText().trim());
        newStrength.saveTo(save);
        gui.getHandler().playWithEffect(save);
    }

    //Escape goes back a page
    public void back () {
        switch (current) {
            case LOAD:   show(page.MAIN); break;
            case NEW:    previousStep(); break;
            case DELETE: openLoad(); break;
            case DUEL:   duelMenu.back(); break;
            case DAILY:  dailyMenu.back(); break;
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
            case NEW:
                if (nameStep()) list.add(newFields.get(step));
                else list.addAll(strengthPicker.elements());
                list.add(startButton);
                list.add(newBackButton);
                break;
            case DELETE: list.add(deleteButton); list.add(cancelDeleteButton); break;
            default:     list.addAll(mainButtons); break;
        }
        return list;
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        if (current == page.DUEL) {
            duelMenu.layout(screenWidth, screenHeight);
            setBounds(duelMenu.x, duelMenu.y, duelMenu.width, duelMenu.height);
            return;
        }
        if (current == page.DAILY) {
            dailyMenu.layout(screenWidth, screenHeight);
            setBounds(dailyMenu.x, dailyMenu.y, dailyMenu.width, dailyMenu.height);
            return;
        }
        //Green (as Save) while today's try is there, red (as Cancel) once it is used
        boolean played = daily.shared().playedToday();
        dailyButton.setText(played ? "Daily battles: next in " + daily.countdown() : "Play daily battles");
        dailyButton.setColor(played ? "#E04040" : "#40C040");
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
                //A name step has its text field, the last step the strength's two rows (guiStrengthPicker)
                double content = nameStep() ? buttonHeight : strengthPicker.height(s);
                width = buttonWidth + 2*pad;
                height = 2*pad + titleHeight + textHeight + content + 2*gap + 2*buttonHeight + gap;
                setPosition(screenWidth, screenHeight);
                double top = y + pad + titleHeight + textHeight;
                if (nameStep()) newFields.get(step).setBounds(x + pad, Math.round(top), buttonWidth, buttonHeight);
                else strengthPicker.layout(x + pad, top, buttonWidth, s);
                double buttonsTop = top + content + 2*gap;
                startButton.setBounds(x + pad, Math.round(buttonsTop), buttonWidth, buttonHeight);
                newBackButton.setBounds(x + pad, Math.round(buttonsTop + buttonHeight + gap), buttonWidth, buttonHeight);
                startButton.setText(step == STEPS - 1 ? "Start" : "Next");
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
        if (current == page.DUEL) {
            duelMenu.draw(gc);
            return;
        }
        if (current == page.DAILY) {
            dailyMenu.draw(gc);
            return;
        }
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
        if (current == page.DUEL) return duelMenu.click(mx, my, rightClick);
        if (current == page.DAILY) return dailyMenu.click(mx, my, rightClick);
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
        if (current == page.DUEL) {
            duelMenu.hover(mx, my);
            return;
        }
        if (current == page.DAILY) {
            dailyMenu.hover(mx, my);
            return;
        }
        for (guiElement element : currentElements()) element.hover(mx, my);
        if (current == page.LOAD) saves.hover(mx, my);
    }

    //The menu has the keyboard: the name field gets the keys on its page, escape goes back
    @Override
    public boolean keyPressed (KeyCode code) {
        if (current == page.DUEL) return duelMenu.keyPressed(code);
        if (current == page.DAILY) return dailyMenu.keyPressed(code);
        if (code == KeyCode.ESCAPE) {
            back();
            return true;
        }
        if (current == page.NEW) {
            if (nameStep()) newFields.get(step).keyPressed(code);
            else if (code == KeyCode.ENTER) nextStep();
        }
        return true;
    }

    @Override
    public boolean keyTyped (String typed) {
        if (current == page.DUEL) return duelMenu.keyTyped(typed);
        if (current == page.DAILY) return dailyMenu.keyTyped(typed);
        if (current == page.NEW && nameStep()) newFields.get(step).keyTyped(typed);
        return true;
    }
}
