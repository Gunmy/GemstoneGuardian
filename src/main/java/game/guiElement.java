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
    //instead of popping. gui moves it along every frame (slide) and draws it moved (slideOffset). Every menu comes in
    //from the top, taking MENU_SLIDE_TIME seconds; the fight's panel (guiBattle) and the hotbar from the bottom, taking
    //SLIDE_TIME. A menu that goes as another one comes (leaveDown, or another page of the same menu, swapPage) goes
    //out at the bottom while the new one comes down from the top, both at once; one closed with nothing in its place
    //goes back up
    public enum edge {NONE, TOP, BOTTOM}
    public static final double SLIDE_TIME = 0.25, MENU_SLIDE_TIME = 0.5;
    private edge slideEdge = edge.NONE;
    //0 is off screen, 1 in place
    private double shown = 1;
    //Going out at the bottom instead of back to its edge
    private boolean exitDown = false;
    //Whether it was shown at the last check (gui.next), to see it being hidden
    private boolean wasVisible = true;

    //Starts where the element is now: in place when visible, off screen when not
    public void slideFrom (edge slideEdge) {
        this.slideEdge = slideEdge;
        this.shown = visible ? 1 : 0;
        this.wasVisible = visible;
    }

    private double slideTime () {return slideEdge == edge.TOP ? MENU_SLIDE_TIME : SLIDE_TIME;}

    public void slide (double deltaTime) {
        if (slideEdge == edge.NONE) return;
        double step = deltaTime/slideTime();
        if (ghost != null) {
            ghostShown = Math.max(0, ghostShown - step);
            if (ghostShown <= 0) ghost = null;
        }
        //Back in from the top once it is out of sight
        if (visible || shown <= 0) exitDown = false;
        shown = visible ? Math.min(1, shown + step) : Math.max(0, shown - step);
    }

    //Called by gui once a frame: when it has just been hidden, it goes out at the bottom if down is true (another menu
    //is coming in at the same time), else back to its edge
    public boolean justHidden () {
        boolean hidden = wasVisible && !visible;
        wasVisible = visible;
        return hidden;
    }
    public boolean justShown () {return visible && !wasVisible;}
    public void leaveDown (boolean down) {
        if (shown > 0) exitDown = down;
    }

    //Going to another page of a menu: a picture of the page now (the ghost) goes out at the bottom while the menu,
    //changed (change switches the page), comes down from the top, both at once. Straight away when it is not on screen.
    //Nothing is clicked in the menu until the old page is gone (gui.click)
    private javafx.scene.image.WritableImage ghost;
    private double ghostShown, ghostY;
    private static javafx.scene.canvas.Canvas ghostCanvas;
    private static final javafx.scene.SnapshotParameters GHOST_PARAMS = new javafx.scene.SnapshotParameters();
    static {GHOST_PARAMS.setFill(javafx.scene.paint.Color.TRANSPARENT);}

    public void swapPage (Runnable change) {
        if (slideEdge == edge.NONE || !isDrawn() || shown <= 0) {
            change.run();
            return;
        }
        double w = gui.getHandler().getWIDTH(), h = gui.getHandler().getHEIGHT();
        int iw = Math.max(1, (int) Math.ceil(w)), ih = Math.max(1, (int) Math.ceil(h));
        if (ghostCanvas == null || ghostCanvas.getWidth() != iw || ghostCanvas.getHeight() != ih) {
            ghostCanvas = new javafx.scene.canvas.Canvas(iw, ih);
        }
        GraphicsContext g = ghostCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, iw, ih);
        g.setImageSmoothing(false);
        g.setGlobalAlpha(1);
        //Where it is drawn now, so the picture starts from there
        double offset = slideOffset(h);
        layout(w, h);
        g.save();
        g.translate(0, offset);
        draw(g);
        g.restore();
        ghost = ghostCanvas.snapshot(GHOST_PARAMS, new javafx.scene.image.WritableImage(iw, ih));
        ghostY = drawnTop() + offset;
        ghostShown = 1;
        change.run();
        shown = 0;
        exitDown = false;
    }
    public boolean isSwapping () {return ghost != null;}

    //Still on screen while sliding out after being hidden, or while its old page goes
    public boolean isDrawn () {
        return visible || (slideEdge != edge.NONE && shown > 0) || ghost != null;
    }

    //The top and bottom of everything the element draws (layout's box unless it draws outside it, like the
    //inventory's chest above it), so a slide takes all of it off the screen
    protected double drawnTop () {return y;}
    protected double drawnBottom () {return y + height;}

    //How far down (negative is up) the element is drawn from where layout put it
    public double slideOffset (double screenHeight) {
        if (slideEdge == edge.NONE || shown >= 1) return 0;
        double t = shown*shown*(3 - 2*shown);
        boolean down = slideEdge == edge.BOTTOM || (!visible && exitDown);
        double away = down ? screenHeight - drawnTop() + 4 : -(drawnBottom() + 4);
        return Math.round((1 - t)*away);
    }

    //Lays out and draws the element where its slide has it, and the old page going out under it
    public void drawSliding (GraphicsContext gc, double screenWidth, double screenHeight) {
        if (ghost != null) {
            double t = ghostShown*ghostShown*(3 - 2*ghostShown);
            gc.drawImage(ghost, 0, Math.round((1 - t)*(screenHeight - ghostY + 4)));
        }
        if (!visible && (slideEdge == edge.NONE || shown <= 0)) return;
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
