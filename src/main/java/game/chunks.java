package game;

import java.util.Arrays;
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
    
    public void drawChunks () {

        int tempX = (int) (handler.getCamera().getX()+0.5);
        int tempY = (int) (handler.getCamera().getY()+0.5);
    

        int chunkCountLoad = 2;

        IntStream.range(-chunkCountLoad, chunkCountLoad).forEachOrdered(relativeX -> {
            IntStream.range(-chunkCountLoad, chunkCountLoad).forEachOrdered(relativeY -> {

                if (getChunk(tempX+relativeX, tempY+relativeY) == null) {
                    generateNewChunk(tempX+relativeX, tempY+relativeY);
                }

                getChunk(tempX+relativeX, tempY+relativeY).draw();
            });
        });

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

    public List<Integer> getChunkCoords (int x, int y) {
        return Arrays.asList((int) x, (int) y);
    }

    public List<Integer> getTileInChunkCoords (int x, int y) {
        int tilex = (int) Math.round(x%1*handler.getTileCount())%handler.getTileCount();
        int tiley = (int) Math.round(y%1*handler.getTileCount())%handler.getTileCount();
        return Arrays.asList(tilex, tiley);
    }

    

}
 