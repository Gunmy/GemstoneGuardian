package game;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//A block of slots drawn as a grid: one kind of the inventory's slots, or any other slotGroup (a chest...).
//Used by the inventory, the active bar, the battle and the chest panel
public class slotGrid {

    private inventory.slotKind kind;
    private int columns, rows;
    private int firstIndex = 0;
    private double x, y, size, gap;

    //For a slotGroup that is not part of the inventory the kind is not used
    public slotGrid (int columns, int rows) {
        this(inventory.slotKind.TOP, columns, rows);
    }

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
    //The left edge and top of the slot showing that index, and the slots' size
    public double slotX (int index) {return cellX((index - firstIndex) % columns);}
    public double slotY (int index) {return cellY((index - firstIndex) / columns);}
    public double getSlotSize () {return this.size;}
    private double cellY (int row) {return y + row*(size+gap);}

    //The inventory index of the slot under the point, or -1
    public int indexAt (double mx, double my, inventory inv) {
        return indexAt(mx, my, inv.group(kind));
    }

    public int indexAt (double mx, double my, slotGroup group) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int index = firstIndex + r*columns + c;
                if (index >= group.size()) return -1;
                if (cellX(c) <= mx && mx < cellX(c)+size && cellY(r) <= my && my < cellY(r)+size) return index;
            }
        }
        return -1;
    }

    //The item and its count as they sit in a slot of the given size with its top left at (x, y).
    //Also used for the stack on the cursor, so the count stays in the same place relative to the item
    public static void drawStack (GraphicsContext gc, itemStack stack, double x, double y, double size) {
        item item = stack.getItem();
        item.drawGlowOutline(gc, x + size*0.1, y + size*0.1, size*0.8);
        item.drawIcon(gc, x + size*0.1, y + size*0.1, size*0.8);
        item.drawChange(gc, x + size*0.1, y + size*0.1, size*0.8);
        item.drawGlowOver(gc, x + size*0.1, y + size*0.1, size*0.8);
        if (stack.getItem().getMaxStack() > 1 && stack.getCount() > 1 && size >= 24) {
            guiText.draw(gc, String.valueOf(stack.getCount()), x + size*0.9, y + size*0.62, size*0.3, Color.BLACK, TextAlignment.RIGHT);
        }
    }

    //selected is drawn with the selected slot frame. Particles are only given off when wanted (not for tiny previews)
    public void draw (GraphicsContext gc, gui gui, inventory inv, item selected, boolean particles) {
        draw(gc, gui, inv.group(kind), selected, particles);
    }

    //The glow around charged items is not drawn here but by gui after everything (itemInteractions.drawGlows),
    //so slots drawn later can never cover it and cut it into squares
    public void draw (GraphicsContext gc, gui gui, slotGroup group, item selected, boolean particles) {
        Random rdm = gui.getRandom();
        int count = Math.max(0, Math.min(rows*columns, group.size() - firstIndex));
        for (int i = 0; i < count; i++) {
            int c = i % columns, r = i / columns;
            itemStack stack = group.get(firstIndex + i);
            double cx = cellX(c), cy = cellY(r);
            //An item that has turned into another (sand into glass) is swapped for it here, with a burst
            if (group.takesPart() && stack != null && stack.getItem().latest() != stack.getItem()) {
                item became = stack.getItem().latest();
                if (group.accepts(became)) {
                    group.set(firstIndex + i, new itemStack(became, Math.min(stack.getCount(), became.getMaxStack())));
                    gui.getInteractions().changeBurst(became, cx, cy, size);
                    if (group.get(firstIndex + i) != null) stack = group.get(firstIndex + i);
                }
            }
            gui.getSkin().drawSlot(gc, cx, cy, size, stack != null && stack.getItem() == selected);
            if (stack == null) {
                gui.getSkin().drawSlotHint(gc, group.hint(), cx, cy, size);
                continue;
            }
            drawStack(gc, stack, cx, cy, size);
            if (particles) stack.getItem().spawnParticles(gui, cx, cy, size, rdm, 0, 0);
            //Small previews (the rocks in a fight) take part in the items' interactions too, so copper keeps
            //charging a boltstone in a fight; they only give off no particles
            if (group.takesPart()) gui.getInteractions().place(stack.getItem(), cx, cy, size, false);
        }
    }
}
