"""Drafts for the areas after the final boss (volcano depths, desert, sky ruins, crystal caverns), their characters,
and the trinkets. Every one is 20x20 art scaled 5x, in the colours of the existing art only, with the #404868
outline on characters and objects (not plants), and specks at least 2x2 (see pixelart.py). The trinket icons are the
owner's (redrawn from earlier drafts) and are not in here.

    python tools/art_postgame.py            draws the cells that are still empty
    python tools/art_postgame.py --redraw   draws them all again: only while every cell is still a draft from here,
                                            it overwrites redrawn ones

The owner redraws them. Rows y = 1700 (tiles) and y = 1800 (gate and characters).
"""
import random
import sys

from pixelart import Sheet, noise, outline, OUTLINE

CELLS = {
    'basalt': (0, 1700), 'ash': (100, 1700), 'vent': (200, 1700), 'spire': (300, 1700),
    'dune': (400, 1700), 'cactus': (500, 1700), 'desert_brick': (600, 1700), 'ruin_pillar': (700, 1700),
    'sky': (800, 1700), 'cloud': (900, 1700), 'marble': (1000, 1700), 'broken_column': (1100, 1700),
    'crystal_wall': (1200, 1700), 'crystal_floor': (1300, 1700), 'crystal_cluster': (1400, 1700),
    'gate': (0, 1800),
    'ember_wraith': (100, 1800), 'ash_golem': (200, 1800), 'dune_scorpion': (300, 1800), 'desert_nomad': (400, 1800),
    'storm_harpy': (500, 1800), 'cloud_knight': (600, 1800), 'crystal_sentinel': (700, 1800), 'prism_warden': (800, 1800),
    'goggles': (900, 1800),
}

ART = {}


def framed(rows, palette):
    """A character or object: outlined in #404868"""
    p = dict(palette)
    p['o'] = OUTLINE
    return outline(rows), p


# ---- Volcano depths -------------------------------------------------------------------------------------------

def basalt():
    # Columns of basalt seen from the side: 4 wide faces between dark seams, a light top every 10 rows, the rows of
    # columns shifted against each other. Everything repeats within the 20 pixels, so tiles join seamlessly
    rows = []
    for y in range(20):
        row = ''
        for x in range(20):
            col = (x + (2 if (y // 10) % 2 else 0)) % 5
            if col == 0:
                row += 'k'
            elif y % 10 in (0, 1):
                row += 'c' if y % 10 == 0 else 'a'
            else:
                row += 'a' if col in (1, 2) else 'b'
        rows.append(row)
    return rows
ART['basalt'] = (basalt(), {'k': '#232631', 'a': '#3C3C3C', 'b': '#303030', 'c': '#515151'})

# Floors stay calm: a few specks of a near shade, nothing more
ART['ash'] = (noise('a', [('b', 0.06), ('e', 0.012)], 148),
              {'a': '#6E6E6E', 'b': '#808088', 'c': '#5E5E5E', 'e': '#DD0B00'})

# 'o' is the outline in framed(): the vent's orange is another letter
ART['vent'] = framed([r.replace('o', 'g') for r in [
    "....................",
    "....................",
    "....................",
    "....................",
    "......dddddddd......",
    ".....ddrrrrrrdd.....",
    "....ddrrgggggrrd....",
    "....drrggyyyggrd....",
    "....drgyywwwyygd....",
    "....drgywwwwwygd....",
    "....drgyywwwyygd....",
    "....drrggyyyggrd....",
    "....ddrrgggggrrd....",
    ".....ddrrrrrrdd.....",
    "......dddddddd......",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
]], {'d': '#303030', 'r': '#DD0B00', 'g': '#FC9F00', 'y': '#FFE500', 'w': '#FFF0B0'})

ART['spire'] = framed([
    "....................",
    ".........kk.........",
    ".........pk.........",
    "........ppbk........",
    "........pbbk........",
    ".......ppbbbk.......",
    ".......pbbbbk.......",
    "......ppbbbbbk......",
    "......pbbbbbbk......",
    ".....ppbbbbbbbk.....",
    ".....pbbbbbbbbk.....",
    "....ppbbbbbbbbbk....",
    "....pbbbbbbbbbbk....",
    "...ppbbbbbbbbbbbk...",
    "...pbbbbbbbbbbbbk...",
    "..ppbbbbbbbbbbbbbk..",
    "..kkkkkkkkkkkkkkkk..",
    "....................",
    "....................",
    "....................",
], {'k': '#232631', 'b': '#41475B', 'p': '#675EC1'})

# ---- Desert ----------------------------------------------------------------------------------------------------

def dune():
    # Ripples two pixels thick running across the sand
    rows = []
    for y in range(20):
        row = ''
        for x in range(20):
            import math
            band = (y + int(round(2*math.sin(2*math.pi*x/20)))) % 10
            row += 'c' if band in (0, 1) else ('b' if band in (5, 6) else 'a')
        rows.append(row)
    return rows
ART['dune'] = (dune(), {'a': '#E8E0A0', 'b': '#EFE7A5', 'c': '#D0B068'})

# The cactus is a plant: the tree's greens and no outline
ART['cactus'] = ([
    "....................",
    ".........kk.........",
    "........kggk........",
    "........kglk........",
    "........kglk........",
    "...kk...kglk........",
    "..kggk..kglk...kk...",
    "..kglk..kglk..kggk..",
    "..kglk..kglk..kglk..",
    "..kglkkkkglk..kglk..",
    "..kgllllggglkkkglk..",
    "...kkkkkkglllllglk..",
    "........kglkkkkkk...",
    "........kglk........",
    "........kglk........",
    "........kglk........",
    "........kglk........",
    ".......kkggkk.......",
    "......ddddddddd.....",
    "....................",
], {'k': '#1B895B', 'g': '#20A36E', 'l': '#23AF75', 'd': '#D0B068'})

def desert_brick():
    rows = []
    for y in range(20):
        row = ''
        for x in range(20):
            offset = 5 if (y // 5) % 2 else 0
            if y % 5 == 4 or (x + offset) % 10 in (8, 9):
                row += 'm'
            elif y % 5 == 0:
                row += 'b'
            else:
                row += 'a'
        rows.append(row)
    return rows
ART['desert_brick'] = (desert_brick(), {'a': '#D8C880', 'b': '#EFE7A5', 'm': '#C8C078'})

ART['ruin_pillar'] = framed([
    "....................",
    "....................",
    "......aa..aaa.......",
    "......baa.bba.......",
    "......baaabba.......",
    ".....bbaaaaaaa......",
    ".....bbaaaaaaa......",
    "......baammaa.......",
    "......baammaa.......",
    "......baaaaaa.......",
    "......baammaa.......",
    "......baammaa.......",
    "......baaaaaa.......",
    "......baammaa.......",
    "......baammaa.......",
    ".....bbaaaaaaa......",
    "....bbbbbbbbbbbb....",
    "....mmmmmmmmmmmm....",
    "....................",
    "....................",
], {'a': '#D8C880', 'b': '#E8E0A0', 'm': '#C8C078'})

# ---- Sky ruins -------------------------------------------------------------------------------------------------

ART['sky'] = (noise('a', [('b', 0.04)], 155), {'a': '#8EDCF2', 'b': '#A0D0E0', 'c': '#FFFFFF'})

def cloud():
    rnd = random.Random(156)
    rows = [['a'] * 20 for _ in range(20)]
    for _ in range(6):
        cx, cy, r = rnd.randrange(20), rnd.randrange(20), rnd.randrange(3, 5)
        for y in range(20):
            for x in range(20):
                dx = min(abs(x - cx), 20 - abs(x - cx))
                dy = min(abs(y - cy), 20 - abs(y - cy))
                d = dx * dx + dy * dy
                if d <= r * r:
                    rows[y][x] = 'b' if d > (r - 2) * (r - 2) else 'c'
    return [''.join(r) for r in rows]
ART['cloud'] = (cloud(), {'a': '#FFFFFF', 'b': '#D8DCE4', 'c': '#F0F0F4'})

def marble():
    rows = []
    for y in range(20):
        row = ''
        for x in range(20):
            if x % 10 == 0 or y % 10 == 0:
                row += 'g'
            elif (x - y) % 10 in (3, 4) and (x // 10 + y // 10) % 2 == 0:
                row += 'v'
            else:
                row += 'a' if ((x // 10) + (y // 10)) % 2 == 0 else 'b'
        rows.append(row)
    return rows
ART['marble'] = (marble(), {'a': '#F7F7F7', 'b': '#EFEFEF', 'g': '#D8DCE4', 'v': '#C8C8C8'})

ART['broken_column'] = framed([
    "....................",
    "....................",
    "....................",
    ".........a.aa.......",
    "........aaabba......",
    ".......aaabbbba.....",
    ".......awabbbsa.....",
    ".......awabbbsa.....",
    ".......awabbbsa.....",
    ".......awabbbsa.....",
    ".......awabbbsa.....",
    ".......awabbbsa.....",
    ".......awabbbsa.....",
    ".......awabbbsa.....",
    "......aawabbbsaa....",
    ".....aaaaaaaaaaaa...",
    "....bbbbbbbbbbbbbb..",
    "....ssssssssssssss..",
    "....................",
    "....................",
], {'a': '#F7F7F7', 'b': '#E2E2D3', 's': '#D0D0C0', 'w': '#FFFFFF'})

# ---- Crystal caverns -------------------------------------------------------------------------------------------

def crystal_wall():
    # Facets: triangles of crystal two pixels and up wide in the dark rock
    rnd = random.Random(159)
    rows = [['a'] * 20 for _ in range(20)]
    for _ in range(6):
        x0, y0, size = rnd.randrange(20), rnd.randrange(20), rnd.randrange(4, 7)
        for i in range(size):
            for j in range(-(i // 2) - 1, i // 2 + 1):
                x, y = (x0 + j) % 20, (y0 + i) % 20
                rows[y][x] = 'c' if j < 0 else 'b'
    return [''.join(r) for r in rows]
ART['crystal_wall'] = (crystal_wall(), {'a': '#232631', 'b': '#675EC1', 'c': '#857BEF'})

ART['crystal_floor'] = (noise('a', [('b', 0.06), ('s', 0.012)], 160),
                        {'a': '#464C61', 'b': '#545B75', 'c': '#41475B', 's': '#8EDCF2'})

ART['crystal_cluster'] = framed([
    "....................",
    "....................",
    "........cc..........",
    "........cp......cc..",
    ".......ccpp.....cp..",
    ".......cwpp....ccpp.",
    "..cc...cwpp....cwpp.",
    "..cp..ccwppp..ccwpp.",
    ".ccpp.ccwppp..ccwpp.",
    ".cwpp.ccwppp..cwwpp.",
    ".cwpppccwpppcccwwpp.",
    "ccwpppccwpppccwwppp.",
    "ccwpppccwpppccwwppp.",
    "ccwpppccwpppccwwppp.",
    "pppppppppppppppppppp",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
], {'c': '#857BEF', 'p': '#675EC1', 'w': '#8EDCF2'})

# ---- Gate between the areas ------------------------------------------------------------------------------------

ART['gate'] = framed([
    "....................",
    ".......tttttt.......",
    ".....ttssssstt......",
    "....tssttttssst.....",
    "...tsstppppptsst....",
    "...tstpppwwpptst....",
    "..tsstppwwppptsst...",
    "..tstppwwppllptst...",
    "..tstpppppllpptst...",
    "..tstppllppppptst...",
    "..tstpllppwwpptst...",
    "..tstppppwwppptst...",
    "..tstpwwppppllptst..",
    "..tstppwwppllpptst..",
    "..tstppppppppptst...",
    "..tstppppppppptst...",
    ".ttttttttttttttttt..",
    ".sssssssssssssssss..",
    "....................",
    "....................",
], {'t': '#747D91', 's': '#94A0BA', 'p': '#675EC1', 'w': '#857BEF', 'l': '#887BFE'})

# ---- Characters (all outlined) ---------------------------------------------------------------------------------

ART['ember_wraith'] = framed([
    "....................",
    ".........f..........",
    "........fyf..f......",
    ".......fyyf.fyf.....",
    "......kkkkkkkkf.....",
    ".....khhhhhhhhk.....",
    ".....khkkkkkkhk.....",
    ".....khkwkkwkhk.....",
    ".....khkkkkkkhk.....",
    ".....khhkkkkhhk.....",
    "....khhhhyyhhhhk....",
    "...khhhhyffyhhhhk...",
    "...khhhhhyyhhhhhk...",
    "...kkhhhhhhhhhhkk...",
    "....khhhhhhhhhhk....",
    "....khhhhhhhhhhk....",
    "....kfhhfhhhfhhk....",
    ".....f.yf.fy.f......",
    "....................",
    "....................",
], {'k': '#303030', 'h': '#3C3C3C', 'f': '#DD0B00', 'y': '#FC9F00', 'w': '#FFE500'})

ART['ash_golem'] = framed([
    "....................",
    "....................",
    "......dddddddd......",
    ".....dggggggggd.....",
    ".....dgyglglygd.....",
    ".....dggggggggd.....",
    "...ddddgglggdddd....",
    "..dgglgggyggglggd...",
    "..dggggyggggggggd...",
    "..dgygdgglggdgygd...",
    "..dgggdggggygdggd...",
    "..ddddggglgggdddd...",
    "......dggydggd......",
    ".....dggldggld......",
    ".....dgygddgygd.....",
    ".....dgggddgggd.....",
    "....ddgggddgggdd....",
    "....dddddddddddd....",
    "....................",
    "....................",
], {'d': '#3C3C3C', 'g': '#606878', 'l': '#747D91', 'y': '#FC9F00'})

ART['dune_scorpion'] = framed([
    "....................",
    "..........ss........",
    ".........sabd.......",
    ".........dd.ad......",
    ".............ad.....",
    ".............ad.....",
    "............aad.....",
    "............ad......",
    "..dd.......aad..dd..",
    ".daad.....aaad.daad.",
    ".daaddd.ddaaadddaad.",
    "..ddaadaaaaaaadad...",
    "....daaadaaadaaad...",
    "...dadaaaaaaaaadad..",
    "..dad.daaaaaaad.dad.",
    "..d..dad.dad.dad..d.",
    ".....d...d.d...d....",
    "....................",
    "....................",
    "....................",
], {'a': '#D0B068', 'b': '#C8A020', 'd': '#84704F', 's': '#CC0D00'})

ART['desert_nomad'] = framed([
    "....................",
    "......wwwwwww.......",
    ".....wwwwwwwww......",
    "....wwbbbbbbbww.....",
    "....wbsssssssbw.....",
    "....wbsesssesbw.....",
    "....wbsssssssbw.....",
    "....wrrrrrrrrrw.....",
    "....wrrrrrrrrrw.....",
    "...dbbbbbbbbbbbd....",
    "..dbbbbbbbbbbbbbd...",
    "..dbbdbbbbbbbdbbd...",
    "..dsbdbbbbbbbdbsd...",
    "....dbbbbbbbbbd.....",
    "....dbbbdbbbbbd.....",
    "....dbbbdbbbbbd.....",
    "....dbbbdbbbbbd.....",
    "....ddddddddddd.....",
    "....................",
    "....................",
], {'w': '#EFE7A5', 'b': '#B0A078', 'd': '#A08860', 's': '#F6C5A7', 'e': '#3C2B26', 'r': '#CC0D00'})

ART['storm_harpy'] = framed([
    "....................",
    "......hhhhhh........",
    ".....hhhhhhhh.......",
    ".....hssssssh.......",
    ".....sesssess.......",
    ".....ssssssss.......",
    ".nn...ssssss....nn..",
    "ngnn..bbbbbb...nngn.",
    "nggnnbbbbbbbbnnnggn.",
    "ngggnbbbbbbbbnggggn.",
    ".nggggbbbbbbggggnn..",
    "..nnggbbbbbbggnn....",
    "....nnbbbbbbnn......",
    ".....nbbbbbbn.......",
    "......bbbbbb........",
    "......yy..yy........",
    ".....yyy..yyy.......",
    "....................",
    "....................",
    "....................",
], {'h': '#3C2B26', 's': '#FCC9AB', 'e': '#232631', 'g': '#94A0BA', 'n': '#656D8E', 'b': '#545B75', 'y': '#FFE500'})

ART['cloud_knight'] = framed([
    "........rr..........",
    ".......rrr..........",
    "......gggggg........",
    ".....gwwwwwwg.......",
    ".....gwwwwwwg.......",
    ".....gwvvvvwg.......",
    ".....gwwwwwwg.......",
    "....gggggggggg......",
    "...gwwwwwwwwwwg.....",
    "..gwwlwwyywwlwwg....",
    "..gwwlwwyywwlwwg....",
    "..gwwlwwwwwwlwwg....",
    "..gggglwwwwlgggg....",
    ".....gwwwwwwg.......",
    ".....gwwggwwg.......",
    ".....gwwggwwg.......",
    ".....gwwggwwg.......",
    "....gggggggggg......",
    "....................",
    "....................",
], {'g': '#B8C0A8', 'w': '#F0F0F4', 'l': '#D8DCE4', 'v': '#232631', 'r': '#CC0D00', 'y': '#FFE500'})

ART['crystal_sentinel'] = framed([
    "....................",
    ".........c..........",
    "........ccp.........",
    ".......ccwpp........",
    "......ccwwppp.......",
    "......ccwpppp.......",
    ".......ccppp........",
    "...cc..ccppp..cc....",
    "..ccpp.ccppp.ccpp...",
    "..ccppcccpppcccpp...",
    "..ccpccpwppppcpp....",
    "...cc.ccpwpppp.cc...",
    "......ccppppp.......",
    ".......ccppp........",
    "......ccp..ppp......",
    "......ccp..ppp......",
    ".....cccp..pppp.....",
    ".....pppp..pppp.....",
    "....................",
    "....................",
], {'c': '#857BEF', 'p': '#675EC1', 'w': '#8EDCF2'})

ART['prism_warden'] = framed([
    ".........r..........",
    "........rrr.........",
    ".......ryyyg........",
    "......ryyyggb.......",
    ".......gggbb........",
    "........cpp.........",
    "....ccccccpppppp....",
    "...ccccccppppppppp..",
    "..ccwcccppppppppwpp.",
    "..cccccppwpppppppp..",
    "..ccccppwwwpppppppp.",
    "..cccppppwwwpppppp..",
    "....ccppppwpppppp...",
    "....ccpppppppppp....",
    "....ccppppppppppp...",
    "...cccppp...ppppp...",
    "...cccpp.....pppp...",
    "...pppp......pppp...",
    "....................",
    "....................",
], {'c': '#857BEF', 'p': '#675EC1', 'w': '#FFFFFF', 'r': '#CC0D00', 'y': '#FFE500', 'g': '#21FF2C', 'b': '#3888DD'})

# ---- Goggles (trinket, outlined like the bandage): a strap, two lenses and a little lamp between them --------------

ART['goggles'] = framed([
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
    ".........yy.........",
    "....fffff..fffff....",
    "...fllllf..fllllf...",
    "ssfllwlllffllwlllfss",
    "ssflllllfyyfllllfss.".replace('fss.', 'fsss'),
    "..flllllf..flllllf..",
    "...fllllf..fllllf...",
    "....fffff..fffff....",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
], {'f': '#747D91', 'l': '#79BCCE', 'w': '#A0D0E0', 's': '#3C2B26', 'y': '#FFE500'})

# ---- Portal: a fluid (sliding) texture of 2x2 cells, 40x40 art that repeats seamlessly --------------------------

PORTAL_CELLS = {'portal_tl': (1000, 1800), 'portal_tr': (1100, 1800), 'portal_bl': (1000, 1900), 'portal_br': (1100, 1900)}

def portal():
    import math
    rows = []
    for y in range(40):
        row = ''
        for x in range(40):
            v = math.sin(2*math.pi*(x + y)/20) + math.sin(2*math.pi*(x - 2*y)/20) + 0.6*math.sin(2*math.pi*y/10)
            row += 'd' if v < -1.0 else ('p' if v < 0.2 else ('c' if v < 1.3 else 'l'))
        rows.append(row)
    return rows
PORTAL_PALETTE = {'d': '#5E4C5E', 'p': '#675EC1', 'c': '#857BEF', 'l': '#887BFE'}
_big = portal()
ART['portal_tl'] = ([r[:20] for r in _big[:20]], PORTAL_PALETTE)
ART['portal_tr'] = ([r[20:] for r in _big[:20]], PORTAL_PALETTE)
ART['portal_bl'] = ([r[:20] for r in _big[20:]], PORTAL_PALETTE)
ART['portal_br'] = ([r[20:] for r in _big[20:]], PORTAL_PALETTE)
CELLS.update(PORTAL_CELLS)


# Cave floors are the game's cave floor (tile 25, cell (600, 0)) recoloured, not new textures: the brown and its two
# lighter specks become greys for the volcano's ash and dark blue-purples for the crystal caverns
CAVE_FLOOR = (600, 0)
RECOLORED = {
    'ash': {'#8C6E56': '#515151', '#9B8063': '#5E5E5E', '#B99B65': '#706C6C'},
    'crystal_floor': {'#8C6E56': '#464C61', '#9B8063': '#545B75', '#B99B65': '#656D8E'},
}


# Taller pillars: the owner's pillar top (1000, 300) is the capital and the shaft, with a flare at the bottom (rows 18-19)
# that meets the base. A pillar more than one tile high is the top, then middles (the shaft, its row 6 repeated), then
# a bottom (the shaft and the top's flare), then the base. Made from the owner's own cell, not drawn
PILLAR_TOP = (1000, 300)
PILLAR_CELLS = {'pillar_middle': (1200, 1900), 'pillar_bottom': (1300, 1900)}
PILLAR_COLORS = ['#404868', '#D0D0C0', '#E0D8D8', '#94A0BA', '#B8C0A8']


def pillar_parts(sheet):
    rows, palette = sheet.recolor(PILLAR_TOP[0], PILLAR_TOP[1], {c: c for c in PILLAR_COLORS})
    shaft = rows[6]
    return {'pillar_middle': ([shaft]*20, palette), 'pillar_bottom': ([shaft]*18 + rows[18:20], palette)}


if __name__ == '__main__':
    redraw = '--redraw' in sys.argv
    #--only a,b: just these cells (your own drafts), drawn again even when drawn in
    only = None
    if '--only' in sys.argv:
        only = set(sys.argv[sys.argv.index('--only') + 1].split(','))
    sheet = Sheet()
    sheet.grow(2000)
    for name, mapping in RECOLORED.items():
        ART[name] = sheet.recolor(CAVE_FLOOR[0], CAVE_FLOOR[1], mapping)
    ART.update(pillar_parts(sheet))
    CELLS.update(PILLAR_CELLS)
    drawn = 0
    for name, (x, y) in CELLS.items():
        if only is not None:
            if name not in only:
                continue
            rows, palette = ART[name]
            sheet.draw(x, y, rows, palette, replace=True)
            drawn += 1
            continue
        #A cell that is drawn in already (the owner's redrawn texture) is left alone, unless asked to redraw the drafts
        if not redraw and not sheet.is_empty(x, y):
            print('Skipped %s at (%d, %d): already drawn' % (name, x, y))
            continue
        rows, palette = ART[name]
        sheet.draw(x, y, rows, palette, replace=True)
        drawn += 1
    sheet.save()
    print('Drew %d cells' % drawn)
