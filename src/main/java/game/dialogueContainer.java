package game;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TextArea;

public class dialogueContainer {
    private List<dialogue> dialogues = new ArrayList<dialogue>();
    private TextArea dialogueBox;

    public dialogueContainer (TextArea dialogueBox) {
        this.dialogueBox = dialogueBox;
        dialogueBox.setVisible(false);
    }

    public boolean hasNext() {
        return dialogues.size() > 0;
    }
    
    public void next() {
        if (hasNext()) {
            dialogueBox.setVisible(true);

            dialogueBox.setText("");
            dialogueBox.appendText(dialogues.get(0).getNameText() + ":\n");
            dialogueBox.appendText(dialogues.get(0).getText());
            dialogueBox.appendText("\n\n----- PRESS SPACE TO CONTINUE -----");

            dialogues.remove(0);
        } else {
            dialogueBox.setVisible(false);
        }
    }

    public boolean isActive () {
        return dialogueBox.isVisible();
    }

    public void add (dialogue dialog) {
        dialogues.add(dialog);
    }
}
