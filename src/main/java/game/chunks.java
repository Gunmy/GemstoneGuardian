package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class chunks {

    private Hashtable<List<Integer>, chunk> chunks = new Hashtable<List<Integer>, chunk>();
    private gameHandler handler;
    private int standardTile;
    private double renderDistance;
    private String backgroundColor;
    //Shown to the player, for example in the teleports menu
    private String name;

    private double widthSize;
    private double heightSize;

    public chunks (String name, int standardTile, gameHandler handler, double renderDistance, String backgroundColor, double width, double height) {
        this.name = name;
        this.handler = handler;
        this.standardTile = standardTile;
        this.renderDistance = renderDistance;
        this.backgroundColor = backgroundColor;
        this.widthSize = width;
        this.heightSize = height;
    }

    public List<Double> getUnitWidthHeight () {
        return Arrays.asList(widthSize, heightSize);
    }

    public String getName () {return this.name;}
    //With the darkness cheat on (H, hidden secrets) dark worlds are drawn as far as the light ones
    public double getRenderDistance() {return handler.darknessOff() ? Math.max(this.renderDistance, LIT) : this.renderDistance;}
    public static final double LIT = 1.1;

    //Every tile in the chunks made so far whose type matches, top to bottom and then left to right.
    //Everything from the map file is made at start, so this finds all the tiles placed by hand
    public List<tile> findTiles (Predicate<tileType> matches) {
        List<tile> found = new ArrayList<tile>();
        for (chunk c : new ArrayList<chunk>(chunks.values())) {
            for (tile t : c.getTiles()) {
                if (matches.test(t.getTileType())) found.add(t);
            }
        }
        found.sort(Comparator.comparingDouble(tile::getRealY).thenComparingDouble(tile::getRealX));
        return found;
    }
    public String getBackground () {return this.backgroundColor;}

    //A mirror world (the mirror dimension): every tile is drawn flipped left to right and in cold glassy blues (from
    //the cold copy of the sheet, imgDict.setCold), so a copy of a place with its columns turned round is its mirror
    //image. The characters in it keep their own colours
    private boolean mirror = false;
    public chunks mirror () {this.mirror = true; return this;}
    public boolean isMirror () {return this.mirror;}

    //A sky world: the open sky with its clouds is drawn behind it (and a few in front), see skyLayer
    private boolean sky = false;
    public chunks sky () {this.sky = true; return this;}
    public boolean isSky () {return this.sky;}

    public chunk getChunk (int x, int y) {
        if (chunks.get(Arrays.asList(x, y)) == null) {
            generateNewChunk(x, y);
        }
        return chunks.get(Arrays.asList(x, y));
    }
    
    //A render distance below 1 does not reach the edge of the screen, the rest is left dark
    public boolean isDark () {return getRenderDistance() < 1;}

    //Enough chunks in each direction to cover the screen, also when zoomed out
    private int getChunkCountLoad () {
        double halfScreen = Math.max(handler.getMidWIDTH()/handler.getUnitWidth(), handler.getMidHEIGHT()/handler.getUnitHeight());
        return Math.max(2, (int) Math.ceil(halfScreen+1));
    }

    //Z = 0: the ground of every tile on screen
    public void drawGround () {

        int tempX = (int) (handler.getCamera().getX()+0.5);
        int tempY = (int) (handler.getCamera().getY()+0.5);
        int chunkCountLoad = getChunkCountLoad();

        IntStream.range(-chunkCountLoad, chunkCountLoad).forEachOrdered(relativeX -> {
            IntStream.range(-chunkCountLoad, chunkCountLoad).forEachOrdered(relativeY -> {
                getChunk(tempX+relativeX, tempY+relativeY).drawGround();
            });
        });

    }

    //Z = 1 and up: everything on the given level. Drawn row by row from the top of the screen
    //and down, with the characters in between, so that whatever is further down ends up in front
    public void drawLayer (int z, List<character> characters) {

        int tempX = (int) (handler.getCamera().getX()+0.5);
        int tempY = (int) (handler.getCamera().getY()+0.5);
        int tC = handler.getTileCount();
        int chunkCountLoad = getChunkCountLoad();

        List<character> sorted = new ArrayList<character>();
        for (character c : characters) {
            if (c.getZ() == z) sorted.add(c);
        }
        sorted.sort(Comparator.comparingDouble(character::getFeetY));
        int next = 0;

        for (int chunkY = tempY-chunkCountLoad; chunkY < tempY+chunkCountLoad; chunkY++) {
            for (int relativeY = 0; relativeY < tC; relativeY++) {

                //Characters with their feet above the bottom of this row are behind it
                double rowBottom = chunkY + (relativeY+1.0)/tC;
                while (next < sorted.size() && sorted.get(next).getFeetY() < rowBottom) {
                    sorted.get(next).draw();
                    next++;
                }

                for (int chunkX = tempX-chunkCountLoad; chunkX < tempX+chunkCountLoad; chunkX++) {
                    getChunk(chunkX, chunkY).drawRow(relativeY, z);
                }
            }
        }

        while (next < sorted.size()) {
            sorted.get(next).draw();
            next++;
        }
    }

    private void generateNewChunk (int x, int y) {
        chunks.put(Arrays.asList(x, y), new chunk(x, y, handler, standardTile));
    }

    public void generateSpecialChunk(int x, int y, List<Integer> tiles) {
   

        Hashtable<List<Integer>, tile> hashTiles = new Hashtable<List<Integer>, tile>();

        int tC = handler.getTileCount();

        Iterator<Integer> tileIterator = tiles.iterator();

        IntStream.range(0, tC).forEachOrdered(relativeY -> {
            IntStream.range(0, tC).forEachOrdered(relativeX -> {
                hashTiles.put(Arrays.asList(relativeX, relativeY), new tile(relativeX, relativeY, handler.getTileTypes().get(tileIterator.next()), x, y, handler));
            });
        });

        chunk tempChunk = new chunk(x, y, handler, standardTile);
        tempChunk.setTiles(hashTiles);
        chunks.put(Arrays.asList(x, y), tempChunk);

    }

    public tileType getTileType(double x, double y) {
        return getTile(x, y).getTileType();
    }

    //The tile nearest (x, y) (characters stand on tile corners, and are between two while walking). Rounded to a whole
    //tile first and the chunk worked out from that, so rounding up past a chunk's last row or column gives the first one
    //of the next chunk. (It used to round within the chunk, so the last row wrapped round to the chunk's own first row:
    //walking into the final boss's room from the south stepped on the portal in its north wall.)
    public tile getTile(double x, double y) {
        int tC = handler.getTileCount();
        long tx = Math.round(x*tC), ty = Math.round(y*tC);
        int chunkx = (int) Math.floorDiv(tx, (long) tC);
        int chunky = (int) Math.floorDiv(ty, (long) tC);
        return getChunk(chunkx, chunky).getTile((int) Math.floorMod(tx, (long) tC), (int) Math.floorMod(ty, (long) tC));
    }

}
 