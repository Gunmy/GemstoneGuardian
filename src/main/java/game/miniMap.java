package game;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

//A small still picture of the map around one chunk: the chunk and `radius` chunks in every direction,
//with the middle chunk outlined and one tile in it marked. Drawn once per size and kept as an image,
//since the tiles do not move in it. Used by the teleports menu to hint at where a healing pad is
public class miniMap {

    private gameHandler handler;
    private int world, chunkX, chunkY, radius;
    private tile mark;

    private Image cached;
    private int cachedSize = -1;

    public miniMap (gameHandler handler, int world, int chunkX, int chunkY, int radius, tile mark) {
        if (radius < 0) throw new IllegalArgumentException("Radius kan ikke være under 0");
        this.handler = handler;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.radius = radius;
        this.mark = mark;
    }

    //The picture, size pixels wide and high
    public Image get (int size) {
        if (size != cachedSize) {
            cached = render(Math.max(1, size));
            cachedSize = size;
        }
        return cached;
    }

    private Image render (int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);

        chunks map = handler.getWorld(world);
        Image sheet = handler.getImages().getImage(0);
        int tC = handler.getTileCount();
        int span = (2*radius + 1)*tC;

        gc.setFill(Color.web(map.getBackground()));
        gc.fillRect(0, 0, size, size);

        for (int ty = 0; ty < span; ty++) {
            for (int tx = 0; tx < span; tx++) {
                tile t = map.getChunk(chunkX - radius + tx/tC, chunkY - radius + ty/tC).getTile(tx%tC, ty%tC);
                //Whole pixels, so there are no gaps between the tiles
                double x0 = edge(tx, span, size), x1 = edge(tx+1, span, size);
                double y0 = edge(ty, span, size), y1 = edge(ty+1, span, size);
                t.getTileType().drawMini(gc, sheet, x0, y0, x1-x0, y1-y0);
            }
        }

        double line = Math.max(1, Math.round(size/90.0));
        gc.setLineWidth(line);

        //The middle chunk
        double c0 = edge(radius*tC, span, size), c1 = edge((radius+1)*tC, span, size);
        gc.setStroke(Color.WHITE);
        gc.strokeRect(c0 + line/2, c0 + line/2, c1 - c0 - line, c1 - c0 - line);

        //The marked tile
        if (mark != null) {
            int mx = (int) Math.round((mark.getRealX() - (chunkX - radius))*tC);
            int my = (int) Math.round((mark.getRealY() - (chunkY - radius))*tC);
            double x0 = edge(mx, span, size), x1 = edge(mx+1, span, size);
            double y0 = edge(my, span, size), y1 = edge(my+1, span, size);
            gc.setStroke(Color.web("#FFE040"));
            gc.strokeRect(x0 - line/2, y0 - line/2, x1 - x0 + line, y1 - y0 + line);
        }

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, null);
    }

    private static double edge (int tile, int span, int size) {
        return Math.round(tile*(double) size/span);
    }
}
