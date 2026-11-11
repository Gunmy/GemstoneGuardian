package game;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The full inventory screen: the top rows (scrolling when there are more than fit) over the active row,
//which is the 4 rock slots and the 4 item slots. Items are moved by clicking one up onto the cursor
//and clicking it down somewhere else. Right clicking a rock equips it.
//Opened with a chest (any slotGroup, see openWith) the chest is shown in a panel above, and items move between the two
public class guiInventory extends guiElement {

    private static final int VISIBLE_ROWS = 4;

    private slotGrid topGrid = new slotGrid(inventory.slotKind.TOP, inventory.COLUMNS, 1);
    private slotGrid rockGrid = new slotGrid(inventory.slotKind.ROCK, inventory.ACTIVE, 1);
    private slotGrid itemGrid = new slotGrid(inventory.slotKind.ITEM, inventory.ACTIVE, 1);
    //The trinket slots, a column in a small panel of their own left of the inventory, its top in line with the inventory's
    private slotGrid trinketGrid = new slotGrid(inventory.slotKind.TRINKET, 1, inventory.TRINKETS);
    private slotGrid[] grids = {topGrid, rockGrid, itemGrid, trinketGrid};
    private double trinketX, trinketWidth, trinketHeight;

    //An elemental rock's three slots, in a small panel under the inventory while it is open (right click the rock to
    //open and close it). The slots look like the hotbar's rock slots
    private elementalRock elemental;
    private slotGrid elementalGrid = new slotGrid(elementalRock.SLOTS, 1);
    private double elementalX, elementalY, elementalWidth, elementalHeight;

    public void toggleElemental (elementalRock rock) {
        elemental = elemental == rock ? null : rock;
    }

    private int scroll = 0;
    private double slotSize = 48, gap = 4, divider = 20;
    private double activeY;

    //The panel to the right that tells about the hovered item. It is always there, so the slots sit left of the middle
    private double infoWidth = 260, infoGap = 8;
    private double infoX, infoY, infoHeight;
    private item hovered;
    //The inventory slot under the mouse (its grid and index), for highlighting items that have a right click action
    private slotGrid hoveredGrid;
    private int hoveredIndex = -1;

    //The chest shown above the inventory, or null. Its rows scroll when there are more than fit
    private static final int CHEST_VISIBLE_ROWS = 3;
    private slotGroup chest;
    private String chestTitle;
    private slotGrid chestGrid = new slotGrid(inventory.COLUMNS, 1);
    private int chestScroll = 0;
    private double chestY, chestHeight, chestTitleHeight;

    public guiInventory (gui gui) {
        super(gui);
        visible = false;
    }

    //Opens the inventory with the chest above it. Closing the inventory closes the chest too
    public void openWith (slotGroup chest, String title) {
        this.chest = chest;
        this.chestTitle = title;
        this.chestScroll = 0;
        setVisible(true);
    }

    private int chestRows () {
        return chest == null ? 0 : (int) Math.ceil(chest.size()/(double) inventory.COLUMNS);
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
        slotGroup closing = visible ? null : chest;
        if (!visible) chest = null;
        if (!visible) elemental = null;
        //Whatever is on the cursor goes back in when the screen closes
        if (!visible && gui.getCursor() != null) {
            itemStack held = gui.getCursor();
            gui.setCursor(null);
            for (int i = 0; i < held.getCount(); i++) getInventory().give(held.getItem());
        }
        if (closing != null) closing.closed();
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        inventory inv = getInventory();
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, inv.getRows() - rows));

        double s = gui.s();
        double slotSize = this.slotSize*s, gap = this.gap*s, divider = this.divider*s;
        double pad = gui.getSkin().getBorder() + 4*s;
        width = 2*pad + inventory.COLUMNS*slotSize + (inventory.COLUMNS-1)*gap;
        height = 2*pad + (rows+1)*slotSize + (rows-1)*gap + divider;
        trinketWidth = 2*pad + slotSize;
        trinketHeight = 2*pad + inventory.TRINKETS*slotSize + (inventory.TRINKETS-1)*gap;
        double total = trinketWidth + infoGap*s + width + infoGap*s + infoWidth*s;
        trinketX = Math.round((screenWidth - total)/2);
        x = Math.round(trinketX + trinketWidth + infoGap*s);

        //The chest panel is as wide as the inventory and sits above it, the two together are centered
        if (chest != null) {
            int chestVisible = Math.max(1, Math.min(chestRows(), CHEST_VISIBLE_ROWS));
            chestScroll = Math.max(0, Math.min(chestScroll, chestRows() - chestVisible));
            chestTitleHeight = guiText.height(12*s) + 6*s;
            chestHeight = 2*pad + chestTitleHeight + chestVisible*slotSize + (chestVisible-1)*gap;
            chestY = Math.round((screenHeight - chestHeight - infoGap*s - height)/2 - 20*s);
            y = Math.round(chestY + chestHeight + infoGap*s);

            chestGrid.setRows(chestVisible);
            chestGrid.setFirstIndex(chestScroll*inventory.COLUMNS);
            chestGrid.setBounds(x + pad, chestY + pad + chestTitleHeight, slotSize, gap);
        } else {
            y = Math.round((screenHeight - height)/2 - 20*s);
        }

        elementalWidth = 2*pad + elementalRock.SLOTS*slotSize + (elementalRock.SLOTS-1)*gap;
        elementalHeight = 2*pad + slotSize;
        elementalX = Math.round(x + (width - elementalWidth)/2);
        elementalY = Math.round(y + height + infoGap*s);
        elementalGrid.setBounds(elementalX + pad, elementalY + pad, slotSize, gap);

        infoX = x + width + infoGap*s;
        infoY = y;
        infoHeight = height;

        topGrid.setRows(rows);
        topGrid.setFirstIndex(scroll*inventory.COLUMNS);
        topGrid.setBounds(x + pad, y + pad, slotSize, gap);

        activeY = y + pad + rows*slotSize + (rows-1)*gap + divider;
        rockGrid.setBounds(x + pad, activeY, slotSize, gap);
        trinketGrid.setBounds(trinketX + pad, y + pad, slotSize, gap);
        itemGrid.setBounds(x + pad + rockGrid.getWidth() + gap, activeY, slotSize, gap);
    }

    @Override
    public void draw (GraphicsContext gc) {
        inventory inv = getInventory();
        item equipped = gui.getHandler().getMainCharacter().getGemstone();

        double s = gui.s();
        if (chest != null) {
            gui.getSkin().drawPanel(gc, x, chestY, width, chestHeight);
            guiText.draw(gc, chestTitle, x + width/2, chestY + gui.getSkin().getBorder() + 2*s, 12*s, Color.BLACK, TextAlignment.CENTER);
            chestGrid.draw(gc, gui, chest, null, true);
            drawScrollbar(gc, chestY + gui.getSkin().getBorder() + 4*s + chestTitleHeight, chestGrid.getHeight(), CHEST_VISIBLE_ROWS, chestRows(), chestScroll);
        }

        gui.getSkin().drawPanel(gc, trinketX, y, trinketWidth, trinketHeight);
        if (elemental != null) {
            gui.getSkin().drawPanel(gc, elementalX, elementalY, elementalWidth, elementalHeight);
            //The rocks give off their particles here, but their lightning is the elemental rock's: itemInteractions draws
            //what happens between them (and their short strikes) here, from where these slots are
            elementalGrid.draw(gc, gui, elemental.slots(), null, false);
            for (int i = 0; i < elementalRock.SLOTS; i++) {
                double sx = elementalGrid.slotX(i), sy = elementalGrid.slotY(i), size = elementalGrid.getSlotSize();
                gui.getInteractions().showInside(elemental, i, sx, sy, size);
                if (elemental.getInside(i) != null) elemental.getInside(i).spawnParticles(gui, sx, sy, size, gui.getRandom(), 0, 0, false);
                //With two rocks in, the empty slot is covered over: nothing more fits
                if (elemental.getInside(i) == null && elemental.getInsideList().size() >= elementalRock.MAX_ROCKS) drawClosedSlot(gc, sx, sy, size);
            }
        }
        gui.getSkin().drawPanel(gc, x, y, width, height);
        for (slotGrid grid : grids) grid.draw(gc, gui, inv, equipped, true);
        //An item that does something on a right click lights up while hovered, like a button
        if (hoveredGrid != null && gui.getCursor() == null && hovered != null && hovered.hasInventoryAction()) {
            double size = hoveredGrid.getSlotSize();
            gc.setFill(Color.rgb(255, 255, 255, 0.3));
            gc.fillRect(hoveredGrid.slotX(hoveredIndex), hoveredGrid.slotY(hoveredIndex), size, size);
        }
        drawInfo(gc);

        //A line between the top part and the active row
        double lineY = Math.round(activeY - divider*s/2 - 1);
        gc.setFill(Color.rgb(0, 0, 0, 0.25));
        gc.fillRect(x + gui.getSkin().getBorder(), lineY, width - 2*gui.getSkin().getBorder(), Math.max(1, Math.round(2*s)));

        drawScrollbar(gc, y + gui.getSkin().getBorder() + 4*s, topGrid.getHeight(), VISIBLE_ROWS, inv.getRows(), scroll);
    }

    //A thin bar at the right edge of the panels, only when there are more rows than fit
    private void drawScrollbar (GraphicsContext gc, double trackY, double trackH, int visibleRows, int rows, int scrolled) {
        if (rows <= visibleRows) return;
        double s = gui.s();
        double trackX = x + width - gui.getSkin().getBorder() - 2*s;
        double thumbH = trackH*visibleRows/rows;
        double thumbY = trackY + (trackH-thumbH)*scrolled/(rows-visibleRows);
        gc.setFill(Color.rgb(0, 0, 0, 0.15));
        gc.fillRect(trackX, trackY, 4*s, trackH);
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(trackX, thumbY, 4*s, thumbH);
    }

    private boolean overChest (double mx, double my) {
        return chest != null && x <= mx && mx < x + width && chestY <= my && my < chestY + chestHeight;
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

    //A slot that takes nothing is covered with the panel's own middle, so it looks like it is not there
    private void drawClosedSlot (GraphicsContext gc, double x, double y, double size) {
        gui.getSkin().drawCell(gc, guiSkin.PANEL_X + 3*guiSkin.CELL, guiSkin.GUI_Y, x, y, size, size);
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
        //The item being held while it is on the cursor, otherwise the one under the mouse
        item shown = gui.getCursor() != null ? gui.getCursor().getItem() : hovered;

        //Nothing hovered or held: the player's own stats
        if (shown == null) {
            character boy = gui.getHandler().getMainCharacter();
            double nameSize = 14*s;
            if (draw) {
                boy.draw(gc, gui, infoX + pad, textY, nameSize*1.6);
                String name = boy.getName() + " (Lvl " + boy.getLevel() + ")";
                guiText.draw(gc, name, infoX + pad + nameSize*1.6 + 6*s, textY + (nameSize*1.6 - guiText.height(nameSize))/2, nameSize, Color.BLACK);
            }
            textY += nameSize*1.6 + 8*s;
            double boxGap = 6*s;
            for (infoBox box : boy.getStatBoxes()) {
                textY += box.draw(gc, gui, infoX + pad, textY, textWidth, draw) + boxGap;
            }
            return textY - boxGap - top;
        }

        return drawItemDetails(gc, gui, shown, infoX + pad, top, textWidth, draw);
    }

    //An item's details as the info panel shows them: picture and name, lore, what lvl and maxHP are right now, then one
    //box per ability or effect. Laid out from (x, y), width wide, and returns the height. Only draws when asked, so the
    //same code measures first and draws after. Also used for the rocks and items in the battle's More view
    public static double drawItemDetails (GraphicsContext gc, gui gui, item shown, double x, double y, double width, boolean draw) {
        double s = gui.s();
        double textY = y;
        double nameSize = 14*s, textSize = 11*s;
        if (draw) {
            shown.drawIcon(gc, x, textY, nameSize*1.6);
            guiText.draw(gc, shown.getName(), x + nameSize*1.6 + 6*s, textY + (nameSize*1.6 - guiText.height(nameSize))/2, nameSize, Color.BLACK);
        }
        textY += nameSize*1.6 + 8*s;

        double lineHeight = guiText.height(textSize);
        for (String line : guiText.wrap(shown.getLore(), textSize, width)) {
            if (draw) guiText.draw(gc, line, x, textY, textSize, Color.GRAY);
            textY += lineHeight;
        }
        textY += lineHeight*0.6;

        //What the letters in the formulas mean, with the player's current numbers
        character boy = gui.getHandler().getMainCharacter();
        String legend = "lvl = your level (" + boy.getLevel() + "), maxHP = your max health (" + Math.round(boy.getMaxHealth()) + ")";
        for (String line : guiText.wrap(legend, 9*s, width)) {
            if (draw) guiText.draw(gc, line, x, textY, 9*s, Color.GRAY);
            textY += guiText.height(9*s);
        }
        textY += lineHeight*0.4;

        //One box per ability or effect: picture to the left, title and subtitle on top, text under
        double boxGap = 6*s;
        for (infoBox box : shown.getInfoBoxes()) {
            textY += box.draw(gc, gui, x, textY, width, draw) + boxGap;
        }
        return textY - boxGap - y;
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        inventory inv = getInventory();
        if (chest != null) {
            int index = chestGrid.indexAt(mx, my, chest);
            if (index >= 0) {
                gui.setCursor(chest.click(index, gui.getCursor(), rightClick));
                return true;
            }
        }
        if (elemental != null) {
            int index = elementalGrid.indexAt(mx, my, elemental.slots());
            if (index >= 0) {
                gui.setCursor(elemental.slots().click(index, gui.getCursor(), rightClick));
                return true;
            }
        }
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

        //Right clicking an item with an inventory action runs it
        if (rightClick && cursor == null && inSlot != null && inSlot.getItem().hasInventoryAction()) {
            inSlot.getItem().inventoryUse(gui.getHandler().getMainCharacter());
            return;
        }

        //Right clicking a rock equips it, everything else is the standard slot click
        if (rightClick && cursor == null && kind == inventory.slotKind.ROCK && inSlot != null && inSlot.getItem() instanceof gemstone) {
            character boy = gui.getHandler().getMainCharacter();
            if (boy.getGemstone() == inSlot.getItem()) boy.deEquip();
            else boy.equipGemstone((gemstone) inSlot.getItem());
            return;
        }
        gui.setCursor(inv.group(kind).click(index, cursor, rightClick));
    }

    @Override
    public boolean scroll (double mx, double my, double delta) {
        int step = delta < 0 ? 1 : delta > 0 ? -1 : 0;
        if (overChest(mx, my)) chestScroll += step;
        else scroll += step;
        return true;
    }

    //The info panel shows the hovered item, so no tooltip is needed here
    @Override
    public void hover (double mx, double my) {
        inventory inv = getInventory();
        hovered = null;
        if (chest != null) {
            int index = chestGrid.indexAt(mx, my, chest);
            if (index >= 0 && chest.get(index) != null) hovered = chest.get(index).getItem();
        }
        hoveredGrid = null;
        for (slotGrid grid : grids) {
            int index = grid.indexAt(mx, my, inv);
            if (index >= 0 && inv.get(grid.getKind(), index) != null) {
                hovered = inv.get(grid.getKind(), index).getItem();
                hoveredGrid = grid;
                hoveredIndex = index;
            }
        }
        if (elemental != null) {
            int index = elementalGrid.indexAt(mx, my, elemental.slots());
            if (index >= 0 && elemental.getInside(index) != null) hovered = elemental.getInside(index);
        }
    }
}
