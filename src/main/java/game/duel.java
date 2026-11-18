package game;

import java.util.List;

//A 1v1 against another player on the local network: the campaign's fight, with each player's build (duelBuild), at a
//random place in the world outside where both fit with one tile between them.
//
//The one who made the challenge (the host) runs the fight. The other only shows it: their moves are sent to the host
//(ACT), which plays them as the opponent's turn. Every message the host's fight queues is sent on (DLG) with both
//fighters' state at that moment (character.writeFightState), and the other player puts that state on its copies when
//the message comes on screen, so the health bars and effects move with the messages as in the campaign. TURN tells the
//other player it is their turn, END that the fight is over. Rocks and their randomness only ever run on the host, so
//both see the same fight.
//
//Started from the 1v1 page of the main menu (guiMainMenu), which finds challenges and does the handshake
//(HELLO name build, then START place whoBegins level from the host). Both then build a game for it (gameLauncher.duel)
public class duel {

    //Both fight at the level picked on the 1v1 page (settings.getDuelLevel; over the network the host's), whatever
    //level the character they play as has in the campaign
    public static final int DEFAULT_LEVEL = 13, MAX_LEVEL = 25;
    private final int level;
    public int getLevel () {return this.level;}

    private final duelLink link;
    private final boolean host;
    private final String myName, theirName;
    //The campaign characters (by id) each plays as: the fighter gets its sprite
    private final String myLook, theirLook;
    private final duelBuild myBuild, theirBuild;
    //Where they fight (the host stands there, the guest two tiles to the right), and whether the host moves first
    private final spot place;
    private final boolean hostBegins;

    private gameHandler handler;
    private character me, them;

    private boolean ended = false, leaving = false;
    //Host: a move the other player sent, waiting to be played, and whether they have been told it is their turn
    private String[] pendingAction;
    private boolean turnSent = false;
    //Guest: the host says it is our turn
    private boolean myTurn = false;

    public duel (duelLink link, boolean host, String myName, String myLook, duelBuild myBuild,
                 String theirName, String theirLook, duelBuild theirBuild, spot place, boolean hostBegins, int level) {
        this.level = level;
        this.myLook = myLook;
        this.theirLook = theirLook;
        this.link = link;
        this.host = host;
        this.myName = myName;
        this.myBuild = myBuild;
        this.theirName = theirName;
        this.theirBuild = theirBuild;
        this.place = place;
        this.hostBegins = hostBegins;
    }

    //A 1v1 against the computer ("Battle random" on the 1v1 page): no network, the player is the host and the opponent
    //plays its turns with battleAI like any fighter in the campaign. The player always moves first
    public static duel againstComputer (String myName, String myLook, duelBuild myBuild,
                                        String theirName, String theirLook, duelBuild theirBuild, spot place, int level) {
        return new duel(null, true, myName, myLook, myBuild, theirName, theirLook, theirBuild, place, true, level);
    }

    public boolean isAgainstComputer () {return this.link == null;}

    //Against a random campaign character (not the one you play as) with a random build (duelBuild.random), at a random
    //place. Null if no place has room
    public static duel againstRandom (gameHandler handler, String myName, String myLook, duelBuild myBuild, int level) {
        spot place = pickSpot(handler);
        if (place == null) return null;
        //Who can be fought: taken from the menu's game, where nobody has been dressed up as a fighter yet (in a 1v1
        //the player looks like someone else), and kept for the battles after it
        if (opponents == null || handler.getDuel() == null) {
            opponents = new java.util.ArrayList<String[]>();
            for (character c : choices(handler)) opponents.add(new String[] {c.getId(), c.getName()});
        }
        List<String[]> pool = new java.util.ArrayList<String[]>(opponents);
        if (pool.size() > 1) pool.removeIf(o -> o[0].equals(myLook));
        String[] opponent = pool.get((int) (Math.random()*pool.size()));
        return againstComputer(myName, myLook, myBuild, opponent[1], opponent[0], duelBuild.random(handler), place, level);
    }
    private static List<String[]> opponents;

    //Random battles won in a row before this one. Battle again carries it on (one more after a win, 0 after a loss);
    //going back to the menu ends it, so a battle started from the menu begins at 0
    private int streak = 0;
    public int getStreak () {return this.streak;}

    //The end of a fight against the computer: the streak goes on or is lost (shown on the end screen), and a new best
    //is kept with the build and look it was won with (duelBuild.library.record)
    //A win with cheat mode on (cheated) leaves the streak where it was and is no record; a loss still ends it
    public void countStreak (fightOutcome outcome, boolean cheated) {
        int after = outcome.youWon ? (cheated ? streak : streak + 1) : 0;
        boolean best = outcome.youWon && !cheated && duelBuild.library.shared().record(after, myBuild, myName, myLook);
        outcome.streak(streak, after, best);
        outcome.cheated = cheated;
    }

    //The end of a fight against the computer: battle again with the same build (a new random one if the random build
    //is picked on the 1v1 page), or back to the menu
    public void offerRematch (fightOutcome outcome) {
        outcome.offer(() -> {
            duelBuild build = duelBuild.library.shared().isRandom() ? duelBuild.random(handler) : myBuild;
            duel next = againstRandom(handler, myName, myLook, build, level);
            if (next != null) next.streak = outcome.streakAfter;
            if (next != null) handler.duelWithEffect(next);
            else handler.quitToMenu();
        }, () -> handler.quitToMenu());
    }

    public boolean isHost () {return this.host;}
    public String getTheirName () {return this.theirName;}

    //Where a 1v1 is fought: a place's name, its world and where the host stands
    public static class spot {
        public final String name;
        public final int world;
        public final double x, y;

        spot (String name, int world, double x, double y) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
        }

        //One field for START
        public String encode () {return duelBuild.enc(name) + "," + world + "," + x + "," + y;}

        public static spot decode (String field) {
            String[] parts = field.split(",");
            if (parts.length != 4) return null;
            try {
                return new spot(duelBuild.dec(parts[0]), Integer.parseInt(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    //The places a 1v1 can be fought in, picked by hand: a chunk of the map (by its label in chunks.txt) for each, and
    //the fighters stand at the free tiles nearest its middle. Add places here
    private static final Object[][] PLACES = {
        {"the village", 0, 10000, 10000},
        {"the castle", 0, 10000, 9991},
        {"the beach", 0, 10002, 9976},
        {"the bridge", 0, 10002, 9975},
        {"the ice pond", 0, 10010, 9975},
        {"the snowfield", 0, 10009, 9972},
        {"the castle grounds", 0, 10010, 9945},
        {"the desert", 0, 10010, 9955},
        {"the crystal caverns", 1, 10010, 9950},
        {"the sky ruins", 4, 10010, 9952},
    };
    //How far from a place's middle (in chunks) its free tiles may be
    private static final double PLACE_REACH = 1.5;

    //One of the places, at random, with three free tiles in a row near its middle: the host, an empty tile, the guest.
    //Free: not solid, nothing happens when stepped on, not a chest, nobody standing there. Null if no place has room
    public static spot pickSpot (gameHandler handler) {
        List<Object[]> places = new java.util.ArrayList<Object[]>(java.util.Arrays.asList(PLACES));
        java.util.Collections.shuffle(places);
        for (Object[] place : places) {
            spot found = freeNear(handler, (String) place[0], (Integer) place[1], (Integer) place[2] + 0.5, (Integer) place[3] + 0.5);
            if (found != null) return found;
        }
        return null;
    }

    private static spot freeNear (gameHandler handler, String name, int worldId, double cx, double cy) {
        chunks world = handler.getWorld(worldId);
        if (world == null) return null;
        double t = 1.0/handler.getTileCount();
        List<tile> candidates = world.findTiles(type -> free(type));
        candidates.removeIf(c -> Math.hypot(c.getRealX() - cx, c.getRealY() - cy) > PLACE_REACH);
        candidates.sort(java.util.Comparator.comparingDouble(c -> Math.hypot(c.getRealX() + t - cx, c.getRealY() - cy)));
        for (tile first : candidates) {
            boolean ok = true;
            for (int k = 0; k < 3 && ok; k++) {
                tile at = world.getTile(first.getRealX() + k*t, first.getRealY());
                ok = free(at.getTileType()) && nobodyOn(handler, worldId, at);
            }
            if (ok) return new spot(name, worldId, first.getRealX(), first.getRealY());
        }
        return null;
    }

    private static boolean free (tileType type) {
        return !type.isBlocked() && !type.hasFunction() && !type.isContainer();
    }

    private static boolean nobodyOn (gameHandler handler, int world, tile at) {
        for (character c : handler.getCharacters()) {
            if (c.getWorld() == world && !c.isGone() && (c.getStandingTile() == at || c.getTargetTile() == at)) return false;
        }
        return true;
    }

    //Sets the fight up in the game built for it (from the gameHandler constructor)
    public void start (gameHandler handler) {
        this.handler = handler;
        double t = 1.0/handler.getTileCount();
        double myX = host ? place.x : place.x + 2*t, theirX = host ? place.x + 2*t : place.x;

        me = handler.getMainCharacter();
        me.setDisplayName(myName);
        me.placeAt(place.world, myX, place.y);
        look(me, myLook);

        them = new character("1v1 opponent", theirX, place.y, me.getImgX(), me.getImgY(), guiSkin.CELL, guiSkin.CELL, place.world, handler, 3);
        them.setDisplayName(theirName);
        look(them, theirLook);
        handler.addCharacter(them);

        prepare(me, myBuild);
        prepare(them, theirBuild);

        handler.getCamera().setX(me.getX() + 0.5*t);
        handler.getCamera().setY(me.getY() + 0.5*t);

        battle fight = handler.getBattle();
        fight.setDuel(this);
        fight.newBattle(me, them, null, null, place.x, place.y, 0, place.world);
        if (host) {
            if (link != null) handler.getDiealoguecontainer().setListener(this::forward);
            //The fight's own turn order has the player (the host) first
            fight.resumeTurn(hostBegins);
            String first = hostBegins ? myName : theirName;
            handler.getDiealoguecontainer().add(new dialogue("Game", myName + " vs " + theirName + " at " + place.name + ". " + first + " begins"));
        }
    }

    //Gives the fighter the sprite of the campaign character with that id (unchanged if there is none)
    private void look (character fighter, String id) {
        character model = handler.getCharacters().stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
        if (model != null) fighter.setSprite(model.getImgX(), model.getImgY());
    }

    //The campaign characters a player can fight as, one per look (the three guards look the same), in the order the
    //campaign makes them
    public static List<character> choices (gameHandler handler) {
        List<character> list = new java.util.ArrayList<character>();
        for (character c : handler.getCharacters()) {
            boolean seen = false;
            for (character other : list) if (other.looksLike(c.getImgX(), c.getImgY())) seen = true;
            if (!seen) list.add(c);
        }
        return list;
    }

    private void prepare (character c, duelBuild build) {
        c.setLevel(level);
        build.giveTo(c, handler.getItemRegistry());
        c.clearEffects();
        c.resetHealth();
        c.syncShownHealth();
    }

    //Every frame: takes what has come over the network, and leaves once the fight is over and its messages are read
    public void next () {
        String[] m;
        while (link != null && (m = link.poll()) != null) {
            if (host && m[0].equals("ACT") && m.length >= 3) pendingAction = m;
            else if (!host && m[0].equals("DLG")) received(m);
            else if (!host && m[0].equals("TURN")) myTurn = true;
            else if (!host && m[0].equals("END")) ended = true;
            else if (!host && m[0].equals("OUT") && m.length >= 3) outcome(m);
        }
        if (!ended && link != null && link.isClosed()) {
            handler.getDiealoguecontainer().add(new dialogue("Game", theirName + " left the 1v1"));
            ended = true;
            handler.getBattle().endDuel();
        }
        dialogueContainer messages = handler.getDiealoguecontainer();
        if (ended && !leaving && !messages.isActive() && !messages.hasNext()) {
            leaving = true;
            handler.quitToMenu();
        }
    }

    public void close () {if (link != null) link.close();}

    //---- The host's side ----

    //Sends a message the fight queued on, with both fighters' state as it is now
    private void forward (dialogue d) {
        //The end of the fight: who won and the numbers, for the other player's own winner screen
        if (d instanceof fightOutcome) {
            fightOutcome o = (fightOutcome) d;
            link.send("OUT", o.youWon ? "H" : "G", o.numbers.encode());
            return;
        }
        String speaker = d.getSpeaker() == me ? "H" : d.getSpeaker() == them ? "G" : "N";
        String picture = "";
        if (d.getOwnPicture() instanceof abilityType) picture = "A" + ((abilityType) d.getOwnPicture()).name();
        else if (d.getOwnPicture() instanceof item) picture = "I" + ((item) d.getOwnPicture()).getName();
        saveFile state = saveFile.inMemory();
        me.writeFightState(state, "h.");
        them.writeFightState(state, "g.");
        String name = d.getSpeakerName() == null ? "" : d.getSpeakerName();
        //Its animations: key, user and target (H or G) each, joined
        java.util.List<String> animations = new java.util.ArrayList<String>();
        for (battleAnimation.playing a : d.getAnimations()) {
            animations.add(duelBuild.enc(a.getKey()) + ";" + side(a.getUser()) + ";" + side(a.getTarget()));
        }
        link.send("DLG", speaker, duelBuild.enc(name), duelBuild.enc(d.getText()), d.getBorder() == null ? "" : d.getBorder(),
            duelBuild.enc(picture), duelBuild.enc(state.toText()), String.join(",", animations));
    }

    private String side (character c) {return c == me ? "H" : "G";}

    //The other player's move, if one has come (taken: it is played once)
    public String[] takeAction () {
        String[] action = pendingAction;
        pendingAction = null;
        return action;
    }

    //The fight waits for the other player: tells them, once
    public void waitingForThem () {
        if (!turnSent && link != null) link.send("TURN");
        turnSent = true;
    }

    public void theirMoveDone () {turnSent = false;}

    //The fight is over (its last message is already sent)
    public void finish () {
        if (link != null) link.send("END");
        ended = true;
    }

    //---- The other player's side ----

    public boolean isMyTurn () {return this.myTurn;}

    //Our move, for the host to play. kind is ability (n = 0 to 2), rock (n = the rock slot) or giveup
    public void sendAction (String kind, int n) {
        link.send("ACT", kind, String.valueOf(n));
        myTurn = false;
    }

    //OUT winner stats from the host: the hit that ended it in slow motion, then the screen, seen from here
    private void outcome (String[] m) {
        dialogue last = handler.getDiealoguecontainer().last();
        if (last != null) last.slowMotion();
        fightOutcome.stats numbers = fightOutcome.stats.decode(m[2]).swapped();
        handler.getDiealoguecontainer().add(fightOutcome.duel(me, them, m[1].equals("G"), numbers));
    }

    private void received (String[] m) {
        if (m.length < 7) return;
        character speaker = m[1].equals("H") ? them : m[1].equals("G") ? me : null;
        String text = duelBuild.dec(m[3]);
        dialogue d = speaker != null ? new dialogue(speaker, text) : new dialogue(duelBuild.dec(m[2]), text);
        String pictured = duelBuild.dec(m[5]);
        picture shown = null;
        if (pictured.startsWith("A")) {
            try {
                shown = abilityType.valueOf(pictured.substring(1));
            } catch (IllegalArgumentException e) {
                shown = null;
            }
        } else if (pictured.startsWith("I")) {
            shown = handler.getItemRegistry().make(pictured.substring(1));
        }
        if (!m[4].isEmpty() || shown != null) d.highlight(m[4].isEmpty() ? null : m[4], shown);
        if (m.length >= 8 && !m[7].isEmpty()) {
            for (String one : m[7].split(",")) {
                String[] f = one.split(";");
                if (f.length != 3) continue;
                String key = duelBuild.dec(f[0]);
                battleAnimation played = battleAnimation.fromKey(key, handler.getItemRegistry());
                //H is the host, the other fighter here
                character user = f[1].equals("H") ? them : me, target = f[2].equals("H") ? them : me;
                if (played != null) d.animate(new battleAnimation.playing(played, user, target, key));
            }
        }
        saveFile state = saveFile.fromText(duelBuild.dec(m[6]));
        d.onShow(() -> {
            me.readFightState(state, "g.", handler.getItemRegistry());
            them.readFightState(state, "h.", handler.getItemRegistry());
        });
        handler.getDiealoguecontainer().add(d);
    }

    //The rock slot an item is in, for sendAction
    public static int rockSlot (character c, item rock) {
        for (int i = 0; i < inventory.ACTIVE; i++) {
            itemStack stack = c.getInventory().get(inventory.slotKind.ROCK, i);
            if (stack != null && stack.getItem() == rock) return i;
        }
        return -1;
    }

}
