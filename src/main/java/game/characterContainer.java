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

    //The character with this name, or null. Unlike get this compares the text, so names read from a file work too
    public character find (String name) {
        for (character character : characters) {
            if (character.getId().equals(name)) return character;
        }
        return null;
    }

    public character get (String name) {
        for (character character : characters) {
            if (character.getId() == name) {
                return character;
            }
        }
        throw new IllegalArgumentException("Ingen karakter ved dette navnet");
    }



}
