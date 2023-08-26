package game;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.IntStream;

public class chunk {

    private Hashtable<List<Integer>, tile> tiles = new Hashtable<List<Integer>, tile>();
    private int x, y;
    private gameHandler handler;

    public chunk (int x, int y, gameHandler handler, int standardTile) {
        this.x = x;
        this.y = y;
        this.handler = handler;


        int tC = handler.getTileCount();

        IntStream.range(0, tC).forEachOrdered(relativeY -> {
            IntStream.range(0, tC).forEachOrdered(relativeX -> {
                generateNewTile(relativeX, relativeY, handler.getTileTypes().get(standardTile));
            });
        });
    }

    public tile getTile (int x, int y) {
        return tiles.get(Arrays.asList(x, y));
    }

    public void draw () {
        camera cam = handler.getCamera();

        double realx = handler.getMidWIDTH() + ((x-cam.getX()))*handler.getUnitWidth();
        double realy = handler.getMidHEIGHT() + ((y-cam.getY()))*handler.getUnitHeight();

        drawTiles(realx, realy);

        if (handler.infoOn()) {
            handler.getTempGc().strokeRect(realx, realy, handler.getUnitWidth(), handler.getUnitHeight());

        }
        //handler.getTempGc().drawImage(handler.images.getImage(0), (x%5)*100, (y%2)*100, 100, 100, (int) realx,  (int) realy, handler.getUnitWidth(), handler.getUnitHeight());
        
    }

    private void drawTiles (double x, double y) {    

        int tC = handler.getTileCount();
        double tW = handler.getUnitWidth()/handler.getTileCount();
        double tH = handler.getUnitHeight()/handler.getTileCount();

        //For loops er nok bedre her men streams er kulere B)
        IntStream.range(0, tC).forEachOrdered(relativeX -> {
            IntStream.range(0, tC).forEachOrdered(relativeY -> {
                getTile(relativeX, relativeY).draw(x+relativeX*tW, y+relativeY*tH);
            });
        });

    }

    public void setTiles ( Hashtable<List<Integer>, tile> replaceTiles) {
        if (replaceTiles.size() != 81) throw new IllegalArgumentException("Ikke rett antall tiles");
        tiles = replaceTiles;
    }

    private void generateNewTile (int x, int y, tileType tileType) {
        tiles.put(Arrays.asList(x, y), new tile(x, y, tileType, this.x, this.y, this.handler));
    }

}
