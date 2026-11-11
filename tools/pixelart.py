"""Draws textures into media/tiles.png the way the game's art is made: every cell is 20x20 pixels of art, scaled up
5 times with nearest neighbour into its 100x100 cell, so one art pixel is a 5x5 block.

Art is a list of 20 strings of 20 characters, one character per pixel, with a palette that maps characters to
colours ("#RRGGBB"). '.' is always see-through. Floors and walls fill every pixel; overlays (objects lying on a tile)
and characters leave the rest see-through so the tile under them shows.

The rules the owner's art follows, which new art has to follow too:
- Only colours that are already in the art (tools/palette.txt). draw() refuses any other colour.
- Characters, and objects lying on a tile (chests, signs, pillars...), have a one pixel outline in OUTLINE (#404868)
  all round: draw the shape without it and pass it through outline(). Plants (trees, flowers, cactus) have none.
- No lone single pixels as texture on tiles: specks are at least 2x2 (noise() makes them so).

Use from a script (see tools/art_postgame.py):

    from pixelart import Sheet, outline, noise
    sheet = Sheet()                      # opens src/main/resources/game/media/tiles.png
    sheet.grow(1900)                     # makes the sheet taller (never shorter), new space see-through
    sheet.draw(0, 1700, ART, PALETTE)    # cell with its top left at (0, 1700); refuses a cell already drawn in
    sheet.save()

Never draw over a finished cell: draw() refuses cells that are not empty unless replace=True, which is only for
redrawing your own drafts.
"""
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SHEET = os.path.join(HERE, '..', 'src', 'main', 'resources', 'game', 'media', 'tiles.png')
PALETTE_FILE = os.path.join(HERE, 'palette.txt')
ART = 20
CELL = 100
SCALE = CELL // ART
OUTLINE = '#404868'


def color(hex_color):
    h = hex_color.lstrip('#')
    if len(h) == 6:
        h += 'FF'
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4, 6))


def allowed_colors():
    """The colours of the owner's art, from palette.txt"""
    out = set()
    with open(PALETTE_FILE) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('# '):
                out.add(line.upper())
    return out


def to_image(art, palette):
    """The 20x20 art as an image, then scaled up 5x with nearest neighbour."""
    if len(art) != ART or any(len(row) != ART for row in art):
        raise ValueError('Art must be 20 rows of 20 characters')
    small = Image.new('RGBA', (ART, ART), (0, 0, 0, 0))
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            if ch not in palette:
                raise ValueError('No colour for %r' % ch)
            small.putpixel((x, y), color(palette[ch]))
    return small.resize((CELL, CELL), Image.NEAREST)


def outline(art, ch='o'):
    """The art with a one pixel outline (the character ch, give it OUTLINE in the palette) round everything drawn:
    every see-through pixel next to a drawn one, sideways or up and down"""
    rows = [list(r) for r in art]
    out = [list(r) for r in art]
    for y in range(ART):
        for x in range(ART):
            if rows[y][x] != '.':
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < ART and 0 <= ny < ART and rows[ny][nx] != '.':
                    out[y][x] = ch
                    break
    return [''.join(r) for r in out]


def noise(base, specks, seed, size=ART, block=2):
    """Floor-like art: the base character everywhere, with specks of each character (char, chance) sprinkled over it
    as block x block squares (never single pixels). The tile repeats, so specks wrap round the edges"""
    rnd = random.Random(seed)
    rows = [[base] * size for _ in range(size)]
    for y in range(0, size, block):
        for x in range(0, size, block):
            for speck, chance in specks:
                if rnd.random() < chance:
                    ox, oy = rnd.randrange(block), rnd.randrange(block)
                    for dy in range(block):
                        for dx in range(block):
                            rows[(y + oy + dy) % size][(x + ox + dx) % size] = speck
    return [''.join(r) for r in rows]


class Sheet:
    def __init__(self, path=SHEET):
        self.path = path
        self.image = Image.open(path).convert('RGBA')
        self.allowed = allowed_colors()

    def grow(self, height):
        """Makes the sheet at least this tall. New space is see-through."""
        if height <= self.image.height:
            return
        bigger = Image.new('RGBA', (self.image.width, height), (0, 0, 0, 0))
        bigger.paste(self.image, (0, 0))
        self.image = bigger

    def recolor(self, x, y, mapping):
        """The 20x20 art of the cell at (x, y) with its colours swapped by mapping ({'#old': '#new'}), as (rows, palette)
        for draw(): a recoloured copy of an existing texture (every colour in it has to be in the mapping)"""
        letters, rows = {}, []
        for py in range(ART):
            row = ''
            for px in range(ART):
                r, g, b, a = self.image.getpixel((x + px*SCALE + SCALE//2, y + py*SCALE + SCALE//2))
                if a == 0:
                    row += '.'
                    continue
                hexc = '#%02X%02X%02X' % (r, g, b)
                if hexc not in mapping:
                    raise ValueError('No new colour for %s in (%d, %d)' % (hexc, x, y))
                if hexc not in letters:
                    letters[hexc] = 'abcdefghijklmnopqrstuvwxyz'[len(letters)]
                row += letters[hexc]
            rows.append(row)
        return rows, {letter: mapping[hexc] for hexc, letter in letters.items()}

    def is_empty(self, x, y):
        box = self.image.crop((x, y, x + CELL, y + CELL))
        return box.getchannel('A').getextrema()[1] == 0

    def draw(self, x, y, art, palette, replace=False):
        if x % CELL or y % CELL:
            raise ValueError('Cells start at multiples of 100')
        if x + CELL > self.image.width or y + CELL > self.image.height:
            raise ValueError('Cell (%d, %d) is outside the sheet, grow it first' % (x, y))
        if not replace and not self.is_empty(x, y):
            raise ValueError('Cell (%d, %d) is already drawn in' % (x, y))
        used = {ch for row in art for ch in row if ch != '.'}
        for ch in used:
            if palette[ch].upper() not in self.allowed:
                raise ValueError('%s is not a colour of the art (palette.txt), at (%d, %d)' % (palette[ch], x, y))
        cell = to_image(art, palette)
        self.image.paste((0, 0, 0, 0), (x, y, x + CELL, y + CELL))
        self.image.alpha_composite(cell, (x, y))

    def save(self):
        self.image.save(self.path)
