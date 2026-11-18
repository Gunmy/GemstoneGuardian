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
        SMALL(0.75, "Small"), MEDIUM(1, "Medium"), LARGE(1.15, "Large");

        public final double factor;
        public final String label;

        guiSize (double factor, String label) {
            this.factor = factor;
            this.label = label;
        }

        //The biggest factor of all the sizes, which the screen height is fitted to
        public static double largest () {
            double max = 0;
            for (guiSize size : values()) max = Math.max(max, size.factor);
            return max;
        }

        public guiSize next () {
            return values()[(ordinal()+1) % values().length];
        }
    }

    private guiSize size = guiSize.MEDIUM;

    //The screen width where the medium GUI is drawn at the pixel sizes written in the gui classes.
    //Everything in the GUI scales with the width of the screen relative to this
    private double referenceWidth = 1000;

    //How water, lava, toxic and the crystal floor are drawn (see fluidType.drawSpecial):
    //the shader (two sliding layers of noise), the sheet's own texture slid over (cheapest), or ripples
    //(waves in a few flat colour bands with sparkles, see shader.drawRipples)
    public enum fluidStyle {
        SHADER("Shader"), SIMPLE("Simple"), RIPPLES("Ripples");

        public final String label;

        fluidStyle (String label) {this.label = label;}

        public fluidStyle next () {return values()[(ordinal()+1) % values().length];}
    }

    private fluidStyle fluids = fluidStyle.SHADER;

    public settings () {
        load();
    }

    public guiSize getGuiSize () {return this.size;}
    public void cycleGuiSize () {size = size.next(); save();}
    public double getReferenceWidth () {return this.referenceWidth;}

    //How tall (in the gui classes' pixel sizes) the tallest screen is, with some room around it: the inventory with
    //a chest above it, like the give item menu, is about 590. At the largest GUI size this always fits on the screen
    //(the other sizes are that times their share). Raise it if a taller screen is added
    private double fitHeight = 620;
    public double getFitHeight () {return this.fitHeight;}

    public fluidStyle getFluidStyle () {return this.fluids;}
    public void cycleFluidStyle () {fluids = fluids.next(); save();}

    //How strong the computer plays in random 1v1 battles (the campaigns keep their own, see aiStrength)
    private aiStrength duelAI = aiStrength.DEFAULT;
    public aiStrength getDuelAI () {return this.duelAI;}
    public void setDuelAI (aiStrength ai) {duelAI = ai; save();}

    //The level both fight at in a 1v1 (the host's in one over the network), picked on the 1v1 page ("Battle level")
    private int duelLevel = duel.DEFAULT_LEVEL;
    public int getDuelLevel () {return this.duelLevel;}
    public void setDuelLevel (int level) {duelLevel = Math.max(0, Math.min(duel.MAX_LEVEL, level)); save();}

    //Cheat mode (a hidden secret): in a fight, hovering an action shows how good it is now (battle.getHint). Never
    //in a 1v1 against another person, and a random battle won with it on does not count for the streak

    private boolean cheatMode = false;
    public boolean cheatMode () {return this.cheatMode;}
    public void toggleCheatMode () {cheatMode = !cheatMode; save();}

    private static File getFile () {
        return gameFiles.writable("settings.txt");
    }

    //With no settings.txt yet (the first time the packaged game is started) the defaults are written, so the file is
    //there to find
    private void load () {
        File file = getFile();
        if (!file.exists()) {save(); return;}

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
        //Settings saved before there were three styles only say whether the simple one was on
        if (Boolean.parseBoolean(saved.getProperty("simpleFluids", "false"))) fluids = fluidStyle.SIMPLE;
        try {
            fluids = fluidStyle.valueOf(saved.getProperty("fluids", fluids.name()));
        } catch (IllegalArgumentException e) {
            System.out.println("Ukjent fluids i settings.txt, bruker " + fluids.name());
        }
        try {
            duelAI = new aiStrength(Boolean.parseBoolean(saved.getProperty("duelAITrained", String.valueOf(duelAI.trained))),
                Integer.parseInt(saved.getProperty("duelAIDepth", String.valueOf(duelAI.depth))));
        } catch (NumberFormatException e) {
            System.out.println("Ukjent duelAIDepth i settings.txt, bruker " + duelAI.depth);
        }
        cheatMode = Boolean.parseBoolean(saved.getProperty("cheatMode", "false"));
        try {
            duelLevel = Math.max(0, Math.min(duel.MAX_LEVEL, Integer.parseInt(saved.getProperty("duelLevel", String.valueOf(duel.DEFAULT_LEVEL)))));
        } catch (NumberFormatException e) {
            System.out.println("Ukjent duelLevel i settings.txt, bruker " + duel.DEFAULT_LEVEL);
        }
    }

    private void save () {
        Properties saved = new Properties();
        saved.setProperty("guiSize", size.name());
        saved.setProperty("fluids", fluids.name());
        saved.setProperty("duelAITrained", String.valueOf(duelAI.trained));
        saved.setProperty("duelAIDepth", String.valueOf(duelAI.depth));
        saved.setProperty("cheatMode", String.valueOf(cheatMode));
        saved.setProperty("duelLevel", String.valueOf(duelLevel));

        try (FileOutputStream out = new FileOutputStream(getFile())) {
            saved.store(out, "Gemstone Guardian settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
