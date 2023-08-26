package game;
import java.util.ArrayList;
import java.util.List;


import javafx.scene.image.Image;


public class imgDict {

    private String path = "/game/media/";
    private List<Image> images  = new ArrayList<Image>();

    public imgDict () {
        images.add(new Image(path + "tiles.png"));
    }

    public Image getImage(Integer n) {
        if (n < 0 || n >= images.size()) throw new IllegalArgumentException("Item finnes ikke");
        return images.get(n);
    }
}
