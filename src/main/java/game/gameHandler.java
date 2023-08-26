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

    public double getUnitWidth () {return unitWidth*getWIDTH();}
    public double getUnitHeight () {return unitHeight*getWIDTH();}

    private void updateWidthHeight () {
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
    private List<chunks> worlds = Arrays.asList(
        new chunks(24, this, 1.1, "#467764", 0.6, 0.6), //outside
        new chunks(17, this, 0.3, "#000", 1, 1), //underground
        new chunks(17, this, 1.1, "#000", 0.9, 0.9), //inside, 1st floor
        new chunks(17, this, 1.1, "#000", 0.9, 0.9) //inside, 2nd floor
    ); 

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

    public character getMainCharacter () {return characters.get("Boy");}

    private battle battleHandler;;
    public battle getBattle () {return this.battleHandler;}

    private SnapshotParameters params = new SnapshotParameters();


    public gameHandler (RCanvas canvas, dialogueContainer dialogueContainer, battle battleHandler, imgDict imgDict) {
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
        characters.add(new character("Boy",  10000.3, 10000.3, 0, 500, 100, 100, 3, this, 5));


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
        event giveFirstStoneEvent = new giveEvent(getMainCharacter(), gemstoneDict.rock(), dialogueContainer);
        event giveObisidan = new giveEvent(getMainCharacter(), gemstoneDict.obsidian(), dialogueContainer);
        event giveBoltstone = new giveEvent(getMainCharacter(), gemstoneDict.boltstone(), dialogueContainer);
        event giveBolder = new giveEvent(getMainCharacter(), gemstoneDict.giantBolder(), dialogueContainer);
        event giveUranium = new giveEvent(getMainCharacter(), gemstoneDict.uranium(), dialogueContainer);
        event giveDiscombobulateshard = new giveEvent(getMainCharacter(), gemstoneDict.discombobulateShard(), dialogueContainer);
        event giveGravimorphcrystal = new giveEvent(getMainCharacter(), gemstoneDict.meteorite(), dialogueContainer);
        event giveMagma = new giveEvent(getMainCharacter(), gemstoneDict.magma(), dialogueContainer);
        event giveHealite = new giveEvent(getMainCharacter(), gemstoneDict.healite(), dialogueContainer);
        event givePebble = new giveEvent(getMainCharacter(), gemstoneDict.pebble(), dialogueContainer);

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
        eventListeners.add(new eventListener(this, 10001.54, 10000.11, 0.15, 2, giveFirstStoneEvent, Arrays.asList(
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
        )));


        eventListeners.add(new eventListener(this, 10001.54, 10000.11, 0.15, 2, grandmaCome));
        eventListeners.add(new eventListener(this, 9999.11, 9993.11, 0.15, 1, giveObisidan));
        eventListeners.add(new eventListener(this, 10000.78, 9991.11, 0.15, 0, giveBoltstone));
        eventListeners.add(new eventListener(this, 10000.44, 9990.22, 0.15, 2, giveBolder));
        eventListeners.add(new eventListener(this, 10002.44, 9987.44, 0.15, 1, giveUranium));

        eventListeners.add(new eventListener(this, 10015.67, 9962.22, 0.15, 2, giveMagma));
        eventListeners.add(new eventListener(this, 10010, 9973.11, 0.15, 2, giveGravimorphcrystal));
        eventListeners.add(new eventListener(this, 10003.44, 9973.11, 0.15, 2, giveDiscombobulateshard));
        eventListeners.add(new eventListener(this, 10013.89, 9968.89, 0.15, 1, giveHealite));
        eventListeners.add(new eventListener(this, 10003.22, 9981.33, 0.15, 2, givePebble));


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


        camera = new camera(this, getMainCharacter());
        //Generate map
        readChunks.read(this);
    }

    private dialogueContainer dialogueContainer;
    public dialogueContainer getDiealoguecontainer () {return this.dialogueContainer;}

    private characterContainer characters = new characterContainer();
    private List<baseParticle> frontParticles = new ArrayList<baseParticle>();
    private List<baseParticle> behindParticles = new ArrayList<baseParticle>();

    public void addFrontParticle (baseParticle particle) {frontParticles.add(particle);}
    public void addBehindParticle (baseParticle particle) {behindParticles.add(particle);}
    public List<character> getCharacters () {return this.characters.getCharacters().stream().collect(Collectors.toList());}

    private List<eventListener> eventListeners = new ArrayList<eventListener>();
    public void addEventListener (eventListener listener) {eventListeners.add(listener);} 


    private long startTime = System.nanoTime();
    private boolean finished = false;
    public void finish () {
        if (!finished) {
            finished = true;
            double seconds = (System.nanoTime()-startTime)/1_000_000_000;
            String msg = highscore.endGame(seconds);
            dialogueContainer.add(new dialogue("Game", msg));
        }
    }
    public void next (Double deltaTime) {

        tempCanvas.resize(getWIDTH(), getHEIGHT());

        updateWidthHeight();

        if (!dialogueContainer.isActive() && dialogueContainer.hasNext()) {
            dialogueContainer.next();
        }

        getCamera().bindTo(getMainCharacter());

        //Set standardcolorthingys
        getTempGc().setStroke(Color.WHITE);
        getTempGc().setLineWidth(1);

        //Set current world
        this.world = getCamera().getBound().getWorld();
        worlds.get(getCurrentWorld()).drawChunks();

        //Remove dead
        behindParticles = behindParticles.stream().filter(e -> !e.isDead()).collect(Collectors.toList());
        frontParticles  = frontParticles.stream().filter(e -> !e.isDead()).collect(Collectors.toList());

        //Draw entities (particles + entities)
        behindParticles.forEach(c -> c.draw());
        characters.getCharacters().forEach(c ->        c.draw());
        frontParticles.forEach(c ->  c.draw());

        //Info
        if (infoOn()) {
            getTempGc().strokeRoundRect(getMidWIDTH()-5, getMidHEIGHT()-5, 10, 10, 10, 10);
        }
        eventListeners.forEach(c -> c.draw());

        

        //Move stuff

        if (!dialogueContainer.isActive() && getCamera().bindTimeOver() && !battleHandler.isActive()) {
            getMainCharacter().setTarget(dPressed - aPressed, sPressed - wPressed);
        }
        getCamera().move(deltaTime);
        behindParticles.forEach(c -> c.move(deltaTime));
        characters.getCharacters().forEach(c ->        c.move(deltaTime));
        frontParticles.forEach(c ->  c.move(deltaTime));

        eventListeners = eventListeners.stream().filter(c -> !c.testForTrigger()).collect(Collectors.toList());
        
        if (battleHandler.isActive()) {
            battleHandler.carryOn(this);
        }

        //Copy from tempCanvas to other canvas
        getCanvas().getGraphicsContext2D().clearRect(0, 0, getWIDTH(), getHEIGHT());
        getCanvas().getGraphicsContext2D().drawImage(getTempCanvas().snapshot(params, null), 0, 0);
        getTempGc().clearRect(0, 0, getWIDTH(), getHEIGHT());
        getTempGc().setFill(Color.web(getWorld(getCurrentWorld()).getBackground())); //"#70C0A0"
        getTempGc().fillRect(0, 0, getWIDTH(), getHEIGHT());
    }
}
