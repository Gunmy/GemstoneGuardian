package game;

//A tile together with the world it is in, since tiles do not know their world. Used for teleport targets
public class worldTile {

    private final int world;
    private final tile tile;

    public worldTile (int world, tile tile) {
        this.world = world;
        this.tile = tile;
    }

    public int getWorld () {return this.world;}
    public tile getTile () {return this.tile;}
    public double getX () {return tile.getRealX();}
    public double getY () {return tile.getRealY();}
    public int getChunkX () {return (int) tile.getRealX();}
    public int getChunkY () {return (int) tile.getRealY();}

    @Override
    public boolean equals (Object other) {
        return other instanceof worldTile && ((worldTile) other).world == world && ((worldTile) other).tile == tile;
    }

    @Override
    public int hashCode () {return 31*world + System.identityHashCode(tile);}
}
