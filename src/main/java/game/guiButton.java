package game;

import java.util.function.Consumer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public class guiButton extends guiElement {

    private String text;
    private Runnable action;
    private boolean enabled = true;
    private boolean hovered = false;
    private double textSize = 13;

    //Optional extra drawing on top of the button, for buttons that show more than text
    private Consumer<GraphicsContext> face;

    //Optional icon in the top right corner. The text then sits in the lower part of the button
    private Image icon;

    public guiButton (gui gui, String text, Runnable action) {
        super(gui);
        this.text = text;
        this.action = action;
    }

    public void setText (String text) {this.text = text;}
    public void setEnabled (boolean enabled) {this.enabled = enabled;}
    public boolean isEnabled () {return this.enabled;}
    public void setFace (Consumer<GraphicsContext> face) {this.face = face;}
    public void setTextSize (double size) {this.textSize = size;}
    public void setIcon (Image icon) {this.icon = icon;}

    @Override
    public void draw (GraphicsContext gc) {
        gui.getSkin().drawButton(gc, x, y, width, height);

        if (hovered && enabled) {
            gc.setFill(Color.rgb(255, 255, 255, 0.25));
            gc.fillRect(x, y, width, height);
        }

        if (text != null && !text.isEmpty()) {
            Color color = enabled ? Color.BLACK : Color.GRAY;
            double size = textSize*gui.s();
            //Shrink the text until it fits
            while (size > 7 && guiText.width(text, size) > width - 2*gui.getSkin().getBorder()*0.6) size -= 1;
            double textY = icon == null ? y + (height - guiText.height(size))/2 : y + height*0.62 - guiText.height(size)/2;
            guiText.draw(gc, text, x + width/2, textY, size, color, TextAlignment.CENTER);
        }

        if (icon != null) {
            double iconSize = Math.round(Math.min(height*0.4, 24*gui.s()));
            double inset = Math.round(gui.getSkin().getBorder()*0.4);
            gc.drawImage(icon, 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, Math.round(x + width - inset - iconSize), Math.round(y + inset), iconSize, iconSize);
        }

        if (face != null) face.accept(gc);
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        if (enabled && action != null) action.run();
        return true;
    }

    @Override
    public void hover (double mx, double my) {
        hovered = contains(mx, my);
    }
}
