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
    public chunks getWorld (int n) {return this.worlds.get(n);}
    public int getWorldCount () {return this.worlds.size();}
    private List<chunks> worlds = Arrays.asList(
        new chunks("Outside", 24, this, 1.1, "#467764", 0.6, 0.6), //outside
        new chunks("Underground", 17, this, 0.3, "#000", 1, 1), //underground
        new chunks("Inside, 1st floor", 17, this, 1.1, "#000", 0.9, 0.9), //inside, 1st floor
        new chunks("Inside, 2nd floor", 17, this, 1.1, "#000", 0.9, 0.9) //inside, 2nd floor
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
        new battleEvent(getMainCharacter(), tC, battleHandler,  new finishGameEvent(this), theyWin, tC.getX(), tC.getY(), 0.15, tC.getWorld()), 
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

    public void addFrontParticle (baseParticle particle) {frontParticles.add(particle);}

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
        if (transition.isActive()) return;
        character player = getMainCharacter();
        burst(player.getX() + 0.5/getTileCount(), player.getY() + 0.5/getTileCount(), player.getWorld(), 90);
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
        java.util.Random rdm = new java.util.Random();
        String[] colors = {"#21FF2C", "#21FF2C", "#8CFF90", "#D8FFD8"};
        for (int i = 0; i < count; i++) {
            double angle = rdm.nextDouble()*2*Math.PI;
            double reach = 0.08 + rdm.nextDouble()*0.25;
            double size = 0.008 + rdm.nextDouble()*0.014;
            addEffectParticle(new coloredParticle(x, y, world, 0.6 + rdm.nextDouble()*0.7, 0, size, size, -0.9, this,
                Math.cos(angle)*reach, Math.sin(angle)*reach - 0.05, colors[rdm.nextInt(colors.length)]));
        }
    }
    public void addBehindParticle (baseParticle particle) {behindParticles.add(particle);}
    public List<character> getCharacters () {return this.characters.getCharacters().stream().collect(Collectors.toList());}

    private List<eventListener> eventListeners = new ArrayList<eventListener>();
    public void addEventListener (eventListener listener) {eventListeners.add(listener);} 


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

        if (menu) getCamera().bindTo(menuView);
        else getCamera().bindTo(getMainCharacter());

        //Set standardcolorthingys
        getTempGc().setStroke(Color.WHITE);
        getTempGc().setLineWidth(1);

        //Set current world
        this.world = getCamera().getBound().getWorld();
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

        if (!menu && !dialogueContainer.isActive() && getCamera().bindTimeOver() && !battleHandler.isActive() && !gui.blocksMovement() && !transition.isActive()) {
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

        if (!menu && (System.nanoTime() - lastSave)/1e9 > AUTOSAVE_SECONDS) saveGame();

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
