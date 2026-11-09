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
}
