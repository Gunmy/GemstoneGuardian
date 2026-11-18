package game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

//Where the game's files are, decided in one place. Run from the project folder (development, recognised by
//src/main/resources/game/reading/chunks.txt being there) everything stays where it has always been: the map, highscore
//and settings in src/main/resources/game/reading/ and the savefiles in saves/, so N picks up edits to chunks.txt and the
//saves already made are the ones used. Run from anywhere else (the packaged Windows version) the map is read from
//inside the game itself, and everything the game writes goes in a folder of the player's own, %APPDATA%\GemstoneGuardian
//(the home folder if there is no APPDATA), made the first time with the highscore that comes with the game copied in
public class gameFiles {

    private static final String READING = "src/main/resources/game/reading/";
    private static final String RESOURCES = "/game/reading/";

    //The files the game writes that start as the copy packaged with it. settings.txt is not one: settings writes its
    //own defaults when it finds none
    private static final String[] DEFAULTS = {"highscore.txt"};

    private static final boolean DEVELOPMENT = new File(READING, "chunks.txt").exists();

    static {
        if (!DEVELOPMENT) prepareUserFolder();
    }

    public static boolean isDevelopment () {return DEVELOPMENT;}

    //A file the game only reads, such as chunks.txt. From the project folder it is read from disk every time, so edits
    //show up without rebuilding
    public static BufferedReader read (String name) throws IOException {
        if (DEVELOPMENT) return new BufferedReader(new FileReader(new File(READING, name).getAbsoluteFile()));
        InputStream in = gameFiles.class.getResourceAsStream(RESOURCES + name);
        if (in == null) throw new IOException("Finner ikke " + name + " i spillet");
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    //A file the game writes, such as highscore.txt and settings.txt
    public static File writable (String name) {
        if (DEVELOPMENT) return new File(READING, name).getAbsoluteFile();
        return new File(userFolder(), name);
    }

    //The folder the savefiles (and the 1v1 build, duel.txt) are kept in
    public static File savesFolder () {
        if (DEVELOPMENT) return new File("saves").getAbsoluteFile();
        return new File(userFolder(), "saves");
    }

    private static File userFolder () {
        String appData = System.getenv("APPDATA");
        File base = (appData == null || appData.isEmpty()) ? new File(System.getProperty("user.home")) : new File(appData);
        return new File(base, "GemstoneGuardian");
    }

    //Makes the player's folder and copies in the packaged defaults it does not have yet. A failure is only printed: the
    //game can still be played, it just can not keep what it writes
    private static void prepareUserFolder () {
        File folder = userFolder();
        new File(folder, "saves").mkdirs();
        for (String name : DEFAULTS) {
            File file = new File(folder, name);
            if (file.exists()) continue;
            try (InputStream in = gameFiles.class.getResourceAsStream(RESOURCES + name)) {
                if (in != null) Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
