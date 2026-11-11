package game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

//One campaign's progress on disk: a key = value file in saves/ (next to the project, git-ignored). The map is not in it,
//it always comes from the campaign file (chunks.txt), so a save is only what has changed: characters, inventories,
//which events have happened, an ongoing fight and the dialogue on screen. saveState fills and reads it.
//The header (name, level, play time, where the player is) is read by the savefile list without loading the game
public class saveFile {

    public static final int VERSION = 1;

    private File file;
    private Properties data = new Properties();

    private saveFile (File file) {
        this.file = file;
    }

    public static File getFolder () {
        return new File("saves").getAbsoluteFile();
    }

    //Every savefile, the one played last first. Files that can not be read are skipped
    public static List<saveFile> list () {
        List<saveFile> saves = new ArrayList<saveFile>();
        File[] files = getFolder().listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null) return saves;
        for (File f : files) {
            saveFile save = new saveFile(f);
            if (save.read()) saves.add(save);
        }
        saves.sort(Comparator.comparingLong(saveFile::getLastPlayed).reversed());
        return saves;
    }

    //A new, empty campaign. It is written the first time the game saves it
    public static saveFile create (String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Navnet kan ikke være tomt");
        File folder = getFolder();
        File f = new File(folder, "save_" + System.currentTimeMillis() + ".txt");
        for (int n = 1; f.exists(); n++) f = new File(folder, "save_" + System.currentTimeMillis() + "_" + n + ".txt");
        saveFile save = new saveFile(f);
        save.set("name", name.trim());
        save.set("created", System.currentTimeMillis());
        return save;
    }

    private boolean read () {
        try (FileInputStream in = new FileInputStream(file)) {
            data.load(in);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Kunne ikke lese " + file.getName() + ": " + e);
            return false;
        }
    }

    //Written to a temporary file first and moved over the old one, so a crash while saving never leaves half a save
    public void write () {
        set("version", VERSION);
        set("lastPlayed", System.currentTimeMillis());
        file.getParentFile().mkdirs();
        File temp = new File(file.getParentFile(), file.getName() + ".part");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            data.store(out, "Gemstone Guardian save");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void delete () {
        if (file.exists() && !file.delete()) System.out.println("Kunne ikke slette " + file.getName());
    }

    //Everything is thrown away before a new save is filled in, except the header that is not game state
    public void clearState () {
        String name = getName();
        long created = getLong("created", System.currentTimeMillis());
        data.clear();
        set("name", name);
        set("created", created);
    }

    public boolean exists () {return file.exists();}

    //The header
    public String getName () {return getString("name", file.getName());}
    public long getLastPlayed () {return getLong("lastPlayed", file.lastModified());}
    public double getPlayTime () {return getDouble("playTime", 0);}
    public int getLevel () {return getInt("player.level", 0);}
    public int getPlayerWorld () {return getInt("player.world", 0);}
    public double getPlayerX () {return getDouble("player.x", 10000);}
    public double getPlayerY () {return getDouble("player.y", 10000);}
    public boolean hasPlayer () {return has("player.world");}

    //Values. Doubles are written with Double.toString so they read back exactly
    public void set (String key, Object value) {data.setProperty(key, String.valueOf(value));}
    public boolean has (String key) {return data.containsKey(key);}

    public String getString (String key, String fallback) {return data.getProperty(key, fallback);}

    public double getDouble (String key, double fallback) {
        try {
            return has(key) ? Double.parseDouble(data.getProperty(key)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public int getInt (String key, int fallback) {
        try {
            return has(key) ? Integer.parseInt(data.getProperty(key)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public long getLong (String key, long fallback) {
        try {
            return has(key) ? Long.parseLong(data.getProperty(key)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBool (String key, boolean fallback) {
        return has(key) ? Boolean.parseBoolean(data.getProperty(key)) : fallback;
    }

    //Lists are stored as one value split by the separator. An empty value is an empty list
    public void setList (String key, List<String> values, String separator) {
        set(key, String.join(separator, values));
    }

    public List<String> getList (String key, String separator) {
        List<String> values = new ArrayList<String>();
        String raw = getString(key, "");
        if (raw.isEmpty()) return values;
        for (String v : raw.split(java.util.regex.Pattern.quote(separator), -1)) values.add(v);
        return values;
    }
}
