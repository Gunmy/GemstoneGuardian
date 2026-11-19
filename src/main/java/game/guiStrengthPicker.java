package game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.scene.canvas.GraphicsContext;

//Picks how strong the computer plays (aiStrength): a button that switches the judge (Calculated / Trained) and the
//depth with a - and a + at the ends of its row (the side button pattern of the settings). Used by the new campaign's
//last step (guiMainMenu) and the 1v1 page's computer strength (guiDuelMenu). Two 40 high rows with a gap of 8.
//The new campaign's can only look so far ahead (maxDepth) and has a third row: what that comes to as a difficulty
//(aiStrength.difficulty), Easy, Medium or Hard on green, orange or red
public class guiStrengthPicker {

    private final guiButton judgeButton, depthButton, depthDown, depthUp, difficultyButton;
    private final Supplier<aiStrength> get;
    private final int maxDepth;
    private final boolean showDifficulty;

    public guiStrengthPicker (gui gui, Supplier<aiStrength> get, Consumer<aiStrength> set) {
        this(gui, get, set, aiStrength.MAX_DEPTH, false);
    }

    public guiStrengthPicker (gui gui, Supplier<aiStrength> get, Consumer<aiStrength> set, int maxDepth, boolean showDifficulty) {
        this.get = get;
        this.maxDepth = maxDepth;
        this.showDifficulty = showDifficulty;
        judgeButton = new guiButton(gui, "", () -> set.accept(get.get().withTrained(!get.get().trained)));
        depthButton = new guiButton(gui, "", () -> {});
        depthButton.setEnabled(false);
        depthDown = new guiButton(gui, "-", () -> set.accept(get.get().withDepth(get.get().depth - 1)));
        depthUp = new guiButton(gui, "+", () -> set.accept(get.get().withDepth(Math.min(maxDepth, get.get().depth + 1))));
        //Only shows the difficulty: no hover, no click
        difficultyButton = new guiButton(gui, "", null).displayOnly();
    }

    //Every button, to draw, click and hover
    public List<guiElement> elements () {
        List<guiElement> list = new ArrayList<guiElement>();
        list.add(judgeButton);
        list.add(depthButton);
        list.add(depthDown);
        list.add(depthUp);
        if (showDifficulty) list.add(difficultyButton);
        return list;
    }

    //How tall the rows are
    public double height (double s) {return (showDifficulty ? 3 : 2)*40*s + (showDifficulty ? 2 : 1)*8*s;}

    //Lays the rows out from (x, y), width wide, and refreshes their texts
    public void layout (double x, double y, double width, double s) {
        double buttonHeight = 40*s, gap = 8*s;
        judgeButton.setBounds(x, Math.round(y), width, buttonHeight);
        double row = Math.round(y + buttonHeight + gap);
        depthDown.setBounds(x, row, buttonHeight, buttonHeight);
        depthUp.setBounds(x + width - buttonHeight, row, buttonHeight, buttonHeight);
        depthButton.setBounds(x + buttonHeight + gap, row, width - 2*(buttonHeight + gap), buttonHeight);
        difficultyButton.setBounds(x, Math.round(row + buttonHeight + gap), width, buttonHeight);

        aiStrength now = get.get();
        judgeButton.setText("Judge: " + now.judgeLabel());
        depthButton.setText("Looks ahead: " + now.depth + (now.depth == 1 ? " action" : " actions"));
        depthDown.setEnabled(now.depth > aiStrength.MIN_DEPTH);
        depthUp.setEnabled(now.depth < Math.min(maxDepth, aiStrength.MAX_DEPTH));
        aiStrength.difficulty level = now.difficulty();
        difficultyButton.setText("Difficulty: " + level.label);
        difficultyButton.setColor(level.color);
    }

    public void draw (GraphicsContext gc) {
        for (guiElement e : elements()) e.draw(gc);
    }
}
