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
        addType("Window", 0, 0, 20, 20, true, null); //0 window
        addType("Grass", 20, 0, 20, 20, false, null); //1 grass
        addWaveType("Path", 20, 20, 20, 20, false, null, types.get(1), false); //2 path
        addType("Door", 40, 0, 20, 20, false, c -> c.setWorld(2)); //3 door
        addType("Roof", 60, 0, 20, 20, true, null); //4 roof
        addType("TopLeft", 60, 20, 20, 20, true, null); //5 topleft
        addType("MiddleLeft", 60, 40, 20, 20, true, null); //6 middleleft
        addType("BottomLeft", 60, 60, 20, 20, true, null); //7 bottomleft
        addType("TopMiddle", 80, 20, 20, 20, true, null); //8 topmiddle
        addType("MiddleMiddle", 80, 40, 20, 20, true, null); //9 middlemiddle
        addType("BottomMiddle", 80, 60, 20, 20, true, null); //10 bottommiddle
        addType("TopRight", 100, 20, 20, 20, true, null); //11 topright
        addType("MiddleRight", 100, 40, 20, 20, true, null); //12 middleright
        addType("BottomRight", 100, 60, 20, 20, true, null); //13 bottomright
        addType("Window2", 0, 20, 20, 20, true, null); //14 window2
        addType("Floor", 40, 40, 20, 20, false, null); //15 floor
        addWaveType("FloorHome", 40, 60, 20, 20, false, c -> c.setWorld(0), types.get(15), false); //16 floorhome
        addType("Void", 20, 40, 20, 20, true, null); //17 void
        addType("VoidEdge", 20, 60, 20, 20, true, null); //18 voidedge
        addType("InsideLeft", 60, 80, 20, 20, true, null); //19 insideleft
        addType("InsideMiddle", 80, 80, 20, 20, true, null); //20 insidemiddle
        addType("InsideRight", 100, 80, 20, 20, true, null); //21 insideright
        addType("Firdgetop", 40, 80, 20, 20, true, null); //22 fridgetop
        addWaveType("Firdgebottom", 40, 100, 20, 20, true, null, types.get(15), false); //23 fridgebottom
        addTreeType("Tree", 80, 220, types.get(1)); //24  tree
        addType("Cavefloor", 120, 0, 20, 20, false, null); //25  cavefloor
        addType("CaveWallLeft", 140, 0, 20, 20, true, null); //26  cavewallleft
        addType("CaveWallMiddle", 160, 0, 20, 20, true, null); //27  cavewallmiddle
        addType("CaveWallRight", 180, 0, 20, 20, true, null); //28  cavewallright
        addType("StairUp", 40, 120, 20, 20, false, c -> c.setWorld(c.getWorld()+1)); //29  stairUp
        addType("StairDown", 40, 140, 20, 20, false, c -> c.setWorld(c.getWorld()-1)); //30  stairDown
        addType("LadderDown", 60, 120, 20, 20, false, c -> c.setWorld(c.getWorld()-1)); //31  ladderDown
        addType("LadderUp", 60, 140, 20, 20, false, c -> c.setWorld(c.getWorld()+1)); //32  ladderUp
        addType("Wall1x1", 100, 100, 20, 20, true, null); //33 Wall1x1
        addType("GrassWallLeft", 140, 20, 20, 20, true, null); //34  grasswallleft
        addType("GrassWallMiddle", 160, 20, 20, 20, true, null); //35  grasswallmiddle
        addType("GrassWallRight", 180, 20, 20, 20, true, null); //36  grasswallright
        addWaveType("GrassEdgeRight", 160, 40, 20, 20, true, null, types.get(24), false); //37  grassedgeright
        addWaveType("GrassEdgeLeft", 180, 40, 20, 20, true, null, types.get(24), false); //38  grassedgeleft
        addWaveType("GrassEdgeTop", 200, 40, 20, 20, true, null, types.get(24), false); //39  grassedgetop
        addType("PassageBottom", 200, 20, 20, 20, false, c -> c.setWorld(1)); //40 portalbottom
        addType("PassageTop", 220, 20, 20, 20, false, c -> c.setWorld(1)); //41 portaltop
        addType("PassageBottomToReal", 200, 20, 20, 20, false, c -> c.setWorld(0)); //42 portalbottom2
        addType("PassageTopToReal", 220, 20, 20, 20, false, c -> c.setWorld(0)); //43 portaltop2
        addWaveType("Rock", 140, 60, 20, 20, true, null, types.get(1), false); //44  rock
        addType("Pipe", 80, 0, 20, 20, true, null); //45  pipe
        addType("Tiles", 100, 0, 20, 20, false, null); //46  tiles
        addType("Painting", 100, 120, 20, 20, true, null); //47  painting
        addType("Vasktop", 120, 100, 20, 20, true, null); //48  vasktop
        addType("Cuppboardbottom", 120, 120, 20, 20, false, null); //49  cuppboardbottom
        addType("Cuppboardtop", 140, 100, 20, 20, true, null); //50 cuppboardtop
        addWaveType("Cuppboard", 140, 120, 20, 20, true, null, types.get(15), false); //51 cuppboard
        addWaveType("CarpetLeft", 0, 160, 20, 20, false, null, types.get(15), false); //52 carpetleft
        addWaveType("CarpetRight", 20, 160, 20, 20, false, null, types.get(15), false); //53 carpetright
        addType("DirtTile", 80, 160, 20, 20, false, null); //54 dirttile
        addWaveType("Flower1", 60, 200, 20, 20, false, null, types.get(1), true); sway(); //55 flower1
        addWaveType("Flower2", 60, 220, 20, 20, false, null, types.get(1), true); sway(); //56 flower2
        addFluidType("Water", 160, 200, 20, 20, true, null, 0.5, 0.33, 0.75, 1, false); //57 water
        addType("Brick", 180, 100, 20, 20, false, null); //58 brick
        addWaveType("Flowerpot", 160, 120, 20, 20, true, null, types.get(15), false); //59 flowerpot
        addType("Bluebrick", 180, 120, 20, 20, true, null); //60 bluebrick
        addWaveType("BluebrickTop", 200, 120, 20, 20, false, null, types.get(58), false); //61 bluebricktop
        addWaveType("Brickpath", 200, 100, 20, 20, false, null, types.get(1), false); //62 brickpath
        addType("Sand", 220, 100, 20, 20, false, null); //63 sand
        addType("TL", 180, 140, 20, 20, true, null); //64 TL
        addType("TM", 200, 140, 20, 20, true, null); //65 TM
        addType("TR", 220, 140, 20, 20, true, null); //66 TR
        addType("ML", 180, 160, 20, 20, true, null); //67 ML
        addType("MM", 200, 160, 20, 20, true, null); //68 MM
        addType("MR", 220, 160, 20, 20, true, null); //69 MR
        addType("BL", 180, 180, 20, 20, true, null); //70 BL
        addType("BM", 200, 180, 20, 20, true, null); //71 BM
        addType("BR", 220, 180, 20, 20, true, null); //72 BR
        addWaveType("Brickpressureplate", 160, 80, 20, 20, false, null, types.get(58), false); //73 brickpressureplate
        addWaveType("Bedfloor", 100, 160, 20, 20, true, null, types.get(15), false); //74 bedfloor
        addType("Bedwall", 100, 140, 20, 20, true, null); //75 bedwall
        addWaveType("Cupboardfloor", 120, 140, 20, 20, false, null, types.get(15), false); //76 cupboardfloor
        addWaveType("Emptyshelf", 140, 140, 20, 20, true, null, types.get(15), false); //77 emptyshelf
        //78 and 79 were the wheelchair (top, bottom), taken out of the game: kept so the ids after them stay, their cells
        //in tiles.png are empty and they are on no map
        addType("Unused 78", 80, 100, 20, 20, true, null); //78 unused (was the wheelchair top)
        addWaveType("Unused 79", 80, 120, 20, 20, true, null, types.get(15), false); //79 unused (was the wheelchair bottom)
        addWaveType("Sign", 40, 20, 20, 20, true, null, types.get(1), false, 1, 1, 0); solidFrom(0, 0, 0, 1); solidTo(0, 0, 0, 1); //80 sign, can be walked behind
        addWaveType("Chest", 160, 140, 20, 20, true, null, types.get(15), true); container(); //81 chest
        addFluidType("Lava", 200, 200, 20, 20, true, null, 0.3, 0, 0.4, 1, true); //82 lava
        addWaveType("Cracked", 220, 80, 20, 20, false, null, types.get(82), false); //83 cracked
        addWaveType("Pillarbottom", 200, 80, 20, 20, false, null, types.get(25), false); //84 pillarbottom
        addWaveType("Pillartop", 200, 60, 20, 20, true, null, types.get(82), false); //85 pillartop
        addWaveType("BluebrickTopWater", 200, 120, 20, 20, true, null, types.get(57), false); //86 bluebricktopwater
        addWaveType("ChestLava", 160, 140, 20, 20, true, null, types.get(58), true); container(); //87 chest2
        addFluidType("Crystaltexture", 140, 160, 20, 20, false, null, 0.3, 0.66, 1, 0.8, false); //88 crystal texture
        addWaveType("Crystal", 120, 180, 20, 20, true, null, types.get(88), false); //89 crystal
        addWaveType("Lilypad", 80, 140, 20, 20, false, null, types.get(57), false); springy(); //90 lilypad, bounces when stepped on
        addWaveType("Healing", 120, 160, 20, 20, false, c -> {c.restore(); c.setSpawn();}, types.get(88), false); teleportTarget(); //91 healingpad
        addWaveType("Pillarbottom", 200, 80, 20, 20, false, null, types.get(58), false); //92 pillarbottombrick
        addWaveType("Pillartop", 200, 60, 20, 20, true, null, types.get(60), false); //93 pillartopbluebrick
        addType("Carpet1", 240, 240, 20, 20, false, null); //94 carpet TL
        addType("Carpet2", 260, 240, 20, 20, false, null); //95 carpet TM
        addType("Carpet3", 280, 240, 20, 20, false, null); //96 carpet TR
        addType("Carpet4", 240, 260, 20, 20, false, null); //97 carpet ML
        addType("Carpet5", 260, 260, 20, 20, false, null); //98 carpet MM
        addType("Carpet6", 280, 260, 20, 20, false, null); //99 carpet MR
        addType("Carpet7", 240, 280, 20, 20, false, null); //100 carpet BL
        addType("Carpet8", 260, 280, 20, 20, false, null); //101 carpet BM
        addType("Carpet9", 280, 280, 20, 20, false, null); //102 carpet BR
        addType("Doortosecond", 40, 0, 20, 20, false, c -> c.setWorld(3)); //103 door to second
        addFluidType("Toxic", 120, 200, 20, 20, true, null, 0.33, 0.33, 1, 0.75, false); //104 toxic
        addWaveType("Pillartopdirt", 200, 60, 20, 20, true, null, types.get(25), false); //105 pillartopdirt
        addWaveType("Rockw", 140, 60, 20, 20, true, null, types.get(57), false); //106  rockwater
        addWaveType("Path", 20, 20, 20, 20, false, null, types.get(63), false); //107 pathsand
        addWaveType("Brickpathsand", 200, 100, 20, 20, false, null, types.get(63), false); //108 brickpathsand
        addWaveType("Rocksand", 140, 60, 20, 20, true, null, types.get(63), false); //109  rocksand
        addType("Snow", 160, 100, 20, 20, false, null); //110 snow
        addWaveType("Brickpathsnow", 200, 100, 20, 20, false, null, types.get(110), false); //111 brickssnow
        addType("Ice", 160, 200, 20, 20, false, null); //112 ice
        addTreeType("Treesnow", 80, 240, types.get(110)); //113 treesnow
        addWaveType("Rocksnow", 140, 60, 20, 20, true, null, types.get(110), false); //114  rocksnow
        addTreeType("Treesnow2", 80, 240, types.get(1)); //115 treesnow2
        addWaveType("Oven top", 220, 260, 20, 20, true, null, types.get(20), true); //116 oven top
        addWaveType("Oven bottom", 220, 280, 20, 20, true, null, types.get(82), false); //117 oven bottom
        addType("BigDoor", 20, 40, 20, 20, false, c -> c.setWorld(2)); //118 big door
        addWaveType("Bluebricklava", 220, 120, 20, 20, false, null, types.get(82), false); //119 bluebricklava
        addType("BigDoorTop", 180, 240, 20, 20, true, null); //120 bigdoortop

        addWaveType("Seat", 240, 140, 20, 20, false, null, types.get(15), false); //121 seat
        addWaveType("Table1", 260, 140, 20, 20, true, null, types.get(15), false); //122 table1
        addWaveType("Table2", 280, 140, 20, 20, true, null, types.get(15), false); //123 table2
        addType("PassageBottom2", 200, 0, 20, 20, false, c -> c.setWorld(1)); //124 potal bottom 2

        addWaveType("Sandcastle", 240, 100, 20, 20, false,  c -> c.setWorld(2), types.get(82), true); //125 sandcastle
        addWaveType("Big rock", 140, 260, 20, 20, true, null, types.get(25), false); //126 big rock

        addWaveType("Waterfall", 260, 20, 20, 20, true, null, types.get(57), false); //127 waterfall
        addWaveType("Watertiny1", 260, 40, 20, 20, false, null, types.get(57), false); //128 watertiny1
        addWaveType("Watertiny2", 260, 60, 20, 20, false, null, types.get(57), false); //129 watertiny2
        addWaveType("Watertiny3", 260, 80, 20, 20, false, null, types.get(57), false); //130 watertiny3
        addWaveType("Watertinypath", 20, 20, 20, 20, false, null, types.get(130), false); //131 watertinypath
        addWaveType("Watertinypath2", 20, 20, 20, 20, false, null, types.get(129), false); //132 watertinypath2
        addTreeType("Watertinytree", 80, 220, types.get(130)); //133 watertinytree
        addTreeType("Watertinytree", 80, 220, types.get(129)); //134 watertinytree

        addType("Snowwall", 140, 80, 20, 20, true, null); //135 snowwall
        addWaveType("Igloo1", 160, 260, 20, 20, true, null, types.get(110), false); //136 igloo1
        addWaveType("Igloo2", 180, 260, 20, 20, true, null, types.get(110), false); //137 igloo2
        addWaveType("Igloo3", 200, 260, 20, 20, true, null, types.get(110), false); //138 igloo3
        addWaveType("Igloo4", 160, 280, 20, 20, true, null, types.get(110), false); //139 igloo4
        addWaveType("Igloo5", 180, 280, 20, 20, false, c -> c.setWorld(2), types.get(110), false); //140 igloo5
        addWaveType("Igloo6", 200, 280, 20, 20, true, null, types.get(110), false); //141 igloo6
        addType("Snowtile", 120, 80, 20, 20, false, null); //142 snowwall
        addWaveType("FloorHome", 40, 60, 20, 20, false, c -> c.setWorld(0), types.get(110), false); //143 floorhome
        addWaveType("Oven bottom snow", 140, 280, 20, 20, true, null, types.get(82), false); //144 oven bottom snow
        addWaveType("Oven top snow", 220, 260, 20, 20, true, null, types.get(135), true); //145 oven top snow
        addWaveType("ChestSnow", 160, 140, 20, 20, true, null, types.get(110), true); container(); //146 chest on snow

        //After the final boss: volcano depths (world 4), desert (5), sky ruins (6), crystal caverns (7).
        //Solid things standing on a tile (spires, pillars, crystals, the cactus) do not bob: wave false, but z = 1 so they
        //are drawn among the characters
        //Drafts drawn by tools/art_postgame.py, rows y = 340 and 360
        addType("Basalt", 0, 340, 20, 20, true, null); //147 basalt wall
        addType("Ash", 20, 340, 20, 20, false, null); //148 ash floor
        addWaveType("Vent", 40, 340, 20, 20, false, null, types.get(148), false); //149 glowing vent on ash
        addWaveType("Obsidian spire", 60, 340, 20, 20, true, null, types.get(148), false, 1, 1, 0); //150 obsidian spire
        addType("Dune", 80, 340, 20, 20, true, null); //151 dune wall
        addWaveType("Cactus", 100, 340, 20, 20, true, null, types.get(63), false, 1, 1, 0); //152 cactus on sand
        addType("Desert brick", 120, 340, 20, 20, false, null); //153 desert brick floor
        addWaveType("Ruin pillar", 140, 340, 20, 20, true, null, types.get(63), false, 1, 1, 0); //154 ruin pillar on sand
        addType("Sky", 160, 340, 20, 20, true, null); //155 open sky
        addType("Cloud", 180, 340, 20, 20, false, null); //156 cloud floor
        addType("Marble", 200, 340, 20, 20, false, null); //157 marble floor
        addWaveType("Broken column", 220, 340, 20, 20, true, null, types.get(156), false, 1, 1, 0); //158 broken column on cloud
        addType("Crystal wall", 240, 340, 20, 20, true, null); //159 crystal wall
        addType("Crystal floor", 260, 340, 20, 20, false, null); //160 crystal floor
        addWaveType("Crystal cluster", 280, 340, 20, 20, true, null, types.get(160), false, 1, 1, 0); //161 crystals on crystal floor
        addWaveType("ChestAsh", 160, 140, 20, 20, true, null, types.get(148), true); container(); //162 chest on ash
        addWaveType("ChestCrystal", 160, 140, 20, 20, true, null, types.get(160), true); container(); //163 chest on crystal floor
        //The old one-tile gates (see the portals below, 172-179, which replaced them)
        addWaveType("Gate to volcano", 0, 360, 20, 20, false, sealedGate(4), types.get(58), false); //164 gate to volcano (world 2 -> 4)
        addWaveType("Gate to castle", 0, 360, 20, 20, false, gate(2), types.get(148), false); //165 gate back (4 -> 2)
        addWaveType("Gate to desert", 0, 360, 20, 20, false, gate(5), types.get(148), false); //166 gate (4 -> 5)
        addWaveType("Gate to volcano", 0, 360, 20, 20, false, gate(4), types.get(63), false); //167 gate back (5 -> 4)
        addWaveType("Gate to sky", 0, 360, 20, 20, false, gate(6), types.get(63), false); //168 gate (5 -> 6)
        addWaveType("Gate to desert", 0, 360, 20, 20, false, gate(5), types.get(156), false); //169 gate back (6 -> 5)
        addWaveType("Gate to caverns", 0, 360, 20, 20, false, gate(7), types.get(156), false); //170 gate (6 -> 7)
        addWaveType("Gate to sky", 0, 360, 20, 20, false, gate(6), types.get(160), false); //171 gate back (7 -> 6)
        //164-171 are not on the map any more: the one way out of the final boss's room is a 3x3 portal (portalType: black,
        //with the teleport's stars fixed to the screen showing through). Stepping on any of its tiles takes you to the
        //same tile in the other world, so it has a 3x3 landing spot there, next to the 3x3 portal back
        addPortalType("Portal to the volcano caves", 200, 360, gate(1)); ((portalType) types.get(getLength()-1)).offUntilLit(); //172 portal in the girl's room (2 -> 1), black and solid until it lights after she is taken
        addPortalType("Portal to castle", 200, 360, gate(2)); //173 portal back (1 -> 2)
        //174-179 were portals between areas that are now joined by caves and paths; not on the map
        addPortalType("Portal to desert", 200, 360, gate(5)); //174 (unused)
        addPortalType("Portal to volcano", 200, 360, gate(4)); //175 (unused)
        addPortalType("Portal to sky", 200, 360, gate(6)); //176 (unused)
        addPortalType("Portal to desert", 200, 360, gate(5)); //177 (unused)
        addPortalType("Portal to caverns", 200, 360, gate(7)); //178 (unused)
        addPortalType("Portal to sky", 200, 360, gate(6)); //179 (unused)
        //The pillar (top and base, as in the castle and the caves) on sand and on cloud
        addWaveType("Pillartopsand", 200, 60, 20, 20, true, null, types.get(63), false); //180 pillar top on sand
        addWaveType("Pillarbottomsand", 200, 80, 20, 20, false, null, types.get(63), false); //181 pillar base on sand
        addWaveType("Pillartopsnow", 200, 60, 20, 20, true, null, types.get(110), false); //182 pillar top on snow (the sky ruins)
        addWaveType("Pillarbottomsnow", 200, 80, 20, 20, false, null, types.get(110), false); //183 pillar base on snow
        //The sky ruins (world 4): open sky shows the sky behind (skyLayer), and sky portals lead there. A sky portal is
        //1x1 and on the same tile in both worlds, like the cave passages
        types.add(new openSkyType("Open sky", 160, 340, 20, 20, getLength(), true, null)); //184 open sky (drawn as nothing)
        types.add(new skyPortalType("Sky portal", 160, 340, 20, 20, getLength(), false, gate(4)).offUntilLit(types.get(63))); //185 sky portal to the sky ruins (in the desert and the crystal caverns), sand until the Desert Nomad is beaten
        types.add(new skyPortalType("Sky portal to the desert", 160, 340, 20, 20, getLength(), false, gate(0))); //186 sky portal back to the desert (in the sky ruins)
        types.add(new skyPortalType("Sky portal to the caverns", 160, 340, 20, 20, getLength(), false, gate(1))); //187 sky portal down to the crystal caverns (in the sky ruins)
        //Pillars more than one tile high: the top (93), then middles, then a bottom, then the base (92). Made from the
        //pillar top's own shaft (tools/art_postgame.py)
        addWaveType("Pillarmiddlebluebrick", 240, 380, 20, 20, true, null, types.get(60), false); //188 pillar middle on bluebrick
        addWaveType("Pillarbottombluebrick", 260, 380, 20, 20, true, null, types.get(60), false); //189 pillar bottom (above the base) on bluebrick
        addWaveType("Pillarmiddlesand", 240, 380, 20, 20, true, null, types.get(63), false); //190 pillar middle on sand
        addWaveType("Pillarbottomsand", 260, 380, 20, 20, true, null, types.get(63), false); //191 pillar bottom on sand
        addWaveType("Pillarmiddlesnow", 240, 380, 20, 20, true, null, types.get(110), false); //192 pillar middle on snow
        addWaveType("Pillarbottomsnow", 260, 380, 20, 20, true, null, types.get(110), false); //193 pillar bottom on snow
        //The mirror in the girl's room the crystal boy steps out of (gameHandler.kidnapping), a draft. It stands on the
        //blue brick over lava (119), like the tile left of the pillar next to it, which is what is left when it is gone
        types.add(new mirrorType("Mirror", 260, 360, getLength(), types.get(119))); //194 mirror on bluebrick over lava
        //Every mirror is a way into the mirror dimension or out of it (mirrorType). The one behind the crystal boy in his
        //hall, and the one back out in the mirror dimension (the mirrored house's front door); only the player goes
        //through by walking in, the crystal boy and the girl are taken through by the cutscenes
        types.add(new mirrorType("Mirror to the mirror dimension", 260, 360, getLength(), types.get(119),
            c -> {if (c == c.getHandler().getMainCharacter()) c.getHandler().intoMirrorDimension();}).alwaysOpen()); //195 mirror into the mirror dimension
        types.add(new mirrorType("Mirror out of the mirror dimension", 260, 360, getLength(), types.get(10),
            c -> {if (c == c.getHandler().getMainCharacter()) c.getHandler().outOfMirrorDimension();}).alwaysOpen()); //196 mirror back to the crystal boy's hall
        //The two mirrors that appear on each side of the crystal boy when he blows up for good (gameHandler.becoming): the
        //girl flees into the first, and it is gone; the second is yours, and takes you back to her room, where it began
        types.add(new mirrorType("Mirror the girl flees into", 260, 360, getLength(), types.get(1))); //197 mirror on grass (the girl's)
        types.add(new mirrorType("Mirror back to where it began", 260, 360, getLength(), types.get(1),
            c -> {if (c == c.getHandler().getMainCharacter()) c.getHandler().intoTheLoop();})); //198 mirror on grass (yours)
        //Ladders between the caves and outside (the volcano depths up to the desert), with the pictures of the ladders
        //under the house at the start (31, 32), which go one world up or down instead
        addType("LadderUpToOutside", 60, 140, 20, 20, false, gate(0)); //199 ladder up, caves -> outside
        addType("LadderDownToCaves", 60, 120, 20, 20, false, gate(1)); //200 ladder down, outside -> caves
        //The ways out of the sky ruins are black portals with the teleport's stars (portalType, 3x3, like the one behind
        //the girl); the ways up into the sky (in the desert and the crystal caverns) stay sky portals (185)
        addPortalType("Portal to the crystal caverns", 200, 360, gate(1)); //201 portal in the sky ruins down to the caverns (4 -> 1)
        addPortalType("Portal to the sky ruins", 200, 360, gate(4)); //202 (unused: the way up from the caverns is a sky portal)
        addPortalType("Portal to the desert", 200, 360, gate(0)); //203 portal in the sky ruins back to the desert (4 -> 0)
        addWaveType("ChestGrass", 160, 140, 20, 20, true, null, types.get(1), true); container(); //204 chest on grass (the mirror dimension's)

    }

    //Gives every tile type its own primary colour (tileType.getPrimaryColor), so no two look the same when zoomed far
    //out: grass and trees, or two floors of the same green, would otherwise be the same colour. Each keeps the colour of
    //its texture when nothing near it is taken, or else is moved a little lighter, darker or round in hue until it is
    //MIN_DISTANCE from every colour given before it. The ones set by hand (primary) are kept and go first
    private static final double MIN_DISTANCE = 26;
    //What it gave last time, per tile type (null: kept its own), for the pictures it was worked out from: every game
    //has the same tile types and shares the pictures, so a new game takes them from here
    private static imgDict assignedFor;
    private static javafx.scene.paint.Color[] assigned;

    public void assignPrimaryColors (imgDict images) {
        if (images == assignedFor && assigned != null && assigned.length == types.size()) {
            for (int i = 0; i < types.size(); i++) if (assigned[i] != null) types.get(i).assignPrimaryColor(assigned[i]);
            return;
        }
        javafx.scene.paint.Color[] given = new javafx.scene.paint.Color[types.size()];
        List<javafx.scene.paint.Color> taken = new ArrayList<javafx.scene.paint.Color>();
        for (tileType t : types) {
            if (t.isPrimaryFixed() && t.getPrimaryColor(images) != null) taken.add(t.getPrimaryColor(images));
        }
        for (tileType t : types) {
            if (t.isPrimaryFixed()) continue;
            javafx.scene.paint.Color own = t.getPrimaryColor(images);
            if (own == null) continue;
            javafx.scene.paint.Color unique = uniqueColor(own, taken);
            t.assignPrimaryColor(unique);
            given[types.indexOf(t)] = unique;
            taken.add(unique);
        }
        assignedFor = images;
        assigned = given;
    }

    private static javafx.scene.paint.Color uniqueColor (javafx.scene.paint.Color own, List<javafx.scene.paint.Color> taken) {
        if (nearest(own, taken) >= MIN_DISTANCE) return own;
        javafx.scene.paint.Color best = own;
        double bestDistance = nearest(own, taken);
        for (int k = 1; k <= 12; k++) {
            //Lighter, darker, then round the colour wheel either way, a little further each time
            javafx.scene.paint.Color[] tries = {
                own.deriveColor(0, 1, 1 + 0.12*k, 1),
                own.deriveColor(0, 1, Math.max(0, 1 - 0.1*k), 1),
                own.deriveColor(9*k, 1, 1, 1),
                own.deriveColor(-9*k, 1, 1, 1),
                own.deriveColor(0, Math.max(0, 1 - 0.12*k), 1 + 0.06*k, 1),
            };
            for (javafx.scene.paint.Color c : tries) {
                double d = nearest(c, taken);
                if (d >= MIN_DISTANCE) return c;
                if (d > bestDistance) {best = c; bestDistance = d;}
            }
        }
        return best;
    }

    //How far the colour is from the nearest of the others, in 0-255 RGB
    private static double nearest (javafx.scene.paint.Color c, List<javafx.scene.paint.Color> others) {
        double min = Double.MAX_VALUE;
        for (javafx.scene.paint.Color o : others) {
            double dr = (c.getRed() - o.getRed())*255, dg = (c.getGreen() - o.getGreen())*255, db = (c.getBlue() - o.getBlue())*255;
            min = Math.min(min, Math.sqrt(dr*dr + dg*dg + db*db));
        }
        return min;
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
        types.add(new portalType(name, x, y, guiSkin.CELL, guiSkin.CELL, getLength(), false, pressurePlate));
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
        addWaveType(name, xOverlay, yOverlay, guiSkin.CELL, guiSkin.CELL, true, null, underlay, true, 1, treeHeight, treeScatter);
        sway();
    }

    //Makes the type that was just added show up in the teleports menu (in the hidden secrets settings)
    //The colour the tile added last is drawn in when zoomed far out, for when the one worked out from its texture
    //(the colour most of its pixels have) is not the one it should be
    private void primary (String hex) {
        types.get(getLength()-1).setPrimaryColor(hex);
    }

    private void teleportTarget () {
        types.get(getLength()-1).setTeleportTarget(true);
    }

    //Makes the type that was just added a chest (see chest)
    private void container () {
        types.get(getLength()-1).setContainer(true);
    }

    //Makes the wave type that was just added a plant: it sways in the wind instead of bobbing
    //Makes the type that was just added give under whoever steps on it (waveType.setSpringy)
    private void springy () {
        ((waveType) types.get(getLength()-1)).setSpringy(true);
    }

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
