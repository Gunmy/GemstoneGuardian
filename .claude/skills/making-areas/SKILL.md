---
name: making-areas
description: How to add a new area to GemstoneGuardian - map chunks, worlds, gates, new tile types, 20x20 textures in tiles.png, characters, fights and chests. Use whenever making or extending a map area, drawing new tiles or character sprites, or connecting areas.
---

# Making areas in GemstoneGuardian

Read the README's developer section first. This is the workflow the post-game areas (volcano depths, desert, sky ruins,
crystal caverns, after the final boss) were made with; follow it so new areas look and work the same.

## 1. Plan

- Ask the owner the design questions first (theme, size, who is in it, rewards). Big systems are agreed before building.
- An area is a set of 9x9 tile **chunks**. Chunk (x, y) covers world coordinates x..x+1, y..y+1; a tile is 1/9.
  North is smaller y. Row 0 of a chunk is its top; going north out of row 0 enters row 8 of chunk y-1.
- Put areas in the worlds that are there: outside (world 0) or the caves (world 1). A new world only for a really
  different kind of place, and only when the owner asks (the sky ruins, world 4, are one: a sky world with
  `chunks.sky()` and `skyLayer`).
- A door must never lead onto a door. Doors only change the world, keeping the place, so the tiles you arrive on must
  be floor in the other world, and the way back is a door next to them, on the tiles that are floor where you came
  from (the approach). The area in the other world then goes on away from the way back. tools/maps_postgame.py checks
  every door tile in both directions (`check_doors`); do the same for new doors.
- Caves are walled by the black void tile (17), like every cave in the game; do not draw cave walls. Outside, the
  edge of an area is whatever fits (dunes, sky, forest).
- Make areas open, not corridors: a region of several chunks (the post-game areas are 3x3 chunks, drawn as one 27x27
  grid) with more than one way through, side pockets, optional fights and something to find off the main way.
- Between outside and the caves use the cave passage tiles: 40 (outside, down) and 43 (caves, up), next to each other
  so neither leads onto the other.
- A 3x3 portal (`portalType`, 172/173) is for something special (the way out of the final boss's room): stepping on
  any of its tiles lands on the same tile in the other world, so it needs a 3x3 landing spot with the portal back
  next to it. Set portals in walls, not in the middle of a floor, so nobody walks into them by accident.

## 2. Textures (tools/pixelart.py)

- Every texture is **20x20 pixels of art**, scaled 5x with nearest neighbour into its 100x100 cell. Draw with
  `pixelart.Sheet` from a script (see `tools/art_postgame.py`); it grows the sheet when needed (new rows at the
  bottom, the sheet is 1500 wide) and refuses cells already drawn in.
- **Only colours already in the art** (`tools/palette.txt`); `draw()` refuses others. Pick the matching existing set:
  tree greens for plants, the sand colours for sand, lava oranges for heat, the greys for stone, and so on. If a colour
  really does not exist, do without it.
- **Outline**: characters, and objects lying on a tile (chests, signs, pillars, gates, crystals), have a one pixel
  `#404868` outline all round: draw the shape without it and wrap it in `outline()` (see `framed()` in
  art_postgame.py). Plants (trees, flowers, cactus) have none.
- **No single-pixel specks** on tiles: specks are at least 2x2 (`noise()` makes them so). Patterns and lines are fine.
- Floors and walls fill all 400 pixels and tile seamlessly, and stay calm: a base colour with only a few specks of a
  near shade (the owner found busy floors too noisy). Overlays leave the rest see-through; the tile under them is the
  underlay given in `tileTypes`.
- Sliding textures (water, lava, portals) are fluid types: a seamless 2x2 cell region (40x40 art), see the portal in
  art_postgame.py.
- Use what is there before drawing new: the owner's pillar (top (1000, 300) over base (1000, 400), as new tile types
  with the underlay you need), chests, the sand, the healing pad. Only draw what does not exist yet.
- Pillars more than one tile high: the top, then middles, then a bottom, then the base; never a top on a top. The
  middle and bottom are made from the top's own shaft (`pillar_parts` in art_postgame.py; 188/189 on bluebrick); make
  the same pair for another underlay when you need one.
- A variant of an existing texture is a recolour of it, not a new drawing: `Sheet.recolor(x, y, {'#old': '#new'})`
  gives its art with the colours swapped (the post-game cave floors are the cave floor, tile 25, in greys and purples).
- Never draw over the owner's cells. Drafts go in empty cells, and the script skips cells that are already drawn.
  `--redraw` overwrites, so only use it while every cell in that script is still your own draft (check against
  `git show HEAD:src/main/resources/game/media/tiles.png` first). Tell the owner which cells are drafts.
- Do not screenshot to check: start the game and ask the owner to look.

## 3. Tile types (tileTypes.java)

- Tile ids are list positions: **only append**, never reorder. Note the id in a `//147 name` comment.
- `addType(name, x, y, 100, 100, blocked, pressurePlate)` for a plain cell. `addWaveType(..., underlay, wave)` for an
  overlay on another type. `wave` true makes it bob (and lifts it to z = 1): only for things that would move, never for
  pillars, spikes, rocks or crystals. Solid standing things use `addWaveType(..., underlay, false, 1, 1, 0)` (no bob,
  z = 1, drawn among the characters), like the sign. `addTreeType` for trees.
- `container()` after a type makes every tile of it a chest; `teleportTarget()` lists it in the teleports menu.
- An overlay's underlay must be added before it. A gate per direction needs its own type (its own pressure plate),
  all sharing one cell: see `gate(world)` and `sealedGate(world)`.
- Ambient particles of a tile go in the `switch` in `tile.drawGround`.

## 4. Maps (tools/maps_postgame.py)

- Write the map as symbols, 9 rows of 9 per chunk, with a legend per area (symbol -> tile id) and digits where
  characters stand. The script flood-fills from the arrival tile to check the way reaches the exit and every
  character, prints the characters' places, and with `--write` writes the blocks into `chunks.txt`.
- `tools/maps_postgame.py` is the example: areas as 27x27 symbol grids, cut into chunks, checked (flood fill from the
  arrival to the exit, every character, chest and healing pad reachable, doors lined up, pillar tops with a base under
  them, no chunk of the game's own overwritten) and written as blocks named "PG ...".
- `chunks.txt` format: a header line `world, x, y, name` (several headers can share one block of 9 rows), then 9
  lines of 9 ids, blocks split by blank lines. No comments. A chunk listed twice: the later block wins, so check with
  `grep -n "^W, X, Y" chunks.txt` before editing one, and split a shared block when changing only one of its chunks.
- Use a 1-tile-wide way where a guard stands, so the player can not walk round the fight (the fight starts within
  0.15, about 1.35 tiles).

## 5. Characters, fights, rewards (gameHandler constructor)

- `new character(name, x, y, imgX, imgY, 100, 100, world, this, speed, level)`; x = chunk + column/9,
  y = chunk + row/9. Sprite cells for new characters are drafts (20x20, outlined).
- Gems with `give(gemstones.x())` (existing gems only unless asked), trinkets with `wear(items.x())` after `itemDict`.
- A guard: `guardFight(character, dialogue)` (fights, steps up and left when beaten). XP fights: a `battleEvent` with
  no win event. Add every `eventListener` before `nameCampaignListeners()` (savefiles name them).
- Chests: the container tile in the map, then `chestAt(world, x, y).put(row, column, item)`. Every chest heals on its
  first close.
- A healing pad (tile 91) in each area is its checkpoint.

## 6. Finish

- Build (README), start the game, and ask the owner to walk it. Update `BALANCING.md` (who is where, what they use,
  what you have by then, an empty "should be level" line) and the README.
