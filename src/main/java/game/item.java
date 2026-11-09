package game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class item implements picture {

    protected String name;
    protected BiConsumer<character, character> useFunct;
    protected boolean oneUse;
    protected int useCount;
    protected gameHandler handler;

    //How the item looks: a grey-scale cell in tiles.png that is tinted with a colour, and particles it gives off
    protected int imgX = guiSkin.ROCK_X;
    protected int imgY = guiSkin.GUI_Y;
    protected String tint = null;
    protected itemParticles particles = itemParticles.NONE;

    //How many fit in one slot. 1 means the item does not stack
    protected int maxStack = 1;

    //Shown in the inventory when the item is hovered: a bit of story, and what the item does
    protected String lore = "";
    protected String details = "";

    public item (String name, BiConsumer<character, character> useFunct, boolean oneUse, gameHandler handler) {
        this.name = name;
        this.useFunct = useFunct;
        this.oneUse = oneUse;
        this.handler = handler;
    }

    public item look (int imgX, int imgY, String tint) {
        this.imgX = imgX;
        this.imgY = imgY;
        this.tint = tint;
        return this;
    }

    public item particles (itemParticles particles) {
        this.particles = particles;
        return this;
    }

    public item stackable (int maxStack) {
        if (maxStack < 1) throw new IllegalArgumentException("Maxstack kan ikke være under 1");
        this.maxStack = maxStack;
        return this;
    }

    public item lore (String lore) {
        this.lore = lore;
        return this;
    }

    public item details (String details) {
        this.details = details;
        return this;
    }

    public String getLore () {return this.lore;}

    //One box per thing worth knowing. Gemstones make one per ability
    public List<infoBox> getInfoBoxes () {
        List<infoBox> boxes = new ArrayList<infoBox>();
        if (!details.isEmpty()) boxes.add(new infoBox(null, "Effect", "", details));
        return boxes;
    }

    public int getMaxStack () {return this.maxStack;}
    public itemParticles getParticles () {return this.particles;}

    //Two items of the same type stack when the type allows it. Items with their own state (like gemstones) never do
    public boolean stacksWith (item other) {
        return maxStack > 1 && other != null && other.getClass() == getClass() && other.name.equals(name);
    }

    public Image getIcon () {
        return handler.getImages().getTinted(imgX, imgY, tint);
    }

    public void drawIcon (GraphicsContext gc, double x, double y, double size) {
        gc.drawImage(getIcon(), 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, Math.round(x), Math.round(y), Math.round(size), Math.round(size));
    }

    //In a box the item looks like it does in a slot: the icon with its particles
    @Override
    public void draw (GraphicsContext gc, gui gui, double x, double y, double size) {
        drawIcon(gc, x, y, size);
        particles.spawn(gui, x - size*0.125, y - size*0.125, size*1.25, gui.getRandom());
    }

    //I tilfelle jeg utvider programmet og legger til flere funksjoner, som consumables
    public boolean isUsedUp () {
        return oneUse && (useCount > 0);
    }

    //Consumables disappear from the inventory when used
    public boolean isConsumable () {
        return oneUse;
    }

    protected void dialogueAdd(character you) {
        handler.getDiealoguecontainer().add(new dialogue(you, "Used " + getName()));
    }

    public void use (character character, character oppositeCharacter) {
        if (character == oppositeCharacter) throw new IllegalArgumentException("use må ta inn to ulike karakterer");
        handler.getDiealoguecontainer().setSource(this);
        useFunct.accept(character, oppositeCharacter);
        handler.getDiealoguecontainer().clearSource();
        useCount += 1;
        dialogueAdd(character);
    }

    public String getName () {
        return this.name;
    }

    public void restore() {}
}
