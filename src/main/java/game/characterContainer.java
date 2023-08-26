package game;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class characterContainer {

    private List<character> characters = new ArrayList<character>();

    public void add (character character) {
        characters.add(character);
    }

    public List<character> getCharacters () {
        return this.characters.stream().collect(Collectors.toList());
    }

    public character get (String name) {
        for (character character : characters) {
            if (character.getName() == name) {
                return character;
            }
        }
        throw new IllegalArgumentException("Ingen karakter ved dette navnet");
    }



}
