"""Checks the post-game characters in gameHandler.java against the maps (tools/maps_postgame.py) and BALANCING.md:
world, tile, level, gems, trinket, their fight, that guards step aside onto floor, and that chests are on chest tiles.

    python tools/check_postgame_characters.py .
"""
import re
import sys
import importlib.util

root = sys.argv[1]
spec = importlib.util.spec_from_file_location('maps', root + '/tools/maps_postgame.py')
maps = importlib.util.module_from_spec(spec)
spec.loader.exec_module(maps)
java = open(root + '/src/main/java/game/gameHandler.java', encoding='utf-8').read()

DIGITS = {'1': 'Ember Wraith', '2': 'Ash Golem', '3': 'Dune Scorpion', '4': 'Desert Nomad', '5': 'Storm Harpy',
          '6': 'Cloud Knight', '7': 'Crystal Sentinel', '8': 'Prism Warden', '9': 'Crystal Boy'}
# From BALANCING.md: level, gems, what they wear, guard or XP fight
EXPECT = {
    'Ember Wraith': (17, ['magma', 'obsidian'], None, 'xp'),
    'Ash Golem': (18, ['giantBolder', 'lava'], None, 'guard'),
    'Dune Scorpion': (18, ['sandstone', 'discombobulateShard'], None, 'xp'),
    'Desert Nomad': (19, ['saltstone', 'glass'], 'creatine', 'guard'),
    'Storm Harpy': (19, ['boltstone', 'copper'], None, 'xp'),
    'Cloud Knight': (20, ['meteorite', 'healite'], 'featherOfHermesBoots', 'guard'),
    'Crystal Sentinel': (20, ['uranium', 'ice'], None, 'xp'),
    'Prism Warden': (22, ['meteorite', 'boltstone', 'uranium', 'healite'], 'heartOfTheWarrior', 'guard'),
    'Crystal Boy': (24, ['ice', 'uranium', 'meteorite', 'healite'], None, 'boss'),
}
balancing = open(root + '/BALANCING.md', encoding='utf-8').read()


def fights(name, how):
    """Whether the character has a fight of this kind (guardFight or servantFight), by name or by the variable for it"""
    if '%s(characters.get("%s")' % (how, name) in java:
        return True
    var = re.search(r'(\w+) = characters\.get\("%s"\)' % name, java)
    return bool(var) and ('%s(%s,' % (how, var.group(1))) in java

tiles = maps.everything()
problems = []
places = {}
for area in maps.AREAS:
    for r, row in enumerate(area['grid']):
        for c, ch in enumerate(row):
            if ch.isdigit():
                places[DIGITS[ch]] = (area['world'], area['x'] + c // 9, area['y'] + r // 9, c % 9, r % 9, area['name'])

for name, (level, gems, worn, kind) in EXPECT.items():
    m = re.search(r'characters\.add\(new character\("%s", (\d+) \+ (\d+)\*pt, (\d+) \+ (\d+)\*pt, (\d+), (\d+), 100, 100, (\d+), this, 3, (\d+)\)\);' % name, java)
    if not m:
        problems.append('%s: no character line found' % name)
        continue
    x, cx, y, cy, imgx, imgy, world, lvl = map(int, m.groups())
    w, px, py, pc, pr, area = places[name]
    if (world, x, y, cx, cy) != (w, px, py, pc, pr):
        problems.append('%s: code has world %d (%d+%d/9, %d+%d/9), map has world %d (%d+%d/9, %d+%d/9)' % (name, world, x, cx, y, cy, w, px, pc, py, pr))
    if lvl != level:
        problems.append('%s: level %d in code, %d in BALANCING' % (name, lvl, level))
    if not re.search(r'\*\*%s\*\*[^\n]*level %d' % (name, level), balancing):
        problems.append('%s: BALANCING.md does not list level %d' % (name, level))
    given = re.findall(r'characters\.get\("%s"\)\.give\(gemstoneDict\.(\w+)\(\)\)' % name, java)
    if given != gems:
        problems.append('%s: gems %s in code, expected %s' % (name, given, gems))
    wears = re.findall(r'characters\.get\("%s"\)\.wear\(itemDict\.(\w+)\(\)\)' % name, java)
    if wears != ([worn] if worn else []):
        problems.append('%s: wears %s, expected %s' % (name, wears, worn))
    # Standing on floor
    if tiles[(w, px, py, pc, pr)] in maps.BLOCKED:
        problems.append('%s: stands on a blocked tile' % name)
    if kind == 'guard':
        if not fights(name, 'guardFight') and not fights(name, 'servantGuard'):
            problems.append('%s: no guard fight' % name)
        # Steps up then left out of the way: both must be floor
        g = [a for a in maps.AREAS if a['name'] == area][0]
        gr, gc = [(r, row.index(d)) for r, row in enumerate(g['grid']) for d in row if DIGITS.get(d) == name][0]
        for rr, cc in ((gr - 1, gc), (gr - 1, gc - 1)):
            if maps.tile_id(g, rr, cc) in maps.BLOCKED or maps.tile_id(g, rr, cc) in maps.DOORS:
                problems.append('%s: steps aside onto %s, which is not floor' % (name, (rr, cc)))
    elif kind == 'boss':
        if 'crystalBoyFight = new eventListener' not in java:
            problems.append('%s: no fight' % name)
    else:
        if not fights(name, 'servantFight'):
            problems.append('%s: not in the XP fights' % name)
    print('%-17s world %d  x %d+%d/9  y %d+%d/9  level %d  %-30s %s  (%s)' % (name, world, x, cx, y, cy, lvl, ', '.join(given), worn or '-', area))

# Chests on chest tiles
for m in re.finditer(r'chestAt\((\d), (\d+) \+ (\d)\.0/getTileCount\(\), (\d+) \+ (\d)\.0/getTileCount\(\)\)', java):
    world, x, c, y, r = map(int, m.groups())
    key = (world, x, y, c, r)
    if key in tiles:
        if tiles[key] not in (87, 162, 163):
            problems.append('Chest at %s is not on a chest tile (%s)' % (key, tiles[key]))
        else:
            print('Chest at world %d (%d+%d/9, %d+%d/9): chest tile %d' % (world, x, c, y, r, tiles[key]))

# Statue spots (where beaten servants stand) in the code are the 's' cells of the castle grounds, one each
spots = set()
for area in maps.AREAS:
    for r, row in enumerate(area['grid']):
        for c, ch in enumerate(row):
            if ch == 's':
                w, x, y, cc, rr = maps.absolute(area, r, c)
                spots.add((x, cc, y, rr))
coded = re.findall(r'case "([^"]+)":\s+return new double\[\] \{(\d+) \+ (\d)\*t, (\d+) \+ (\d)\*t\};', java)
coded += [('Prism Warden',) + m for m in re.findall(r'default:\s+return new double\[\] \{(\d+) \+ (\d)\*t, (\d+) \+ (\d)\*t\}; //Prism Warden', java)]
placed = {name: (int(x), int(c), int(y), int(r)) for name, x, c, y, r in coded}
for name in DIGITS.values():
    if name == 'Crystal Boy':
        continue
    if name not in placed:
        problems.append('%s: no statue spot' % name)
    elif placed[name] not in spots:
        problems.append('%s: statue spot %s is not an s in the castle grounds' % (name, placed[name]))
if len(set(placed.values())) != len(placed):
    problems.append('Two servants share a statue spot')
print('Statue spots: %d in the code, %d in the map' % (len(placed), len(spots)))

print()
print('\n'.join(problems) if problems else 'Everything matches')
