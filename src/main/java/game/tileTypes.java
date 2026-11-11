package game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.scene.canvas.GraphicsContext;

public class tileTypes {

    private List<tileType> types = new ArrayList<tileType>();
    private shader shader;
    //How many tiles high a tree is drawn
    private double treeHeight = 1.4;
    //How far (in tiles) a tree can stand from the middle of its tile, so that a forest does not look like a grid.
    //A tree is 0.7 tiles wide, so above 0.15 the trees next to each other start to overlap
    private double treeScatter = 0.06;
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
        addTreeType("Tree", 400, 1100, types.get(1)); //24  tree
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
        addWaveType("Flower1", 300, 1000, 100, 100, false, null, types.get(1), true); sway(); //55 flower1
        addWaveType("Flower2", 300, 1100, 100, 100, false, null, types.get(1), true); sway(); //56 flower2
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
        addWaveType("Sign", 200, 100, 100, 100, true, null, types.get(1), false, 1, 1, 0); solidFrom(0, 0, 0, 1); solidTo(0, 0, 0, 1); //80 sign, can be walked behind
        addWaveType("Chest", 800, 700, 100, 100, true, null, types.get(15), true); container(); //81 chest
        addFluidType("Lava", 1000, 1000, 100, 100, true, null, 0.3, 0, 0.4, 1, true); //82 lava
        addWaveType("Cracked", 1100, 400, 100, 100, false, null, types.get(82), false); //83 cracked
        addWaveType("Pillarbottom", 1000, 400, 100, 100, false, null, types.get(25), false); //84 pillarbottom
        addWaveType("Pillartop", 1000, 300, 100, 100, true, null, types.get(82), false); //85 pillartop
        addWaveType("BluebrickTopWater", 1000, 600, 100, 100, true, null, types.get(57), false); //86 bluebricktopwater
        addWaveType("ChestLava", 800, 700, 100, 100, true, null, types.get(58), true); container(); //87 chest2
        addFluidType("Crystaltexture", 700, 800, 100, 100, false, null, 0.3, 0.66, 1, 0.8, false); //88 crystal texture
        addWaveType("Crystal", 600, 900, 100, 100, true, null, types.get(88), false); //89 crystal
        addWaveType("Lilypad", 400, 700, 100, 100, false, null, types.get(57), false); //90 lilypad
        addWaveType("Healing", 600, 800, 100, 100, false, c -> {c.restore(); c.setSpawn();}, types.get(88), false); teleportTarget(); //91 healingpad
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
        addTreeType("Treesnow", 400, 1200, types.get(110)); //113 treesnow
        addWaveType("Rocksnow", 700, 300, 100, 100, true, null, types.get(110), false); //114  rocksnow
        addTreeType("Treesnow2", 400, 1200, types.get(1)); //115 treesnow2
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
        addTreeType("Watertinytree", 400, 1100, types.get(130)); //133 watertinytree
        addTreeType("Watertinytree", 400, 1100, types.get(129)); //134 watertinytree

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
        addWaveType("ChestSnow", 800, 700, 100, 100, true, null, types.get(110), true); container(); //146 chest on snow

        //After the final boss: volcano depths (world 4), desert (5), sky ruins (6), crystal caverns (7).
        //Solid things standing on a tile (spires, pillars, crystals, the cactus) do not bob: wave false, but z = 1 so they
        //are drawn among the characters
        //Drafts drawn by tools/art_postgame.py, rows y = 1700 and 1800
        addType("Basalt", 0, 1700, 100, 100, true, null); //147 basalt wall
        addType("Ash", 100, 1700, 100, 100, false, null); //148 ash floor
        addWaveType("Vent", 200, 1700, 100, 100, false, null, types.get(148), false); //149 glowing vent on ash
        addWaveType("Obsidian spire", 300, 1700, 100, 100, true, null, types.get(148), false, 1, 1, 0); //150 obsidian spire
        addType("Dune", 400, 1700, 100, 100, true, null); //151 dune wall
        addWaveType("Cactus", 500, 1700, 100, 100, true, null, types.get(63), false, 1, 1, 0); //152 cactus on sand
        addType("Desert brick", 600, 1700, 100, 100, false, null); //153 desert brick floor
        addWaveType("Ruin pillar", 700, 1700, 100, 100, true, null, types.get(63), false, 1, 1, 0); //154 ruin pillar on sand
        addType("Sky", 800, 1700, 100, 100, true, null); //155 open sky
        addType("Cloud", 900, 1700, 100, 100, false, null); //156 cloud floor
        addType("Marble", 1000, 1700, 100, 100, false, null); //157 marble floor
        addWaveType("Broken column", 1100, 1700, 100, 100, true, null, types.get(156), false, 1, 1, 0); //158 broken column on cloud
        addType("Crystal wall", 1200, 1700, 100, 100, true, null); //159 crystal wall
        addType("Crystal floor", 1300, 1700, 100, 100, false, null); //160 crystal floor
        addWaveType("Crystal cluster", 1400, 1700, 100, 100, true, null, types.get(160), false, 1, 1, 0); //161 crystals on crystal floor
        addWaveType("ChestAsh", 800, 700, 100, 100, true, null, types.get(148), true); container(); //162 chest on ash
        addWaveType("ChestCrystal", 800, 700, 100, 100, true, null, types.get(160), true); container(); //163 chest on crystal floor
        //The old one-tile gates (see the portals below, 172-179, which replaced them)
        addWaveType("Gate to volcano", 0, 1800, 100, 100, false, sealedGate(4), types.get(58), false); //164 gate to volcano (world 2 -> 4)
        addWaveType("Gate to castle", 0, 1800, 100, 100, false, gate(2), types.get(148), false); //165 gate back (4 -> 2)
        addWaveType("Gate to desert", 0, 1800, 100, 100, false, gate(5), types.get(148), false); //166 gate (4 -> 5)
        addWaveType("Gate to volcano", 0, 1800, 100, 100, false, gate(4), types.get(63), false); //167 gate back (5 -> 4)
        addWaveType("Gate to sky", 0, 1800, 100, 100, false, gate(6), types.get(63), false); //168 gate (5 -> 6)
        addWaveType("Gate to desert", 0, 1800, 100, 100, false, gate(5), types.get(156), false); //169 gate back (6 -> 5)
        addWaveType("Gate to caverns", 0, 1800, 100, 100, false, gate(7), types.get(156), false); //170 gate (6 -> 7)
        addWaveType("Gate to sky", 0, 1800, 100, 100, false, gate(6), types.get(160), false); //171 gate back (7 -> 6)
        //164-171 are not on the map any more: the one way out of the final boss's room is a 3x3 portal (portalType: black,
        //with the teleport's stars fixed to the screen showing through). Stepping on any of its tiles takes you to the
        //same tile in the other world, so it has a 3x3 landing spot there, next to the 3x3 portal back
        addPortalType("Portal to the volcano caves", 1000, 1800, gate(1)); ((portalType) types.get(getLength()-1)).offUntilLit(); //172 portal in the girl's room (2 -> 1), black and solid until it lights after she is taken
        addPortalType("Portal to castle", 1000, 1800, gate(2)); //173 portal back (1 -> 2)
        //174-179 were portals between areas that are now joined by caves and paths; not on the map
        addPortalType("Portal to desert", 1000, 1800, gate(5)); //174 (unused)
        addPortalType("Portal to volcano", 1000, 1800, gate(4)); //175 (unused)
        addPortalType("Portal to sky", 1000, 1800, gate(6)); //176 (unused)
        addPortalType("Portal to desert", 1000, 1800, gate(5)); //177 (unused)
        addPortalType("Portal to caverns", 1000, 1800, gate(7)); //178 (unused)
        addPortalType("Portal to sky", 1000, 1800, gate(6)); //179 (unused)
        //The pillar (top and base, as in the castle and the caves) on sand and on cloud
        addWaveType("Pillartopsand", 1000, 300, 100, 100, true, null, types.get(63), false); //180 pillar top on sand
        addWaveType("Pillarbottomsand", 1000, 400, 100, 100, false, null, types.get(63), false); //181 pillar base on sand
        addWaveType("Pillartopsnow", 1000, 300, 100, 100, true, null, types.get(110), false); //182 pillar top on snow (the sky ruins)
        addWaveType("Pillarbottomsnow", 1000, 400, 100, 100, false, null, types.get(110), false); //183 pillar base on snow
        //The sky ruins (world 4): open sky shows the sky behind (skyLayer), and sky portals lead there. A sky portal is
        //1x1 and on the same tile in both worlds, like the cave passages
        types.add(new openSkyType("Open sky", 800, 1700, 100, 100, getLength(), true, null)); //184 open sky (drawn as nothing)
        types.add(new skyPortalType("Sky portal", 800, 1700, 100, 100, getLength(), false, gate(4)).offUntilLit(types.get(63))); //185 sky portal to the sky ruins (in the desert and the crystal caverns), sand until the Desert Nomad is beaten
        types.add(new skyPortalType("Sky portal to the desert", 800, 1700, 100, 100, getLength(), false, gate(0))); //186 sky portal back to the desert (in the sky ruins)
        types.add(new skyPortalType("Sky portal to the caverns", 800, 1700, 100, 100, getLength(), false, gate(1))); //187 sky portal down to the crystal caverns (in the sky ruins)
        //Pillars more than one tile high: the top (93), then middles, then a bottom, then the base (92). Made from the
        //pillar top's own shaft (tools/art_postgame.py)
        addWaveType("Pillarmiddlebluebrick", 1200, 1900, 100, 100, true, null, types.get(60), false); //188 pillar middle on bluebrick
        addWaveType("Pillarbottombluebrick", 1300, 1900, 100, 100, true, null, types.get(60), false); //189 pillar bottom (above the base) on bluebrick
        addWaveType("Pillarmiddlesand", 1200, 1900, 100, 100, true, null, types.get(63), false); //190 pillar middle on sand
        addWaveType("Pillarbottomsand", 1300, 1900, 100, 100, true, null, types.get(63), false); //191 pillar bottom on sand
        addWaveType("Pillarmiddlesnow", 1200, 1900, 100, 100, true, null, types.get(110), false); //192 pillar middle on snow
        addWaveType("Pillarbottomsnow", 1300, 1900, 100, 100, true, null, types.get(110), false); //193 pillar bottom on snow
        //The mirror in the girl's room the crystal boy steps out of (gameHandler.kidnapping), a draft. It stands on the
        //blue brick over lava (119), like the tile left of the pillar next to it, which is what is left when it is gone
        types.add(new mirrorType("Mirror", 1300, 1800, getLength(), types.get(119))); //194 mirror on bluebrick over lava
        //Every mirror is a way into the mirror dimension or out of it (mirrorType). The one behind the crystal boy in his
        //hall, and the one back out in the mirror dimension (the mirrored house's front door); only the player goes
        //through by walking in, the crystal boy and the girl are taken through by the cutscenes
        types.add(new mirrorType("Mirror to the mirror dimension", 1300, 1800, getLength(), types.get(119),
            c -> {if (c == c.getHandler().getMainCharacter()) c.getHandler().intoMirrorDimension();}).alwaysOpen()); //195 mirror into the mirror dimension
        types.add(new mirrorType("Mirror out of the mirror dimension", 1300, 1800, getLength(), types.get(10),
            c -> {if (c == c.getHandler().getMainCharacter()) c.getHandler().outOfMirrorDimension();}).alwaysOpen()); //196 mirror back to the crystal boy's hall
        //The two mirrors that appear on each side of the crystal boy when he blows up for good (gameHandler.becoming): the
        //girl flees into the first, and it is gone; the second is yours, and takes you back to her room, where it began
        types.add(new mirrorType("Mirror the girl flees into", 1300, 1800, getLength(), types.get(1))); //197 mirror on grass (the girl's)
        types.add(new mirrorType("Mirror back to where it began", 1300, 1800, getLength(), types.get(1),
            c -> {if (c == c.getHandler().getMainCharacter()) c.getHandler().intoTheLoop();})); //198 mirror on grass (yours)
        //Ladders between the caves and outside (the volcano depths up to the desert), with the pictures of the ladders
        //under the house at the start (31, 32), which go one world up or down instead
        addType("LadderUpToOutside", 300, 700, 100, 100, false, gate(0)); //199 ladder up, caves -> outside
        addType("LadderDownToCaves", 300, 600, 100, 100, false, gate(1)); //200 ladder down, outside -> caves
        //The ways out of the sky ruins are black portals with the teleport's stars (portalType, 3x3, like the one behind
        //the girl); the ways up into the sky (in the desert and the crystal caverns) stay sky portals (185)
        addPortalType("Portal to the crystal caverns", 1000, 1800, gate(1)); //201 portal in the sky ruins down to the caverns (4 -> 1)
        addPortalType("Portal to the sky ruins", 1000, 1800, gate(4)); //202 (unused: the way up from the caverns is a sky portal)
        addPortalType("Portal to the desert", 1000, 1800, gate(0)); //203 portal in the sky ruins back to the desert (4 -> 0)


    }

    //A gate's pressure plate: whoever steps on it goes to that world, to the same spot (the tile there is its way back)
    private static Consumer<character> gate (int world) {
        return c -> c.setWorld(world);
    }

    //A gate that only works once the portal behind the girl is lit (gameHandler.portalLit). Only on tile 164, which is
    //no longer on the map; the portal itself (172) is solid until then (portalType.offUntilLit)
    private static Consumer<character> sealedGate (int world) {
        return c -> {
            if (c.getHandler().portalLit()) c.setWorld(world);
        };
    }

    private void addType (String name, int x, int y, int width, int height, boolean blocked, Consumer<character> pressurePlate) {
        types.add(new tileType(name, x, y, width, height, getLength(), blocked, pressurePlate));
    }

    private void addPortalType (String name, int x, int y, Consumer<character> pressurePlate) {
        types.add(new portalType(name, x, y, 100, 100, getLength(), false, pressurePlate));
    }

    private void addFluidType (String name, int x, int y, int width, int height, boolean blocked, Consumer<character> pressurePlate, double speed, double redWeight, double greenWeight, double blueWeight, boolean inverse) {
        types.add(new fluidType(name, x, y, width, height, getLength(), blocked, pressurePlate, speed, redWeight, greenWeight, blueWeight, shader, inverse));
    }

    //Overlays that wave stick out of their own tile, so they are lifted off the ground (z = 1). The rest lie flat on it (z = 0)
    private void addWaveType (String name, int xOverlay, int yOverlay, int width, int height, boolean blocked, Consumer<character> pressurePlate, tileType underlay, boolean wave) {
        addWaveType(name, xOverlay, yOverlay, width, height, blocked, pressurePlate, underlay, wave, wave ? 1 : 0, 1, 0);
    }

    //z is the level the overlay is drawn on (2 and up is above the characters), tall is how many tiles high it is,
    //scatter is how far (in tiles) it can be randomly moved away from the middle of its tile (0 keeps it in the grid)
    private void addWaveType (String name, int xOverlay, int yOverlay, int width, int height, boolean blocked, Consumer<character> pressurePlate, tileType underlay, boolean wave, int z, double tall, double scatter) {
        types.add(new waveType(name, xOverlay, yOverlay, width, height, getLength(), blocked, pressurePlate, underlay, wave, z, tall, scatter));
    }

    private void addTreeType (String name, int xOverlay, int yOverlay, tileType underlay) {
        addWaveType(name, xOverlay, yOverlay, 100, 100, true, null, underlay, true, 1, treeHeight, treeScatter);
        sway();
    }

    //Makes the type that was just added show up in the teleports menu (in the hidden secrets settings)
    private void teleportTarget () {
        types.get(getLength()-1).setTeleportTarget(true);
    }

    //Makes the type that was just added a chest (see chest)
    private void container () {
        types.get(getLength()-1).setContainer(true);
    }

    //Makes the wave type that was just added a plant: it sways in the wind instead of bobbing
    private void sway () {
        ((waveType) types.get(getLength()-1)).setSway(true);
    }

    //Makes the type that was just added solid only from the given sides (1 solid, 0 open). The side is the one you come from,
    //so solidFrom(0, 0, 0, 1) can be entered from the left, top and right, but not from below
    private void solidFrom (int left, int top, int right, int bottom) {
        types.get(getLength()-1).setSolidFrom(left, top, right, bottom);
    }

    //Stops you from leaving the type that was just added in the given directions (1 closed, 0 open),
    //so solidTo(0, 0, 0, 1) lets you walk off it to the left, up and to the right, but not down
    private void solidTo (int left, int top, int right, int bottom) {
        types.get(getLength()-1).setSolidTo(left, top, right, bottom);
    }

    public int getMaxZ () {return types.stream().mapToInt(t -> t.getMaxZ()).max().orElse(0);}

    private int getLength () {return types.size();}
    public tileType get(int n) {return types.get(n);}


}
