package game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

//The daily battles ("Play daily battles" on the main menu, guiDailyMenu): every day the same five opponents for
//everyone, made from the date alone (seeded.random), fought one after the other as a fightRun. Each is a campaign
//character's looks and name with a build of its own (duelBuild.random), playing stronger and wearing more the further
//you get (DEPTHS, TRINKETS; the last one a level above you, LEVELS_ABOVE), at a place by stage: the home village, outside the first castle, the beach, the snow,
//and the girl's own room in her castle. You fight at duel.DEFAULT_LEVEL.
//One try a day: the try is used once its first fight has started (started, from duel.start), not by picking the
//build. After that the menu shows how it went (the run as it was saved after every graded action and every fight,
//so quitting keeps what was done) until the next day, local midnight.
//Kept in saves/daily.txt: the build (picked again the next day), the date of the last try and that try's run
public class daily {

    public static final int STAGES = 5;
    //How many actions ahead each opponent looks (the trained judge; more than 3 takes too long to think), how many
    //trinkets it wears, and how many levels it is above you
    private static final int[] DEPTHS = {1, 2, 3, 3, 3};
    private static final int[] TRINKETS = {0, 1, 1, 2, 2};
    private static final int[] LEVELS_ABOVE = {0, 0, 0, 0, 1};

    //Where each stage is fought: chunks of the map (name, world, chunk x, chunk y), one picked by the day, with how far
    //from its middle the fighters may stand (a room is small)
    private static final Object[][][] PLACES = {
        {{"the village", 0, 10000, 10000}, {"the village", 0, 10001, 10000}, {"the village", 0, 10000, 10001}, {"the village pond", 0, 10001, 10001}},
        {{"the castle", 0, 10000, 9991}, {"the castle", 0, 10001, 9991}, {"the castle", 0, 10000, 9990}},
        {{"the sand", 0, 10002, 9973}, {"the sandcastle", 0, 10003, 9973}, {"the beach", 0, 10002, 9976}, {"the beach", 0, 10006, 9970}, {"the bridge", 0, 10002, 9975}},
        {{"the snowfield", 0, 10009, 9972}, {"the ice pond", 0, 10010, 9975}, {"the snowfield", 0, 10013, 9973}, {"the snowfield", 0, 10009, 9973}, {"the snowfield", 0, 10010, 9973}},
        {{"the girl's room", 2, 10010, 9959}},
    };
    private static final double[] REACH = {1.5, 1.5, 1.5, 1.5, 0.6};
    //If her room has no room: in front of her castle
    private static final Object[][] LAST_FALLBACK = {{"the final castle", 0, 10013, 9963}, {"the final castle", 0, 10014, 9963}};

    private duelBuild build;
    private LocalDate played;
    private fightRun result;

    //The run being fought now (made when Play is pressed), null otherwise
    private fightRun running;

    private static daily shared;
    public static daily shared () {
        if (shared == null) shared = load();
        return shared;
    }

    public static LocalDate today () {return LocalDate.now();}

    //True when today's try is used (or the clock was set back past it)
    public boolean playedToday () {return played != null && !played.isBefore(today());}

    //Seconds until the next day's battles
    public static long secondsLeft () {
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(now, today().plusDays(1).atStartOfDay()).getSeconds();
    }

    //"07:12:33"
    public static String countdown () {
        long s = Math.max(0, secondsLeft());
        return String.format(java.util.Locale.ENGLISH, "%02d:%02d:%02d", s/3600, s%3600/60, s%60);
    }

    public duelBuild getBuild () {return this.build;}
    public fightRun getResult () {return this.result;}
    public LocalDate getPlayed () {return this.played;}

    //The build was edited
    public void buildChanged () {save();}

    //---- The day's run ----

    //Today's five, made from the date only. handler is the menu's game: its campaign characters give the looks and
    //names (duel.choices), its items the builds, its map the places
    public fightRun makeRun (gameHandler handler, String myName, String myLook) {
        long day = today().toEpochDay();
        List<character> pool = new ArrayList<character>(duel.choices(handler));
        java.util.Collections.shuffle(pool, seeded.random(day*16));
        List<fightRun.stage> stages = new ArrayList<fightRun.stage>();
        for (int i = 0; i < STAGES; i++) {
            character who = pool.get(i % pool.size());
            duelBuild theirs = duelBuild.random(handler, seeded.random(day*16 + 1 + i), TRINKETS[i]);
            Random where = seeded.random(day*16 + 8 + i);
            duel.spot place = duel.spotIn(handler, PLACES[i], where, REACH[i]);
            if (place == null && i == STAGES - 1) place = duel.spotIn(handler, LAST_FALLBACK, where, 1.5);
            if (place == null) place = duel.pickSpot(handler);
            stages.add(new fightRun.stage(who.getName(), who.getId(), theirs, place, new aiStrength(true, DEPTHS[i]), LEVELS_ABOVE[i]));
        }
        return new fightRun(stages, duelBuild.decode(build.encode()), myName, myLook, duel.DEFAULT_LEVEL);
    }

    //Play: the run is made and its first fight zooms in (the try is only used once that fight starts, started)
    public void play (gameHandler menu, String myName, String myLook) {
        if (playedToday() || !build.canFight()) return;
        save();
        running = makeRun(menu, myName, myLook).onChange(() -> {
            result = running;
            save();
        });
        menu.duelWithEffect(running.current());
    }

    //The first fight of the run has started: today's try is used, and the run is kept as it goes
    public void started (fightRun run) {
        if (run != running) return;
        played = today();
        result = run;
        save();
    }

    //The run is over or left: the menu shows the result
    public void stop () {running = null;}

    //---- On file ----

    private static File file () {return new File(saveFile.getFolder(), "daily.txt");}

    private static daily load () {
        daily d = new daily();
        Properties data = new Properties();
        File f = file();
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                data.load(in);
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Kunne ikke lese daily.txt: " + e);
            }
        }
        String saved = data.getProperty("build");
        //The first time: the build picked on the 1v1 page
        d.build = saved != null ? duelBuild.decode(saved) : duelBuild.decode(duelBuild.library.shared().current().encode());
        try {
            String day = data.getProperty("played");
            d.played = day == null ? null : LocalDate.parse(day);
        } catch (RuntimeException e) {
            d.played = null;
        }
        d.result = fightRun.decode(data.getProperty("result"));
        return d;
    }

    private void save () {
        Properties data = new Properties();
        data.setProperty("build", build.encode());
        if (played != null) data.setProperty("played", played.toString());
        if (result != null) data.setProperty("result", result.encode());
        File f = file();
        f.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            data.store(out, "Gemstone Guardian daily battles");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
