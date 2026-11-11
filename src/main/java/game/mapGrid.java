package game;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//A grid of mini map buttons with a label under each (and an optional smaller second line), as big as fits
//in the room it is given, at most 140 wide. A map can have a small X button in its corner (deleting a savefile).
//Not an element on its own: a menu measures it, places it and passes the mouse on. Used by the teleports menu
//and by the savefile list in the main menu.
//A scrollable grid (scrollable()) never shrinks its maps to fit them all: they keep their full size, the grid is only
//as tall as the room it is given, and the mouse wheel (scroll) moves the rows up and down inside it, cut off at its
//edges, with a thin bar at the right showing where you are
public class mapGrid {

    private static class entry {
        guiButton button;
        guiButton delete;
        miniMap view;
        String label;
        String subLabel;
    }

    private gui gui;
    private List<entry> entries = new ArrayList<entry>();
    private double viewSize, width, height;
    private int cols, rows;

    public mapGrid (gui gui) {
        this.gui = gui;
    }

    //The most columns the grid may have (0: as many as fit)
    private int maxColumns = 0;
    public mapGrid maxColumns (int maxColumns) {
        this.maxColumns = maxColumns;
        return this;
    }

    private boolean scrollable = false;
    public mapGrid scrollable () {
        this.scrollable = true;
        return this;
    }
    //How far the rows are scrolled up, in screen pixels, and the whole height of the rows
    private double scrolled = 0, contentHeight = 0;
    //Where the grid is on the screen (set by place)
    private double gridX, gridY;

    public void clear () {entries.clear();}
    public boolean isEmpty () {return entries.isEmpty();}

    //Adds a map. The returned button can be disabled. subLabel may be null
    public guiButton add (miniMap view, String label, String subLabel, Runnable action) {
        entry e = new entry();
        e.view = view;
        e.label = label;
        e.subLabel = subLabel;
        e.button = new guiButton(gui, "", action);
        guiButton button = e.button;
        button.setFace(gc -> drawView(gc, button, view));
        entries.add(e);
        return button;
    }

    //Gives the map added last an X button in its corner
    public void addDelete (Runnable delete) {
        if (entries.isEmpty()) throw new IllegalStateException("Ingen kart å legge slett-knapp på");
        guiButton x = new guiButton(gui, "X", delete);
        x.setTextSize(18);
        entries.get(entries.size()-1).delete = x;
    }

    //The mini map inside the button's frame, lightened while hovered
    private void drawView (GraphicsContext gc, guiButton button, miniMap view) {
        double inset = Math.round(gui.getSkin().getBorder()*0.5);
        int size = (int) Math.round(button.width - 2*inset);
        double vx = Math.round(button.x + inset), vy = Math.round(button.y + inset);
        gc.drawImage(view.get(size), vx, vy);
        if (button.isHovered() && button.isEnabled()) {
            gc.setFill(Color.rgb(255, 255, 255, 0.2));
            gc.fillRect(vx, vy, size, size);
        } else if (!button.isEnabled()) {
            gc.setFill(Color.rgb(0, 0, 0, 0.4));
            gc.fillRect(vx, vy, size, size);
        }
    }

    private double gap () {return 8*gui.s();}
    private double labelHeight () {return guiText.height(10*gui.s()) + 6*gui.s();}
    private double subLabelHeight () {return guiText.height(8*gui.s()) + 2*gui.s();}

    private double textHeight () {
        boolean anySub = entries.stream().anyMatch(e -> e.subLabel != null);
        return labelHeight() + (anySub ? subLabelHeight() : 0);
    }

    //Picks the columns and the map size so the grid fits in maxWidth x maxHeight: every column count is tried and
    //the one with the biggest maps wins. When several reach the full size, the most columns win, so rows fill up
    //as far as the room allows before a new row is started
    public void measure (double maxWidth, double maxHeight) {
        if (scrollable) {
            measureScrolling(maxWidth, maxHeight);
            return;
        }
        double s = gui.s();
        int n = entries.size();
        cols = 1;
        rows = 1;
        double best = -1;
        int most = maxColumns > 0 ? Math.min(Math.max(1, n), maxColumns) : Math.max(1, n);
        for (int c = 1; c <= most; c++) {
            int r = Math.max(1, (int) Math.ceil(n/(double) c));
            double size = Math.floor(sizeFor(c, r, maxWidth, maxHeight));
            if (size >= best) {
                best = size;
                cols = c;
                rows = r;
            }
        }
        double gap = gap(), text = textHeight();
        viewSize = Math.max(24*s, best);

        width = cols*viewSize + (cols-1)*gap;
        height = rows*(viewSize + text + gap);
    }

    //A scrollable grid: as many columns as allowed, the maps as big as one row of them can be, and only as tall as the
    //room (the rest is scrolled to). A bar at the right takes a little of the width when there is more than fits
    private void measureScrolling (double maxWidth, double maxHeight) {
        double s = gui.s();
        int n = entries.size();
        cols = Math.max(1, maxColumns > 0 ? Math.min(n, maxColumns) : n);
        double size = Math.floor(sizeFor(cols, 1, maxWidth - barSpace(), maxHeight));
        viewSize = Math.max(24*s, size);
        rows = Math.max(1, (int) Math.ceil(n/(double) cols));
        double gap = gap(), text = textHeight();
        contentHeight = rows*(viewSize + text + gap);
        width = cols*viewSize + (cols-1)*gap + (contentHeight > maxHeight ? barSpace() : 0);
        height = Math.min(contentHeight, Math.max(viewSize + text + gap, maxHeight));
        scrolled = Math.max(0, Math.min(scrolled, contentHeight - height));
    }

    private double barSpace () {return 12*gui.s();}
    private boolean scrolls () {return scrollable && contentHeight > height;}

    //Scrolls by a third of a row per notch of the wheel (delta as the mouse gives it, up is positive). True when the
    //mouse is over the grid
    public boolean scroll (double mx, double my, double delta) {
        if (!scrollable || !inView(mx, my)) return false;
        double row = viewSize + textHeight() + gap();
        scrolled -= Math.signum(delta)*row/3;
        scrolled = Math.max(0, Math.min(scrolled, contentHeight - height));
        place(gridX, gridY);
        return true;
    }

    //Whether (mx, my) is inside the part of the grid that shows
    private boolean inView (double mx, double my) {
        return gridX <= mx && mx < gridX + width && gridY <= my && my < gridY + height;
    }

    //How big the maps can be with this many columns and rows, at most 140 wide
    private double sizeFor (int cols, int rows, double maxWidth, double maxHeight) {
        double gap = gap();
        double size = 140*gui.s();
        size = Math.min(size, (maxWidth - (cols-1)*gap)/cols);
        size = Math.min(size, maxHeight/rows - textHeight() - gap);
        return size;
    }

    public double getWidth () {return this.width;}
    public double getHeight () {return this.height;}

    public void place (double gridX, double gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
        double gap = gap(), text = textHeight();
        double top = gridY - (scrollable ? Math.round(scrolled) : 0);
        for (int i = 0; i < entries.size(); i++) {
            int col = i % cols, row = i / cols;
            entry e = entries.get(i);
            double bx = Math.round(gridX + col*(viewSize + gap)), by = Math.round(top + row*(viewSize + text + gap));
            e.button.setBounds(bx, by, viewSize, viewSize);
            if (e.delete != null) {
                double size = Math.round(Math.max(30*gui.s(), viewSize*0.3));
                e.delete.setBounds(bx + viewSize - size, by, size, size);
            }
        }
    }

    public void draw (GraphicsContext gc) {
        double s = gui.s();
        gc.save();
        //Rows scrolled out of the grid are cut off at its edges
        if (scrollable) gui.clipTo(gc, gridX, gridY, width, height);
        for (entry e : entries) {
            if (scrollable && (e.button.y + viewSize + textHeight() < gridY || e.button.y > gridY + height)) continue;
            e.button.draw(gc);
            if (e.delete != null) e.delete.draw(gc);

            //The labels under each mini map, made smaller until they fit
            double size = 10*s;
            while (size > 6 && guiText.width(e.label, size) > viewSize) size -= 0.5;
            guiText.draw(gc, e.label, e.button.x + viewSize/2, e.button.y + viewSize + 3*s, size, Color.BLACK, TextAlignment.CENTER);
            if (e.subLabel != null) {
                double subSize = 8*s;
                while (subSize > 5 && guiText.width(e.subLabel, subSize) > viewSize) subSize -= 0.5;
                guiText.draw(gc, e.subLabel, e.button.x + viewSize/2, e.button.y + viewSize + 3*s + labelHeight(), subSize, Color.rgb(60, 60, 60), TextAlignment.CENTER);
            }
        }
        gc.restore();
        if (scrolls()) drawBar(gc);
    }

    //The scroll bar at the right edge: the track, and the thumb as big a share of it as the share of the rows showing
    private void drawBar (GraphicsContext gc) {
        double s = gui.s();
        double barX = gridX + width - 5*s, barW = 4*s;
        double thumbH = Math.max(12*s, height*height/contentHeight);
        double thumbY = gridY + (height - thumbH)*scrolled/(contentHeight - height);
        gc.setFill(Color.rgb(0, 0, 0, 0.15));
        gc.fillRect(barX, gridY, barW, height);
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(barX, thumbY, barW, thumbH);
    }

    //True when a map or an X was clicked
    public boolean click (double mx, double my, boolean rightClick) {
        //Maps scrolled out of sight can not be clicked
        if (scrollable && !inView(mx, my)) return false;
        for (entry e : entries) {
            if (e.delete != null && e.delete.contains(mx, my)) return e.delete.click(mx, my, rightClick);
        }
        for (entry e : entries) {
            if (e.button.contains(mx, my)) return e.button.click(mx, my, rightClick);
        }
        return false;
    }

    public void hover (double mx, double my) {
        if (scrollable && !inView(mx, my)) {
            mx = -1;
            my = -1;
        }
        for (entry e : entries) {
            boolean onDelete = e.delete != null && e.delete.contains(mx, my);
            if (e.delete != null) e.delete.hover(mx, my);
            //The map under the X does not light up as well
            e.button.hover(onDelete ? -1 : mx, onDelete ? -1 : my);
        }
    }
}
