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

    //A number as the player sees it: rounded to one decimal, without a trailing .0
    public static String num (double value) {
        double rounded = Math.round(value*10)/10.0;
        if (rounded == 0) rounded = 0; //no "-0"
        return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }

    public static Font font (double size) {return Font.font(family, size);}

    //Icons written in as {key} (textIcons) count as one icon each
    public static double width (String text, double size) {
        if (textIcons.in(text)) {
            double w = 0;
            for (Object part : textIcons.split(text)) w += part instanceof String ? plainWidth((String) part, size) : iconSize(size);
            return w;
        }
        Text measure = new Text(text);
        measure.setFont(font(size));
        return measure.getLayoutBounds().getWidth();
    }

    //A piece of text's width with its spaces at the ends counted
    private static double plainWidth (String text, double size) {
        Text measure = new Text(text + "|"), bar = new Text("|");
        measure.setFont(font(size));
        bar.setFont(font(size));
        return measure.getLayoutBounds().getWidth() - bar.getLayoutBounds().getWidth();
    }

    //How big an icon in the text is: as tall as the line
    public static double iconSize (double size) {return Math.round(height(size)*0.95);}

    public static double height (double size) {
        Text measure = new Text("Hg");
        measure.setFont(font(size));
        return measure.getLayoutBounds().getHeight();
    }

    //x is the left, middle or right of the text depending on align. y is always the top
    public static void draw (GraphicsContext gc, String text, double x, double y, double size, Color color, TextAlignment align) {
        draw(gc, text, x, y, size, color, align, false);
    }

    //outline: a pass of a dark outline round the text (the flying numbers): the icons are drawn in the colour too, as
    //their shape only
    public static void draw (GraphicsContext gc, String text, double x, double y, double size, Color color, TextAlignment align, boolean outline) {
        if (textIcons.in(text)) {
            drawWithIcons(gc, text, x, y, size, color, align, outline);
            return;
        }
        gc.setFont(font(size));
        gc.setFill(color);
        gc.setTextAlign(align);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(text, Math.round(x), Math.round(y));
    }

    private static void drawWithIcons (GraphicsContext gc, String text, double x, double y, double size, Color color, TextAlignment align, boolean outline) {
        double total = width(text, size);
        double left = align == TextAlignment.CENTER ? x - total/2 : align == TextAlignment.RIGHT ? x - total : x;
        double icon = iconSize(size), lineHeight = height(size);
        gc.setFont(font(size));
        gc.setFill(color);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        for (Object part : textIcons.split(text)) {
            if (part instanceof String) {
                gc.fillText((String) part, Math.round(left), Math.round(y));
                left += plainWidth((String) part, size);
            } else {
                ((textIcons.icon) part).draw(gc, left, y + (lineHeight - icon)/2, icon, outline ? color : null);
                left += icon;
            }
        }
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
