package game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

//Little icons written into any text as {key} (guiText draws them in the line, as tall as the letters): "took 21 {dmg}
//damage", "-21 {dmg}" thrown off a fighter. Every text in the game goes through guiText, so they work everywhere: the
//speech box, the flying numbers, buttons. A key that is not known is left as it is. Add icons here (a sheet cell and
//the colour it is tinted with, null for the cell's own colours)
public class textIcons {

    public static class icon {
        public final int cellX, cellY;
        public final String tint;

        icon (int cellX, int cellY, String tint) {
            this.cellX = cellX;
            this.cellY = cellY;
            this.tint = tint;
        }

        //In its colours, or (shape not null) only its shape in that colour, for a dark outline round it
        public void draw (GraphicsContext gc, double x, double y, double size, Color shape) {
            if (images == null) return;
            Image img = shape == null ? images.getTinted(cellX, cellY, tint) : images.getSilhouette(cellX, cellY, hex(shape));
            gc.drawImage(img, 0, 0, guiSkin.CELL, guiSkin.CELL, Math.round(x), Math.round(y), Math.round(size), Math.round(size));
        }
    }

    private static final Map<String, icon> ICONS = new LinkedHashMap<String, icon>();
    static {
        add("dmg", abilityType.ATTACK, "#FF4040");
        add("burn", abilityType.FIRE, null);
        add("poison", abilityType.POISON, null);
        add("heal", abilityType.HEAL, null);
        add("confuse", abilityType.CONFUSE, null);
        add("lightning", abilityType.LIGHTNING, null);
        add("drain", abilityType.DRAIN, null);
        add("cleanse", abilityType.CLEANSE, null);
        add("restore", abilityType.RESTORE, null);
        add("attack", abilityType.ATTACK, null);
        ICONS.put("hp", new icon(guiSkin.HEART_X, guiSkin.GUI_Y, null));
        ICONS.put("lvl", new icon(guiSkin.LEVEL_UP_X, guiSkin.ICON_Y, "#60E060"));
    }

    //An ability type's icon, in its own colour unless tint is given
    private static void add (String key, abilityType type, String tint) {
        ICONS.put(key, new icon(type.getCellX(), guiSkin.ICON_Y, tint != null ? tint : type.tint()));
    }

    private static imgDict images;
    //The sheet the icons are cut from: set once the game has its images (gui)
    public static void use (imgDict sheet) {images = sheet;}

    //The key of an ability type's icon, written into a text as {key}
    public static String of (abilityType type) {
        switch (type) {
            case FIRE: return "{burn}";
            case POISON: return "{poison}";
            case HEAL: return "{heal}";
            case CONFUSE: return "{confuse}";
            case LIGHTNING: return "{lightning}";
            case DRAIN: return "{drain}";
            case CLEANSE: return "{cleanse}";
            case RESTORE: return "{restore}";
            default: return "{attack}";
        }
    }

    //The icon for damage done or health healed by what caused it (burn and poison their own, like the number's colour)
    public static String forAmount (boolean heal, picture cause) {
        if (heal) return "{heal}";
        if (cause == abilityType.FIRE) return "{burn}";
        if (cause == abilityType.POISON) return "{poison}";
        return "{dmg}";
    }

    //Whether the text may have icons in it (cheap: most texts have no braces)
    public static boolean in (String text) {return text.indexOf('{') >= 0;}

    //The text cut into plain strings and icons, in order
    public static List<Object> split (String text) {
        List<Object> parts = new ArrayList<Object>();
        StringBuilder plain = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            int close = c == '{' ? text.indexOf('}', i) : -1;
            icon found = close > i ? ICONS.get(text.substring(i + 1, close)) : null;
            if (found != null) {
                if (plain.length() > 0) parts.add(plain.toString());
                plain.setLength(0);
                parts.add(found);
                i = close + 1;
            } else {
                plain.append(c);
                i++;
            }
        }
        if (plain.length() > 0) parts.add(plain.toString());
        return parts;
    }

    private static String hex (Color c) {
        return String.format("#%02X%02X%02X", (int) Math.round(c.getRed()*255), (int) Math.round(c.getGreen()*255), (int) Math.round(c.getBlue()*255));
    }
}
