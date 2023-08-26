package game;

import java.util.function.BiConsumer;

public class item {

    protected String name;
    protected BiConsumer<character, character> useFunct;
    protected boolean oneUse;
    protected int useCount;
    protected gameHandler handler;

    public item (String name, BiConsumer<character, character> useFunct, boolean oneUse, gameHandler handler) {
        this.name = name;
        this.useFunct = useFunct;
        this.oneUse = oneUse;
        this.handler = handler;
    }

    //I tilfelle jeg utvider programmet og legger til flere funksjoner, som consumables
    public boolean isUsedUp () {
        return oneUse && (useCount > 0);
    }

    public void buttonUse () {
        character you = handler.getBattle().getTurnHaver();
        character them = handler.getBattle().getNotTurnHaver();
        use(you, them);
        handler.getBattle().itemChosen();
    }

    protected void dialogueAdd(character you) {
        handler.getDiealoguecontainer().add(new dialogue(you, "Used " + getName()));
    }

    public void use (character character, character oppositeCharacter) {
        if (character == oppositeCharacter) throw new IllegalArgumentException("use må ta inn to ulike karakterer");
        useFunct.accept(character, oppositeCharacter);
        useCount += 1;
        dialogueAdd(character);
    }

    public String getName () {
        return this.name;
    }

    public void restore() {}
}
