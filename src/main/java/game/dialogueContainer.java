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
            current.shown();
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
        if (groupDepth > 0) group.add(dialog);
        else dialogues.add(dialog);
    }

    //Everything added between beginGroup and endGroup becomes one message (see dialogue.combine), so one action
    //(a move with its damage and effects) is read in one go. Groups can be nested, the outermost one decides
    private List<dialogue> group = new ArrayList<dialogue>();
    private int groupDepth = 0;

    public void beginGroup () {groupDepth++;}

    public void endGroup () {
        if (groupDepth == 0) return;
        groupDepth--;
        if (groupDepth > 0 || group.isEmpty()) return;
        dialogues.add(dialogue.combine(group));
        group = new ArrayList<dialogue>();
    }

    //Runs action as one group
    public void grouped (Runnable action) {
        beginGroup();
        try {action.run();}
        finally {endGroup();}
    }

    //The message on screen and the ones waiting, as dialogue.N.speaker / .name / .text / .border / .item.
    //Only items survive as pictures; what a message does when shown (moving a health bar) is not saved,
    //the health bars start at the real health after loading instead
    public void saveTo (saveFile save) {
        List<dialogue> all = new ArrayList<dialogue>();
        if (current != null) all.add(current);
        all.addAll(dialogues);
        save.set("dialogue.count", all.size());
        for (int i = 0; i < all.size(); i++) {
            dialogue d = all.get(i);
            String p = "dialogue." + i + ".";
            if (d.getSpeaker() != null) save.set(p + "speaker", d.getSpeaker().getId());
            else if (d.getSpeakerName() != null) save.set(p + "name", d.getSpeakerName());
            save.set(p + "text", d.getText());
            if (d.getBorder() != null) save.set(p + "border", d.getBorder());
            if (d.getOwnPicture() instanceof item) save.set(p + "item", ((item) d.getOwnPicture()).getName());
        }
    }

    //Queues the saved messages, the one that was on screen first
    public void loadFrom (saveFile save, characterContainer characters, itemRegistry registry) {
        dialogues.clear();
        current = null;
        int count = save.getInt("dialogue.count", 0);
        for (int i = 0; i < count; i++) {
            String p = "dialogue." + i + ".";
            character speaker = save.has(p + "speaker") ? characters.find(save.getString(p + "speaker", "")) : null;
            String text = save.getString(p + "text", "");
            dialogue d = speaker != null ? new dialogue(speaker, text) : new dialogue(save.getString(p + "name", "Game"), text);
            item pictured = save.has(p + "item") ? registry.make(save.getString(p + "item", "")) : null;
            if (save.has(p + "border") || pictured != null) d.highlight(save.getString(p + "border", null), pictured);
            dialogues.add(d);
        }
    }
}
