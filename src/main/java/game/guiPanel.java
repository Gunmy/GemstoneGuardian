package game;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;

//A box with other elements inside it. Clicks inside the box never fall through to the game
public class guiPanel extends guiElement {

    protected List<guiElement> children = new ArrayList<guiElement>();
    protected boolean background = true;

    public guiPanel (gui gui) {
        super(gui);
    }

    public void add (guiElement child) {
        children.add(child);
    }

    @Override
    public void draw (GraphicsContext gc) {
        if (background) gui.getSkin().drawPanel(gc, x, y, width, height);
        for (guiElement child : children) {
            if (child.isVisible()) child.draw(gc);
        }
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        for (int i = children.size()-1; i >= 0; i--) {
            guiElement child = children.get(i);
            if (child.contains(mx, my) && child.click(mx, my, rightClick)) return true;
        }
        return contains(mx, my);
    }

    @Override
    public boolean scroll (double mx, double my, double delta) {
        for (int i = children.size()-1; i >= 0; i--) {
            guiElement child = children.get(i);
            if (child.contains(mx, my) && child.scroll(mx, my, delta)) return true;
        }
        return contains(mx, my);
    }

    @Override
    public void hover (double mx, double my) {
        for (guiElement child : children) {
            child.hover(mx, my);
        }
    }
}
