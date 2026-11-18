package game;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//One line of text with pictures in it, like "18 [damage icon]  3 [burn icon] 3 [rounds icon]". Parts sit right
//next to each other (a small gap), gap() puts a wider space between groups. Icons are as tall as the text.
//A divider is a grey label with a thin line after it, for splitting the lines of a box into sections
public class iconText {

    private static final Color GREY = Color.GRAY;

    private final List<Object> parts = new ArrayList<Object>();
    private boolean divider = false;

    private static class words {
        final String text;
        final Color color;
        words (String text, Color color) {this.text = text; this.color = color;}
    }
    private static final Object GAP = new Object();

    public iconText text (String text) {parts.add(new words(text, Color.BLACK)); return this;}
    public iconText grey (String text) {parts.add(new words(text, GREY)); return this;}
    public iconText icon (picture icon) {parts.add(icon); return this;}
    public iconText gap () {parts.add(GAP); return this;}
    //The parts of other after these, with every word grey (an upcoming level milestone in a move's box)
    public iconText appendGreyed (iconText other) {
        for (Object part : other.parts) parts.add(part instanceof words ? new words(((words) part).text, GREY) : part);
        return this;
    }

    //A section label: grey text and a line to the right edge
    public static iconText divider (String label) {
        iconText line = new iconText().grey(label);
        line.divider = true;
        return line;
    }

    public boolean isEmpty () {return parts.isEmpty();}

    public static double iconSize (double size) {return Math.round(guiText.height(size));}
    public double height (double size) {return iconSize(size);}

    private static double partWidth (Object part, double size) {
        if (part == GAP) return size*0.6;
        if (part instanceof words) return guiText.width(((words) part).text, size);
        return iconSize(size);
    }
    private static double spacing (double size) {return Math.round(size*0.15);}

    public double width (double size) {
        double w = 0;
        for (int i = 0; i < parts.size(); i++) w += partWidth(parts.get(i), size) + (i > 0 ? spacing(size) : 0);
        return w;
    }

    //The line split into lines no wider than maxWidth, broken before a part that would stick out (at a gap if there
    //is one). A part wider than the whole width gets a line of its own
    public List<iconText> wrap (double size, double maxWidth) {
        List<iconText> lines = new ArrayList<iconText>();
        if (divider || width(size) <= maxWidth) {
            lines.add(this);
            return lines;
        }
        iconText line = new iconText();
        for (Object part : parts) {
            iconText attempt = new iconText();
            attempt.parts.addAll(line.parts);
            attempt.parts.add(part);
            if (!line.parts.isEmpty() && attempt.width(size) > maxWidth) {
                line.trimGaps();
                lines.add(line);
                line = new iconText();
                if (part != GAP) line.parts.add(part);
            } else {
                line = attempt;
            }
        }
        line.trimGaps();
        if (!line.parts.isEmpty()) lines.add(line);
        return lines;
    }

    private void trimGaps () {
        while (!parts.isEmpty() && parts.get(parts.size() - 1) == GAP) parts.remove(parts.size() - 1);
        while (!parts.isEmpty() && parts.get(0) == GAP) parts.remove(0);
    }

    //y is the top. width is only used by a divider, for its line
    public void draw (GraphicsContext gc, gui gui, double x, double y, double size, double width) {
        double h = height(size), icon = iconSize(size);
        double textY = y + (h - guiText.height(size))/2;
        double at = x;
        for (int i = 0; i < parts.size(); i++) {
            Object part = parts.get(i);
            if (i > 0) at += spacing(size);
            if (part instanceof words) {
                words w = (words) part;
                guiText.draw(gc, w.text, at, textY, size, w.color);
            } else if (part instanceof picture) {
                ((picture) part).draw(gc, gui, at, y + (h - icon)/2, icon);
            }
            at += partWidth(part, size);
        }
        if (divider && at + spacing(size) < x + width) {
            gc.setFill(Color.rgb(0, 0, 0, 0.25));
            gc.fillRect(Math.round(at + 2*spacing(size)), Math.round(y + h/2), Math.round(x + width - at - 2*spacing(size)), Math.max(1, Math.round(size/11)));
        }
    }
}
