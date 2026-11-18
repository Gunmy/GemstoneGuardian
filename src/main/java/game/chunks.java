package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import javafx.scene.image.PixelFormat;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;

public class chunks {

    //By position, see key. Looked up for every row of every chunk on screen every frame, so the key is a plain number
    private HashMap<Long, chunk> chunks = new HashMap<Long, chunk>();
    private static long key (int x, int y) {return ((long) x << 32) | (y & 0xFFFFFFFFL);}
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

    //An open world (outside) that has clouds high over it, seen when zoomed far out (skyLayer.drawHigh)
    private boolean cloudsFromAfar = false;
    public chunks cloudsFromAfar () {this.cloudsFromAfar = true; return this;}
    public boolean hasCloudsFromAfar () {return this.cloudsFromAfar;}
    public boolean isSky () {return this.sky;}

    //A world that can be seen from space (gameHandler.planetView, spaceView): zoomed far out, behind a cover of cloud,
    //it becomes a planet, a square in space. The square is around every chunk of the map with SQUARE_MARGIN chunks to
    //spare, centred on them, as wide as the map is long (makeSquare, after the map is read): the planet is that big,
    //and where the player is on the map is where the view comes out of the clouds
    private boolean space = false;
    public chunks space () {this.space = true; return this;}
    //Seen from space as another world's planet (the sky ruins are over the overworld, so they show its planet)
    private chunks planetOf;
    public chunks space (chunks planetOf) {this.space = true; this.planetOf = planetOf; return this;}
    //The world whose planet is shown from space: its own, or the one set with space(planetOf)
    public chunks getPlanet () {return planetOf != null ? planetOf : this;}
    public boolean isSpace () {return this.space;}

    public static final int SQUARE_MARGIN = 2;
    //Left, top and side, in chunks; null before makeSquare or for a world with no chunks
    private double[] square;
    public double[] getSquare () {return this.square;}

    public void makeSquare () {
        if (chunks.isEmpty()) return;
        long minX = Long.MAX_VALUE, maxX = Long.MIN_VALUE, minY = Long.MAX_VALUE, maxY = Long.MIN_VALUE;
        for (long k : chunks.keySet()) {
            int x = (int) (k >> 32), y = (int) k;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        double side = Math.max(maxX - minX + 1, maxY - minY + 1) + 2*SQUARE_MARGIN;
        double midX = (minX + maxX + 1)/2.0, midY = (minY + maxY + 1)/2.0;
        square = new double[] {Math.floor(midX - side/2), Math.floor(midY - side/2), side};
    }


    public chunk getChunk (int x, int y) {
        chunk c = chunks.get(key(x, y));
        if (c == null) c = generateNewChunk(x, y);
        return c;
    }
    
    //A render distance below 1 does not reach the edge of the screen, the rest is left dark
    public boolean isDark () {return getRenderDistance() < 1;}

    //Enough chunks to cover the screen, also when zoomed out, counted across and down on their own (a wide screen needs
    //fewer rows than columns). The extra chunk keeps the tiles just outside the screen, whose tall overlays reach in
    private int getChunkCountX () {return Math.max(2, (int) Math.ceil(handler.getMidWIDTH()/handler.getUnitWidth()+1));}
    private int getChunkCountY () {return Math.max(2, (int) Math.ceil(handler.getMidHEIGHT()/handler.getUnitHeight()+1));}

    //Whether any tile of the chunk can be lit: the light fades with the distance from the middle of the screen
    //(camera.inRendered), and no tile in a chunk is nearer than the nearest point of the chunk. Tiles fainter than 0.01
    //are not drawn (tile.drawGround), so a chunk with its nearest point that faint is skipped as a whole. In a dark world
    //zoomed out that is most of the screen
    private boolean anyLit (int chunkX, int chunkY) {
        camera cam = handler.getCamera();
        double left = handler.getMidWIDTH() + (chunkX - cam.getX())*handler.getUnitWidth();
        double top = handler.getMidHEIGHT() + (chunkY - cam.getY())*handler.getUnitHeight();
        double nearestX = Math.max(left, Math.min(handler.getMidWIDTH(), left + handler.getUnitWidth()));
        double nearestY = Math.max(top, Math.min(handler.getMidHEIGHT(), top + handler.getUnitHeight()));
        return cam.inRendered(nearestX, nearestY) > 0.01;
    }

    //Whether every tile of the chunk is fully lit: its furthest point from the middle of the screen is
    private boolean allLit (int chunkX, int chunkY) {
        camera cam = handler.getCamera();
        double left = handler.getMidWIDTH() + (chunkX - cam.getX())*handler.getUnitWidth();
        double top = handler.getMidHEIGHT() + (chunkY - cam.getY())*handler.getUnitHeight();
        double farX = Math.abs(left - handler.getMidWIDTH()) > Math.abs(left + handler.getUnitWidth() - handler.getMidWIDTH()) ? left : left + handler.getUnitWidth();
        double farY = Math.abs(top - handler.getMidHEIGHT()) > Math.abs(top + handler.getUnitHeight() - handler.getMidHEIGHT()) ? top : top + handler.getUnitHeight();
        return cam.inRendered(farX, farY) >= 1;
    }

    //The colour a chunk is drawn in when every chunk is one colour (gameHandler.chunkView), as ARGB (see
    //chunk.getChunkColor). Worked out once per chunk
    private HashMap<Long, Integer> chunkColors = new HashMap<Long, Integer>();
    private int chunkColor (int x, int y) {
        Integer known = chunkColors.get(key(x, y));
        if (known != null) return known;
        chunk c = chunks.get(key(x, y));
        //A chunk never visited is all the standard tile: one stands in for all of them
        if (c == null) c = standardChunk();
        Color color = c.getChunkColor(handler.getImages());
        if (color != null && isMirror()) color = imgDict.cold(color);
        int argb = imgDict.argb(color);
        //Only chunks that exist are kept, so the map does not fill up with the empty ones flown over
        if (chunks.containsKey(key(x, y))) chunkColors.put(key(x, y), argb);
        return argb;
    }
    private chunk standard;
    private chunk standardChunk () {
        if (standard == null) standard = new chunk(0, 0, handler, standardTile);
        return standard;
    }

    //Zoomed out so far that a chunk is a few pixels (gameHandler.chunkView): one pixel per chunk in its colour
    //(chunkColor), faded like the tiles, drawn scaled up in one go like drawFlat
    private WritableImage chunkImage;
    private int[] chunkPixels = new int[0];

    public void drawChunks () {
        camera cam = handler.getCamera();
        int tempX = (int) (cam.getX()+0.5);
        int tempY = (int) (cam.getY()+0.5);
        int countX = getChunkCountX(), countY = getChunkCountY();
        int firstX = tempX-countX, firstY = tempY-countY;
        int cols = 2*countX, rows = 2*countY;

        if (chunkImage == null || chunkImage.getWidth() < cols || chunkImage.getHeight() < rows) {
            chunkImage = new WritableImage(Math.max(cols, chunkImage == null ? 0 : (int) chunkImage.getWidth()),
                Math.max(rows, chunkImage == null ? 0 : (int) chunkImage.getHeight()));
        }
        if (chunkPixels.length < cols*rows) chunkPixels = new int[cols*rows];

        double cW = handler.getUnitWidth(), cH = handler.getUnitHeight();
        double screenLeft = handler.getMidWIDTH() + (firstX - cam.getX())*cW;
        double screenTop = handler.getMidHEIGHT() + (firstY - cam.getY())*cH;
        for (int cy = 0; cy < rows; cy++) {
            for (int cx = 0; cx < cols; cx++) {
                int color = 0;
                if (anyLit(firstX + cx, firstY + cy)) {
                    color = chunkColor(firstX + cx, firstY + cy);
                    double fade = cam.inRendered(screenLeft + (cx + 0.5)*cW, screenTop + (cy + 0.5)*cH);
                    if (fade < 1) color = ((int) (Math.max(0, fade)*(color >>> 24)) << 24) | (color & 0xFFFFFF);
                }
                chunkPixels[cy*cols + cx] = color;
            }
        }

        chunkImage.getPixelWriter().setPixels(0, 0, cols, rows, PixelFormat.getIntArgbInstance(), chunkPixels, 0, cols);
        handler.getTempGc().drawImage(chunkImage, 0, 0, cols, rows, screenLeft, screenTop, cols*cW, rows*cH);
    }

    //Zoomed so far out that every tile is one colour (gameHandler.flatView): the whole screen of tiles is written into
    //an image with one pixel per tile, in each tile's primary colour (tileType.getPrimaryColor), faded like the tiles
    //are, and that image is drawn scaled up in one go. One draw instead of one per tile, which is what lets the view go
    //this far out. Chunks never visited are not made for it, they are the world's standard tile
    private WritableImage flatImage;
    private int[] flatPixels = new int[0];

    public void drawFlat () {
        camera cam = handler.getCamera();
        imgDict images = handler.getImages();
        int tempX = (int) (cam.getX()+0.5);
        int tempY = (int) (cam.getY()+0.5);
        int tC = handler.getTileCount();
        int countX = getChunkCountX(), countY = getChunkCountY();
        int firstX = tempX-countX, firstY = tempY-countY;
        int cols = 2*countX*tC, rows = 2*countY*tC;

        if (flatImage == null || flatImage.getWidth() < cols || flatImage.getHeight() < rows) {
            int w = Math.max(cols, flatImage == null ? 0 : (int) flatImage.getWidth());
            int h = Math.max(rows, flatImage == null ? 0 : (int) flatImage.getHeight());
            flatImage = new WritableImage(w, h);
        }
        if (flatPixels.length < cols*rows) flatPixels = new int[cols*rows];

        boolean cold = isMirror();
        int standard = handler.getTileTypes().get(standardTile).getPrimaryArgb(images, cold);
        double tW = handler.getUnitWidth()/tC, tH = handler.getUnitHeight()/tC;
        double screenLeft = handler.getMidWIDTH() + (firstX - cam.getX())*handler.getUnitWidth();
        double screenTop = handler.getMidHEIGHT() + (firstY - cam.getY())*handler.getUnitHeight();

        for (int cy = 0; cy < 2*countY; cy++) {
            for (int cx = 0; cx < 2*countX; cx++) {
                int chunkX = firstX + cx, chunkY = firstY + cy;
                int base = cy*tC*cols + cx*tC;
                boolean any = anyLit(chunkX, chunkY);
                boolean all = any && allLit(chunkX, chunkY);
                chunk c = chunks.get(key(chunkX, chunkY));
                for (int ry = 0; ry < tC; ry++) {
                    for (int rx = 0; rx < tC; rx++) {
                        int color = 0;
                        if (any) {
                            color = c == null ? standard : c.getTile(rx, ry).getTileType().getPrimaryArgb(images, cold);
                            if (!all && color != 0) {
                                //Faded out towards the edge of the light like the tiles are (tile.getRenderVal)
                                double fade = cam.inRendered(screenLeft + (cx*tC + rx + 0.5)*tW, screenTop + (cy*tC + ry + 0.5)*tH);
                                int alpha = fade <= 0.01 ? 0 : (int) (Math.min(1, fade)*(color >>> 24));
                                color = (alpha << 24) | (color & 0xFFFFFF);
                            }
                        }
                        flatPixels[base + ry*cols + rx] = color;
                    }
                }
            }
        }

        flatImage.getPixelWriter().setPixels(0, 0, cols, rows, PixelFormat.getIntArgbInstance(), flatPixels, 0, cols);
        handler.getTempGc().drawImage(flatImage, 0, 0, cols, rows, screenLeft, screenTop, cols*tW, rows*tH);
    }

    //Z = 0: the ground of every tile on screen
    public void drawGround () {

        int tempX = (int) (handler.getCamera().getX()+0.5);
        int tempY = (int) (handler.getCamera().getY()+0.5);
        int countX = getChunkCountX(), countY = getChunkCountY();

        for (int relativeX = -countX; relativeX < countX; relativeX++) {
            for (int relativeY = -countY; relativeY < countY; relativeY++) {
                if (anyLit(tempX+relativeX, tempY+relativeY)) getChunk(tempX+relativeX, tempY+relativeY).drawGround();
            }
        }

    }

    //Z = 1 and up: everything on the given level. Drawn row by row from the top of the screen
    //and down, with the characters in between, so that whatever is further down ends up in front
    public void drawLayer (int z, List<character> characters) {

        int tempX = (int) (handler.getCamera().getX()+0.5);
        int tempY = (int) (handler.getCamera().getY()+0.5);
        int tC = handler.getTileCount();
        int countX = getChunkCountX(), countY = getChunkCountY();

        //The chunks of the screen that have anything lit, the same for every row of tiles in them
        int firstX = tempX-countX, firstY = tempY-countY;
        chunk[][] lit = new chunk[2*countY][2*countX];
        for (int chunkY = firstY; chunkY < tempY+countY; chunkY++) {
            for (int chunkX = firstX; chunkX < tempX+countX; chunkX++) {
                if (anyLit(chunkX, chunkY)) lit[chunkY-firstY][chunkX-firstX] = getChunk(chunkX, chunkY);
            }
        }

        List<character> sorted = new ArrayList<character>();
        for (character c : characters) {
            if (c.getZ() == z) sorted.add(c);
        }
        sorted.sort(Comparator.comparingDouble(character::getFeetY));
        int next = 0;

        for (int chunkY = firstY; chunkY < tempY+countY; chunkY++) {
            chunk[] litRow = lit[chunkY-firstY];
            for (int relativeY = 0; relativeY < tC; relativeY++) {

                //Characters with their feet above the bottom of this row are behind it
                double rowBottom = chunkY + (relativeY+1.0)/tC;
                while (next < sorted.size() && sorted.get(next).getFeetY() < rowBottom) {
                    sorted.get(next).draw();
                    next++;
                }

                for (chunk c : litRow) {
                    if (c != null) c.drawRow(relativeY, z);
                }
            }
        }

        while (next < sorted.size()) {
            sorted.get(next).draw();
            next++;
        }
    }

    private chunk generateNewChunk (int x, int y) {
        chunk c = new chunk(x, y, handler, standardTile);
        chunks.put(key(x, y), c);
        return c;
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
        chunks.put(key(x, y), tempChunk);
        chunkColors.remove(key(x, y));

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
 