package game;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The full inventory screen: the top rows (scrolling when there are more than fit) over the active row,
//which is the 4 rock slots and the 4 item slots. Items are moved by clicking one up onto the cursor
//and clicking it down somewhere else. Right clicking a rock equips it
public class guiInventory extends guiElement {

    private static final int VISIBLE_ROWS = 4;

    private slotGrid topGrid = new slotGrid(inventory.slotKind.TOP, inventory.COLUMNS, 1);
    private slotGrid rockGrid = new slotGrid(inventory.slotKind.ROCK, inventory.ACTIVE, 1);
    private slotGrid itemGrid = new slotGrid(inventory.slotKind.ITEM, inventory.ACTIVE, 1);
    private slotGrid[] grids = {topGrid, rockGrid, itemGrid};

    private int scroll = 0;
    private double slotSize = 48, gap = 4, divider = 20;
    private double activeY;

    //The panel to the right that tells about the hovered item. It is always there, so the slots sit left of the middle
    private double infoWidth = 260, infoGap = 8;
    private double infoX, infoY, infoHeight;
    private item hovered;

    public guiInventory (gui gui) {
        super(gui);
        visible = false;
    }

    private inventory getInventory () {
        return gui.getHandler().getMainCharacter().getInventory();
    }

    private int visibleRows () {
        return Math.min(getInventory().getRows(), VISIBLE_ROWS);
    }

    @Override
    public void setVisible (boolean visible) {
        super.setVisible(visible);
        //Whatever is on the cursor goes back in when the screen closes
        if (!visible && gui.getCursor() != null) {
            itemStack held = gui.getCursor();
            gui.setCursor(null);
            for (int i = 0; i < held.getCount(); i++) getInventory().give(held.getItem());
        }
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        inventory inv = getInventory();
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, inv.getRows() - rows));

        double s = gui.s();
        double slotSize = this.slotSize*s, gap = this.gap*s, divider = this.divider*s;
        double pad = gui.getSkin().getBorder() + 4*s;
        double split = gap*3;
        width = 2*pad + inventory.COLUMNS*slotSize + (inventory.COLUMNS-1)*gap + split;
        height = 2*pad + (rows+1)*slotSize + (rows-1)*gap + divider;
        double total = width + infoGap*s + infoWidth*s;
        x = Math.round((screenWidth - total)/2);
        y = Math.round((screenHeight - height)/2 - 20*s);

        infoX = x + width + infoGap*s;
        infoY = y;
        infoHeight = height;

        topGrid.setRows(rows);
        topGrid.setFirstIndex(scroll*inventory.COLUMNS);
        topGrid.setBounds(x + pad + split/2, y + pad, slotSize, gap);

        activeY = y + pad + rows*slotSize + (rows-1)*gap + divider;
        rockGrid.setBounds(x + pad, activeY, slotSize, gap);
        itemGrid.setBounds(x + pad + rockGrid.getWidth() + gap + split, activeY, slotSize, gap);
    }

    @Override
    public void draw (GraphicsContext gc) {
        inventory inv = getInventory();
        item equipped = gui.getHandler().getMainCharacter().getGemstone();

        gui.getSkin().drawPanel(gc, x, y, width, height);
        for (slotGrid grid : grids) grid.draw(gc, gui, inv, equipped, true);
        drawInfo(gc);

        //A line between the top part and the active row
        double s = gui.s();
        double lineY = Math.round(activeY - divider*s/2 - 1);
        gc.setFill(Color.rgb(0, 0, 0, 0.25));
        gc.fillRect(x + gui.getSkin().getBorder(), lineY, width - 2*gui.getSkin().getBorder(), Math.max(1, Math.round(2*s)));

        //Scrollbar when there is more than fits
        if (inv.getRows() > VISIBLE_ROWS) {
            double trackX = x + width - gui.getSkin().getBorder() - 2*s;
            double trackY = y + gui.getSkin().getBorder() + 4*s;
            double trackH = topGrid.getHeight();
            double thumbH = trackH*VISIBLE_ROWS/inv.getRows();
            double thumbY = trackY + (trackH-thumbH)*scroll/(inv.getRows()-VISIBLE_ROWS);
            gc.setFill(Color.rgb(0, 0, 0, 0.15));
            gc.fillRect(trackX, trackY, 4*s, trackH);
            gc.setFill(Color.rgb(0, 0, 0, 0.5));
            gc.fillRect(trackX, thumbY, 4*s, thumbH);
        }
    }

    //Name, lore and what the item does, wrapped to the panel. Gemstones list their abilities.
    //The panel is at least as tall as the inventory and grows when the contents need more
    private void drawInfo (GraphicsContext gc) {
        double s = gui.s();
        double w = infoWidth*s;
        double pad = gui.getSkin().getBorder() + 4*s;

        double needed = drawInfoContents(gc, false) + 2*pad;
        double h = Math.max(infoHeight, needed);
        //Keep the whole panel on screen if it grew past the bottom
        infoY = Math.min(y, Math.max(10*s, gui.getHandler().getHEIGHT() - 10*s - h));

        gui.getSkin().drawPanel(gc, infoX, infoY, w, h);
        drawInfoContents(gc, true);
    }

    //Lays the contents out from the top of the panel and returns their height. Only draws when asked,
    //so the same code measures first and draws after
    private double drawInfoContents (GraphicsContext gc, boolean draw) {
        double s = gui.s();
        double w = infoWidth*s;
        double pad = gui.getSkin().getBorder() + 4*s;
        double textWidth = w - 2*pad;
        double top = infoY + pad;
        double textY = top;

        if (hovered == null) {
            if (draw) guiText.draw(gc, "Hover over an item", infoX + w/2, textY, 11*s, Color.GRAY, TextAlignment.CENTER);
            return guiText.height(11*s);
        }

        double nameSize = 14*s, textSize = 11*s;
        if (draw) {
            hovered.drawIcon(gc, infoX + pad, textY, nameSize*1.6);
            guiText.draw(gc, hovered.getName(), infoX + pad + nameSize*1.6 + 6*s, textY + (nameSize*1.6 - guiText.height(nameSize))/2, nameSize, Color.BLACK);
        }
        textY += nameSize*1.6 + 8*s;

        double lineHeight = guiText.height(textSize);
        for (String line : guiText.wrap(hovered.getLore(), textSize, textWidth)) {
            if (draw) guiText.draw(gc, line, infoX + pad, textY, textSize, Color.GRAY);
            textY += lineHeight;
        }
        textY += lineHeight*0.6;

        //What the letters in the formulas mean, with the player's current numbers
        character boy = gui.getHandler().getMainCharacter();
        String legend = "lvl = your level (" + boy.getLevel() + "), maxHP = your max health (" + Math.round(boy.getMaxHealth()) + ")";
        for (String line : guiText.wrap(legend, 9*s, textWidth)) {
            if (draw) guiText.draw(gc, line, infoX + pad, textY, 9*s, Color.GRAY);
            textY += guiText.height(9*s);
        }
        textY += lineHeight*0.4;

        //One box per ability or effect: picture to the left, title and subtitle on top, text under
        double boxPad = 8*s, iconSize = 24*s, boxGap = 6*s;
        for (infoBox box : hovered.getInfoBoxes()) {
            double textX = infoX + pad + boxPad + (box.getIcon() != null ? iconSize + boxPad : 0);
            double innerWidth = infoX + pad + textWidth - boxPad - textX;
            List<String> lines = guiText.wrap(box.getText(), textSize, innerWidth);
            double headerHeight = guiText.height(12*s) + (box.getSubtitle().isEmpty() ? 0 : guiText.height(9*s));
            double boxHeight = 2*boxPad + Math.max(headerHeight + lines.size()*lineHeight + 2*s, iconSize);

            if (draw) {
                gui.getSkin().drawButton(gc, infoX + pad, textY, textWidth, boxHeight);
                if (box.getIcon() != null) box.getIcon().draw(gc, gui, infoX + pad + boxPad, textY + boxPad, iconSize);

                double lineY = textY + boxPad;
                guiText.draw(gc, box.getTitle(), textX, lineY, 12*s, Color.BLACK);
                lineY += guiText.height(12*s);
                if (!box.getSubtitle().isEmpty()) {
                    guiText.draw(gc, box.getSubtitle(), textX, lineY, 9*s, Color.GRAY);
                    lineY += guiText.height(9*s);
                }
                lineY += 2*s;
                for (String line : lines) {
                    guiText.draw(gc, line, textX, lineY, textSize, Color.BLACK);
                    lineY += lineHeight;
                }
            }
            textY += boxHeight + boxGap;
        }
        return textY - boxGap - top;
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        inventory inv = getInventory();
        for (slotGrid grid : grids) {
            int index = grid.indexAt(mx, my, inv);
            if (index >= 0) {
                slotClicked(grid.getKind(), index, rightClick);
                return true;
            }
        }
        //The screen is modal, nothing behind it gets clicked
        return true;
    }

    private void slotClicked (inventory.slotKind kind, int index, boolean rightClick) {
        inventory inv = getInventory();
        itemStack cursor = gui.getCursor();
        itemStack inSlot = inv.get(kind, index);

        if (rightClick) {
            if (cursor == null && kind == inventory.slotKind.ROCK && inSlot != null && inSlot.getItem() instanceof gemstone) {
                character boy = gui.getHandler().getMainCharacter();
                if (boy.getGemstone() == inSlot.getItem()) boy.deEquip();
                else boy.equipGemstone((gemstone) inSlot.getItem());
            }
            return;
        }

        if (cursor == null) {
            if (inSlot != null) {
                inv.set(kind, index, null);
                gui.setCursor(inSlot);
            }
        } else if (!inv.accepts(kind, cursor.getItem())) {
            return;
        } else if (inSlot == null) {
            inv.set(kind, index, cursor);
            gui.setCursor(null);
        } else if (inSlot.stacksWith(cursor)) {
            inSlot.takeFrom(cursor);
            if (cursor.isEmpty()) gui.setCursor(null);
        } else {
            inv.set(kind, index, cursor);
            gui.setCursor(inSlot);
        }
    }

    @Override
    public boolean scroll (double mx, double my, double delta) {
        if (delta < 0) scroll++;
        if (delta > 0) scroll--;
        return true;
    }

    //The info panel shows the hovered item, so no tooltip is needed here
    @Override
    public void hover (double mx, double my) {
        inventory inv = getInventory();
        hovered = null;
        for (slotGrid grid : grids) {
            int index = grid.indexAt(mx, my, inv);
            if (index >= 0 && inv.get(grid.getKind(), index) != null) {
                hovered = inv.get(grid.getKind(), index).getItem();
            }
        }
    }
}
