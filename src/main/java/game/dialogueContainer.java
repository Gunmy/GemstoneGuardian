package game;

import java.util.ArrayList;
import java.util.List;

//The queue of things said. The one being shown is drawn by guiDialogue
public class dialogueContainer {
    private List<dialogue> dialogues = new ArrayList<dialogue>();
    private dialogue current;

    //What is hurting or healing right now (an ability, an item, a pad...). Whoever applies damage
    //or healing sets it around the call, and the damage and heal messages pick it up as their picture
    private picture source;

    public void setSource (picture source) {this.source = source;}
    public void clearSource () {this.source = null;}
    public picture getSource () {return this.source;}

    public boolean hasNext() {
        return dialogues.size() > 0;
    }

    public void next() {
        if (hasNext()) {
            current = dialogues.remove(0);
        } else {
            current = null;
        }
    }

    public boolean isActive () {
        return current != null;
    }

    public dialogue getCurrent () {
        return this.current;
    }

    public void add (dialogue dialog) {
        dialogues.add(dialog);
    }
}
