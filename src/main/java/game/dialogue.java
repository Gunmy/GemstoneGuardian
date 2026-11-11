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

    //Run once when the message comes on screen, so what it describes (a health bar dropping...) happens then
    private Runnable onShow;

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

    public dialogue onShow (Runnable onShow) {
        this.onShow = onShow;
        return this;
    }

    //One message made of several, said by whoever said the first. The lines follow each other, the first border and
    //picture given with highlight are kept (the damage message's ability icon), and everything they do when shown is run
    public static dialogue combine (java.util.List<dialogue> parts) {
        if (parts.size() == 1) return parts.get(0);
        dialogue first = parts.get(0);
        StringBuilder text = new StringBuilder();
        for (dialogue d : parts) {
            if (text.length() > 0) text.append("\n");
            text.append(d.text);
        }
        dialogue combined = first.c != null ? new dialogue(first.c, text.toString()) : new dialogue(first.name, text.toString());
        for (dialogue d : parts) {
            if (combined.border == null) combined.border = d.border;
            if (combined.picture == null) combined.picture = d.picture;
        }
        java.util.List<Runnable> shows = new java.util.ArrayList<Runnable>();
        for (dialogue d : parts) if (d.onShow != null) shows.add(d.onShow);
        if (!shows.isEmpty()) combined.onShow = () -> shows.forEach(Runnable::run);
        return combined;
    }

    public void shown () {
        if (onShow != null) onShow.run();
    }

    public String getBorder () {return this.border;}

    //For savefiles: who talks (a character or just a name) and the picture given with highlight
    public character getSpeaker () {return this.c;}
    public String getSpeakerName () {return this.name;}
    public picture getOwnPicture () {return this.picture;}

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
