package game;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//One box in the item info panel: an optional picture, a title, a smaller subtitle and a text.
//Gemstones make one per ability, other items one for what they do.
//Under the text a box can also have rows (a text on the left, another lined up on the right, see row) and a bar
//(how full, its colour, and a label drawn on it, see bar). Both are drawn in that order and left out when not set
public class infoBox {

    private picture icon;
    private String title, subtitle, text;

    public infoBox (picture icon, String title, String subtitle, String text) {
        this.icon = icon;
        this.title = title;
        this.subtitle = subtitle;
        this.text = text;
    }

    public picture getIcon () {return this.icon;}
    public String getTitle () {return this.title;}
    public String getSubtitle () {return this.subtitle;}
    public String getText () {return this.text;}

    private List<String[]> rows = new ArrayList<String[]>();
    //A line with left on the left and right lined up at the right edge, like a move and its uses
    public infoBox row (String left, String right) {
        rows.add(new String[] {left, right});
        return this;
    }

    private double barFill = -1;
    private Color barColor;
    private String barLabel;
    //A bar filled fill (0 to 1) of the way in color, with label written on it
    public infoBox bar (double fill, Color color, String label) {
        this.barFill = Math.max(0, Math.min(1, fill));
        this.barColor = color;
        this.barLabel = label;
        return this;
    }

    //Drawn as a button-skinned box with the picture to the left, title and subtitle on top and the text wrapped under.
    //Used by the inventory's info panel and by tooltips (moves in battle). Returns the height, draw=false only measures
    public double draw (GraphicsContext gc, gui gui, double x, double y, double width, boolean draw) {
        double s = gui.s();
        double textSize = 11*s, lineHeight = guiText.height(textSize);
        double boxPad = 8*s, iconSize = 24*s;
        double textX = x + boxPad + (icon != null ? iconSize + boxPad : 0);
        double innerWidth = x + width - boxPad - textX;
        List<String> lines = text.isEmpty() ? new ArrayList<String>() : guiText.wrap(text, textSize, innerWidth);
        double headerHeight = guiText.height(12*s) + (subtitle.isEmpty() ? 0 : guiText.height(9*s));
        double barSize = 9*s, barHeight = barSize + 6*s, barGap = 3*s;
        double extra = rows.size()*lineHeight + (barFill >= 0 ? barGap + barHeight : 0);
        double boxHeight = 2*boxPad + Math.max(headerHeight + lines.size()*lineHeight + extra + 2*s, iconSize);

        if (draw) {
            gui.getSkin().drawButton(gc, x, y, width, boxHeight);
            if (icon != null) icon.draw(gc, gui, x + boxPad, y + boxPad, iconSize);

            double lineY = y + boxPad;
            guiText.draw(gc, title, textX, lineY, 12*s, Color.BLACK);
            lineY += guiText.height(12*s);
            if (!subtitle.isEmpty()) {
                guiText.draw(gc, subtitle, textX, lineY, 9*s, Color.GRAY);
                lineY += guiText.height(9*s);
            }
            lineY += 2*s;
            for (String line : lines) {
                guiText.draw(gc, line, textX, lineY, textSize, Color.BLACK);
                lineY += lineHeight;
            }
            for (String[] row : rows) {
                guiText.draw(gc, row[0], textX, lineY, textSize, Color.BLACK);
                guiText.draw(gc, row[1], textX + innerWidth, lineY, textSize, Color.BLACK, javafx.scene.text.TextAlignment.RIGHT);
                lineY += lineHeight;
            }
            if (barFill >= 0) {
                lineY += barGap;
                gc.setFill(Color.rgb(0, 0, 0, 0.2));
                gc.fillRect(textX, lineY, innerWidth, barHeight);
                gc.setFill(barColor);
                gc.fillRect(textX, lineY, innerWidth*barFill, barHeight);
                guiText.draw(gc, barLabel, textX + innerWidth/2, lineY + (barHeight - guiText.height(barSize))/2, barSize, Color.BLACK, javafx.scene.text.TextAlignment.CENTER);
            }
        }
        return boxHeight;
    }
}
