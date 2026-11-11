package game;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;


public class imgDict {

    private String path = "/game/media/";
    private List<Image> images  = new ArrayList<Image>();

    //Grey-scale cells of the sheet coloured in, made once per colour and kept
    private Map<String, Image> tinted = new HashMap<String, Image>();

    public imgDict () {
        images.add(new Image(path + "tiles.png"));
    }

    public Image getImage(Integer n) {
        if (n < 0 || n >= images.size()) throw new IllegalArgumentException("Item finnes ikke");
        if (n == 0 && cold) return coldSheet();
        return images.get(n);
    }

    //While set, the sheet handed out is its cold copy: the tiles of a mirror world are drawn from it (tile.flipStart
    //sets it around each tile), and the characters standing on them keep their own colours
    private boolean cold = false;
    public void setCold (boolean cold) {this.cold = cold;}

    //The whole sheet washed out and laid over with a pale blue, made the first time it is needed
    private Image coldSheet;
    private Image coldSheet () {
        if (coldSheet == null) {
            Image sheet = images.get(0);
            int w = (int) sheet.getWidth(), h = (int) sheet.getHeight();
            WritableImage out = new WritableImage(w, h);
            PixelReader reader = sheet.getPixelReader();
            PixelWriter writer = out.getPixelWriter();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Color c = reader.getColor(x, y);
                    if (c.getOpacity() == 0) {
                        writer.setColor(x, y, c);
                        continue;
                    }
                    //Washed out (most of the way to its own grey), a little brighter, then a pale blue over it
                    double grey = 0.3*c.getRed() + 0.59*c.getGreen() + 0.11*c.getBlue();
                    double r = Math.min(1, (c.getRed() + (grey - c.getRed())*0.55)*1.08);
                    double g = Math.min(1, (c.getGreen() + (grey - c.getGreen())*0.55)*1.08);
                    double b = Math.min(1, (c.getBlue() + (grey - c.getBlue())*0.55)*1.08);
                    double a = 0.22;
                    writer.setColor(x, y, new Color(r + (190/255.0 - r)*a, g + (225/255.0 - g)*a, b + (1 - b)*a, c.getOpacity()));
                }
            }
            coldSheet = out;
        }
        return coldSheet;
    }

    //One cell of the sheet with its grey levels turned into shades of the colour (null keeps it as it is).
    //The result is a cell sized image, so it is drawn from (0, 0)
    public Image getTinted (int cellX, int cellY, String hex) {
        return getTinted(cellX, cellY, hex, 0);
    }

    //lift (0 to under 1) raises the grey levels before they are tinted, so a light colour comes out bright and clean
    //instead of muddied by the dark greys: a grey g becomes lift + (1 - lift)*g
    public Image getTinted (int cellX, int cellY, String hex, double lift) {
        if (lift < 0 || lift >= 1) throw new IllegalArgumentException("Lift må være fra 0 til under 1");
        return recolor("tint" + (lift > 0 ? lift : ""), cellX, cellY, hex, false, lift);
    }

    //One cell of the sheet with every pixel moved this share of the way to white: lighter, but with all of the texture kept
    public Image getBrightened (int cellX, int cellY, double amount) {
        if (amount < 0 || amount > 1) throw new IllegalArgumentException("Amount må være mellom 0 og 1");
        String key = "bright," + cellX + "," + cellY + "," + amount;
        if (!tinted.containsKey(key)) {
            int size = guiSkin.CELL;
            WritableImage out = new WritableImage(size, size);
            PixelReader reader = images.get(0).getPixelReader();
            PixelWriter writer = out.getPixelWriter();
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    Color c = reader.getColor(cellX+x, cellY+y);
                    writer.setColor(x, y, new Color(c.getRed() + (1 - c.getRed())*amount, c.getGreen() + (1 - c.getGreen())*amount,
                        c.getBlue() + (1 - c.getBlue())*amount, c.getOpacity()));
                }
            }
            tinted.put(key, out);
        }
        return tinted.get(key);
    }

    //The edge of a cell's shape in the colour: the pixels that are not transparent but are within `thickness` pixels of
    //one that is (or of the cell's border). An outline made from the texture itself
    public Image getOutline (int cellX, int cellY, String hex, int thickness) {
        String key = "outline," + cellX + "," + cellY + "," + hex + "," + thickness;
        if (!tinted.containsKey(key)) {
            int size = guiSkin.CELL;
            WritableImage out = new WritableImage(size, size);
            PixelReader reader = images.get(0).getPixelReader();
            PixelWriter writer = out.getPixelWriter();
            Color color = Color.web(hex);
            boolean[] solid = new boolean[size*size];
            for (int y = 1; y < size-1; y++) {
                for (int x = 1; x < size-1; x++) solid[y*size + x] = reader.getColor(cellX+x, cellY+y).getOpacity() > 0.5;
            }
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (!solid[y*size + x]) continue;
                    boolean edge = false;
                    for (int dy = -thickness; dy <= thickness && !edge; dy++) {
                        for (int dx = -thickness; dx <= thickness && !edge; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx < 0 || ny < 0 || nx >= size || ny >= size || !solid[ny*size + nx]) edge = true;
                        }
                    }
                    if (edge) writer.setColor(x, y, color);
                }
            }
            tinted.put(key, out);
        }
        return tinted.get(key);
    }

    //One cell of the sheet where every pixel that is not transparent gets the colour: the shape only
    public Image getSilhouette (int cellX, int cellY, String hex) {
        return recolor("silhouette", cellX, cellY, hex, true, 0);
    }

    private Image recolor (String mode, int cellX, int cellY, String hex, boolean silhouette, double lift) {
        String key = mode + "," + cellX + "," + cellY + "," + hex;
        if (!tinted.containsKey(key)) {
            int size = guiSkin.CELL;
            WritableImage out = new WritableImage(size, size);
            PixelReader reader = images.get(0).getPixelReader();
            PixelWriter writer = out.getPixelWriter();
            Color tint = hex == null ? null : Color.web(hex);

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    Color c = reader.getColor(cellX+x, cellY+y);
                    if (silhouette) {
                        c = new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), c.getOpacity());
                    } else if (tint != null) {
                        double grey = lift + (1 - lift)*(c.getRed() + c.getGreen() + c.getBlue())/3;
                        c = new Color(grey*tint.getRed(), grey*tint.getGreen(), grey*tint.getBlue(), c.getOpacity());
                    }
                    writer.setColor(x, y, c);
                }
            }
            tinted.put(key, out);
        }
        return tinted.get(key);
    }
}
