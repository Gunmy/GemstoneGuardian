package game;

public class dialogue {
    private String name;
    private String text;
    private character c;

    //Special messages get a coloured border and a picture of what the message is about
    //(the item you got, the ability or pad that hurt or healed you...) drawn in the right part of the box
    public static final String ITEM_BORDER = "#E0C040";
    public static final String DAMAGE_BORDER = "#E04040";
    public static final String LEVEL_BORDER = "#40C040";
    private String border;
    private picture picture;

    public dialogue (character character, String text) {
        this.c = character;
        this.text = text;
    }

    public dialogue (String name, String text) {
        this.name = name;
        this.text = text;
    }

    public dialogue highlight (String border, picture picture) {
        this.border = border;
        this.picture = picture;
        return this;
    }

    public String getBorder () {return this.border;}

    //Whoever is talking is shown unless the message has its own picture
    public picture getPicture () {
        if (picture == null && c != null) return c;
        return this.picture;
    }

    public boolean isSpecial () {return border != null || getPicture() != null;}

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
