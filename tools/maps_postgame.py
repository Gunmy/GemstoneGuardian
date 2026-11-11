"""The maps of the areas after the final boss, written as symbols, turned into chunks.txt blocks.

The volcano depths and the crystal caverns are caves (underground, world 1), the desert is outside (world 0), the sky
ruins are their own sky world (4). Each area is one open region of 3x3 chunks (27x27 tiles) with several ways through,
side pockets and optional fights, written as one big grid; the script cuts it into chunks. The legend of an area says
which tile id each symbol is; digits mark where a character stands (floor under them). Pillars are the game's own
pillar: a top with its base right under it, and for taller ones middles and a bottom in between. Caves are walled by
the black void (17), like every other cave in the game.

Doors only change the world, keeping the place. So a door must never lead onto a door: the tiles you arrive on are
floor, and the way back is a door next to them, on the tiles that are floor where you came from (the approach). The
script checks this for every door tile in both directions. The doors:
- the final boss's room (world 2): 3x3 portal (172) in its north wall -> the volcano caves, landing in front of the
  3x3 portal back (173)
- volcano 'U' (199, a ladder up, set in the cave wall like the one under the house at the start) -> the desert's path
  right above its 'D' (200, a ladder down, one row lower, with cacti left, below and right of it, so you climb out
  upwards), which leads to the volcano's floor right below 'U'. As the ladders under the house at the start
- desert 'G' (185, a 3x3 sky portal set in the dunes, entered from the south) -> the sky ruins, landing on the same nine
  tiles; the sky ruins' portal back (186) is the 3x3 just south of them, where the desert's approach is, and the sky
  ruins go on north of the landing
- sky ruins 'X' (201, a black portal with the teleport's stars, 3x3, set in the open sky, entered from the south) ->
  the crystal caverns, the same way: landing on the same tiles, the sky portal back up (185) south of them, the
  caverns going on north. In the sky ruins every way out is a black portal (201, and 203 back to the desert), every way
  up into the sky a sky portal (185)
- crystal caverns 'U' (42, up, next to the Elemental Rock's chest) -> the castle grounds (outside, world 0), onto the
  floor next to their 'D' (41, down); the grounds go on north to the crystal boy's castle. The same pair as the cave
  under the start (chunk 10000, 10002): 42 in the caves, 41 a row lower outside
- the castle's big door (118, 3 tiles wide and 3 deep) -> the castle hall (world 2), whose door back (16) is the row
  under it, on the castle's bridge

The script also checks with a flood fill that every area's way through reaches its exit and every character, chest and
healing pad, that pillar tops have what belongs under them, and that no chunk of the game's own is overwritten. It
prints where the characters stand, and with --write replaces its blocks in chunks.txt (the ones named "PG ...", and any
left in worlds 5 to 7 from an earlier version).

    python tools/maps_postgame.py            check and print
    python tools/maps_postgame.py --write    also write chunks.txt
"""
import os
import random
import sys
from collections import deque

HERE = os.path.dirname(os.path.abspath(__file__))
CHUNKS = os.path.join(HERE, '..', 'src', 'main', 'resources', 'game', 'reading', 'chunks.txt')
LF, CRLF = chr(10), chr(13) + chr(10)
BLOCKED = {147, 150, 151, 152, 155, 159, 161, 162, 163, 82, 57, 17, 93, 180, 182, 184, 188, 189, 104, 190, 191, 192, 193,
           24, 44, 60, 64, 65, 66, 67, 68, 69, 70, 71, 72, 87, 89, 120, 194, 26, 27, 28, 33}
# Tiles that take you to another world when stepped on
DOORS = {3, 16, 29, 30, 31, 32, 40, 41, 42, 43, 103, 118, 124, 125, 140, 143} | set(range(164, 180)) | {185, 186, 187, 195, 196, 197, 198, 199, 200, 201, 202, 203}
# Where each door of these areas leads
LEADS_TO = {172: 1, 173: 2, 40: 1, 41: 1, 42: 0, 43: 0, 185: 4, 186: 0, 187: 1, 118: 2, 16: 0, 199: 0, 200: 1, 201: 1, 202: 4, 203: 0}
# A ladder up and its ladder down are on the same tile, each leading onto the other (see the doors above)
LADDERS = {199, 200}

AREAS = [
    {
        'name': 'Volcano depths', 'world': 1, 'x': 10009, 'y': 9956, 'floor': 148,
        'legend': {'#': 17, '.': 148, 'v': 149, 'S': 150, 'L': 82, 'C': 162, 'H': 91, 'U': 199},
        'cave_walls': True,
        'start': (26, 13), 'exit': 'U',
        'grid': [
            "###########################",
            "#####....S#########U.######",
            "####......#######...#######",
            "###..LL....#######2########",
            "##..LLLL...v####.....S...##",
            "##.LLLLLL.....#..........##",
            "#..LLLLLL..S.....LL..v...##",
            "#...LLLL....1...LLLL.....##",
            "##...LL......LLLLLLLL....##",
            "###.......#..LLLLLLLL..S.##",
            "####.v...###..LLLLLL.....##",
            "###.....#####...LLL...#####",
            "##..S..####C#.........#####",
            "##.......#...#....v.....###",
            "###......LLL......LL....###",
            "####...LLLLLLL...LLLL...###",
            "####..LLLLLLLLL..LLLL..####",
            "###..LLLL..LLLL.....S..####",
            "###.....S....LL.....v...###",
            "##....v..........LL.....###",
            "##..LL.....H.....LLL....###",
            "##.LLLL..........LL..S..###",
            "###.LL...S....#.......#####",
            "####......#####.....#######",
            "#####.....##.......########",
            "##########.....############",
            "############...############"],
    },
    {
        'name': 'Desert', 'world': 0, 'x': 10009, 'y': 9954, 'floor': 63,
        'legend': {'#': 151, '.': 63, ',': 107, 'c': 152, 'b': 153, 'P': 180, 'Q': 181, 'B': 191, '~': 57, 'H': 91, 'D': 200, 'G': 185},
        'start': 'D', 'exit': 'G',
        'grid': [
            "###########BGGGB###########",
            "###########Q,,,Q###########",
            "###########,,,,,###########",
            "###########,,,,,###########",
            "#############4#############",
            "###....bbbbbbbbbbb.....c.##",
            "##..c..bbbbbbbbbbb...~~~..#",
            "##.....bbbbbbbbbbb..~~~~..#",
            "#..c...bbbbb3bbbbb...~~.c.#",
            "#......bbbbbbbbbbb.....H..#",
            "#...c..bbbbbbbbbbb..c.....#",
            "##.......,,,,,,,......c..##",
            "##..PP...,.....,...PP....##",
            "#...QQ..,,..c..,,..QQ..c..#",
            "#.c....,,.......,,........#",
            "#....,,..~~~.....,,...c...#",
            "##..,,..~~~~~.....,,.....##",
            "##..,...~~~~~...c..,,....##",
            "###.,....~~~.......,,,..###",
            "###.,,.........c..,,,...###",
            "####,,,.......c...cDc,..###",
            "#####..,,,,,,,,,,,,c,..####",
            "######..c.....c.....c..####",
            "#######..............######",
            "#########.....c....########",
            "###########....############",
            "###########################"],
    },
    {
        'name': 'Sky ruins', 'world': 4, 'x': 10009, 'y': 9951, 'floor': 110,
        'legend': {'#': 184, '.': 110, 'm': 46, 'K': 182, 'k': 183, 'B': 193, 'H': 91, 'X': 201},
        'start': (26, 13), 'exit': (0, 13),
        'grid': [
            "###########BXXXB###########",
            "###########k...k###########",
            "###########.....###########",
            "###########.....###########",
            "###########.....###########",
            "###########.....###########",
            "#############6#############",
            "############mmm############",
            "###....####.mmm.####....###",
            "##..K...##..mmm..##..K...##",
            "##..k5..mmmmmmmmmmm..k...##",
            "##......m##..m..##m...H..##",
            "###....m###..m..###m.....##",
            "####..m####.....####mm.####",
            "#####.m#####.K.#####.m.####",
            "######m#####.k.#####.m..###",
            "##...mmmmmmmmmmmmmmmmm...##",
            "##..K.###...m...###......##",
            "##..k.###..mmm..###..K..###",
            "###......##m.m##.....k..###",
            "####..mmmmm...mmmmm....####",
            "#####..##.......##..#######",
            "#########.......###########",
            "###########.....###########",
            "############...############",
            "############...############",
            "############...############"],
    },
    {
        'name': 'Crystal caverns', 'world': 1, 'x': 10009, 'y': 9949, 'floor': 160,
        'legend': {'#': 17, '.': 160, 'x': 161, 'p': 104, 'C': 163, 'H': 91, 'W': 185, 'U': 42},
        'cave_walls': True,
        'start': (18, 13), 'exit': (2, 13),
        'grid': [
            "############U##############",
            "###########x.Cxx###########",
            "###########.....###########",
            "############...############",
            "#############8#############",
            "############...############",
            "####...####.......####...##",
            "###..x..##...ppp...##..x..#",
            "##..ppp.....ppppp.....pp..#",
            "##.ppppp..x..ppp..x..ppp..#",
            "##..ppp.......7.......p...#",
            "###.....x.........x.....###",
            "####..H.....x.x.......#####",
            "#####.....##...##....######",
            "####..x..###...###...x..###",
            "###..ppp.###...###..ppp..##",
            "##..ppppp.##...##..ppppp.##",
            "##...ppp..##...##..ppp...##",
            "###.......##...##.......###",
            "############WWW############",
            "############WWW############",
            "############WWW############",
            "###########################",
            "###########################",
            "###########################",
            "###########################",
            "###########################"],
    },
    {
        # Out of the crystal caverns (the way up next to the Elemental Rock's chest) into the open again, and north up a
        # long avenue to the crystal boy's castle. 5x8 chunks: the castle is the top three, drawn after the final castle
        # (chunk 10014, 9963) but grander: tall corner towers, a keep behind crested curtain walls, gate towers round
        # the big door, all of the castle blocks with glowing lava windows; a lava moat with a brick bridge, and a
        # plaza with pillars and a healing pad. The avenue (brick paving, three wide) is lined with columns, some
        # fallen, and ruined buildings: old floors, a broken stretch of castle wall, rubble. A chest is hidden in the
        # trees to the east. 's' are where the crystal boy's servants stand as statues once beaten (gameHandler
        # statueSpots), on the grass beside the avenue: the first ones met nearest the cave, the last by the castle
        'name': 'Castle grounds', 'world': 0, 'x': 10008, 'y': 9942, 'floor': 1,
        'legend': {'#': 24, '.': 1, ',': 2, 'r': 62, 'f': 55, 'F': 56, 'o': 44, '~': 57, '=': 58, 'k': 83, 'L': 82,
                   'T': 64, 't': 65, 'Y': 66, 'M': 67, 'm': 68, 'N': 69, 'B': 70, 'b': 71, 'V': 72, 'w': 119,
                   '^': 61, '_': 120, '|': 118, 'P': 93, 'p': 92, 'W': 60, 'H': 91, 'C': 87, 'D': 41, 's': 1},
        'start': (63, 21), 'exit': (19, 22),
        'grid': [
            "#############################################",
            "TtYTtY###^^^^^^^^^^^^^^^^^^^^^^^^^^^###TtYTtY",
            "MwNMmN###TtYTtYTtYTtYTtYTtYTtYTtYTtY###MmNMwN",
            "MmNMwN###MmNMwNMmNMwNMmNMwNMmNMwNMmN###MwNMmN",
            "MwNMmN###MwNMmNTtYTtYTtYTtYTtYMmNMwN###MmNMwN",
            "MmNMwN###MmNMwNMwNMmNMwNMmNMwNMwNMmN###MwNMmN",
            "MwNMmN###MwNMmNMmNMwNMmNMwNMmNMmNMwN###MmNMwN",
            "MmNMwN###MmNMwNMwNMmNMwNMmNMwNMwNMmN###MwNMmN",
            "MwNMmN###BbVBbVMmNMwNMmNMwNMmNBbVBbV###MmNMwN",
            "MmNMwN^^^^^^^^^MwNMmNMwNMmNMwN^^^^^^^^^MwNMmN",
            "MwNMmNTtYTtYTtYMmNMwNMmNMwNMmNTtYTtYTtYMmNMwN",
            "MmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmN",
            "MwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwN",
            "MmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmN",
            "MwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwNMmNMwN",
            "MmNMwNMmNMwNMmNMwNMmNBbVMmNMwNMmNMwNMmNMwNMmN",
            "MwNMmNMwNMmNMwNMmNMwN___MwNMmNMwNMmNMwNMmNMwN",
            "MmNMwNMmNMwNMmNMwNMmN|||MmNMwNMmNMwNMmNMwNMmN",
            "MwNMmNMwNMmNMwNMmNMwN|||MwNMmNMwNMmNMwNMmNMwN",
            "BbVBbVBbVBbVBbVBbVBbV|||BbVBbVBbVBbVBbVBbVBbV",
            "LLLLLLLLLLLLLLLLLLLLL===LLLLLLLLLLLLLLLLLLLLL",
            "LLLLLLLLLLLLLLLLLLLLL=k=LLLLLLLLLLLLLLLLLLLLL",
            "##=========================================##",
            "##=P=====P=====P=============P=====P=====P=##",
            "##=p=====p=====p======H======p=====p=====p=##",
            "##====w=====w===================w=====w====##",
            "##=========================================##",
            "##...................rrr...................##",
            "###.#..............=.rrr.=..............#.###",
            "###.........f......P.rrr.P...W............###",
            "##....TtYTtYr=r....p.rrrsp.................##",
            "##....BbVBbV===......rrr...................##",
            "###...==r==r==.....=.rrr.=.f..............###",
            "###....W=rr==P=....P.rrr.P.......TtYr.=...###",
            "##....r=r==r=pr...Fpsrrr.p.......BbV===....##",
            "##.......o...........rrr.........===.==....##",
            "###.#..............=.rrr.=.......=W=rPF...###",
            "###.#..........W...P.rrr.P.......==rrpr...###",
            "##..............W..p.rrrsp..............#..##",
            "##...................rrr...F.............#.##",
            "###................=.rrr.=....TtYTtY.=r...###",
            "###......F.........P.rrr.P....BbVBbVr==...###",
            "##.................psrrr.p.....r=r=r=rr....##",
            "##.............~~~...rrr......=W.==rrr=....##",
            "###.....#.....~~~~.=.rrr.=....rf=rr==r=...###",
            "###.....#.....~~~~.P.rrr.P..r.............###",
            "##.........W..~~~..p.rrrsp...........########",
            "##...................rrr.............#==C####",
            "###..TtY===........=.rrr.=...........#===####",
            "###..BbVrrr........P.rrr.Pf..........#===####",
            "##....rr=r=........psrrr.p...........#===####",
            "##...=Wr=P...........rrr.............##.#####",
            "###..==..p...TtYrr.=.rrr.=...........##.#####",
            "###..........BbV=.rP.rrr.P..........f##.#####",
            "##...........======p.rrrsp.................##",
            "##............W==P=..rrr...................##",
            "###..........===rp==.rrr.=.....TtYr.=.....###",
            "###................P.rrr.P.W...BbV=r=.....###",
            "##........F........psrrr.p.....r====r......##",
            "##...............r...rrr.......=W==P=......##",
            "###..#...............,rr...f...====pr..#..###",
            "###...#..............,....................###",
            "##..............F....,.....................##",
            "###############......,.......################",
            "#####################D#######################",
            "#############################################",
            "#############################################",
            "#############################################",
            "#############################################",
            "#############################################",
            "#############################################",
            "#############################################"],
    },
    {
        # Inside the big door: the crystal boy's throne hall (one chunk wide, its doors back at the castle's bridge).
        # At its top end he (9) stands in the doorway of a room (5x5) with a mirror in its north wall (M, 195, the way
        # into the mirror dimension), and the girl (G) is held in a stone niche beside him: he is the only way to her and
        # to it. The doors back (D) stay under the castle's big door
        'name': 'Castle hall', 'world': 2, 'x': 10010, 'y': 9942, 'floor': 58,
        'legend': {'#': 17, '=': 58, 'W': 60, 'l': 119, 'L': 82, 'M': 195, 'P': 93, 'p': 92, 'G': 58, 'D': 16,
                   'a': 94, 'b': 95, 'c': 96, 'd': 97, 'e': 98, 'f': 99, 'g': 100, 'h': 101, 'i': 102},
        'start': (19, 4), 'exit': (2, 4),
        'grid': [
            "WWWWMWWWW",
            "WWl===lWW",
            "WW=====WW",
            "WW=====WW",
            "WW=====WW",
            "WWl===lWW",
            "WWWG9WWWW",
            "W==W=W==W",
            "W=abbbc=W",
            "W=deeef=W",
            "WPdeeefPW",
            "WpdeeefpW",
            "W=deeef=W",
            "W=ghhhi=W",
            "WL=====LW",
            "Wl=====lW",
            "WL=====LW",
            "W=======W",
            "WP=====PW",
            "Wp=====pW",
            "###DDD###",
            "#########",
            "#########",
            "#########",
            "#########",
            "#########",
            "#########"],
    },
]

# Single chunks: the top of the boss's portal (north of her room), the landing in the volcano caves, the top two rows
# of the desert's sky portal (in the dunes north of the desert), and where you land coming back from the sky ruins
EXTRA = [
    (2, 10010, 9958, 'PG Portal in the wall', [
        "#########", "#########", "#########", "#########", "#########", "#########", "#########",
        "##|PPP|##", "##:PPP:##"], {'#': 17, '|': 93, ':': 188, 'P': 172}),
    (1, 10010, 9959, 'PG Volcano landing', [
        "###...###", "###RRR###", "###RRR###", "###RRR###", "#########", "#########", "#########",
        "#########", "#########"], {'#': 17, '.': 148, 'R': 173}),
    (0, 10010, 9953, 'PG Desert sky portal', [
        "#########", "#########", "#########", "#########", "#########", "#########", "#########",
        "##|PPP|##", "##:PPP:##"], {'#': 151, '|': 180, ':': 190, 'P': 185}),
    (4, 10010, 9950, 'PG Sky ruins portal down', [
        "#########", "#########", "#########", "#########", "#########", "#########", "#########",
        "##|PPP|##", "##:PPP:##"], {'#': 184, '|': 182, ':': 192, 'P': 201}),
    (4, 10010, 9954, 'PG Sky ruins landing', [
        "###...###", "###RRR###", "###RRR###", "###RRR###", "#########", "#########", "#########",
        "#########", "#########"], {'#': 184, '.': 110, 'R': 203}),
]


def find(area, symbol):
    for r, row in enumerate(area['grid']):
        if symbol in row:
            return r, row.index(symbol)
    raise ValueError('%s has no %s' % (area['name'], symbol))


def absolute(area, r, c):
    """(world, chunk x, chunk y, column, row) of grid cell r, c"""
    return area['world'], area['x'] + c // 9, area['y'] + r // 9, c % 9, r % 9


# Caves (an area with 'cave_walls') get the wall faces of the game's own caves: the void right above anything with floor
# in it (floor, and what stands on floor: crystals, vents, spires, chests, pads) is a cave wall, the left end (26),
# middle (27) and right end (28) of each stretch along a row; one tile wide it is a middle piece too (the dirt cave
# walls only: 33 is the plain wall from inside the houses).
# Not over lava, toxic pools, water or doors
CAVE_WALL_LEFT, CAVE_WALL_MIDDLE, CAVE_WALL_RIGHT, CAVE_WALL_SINGLE = 26, 27, 28, 27
NOT_FLOOR = {17, 82, 104, 57}
_walls = {}


def raw_id(area, r, c):
    ch = area['grid'][r][c]
    return area['floor'] if ch.isdigit() else area['legend'][ch]


def cave_walls(area):
    """{(r, c): wall tile} of an area with 'cave_walls'"""
    key = area['name']
    if key in _walls:
        return _walls[key]
    grid = area['grid']
    faces = set()
    for r in range(len(grid) - 1):
        for c in range(len(grid[0])):
            below = raw_id(area, r + 1, c)
            if raw_id(area, r, c) == 17 and below not in NOT_FLOOR and below not in DOORS:
                faces.add((r, c))
    walls = {}
    for r, c in faces:
        left, right = (r, c - 1) in faces, (r, c + 1) in faces
        walls[(r, c)] = CAVE_WALL_MIDDLE if left and right else CAVE_WALL_RIGHT if left else CAVE_WALL_LEFT if right else CAVE_WALL_SINGLE
    _walls[key] = walls
    return walls


def tile_id(area, r, c):
    if area.get('cave_walls') and (r, c) in cave_walls(area):
        return cave_walls(area)[(r, c)]
    return raw_id(area, r, c)


def everything():
    """Every tile these maps write: {(world, chunk x, chunk y, column, row): id}"""
    tiles = {}
    for area in AREAS:
        for r in range(len(area['grid'])):
            for c in range(len(area['grid'][0])):
                tiles[absolute(area, r, c)] = tile_id(area, r, c)
    for world, x, y, name, rows, legend in EXTRA:
        for r, row in enumerate(rows):
            for c, ch in enumerate(row):
                tiles[(world, x, y, c, r)] = legend[ch]
    return tiles


def check(area):
    grid = area['grid']
    height, width = len(grid), len(grid[0])
    assert height % 9 == 0 and width % 9 == 0 and all(len(r) == width for r in grid), \
        (area['name'], [(i, len(r)) for i, r in enumerate(grid) if len(r) != width])
    start = find(area, area['start']) if isinstance(area['start'], str) else area['start']
    goal = find(area, area['exit']) if isinstance(area['exit'], str) else area['exit']
    assert tile_id(area, *start) not in BLOCKED, '%s: the start %s is blocked' % (area['name'], start)
    seen, todo = {start}, deque([start])
    while todo:
        r, c = todo.popleft()
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (r + dr, c + dc)
            if 0 <= n[0] < height and 0 <= n[1] < width and n not in seen and tile_id(area, *n) not in BLOCKED:
                seen.add(n)
                # A door is a way out, not through
                if tile_id(area, *n) not in DOORS:
                    todo.append(n)
    assert goal in seen, '%s: the way does not reach the exit %s' % (area['name'], goal)
    people = {}
    for r, row in enumerate(grid):
        for c, ch in enumerate(row):
            if ch.isdigit():
                assert (r, c) in seen, '%s: character %s can not be reached' % (area['name'], ch)
                people[ch] = absolute(area, r, c)
            if ch in 'CH':
                near = [(r + dr, c + dc) for dr in (-1, 0, 1) for dc in (-1, 0, 1)]
                assert any(n in seen for n in near), '%s: %s at %s can not be reached' % (area['name'], ch, (r, c))
            # A pillar top has its base (or a middle or bottom) right under it
            if tile_id(area, r, c) in (180, 182):
                below = tile_id(area, r + 1, c) if r + 1 < height else None
                assert below in (181, 183, 190, 191, 192, 193), '%s: pillar at %s has nothing under it' % (area['name'], (r, c))
    return people


def check_doors(tiles):
    """A door never leads onto a door, and what it leads onto is floor"""
    for (world, x, y, c, r), tid in tiles.items():
        if tid not in LEADS_TO:
            continue
        other = LEADS_TO[tid]
        there = tiles.get((other, x, y, c, r))
        assert there is not None, 'Door %s at %s leads to world %d where these maps have no tile' % (tid, (world, x, y, c, r), other)
        if tid in LADDERS and there in LADDERS:
            continue
        assert there not in DOORS, 'Door %s at %s leads onto door %s in world %d' % (tid, (world, x, y, c, r), there, other)
        assert there not in BLOCKED, 'Door %s at %s leads into a wall (%s) in world %d' % (tid, (world, x, y, c, r), there, other)


def blocks(area):
    out = []
    for cy in range(len(area['grid']) // 9):
        for cx in range(len(area['grid'][0]) // 9):
            world, x, y, _, _ = absolute(area, cy*9, cx*9)
            lines = ['%d, %d, %d, PG %s %d %d' % (world, x, y, area['name'], cx, cy)]
            for r in range(cy*9, cy*9 + 9):
                lines.append(', '.join(str(tile_id(area, r, c)) for c in range(cx*9, cx*9 + 9)))
            out.append(LF.join(lines))
    return out


def finale_rows():
    """The final boss's room's first three rows: the bottom row of its portal in the north wall, framed by the pillar
    bottoms, an alcove of floor in front of it, and the mirror (194) the crystal boy steps out of on the wall west of it, with cracked brick over the lava (83) in
    the niche in front of it, so it looks like it can be walked up to"""
    return ['93, 194, 189, 172, 172, 172, 189, 119, 93',
            '92, 83, 92, 58, 58, 58, 92, 82, 92',
            '83, 58, 58, 58, 58, 58, 58, 58, 83']


# Plain land all round an area, so its edge is not a wall of trees: the base tile with now and then something on it
# (the desert is surrounded by dune wall)
# (tile, chance per 2x2 spot), seeded so it never changes. Chunks x0..x1, y0..y1; never over a chunk of these maps or
# of the game's own
FILLERS = [
    {'name': 'Desert surroundings', 'world': 0, 'x': (10007, 10013), 'y': (9952, 9958), 'base': 151, 'seed': 63,
     'specks': []},
]


# The mirror dimension (world 5): the starting village (outside, chunks x 9999-10003, y 9995-10002) copied with left
# and right swapped: chunk x becomes MIRROR_AXIS - x and each row is turned round. The game draws every tile of a
# mirror world flipped too (chunks.mirror()), so the copy is a true mirror image, cast in cold blues. Doors and cave
# passages are walled up: each takes the tile beside it in its own wall (walled_up), so a house's door becomes the wall
# piece of the row it is in (grandma's house has its door in a middle row, the others in their bottom row); house 1's
# front door is the mirror back to the castle hall (196), and you
# arrive on the path in front of it. The crystal boy waits at the top of the path (MIRROR_BOSS)
MIRROR_WORLD = 5
MIRROR_SOURCE = {'world': 0, 'x': (9999, 10003), 'y': (9995, 10002)}
MIRROR_AXIS = 20002
# (source chunk x, y, column, row): house 1's front door, and the tile in front of it
MIRROR_BACK = (10000, 10000, 4, 6)
MIRROR_ARRIVAL = (10000, 10000, 4, 7)
MIRROR_BOSS = (10000, 9995, 4, 4)
# The two mirrors that appear on each side of him when he blows up (gameHandler.becoming), where he has stepped back
# to, three tiles behind where he waited: the girl flees into the left one (197, gone after), the right one (198) is
# yours. In mirror dimension coordinates: (chunk x, y, column, row)
MIRROR_TWINS = [(10002, 9995, 3, 1, 197), (10002, 9995, 5, 1, 198)]


def own_blocks():
    """{(world, x, y): rows} of every chunk in chunks.txt that these maps did not write"""
    out = {}
    for block in open(CHUNKS, 'rb').read().decode('utf-8').replace(CRLF, LF).split(LF + LF):
        lines = [l for l in block.strip().split(LF) if l.strip()]
        if not lines or ', PG ' in lines[0]:
            continue
        heads, rows = [], []
        for line in lines:
            parts = [q.strip() for q in line.split(',') if q.strip()]
            if len(parts) < 9 and len(parts) >= 3 and parts[0].lstrip('-').isdigit():
                heads.append((int(parts[0]), int(parts[1]), int(parts[2])))
            elif len(parts) >= 9:
                rows.append([int(q) for q in parts[:9]])
        for h in heads:
            out[h] = rows
    return out


def mirrored (x, c):
    """Where a source tile (chunk x, column) ends up in the mirror dimension"""
    return MIRROR_AXIS - x, 8 - c


def walled_up (rows, r, c):
    """What a door at (r, c) becomes in the mirror dimension: the tile beside it in its row, left if that is not a door
    too, else right"""
    for cc in (c - 1, c + 1):
        if 0 <= cc < 9 and rows[r][cc] not in DOORS:
            return rows[r][cc]
    return rows[r][c]


def mirror_blocks():
    own = own_blocks()
    out = []
    bx, by, bc, br = MIRROR_BACK
    for y in range(MIRROR_SOURCE['y'][0], MIRROR_SOURCE['y'][1] + 1):
        for x in range(MIRROR_SOURCE['x'][0], MIRROR_SOURCE['x'][1] + 1):
            rows = own.get((MIRROR_SOURCE['world'], x, y))
            if rows is None:
                continue
            walled = [[walled_up(rows, r, c) if v in DOORS else v for c, v in enumerate(row)] for r, row in enumerate(rows)]
            copy = [list(reversed(row)) for row in walled]
            if (x, y) == (bx, by):
                copy[br][8 - bc] = 196
            for tx, ty, tc, tr, tid in MIRROR_TWINS:
                if (MIRROR_AXIS - x, y) == (tx, ty):
                    copy[tr][tc] = tid
            out.append(LF.join(['%d, %d, %d, PG Mirror dimension %d %d' % (MIRROR_WORLD, MIRROR_AXIS - x, y, x, y)]
                               + [', '.join(map(str, row)) for row in copy]))
    for name, (x, y, c, r) in (('back mirror', MIRROR_BACK), ('arrival', MIRROR_ARRIVAL), ('crystal boy', MIRROR_BOSS)):
        mx, mc = mirrored(x, c)
        print('Mirror dimension %s: world %d, x %d + %d/9, y %d + %d/9' % (name, MIRROR_WORLD, mx, mc, y, r))
    return out


def own_chunks():
    """(world, x, y) of every chunk in chunks.txt that these maps did not write"""
    own = set()
    for block in open(CHUNKS, 'rb').read().decode('utf-8').replace(CRLF, LF).split(LF + LF):
        lines = block.strip().split(LF)
        if not lines or ', PG ' in lines[0]:
            continue
        for line in lines:
            parts = [q.strip() for q in line.split(',')]
            if len(parts) < 9 and len(parts) >= 3 and parts[0].lstrip('-').isdigit():
                own.add((int(parts[0]), int(parts[1]), int(parts[2])))
    return own


def filler_blocks(tiles):
    used = {(w, x, y) for (w, x, y, c, r) in tiles} | own_chunks()
    out = []
    for f in FILLERS:
        for y in range(f['y'][0], f['y'][1] + 1):
            for x in range(f['x'][0], f['x'][1] + 1):
                if (f['world'], x, y) in used:
                    continue
                rnd = random.Random(f['seed'] * 1000003 + x * 7919 + y)
                rows = [[f['base']] * 9 for _ in range(9)]
                for r in range(0, 9, 2):
                    for c in range(0, 9, 2):
                        for tid, chance in f['specks']:
                            if rnd.random() < chance:
                                rows[r][c] = tid
                                # dune walls lie in little ridges
                                if tid == 151 and c + 1 < 9:
                                    rows[r][c + 1] = tid
                out.append(LF.join(['%d, %d, %d, PG %s' % (f['world'], x, y, f['name'])] + [', '.join(map(str, row)) for row in rows]))
    return out


def main():
    text = []
    for area in AREAS:
        for d, (w, x, y, c, r) in sorted(check(area).items()):
            print('%s character %s: world %d, x %d + %d/9, y %d + %d/9' % (area['name'], d, w, x, c, y, r))
        text.extend(blocks(area))
    for area in AREAS:
        for r, row in enumerate(area['grid']):
            for c, ch in enumerate(row):
                if ch == 's':
                    w, x, y, cc, rr = absolute(area, r, c)
                    print('%s statue spot: world %d, x %d + %d/9, y %d + %d/9' % (area['name'], w, x, cc, y, rr))
    tiles = everything()
    # The boss's room is the game's own chunk; its first rows are part of the portal
    for r, row in enumerate(finale_rows()):
        for c, v in enumerate(row.split(',')):
            tiles[(2, 10010, 9959, c, r)] = int(v)
    # Coming back from the volcano you land in her room, on its floor (rows 1-3 under the portal's bottom row)
    for r in (1, 2, 3):
        for c in (3, 4, 5):
            tiles.setdefault((2, 10010, 9959, c, r), 58)
    check_doors(tiles)
    for world, x, y, name, rows, legend in EXTRA:
        text.append(LF.join(['%d, %d, %d, %s' % (world, x, y, name)] + [', '.join(str(legend[ch]) for ch in row) for row in rows]))
    text.extend(filler_blocks(tiles))
    text.extend(mirror_blocks())
    print('All ways connect, and no door leads onto a door')
    if '--write' not in sys.argv:
        return
    raw = open(CHUNKS, 'rb').read().decode('utf-8')
    nl = CRLF if CRLF in raw else LF
    s = raw.replace(CRLF, LF)

    def generated(block):
        head = block.strip().split(LF)[0]
        return head.split(',')[0].strip() in ('5', '6', '7') or ', PG ' in head
    kept = [b for b in s.split(LF + LF) if b.strip() and not generated(b)]
    # Nothing of the game's own may sit where these chunks go
    taken = set()
    for b in kept:
        for line in b.strip().split(LF):
            parts = [p.strip() for p in line.split(',')]
            if len(parts) < 9:
                taken.add(tuple(parts[:3]))
    for block in text:
        parts = [p.strip() for p in block.split(LF)[0].split(',')]
        assert tuple(parts[:3]) not in taken, 'Chunk %s is in chunks.txt already' % (parts[:3],)
    s = (LF + LF).join(b.strip(LF) for b in kept) + LF + LF + (LF + LF).join(text) + LF
    header = '2, 10010, 9959, finale fight' + LF
    a = s.index(header) + len(header)
    rows = s[a:].split(LF)
    rows[0:3] = finale_rows()
    s = s[:a] + LF.join(rows)
    open(CHUNKS, 'wb').write(s.replace(LF, nl).encode('utf-8'))
    print('Wrote chunks.txt')


if __name__ == '__main__':
    main()
