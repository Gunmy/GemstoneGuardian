package game;

import java.util.Arrays;

import javafx.scene.paint.Color;
import java.util.Hashtable;
import java.util.List;

public class chunk {

    //Row by row: tile (x, y) is at y*tileCount + x. An array, since a tile is looked up for every tile drawn every frame
    private tile[] tiles;
    private int x, y;
    private gameHandler handler;

    public chunk (int x, int y, gameHandler handler, int standardTile) {
        this.x = x;
        this.y = y;
        this.handler = handler;


        int tC = handler.getTileCount();
        tiles = new tile[tC*tC];

        for (int relativeY = 0; relativeY < tC; relativeY++) {
            for (int relativeX = 0; relativeX < tC; relativeX++) {
                generateNewTile(relativeX, relativeY, handler.getTileTypes().get(standardTile));
            }
        }
    }

    public java.util.Collection<tile> getTiles () {return Arrays.asList(tiles);}

    //The one colour the chunk is drawn in when zoomed out so far that a chunk is a few pixels (gameHandler.chunkView):
    //the colour most of it is, counting each tile as its texture's colour (tileType.getTextureColor, so the pieces of a
    //house in the same brick count together). Solid tiles (walls, roofs, trees, water) count SOLID_WEIGHT times, so a
    //chunk with a house or a wood in it shows that and not the grass round it. Null if no tile has a colour
    public static final int SOLID_WEIGHT = 3;
    public Color getChunkColor (imgDict images) {
        java.util.Map<Integer, Integer> weights = new java.util.HashMap<Integer, Integer>();
        int best = 0, bestWeight = 0;
        for (tile t : tiles) {
            Color c = t.getTileType().getTextureColor(images);
            if (c == null) continue;
            int argb = imgDict.argb(c);
            int weight = weights.merge(argb, t.getTileType().isBlocked() ? SOLID_WEIGHT : 1, Integer::sum);
            if (weight > bestWeight) {best = argb; bestWeight = weight;}
        }
        if (bestWeight == 0) return null;
        return Color.rgb((best >> 16) & 0xFF, (best >> 8) & 0xFF, best & 0xFF);
    }

    //Null outside the chunk
    public tile getTile (int x, int y) {
        int tC = handler.getTileCount();
        if (x < 0 || y < 0 || x >= tC || y >= tC) return null;
        return tiles[y*tC + x];
    }

    private double getRealX () {
        return handler.getMidWIDTH() + ((x-handler.getCamera().getX()))*handler.getUnitWidth();
    }

    private double getRealY () {
        return handler.getMidHEIGHT() + ((y-handler.getCamera().getY()))*handler.getUnitHeight();
    }

    public void drawGround () {
        double realx = getRealX();
        double realy = getRealY();

        drawTiles(realx, realy);

        if (handler.infoOn()) {
            handler.getTempGc().strokeRect(realx, realy, handler.getUnitWidth(), handler.getUnitHeight());

        }
        //handler.getTempGc().drawImage(handler.images.getImage(0), (x%5)*20, (y%2)*20, 20, 20, (int) realx,  (int) realy, handler.getUnitWidth(), handler.getUnitHeight());

    }

    private void drawTiles (double x, double y) {

        int tC = handler.getTileCount();
        double tW = handler.getUnitWidth()/handler.getTileCount();
        double tH = handler.getUnitHeight()/handler.getTileCount();

        for (int relativeX = 0; relativeX < tC; relativeX++) {
            for (int relativeY = 0; relativeY < tC; relativeY++) {
                tiles[relativeY*tC + relativeX].drawGround(x+relativeX*tW, y+relativeY*tH);
            }
        }

    }

    //One row at a time, so that chunks can draw the whole screen row by row
    public void drawRow (int relativeY, int z) {
        double realx = getRealX();
        double realy = getRealY();

        int tC = handler.getTileCount();
        double tW = handler.getUnitWidth()/handler.getTileCount();
        double tH = handler.getUnitHeight()/handler.getTileCount();

        for (int relativeX = 0; relativeX < tC; relativeX++) {
            tiles[relativeY*tC + relativeX].drawLayer(realx+relativeX*tW, realy+relativeY*tH, z);
        }
    }

    public void setTiles ( Hashtable<List<Integer>, tile> replaceTiles) {
        if (replaceTiles.size() != 81) throw new IllegalArgumentException("Ikke rett antall tiles");
        int tC = handler.getTileCount();
        tile[] replaced = new tile[tC*tC];
        for (int relativeY = 0; relativeY < tC; relativeY++) {
            for (int relativeX = 0; relativeX < tC; relativeX++) {
                replaced[relativeY*tC + relativeX] = replaceTiles.get(Arrays.asList(relativeX, relativeY));
            }
        }
        tiles = replaced;
    }

    private void generateNewTile (int x, int y, tileType tileType) {
        tiles[y*handler.getTileCount() + x] = new tile(x, y, tileType, this.x, this.y, this.handler);
    }

}
