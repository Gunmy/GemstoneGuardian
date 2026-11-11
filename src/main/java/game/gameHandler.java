package game;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class gameHandler {

    private double unitWidth = 0.1;
    private double unitHeight = 0.1;

    public double getUnitWidth () {return unitWidth*effectiveZoom()*getWIDTH();}
    public double getUnitHeight () {return unitHeight*effectiveZoom()*getWIDTH();}

    //The scroll zoom, with a teleport's zoom out on top. The teleport never takes it past the furthest
    //you can zoom out yourself (see teleportWithEffect), since drawing that many more tiles makes the game lag
    private double effectiveZoom () {
        return zoom*(transition == null ? 1 : transition.zoomFactor());
    }

    //Zoom, on top of the size the current world is drawn in. Changed with the scrollwheel
    private double zoom = 1;
    private double zoomTarget = 1;
    private double minZoom = 0.2;
    private double maxZoom = 2;

    public void zoom (double scroll) {
        if (battleHandler.isActive() || inMenu()) return;
        //One notch on a mouse wheel is 40
        zoomTarget = Math.max(minZoom, Math.min(maxZoom, zoomTarget*Math.pow(1.1, scroll/40)));
    }

    //The size the fade to the background is measured in. In a dark world the light reaches
    //equally many tiles no matter the zoom, otherwise it always covers the same part of the screen
    public double getFadeUnit () {
        if (worlds.get(getCurrentWorld()).isDark()) return getUnitHeight();
        return unitHeight*getWIDTH();
    }

    private void updateWidthHeight () {
        zoom += ((battleHandler.isActive() ? 1 : zoomTarget)-zoom)*0.2;

        double var = 0.05;
        if (battleHandler.isActive()) {
            unitWidth += (1-unitWidth)*var;
            unitHeight += (1-unitHeight)*var;
        } else {
        unitWidth += (worlds.get(getCurrentWorld()).getUnitWidthHeight().get(0)-unitWidth)*var;
        unitHeight += (worlds.get(getCurrentWorld()).getUnitWidthHeight().get(1)-unitHeight)*var;
        }
        
    }

    private int tileCount = 9;
    public int getTileCount () {return tileCount;}

    private tileTypes tileTypes;
    public tileTypes getTileTypes () {return tileTypes;}

    public double getWIDTH () {return canvas.getWidth();}
    public double getHEIGHT () {return canvas.getHeight();}
    public double getMidWIDTH () {return getWIDTH()/2;}
    public double getMidHEIGHT () {return getHEIGHT()/2;}

    private camera camera;
    public camera getCamera () {return camera;}

    private RCanvas canvas;
    private RCanvas tempCanvas;
    public RCanvas getCanvas () {return canvas;}
    public RCanvas getTempCanvas () {return tempCanvas;}
    public GraphicsContext getTempGc () {return getTempCanvas().getGraphicsContext2D();}

    private imgDict images;
    public imgDict getImages() {return images;}

    private int world = 0;
    public int getCurrentWorld () {return this.world;}

    //The open sky of sky worlds, with its clouds
    private skyLayer sky = new skyLayer(this);
    public skyLayer getSkyLayer () {return this.sky;}
    //null for a world there is not (a savefile can name one that has been taken out)
    public chunks getWorld (int n) {return n >= 0 && n < worlds.size() ? this.worlds.get(n) : null;}
    public int getWorldCount () {return this.worlds.size();}
    private List<chunks> worlds = Arrays.asList(
        new chunks("Outside", 24, this, 1.1, "#467764", 0.6, 0.6), //outside
        new chunks("Underground", 17, this, 0.3, "#000", 1, 1), //underground
        new chunks("Inside, 1st floor", 17, this, 1.1, "#000", 0.9, 0.9), //inside, 1st floor
        new chunks("Inside, 2nd floor", 17, this, 1.1, "#000", 0.9, 0.9), //inside, 2nd floor
        //After the final boss: the sky ruins, islands of snow in the open sky, reached by sky portals
        new chunks("Sky ruins", 184, this, 1.1, skyLayer.SKY, 0.6, 0.6).sky(), //4
        //Behind the mirrors: the starting village mirrored (tools/maps_postgame.py), cast in cold blues
        new chunks("Mirror dimension", 24, this, 1.1, "#C8E4F4", 0.6, 0.6).mirror() //5
    ); 

    //The worlds from the bottom floor to the top: underground, outside, inside 1st floor, inside 2nd floor.
    //Used by the floor cheat in the hidden secrets. A new world goes in here where it belongs
    public static final int[] FLOOR_ORDER = {1, 0, 2, 3};

    public int floorOf (int world) {
        for (int i = 0; i < FLOOR_ORDER.length; i++) if (FLOOR_ORDER[i] == world) return i;
        return -1;
    }

    //Moves the player by floors (+1 up, -1 down) at the same spot, straight away, with the view following. The
    //tile landed on runs its pressure plate as usual. False when there is no floor that way
    public boolean changeFloor (int by) {
        character player = getMainCharacter();
        int to = floorOf(player.getWorld()) + by;
        if (to < 0 || to >= FLOOR_ORDER.length || transition.isActive() || battleHandler.isActive()) return false;
        player.teleport(FLOOR_ORDER[to], player.getX(), player.getY());
        jumpToPlayer();
        return true;
    }

    //Movement
    private int wPressed, sPressed, aPressed, dPressed;
    public void Wpress () {wPressed = 1;}
    public void Spress () {sPressed = 1;}
    public void Apress () {aPressed = 1;}
    public void Dpress () {dPressed = 1;}
    public void Wrelease () {wPressed = 0;}
    public void Srelease () {sPressed = 0;}
    public void Arelease () {aPressed = 0;}
    public void Drelease () {dPressed = 0;}

    //Debug and reload
    private boolean info = false;
    public void Mpress () {info = info == false;}
    public boolean infoOn () {return this.info;}
    public void Npress () {if (infoOn()) readChunks.read(this);}

    //Colliders off lets every character walk through tiles and each other
    private boolean colliders = true;
    public void Cpress () {colliders = colliders == false;}
    public boolean collidersOn () {return this.colliders;}

    //Darkness off: dark worlds (the caves, render distance under 1) are lit like the rest (see chunks.getRenderDistance)
    private boolean darknessOff = false;
    public void Hpress () {darknessOff = !darknessOff;}
    public boolean darknessOff () {return this.darknessOff;}

    public character getMainCharacter () {return characters.get("Boy");}

    private battle battleHandler;;
    public battle getBattle () {return this.battleHandler;}

    private SnapshotParameters params = new SnapshotParameters();

    private gui gui;
    public gui getGui () {return this.gui;}

    private settings settings = new settings();
    public settings getSettings () {return this.settings;}

    private items itemDict;

    //Items handed out by teleport targets (healing pads) to whoever steps on them without carrying one already
    private java.util.Map<worldTile, java.util.function.Supplier<item>> teleportRewards = new java.util.HashMap<worldTile, java.util.function.Supplier<item>>();

    public void addTeleportReward (int world, double x, double y, java.util.function.Supplier<item> reward) {
        tile target = getWorld(world).getTile(x, y);
        if (!target.getTileType().isTeleportTarget()) throw new IllegalArgumentException("Tilen er ikke et teleport-mål");
        teleportRewards.put(new worldTile(world, target), reward);
    }

    public void teleportTargetVisited (character c, tile target) {
        if (c != getMainCharacter()) return;
        java.util.function.Supplier<item> reward = teleportRewards.get(new worldTile(c.getWorld(), target));
        if (reward == null) return;
        item given = reward.get();
        //Pads run every frame you stand on them, so this is what keeps it to one. Held on the cursor counts as carried
        itemStack held = gui == null ? null : gui.getCursor();
        if (c.carries(given.getName()) || (held != null && held.getItem().getName().equals(given.getName()))) return;
        c.give(given);
        dialogueContainer.add(new dialogue("Game", "You got " + given.getName()).highlight(dialogue.ITEM_BORDER, given));
    }

    //Every teleport target in every world
    public List<worldTile> getAllTeleportTargets () {
        List<worldTile> all = new ArrayList<worldTile>();
        for (int w = 0; w < worlds.size(); w++) {
            for (tile t : worlds.get(w).findTiles(tileType::isTeleportTarget)) all.add(new worldTile(w, t));
        }
        return all;
    }

    //Every kind of item, for the give item cheat
    private itemRegistry itemRegistry;
    public itemRegistry getItemRegistry () {return this.itemRegistry;}


    //Starts and stops games (the controller). Asked to switch at the end of a frame, never in the middle of one
    private gameLauncher launcher;
    public gameLauncher getLauncher () {return this.launcher;}

    //The campaign being played, or null while this game is only the world behind the main menu
    private saveFile save;
    public saveFile getSave () {return this.save;}
    public boolean inMenu () {return this.save == null;}

    //save is the campaign to play (an empty one starts it from the beginning), null shows the main menu
    public gameHandler (RCanvas canvas, dialogueContainer dialogueContainer, battle battleHandler, imgDict imgDict, gameLauncher launcher, saveFile save) {
        this.launcher = launcher;
        this.save = save;
        this.canvas = canvas;
        this.dialogueContainer = dialogueContainer;
        this.battleHandler     = battleHandler;

        tempCanvas = new RCanvas();
        images = imgDict;

        params.setFill(Color.TRANSPARENT);


        gemstones gemstoneDict = new gemstones(this);


        tileTypes = new tileTypes(getTempGc());

        //Create characters
        characters.add(new character("Guard 1", 10000, 9992.85, 100, 1100, 100, 100, 0, this, 3));
        characters.add(new character("Guard 2", 10000.4, 9993, 100, 1100, 100, 100, 0, this, 3));
        characters.add(new character("Guard 3", 10000.9, 9992.85, 100, 1100, 100, 100, 0, this, 3));
        characters.add(new character("Tree", 9999.5, 9994, 400, 1100, 100, 100, 0, this, 0.5));
        characters.add(new character("Mother", 10000.33, 10000.22, 200, 1100, 100, 100, 2, this, 3));
        characters.add(new character("Grandma", 10001.11, 10000.22, 400, 1000, 100, 100, 2, this, 3));
        characters.add(new character("Girl (Beginner)", 10000.56, 9999.67, 500, 1100, 100, 100, 0, this, 3));
        characters.get("Girl (Beginner)").give(gemstoneDict.rock());
        characters.add(new character("Girl (Master)", 10010.44, 9959.45, 500, 1100, 100, 100, 2, this, 3, 17));
        characters.get("Girl (Master)").give(gemstoneDict.meteorite());
        characters.get("Girl (Master)").give(gemstoneDict.boltstone());
        characters.get("Girl (Master)").give(gemstoneDict.giantBolder());
        characters.get("Girl (Master)").give(gemstoneDict.healite());

        //After the final boss: the areas behind the portal in her room. Volcano depths and crystal caverns are caves
        //(world 1), the desert is outside (world 0), the sky ruins are their own world (4).
        //WHERE THEY BELONG: every character below is a digit (1-8) in its area's grid in tools/maps_postgame.py, and
        //that grid is the truth. Running `python tools/maps_postgame.py` prints each digit's world and tile as
        //"x chunk + column/9, y chunk + row/9"; the numbers here must be exactly those. A grid cell (row r, column c)
        //of an area with corner chunk (X, Y) is x = X + c//9 + (c%9)/9, y = Y + r//9 + (r%9)/9. If you move a
        //character, move its digit in the grid and copy the printed place here. Guards (guardFight) stand on a one
        //tile wide way and step up then left when beaten, so the tiles above and above-left of them must be floor.
        //Levels, gems and trinkets are listed in BALANCING.md under "After the final boss".
        //`python tools/check_postgame_characters.py .` compares all of this (places, worlds, levels, gems, trinkets,
        //fights, guards' steps aside, chests) with the grids and BALANCING.md and lists every mismatch.
        //Sprites are drafts at y = 1800 (tools/art_postgame.py)
        double pt = 1.0/getTileCount();
        //Ember Wraith (digit 1, XP fight, level 17): volcano depths, in the middle of the cave west of the big lava
        //lake, on the main way north from the landing (grid row 7, column 12; corner chunk 10009, 9956)
        characters.add(new character("Ember Wraith", 10010 + 3*pt, 9956 + 7*pt, 100, 1800, 100, 100, 1, this, 3, 17));
        characters.get("Ember Wraith").give(gemstoneDict.magma());
        characters.get("Ember Wraith").give(gemstoneDict.obsidian());
        //Ash Golem (digit 2, guard, level 18): volcano depths, the north-east, in the one tile wide way just below the
        //cave passage up to the desert ('U', grid row 1 column 19), blocking it (grid row 3, column 18)
        characters.add(new character("Ash Golem", 10011 + 0*pt, 9956 + 3*pt, 200, 1800, 100, 100, 1, this, 3, 18));
        characters.get("Ash Golem").give(gemstoneDict.giantBolder());
        characters.get("Ash Golem").give(gemstoneDict.lava());
        //Dune Scorpion (digit 3, XP fight, level 18): desert, in the middle of the brick plaza in the desert's north
        //half, west of the small oasis (grid row 8, column 12; corner chunk 10009, 9954)
        characters.add(new character("Dune Scorpion", 10010 + 3*pt, 9954 + 8*pt, 300, 1800, 100, 100, 0, this, 3, 18));
        characters.get("Dune Scorpion").give(gemstoneDict.sandstone());
        characters.get("Dune Scorpion").give(gemstoneDict.discombobulateShard());
        //Desert Nomad (digit 4, guard, level 19): desert, the north edge, in the one tile wide neck between the brick
        //plaza and the walkway under the 3x3 sky portal set in the dunes, blocking it (grid row 4, column 13)
        characters.add(new character("Desert Nomad", 10010 + 4*pt, 9954 + 4*pt, 400, 1800, 100, 100, 0, this, 3, 19));
        characters.get("Desert Nomad").give(gemstoneDict.saltstone());
        characters.get("Desert Nomad").give(gemstoneDict.glass());
        //Storm Harpy (digit 5, XP fight, level 19): sky ruins, the western island, next to its pillar
        //(grid row 10, column 5; corner chunk 10009, 9951)
        characters.add(new character("Storm Harpy", 10009 + 5*pt, 9952 + 1*pt, 500, 1800, 100, 100, 4, this, 3, 19));
        characters.get("Storm Harpy").give(gemstoneDict.boltstone());
        characters.get("Storm Harpy").give(gemstoneDict.copper());
        //Cloud Knight (digit 6, guard, level 20): sky ruins, the north, in the one tile wide way between the kitchen
        //tile path and the floor in front of the 3x3 portal down to the crystal caverns, blocking it (grid row 6, column 13)
        characters.add(new character("Cloud Knight", 10010 + 4*pt, 9951 + 6*pt, 600, 1800, 100, 100, 4, this, 3, 20));
        characters.get("Cloud Knight").give(gemstoneDict.meteorite());
        characters.get("Cloud Knight").give(gemstoneDict.healite());
        //Crystal Sentinel (digit 7, XP fight, level 20): crystal caverns, in the open hall between the toxic pools, on
        //the way north from the landing (grid row 10, column 14; corner chunk 10009, 9949)
        characters.add(new character("Crystal Sentinel", 10010 + 5*pt, 9950 + 1*pt, 700, 1800, 100, 100, 1, this, 3, 20));
        characters.get("Crystal Sentinel").give(gemstoneDict.uranium());
        characters.get("Crystal Sentinel").give(gemstoneDict.ice());
        //Prism Warden (digit 8, guard and last boss, level 22): crystal caverns, the far north, in the one tile wide way
        //just below the throne room with the Elemental Rock's chest (grid row 1, column 13), blocking it (grid row 4, column 13)
        characters.add(new character("Prism Warden", 10010 + 4*pt, 9949 + 4*pt, 800, 1800, 100, 100, 1, this, 3, 22));
        characters.get("Prism Warden").give(gemstoneDict.meteorite());
        characters.get("Prism Warden").give(gemstoneDict.boltstone());
        characters.get("Prism Warden").give(gemstoneDict.uranium());
        characters.get("Prism Warden").give(gemstoneDict.healite());
        //Crystal Boy (digit 9, the kidnapper, level 24): the player's double (the boy's sprite with white eyes, a draft
        //at (1200, 1800)). He takes the girl when she is beaten (kidnapping()) and waits in his castle north of the
        //crystal caverns, in the doorway of the mirror room at the top of his hall, beside her (grid row 6, column 4 of the castle hall;
        //corner chunk 10010, 9942)
        characters.add(new character("Crystal Boy", 10010 + 4*pt, 9942 + 6*pt, 1200, 1800, 100, 100, 2, this, 3, 24));
        characters.get("Crystal Boy").give(gemstoneDict.ice());
        characters.get("Crystal Boy").give(gemstoneDict.uranium());
        characters.get("Crystal Boy").give(gemstoneDict.meteorite());
        characters.get("Crystal Boy").give(gemstoneDict.healite());
        //In the mirror dimension your mother and grandma stand in stone outside their mirrored houses (house 1 is
        //chunk 10002 there, grandma's house 3 is 10001), on the grass beside the door. Copies: the real ones are at home
        characters.add(new character("Mother (statue)", 10002 + 2*pt, 10000 + 7*pt, 200, 1100, 100, 100, MIRROR_WORLD, this, 3));
        characters.add(new character("Grandma (statue)", 10001 + 1*pt, 10000 + 7*pt, 400, 1000, 100, 100, MIRROR_WORLD, this, 3));
        characters.get("Mother (statue)").setStatue(true);
        characters.get("Grandma (statue)").setStatue(true);
        //You as you were, just after beating the girl in her room: only there once you have come back through the mirror
        //as the reflection (theLoop), standing below her
        characters.add(new character("You (past)", 10010 + 4*pt, 9959 + 6*pt, 0, 500, 100, 100, 2, this, 5));
        characters.get("You (past)").setGone(true);
        //A copy of the stone guards from the castle at the start, standing where the girl stood when you first met her
        //(chunk 10000, 9999, column 5, row 6), mirrored: in the one tile wide gap of the village wall, on the only way
        //up to the crystal boy (level 23, between the Prism Warden and him)
        characters.add(new character("Stone Guardian", 10002 + 3*pt, 9999 + 6*pt, 100, 1100, 100, 100, MIRROR_WORLD, this, 3, 23));
        characters.get("Stone Guardian").give(gemstoneDict.giantBolder());
        characters.get("Stone Guardian").give(gemstoneDict.meteorite());
        characters.get("Stone Guardian").give(gemstoneDict.healite());

        characters.add(new character("Keeper of Fire", 9999.11, 9993.22, 500, 1000, 100, 100, 1, this, 3, 2));
        characters.get("Keeper of Fire").give(gemstoneDict.obsidian());
        characters.add(new character("Weird Crab", 10002.44, 9976.56, 300, 1200, 100, 100, 0, this, 3, 11));
        characters.get("Weird Crab").give(gemstoneDict.discombobulateShard());

        characters.add(new character("Cool Crab", 10002.33, 9975.56, 300, 1200, 100, 100, 0, this, 3, 10));
        characters.add(new character("Normal Crab", 10002.55, 9974.56, 300, 1200, 100, 100, 0, this, 3, 10));
        
        characters.add(new character("Eugene Krabs", 10001.22, 9971.33, 300, 1200, 100, 100, 0, this, 3, 10));
        characters.add(new character("Magical Crab", 10004.44, 9972.33, 300, 1200, 100, 100, 0, this, 3, 10));
        characters.add(new character("Crabby", 10006.44, 9973, 300, 1200, 100, 100, 0, this, 3, 10));


        characters.add(new character("Sandman", 10003.44, 9973.56, 200, 1300, 100, 100, 0, this, 3, 13));
        characters.get("Sandman").give(gemstoneDict.discombobulateShard());

        characters.add(new character("Tree Guard", 10006.89, 9973.89, 300, 1300, 100, 100, 0, this, 3, 13));
        characters.get("Tree Guard").give(gemstoneDict.meteorite());

        characters.add(new character("Snowman", 10010.44, 9973.89, 400, 1300, 100, 100, 0, this, 3, 13));
        characters.get("Snowman").give(gemstoneDict.meteorite());

        characters.add(new character("Kalle", 10010.44, 9971.33, 400, 1300, 100, 100, 2, this, 3, 13));


        characters.add(new character("Rock", 10013.44, 9968.89, 500, 1300, 100, 100, 0, this, 3, 14));
        characters.get("Rock").give(gemstoneDict.pebble());
        characters.get("Rock").give(gemstoneDict.healite());

        characters.add(new character("Cornerstone", 10002.44, 9984.89, 500, 1300, 100, 100, 1, this, 3, 8));
        characters.get("Cornerstone").give(gemstoneDict.uranium());


        characters.add(new character("Guard 4", 10013.89, 9964.22, 100, 1100, 100, 100, 0, this, 3));
        characters.add(new character("Guard 5", 10015, 9964.22, 100, 1100, 100, 100, 0, this, 3));
        characters.add(new character("Security Guard", 10014.44, 9964.11, 0, 1100, 100, 100, 0, this, 3, 15));
        characters.get("Security Guard").give(gemstoneDict.giantBolder());
        characters.get("Security Guard").give(gemstoneDict.healite());
        characters.add(new character("Gemstone Guard", 10013.78, 9968.22, 500, 1300, 100, 100, 1, this, 3, 14));
        characters.get("Gemstone Guard").give(gemstoneDict.pebble());
        characters.get("Gemstone Guard").give(gemstoneDict.healite());


        characters.add(new character("Guardman", 10002.44, 9980.89, 0, 1100, 100, 100, 0, this, 3, 11));
        characters.get("Guardman").give(gemstoneDict.pebble());
        characters.add(new character("Knight", 10000.44, 9991.33, 100, 1400, 100, 100, 2, this, 3, 6));
        characters.get("Knight").give(gemstoneDict.giantBolder());

        characters.add(new character("Paladin", 10000.22, 9990.22, 100, 1400, 100, 100, 2, this, 3, 6));
        characters.get("Paladin").give(gemstoneDict.boltstone());

        characters.add(new character("King", 10001.11, 9990.56, 0, 1400, 100, 100, 2, this, 3, 7));
        characters.get("King").give(gemstoneDict.boltstone());


        characters.add(new character("Citizen Peter", 10002.44, 9983.89, 0, 1200, 100, 100, 0, this, 3, 9));
        characters.add(new character("Citizen Marcus", 10003.56, 9982.89, 0, 1300, 100, 100, 0, this, 3, 9));
        characters.add(new character("Citizen Martin", 10003, 9982, 100, 1300, 100, 100, 0, this, 3, 8));
        characters.add(new character("Citizen Leidulf Lie", 10002.22, 9981.89, 100, 1200, 100, 100, 0, this, 3, 8));
        characters.add(new character("Citizen Edna", 10001, 9981, 0, 1300, 100, 100, 0, this, 3, 9));
        characters.add(new character("Citizen Johann", 10002, 9979, 100, 1200, 100, 100, 0, this, 3, 8));
        characters.add(new character("Citizen Endre", 10003, 9978, 0, 1200, 100, 100, 0, this, 3, 9));

        characters.add(new character("Citizen Ola", 9980, 9988, 0, 1200, 100, 100, 0, this, 3, 3));
        characters.add(new character("Citizen Olav", 9980.44, 9988.89, 0, 1300, 100, 100, 0, this, 3, 4));
        characters.add(new character("Citizen Ole", 10000.22, 9988.22, 100, 1300, 100, 100, 0, this, 3, 4));
        characters.add(new character("Citizen Olga", 10000.56, 9988, 100, 1200, 100, 100, 0, this, 3, 3));
        characters.add(new character("Citizen Ove", 10002, 9989.89, 0, 1300, 100, 100, 0, this, 3, 4));
        characters.add(new character("Citizen Odd Ingvar", 10003, 9988.56, 100, 1200, 100, 100, 0, this, 3, 4));
        characters.add(new character("Citizen Odde", 10000.33, 9989.89, 0, 1200, 100, 100, 0, this, 3, 3));
        characters.add(new character("Citizen Oliver", 9999.33, 9990.56, 100, 1200, 100, 100, 0, this, 3, 3));
        characters.add(new character("Citizen Owen", 9999.67, 9989.44, 0, 1300, 100, 100, 0, this, 3, 4));
        characters.add(new character("Citizen Orlando", 10000, 9987.78, 0, 1300, 100, 100, 0, this, 3, 4));


        characters.add(new character("Dolly", 10000.22, 9997.56, 0, 600, 100, 100, 0, this, 1, 1));
        characters.add(new character("Sauen Shaun", 10000.56, 9997.33, 0, 600, 100, 100, 0, this, 1, 1));
        characters.add(new character("Gerd Borghild", 10000.44, 9995.78, 0, 600, 100, 100, 0, this, 1, 1));
        characters.add(new character("Sheep", 10000.22, 9995.11, 0, 600, 100, 100, 0, this, 1, 1));
        characters.add(new character("Tikke", 10000.78, 9988.78, 0, 600, 100, 100, 0, this, 1, 1));
        characters.add(new character("Åge", 10001, 9987.56, 0, 600, 100, 100, 0, this, 1, 1));
        characters.add(new character("Magical Sheep", 9997.44, 9988.44, 0, 600, 100, 100, 0, this, 1, 1));
        characters.add(new character("Nonmagical Sheep", 10001.44, 9983, 0, 600, 100, 100, 0, this, 1, 1));
        characters.add(new character("Semi Magical Sheep", 10003.44, 9980.22, 0, 600, 100, 100, 0, this, 1, 1));



        //Main character
        characters.add(new character("Boy",  10000.3, 10000.3, 0, 500, 100, 100, 3, this, 5).showInTransition(true));

        //What enemies leave behind when beaten
        items itemDict = new items(this);
        this.itemDict = itemDict;
        itemRegistry = new itemRegistry(gemstoneDict, itemDict);
        characters.get("Girl (Beginner)").addDrop(() -> itemDict.healingPotion(), 1, 1);

        //Trinkets the bosses wear (they count in the fight), and drop a copy of when beaten
        characters.get("Guardman").wear(itemDict.creatine());
        characters.get("Guardman").addDrop(() -> itemDict.creatine(), 1, 1);
        characters.get("Paladin").wear(itemDict.featherOfHermesBoots());
        characters.get("Paladin").addDrop(() -> itemDict.featherOfHermesBoots(), 1, 1);
        characters.get("King").wear(itemDict.heartOfTheWarrior());
        characters.get("King").addDrop(() -> itemDict.heartOfTheWarrior(), 1, 1);
        //The final boss only wears hers
        characters.get("Girl (Master)").wear(itemDict.witheredHeart());
        //And so do the guards after her
        characters.get("Desert Nomad").wear(itemDict.creatine());
        characters.get("Cloud Knight").wear(itemDict.featherOfHermesBoots());
        characters.get("Prism Warden").wear(itemDict.heartOfTheWarrior());
        for (character sheep : Arrays.asList(characters.get("Dolly"), characters.get("Sauen Shaun"), characters.get("Gerd Borghild"), characters.get("Sheep"),
                characters.get("Tikke"), characters.get("Åge"), characters.get("Magical Sheep"), characters.get("Nonmagical Sheep"), characters.get("Semi Magical Sheep"))) {
            sheep.addDrop(() -> itemDict.woolBandage(), 0.5, 2);
        }


        List<character> noWin = Arrays.asList(
            characters.get("Cool Crab"), 
            characters.get("Normal Crab"),
            characters.get("Gemstone Guard"),
            characters.get("Citizen Peter"),
            characters.get("Citizen Marcus"),
            characters.get("Citizen Martin"),
            characters.get("Citizen Leidulf Lie"),
            characters.get("Citizen Edna"),
            characters.get("Citizen Johann"),
            characters.get("Citizen Endre"),
            characters.get("Citizen Ola"),
            characters.get("Citizen Olav"),
            characters.get("Citizen Ole"),
            characters.get("Citizen Olga"),
            characters.get("Citizen Ove"),
            characters.get("Citizen Odd Ingvar"),
            characters.get("Citizen Odde"),
            characters.get("Dolly"),
            characters.get("Sauen Shaun"),
            characters.get("Gerd Borghild"),
            characters.get("Sheep"),
            characters.get("Tikke"),
            characters.get("Åge"),
            characters.get("Magical Sheep"),
            characters.get("Nonmagical Sheep"),
            characters.get("Eugene Krabs"),
            characters.get("Magical Crab"),
            characters.get("Crabby"),
            characters.get("Kalle"),
            characters.get("Citizen Oliver"),
            characters.get("Citizen Owen"),
            characters.get("Citizen Orlando")

        );


        //Create events
        event blockDoor1 = new movementEvent(this, characters.get("Guard 1"), Arrays.asList(2, 2, 2, 3, 3, 3, 3, 3), false);
        event blockDoor2 = new movementEvent(this, characters.get("Guard 2"), Arrays.asList(3, 3, 3, 3), true, 3.5);
        event blockDoor3 = new movementEvent(this, characters.get("Guard 3"), Arrays.asList(0, 0, 0, 3, 3, 3, 3, 3), false);
        event secretPath = new movementEvent(this, characters.get("Tree"), Arrays.asList(0), true, 2);
        event showPath   = new movementEvent(this, characters.get("Guard 2"), Arrays.asList(), true, 3.5);
        event unblock    = new movementEvent(this, characters.get("Guard 2"), Arrays.asList(3, 3, 0), true, 1);
        event spawnEvent = new movementEvent(this, getMainCharacter(), Arrays.asList(), false);
        event motherTellEvent     = new movementEvent(this, characters.get("Mother"), Arrays.asList(3, 3, 3, 3), true, 1);

        event bindToPers = new movementEvent(this, characters.get("Girl (Beginner)"), Arrays.asList(), true, 3);       
        

        event block = new movementEvent(this, characters.get("Knight"), Arrays.asList(3, 3, 3, 3), true, 3);
        event cornerstone = new movementEvent(this, characters.get("Cornerstone"), Arrays.asList(), true, 3);
        event guardman = new movementEvent(this, characters.get("Guardman"), Arrays.asList(1, 1, 1, 1, 1), true, 3);
        event security = new movementEvent(this, characters.get("Security Guard"), Arrays.asList(1, 1, 1), true, 4);
        event guard4 = new movementEvent(this, characters.get("Guard 4"), Arrays.asList(2, 2, 2, 2, 1, 1, 1), false);
        event guard5 = new movementEvent(this, characters.get("Guard 5"), Arrays.asList(0, 0, 0, 0, 1, 1, 1), false);

        event grandmaCome = new movementEvent(this, characters.get("Grandma"), Arrays.asList(3, 2, 2, 1), true, 2);


        //Battles-------------
        //Battle 1
        event theyWin     = new teleportEvent(getMainCharacter());

        event youWin      = new movementEvent(this, characters.get("Girl (Beginner)"), Arrays.asList(1, 1, 1, 0), true, 1);
        event firstBattle = new battleEvent(getMainCharacter(), characters.get("Girl (Beginner)"), battleHandler, youWin, theyWin, characters.get("Girl (Beginner)").getX(), characters.get("Girl (Beginner)").getY(), 0.15, 0);
        eventListeners.add(new eventListener(this, characters.get("Girl (Beginner)").getX(), characters.get("Girl (Beginner)").getY(), 0.15, 0, firstBattle, Arrays.asList(new dialogue(getMainCharacter(), "!!!"),new dialogue(characters.get("Girl (Beginner)"), "I wont let you pass unless you beat me!\nChallenge accepted?"),new dialogue(getMainCharacter(), "✓✓✓ !"))));

        event unblocklava = new movementEvent(this, characters.get("Keeper of Fire"), Arrays.asList(2), false);
        event fireKeeperBattle = new battleEvent(getMainCharacter(), characters.get("Keeper of Fire"), battleHandler, unblocklava, theyWin, characters.get("Keeper of Fire").getX(), characters.get("Keeper of Fire").getY(), 0.15, 1);
        eventListeners.add(new eventListener(this, characters.get("Keeper of Fire").getX(), characters.get("Keeper of Fire").getY(), 0.15, 1, fireKeeperBattle, Arrays.asList(new dialogue(getMainCharacter(), "ツ?"),new dialogue(characters.get("Keeper of Fire"), "I will guard this fire with my own life if I have to.\nUntil I find somebody worthy"),new dialogue(getMainCharacter(), "..."))));

        event unblockCrab = new movementEvent(this, characters.get("Weird Crab"), Arrays.asList(1, 1), true);
        event crabBattle = new battleEvent(getMainCharacter(), characters.get("Weird Crab"), battleHandler, unblockCrab, theyWin, characters.get("Weird Crab").getX(), characters.get("Weird Crab").getY(), 0.15, 0);
        eventListeners.add(new eventListener(this, characters.get("Weird Crab").getX(), characters.get("Weird Crab").getY(), 0.15, 0, crabBattle, Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));


        character tC = characters.get("Sandman"); //Make it easier to create mass battles
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(1, 1, 0, 0), true, 3), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
        Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));

        tC = characters.get("Tree Guard");
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(1, 1), false), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
        Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));


        tC = characters.get("Snowman");
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(1, 1, 2), true, 3), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
        Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));

        tC = characters.get("Rock");
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(0, 0), true, 1), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
        Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));

        tC = characters.get("Security Guard");
        eventListeners.add(new eventListener(this, 10014.44, 9963.78, 0.15, 0, 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(1, 1, 1, 1, 2, 2), true, 3), theyWin, 10014.44, 9963.78, 0.15, 0), 
        Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));

        tC = characters.get("Guardman");
        eventListeners.add(new eventListener(this, 10002.44, 9980.33, 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(1, 1, 2, 2), true, 3), theyWin, 10002.44, 9980.33, 0.15, tC.getWorld()), 
        Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));

        tC = characters.get("Knight");
        eventListeners.add(new eventListener(this, 10000.44, 9991.78, 0.15, 0, 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(1, 1, 2, 2), true, 3), theyWin, 10000.44, 9991.78, 0.15, 0), 
        Arrays.asList(
        new dialogue(tC, "Hello citizen!"),
        new dialogue(getMainCharacter(), "☺! ▲?"),
        new dialogue(tC, "Oh you want to come in?"),
        new dialogue(getMainCharacter(), "✓ !!!"),
        new dialogue(tC, "You're not a man of many words are you?"),
        new dialogue(getMainCharacter(), "..."),
        new dialogue(tC, "Anyways, Im afraid that that is not possible...\nYou would have to defeat me which not many have done!"),
        new dialogue(getMainCharacter(), "☻")
        )));

        tC = characters.get("Paladin");
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(1, 2, 2, 2), true, 3), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
        Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));

        tC = characters.get("King");
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(3, 2, 2, 2), false), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
        Arrays.asList(
            new dialogue(getMainCharacter(), "!!!"),
            new dialogue(tC, "Who are you, my child?"),
            new dialogue(getMainCharacter(), "..."),
            new dialogue(tC, "What?"),
            new dialogue(getMainCharacter(), "♛↘↘↘!"),
            new dialogue(tC, "You want me to what?"),
            new dialogue(getMainCharacter(), "..."),
            new dialogue(getMainCharacter(), "♛↘♟!"),
            new dialogue(tC, "You challenge me?"),
            new dialogue(getMainCharacter(), "✓✓✓ :D"),
            new dialogue(tC, "Let it commence")
        )));

        tC = characters.get("Girl (Master)");
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  girlBeaten(tC), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
        Arrays.asList(
            new dialogue(tC, "You!"),
            new dialogue(getMainCharacter(), "!!!"),
            new dialogue(tC, "Still havent learnt to speak?"),
            new dialogue(getMainCharacter(), "): ..."),
            new dialogue(tC, "I see, anyways, may I ask why you are here?"),
            new dialogue(getMainCharacter(), "↘↘↘"),
            new dialogue(tC, "You want to bring me down?"),
            new dialogue(getMainCharacter(), "↗↗↗"),
            new dialogue(tC, "Huh?"),
            new dialogue(getMainCharacter(), "✓✓✓*"),
            new dialogue(tC, "Then this will be your conclusion,\nlet us finish this!")        
        )));

        tC = characters.get("Cornerstone");
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(), 
        new battleEvent(getMainCharacter(), tC, battleHandler,  new movementEvent(this, tC, Arrays.asList(0), false), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
        Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));

        //After the final boss: the crystal boy's servants. A guard before every gate (steps aside up and left when beaten),
        //XP fights on the way. Before the fight they serve him; beaten, they let slip that they only do it out of fear
        character me = getMainCharacter();
        character ember = characters.get("Ember Wraith"), golem = characters.get("Ash Golem"), scorpion = characters.get("Dune Scorpion");
        character nomad = characters.get("Desert Nomad"), harpy = characters.get("Storm Harpy"), knight = characters.get("Cloud Knight");
        character sentinel = characters.get("Crystal Sentinel"), warden = characters.get("Prism Warden");
        servantFight(ember, Arrays.asList(
            new dialogue(ember, "A boy with your face... no. The master's eyes are white. Yours are not."),
            new dialogue(ember, "No one follows him through the fire. Those are his orders."),
            new dialogue(me, "!!!")), Arrays.asList(
            new dialogue(ember, "Please... do not tell him I lost."),
            new dialogue(ember, "You have not seen what he does to those who fail him.")));
        servantGuard(golem, Arrays.asList(
            new dialogue(golem, "I guard this gate for the master. Turn back."),
            new dialogue(me, "!!!")), Arrays.asList(
            new dialogue(golem, "I was stone once, cold and free. He made me burn for him."),
            new dialogue(golem, "If he learns I let you pass... go. Quickly.")));
        servantFight(scorpion, Arrays.asList(
            new dialogue(scorpion, "Sssso the other one comes at last. The master said you would."),
            new dialogue(me, "!!!")), Arrays.asList(
            new dialogue(scorpion, "He will know. He sssees everything in his glass..."),
            new dialogue(scorpion, "Every mirror, every pool of water. He is watching ussss.")));
        servantGuard(nomad, Arrays.asList(
            new dialogue(nomad, "I walk these dunes for the master now. Few walk past me."),
            new dialogue(me, "☻")), Arrays.asList(
            new dialogue(nomad, "I did not choose this. He took my caravan into his mirror, one by one."),
            new dialogue(nomad, "I serve so that I am not next.")));
        servantFight(harpy, Arrays.asList(
            new dialogue(harpy, "Kreee! The master's sky is closed to you!"),
            new dialogue(me, "!!!")), Arrays.asList(
            new dialogue(harpy, "He clipped the wings of every bird that would not serve him."),
            new dialogue(harpy, "Mine he left. For now.")));
        servantGuard(knight, Arrays.asList(
            new dialogue(knight, "Halt! I hold this gate in the master's name. Prove you are worthy of passing it."),
            new dialogue(me, "✓✓✓ !")), Arrays.asList(
            new dialogue(knight, "A knight should serve for honour. I serve because I have seen my own reflection turn and look back at me."),
            new dialogue(knight, "Pass. And end it, if you can.")));
        servantFight(sentinel, Arrays.asList(
            new dialogue(sentinel, "The crystal sings of you. It says you are the master... only warmer."),
            new dialogue(me, "!!!")), Arrays.asList(
            new dialogue(sentinel, "Warmer... yes. He is so cold."),
            new dialogue(sentinel, "We all crack a little when he walks past.")));
        servantGuard(warden, Arrays.asList(
            new dialogue(warden, "So you are his reflection. He left me the stone that is every stone, and the way behind it."),
            new dialogue(me, "!!!?"),
            new dialogue(warden, "Neither will be taken from me easily.")), Arrays.asList(
            new dialogue(warden, "Take the stone, and the way up. We all fear him. None of us ever dared stand against him."),
            new dialogue(warden, "Free us...")));

        //The crystal boy, in front of the girl in his hall. Beating him frees her (rescue())
        tC = characters.get("Crystal Boy");
        crystalBoyFight = new eventListener(this, tC.getX(), tC.getY(), 0.15, tC.getWorld(),
            new battleEvent(getMainCharacter(), tC, battleHandler, () -> play(escape()), new teleportEvent(getMainCharacter()), tC.getX(), tC.getY(), 0.15, tC.getWorld()),
            Arrays.asList(
                new dialogue(tC, "So you made it past my servants. Useless, all of them. They will pay for that."),
                new dialogue(getMainCharacter(), "!!!"),
                new dialogue(tC, "Look at us. The same face. But I do not tire, I do not bleed, I do not lose."),
                new dialogue(characters.get("Girl (Master)"), "Do not listen to him! Break him!"),
                new dialogue(tC, "Come then. Let us see which of us is the real one.")));
        eventListeners.add(crystalBoyFight);

        //The rematch in the mirror dimension, at the top of the mirrored village's path. Beating him there ends him
        double[] boss = mirrorBoss();
        mirrorFight = new eventListener(this, boss[0], boss[1], 0.15, MIRROR_WORLD,
            new battleEvent(getMainCharacter(), tC, battleHandler, () -> play(becoming()), new teleportEvent(getMainCharacter()), boss[0], boss[1], 0.15, MIRROR_WORLD),
            Arrays.asList(
                new dialogue(tC, "Welcome to my world. Here everything is the way it should be: the other way round."),
                new dialogue(characters.get("Girl (Master)"), "It's our village... but wrong. Cold."),
                new dialogue(tC, "There is nowhere left for me to run, and nowhere for you to hide."),
                new dialogue(getMainCharacter(), "!!!")));
        eventListeners.add(mirrorFight);

        //The stone guardian in the gap of the mirrored village wall: beaten, it crumbles away and the way is open
        tC = characters.get("Stone Guardian");
        eventListeners.add(new eventListener(this, tC.getX(), tC.getY(), 0.15, MIRROR_WORLD,
            new battleEvent(getMainCharacter(), tC, battleHandler, () -> play(guardianCrumbles()), new teleportEvent(getMainCharacter()), tC.getX(), tC.getY(), 0.15, MIRROR_WORLD),
            Arrays.asList(
                new dialogue(tC, "None shall pass to the master."),
                new dialogue(characters.get("Girl (Master)"), "That's where I stood, the first time we met. He even copied that."),
                new dialogue(getMainCharacter(), "!!!"))));

        //Walking out of the mirror the first time: the camera goes over the stone statues of your mother and grandma
        double[] arrival = mirrorArrival();
        eventListeners.add(new eventListener(this, arrival[0], arrival[1], 0.15, MIRROR_WORLD, () -> play(mirrorWelcome())));

        //Battles for xp with no win event
        for (character c : noWin) {
            c.give(gemstoneDict.pebble());            
            event battle = new battleEvent(getMainCharacter(), c, battleHandler, null, theyWin, c.getX(), c.getY(), 0.15, c.getWorld());
            eventListeners.add(new eventListener(this, c.getX(), c.getY(), 0.15, c.getWorld(), battle, Arrays.asList(new dialogue(getMainCharacter(), "!!!"))));
        }

        //Add eventlisteners for events ---------
        eventListeners.add(new eventListener(this, 10000.1, 10000.1, 0.5, 3, spawnEvent, Arrays.asList(new dialogue(getMainCharacter(), "What a weird dream..."),new dialogue(getMainCharacter(), "Wait..."), new dialogue(getMainCharacter(), "I can feel my legs?"),new dialogue(getMainCharacter(), "Wait how do I walk again...\nIt is WASD to move right?"))));
        eventListeners.add(new eventListener(this, 10000.3, 10000.56, 0.3, 2, motherTellEvent, Arrays.asList(new dialogue(characters.get("Mother"), "Dont forget that you promised granny to\nclean her house!"),new dialogue(getMainCharacter(), "... ):"))));





        eventListeners.add(new eventListener(this, 9998.11, 9991.89, 0.5, 0, block));
        eventListeners.add(new eventListener(this, 10002.44, 9990.67, 0.5, 1, cornerstone));
        eventListeners.add(new eventListener(this, 10002.44, 9984.33, 0.3, 0, guardman));


        eventListeners.add(new eventListener(this, 10001.11, 10000.22, 0.2, 2, null, Arrays.asList(new dialogue(characters.get("Grandma"), "Get to cleaning child"),new dialogue(getMainCharacter(), "..."))));
        eventListeners.add(new eventListener(this, 10000.44, 9994.33, 0.5, 0, blockDoor1));
        eventListeners.add(new eventListener(this, 10000.44, 9994.33, 0.5, 0, blockDoor2, Arrays.asList(new dialogue(characters.get("Guard 2"), "Hey! We dont allow outsiders in here!"),new dialogue(getMainCharacter(), "..."))));
        eventListeners.add(new eventListener(this, 10000.44, 9994.33, 0.5, 0, blockDoor3));
        eventListeners.add(new eventListener(this, 10001.78, 9994.22, 0.06, 0, secretPath, Arrays.asList(new dialogue(getMainCharacter(), "???!"))));
        eventListeners.add(new eventListener(this, 10000.56, 9999.67, 0.06, 0, showPath));
        eventListeners.add(new eventListener(this, 10000.44, 9993.11, 0.2, 0, unblock));

        eventListeners.add(new eventListener(this, 10014.44, 9968.78, 0.3, 0, security));
        eventListeners.add(new eventListener(this, 10014.44, 9968.78, 0.3, 0, guard4));
        eventListeners.add(new eventListener(this, 10014.44, 9968.78, 0.3, 0, guard5));



        //Signs
        eventListeners.add(new eventListener(this, 10000.56, 10001.89, 0.05, 0, bindToPers, Arrays.asList(new dialogue("Sign", "House of the orphaned child that for some story-related-reason\n always stand in the same spot"))));
        eventListeners.add(new eventListener(this, 10001.56, 10000.89, 0.05, 0, null, Arrays.asList(new dialogue("Sign", "Grandmas house"))));
        eventListeners.add(new eventListener(this, 10000.33, 10000.89, 0.05, 0, null, Arrays.asList(new dialogue("Sign", "Mothers house"))));
        eventListeners.add(new eventListener(this, 9999.56, 9992, 0.05, 0, null, Arrays.asList(new dialogue("Sign", "▲ Village\n► Castle"))));

        nameCampaignListeners();


        camera = new camera(this, getMainCharacter());
        //Generate map
        readChunks.read(this);

        //Chests: every chest tile, then what is in them. One item sits in the middle row; the boltstone shares its
        //chest with copper right next to it, so the player sees the two set each other off
        for (int w = 0; w < worlds.size(); w++) {
            for (tile t : worlds.get(w).findTiles(tileType::isContainer)) chests.add(new chest(new worldTile(w, t)));
        }
        //Grandma's chest with the first stone: closing it the first time, grandma walks over and talks about it
        List<dialogue> grandmaDialogue = Arrays.asList(
            new dialogue(getMainCharacter(), "???!"), 
            new dialogue(characters.get("Grandma"), "What did you find?"),
            new dialogue(getMainCharacter(), "..."),
            new dialogue(characters.get("Grandma"), "Oh, one of those old gemstones? The adventures I had with those..."),
            new dialogue(getMainCharacter(), "!!!?"),
            new dialogue(characters.get("Grandma"), "Oh yes, your old grandma had quite som campaigns back in the day"),
            new dialogue(getMainCharacter(), "oo"),
            new dialogue(characters.get("Grandma"), "Keep it if you want, maybe it can kickstart your own campaign"),
            new dialogue(getMainCharacter(), ":D"),
            new dialogue(characters.get("Grandma"), "Its time for you to grind, my child"),
            new dialogue(getMainCharacter(), "✓✓✓ !")
        );
        chestAt(2, 10001.54, 10000.11).put(1, 3, gemstoneDict.rock()).put(1, 4, itemDict.healingPotion()).healing().onFirstClose(() -> {
            getMainCharacter().restore();
            grandmaCome.trigger();
            for (dialogue line : grandmaDialogue) dialogueContainer.add(line);
        });
        chestAt(1, 9999.11, 9993.11).put(1, 3, gemstoneDict.obsidian()).put(1, 4, itemDict.healingPotion());
        chestAt(0, 10000.78, 9991.11).put(1, 3, gemstoneDict.boltstone()).put(1, 4, gemstoneDict.copper());
        chestAt(2, 10000.22, 9990.89).put(1, 3, gemstoneDict.giantBolder()).put(1, 4, itemDict.healingPotion());
        chestAt(1, 10002.44, 9987.44).put(1, 3, gemstoneDict.uranium()).put(1, 4, itemDict.healingPotion());
        chestAt(2, 10015.67, 9962.22).put(1, 3, gemstoneDict.magma()).put(1, 4, itemDict.healingPotion());
        chestAt(2, 10010, 9973.11).put(1, 3, gemstoneDict.meteorite()).put(1, 4, itemDict.healingPotion());
        chestAt(2, 10003.44, 9973.11).put(1, 3, gemstoneDict.discombobulateShard()).put(1, 4, gemstoneDict.sandstone());
        chestAt(1, 10013.89, 9968.89).put(1, 3, gemstoneDict.healite()).put(1, 4, itemDict.healingPotion());
        chestAt(2, 10003.22, 9981.33).put(1, 3, gemstoneDict.pebble()).put(1, 4, itemDict.healingPotion());
        //In the small house past Citizen Peter, outside the cave, chunk (10002, 9983)
        chestAt(2, 10002 + 5.0/getTileCount(), 9983 + 3.0/getTileCount()).put(1, 3, gemstoneDict.saltstone()).put(1, 4, itemDict.healingPotion());
        //By the ice pond in the snow, chunk (10013, 9973)
        chestAt(0, 10013 + 2.0/getTileCount(), 9973 + 1.0/getTileCount()).put(1, 3, gemstoneDict.ice()).put(1, 4, itemDict.healingPotion());
        //After the final boss: potions and the goggles in a side pocket of the volcano caves, and the elemental rock behind the Prism Warden
        chestAt(1, 10010 + 2.0/getTileCount(), 9957 + 3.0/getTileCount()).put(1, 3, itemDict.healingPotion()).put(1, 4, itemDict.cleansingPotion()).put(1, 5, itemDict.goggles());
        chestAt(1, 10010 + 4.0/getTileCount(), 9949 + 1.0/getTileCount()).put(1, 3, gemstoneDict.elemental()).put(1, 4, itemDict.healingPotion());
        //Castle grounds: hidden in a pocket in the trees east of the avenue, on its own little patch of brick
        chestAt(0, 10012 + 4.0/getTileCount(), 9947 + 2.0/getTileCount()).put(1, 3, itemDict.healingPotion()).put(1, 4, itemDict.cleansingPotion());
        //Looking in a chest for the first time restores you, like picking up a stone used to
        for (chest c : chests) {
            if (c.getPlace().getWorld() == 2 && Math.abs(c.getPlace().getX() - 10001.54) < 0.06 && Math.abs(c.getPlace().getY() - 10000.11) < 0.06) continue;
            c.healing().onFirstClose(() -> getMainCharacter().restore());
        }

        //The healing pad in the castle outside, chunk (9999, 9992), gives the teleporter
        addTeleportReward(0, 9999 + 4.0/getTileCount(), 9992 + 4.0/getTileCount(), () -> itemDict.teleporter());

        gui = new gui(this);

        if (save != null) {
            readNames(save);
            saveState.apply(this, save);
        } else {
            camera.setX(menuView.getX());
            camera.setY(menuView.getY());
            unitWidth = worlds.get(0).getUnitWidthHeight().get(0);
            unitHeight = worlds.get(0).getUnitWidthHeight().get(1);
        }
    }

    //Beating the girl (Master) finishes the game as before (the time goes to the highscore), and then she steps aside
    //from the portal behind her, which lights once she is taken (portalLit) and leads to the areas after her
    private event girlBeaten (character girl) {
        event finish = new finishGameEvent(this);
        return () -> {
            finish.trigger();
            play(kidnapping());
        };
    }

    //Where the girl is held in the crystal boy's hall (grid row 6, column 3 of the castle hall), in a stone niche
    //beside him
    private double[] heldGirl () {return new double[] {10010 + 3.0/getTileCount(), 9942 + 6.0/getTileCount()};}
    private static final int HALL_WORLD = 2;
    private eventListener crystalBoyFight;

    //Crystal sparks: the crystal boy coming and going
    private static final String[] CRYSTAL = {"#FFFFFF", "#D8F4FF", "#8EDCF2", "#B4A8FF"};
    private void crystalBurst (character c) {
        burst(c.getX() + 0.5/getTileCount(), c.getY() + 0.5/getTileCount(), c.getWorld(), 70, CRYSTAL);
    }

    //Right after the girl is beaten: the crystal boy steps out of the portal behind her, wraps her in crystal and takes
    //her to his castle, north of the crystal caverns
    private cutscene kidnapping () {
        character girl = characters.get("Girl (Master)"), boy = characters.get("Crystal Boy");
        double t = 1.0/getTileCount();
        Runnable takeHer = () -> {
            double[] held = heldGirl();
            girl.placeAt(HALL_WORLD, held[0], held[1]);
            boy.placeAt(boy.getHomeWorld(), boy.getHomeX(), boy.getHomeY());
        };
        //The mirror (tile 194) in the niche west of the portal in her room's north wall, and the tile in front of it
        //The mirror hangs on the north wall, left of the pillar west of the portal, with a niche of cracked brick in front
        double mirrorX = 10010 + 1*t, mirrorY = 9959 + 0*t;
        Runnable mirrorFlash = () -> burst(mirrorX + 0.5*t, mirrorY + 0.5*t, girl.getWorld(), 60, CRYSTAL);
        return new cutscene(this)
            .run(() -> portalHeld = true)
            .say(new dialogue(girl, "...Fine. You win."))
            .wait(0.6)
            //A mirror appears on the wall
            .run(() -> {mirrorShown = true; mirrorFlash.run();})
            .wait(0.5)
            .say(new dialogue(girl, "A mirror...?"))
            //He steps out of it, into the niche in front of it
            .run(() -> {mirrorFlash.run(); boy.placeAt(girl.getWorld(), mirrorX, mirrorY); crystalBurst(boy);})
            .walkThrough(boy, 3, 3)
            .say(new dialogue(boy, "Not so fast."),
                 new dialogue(girl, "Who...? He came out of the mirror... he has your face!"),
                 new dialogue(boy, "I am his reflection. What he could have been. Pure. Hard. Unbreakable."),
                 new dialogue(boy, "And you, the strongest keeper of gems in the land, worn out by a child. How convenient."))
            .walk(boy, 2, 2, 2, 3)
            .run(() -> crystalBurst(girl))
            .sayWhile(() -> zap(boy, girl), new dialogue(girl, "What are you... my legs, they are moving on their own!"))
            .say(new dialogue(boy, "She will be the jewel of my castle, north beyond the caverns."),
                 new dialogue(boy, "Come and take her back, if you can get past my servants."))
            //The mirror opens and they walk into it, him first and her after him, round her and up to it
            .run(() -> {mirrorOpen = true; mirrorFlash.run();})
            .walkThrough(boy, 1, 0, 0, 0, 1, 1)
            .run(() -> {mirrorFlash.run(); boy.placeAt(boy.getHomeWorld(), boy.getHomeX(), boy.getHomeY());})
            .walkThrough(girl, 1, 1, 0, 0, 0, 1, 1)
            .run(() -> {mirrorFlash.run(); double[] held = heldGirl(); girl.placeAt(HALL_WORLD, held[0], held[1]);})
            .wait(0.4)
            //Then it blows apart behind them
            .run(() -> {mirrorOpen = false; mirrorBlast(2, mirrorX, mirrorY);})
            .wait(1.2)
            //And the portal behind where she stood lights up
            .run(() -> {portalHeld = false; portalBurst();})
            .wait(0.8)
            .say(new dialogue(getMainCharacter(), "!!!"))
            .ending(() -> {mirrorOpen = false; portalHeld = false; boy.setGhost(false); girl.setGhost(false); takeHer.run();});
    }

    //The mirror in her room (tile 194, mirrorType): hidden until she is beaten, then it appears (mirrorShown, set by
    //kidnapping()), open while they walk into it (mirrorOpen), and gone after. Set every frame from the story, so a
    //savefile needs nothing of its own for it: gone once the game is finished and she is no longer in her room
    public static final int MIRROR_TILE = 194;
    private boolean mirrorOpen = false;
    private boolean mirrorShown = false;
    private void updateMirror () {
        mirrorType.state state = mirrorOpen ? mirrorType.state.OPEN
            : girlTaken() ? mirrorType.state.GONE
            : mirrorShown || (playerIsReflection() && !loopMirrorHidden) ? mirrorType.state.MIRROR : mirrorType.state.HIDDEN;
        ((mirrorType) tileTypes.get(MIRROR_TILE)).setState(state);
        ((portalType) tileTypes.get(SEALED_PORTAL_TILE)).setLit(portalLit());
        ((skyPortalType) tileTypes.get(SKY_PORTAL_TILE)).setLit(skyPortalLit());
        //The two mirrors where he blew up: hidden until then; hers open while she runs into it and gone after, yours open
        boolean twins = crystalBoyBeaten(mirrorFight) && !twinsHeld;
        boolean herGone = characters.get("Girl (Master)").getWorld() != MIRROR_WORLD;
        ((mirrorType) tileTypes.get(GIRLS_MIRROR_TILE)).setState(!twins ? mirrorType.state.HIDDEN
            : girlsMirrorOpen ? mirrorType.state.OPEN : herGone ? mirrorType.state.GONE : mirrorType.state.MIRROR);
        ((mirrorType) tileTypes.get(YOUR_MIRROR_TILE)).setState(twins ? mirrorType.state.OPEN : mirrorType.state.HIDDEN);
    }
    public static final int SEALED_PORTAL_TILE = 172, GIRLS_MIRROR_TILE = 197, YOUR_MIRROR_TILE = 198, SKY_PORTAL_TILE = 185;
    private boolean twinsHeld = false, girlsMirrorOpen = false;

    //The player, turned into the crystal boy: his sprite, white eyes
    public static final int REFLECTION_X = 1200, REFLECTION_Y = 1800;
    public boolean playerIsReflection () {return getMainCharacter().looksLike(REFLECTION_X, REFLECTION_Y);}

    //The girl beaten and taken out of her room by the crystal boy
    private boolean girlTaken () {
        character girl = characters.get("Girl (Master)");
        //Gone through a mirror counts as out of it
        boolean inHerRoom = girl.getWorld() == 2 && (int) Math.floor(girl.getY()) == 9959 && !girl.isGone();
        return isFinished() && !inHerRoom;
    }

    //The 3x3 portal behind her (172) lights up, and lets you through, once she is taken and the mirror is gone. During
    //the kidnapping it waits for its own moment (portalHeld), when it lights in a burst of sparks (portalBurst)
    private boolean portalHeld = false;
    public boolean portalLit () {return girlTaken() && !portalHeld;}

    //The portal lighting: sparks bursting out of each of its nine tiles, the most from the middle. Its bottom row is
    //the top row of her room, the two above in the chunk north of it
    private void portalBurst () {
        portalBurst(2, 10010, 9959);
    }

    //A 3x3 portal lighting up: its bottom row is row 0 of chunk (x, y), columns 3 to 5, the two rows above in the chunk
    //north of it (the girl's portal, the desert's sky portal)
    private void portalBurst (int world, int x, int y) {
        double t = 1.0/getTileCount();
        for (int r = -2; r <= 0; r++) {
            for (int c = 3; c <= 5; c++) {
                boolean middle = r == -1 && c == 4;
                burst(x + (c + 0.5)*t, y + (r + 0.5)*t, world, middle ? 140 : 40, CRYSTAL);
            }
        }
    }

    //The sky portals up into the sky ruins (185) are sand until the Desert Nomad, the desert's guard right below the
    //desert's, is beaten; it lights when he has been taken away (stoning), in a burst like the girl's portal
    private boolean skyPortalHeld = false;
    public boolean skyPortalLit () {
        character nomad = characters.get("Desert Nomad");
        boolean fighting = battleHandler.isActive() && battleHandler.getThem() == nomad;
        return !fightPending(nomad) && !fighting && !skyPortalHeld;
    }

    //The mirror blowing apart: shards of its glass and its gold frame flying out, with crystal sparks
    private static final String[] MIRROR_SHARDS = {"#8EDCF2", "#A0D0E0", "#F8F8FF", "#C8A020", "#E0C060"};
    private void mirrorBlast (int world, double x, double y) {
        double cx = x + 0.5/getTileCount(), cy = y + 0.5/getTileCount();
        burst(cx, cy, world, 180, MIRROR_SHARDS);
        burst(cx, cy, world, 80, CRYSTAL);
    }

    //The mirror dimension (world 5, chunks.mirror()): the starting village mirrored (tools/maps_postgame.py). Every
    //mirror leads there or back: the one behind the crystal boy in his hall (195) to the arrival, in front of the
    //mirrored house 1, whose front door is the mirror back (196) to the tile in front of the hall's mirror
    public static final int MIRROR_WORLD = 5;
    private double[] mirrorArrival () {return new double[] {10002 + 4.0/getTileCount(), 10000 + 7.0/getTileCount()};}
    private double[] mirrorBoss () {return new double[] {10002 + 4.0/getTileCount(), 9995 + 4.0/getTileCount()};}
    private double[] hallMirrorFront () {return new double[] {10010 + 4.0/getTileCount(), 9942 + 1.0/getTileCount()};}
    private eventListener mirrorFight;

    //Whether a fight with the crystal boy is won: its listener has fired (which it does when the fight starts), the fight
    //is not still going on, and there is no lost fight in that world waiting to be tried again
    private boolean crystalBoyBeaten (eventListener fight) {
        if (getActiveListeners().contains(fight)) return false;
        character boy = characters.get("Crystal Boy");
        if (battleHandler.isActive() && battleHandler.getThem() == boy && boy.getWorld() == fight.getWorld()) return false;
        for (eventListener l : eventListeners) {
            if (l.getEvent() instanceof battleEvent && ((battleEvent) l.getEvent()).getThem() == characters.get("Crystal Boy")
                && l.getWorld() == fight.getWorld()) return false;
        }
        return true;
    }

    //Your mother and grandma: when he flees into the mirror dimension (beaten in his castle) he takes them with him, and
    //they stand there in stone (the statues), gone from home. When he is beaten for good they come home, and the
    //statues are gone. Worked out from the fights, so a savefile needs nothing of its own for it
    private void updateFamily () {
        boolean taken = crystalBoyBeaten(crystalBoyFight) && !crystalBoyBeaten(mirrorFight);
        boolean freed = crystalBoyBeaten(mirrorFight);
        characters.get("Mother").setGone(taken);
        characters.get("Grandma").setGone(taken);
        characters.get("Mother (statue)").setGone(freed);
        characters.get("Grandma (statue)").setGone(freed);
    }

    public void intoMirrorDimension () {
        double[] at = mirrorArrival();
        teleportWithEffect(MIRROR_WORLD, at[0], at[1], CRYSTAL);
    }

    public void outOfMirrorDimension () {
        double[] at = hallMirrorFront();
        teleportWithEffect(HALL_WORLD, at[0], at[1], CRYSTAL);
    }

    //The crystal boy beaten in his hall: he flees through the mirror behind him (and forgets to take it with him, so
    //it stays, and you can follow). The girl is free, and comes along
    private cutscene escape () {
        character girl = characters.get("Girl (Master)"), boy = characters.get("Crystal Boy"), me = getMainCharacter();
        double[] boss = mirrorBoss();
        Runnable intoDimension = () -> {
            boy.restore();
            boy.placeAt(MIRROR_WORLD, boss[0], boss[1]);
        };
        return new cutscene(this)
            .say(new dialogue(boy, "Impossible... beaten by my own reflection..."),
                 new dialogue(boy, "This is not over. Follow me, if you dare!"))
            //Far away at home, he takes your mother and grandma with him
            .run(() -> {
                crystalBurst(characters.get("Mother"));
                crystalBurst(characters.get("Grandma"));
                updateFamily();
            })
            //Up through the mirror room behind him and into the mirror in its north wall
            .walkThrough(boy, 1, 1, 1, 1, 1, 1)
            .run(() -> {crystalBurst(boy); intoDimension.run();})
            .wait(0.8)
            //Her niche lets her go
            .run(() -> crystalBurst(girl))
            .walk(girl, 2)
            .say(new dialogue(girl, "You came all this way. For me. After everything I did to you."),
                 new dialogue(me, "\u2713\u2713\u2713 \u263A"),
                 new dialogue(girl, "He went into the mirror. If he gets away, none of this is over."),
                 new dialogue(girl, "I'm coming with you."))
            .run(() -> girl.follow(me))
            .ending(() -> {
                boy.setGhost(false);
                if (boy.getWorld() != MIRROR_WORLD) intoDimension.run();
                girl.follow(me);
                updateFamily();
            });
    }

    //The first time through the mirror: the camera goes from your mother's statue to grandma's, you can only say so much,
    //and the girl says what she thinks of the place in between
    private cutscene mirrorWelcome () {
        character girl = characters.get("Girl (Master)"), me = getMainCharacter();
        character mother = characters.get("Mother (statue)"), grandma = characters.get("Grandma (statue)");
        return new cutscene(this)
            .wait(1.0)
            .say(new dialogue(girl, "This is... your village? Everything is the wrong way round. And so cold."))
            .look(mother)
            .wait(1.4)
            .say(new dialogue(me, "!!!"),
                 new dialogue(girl, "Is that... your mother? Turned to stone?"),
                 new dialogue(me, ")):"),
                 new dialogue(girl, "He copies everything he looks at, and freezes it. That's all this place is to him: a collection."))
            .look(grandma)
            .wait(1.4)
            .say(new dialogue(me, "!!!"),
                 new dialogue(girl, "Your grandma too. Standing outside her own house, forever."),
                 new dialogue(me, ")):"),
                 new dialogue(girl, "This place is horrible. Nothing grows, nothing moves, nobody is home. Just glass and cold."))
            .lookBack()
            .wait(0.8)
            .say(new dialogue(girl, "Let's find him and end this. I don't want to stay here a moment longer than we have to."));
    }

    //The stone guardian beaten: it crumbles away, out of the gap in the wall
    private cutscene guardianCrumbles () {
        character guardian = characters.get("Stone Guardian");
        Runnable crumble = () -> {
            stoneBurst(guardian);
            crystalBurst(guardian);
            guardian.setGone(true);
        };
        return new cutscene(this)
            .say(new dialogue(guardian, "The master... will not... be..."))
            .run(crumble)
            .wait(0.8)
            .ending(() -> guardian.setGone(true));
    }

    //A bolt of lightning from (x, y) to (toX, toY) in a world, in the colour
    private void strike (int world, double x, double y, double toX, double toY, String colour) {
        double px = (toX - x)*getUnitWidth(), py = (toY - y)*getUnitHeight();
        double tilePx = getUnitWidth()/getTileCount();
        guiLightning bolt = new guiLightning(0, 0, Math.atan2(py, px), Math.hypot(px, py), Math.max(1, Math.round(tilePx*0.05)),
            6 + strikeRandom.nextInt(4), Color.web(colour), strikeRandom);
        worldStrikes.add(new worldLightning(world, x, y, bolt, getUnitWidth()));
    }

    //One frame of the reflection holding her: now and then a crystal bolt of lightning from him into her (while she
    //says her legs are moving on their own)
    private static final String[] HOLD = {"#FFFFFF", "#D8F4FF", "#B4A8FF"};
    private void zap (character from, character to) {
        if (strikeRandom.nextDouble() >= 0.07) return;
        double t = 1.0/getTileCount();
        strike(from.getWorld(), from.getX() + 0.5*t, from.getY() + 0.3*t,
            to.getX() + (0.3 + strikeRandom.nextDouble()*0.4)*t, to.getY() + (0.2 + strikeRandom.nextDouble()*0.5)*t, HOLD[strikeRandom.nextInt(HOLD.length)]);
    }

    //One frame of everything his stones have, flying from him at you: purple (meteorite) and yellow (boltstone)
    //lightning, water and lava, and now and then an explosion bursting out of you
    private static final String[] STRIKES = {"#887BFE", "#B4A8FF", "#FFE500", "#FFFA00"};
    private static final String[] WATER_AND_LAVA = {"#3887DC", "#79BCCE", "#A0D0E0", "#F21C00", "#FC9F00", "#FFAF30"};
    private static final String[] EXPLOSION = {"#FF0800", "#FC9F00", "#FFFA00", "#FFFFFF", "#6E6E6E"};
    private void stormAt (character from, character me) {
        java.util.Random rdm = strikeRandom;
        double t = 1.0/getTileCount();
        double cx = me.getX() + 0.5*t, cy = me.getY() + 0.3*t;
        double fx = from.getX() + 0.5*t, fy = from.getY() + 0.3*t;
        int world = me.getWorld();
        for (int i = 0; i < 7; i++) {
            //From his hands, a little spread, all the way into you over its life
            double sx = fx + (rdm.nextDouble() - 0.5)*0.4*t, sy = fy + (rdm.nextDouble() - 0.5)*0.4*t;
            double size = 0.01 + rdm.nextDouble()*0.012;
            double ex = cx + (rdm.nextDouble() - 0.5)*0.3*t, ey = cy + (rdm.nextDouble() - 0.5)*0.3*t;
            addFrontParticle(new coloredParticle(sx, sy, world, 0.35 + rdm.nextDouble()*0.25, 1, size, size, -0.3, this,
                ex - sx, ey - sy, WATER_AND_LAVA[rdm.nextInt(WATER_AND_LAVA.length)]));
        }
        if (rdm.nextDouble() < 0.4) strike(world, fx, fy, cx + (rdm.nextDouble() - 0.5)*0.4*t, cy + (rdm.nextDouble() - 0.5)*0.4*t, STRIKES[rdm.nextInt(STRIKES.length)]);
        if (rdm.nextDouble() < 0.12) burst(cx, cy, world, 30, EXPLOSION);
    }

    //The crystal boy beaten for good, in the mirror dimension. He steps back from you, and everything he has comes
    //flying at you while he, you and the girl speak (until you have read it all); then he blows up into two mirrors, and
    //as he does you turn into him: white eyes. The girl sees your eyes, runs into one of the mirrors, and it is gone;
    //the other one is yours
    private cutscene becoming () {
        character girl = characters.get("Girl (Master)"), boy = characters.get("Crystal Boy"), me = getMainCharacter();
        double t = 1.0/getTileCount();
        double[] boss = mirrorBoss();
        //Where he steps back to: three tiles behind where he waited. The two mirrors stand on each side of it (the map
        //has them there, tiles 197 and 198)
        double backX = boss[0], backY = boss[1] - 3*t;
        double girlsX = backX - t, yoursX = backX + t;
        double[] home = girlHome();
        Runnable intoHerMirror = () -> {
            girl.follow(null);
            girl.setGhost(false);
            girl.placeAt(HALL_WORLD, home[0], home[1]);
        };
        return new cutscene(this)
            .run(() -> twinsHeld = true)
            .say(new dialogue(boy, "No... I am the reflection. I can not lose to the real thing..."))
            //He steps back from you
            .walkThroughTo(boy, backX, backY)
            .wait(0.4)
            //And everything he has comes at you, until all three have spoken
            .sayWhile(() -> stormAt(boy, me),
                 new dialogue(boy, "...unless the real thing becomes me."),
                 new dialogue(me, "!!!"),
                 new dialogue(girl, "What is he doing?!"))
            //It ends in an explosion: you turn into him, and he blows up into two mirrors, one on each side of him
            .run(() -> {
                burst(me.getX() + 0.5*t, me.getY() + 0.3*t, me.getWorld(), 160, EXPLOSION);
                me.setSprite(REFLECTION_X, REFLECTION_Y);
                mirrorBlast(boy.getWorld(), boy.getX(), boy.getY());
                crystalBurst(boy);
                boy.setGone(true);
                twinsHeld = false;
                burst(girlsX + 0.5*t, backY + 0.5*t, MIRROR_WORLD, 80, CRYSTAL);
                burst(yoursX + 0.5*t, backY + 0.5*t, MIRROR_WORLD, 80, CRYSTAL);
                updateFamily();
            })
            .wait(1.4)
            .say(new dialogue(girl, "It's over... He's gone for good."),
                 new dialogue(girl, "...Look at me."),
                 new dialogue(girl, "Your eyes. They're white. Just like his."),
                 new dialogue(me, "!!!"),
                 new dialogue(girl, "No... Stay away from me!"))
            //She runs into hers, and it is gone
            .run(() -> {girl.follow(null); girlsMirrorOpen = true;})
            .walkThroughTo(girl, girlsX, backY)
            .run(() -> {
                crystalBurst(girl);
                intoHerMirror.run();
                girlsMirrorOpen = false;
                mirrorBlast(MIRROR_WORLD, girlsX, backY);
            })
            .wait(1.0)
            .ending(() -> {
                boy.setGone(true);
                boy.setGhost(false);
                me.setSprite(REFLECTION_X, REFLECTION_Y);
                twinsHeld = false;
                girlsMirrorOpen = false;
                if (girl.getWorld() == MIRROR_WORLD) intoHerMirror.run();
                updateFamily();
            });
    }

    //Where the girl stood when you beat her: her place in her room
    private double[] girlHome () {
        character girl = characters.get("Girl (Master)");
        return new double[] {girl.getHomeX(), girl.getHomeY()};
    }

    //Stepping onto your mirror: the scene starts right there, nothing blacked out. You go into the mirror, and the
    //camera is in her room just after you beat her there; the mirror on her wall appears, and you step out of it, as
    //he did. You are the reflection now, and it begins again
    public void intoTheLoop () {
        if (!playerIsReflection() || inCutsceneNow()) return;
        play(theLoop());
    }
    private boolean inCutsceneNow () {return playing != null;}

    //While the loop scene has not yet shown the mirror on her wall
    private boolean loopMirrorHidden = false;

    private cutscene theLoop () {
        character girl = characters.get("Girl (Master)"), past = characters.get("You (past)"), me = getMainCharacter();
        double t = 1.0/getTileCount();
        double[] home = girlHome();
        double mirrorX = 10010 + 1*t, mirrorY = 9959 + 0*t;
        Runnable mirrorFlash = () -> burst(mirrorX + 0.5*t, mirrorY + 0.5*t, HALL_WORLD, 60, CRYSTAL);
        //Her room as it was just after you beat her: her where she stood, the past you below her
        Runnable room = () -> {
            past.setGone(false);
            past.placeAt(2, past.getHomeX(), past.getHomeY());
            girl.follow(null);
            girl.placeAt(HALL_WORLD, home[0], home[1]);
        };
        return new cutscene(this)
            //Into your mirror
            .run(() -> {
                crystalBurst(me);
                me.setGone(true);
                loopMirrorHidden = true;
                room.run();
                //Inside the mirror on her wall, still hidden; the camera is there at once (it would slide all the way)
                me.placeAt(HALL_WORLD, mirrorX, mirrorY);
                getCamera().setX(mirrorX + 0.5*t);
                getCamera().setY(mirrorY + 2.5*t);
                this.world = HALL_WORLD;
            })
            .wait(1.2)
            .say(new dialogue(girl, "...Fine. You win."))
            .wait(0.6)
            //The mirror appears on her wall
            .run(() -> {loopMirrorHidden = false; mirrorFlash.run();})
            .wait(0.5)
            .say(new dialogue(girl, "A mirror...?"))
            //And you step out of it, over the niche in front of it, as he did
            .run(() -> {mirrorFlash.run(); me.setGone(false); crystalBurst(me);})
            .walkThrough(me, 3, 3)
            .say(new dialogue(me, "Not so fast."),
                 new dialogue(girl, "Who...? He came out of the mirror... he has your face!"),
                 new dialogue(me, "I am his reflection. What he could have been. Pure. Hard. Unbreakable."))
            //Up to her, as he did, and she is frozen in crystal
            .walk(me, 2, 2, 2, 3)
            .run(() -> crystalBurst(girl))
            .sayWhile(() -> zap(me, girl), new dialogue(girl, "What are you... my legs, they are moving on their own!"))
            .say(new dialogue(me, "Come and take her back, if you can."))
            //The mirror opens, and you take her through it, as he did: you first, her after you
            .run(() -> {portalHeld = true; mirrorOpen = true; mirrorFlash.run();})
            .walkThrough(me, 1, 0, 0, 0, 1, 1)
            .run(() -> {mirrorFlash.run(); me.setGone(true);})
            .walkThrough(girl, 1, 1, 0, 0, 0, 1, 1)
            .run(() -> {mirrorFlash.run(); girl.setGone(true);})
            .wait(0.4)
            //Then it blows apart behind you, and the portal behind where she stood lights up
            .run(() -> {mirrorOpen = false; mirrorBlast(HALL_WORLD, mirrorX, mirrorY);})
            .wait(1.2)
            .run(() -> {portalHeld = false; portalBurst();})
            .wait(1.0)
            .say(new dialogue("Game", "And so it begins again."),
                 new dialogue("Game", "The End"))
            .run(this::quitToMenu)
            //You and she are through the mirror: the next time the save is loaded, you are where it ends up (afterLoad)
            .ending(() -> {
                room.run();
                girl.setGone(true);
                me.setGone(true);
                me.setGhost(false);
                girl.setGhost(false);
                loopMirrorHidden = false;
                mirrorOpen = false;
                portalHeld = false;
            });
    }

    //Where the loop ends up, as it was before: you waiting in his place in the castle hall (the doorway of the mirror
    //room), her held in the niche beside you. Her room's mirror is gone and its portal lit again
    private void afterTheLoop () {
        character girl = characters.get("Girl (Master)"), boy = characters.get("Crystal Boy"), me = getMainCharacter();
        double[] held = heldGirl();
        girl.setGone(false);
        girl.placeAt(HALL_WORLD, held[0], held[1]);
        me.setGone(false);
        me.placeAt(HALL_WORLD, boy.getHomeX(), boy.getHomeY());
    }

    //A guard standing in the way: fights the player who comes close, and when beaten steps up and to the left,
    //out of the one tile wide way to the gate behind it
    private void guardFight (character guard, List<dialogue> lines) {
        guardFight(guard, Arrays.asList(1, 0), lines);
    }

    //A guard after the final boss (one of the crystal boy's servants). after: what it says when beaten, before it steps aside
    //It does not step aside like other guards: beaten, it is turned to stone and taken away like every servant, which
    //opens the way it stood in
    private void servantGuard (character guard, List<dialogue> lines, List<dialogue> after) {
        servantFight(guard, lines, after);
    }

    //A fight with one of the crystal boy's servants after the final boss: lines before the fight; beaten, the
    //stoning() cutscene plays with after as its few lines (the first servant beaten gets the long version instead)
    private void servantFight (character c, List<dialogue> lines, List<dialogue> after) {
        event beaten = () -> play(stoning(c, after));
        event battle = new battleEvent(getMainCharacter(), c, battleHandler, beaten, new teleportEvent(getMainCharacter()), c.getX(), c.getY(), 0.15, c.getWorld());
        eventListener listener = new eventListener(this, c.getX(), c.getY(), 0.15, c.getWorld(), battle, lines);
        eventListeners.add(listener);
        servantFights.put(c, listener);
    }
    private final java.util.Map<character, eventListener> servantFights = new java.util.LinkedHashMap<character, eventListener>();

    //Where each servant stands as a statue once beaten: on the grass beside the avenue up to the castle (the 's' in the
    //castle grounds grid in tools/maps_postgame.py), in the order you meet them, the first nearest the cave
    private double[] statueSpot (character c) {
        double t = 1.0/getTileCount();
        switch (c.getId()) {
            case "Ember Wraith":     return new double[] {10010 + 2*t, 9948 + 4*t};
            case "Ash Golem":        return new double[] {10010 + 6*t, 9948 + 0*t};
            case "Dune Scorpion":    return new double[] {10010 + 2*t, 9947 + 5*t};
            case "Desert Nomad":     return new double[] {10010 + 6*t, 9947 + 1*t};
            case "Storm Harpy":      return new double[] {10010 + 2*t, 9946 + 6*t};
            case "Cloud Knight":     return new double[] {10010 + 6*t, 9946 + 2*t};
            case "Crystal Sentinel": return new double[] {10010 + 2*t, 9945 + 7*t};
            default:                 return new double[] {10010 + 6*t, 9945 + 3*t}; //Prism Warden
        }
    }
    private static final int STATUE_WORLD = 0;

    //Whether there is still a fight against this character to come: its own, or one to retry after losing it
    private boolean fightPending (character c) {
        for (eventListener l : eventListeners) {
            if (l.getEvent() instanceof battleEvent && ((battleEvent) l.getEvent()).getThem() == c) return true;
        }
        return false;
    }

    //Stone grey sparks: a servant turning to stone
    private static final String[] STONE = {"#6E6E6E", "#808088", "#A0A0A0", "#C8C8C8"};
    private void stoneBurst (character c) {
        burst(c.getX() + 0.5/getTileCount(), c.getY() + 0.5/getTileCount(), c.getWorld(), 110, STONE);
    }

    //Turned to stone and put in its place among the ruins
    private void toStatue (character c) {
        double[] spot = statueSpot(c);
        c.setStatue(true);
        c.placeAt(STATUE_WORLD, spot[0], spot[1]);
    }

    //A beaten servant pays for failing the crystal boy: his voice from the mirror, it turns to stone (grey sparks, then
    //grey) and the crystal sparks take it away, to stand as a statue on the way up to his castle. The first servant
    //beaten gets the whole scene; the others only say their own few lines (after) first
    private cutscene stoning (character c, List<dialogue> after) {
        character boy = characters.get("Crystal Boy");
        boolean first = servantFights.keySet().stream().noneMatch(character::isStatue);
        cutscene scene = new cutscene(this);
        //The Desert Nomad guards the desert's sky portal: it lights when he is gone (skyPortalLit)
        boolean nomad = c == characters.get("Desert Nomad");
        if (nomad) scene.run(() -> skyPortalHeld = true);
        if (first) {
            scene.say(new dialogue(c, "Oh... you beat me. The master won't like this..."))
                .run(() -> crystalBurst(c))
                .wait(0.6)
                .say(new dialogue(c, "Oh no... what is he..."),
                     new dialogue(boy, "You failed me! This will not go unpunished."))
                .run(() -> crystalBurst(c))
                .wait(0.6)
                .say(new dialogue(c, "Uh oh..."));
        } else {
            scene.say(after.toArray(new dialogue[0]));
        }
        scene.run(() -> {crystalBurst(c); stoneBurst(c);})
            .wait(0.8)
            .run(() -> {stoneBurst(c); c.setStatue(true);})
            .wait(0.6);
        if (first) scene.say(new dialogue(c, "Help..."));
        scene.run(() -> {crystalBurst(c); toStatue(c);})
            .wait(0.5);
        if (nomad) scene.run(() -> {skyPortalHeld = false; portalBurst(0, 10010, 9954);}).wait(0.8);
        return scene.ending(() -> {
            toStatue(c);
            if (nomad) skyPortalHeld = false;
        });
    }

    //aside: the steps out of the way (movementEvent directions: 0 left, 1 up, 2 right, 3 down)
    private void guardFight (character guard, List<Integer> asideSteps, List<dialogue> lines) {
        guardFight(guard, asideSteps, lines, null);
    }

    private void guardFight (character guard, List<Integer> asideSteps, List<dialogue> lines, List<dialogue> after) {
        event step = new movementEvent(this, guard, asideSteps, true, 2);
        event aside = after == null ? step : () -> {after.forEach(dialogueContainer::add); step.trigger();};
        event battle = new battleEvent(getMainCharacter(), guard, battleHandler, aside, new teleportEvent(getMainCharacter()), guard.getX(), guard.getY(), 0.15, guard.getWorld());
        eventListener listener = new eventListener(this, guard.getX(), guard.getY(), 0.15, guard.getWorld(), battle, lines);
        eventListeners.add(listener);
        guards.add(new guardAside(guard, listener, asideSteps));
    }

    //Every guard with its fight and the steps it takes aside when beaten
    private static class guardAside {
        final character guard;
        final eventListener listener;
        final List<Integer> steps;
        guardAside (character guard, eventListener listener, List<Integer> steps) {
            this.guard = guard;
            this.listener = listener;
            this.steps = steps;
        }
    }
    private final List<guardAside> guards = new ArrayList<guardAside>();

    //After loading: a guard the code has moved since the save was made is back at its (new) home, standing in the way.
    //If its fight was already won it goes straight to where it would have stepped aside to, so the way is open
    public void afterLoad () {
        placeBeatenGuards();
        //Saved at the very end, you and she through the mirror on her wall: where the loop ends up
        if (playerIsReflection() && getMainCharacter().isGone()) afterTheLoop();
        updateFamily();
        for (character c : characters.getCharacters()) {
            String name = c.getSavedLeader();
            if (name != null && !name.isEmpty()) c.follow(findCharacter(name));
        }
        //Beaten in his hall in a save from before the mirror dimension (he shattered there then): he fled into it instead,
        //and she comes along
        character boy = characters.get("Crystal Boy"), girlFreed = characters.get("Girl (Master)");
        if (boy.isGone() && !getActiveListeners().contains(crystalBoyFight) && getActiveListeners().contains(mirrorFight)) {
            boy.setGone(false);
            boy.restore();
            double[] boss = mirrorBoss();
            boy.placeAt(MIRROR_WORLD, boss[0], boss[1]);
            if (girlFreed.getLeader() == null) girlFreed.follow(getMainCharacter());
        }
        //Servants beaten in a save from before they were turned to stone: statues all the same
        for (character servant : servantFights.keySet()) {
            if (!fightPending(servant) && !servant.isStatue()) toStatue(servant);
        }
        //Beaten before the crystal boy was there to take her (a save from before him): she is in his hall all the same
        character girl = characters.get("Girl (Master)");
        double[] held = heldGirl();
        //In her niche (an older save can have her where the niche used to be)
        double half = 0.5/getTileCount();
        boolean inHall = girl.getWorld() == HALL_WORLD && Math.abs(girl.getX() - held[0]) < half && Math.abs(girl.getY() - held[1]) < half;
        if (isFinished() && getActiveListeners().contains(crystalBoyFight) && !inHall) girl.placeAt(HALL_WORLD, held[0], held[1]);
    }

    private void placeBeatenGuards () {
        double t = 1.0/getTileCount();
        for (guardAside g : guards) {
            if (!g.guard.wasMovedHome() || getActiveListeners().contains(g.listener)) continue;
            double x = g.guard.getHomeX(), y = g.guard.getHomeY();
            for (int step : g.steps) {
                if (step == 0) x -= t;
                if (step == 1) y -= t;
                if (step == 2) x += t;
                if (step == 3) y += t;
            }
            g.guard.placeAt(g.guard.getHomeWorld(), x, y);
        }
    }

    //The names picked for the player and the girl when the campaign was started. The girl is both the
    //first fight and the final boss, so both of them get her name
    public static final String PLAYER_NAME = "names.player";
    public static final String GIRL_NAME = "names.girl";
    private static final List<String> GIRLS = Arrays.asList("Girl (Beginner)", "Girl (Master)");

    private void readNames (saveFile save) {
        String player = save.getString(PLAYER_NAME, "").trim();
        if (!player.isEmpty()) getMainCharacter().setDisplayName(player);
        String girl = save.getString(GIRL_NAME, "").trim();
        if (!girl.isEmpty()) {
            for (String id : GIRLS) characters.get(id).setDisplayName(girl);
        }
    }

    //Only names that were picked; the rest keep what the code calls them
    public void writeNames (saveFile save) {
        character player = getMainCharacter(), girl = characters.get(GIRLS.get(0));
        if (!player.getName().equals(player.getId())) save.set(PLAYER_NAME, player.getName());
        if (!girl.getName().equals(girl.getId())) save.set(GIRL_NAME, girl.getName());
    }

    //Every chest in every world, see chest
    private List<chest> chests = new ArrayList<chest>();
    public List<chest> getChests () {return new ArrayList<chest>(chests);}

    //The chest on the tile at (x, y) in that world, or null
    public chest chestAt (int world, double x, double y) {
        tile t = getWorld(world).getTile(x, y);
        for (chest c : chests) {
            if (c.getPlace().getWorld() == world && c.getPlace().getTile() == t) return c;
        }
        return null;
    }

    //The chest on that tile of the world being shown, or null
    public chest chestOn (tile t) {
        for (chest c : chests) {
            if (c.getPlace().getWorld() == getCurrentWorld() && c.getPlace().getTile() == t) return c;
        }
        return null;
    }

    //How much of the brightened chest (CHEST_BRIGHTEN of the way to white, texture kept) is laid over the chest on tile
    //(tileX, tileY) (in tiles, as tiles count them): while the player stands on one of the 8 tiles around it, so it can be
    //opened, it fades in and out once every CHEST_PULSE seconds, starting from nothing when the player gets there. 0 otherwise
    public static final double CHEST_BRIGHTEN = 0.35, CHEST_PULSE = 1.0;
    private int nearChestX, nearChestY;
    private long nearChestSince = 0;

    public double chestHighlight (int tileX, int tileY) {
        character player = getMainCharacter();
        if (inMenu() || player.getWorld() != getCurrentWorld()) return 0;
        int tC = getTileCount();
        long dx = Math.round(player.getX()*tC) - tileX, dy = Math.round(player.getY()*tC) - tileY;
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || (dx == 0 && dy == 0)) {
            if (nearChestSince != 0 && nearChestX == tileX && nearChestY == tileY) nearChestSince = 0;
            return 0;
        }
        //The pulse starts at nothing when the player first comes next to this chest
        if (nearChestSince == 0 || nearChestX != tileX || nearChestY != tileY) {
            nearChestSince = System.nanoTime();
            nearChestX = tileX;
            nearChestY = tileY;
        }
        double t = (System.nanoTime() - nearChestSince)/1e9;
        //Half the pulse fading in, half fading out, on a mirrored curve. The eye takes small changes in faint light as
        //big ones, so the curve is put through a gamma (2.2, like screens use) to make the change look even both ways
        double even = 0.5 - 0.5*Math.cos(2*Math.PI*t/CHEST_PULSE);
        return Math.pow(even, 2.2);
    }

    //A click on the world at screen (mx, my): opens the chest there when the player stands on one of the 8 tiles
    //around it. True when the click was used
    public boolean clickWorld (double mx, double my) {
        if (inMenu() || battleHandler.isActive() || transition.isActive() || dialogueContainer.isActive()) return false;
        double wx = camera.getX() + (mx - getMidWIDTH())/getUnitWidth();
        double wy = camera.getY() + (my - getMidHEIGHT())/getUnitHeight();
        if (wx < 0 || wy < 0) return false;
        //The tile the click is on: getTile rounds to the nearest tile corner (made for characters, who stand on them),
        //so the click is moved to the corner of its own tile first
        int tC = getTileCount();
        wx = (Math.floor(wx*tC) + 0.001)/tC;
        wy = (Math.floor(wy*tC) + 0.001)/tC;
        chest clicked = chestAt(getCurrentWorld(), wx, wy);
        if (clicked == null) return false;
        character player = getMainCharacter();
        long dx = Math.round(player.getX()*tC) - Math.round(clicked.getPlace().getX()*tC);
        long dy = Math.round(player.getY()*tC) - Math.round(clicked.getPlace().getY()*tC);
        if (player.getWorld() != getCurrentWorld() || Math.abs(dx) > 1 || Math.abs(dy) > 1) return false;
        gui.openContainer(clicked, "Chest");
        return true;
    }

    //Every listener made above, in the order made. Savefiles name them by their key: world, place and radius,
    //plus a count for listeners on the same spot. That stays the same when listeners are added elsewhere
    private List<eventListener> campaignListeners = new ArrayList<eventListener>();

    private void nameCampaignListeners () {
        java.util.Map<String, Integer> seen = new java.util.HashMap<String, Integer>();
        for (eventListener l : eventListeners) {
            String spot = l.getWorld() + ":" + l.getX() + ":" + l.getY() + ":" + l.getRadius();
            int n = seen.merge(spot, 1, Integer::sum);
            l.setKey(spot + "#" + n);
        }
        campaignListeners = new ArrayList<eventListener>(eventListeners);
    }

    public List<eventListener> getCampaignListeners () {return new ArrayList<eventListener>(campaignListeners);}
    public List<eventListener> getActiveListeners () {return new ArrayList<eventListener>(eventListeners);}
    public void setActiveListeners (List<eventListener> listeners) {eventListeners = new ArrayList<eventListener>(listeners);}

    public character findCharacter (String name) {return characters.find(name);}
    public characterContainer getCharacterContainer () {return this.characters;}

    //Puts the camera, the shown world and its size straight on the player, for a loaded game to start there
    public void jumpToPlayer () {
        character player = getMainCharacter();
        camera.setX(player.getX() + 0.5/getTileCount());
        camera.setY(player.getY() + 0.5/getTileCount());
        this.world = player.getWorld();
        unitWidth = worlds.get(world).getUnitWidthHeight().get(0);
        unitHeight = worlds.get(world).getUnitWidthHeight().get(1);
    }

    //Play time over every session of the campaign
    private double playedBefore = 0;
    private long sessionStart = System.nanoTime();
    public double getPlayTime () {return playedBefore + (System.nanoTime() - sessionStart)/1e9;}
    public void setPlayTime (double seconds) {
        playedBefore = Math.max(0, seconds);
        sessionStart = System.nanoTime();
    }

    //Writes the campaign to its savefile. Does nothing behind the main menu
    public void saveGame () {
        if (save == null) return;
        //Never half a cutscene in a savefile
        if (playing != null) {
            playing.finishNow();
            playing = null;
        }
        saveState.write(this, save);
        lastSave = System.nanoTime();
    }

    //The second half of the teleport, the black opening up. For a game switched to from another one.
    //menu is the main menu that was left (null if none): it stays on top of the black for the first half of it,
    //so the player shows up under it before it goes away
    public void openFromBlack (guiElement menu) {
        transition.startFromBlack(minZoom/zoom);
        if (menu != null) {
            gui.setMenuOverlay(menu);
            //Goes on sliding up from where the menu screen left it, the player is already there under it
            gui.setMenuOverlay(null);
        }
    }

    //From the main menu: the screen closes in and goes black, then the campaign starts (and opens from the black)
    public void playWithEffect (saveFile campaign) {
        if (transition.isActive()) return;
        transition.start(null, minZoom/zoom, () -> launcher.play(campaign));
        //The menu starts sliding up halfway through the black, so it is half gone when the player shows up under it
        transition.atBlackShare(0.5, () -> gui.menuLeaves());
    }

    //Saves first, then the first half of the teleport: sparks, the screen closing in and the black, then the menu
    public void quitToMenu () {
        if (transition.isActive()) return;
        saveGame();
        character player = getMainCharacter();
        burst(player.getX() + 0.5/getTileCount(), player.getY() + 0.5/getTileCount(), player.getWorld(), 90);
        transition.start(null, minZoom/zoom, () -> launcher.openMenu());
        //The menu slides down on top of the black while the player is still on it, and is in place for the switch
        transition.atBlackShare(0, () -> gui.setMenuOverlay(gui.makeMenu()));
    }

    //Autosave, so a crash does not lose much
    private static final double AUTOSAVE_SECONDS = 120;
    private long lastSave = System.nanoTime();

    //Where the camera looks behind the main menu: a slow circle over the village outside
    private cordinate menuView = new cordinate() {
        private long start = System.nanoTime();
        private double t () {return (System.nanoTime() - start)/1e9;}
        @Override public double getX () {return 10000.4 + 2.2*Math.sin(t()*0.025);}
        @Override public double getY () {return 9988 + 3.5*Math.cos(t()*0.018);}
        @Override public int getWorld () {return 0;}
    };

    private dialogueContainer dialogueContainer;
    public dialogueContainer getDiealoguecontainer () {return this.dialogueContainer;}

    private characterContainer characters = new characterContainer();
    private List<baseParticle> frontParticles = new ArrayList<baseParticle>();
    private List<baseParticle> behindParticles = new ArrayList<baseParticle>();

    public void addFrontParticle (baseParticle particle) {frontParticles.add(mirrored(particle));}

    //While a tile of a mirror world gives off its particles, they are mirrored round its middle (tile.drawGround sets
    //this, and NaN after): a chimney's smoke comes out of the other side of it and drifts the other way
    private double particleMirror = Double.NaN;
    public void mirrorParticlesAround (double axis) {this.particleMirror = axis;}
    private baseParticle mirrored (baseParticle particle) {
        if (!Double.isNaN(particleMirror)) particle.mirror(particleMirror);
        return particle;
    }

    //Drawn over everything in the world, also over the black of a screen transition (the teleport burst)
    private List<baseParticle> effectParticles = new ArrayList<baseParticle>();
    public void addEffectParticle (baseParticle particle) {effectParticles.add(particle);}

    //The black that closes in and opens up around the player when teleporting
    private screenTransition transition = new screenTransition(this);
    public boolean inTransition () {return transition.isActive();}
    public boolean isLeaving () {return transition.isLeaving();}

    //Teleports the player with the full show: green sparks burst out, the screen closes in as a diamond,
    //the player is moved while it is black, and the screen opens again as a square around the new place
    public void teleportWithEffect (int world, double x, double y) {
        teleportWithEffect(world, x, y, new String[] {"#21FF2C", "#21FF2C", "#8CFF90", "#D8FFD8"});
    }

    //The same with sparks of these colours (crystal ones through a mirror)
    public void teleportWithEffect (int world, double x, double y, String[] sparks) {
        if (transition.isActive()) return;
        character player = getMainCharacter();
        burst(player.getX() + 0.5/getTileCount(), player.getY() + 0.5/getTileCount(), player.getWorld(), 90, sparks);
        transition.start(() -> {
            player.teleport(world, x, y);
            //The shown level normally follows the camera at the start of the next frame. Switch it now, or the player
            //(who is only drawn in the shown level) is missing for the rest of this frame and blinks
            this.world = world;
            //While it is black the size jumps to the new level's, so the zoom in after ends on that level's
            //own zoom instead of zooming to the old level's size first and sliding to the new one after
            if (!battleHandler.isActive()) {
                double oldWidth = unitWidth;
                unitWidth = worlds.get(world).getUnitWidthHeight().get(0);
                unitHeight = worlds.get(world).getUnitWidthHeight().get(1);
                //Keeps the size on screen the same right now, the opening then eases to the new size
                transition.setSizeCarry(oldWidth/unitWidth);
            }
        }, minZoom/zoom);
    }

    //Lightning out of the player. A lightning stone (boltstone) that is equipped strikes now and then while walking
    //(WALK_STRIKE_CHANCE per frame), and one in the inventory that is charged (by copper next to it on the hotbar or in
    //the open inventory, or by shaking it) strikes out of the player too, more the more charged it is
    //(CHARGED_STRIKE_SHARE of its own strike chance in the GUI)
    public static final double WALK_STRIKE_CHANCE = 0.0015, CHARGED_STRIKE_SHARE = 0.12;
    private List<worldLightning> worldStrikes = new ArrayList<worldLightning>();
    private java.util.Random strikeRandom = new java.util.Random();

    private void playerLightning () {
        character player = getMainCharacter();
        if (player.getWorld() != getCurrentWorld() || transition.isActive()) return;

        double chance = 0;
        item source = null;
        gemstone equipped = player.getGemstone();
        if (equipped != null && equipped.getLightningChance() > 0 && player.isMoving()) {
            chance += WALK_STRIKE_CHANCE;
            source = equipped;
        }
        for (item carried : player.getInventory().getItems()) {
            double a = carried.getAgitation();
            if (carried.getLightningChance() <= 0 || a <= 0) continue;
            chance += carried.getLightningChance()*a*a*CHARGED_STRIKE_SHARE;
            source = carried;
        }
        if (source == null || strikeRandom.nextDouble() >= chance) return;

        java.util.Random rdm = strikeRandom;
        int tC = getTileCount();
        double tilePx = getUnitWidth()/tC;
        //From the middle of the body, any way, half a tile to a tile and a half long
        double x = player.getX() + 0.5/tC, y = player.getY() + 0.3/tC;
        double angle = rdm.nextDouble()*2*Math.PI;
        double length = tilePx*(0.5 + rdm.nextDouble());
        Color color = Color.web(source.getLightningColor());
        guiLightning bolt = new guiLightning(0, 0, angle, length, Math.max(1, Math.round(tilePx*0.04)), 6 + rdm.nextInt(4), color, rdm);
        worldStrikes.add(new worldLightning(player.getWorld(), x, y, bolt, getUnitWidth()));

        //A few sparks where it ends
        double endX = x + bolt.getEndX()/getUnitWidth(), endY = y + bolt.getEndY()/getUnitHeight();
        String[] colors = {"#FFFFFF", source.getLightningColor(), source.getLightningColor()};
        for (int i = 0; i < 6; i++) {
            double sparkAngle = rdm.nextDouble()*2*Math.PI;
            double reach = 0.04 + rdm.nextDouble()*0.1;
            double size = 0.005 + rdm.nextDouble()*0.007;
            addFrontParticle(new coloredParticle(endX, endY, player.getWorld(), 0.3 + rdm.nextDouble()*0.4, 0, size, size, -0.9, this,
                Math.cos(sparkAngle)*reach, Math.sin(sparkAngle)*reach, colors[rdm.nextInt(colors.length)]));
        }
    }

    //Green sparks flying out in every direction from (x, y)
    private void burst (double x, double y, int world, int count) {
        burst(x, y, world, count, new String[] {"#21FF2C", "#21FF2C", "#8CFF90", "#D8FFD8"});
    }

    //Sparks of these colours flying out in every direction from (x, y)
    private void burst (double x, double y, int world, int count, String[] colors) {
        java.util.Random rdm = new java.util.Random();
        for (int i = 0; i < count; i++) {
            double angle = rdm.nextDouble()*2*Math.PI;
            double reach = 0.08 + rdm.nextDouble()*0.25;
            double size = 0.008 + rdm.nextDouble()*0.014;
            addEffectParticle(new coloredParticle(x, y, world, 0.6 + rdm.nextDouble()*0.7, 0, size, size, -0.9, this,
                Math.cos(angle)*reach, Math.sin(angle)*reach - 0.05, colors[rdm.nextInt(colors.length)]));
        }
    }
    public void addBehindParticle (baseParticle particle) {behindParticles.add(mirrored(particle));}
    public List<character> getCharacters () {return this.characters.getCharacters().stream().collect(Collectors.toList());}

    private List<eventListener> eventListeners = new ArrayList<eventListener>();
    public void addEventListener (eventListener listener) {eventListeners.add(listener);} 


    //What a cutscene has the camera look at instead of the player (cutscene.look), null for the player
    private cordinate cameraFocus;
    public void setCameraFocus (cordinate at) {this.cameraFocus = at;}

    //The cutscene playing, if any (see cutscene)
    private cutscene playing;
    public void play (cutscene c) {this.playing = c;}

    private boolean finished = false;
    public boolean isFinished () {return this.finished;}
    public void setFinished (boolean finished) {this.finished = finished;}
    public void finish () {
        if (!finished) {
            finished = true;
            double seconds = getPlayTime();
            String msg = highscore.endGame(seconds);
            dialogueContainer.add(new dialogue("Game", msg));
        }
    }
    public void next (Double deltaTime) {

        tempCanvas.resize(getWIDTH(), getHEIGHT());

        updateWidthHeight();

        //Behind the main menu the world only looks alive: no dialogue, events, fights or walking
        boolean menu = inMenu();

        if (!menu && !dialogueContainer.isActive() && dialogueContainer.hasNext()) {
            dialogueContainer.next();
        }

        if (!menu && playing != null) {
            //A step can save (quitting to the menu), which finishes the cutscene and clears playing
            cutscene now = playing;
            now.next();
            if (playing == now && now.isOver()) playing = null;
        }
        updateMirror();
        if (!menu) characters.getCharacters().forEach(character::followStep);

        if (menu) getCamera().bindTo(menuView);
        else getCamera().bindTo(cameraFocus != null ? cameraFocus : getMainCharacter());

        //Set standardcolorthingys
        getTempGc().setStroke(Color.WHITE);
        getTempGc().setLineWidth(1);

        //Set current world
        this.world = getCamera().getBound().getWorld();
        if (worlds.get(getCurrentWorld()).isSky()) sky.drawBack(getTempGc());
        worlds.get(getCurrentWorld()).drawGround();

        //Remove dead
        behindParticles = behindParticles.stream().filter(e -> !e.isDead()).collect(Collectors.toList());
        frontParticles  = frontParticles.stream().filter(e -> !e.isDead()).collect(Collectors.toList());
        effectParticles = effectParticles.stream().filter(e -> !e.isDead()).collect(Collectors.toList());

        //Draw entities (particles + tiles and characters above the ground, one z-level at a time)
        behindParticles.forEach(c -> c.draw());
        int maxZ = Math.max(tileTypes.getMaxZ(), characters.getCharacters().stream().mapToInt(c -> c.getZ()).max().orElse(0));
        for (int z = 1; z <= maxZ; z++) {
            worlds.get(getCurrentWorld()).drawLayer(z, characters.getCharacters());
        }
        frontParticles.forEach(c ->  c.draw());
        if (worlds.get(getCurrentWorld()).isSky()) sky.drawFront(getTempGc());

        //Lightning out of the player, from a boltstone they carry
        if (!menu) playerLightning();
        worldStrikes.forEach(l -> l.next(deltaTime));
        worldStrikes = worldStrikes.stream().filter(l -> !l.isDead()).collect(Collectors.toList());
        worldStrikes.forEach(l -> l.draw(this));

        //A teleport's black, with its sparks on top
        transition.next();
        transition.draw();
        if (transition.isActive()) {
            for (character c : characters.getCharacters()) {
                if (c.showsInTransition()) c.draw();
            }
        }
        effectParticles.forEach(c -> c.draw());

        //Info
        if (infoOn()) {
            getTempGc().strokeRoundRect(getMidWIDTH()-5, getMidHEIGHT()-5, 10, 10, 10, 10);
        }
        if (!menu) eventListeners.forEach(c -> c.draw());

        

        //Move stuff

        if (!menu && playing == null && !dialogueContainer.isActive() && getCamera().bindTimeOver() && !battleHandler.isActive() && !gui.blocksMovement() && !transition.isActive()) {
            getMainCharacter().setTarget(dPressed - aPressed, sPressed - wPressed);
        }
        getCamera().move(deltaTime);
        behindParticles.forEach(c -> c.move(deltaTime));
        characters.getCharacters().forEach(c ->        c.move(deltaTime));
        frontParticles.forEach(c ->  c.move(deltaTime));
        effectParticles.forEach(c -> c.move(deltaTime));

        if (!menu) eventListeners = eventListeners.stream().filter(c -> !c.testForTrigger()).collect(Collectors.toList());

        if (!menu && battleHandler.isActive()) {
            battleHandler.carryOn(this);
        }

        if (!menu && playing == null && (System.nanoTime() - lastSave)/1e9 > AUTOSAVE_SECONDS) saveGame();

        //The GUI goes on top of everything
        gui.next(deltaTime);
        gui.draw(getTempGc());

        //Copy from tempCanvas to other canvas
        getCanvas().getGraphicsContext2D().clearRect(0, 0, getWIDTH(), getHEIGHT());
        getCanvas().getGraphicsContext2D().drawImage(getTempCanvas().snapshot(params, null), 0, 0);
        getTempGc().clearRect(0, 0, getWIDTH(), getHEIGHT());
        getTempGc().setFill(Color.web(getWorld(getCurrentWorld()).getBackground())); //"#70C0A0"
        getTempGc().fillRect(0, 0, getWIDTH(), getHEIGHT());
    }
}
