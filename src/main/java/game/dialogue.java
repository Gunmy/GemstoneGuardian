package game;

public class dialogue {
    private String name;
    private String text;
    private character c;

    public dialogue (character character, String text) {
        this.c = character;
        this.text = text;
    }

    public dialogue (String name, String text) {
        this.name = name;
        this.text = text;
    }

    public String getText () {
        return text;
    }

    public String getNameText () {
        if (c != null) {
            if (c.getLevel() == 0) {
                return c.getName();
            }
            return c.getName() + " (Lvl " + c.getLevel() + ")";
        }
        return name;
    }
}
