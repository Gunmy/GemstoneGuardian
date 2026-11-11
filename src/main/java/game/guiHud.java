package game;

import javafx.scene.canvas.GraphicsContext;

//The active row (4 rocks, 4 items) shown at the bottom of the screen while walking around
public class guiHud extends guiElement {

    private slotGrid rockGrid = new slotGrid(inventory.slotKind.ROCK, inventory.ACTIVE, 1);
    private slotGrid itemGrid = new slotGrid(inventory.slotKind.ITEM, inventory.ACTIVE, 1);
    private double slotSize = 40, gap = 4;

    public guiHud (gui gui) {
        super(gui);
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        double s = gui.s();
        double slotSize = this.slotSize*s, gap = this.gap*s;
        width = 2*rockGrid.getWidth() + gap;
        height = slotSize;
        x = Math.round((screenWidth - width)/2);
        y = Math.round(screenHeight - height - 12*s);

        rockGrid.setBounds(x, y, slotSize, gap);
        itemGrid.setBounds(x + rockGrid.getWidth() + gap, y, slotSize, gap);
    }

    @Override
    public void draw (GraphicsContext gc) {
        character boy = gui.getHandler().getMainCharacter();
        rockGrid.draw(gc, gui, boy.getInventory(), boy.getGemstone(), true);
        itemGrid.draw(gc, gui, boy.getInventory(), boy.getGemstone(), true);
    }

    //Clicking a rock makes it the equipped one, the rock battles start with. The hotbar swallows clicks on itself
    //so they do not reach the world behind it
    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        character boy = gui.getHandler().getMainCharacter();
        inventory inv = boy.getInventory();
        int index = rockGrid.indexAt(mx, my, inv);
        if (index >= 0) {
            itemStack stack = inv.get(rockGrid.getKind(), index);
            if (stack != null && stack.getItem() instanceof gemstone) boy.equipGemstone((gemstone) stack.getItem());
        }
        return contains(mx, my);
    }

    @Override
    public void hover (double mx, double my) {
        inventory inv = gui.getHandler().getMainCharacter().getInventory();
        for (slotGrid grid : new slotGrid[] {rockGrid, itemGrid}) {
            int index = grid.indexAt(mx, my, inv);
            if (index >= 0 && inv.get(grid.getKind(), index) != null) {
                gui.setTooltip(inv.get(grid.getKind(), index).getItem().getName());
            }
        }
    }
}
