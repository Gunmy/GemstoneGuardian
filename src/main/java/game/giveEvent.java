package game;

public class giveEvent implements event {

    private character targetCharacter;
    private item giveItem;
    private dialogueContainer dialogueContainer;
    
    public giveEvent(character targetCharacter, item giveItem, dialogueContainer dialogueContainer) {
        this.giveItem = giveItem;
        this.targetCharacter = targetCharacter;
        this.dialogueContainer = dialogueContainer;
    }


    @Override
    public void trigger() {
        targetCharacter.give(giveItem);
        dialogueContainer.add(new dialogue("Game", "You picked up " + giveItem.getName()));
        targetCharacter.restore();
    }
    
}
