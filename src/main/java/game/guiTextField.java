package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//A one line box to type in, drawn like a button with the text and a blinking caret. It always has the keyboard
//while its screen is shown. Text set with setText starts out selected, so typing replaces it and enter keeps it
public class guiTextField extends guiElement {

    private String text = "";
    private boolean selected = false;
    private int maxLength;
    private Runnable onEnter;

    //Only the focused field of a screen shows its caret and selection (the screen hands it the keys)
    private boolean focused = true;
    public void setFocused (boolean focused) {this.focused = focused;}

    public guiTextField (gui gui, int maxLength) {
        super(gui);
        if (maxLength < 1) throw new IllegalArgumentException("Maxlength kan ikke være under 1");
        this.maxLength = maxLength;
    }

    public String getText () {return this.text;}

    public void setText (String text) {
        this.text = text.length() > maxLength ? text.substring(0, maxLength) : text;
        this.selected = !this.text.isEmpty();
    }

    public void onEnter (Runnable onEnter) {this.onEnter = onEnter;}

    @Override
    public boolean keyTyped (String typed) {
        if (typed == null || typed.isEmpty()) return false;
        //Control characters (backspace, enter, tab...) come through here too; keyPressed handles those
        for (char c : typed.toCharArray()) {
            if (c < 32 || c == 127) return true;
        }
        if (selected) {
            text = "";
            selected = false;
        }
        if (text.length() + typed.length() <= maxLength) text += typed;
        return true;
    }

    //Takes every key, so nothing typed moves the player or opens the inventory. Escape is left for the screen
    @Override
    public boolean keyPressed (KeyCode code) {
        if (code == KeyCode.ESCAPE) return false;
        if (code == KeyCode.BACK_SPACE) {
            if (selected) text = "";
            else if (!text.isEmpty()) text = text.substring(0, text.length()-1);
            selected = false;
        } else if (code == KeyCode.ENTER) {
            if (onEnter != null) onEnter.run();
        } else if (code == KeyCode.TAB) {
            return false;
        } else if (code == KeyCode.LEFT || code == KeyCode.RIGHT || code == KeyCode.HOME || code == KeyCode.END) {
            selected = false;
        }
        return true;
    }

    @Override
    public void draw (GraphicsContext gc) {
        double s = gui.s();
        gui.getSkin().drawButton(gc, x, y, width, height);
        gc.setFill(Color.rgb(255, 255, 255, 0.35));
        gc.fillRect(x, y, width, height);

        double size = 13*s;
        double inner = gui.getSkin().getBorder()*0.6;
        double textX = x + inner, textY = y + (height - guiText.height(size))/2;
        double textWidth = guiText.width(text, size);

        if (selected && focused) {
            gc.setFill(Color.web("#E0C040", 0.7));
            gc.fillRect(Math.round(textX - 2*s), Math.round(textY), Math.round(textWidth + 4*s), Math.round(guiText.height(size)));
        }
        guiText.draw(gc, text, textX, textY, size, Color.BLACK, TextAlignment.LEFT);

        //Blinks twice a second
        if (focused && !selected && (System.nanoTime()/500_000_000L) % 2 == 0) {
            gc.setFill(Color.BLACK);
            gc.fillRect(Math.round(textX + textWidth + 2*s), Math.round(textY), Math.max(1, Math.round(2*s)), Math.round(guiText.height(size)));
        }
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        selected = false;
        return true;
    }
}
