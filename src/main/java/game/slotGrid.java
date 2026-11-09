package game;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//A block of inventory slots of one kind, drawn as a grid. Used by the inventory, the active bar and the battle
public class slotGrid {

    private inventory.slotKind kind;
    private int columns, rows;
    private int firstIndex = 0;
    private double x, y, size, gap;

    public slotGrid (inventory.slotKind kind, int columns, int rows) {
        this.kind = kind;
        this.columns = columns;
        this.rows = rows;
    }

    public inventory.slotKind getKind () {return this.kind;}
    public void setRows (int rows) {this.rows = rows;}
    //The inventory index shown in the top left slot, for scrolling
    public void setFirstIndex (int firstIndex) {this.firstIndex = firstIndex;}

    public void setBounds (double x, double y, double size, double gap) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.gap = gap;
    }

    public double getWidth () {return columns*size + (columns-1)*gap;}
    public double getHeight () {return rows*size + (rows-1)*gap;}

    private double cellX (int column) {return x + column*(size+gap);}
    private double cellY (int row) {return y + row*(size+gap);}

    //The inventory index of the slot under the point, or -1
    public int indexAt (double mx, double my, inventory inv) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int index = firstIndex + r*columns + c;
                if (index >= inv.size(kind)) return -1;
                if (cellX(c) <= mx && mx < cellX(c)+size && cellY(r) <= my && my < cellY(r)+size) return index;
            }
        }
        return -1;
    }

    //selected is drawn with the selected slot frame. Particles are only given off when wanted (not for tiny previews)
    public void draw (GraphicsContext gc, gui gui, inventory inv, item selected, boolean particles) {
        Random rdm = gui.getRandom();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int index = firstIndex + r*columns + c;
                if (index >= inv.size(kind)) return;

                itemStack stack = inv.get(kind, index);
                double cx = cellX(c), cy = cellY(r);
                gui.getSkin().drawSlot(gc, cx, cy, size, stack != null && stack.getItem() == selected);

                if (stack == null) {
                    gui.getSkin().drawSlotHint(gc, kind, cx, cy, size);
                } else {
                    stack.getItem().drawIcon(gc, cx + size*0.1, cy + size*0.1, size*0.8);
                    if (stack.getItem().getMaxStack() > 1 && stack.getCount() > 1 && size >= 24) {
                        guiText.draw(gc, String.valueOf(stack.getCount()), cx + size*0.9, cy + size*0.62, size*0.3, Color.BLACK, TextAlignment.RIGHT);
                    }
                    if (particles) stack.getItem().getParticles().spawn(gui, cx, cy, size, rdm);
                }
            }
        }
    }
}
