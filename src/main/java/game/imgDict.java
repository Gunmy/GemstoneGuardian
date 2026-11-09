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
        return images.get(n);
    }

    //One cell of the sheet with its grey levels turned into shades of the colour (null keeps it as it is).
    //The result is a cell sized image, so it is drawn from (0, 0)
    public Image getTinted (int cellX, int cellY, String hex) {
        return recolor("tint", cellX, cellY, hex, false);
    }

    //One cell of the sheet where every pixel that is not transparent gets the colour: the shape only
    public Image getSilhouette (int cellX, int cellY, String hex) {
        return recolor("silhouette", cellX, cellY, hex, true);
    }

    private Image recolor (String mode, int cellX, int cellY, String hex, boolean silhouette) {
        String key = mode + "," + cellX + "," + cellY + "," + hex;
        if (!tinted.containsKey(key)) {
            int size = guiSkin.CELL;
            WritableImage out = new WritableImage(size, size);
            PixelReader reader = getImage(0).getPixelReader();
            PixelWriter writer = out.getPixelWriter();
            Color tint = hex == null ? null : Color.web(hex);

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    Color c = reader.getColor(cellX+x, cellY+y);
                    if (silhouette) {
                        c = new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), c.getOpacity());
                    } else if (tint != null) {
                        double grey = (c.getRed() + c.getGreen() + c.getBlue())/3;
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
