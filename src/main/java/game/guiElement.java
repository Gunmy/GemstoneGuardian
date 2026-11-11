package game;

import javafx.scene.canvas.GraphicsContext;

//Something drawn on the GUI layer. Positions are in screen pixels and are set in layout,
//which is called every frame so the elements follow the window size
public abstract class guiElement {

    protected double x, y, width, height;
    protected boolean visible = true;
    protected gui gui;

    public guiElement (gui gui) {
        this.gui = gui;
    }

    public void setBounds (double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains (double mx, double my) {
        return visible && x <= mx && mx < x+width && y <= my && my < y+height;
    }

    public boolean isVisible () {return this.visible;}
    public void setVisible (boolean visible) {this.visible = visible;}

    public void layout (double screenWidth, double screenHeight) {}

    public abstract void draw (GraphicsContext gc);

    //Return true when the click was used, so that nothing behind gets it
    public boolean click (double mx, double my, boolean rightClick) {return false;}

    public boolean scroll (double mx, double my, double delta) {return false;}

    public void hover (double mx, double my) {}

    //Keys, for screens that want the keyboard. Return true when used, so the game does not get the key too
    public boolean keyPressed (javafx.scene.input.KeyCode code) {return false;}
    public boolean keyTyped (String typed) {return false;}

    //Sliding: an element given an edge slides in from it when shown and back out to it when hidden,
    //instead of popping. gui moves it along every frame (slide) and draws it moved (slideOffset)
    public enum edge {NONE, TOP, BOTTOM}
    public static final double SLIDE_TIME = 0.5;
    private edge slideEdge = edge.NONE;
    //0 is off screen, 1 in place
    private double shown = 1;

    //Starts where the element is now: in place when visible, off screen when not
    public void slideFrom (edge slideEdge) {
        this.slideEdge = slideEdge;
        this.shown = visible ? 1 : 0;
    }

    public void slide (double deltaTime) {
        if (slideEdge == edge.NONE) return;
        double step = deltaTime/SLIDE_TIME;
        shown = visible ? Math.min(1, shown + step) : Math.max(0, shown - step);
    }

    //Still on screen while sliding out after being hidden
    public boolean isDrawn () {
        return visible || (slideEdge != edge.NONE && shown > 0);
    }

    //How far down (negative is up) the element is drawn from where layout put it
    public double slideOffset (double screenHeight) {
        if (slideEdge == edge.NONE || shown >= 1) return 0;
        double t = shown*shown*(3 - 2*shown);
        double away = slideEdge == edge.TOP ? -(y + height + 4) : screenHeight - y + 4;
        return Math.round((1 - t)*away);
    }

    //Lays out and draws the element where its slide has it
    public void drawSliding (GraphicsContext gc, double screenWidth, double screenHeight) {
        layout(screenWidth, screenHeight);
        double offset = slideOffset(screenHeight);
        if (offset == 0) {
            draw(gc);
            return;
        }
        gc.save();
        gc.translate(0, offset);
        draw(gc);
        gc.restore();
    }
}
