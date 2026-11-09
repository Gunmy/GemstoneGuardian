package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class chunks {

    private Hashtable<List<Integer>, chunk> chunks = new Hashtable<List<Integer>, chunk>();
    private gameHandler handler;
    private int standardTile;
    private double renderDistance;
    private String backgroundColor;

    private double widthSize;
    private double heightSize;

    public chunks (int standardTile, gameHandler handler, double renderDistance, String backgroundColor, double width, double height) {
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

    public double getRenderDistance() {return this.renderDistance;}
    public String getBackground () {return this.backgroundColor;}

    public chunk getChunk (int x, int y) {
        if (chunks.get(Arrays.asList(x, y)) == null) {
            generateNewChunk(x, y);
        }
        return chunks.get(Arrays.asList(x, y));
    }
    
    //Z = 0: the ground of every tile on screen
    public void drawGround () {

        int tempX = (int) (handler.getCamera().getX()+0.5);
        int tempY = (int) (handler.getCamera().getY()+0.5);
        int chunkCountLoad = 2;

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
        int chunkCountLoad = 2;

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

    public tile getTile(double x, double y) {
        int chunkx = (int) x;
        int chunky = (int) y;

        int tilex = (int) Math.round(x%1*handler.getTileCount())%handler.getTileCount();
        int tiley = (int) Math.round(y%1*handler.getTileCount())%handler.getTileCount();        

        return getChunk(chunkx, chunky).getTile(tilex, tiley);
    }

}
 