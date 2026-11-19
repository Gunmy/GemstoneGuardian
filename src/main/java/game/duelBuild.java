package game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

//What a player brings to a 1v1 (see duel): up to four rocks and two trinkets, by item name and the item's own state
//(item.saveState: the rocks inside an Elemental Rock), made with the itemRegistry when the fight starts. Every rock and
//trinket in the game can be picked.
//The player's builds, which one is picked, the name they fight under and their best streak of random battles are kept
//in saves/duel.txt
public class duelBuild {

    public static final int ROCKS = inventory.ACTIVE;
    public static final int TRINKETS = inventory.TRINKETS;

    private String[] rocks = new String[ROCKS];
    private String[] trinkets = new String[TRINKETS];
    //The rock slot that is equipped, NONE for none. Builds saved before this was kept have the first rock equipped
    public static final int NONE = -1, FIRST = -2;
    private int equipped = FIRST;
    public int getEquipped () {return this.equipped;}
    public void setEquipped (int slot) {this.equipped = slot;}

    //Each slot's item.saveState, "" for most items
    private String[] rockStates = new String[ROCKS];
    private String[] trinketStates = new String[TRINKETS];

    public duelBuild () {}
    public String getRock (int i) {return rocks[i];}
    public void setRock (int i, String rock) {rocks[i] = rock; rockStates[i] = null;}
    public String getTrinket (int i) {return trinkets[i];}
    public void setTrinket (int i, String trinket) {trinkets[i] = trinket; trinketStates[i] = null;}

    //The item in a slot as it is now, name and state (null empties it). The build editor takes them out of the inventory
    public void setRock (int i, item rock) {
        rocks[i] = rock == null ? null : rock.getName();
        rockStates[i] = rock == null ? null : rock.saveState();
    }
    public void setTrinket (int i, item worn) {
        trinkets[i] = worn == null ? null : worn.getName();
        trinketStates[i] = worn == null ? null : worn.saveState();
    }

    //The slot's item made fresh, with its state (null if empty or unknown)
    public item makeRock (int i, itemRegistry registry) {return make(rocks[i], rockStates[i], registry);}
    public item makeTrinket (int i, itemRegistry registry) {return make(trinkets[i], trinketStates[i], registry);}

    private static item make (String name, String state, itemRegistry registry) {
        if (name == null) return null;
        item made = registry.make(name);
        if (made != null && state != null && !state.isEmpty()) made.loadState(state);
        return made;
    }

    public boolean contains (String item) {
        for (String r : rocks) if (item.equals(r)) return true;
        for (String t : trinkets) if (item.equals(t)) return true;
        return false;
    }

    //A build needs at least one rock to fight with
    public boolean canFight () {
        for (String r : rocks) if (r != null) return true;
        return false;
    }

    //Gives the character the build: an empty inventory with the rocks in their slots (the first one equipped) and the
    //trinkets worn
    public void giveTo (character c, itemRegistry registry) {
        c.resetInventory();
        for (int i = 0; i < ROCKS; i++) {
            item rock = makeRock(i, registry);
            if (rock instanceof gemstone) c.getInventory().set(inventory.slotKind.ROCK, i, new itemStack(rock));
        }
        for (int i = 0; i < TRINKETS; i++) {
            item worn = makeTrinket(i, registry);
            if (worn instanceof trinket) c.wear((trinket) worn);
        }
        //The equipped slot as it was saved (or the first with a rock, for old builds)
        for (int i = 0; i < ROCKS; i++) {
            itemStack stack = c.getInventory().get(inventory.slotKind.ROCK, i);
            if (stack == null) continue;
            if (equipped == i || equipped == FIRST) {
                c.equipGemstone((gemstone) stack.getItem());
                break;
            }
        }
    }

    //The rocks and trinkets the campaign hands out (as the build editor offers them), by name, once each: the rocks
    //without the Elemental Rock, which would be empty
    public static void handedOut (gameHandler handler, List<String> rockNames, List<String> trinketNames) {
        for (java.util.function.Supplier<item> maker : handler.getItemRegistry().getMakers()) {
            item made = maker.get();
            String name = made.getName();
            if (!handler.isHandedOut(name) || rockNames.contains(name) || trinketNames.contains(name)) continue;
            if (made instanceof gemstone && !(made instanceof elementalRock)) rockNames.add(name);
            else if (made instanceof trinket) trinketNames.add(name);
        }
    }

    //A build at random from the rocks and trinkets the campaign hands out (handedOut): four different rocks, one of
    //them equipped, and up to two trinkets
    public static duelBuild random (gameHandler handler) {
        java.util.Random rdm = new java.util.Random();
        return random(handler, rdm, rdm.nextInt(TRINKETS + 1));
    }

    //The same from rdm (the same rdm gives the same build: the daily battles), with this many trinkets (if there are)
    public static duelBuild random (gameHandler handler, java.util.Random rdm, int trinkets) {
        List<String> rockNames = new ArrayList<String>(), trinketNames = new ArrayList<String>();
        handedOut(handler, rockNames, trinketNames);
        java.util.Collections.shuffle(rockNames, rdm);
        java.util.Collections.shuffle(trinketNames, rdm);
        duelBuild build = new duelBuild();
        int rockCount = Math.min(ROCKS, rockNames.size());
        for (int i = 0; i < rockCount; i++) build.setRock(i, rockNames.get(i));
        if (rockCount == 0) build.setRock(0, "Rock");
        int trinketCount = Math.min(Math.min(TRINKETS, trinkets), trinketNames.size());
        for (int i = 0; i < trinketCount; i++) build.setTrinket(i, trinketNames.get(i));
        build.setEquipped(rdm.nextInt(Math.max(1, rockCount)));
        return build;
    }

    //One line, for the network and the file: the six slots, each name:state in base64 (empty for an empty slot), and the
    //equipped slot, joined by commas. The first field is empty: it held the build's name when builds had names
    public String encode () {
        List<String> parts = new ArrayList<String>();
        parts.add("");
        for (int i = 0; i < ROCKS; i++) parts.add(slot(rocks[i], rockStates[i]));
        for (int i = 0; i < TRINKETS; i++) parts.add(slot(trinkets[i], trinketStates[i]));
        parts.add(String.valueOf(equipped));
        return String.join(",", parts);
    }

    private static String slot (String item, String state) {
        if (item == null) return "";
        return enc(item) + ":" + enc(state == null ? "" : state);
    }

    public static duelBuild decode (String line) {
        String[] parts = line.split(",", -1);
        duelBuild build = new duelBuild();
        for (int i = 0; i < ROCKS; i++) {
            String[] slot = slotOf(parts, 1 + i);
            if (slot != null) {build.rocks[i] = slot[0]; build.rockStates[i] = slot[1];}
        }
        for (int i = 0; i < TRINKETS; i++) {
            String[] slot = slotOf(parts, 1 + ROCKS + i);
            if (slot != null) {build.trinkets[i] = slot[0]; build.trinketStates[i] = slot[1];}
        }
        int last = 1 + ROCKS + TRINKETS;
        if (last < parts.length) {
            try {
                build.equipped = Integer.parseInt(parts[last]);
            } catch (NumberFormatException e) {
                build.equipped = FIRST;
            }
        }
        return build;
    }

    //name and state of a slot (builds saved before states were kept have only the name)
    private static String[] slotOf (String[] parts, int i) {
        if (i >= parts.length || parts[i].isEmpty()) return null;
        String[] two = parts[i].split(":", -1);
        return new String[] {dec(two[0]), two.length > 1 ? dec(two[1]) : ""};
    }

    public static String enc (String s) {return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    public static String dec (String s) {
        try {
            return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    //How many builds a player has: one is used at a time
    public static final int SLOTS = 3;

    //The player's 1v1 setup on disk: their builds, which is picked, and the name they fight under
    public static class library {
        public List<duelBuild> builds = new ArrayList<duelBuild>();
        public int selected = 0;
        public String player = "Player";
        //The campaign character (by id, character.getId) the player fights as: its looks, not its level
        public String character = "Boy";

        //The best streak of random battles won in a row ("Battle random" on the 1v1 page, see duel.countStreak), with
        //the build it was won with and who the player was then (name and look). That build can be picked like the others
        //(selected is BEST) but not edited; a new record replaces it
        public static final int BEST = SLOTS;
        //Picked like a build, but every fight (the first and each Battle again) gets a new duelBuild.random
        public static final int RANDOM = SLOTS + 1;
        public boolean isRandom () {return selected == RANDOM;}
        public int bestStreak = 0;
        public duelBuild bestBuild;
        public String bestPlayer, bestCharacter;

        public duelBuild current () {
            if (selected == BEST && bestBuild != null) return bestBuild;
            return builds.get(Math.max(0, Math.min(selected, builds.size() - 1)));
        }

        //A streak of wins that beats the record: kept with a copy of the build (so editing the build later does not
        //change it) and saved. False if it is no record
        public boolean record (int streak, duelBuild build, String player, String look) {
            if (streak <= bestStreak) return false;
            bestStreak = streak;
            bestBuild = decode(build.encode());
            bestPlayer = player;
            bestCharacter = look;
            save();
            return true;
        }

        public duelBuild get (int slot) {return builds.get(slot);}

        private static File file () {return new File(saveFile.getFolder(), "duel.txt");}

        //Read once and kept for as long as the game runs, so two games on one machine each keep their own picks
        //(they share the file, and would otherwise read each other's)
        private static library shared;
        public static library shared () {
            if (shared == null) shared = load();
            return shared;
        }

        //With one starter build if there are none
        public static library load () {
            library lib = new library();
            Properties data = new Properties();
            File f = file();
            if (f.exists()) {
                try (FileInputStream in = new FileInputStream(f)) {
                    data.load(in);
                } catch (IOException | IllegalArgumentException e) {
                    System.out.println("Kunne ikke lese duel.txt: " + e);
                }
            }
            lib.player = data.getProperty("player", lib.player);
            lib.character = data.getProperty("character", lib.character);
            int count = 0;
            try {
                count = Integer.parseInt(data.getProperty("build.count", "0"));
                lib.selected = Integer.parseInt(data.getProperty("selected", "0"));
            } catch (NumberFormatException e) {
                count = 0;
            }
            for (int i = 0; i < count; i++) {
                String line = data.getProperty("build." + i);
                if (line != null) lib.builds.add(decode(line));
            }
            //Always SLOTS builds, the first with a rock in it so a new player can fight straight away
            while (lib.builds.size() > SLOTS) lib.builds.remove(lib.builds.size() - 1);
            while (lib.builds.size() < SLOTS) {
                duelBuild made = new duelBuild();
                if (lib.builds.isEmpty()) made.setRock(0, "Rock");
                lib.builds.add(made);
            }
            try {
                lib.bestStreak = Integer.parseInt(data.getProperty("best.streak", "0"));
            } catch (NumberFormatException e) {
                lib.bestStreak = 0;
            }
            String best = data.getProperty("best.build");
            if (lib.bestStreak > 0 && best != null) {
                lib.bestBuild = decode(best);
                lib.bestPlayer = data.getProperty("best.player", lib.player);
                lib.bestCharacter = data.getProperty("best.character", lib.character);
            } else {
                lib.bestStreak = 0;
            }
            if (lib.selected != RANDOM) lib.selected = Math.max(0, Math.min(lib.selected, lib.bestBuild != null ? BEST : SLOTS - 1));
            return lib;
        }

        public void save () {
            Properties data = new Properties();
            data.setProperty("player", player);
            data.setProperty("character", character);
            data.setProperty("selected", String.valueOf(selected));
            data.setProperty("build.count", String.valueOf(builds.size()));
            for (int i = 0; i < builds.size(); i++) data.setProperty("build." + i, builds.get(i).encode());
            if (bestBuild != null) {
                data.setProperty("best.streak", String.valueOf(bestStreak));
                data.setProperty("best.build", bestBuild.encode());
                data.setProperty("best.player", bestPlayer == null ? player : bestPlayer);
                data.setProperty("best.character", bestCharacter == null ? character : bestCharacter);
            }
            File f = file();
            f.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(f)) {
                data.store(out, "Gemstone Guardian 1v1 builds");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
