package game;

public class teleportEvent implements event {

    private character character;

    public teleportEvent (character character) {
        this.character = character;
    }

    @Override
    public void trigger() {
        character.goToSpawn();       
    }
    
}
