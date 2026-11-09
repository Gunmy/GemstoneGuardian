package game;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

//Text drawn straight onto the canvas, in the pixel font that is bundled with the game
public class guiText {

    private static String family = "Silkscreen";

    //Loads the bundled font so that it also works without internet. Call once before anything is drawn
    public static void loadFont () {
        Font loaded = Font.loadFont(guiText.class.getResourceAsStream("/game/media/Silkscreen-Regular.ttf"), 12);
        if (loaded != null) family = loaded.getFamily();
    }

    public static Font font (double size) {return Font.font(family, size);}

    public static double width (String text, double size) {
        Text measure = new Text(text);
        measure.setFont(font(size));
        return measure.getLayoutBounds().getWidth();
    }

    public static double height (double size) {
        Text measure = new Text("Hg");
        measure.setFont(font(size));
        return measure.getLayoutBounds().getHeight();
    }

    //x is the left, middle or right of the text depending on align. y is always the top
    public static void draw (GraphicsContext gc, String text, double x, double y, double size, Color color, TextAlignment align) {
        gc.setFont(font(size));
        gc.setFill(color);
        gc.setTextAlign(align);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(text, Math.round(x), Math.round(y));
    }

    public static void draw (GraphicsContext gc, String text, double x, double y, double size, Color color) {
        draw(gc, text, x, y, size, color, TextAlignment.LEFT);
    }

    //Splits the text into lines that fit inside maxWidth. Line breaks in the text are kept
    public static List<String> wrap (String text, double size, double maxWidth) {
        List<String> lines = new ArrayList<String>();
        for (String paragraph : text.split("\n")) {
            String line = "";
            for (String word : paragraph.split(" ")) {
                String attempt = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && width(attempt, size) > maxWidth) {
                    lines.add(line);
                    line = word;
                } else {
                    line = attempt;
                }
            }
            lines.add(line);
        }
        return lines;
    }
}
