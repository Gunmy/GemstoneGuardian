package game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.scene.canvas.GraphicsContext;

public class tileTypes {

    private List<tileType> types = new ArrayList<tileType>();
    private shader shader;
    public tileTypes (GraphicsContext gc) {
        shader = new shader(gc);
        addType("Window", 0, 0, 100, 100, true, null); //0 window
        addType("Grass", 100, 0, 100, 100, false, null); //1 grass
        addWaveType("Path", 100, 100, 100, 100, false, null, types.get(1), false); //2 path
        addType("Door", 200, 0, 100, 100, false, c -> c.setWorld(2)); //3 door
        addType("Roof", 300, 0, 100, 100, true, null); //4 roof
        addType("TopLeft", 300, 100, 100, 100, true, null); //5 topleft
        addType("MiddleLeft", 300, 200, 100, 100, true, null); //6 middleleft
        addType("BottomLeft", 300, 300, 100, 100, true, null); //7 bottomleft
        addType("TopMiddle", 400, 100, 100, 100, true, null); //8 topmiddle
        addType("MiddleMiddle", 400, 200, 100, 100, true, null); //9 middlemiddle
        addType("BottomMiddle", 400, 300, 100, 100, true, null); //10 bottommiddle
        addType("TopRight", 500, 100, 100, 100, true, null); //11 topright
        addType("MiddleRight", 500, 200, 100, 100, true, null); //12 middleright
        addType("BottomRight", 500, 300, 100, 100, true, null); //13 bottomright
        addType("Window2", 0, 100, 100, 100, true, null); //14 window2
        addType("Floor", 200, 200, 100, 100, false, null); //15 floor
        addWaveType("FloorHome", 200, 300, 100, 100, false, c -> c.setWorld(0), types.get(15), false); //16 floorhome
        addType("Void", 100, 200, 100, 100, true, null); //17 void
        addType("VoidEdge", 100, 300, 100, 100, true, null); //18 voidedge
        addType("InsideLeft", 300, 400, 100, 100, true, null); //19 insideleft
        addType("InsideMiddle", 400, 400, 100, 100, true, null); //20 insidemiddle
        addType("InsideRight", 500, 400, 100, 100, true, null); //21 insideright
        addType("Firdgetop", 200, 400, 100, 100, true, null); //22 fridgetop
        addWaveType("Firdgebottom", 200, 500, 100, 100, true, null, types.get(15), false); //23 fridgebottom
        addWaveType("Tree", 400, 1100, 100, 100, true, null, types.get(1), true); //24  tree
        addType("Cavefloor", 600, 0, 100, 100, false, null); //25  cavefloor
        addType("CaveWallLeft", 700, 0, 100, 100, true, null); //26  cavewallleft
        addType("CaveWallMiddle", 800, 0, 100, 100, true, null); //27  cavewallmiddle
        addType("CaveWallRight", 900, 0, 100, 100, true, null); //28  cavewallright
        addType("StairUp", 200, 600, 100, 100, false, c -> c.setWorld(c.getWorld()+1)); //29  stairUp
        addType("StairDown", 200, 700, 100, 100, false, c -> c.setWorld(c.getWorld()-1)); //30  stairDown
        addType("LadderDown", 300, 600, 100, 100, false, c -> c.setWorld(c.getWorld()-1)); //31  ladderDown
        addType("LadderUp", 300, 700, 100, 100, false, c -> c.setWorld(c.getWorld()+1)); //32  ladderUp
        addType("Wall1x1", 500, 500, 100, 100, true, null); //33 Wall1x1
        addType("GrassWallLeft", 700, 100, 100, 100, true, null); //34  grasswallleft
        addType("GrassWallMiddle", 800, 100, 100, 100, true, null); //35  grasswallmiddle
        addType("GrassWallRight", 900, 100, 100, 100, true, null); //36  grasswallright
        addWaveType("GrassEdgeRight", 800, 200, 100, 100, true, null, types.get(24), false); //37  grassedgeright
        addWaveType("GrassEdgeLeft", 900, 200, 100, 100, true, null, types.get(24), false); //38  grassedgeleft
        addWaveType("GrassEdgeTop", 1000, 200, 100, 100, true, null, types.get(24), false); //39  grassedgetop
        addType("PassageBottom", 1000, 100, 100, 100, false, c -> c.setWorld(1)); //40 portalbottom
        addType("PassageTop", 1100, 100, 100, 100, false, c -> c.setWorld(1)); //41 portaltop
        addType("PassageBottomToReal", 1000, 100, 100, 100, false, c -> c.setWorld(0)); //42 portalbottom2
        addType("PassageTopToReal", 1100, 100, 100, 100, false, c -> c.setWorld(0)); //43 portaltop2
        addWaveType("Rock", 700, 300, 100, 100, true, null, types.get(1), false); //44  rock
        addType("Pipe", 400, 0, 100, 100, true, null); //45  pipe
        addType("Tiles", 500, 0, 100, 100, false, null); //46  tiles
        addType("Painting", 500, 600, 100, 100, true, null); //47  painting
        addType("Vasktop", 600, 500, 100, 100, true, null); //48  vasktop
        addType("Cuppboardbottom", 600, 600, 100, 100, false, null); //49  cuppboardbottom
        addType("Cuppboardtop", 700, 500, 100, 100, true, null); //50 cuppboardtop
        addWaveType("Cuppboard", 700, 600, 100, 100, true, null, types.get(15), false); //51 cuppboard
        addWaveType("CarpetLeft", 0, 800, 100, 100, false, null, types.get(15), false); //52 carpetleft
        addWaveType("CarpetRight", 100, 800, 100, 100, false, null, types.get(15), false); //53 carpetright
        addType("DirtTile", 400, 800, 100, 100, false, null); //54 dirttile
        addWaveType("Flower1", 300, 1000, 100, 100, false, null, types.get(1), true); //55 flower1
        addWaveType("Flower2", 300, 1100, 100, 100, false, null, types.get(1), true); //56 flower2
        addFluidType("Water", 800, 1000, 100, 100, true, null, 0.5, 0.33, 0.75, 1, false); //57 water
        addType("Brick", 900, 500, 100, 100, false, null); //58 brick
        addWaveType("Flowerpot", 800, 600, 100, 100, true, null, types.get(15), false); //59 flowerpot
        addType("Bluebrick", 900, 600, 100, 100, true, null); //60 bluebrick
        addWaveType("BluebrickTop", 1000, 600, 100, 100, false, null, types.get(58), false); //61 bluebricktop
        addWaveType("Brickpath", 1000, 500, 100, 100, false, null, types.get(1), false); //62 brickpath
        addType("Sand", 1100, 500, 100, 100, false, null); //63 sand
        addType("TL", 900, 700, 100, 100, true, null); //64 TL
        addType("TM", 1000, 700, 100, 100, true, null); //65 TM
        addType("TR", 1100, 700, 100, 100, true, null); //66 TR
        addType("ML", 900, 800, 100, 100, true, null); //67 ML
        addType("MM", 1000, 800, 100, 100, true, null); //68 MM
        addType("MR", 1100, 800, 100, 100, true, null); //69 MR
        addType("BL", 900, 900, 100, 100, true, null); //70 BL
        addType("BM", 1000, 900, 100, 100, true, null); //71 BM
        addType("BR", 1100, 900, 100, 100, true, null); //72 BR
        addWaveType("Brickpressureplate", 800, 400, 100, 100, false, null, types.get(58), false); //73 brickpressureplate
        addWaveType("Bedfloor", 500, 800, 100, 100, true, null, types.get(15), false); //74 bedfloor
        addType("Bedwall", 500, 700, 100, 100, true, null); //75 bedwall
        addWaveType("Cupboardfloor", 600, 700, 100, 100, false, null, types.get(15), false); //76 cupboardfloor
        addWaveType("Emptyshelf", 700, 700, 100, 100, true, null, types.get(15), false); //77 emptyshelf
        addType("Wheelchairtop", 400, 500, 100, 100, true, null); //78 wheelchairtop
        addWaveType("Wheelchairbottom", 400, 600, 100, 100, true, null, types.get(15), false); //79 wheelchairbottom
        addWaveType("Sign", 200, 100, 100, 100, true, null, types.get(1), false); //80 sign
        addWaveType("Chest", 800, 700, 100, 100, true, null, types.get(15), true); //81 chest
        addFluidType("Lava", 1000, 1000, 100, 100, true, null, 0.3, 0, 0.4, 1, true); //82 lava
        addWaveType("Cracked", 1100, 400, 100, 100, false, null, types.get(82), false); //83 cracked
        addWaveType("Pillarbottom", 1000, 400, 100, 100, false, null, types.get(25), false); //84 pillarbottom
        addWaveType("Pillartop", 1000, 300, 100, 100, true, null, types.get(82), false); //85 pillartop
        addWaveType("BluebrickTopWater", 1000, 600, 100, 100, true, null, types.get(57), false); //86 bluebricktopwater
        addWaveType("ChestLava", 800, 700, 100, 100, true, null, types.get(58), true); //87 chest2
        addFluidType("Crystaltexture", 700, 800, 100, 100, false, null, 0.3, 0.66, 1, 0.8, false); //88 crystal texture
        addWaveType("Crystal", 600, 900, 100, 100, true, null, types.get(88), false); //89 crystal
        addWaveType("Lilypad", 400, 700, 100, 100, false, null, types.get(57), false); //90 lilypad
        addWaveType("Healing", 600, 800, 100, 100, false, c -> {c.restore(); c.setSpawn();}, types.get(88), false); //91 healingpad
        addWaveType("Pillarbottom", 1000, 400, 100, 100, false, null, types.get(58), false); //92 pillarbottombrick
        addWaveType("Pillartop", 1000, 300, 100, 100, true, null, types.get(60), false); //93 pillartopbluebrick
        addType("Carpet1", 1200, 1200, 100, 100, false, null); //94 carpet TL
        addType("Carpet2", 1300, 1200, 100, 100, false, null); //95 carpet TM
        addType("Carpet3", 1400, 1200, 100, 100, false, null); //96 carpet TR
        addType("Carpet4", 1200, 1300, 100, 100, false, null); //97 carpet ML
        addType("Carpet5", 1300, 1300, 100, 100, false, null); //98 carpet MM
        addType("Carpet6", 1400, 1300, 100, 100, false, null); //99 carpet MR
        addType("Carpet7", 1200, 1400, 100, 100, false, null); //100 carpet BL
        addType("Carpet8", 1300, 1400, 100, 100, false, null); //101 carpet BM
        addType("Carpet9", 1400, 1400, 100, 100, false, null); //102 carpet BR
        addType("Doortosecond", 200, 0, 100, 100, false, c -> c.setWorld(3)); //103 door to second
        addFluidType("Toxic", 600, 1000, 100, 100, true, null, 0.33, 0.33, 1, 0.75, false); //104 toxic
        addWaveType("Pillartopdirt", 1000, 300, 100, 100, true, null, types.get(25), false); //105 pillartopdirt
        addWaveType("Rockw", 700, 300, 100, 100, true, null, types.get(57), false); //106  rockwater
        addWaveType("Path", 100, 100, 100, 100, false, null, types.get(63), false); //107 pathsand
        addWaveType("Brickpathsand", 1000, 500, 100, 100, false, null, types.get(63), false); //108 brickpathsand
        addWaveType("Rocksand", 700, 300, 100, 100, true, null, types.get(63), false); //109  rocksand
        addType("Snow", 800, 500, 100, 100, false, null); //110 snow
        addWaveType("Brickpathsnow", 1000, 500, 100, 100, false, null, types.get(110), false); //111 brickssnow
        addType("Ice", 800, 1000, 100, 100, false, null); //112 ice
        addWaveType("Treesnow", 400, 1200, 100, 100, true, null, types.get(110), true); //113 treesnow
        addWaveType("Rocksnow", 700, 300, 100, 100, true, null, types.get(110), false); //114  rocksnow
        addWaveType("Treesnow2", 400, 1200, 100, 100, true, null, types.get(1), true); //115 treesnow2
        addWaveType("Oven top", 1100, 1300, 100, 100, true, null, types.get(20), true); //116 oven top
        addWaveType("Oven bottom", 1100, 1400, 100, 100, true, null, types.get(82), false); //117 oven bottom
        addType("BigDoor", 100, 200, 100, 100, false, c -> c.setWorld(2)); //118 big door
        addWaveType("Bluebricklava", 1100, 600, 100, 100, false, null, types.get(82), false); //119 bluebricklava
        addType("BigDoorTop", 900, 1200, 100, 100, true, null); //120 bigdoortop

        addWaveType("Seat", 1200, 700, 100, 100, false, null, types.get(15), false); //121 seat
        addWaveType("Table1", 1300, 700, 100, 100, true, null, types.get(15), false); //122 table1
        addWaveType("Table2", 1400, 700, 100, 100, true, null, types.get(15), false); //123 table2
        addType("PassageBottom2", 1000, 0, 100, 100, false, c -> c.setWorld(1)); //124 potal bottom 2

        addWaveType("Sandcastle", 1200, 500, 100, 100, false,  c -> c.setWorld(2), types.get(82), true); //125 sandcastle
        addWaveType("Big rock", 700, 1300, 100, 100, true, null, types.get(25), false); //126 big rock

        addWaveType("Waterfall", 1300, 100, 100, 100, true, null, types.get(57), false); //127 waterfall
        addWaveType("Watertiny1", 1300, 200, 100, 100, false, null, types.get(57), false); //128 watertiny1
        addWaveType("Watertiny2", 1300, 300, 100, 100, false, null, types.get(57), false); //129 watertiny2
        addWaveType("Watertiny3", 1300, 400, 100, 100, false, null, types.get(57), false); //130 watertiny3
        addWaveType("Watertinypath", 100, 100, 100, 100, false, null, types.get(130), false); //131 watertinypath
        addWaveType("Watertinypath2", 100, 100, 100, 100, false, null, types.get(129), false); //132 watertinypath2
        addWaveType("Watertinytree", 400, 1100, 100, 100, true, null, types.get(130), true); //133 watertinytree
        addWaveType("Watertinytree", 400, 1100, 100, 100, true, null, types.get(129), true); //134 watertinytree

        addType("Snowwall", 700, 400, 100, 100, true, null); //135 snowwall
        addWaveType("Igloo1", 800, 1300, 100, 100, true, null, types.get(110), false); //136 igloo1
        addWaveType("Igloo2", 900, 1300, 100, 100, true, null, types.get(110), false); //137 igloo2
        addWaveType("Igloo3", 1000, 1300, 100, 100, true, null, types.get(110), false); //138 igloo3
        addWaveType("Igloo4", 800, 1400, 100, 100, true, null, types.get(110), false); //139 igloo4
        addWaveType("Igloo5", 900, 1400, 100, 100, false, c -> c.setWorld(2), types.get(110), false); //140 igloo5
        addWaveType("Igloo6", 1000, 1400, 100, 100, true, null, types.get(110), false); //141 igloo6
        addType("Snowtile", 600, 400, 100, 100, false, null); //142 snowwall
        addWaveType("FloorHome", 200, 300, 100, 100, false, c -> c.setWorld(0), types.get(110), false); //143 floorhome
        addWaveType("Oven bottom snow", 700, 1400, 100, 100, true, null, types.get(82), false); //144 oven bottom snow
        addWaveType("Oven top snow", 1100, 1300, 100, 100, true, null, types.get(135), true); //145 oven top snow


    }

    private void addType (String name, int x, int y, int width, int height, boolean blocked, Consumer<character> pressurePlate) {
        types.add(new tileType(name, x, y, width, height, getLength(), blocked, pressurePlate));
    }

    private void addFluidType (String name, int x, int y, int width, int height, boolean blocked, Consumer<character> pressurePlate, double speed, double redWeight, double greenWeight, double blueWeight, boolean inverse) {
        types.add(new fluidType(name, x, y, width, height, getLength(), blocked, pressurePlate, speed, redWeight, greenWeight, blueWeight, shader, inverse));
    }

    private void addWaveType (String name, int xOverlay, int yOverlay, int width, int height, boolean blocked, Consumer<character> pressurePlate, tileType underlay, boolean wave) {
        types.add(new waveType(name, xOverlay, yOverlay, width, height, getLength(), blocked, pressurePlate, underlay, wave));
    }

    private int getLength () {return types.size();}
    public tileType get(int n) {return types.get(n);}


}
