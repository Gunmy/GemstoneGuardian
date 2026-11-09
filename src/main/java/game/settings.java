package game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

//Options the player can change in the settings menu. The debug switches (colliders, borders) live in gameHandler.
//Saved to settings.txt next to the highscore whenever something changes, and read back at start
public class settings {

    public enum guiSize {
        SMALL(0.75, "Small"), MEDIUM(1, "Medium"), LARGE(1.3, "Large");

        public final double factor;
        public final String label;

        guiSize (double factor, String label) {
            this.factor = factor;
            this.label = label;
        }

        public guiSize next () {
            return values()[(ordinal()+1) % values().length];
        }
    }

    private guiSize size = guiSize.MEDIUM;

    //The screen width where the medium GUI is drawn at the pixel sizes written in the gui classes.
    //Everything in the GUI scales with the width of the screen relative to this
    private double referenceWidth = 1000;

    //Fluids are either drawn by the shader (pretty, heavy) or by sliding the texture (cheap)
    private boolean simpleFluids = false;

    public settings () {
        load();
    }

    public guiSize getGuiSize () {return this.size;}
    public void cycleGuiSize () {size = size.next(); save();}
    public double getReferenceWidth () {return this.referenceWidth;}

    public boolean simpleFluids () {return this.simpleFluids;}
    public void toggleSimpleFluids () {simpleFluids = !simpleFluids; save();}

    private static File getFile () {
        return new File(new File("src/main/resources/game/reading/").getAbsolutePath(), "settings.txt");
    }

    private void load () {
        File file = getFile();
        if (!file.exists()) return;

        Properties saved = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            saved.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            size = guiSize.valueOf(saved.getProperty("guiSize", size.name()));
        } catch (IllegalArgumentException e) {
            System.out.println("Ukjent guiSize i settings.txt, bruker " + size.name());
        }
        simpleFluids = Boolean.parseBoolean(saved.getProperty("simpleFluids", String.valueOf(simpleFluids)));
    }

    private void save () {
        Properties saved = new Properties();
        saved.setProperty("guiSize", size.name());
        saved.setProperty("simpleFluids", String.valueOf(simpleFluids));

        try (FileOutputStream out = new FileOutputStream(getFile())) {
            saved.store(out, "Gemstone Guardian settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
