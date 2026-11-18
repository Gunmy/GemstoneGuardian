package game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.scene.canvas.GraphicsContext;

//Picks how strong the computer plays (aiStrength): a button that switches the judge (Calculated / Trained) and the
//depth with a - and a + at the ends of its row (the side button pattern of the settings). Used by the new campaign's
//last step (guiMainMenu) and the 1v1 page's computer strength (guiDuelMenu). Two 40 high rows with a gap of 8
public class guiStrengthPicker {

    private final guiButton judgeButton, depthButton, depthDown, depthUp;
    private final Supplier<aiStrength> get;

    public guiStrengthPicker (gui gui, Supplier<aiStrength> get, Consumer<aiStrength> set) {
        this.get = get;
        judgeButton = new guiButton(gui, "", () -> set.accept(get.get().withTrained(!get.get().trained)));
        depthButton = new guiButton(gui, "", () -> {});
        depthButton.setEnabled(false);
        depthDown = new guiButton(gui, "-", () -> set.accept(get.get().withDepth(get.get().depth - 1)));
        depthUp = new guiButton(gui, "+", () -> set.accept(get.get().withDepth(get.get().depth + 1)));
    }

    //Every button, to draw, click and hover
    public List<guiElement> elements () {
        List<guiElement> list = new ArrayList<guiElement>();
        list.add(judgeButton);
        list.add(depthButton);
        list.add(depthDown);
        list.add(depthUp);
        return list;
    }

    //How tall the two rows are
    public static double height (double s) {return 2*40*s + 8*s;}

    //Lays the rows out from (x, y), width wide, and refreshes their texts
    public void layout (double x, double y, double width, double s) {
        double buttonHeight = 40*s, gap = 8*s;
        judgeButton.setBounds(x, Math.round(y), width, buttonHeight);
        double row = Math.round(y + buttonHeight + gap);
        depthDown.setBounds(x, row, buttonHeight, buttonHeight);
        depthUp.setBounds(x + width - buttonHeight, row, buttonHeight, buttonHeight);
        depthButton.setBounds(x + buttonHeight + gap, row, width - 2*(buttonHeight + gap), buttonHeight);

        aiStrength now = get.get();
        judgeButton.setText("Judge: " + now.judgeLabel());
        depthButton.setText("Looks ahead: " + now.depth + (now.depth == 1 ? " action" : " actions"));
        depthDown.setEnabled(now.depth > aiStrength.MIN_DEPTH);
        depthUp.setEnabled(now.depth < aiStrength.MAX_DEPTH);
    }

    public void draw (GraphicsContext gc) {
        for (guiElement e : elements()) e.draw(gc);
    }
}
