package game;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The box at the bottom that shows who is talking and what they say. Clicks fall through, so a click anywhere continues.
//A special dialogue (see dialogue.highlight) gets a coloured border and a picture drawn in the right part of the box
public class guiDialogue extends guiElement {

    private double textSize = 12;
    //How much of the box the picture gets when there is one
    private double iconPart = 0.25;

    public guiDialogue (gui gui) {
        super(gui);
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        double s = gui.s();
        width = Math.min(700*s, screenWidth - 40*s);
        //Tall enough for the name, every line and the continue line, but never smaller than before
        height = 110*s;
        dialogue current = gui.getHandler().getDiealoguecontainer().getCurrent();
        if (current != null) {
            double textSize = this.textSize*s, pad = gui.getSkin().getBorder();
            int lines = guiText.wrap(current.getText(), textSize, textWidth(current)).size();
            height = Math.max(height, Math.ceil(2*pad + (lines + 2)*guiText.height(textSize)));
        }
        x = Math.round((screenWidth - width)/2);
        y = Math.round(screenHeight - height - 70*s);
    }

    //The picture is as tall as the normal box allows, so the text next to it keeps its width when the box grows
    private double pictureSize () {
        return Math.min(110*gui.s() - 2*gui.getSkin().getBorder(), width*iconPart) * 0.8;
    }

    private double textWidth (dialogue current) {
        double pad = gui.getSkin().getBorder();
        double w = width - 2*pad;
        if (current.isSpecial() && current.getPicture() != null) w -= pictureSize() + pad;
        return w;
    }

    @Override
    public void draw (GraphicsContext gc) {
        dialogue current = gui.getHandler().getDiealoguecontainer().getCurrent();
        if (current == null) return;

        gui.getSkin().drawPanel(gc, x, y, width, height);

        double textSize = this.textSize*gui.s();
        double pad = gui.getSkin().getBorder();
        double lineHeight = guiText.height(textSize);
        double textWidth = textWidth(current);

        if (current.isSpecial()) {
            //Coloured border just inside the frame
            if (current.getBorder() != null) {
                gc.setStroke(Color.web(current.getBorder()));
                gc.setLineWidth(3*gui.s());
                gc.strokeRect(x + pad/2, y + pad/2, width - pad, height - pad);
            }

            //The picture, against the right side of the box and centered vertically
            if (current.getPicture() != null) {
                double size = pictureSize();
                current.getPicture().draw(gc, gui, x + width - pad - size, y + (height - size)/2, size);
            }
        }

        double textY = y + pad;
        guiText.draw(gc, current.getNameText() + ":", x + pad, textY, textSize, Color.BLACK);
        textY += lineHeight;

        List<String> lines = guiText.wrap(current.getText(), textSize, textWidth);
        for (String line : lines) {
            if (textY + lineHeight > y + height - pad - lineHeight) break;
            guiText.draw(gc, line, x + pad, textY, textSize, Color.BLACK);
            textY += lineHeight;
        }

        //Centered in the whole box, also when a picture takes up the right part
        guiText.draw(gc, "----- PRESS SPACE TO CONTINUE -----", x + width/2, y + height - pad - lineHeight, textSize, Color.GRAY, TextAlignment.CENTER);
    }
}
