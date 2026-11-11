package game;

//A chest in the world: every tile of a container type (tileTypes, container()) is one. Clicking it from one of the
//8 tiles around it opens the inventory with its ROWS rows of slots above (gameHandler.clickWorld). It can always be
//opened again, and keeps whatever is left in it (saved with the campaign). Items are put in by row and column, so
//rocks that interact can be laid next to each other. onFirstClose runs the first time it is closed after opening
//(a story moment, like grandma walking over after you find your first stone)
public class chest extends itemContainer {

    public static final int ROWS = 3;

    private worldTile place;
    private boolean closedOnce = false;
    private Runnable onFirstClose;

    public chest (worldTile place) {
        super(ROWS*inventory.COLUMNS);
        this.place = place;
    }

    public worldTile getPlace () {return this.place;}

    //Puts an item in a slot, row 0 to ROWS-1 from the top, column 0 to 7 from the left
    public chest put (int row, int column, item item) {
        if (row < 0 || row >= ROWS || column < 0 || column >= inventory.COLUMNS) throw new IllegalArgumentException("Utenfor kisten");
        set(row*inventory.COLUMNS + column, new itemStack(item));
        return this;
    }

    public chest onFirstClose (Runnable onFirstClose) {
        this.onFirstClose = onFirstClose;
        return this;
    }

    @Override
    public void closed () {
        if (closedOnce) return;
        closedOnce = true;
        if (onFirstClose != null) onFirstClose.run();
    }

    public boolean hasBeenClosed () {return this.closedOnce;}

    //Heals the player the first time it is closed (gameHandler sets that up). Until then it gives off the healing
    //pads' green particles, so the player can see it (tile.drawGround)
    private boolean heals = false;
    public chest healing () {
        this.heals = true;
        return this;
    }
    public boolean willHeal () {return heals && !closedOnce;}
    public void setBeenClosed (boolean closedOnce) {this.closedOnce = closedOnce;}
}
