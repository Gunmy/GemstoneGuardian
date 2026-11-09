# GemstoneGuardian
This is a relatively large project I took on for a class I am taking at NTNU, and is called Gemstone Guardian (GG). GG is heavily inspired by role-playing-games (RPGs) such as pokémon, especially the early ones like Pokémon Red and Blue. 

You play as a boy that uncovers one of his grandmas greatest secrets: that she holds an elemental gemstone: a gemstone with special powers. Using said gemstone you fight all kinds of people, whomst also carry such stones, to become the very greatest carrier. 

# How to play
To start the game you run the app.java file.
You use WASD to move around, and space/mouseclick to battle and talk to people.
![image](https://github.com/Gunmy/GemstoneGuardian/assets/99408493/a6eda814-98ec-4c6f-abe9-8c34137b1c1d)|
|:--:|
|*Figure 1: Thumbnail*|

|![image](https://github.com/Gunmy/GemstoneGuardian/assets/99408493/49c7f398-1174-49ca-b0ac-d5d869f0f8bf)|
|:--:|
|*Figure 2: Starting*|

|![image](https://github.com/Gunmy/GemstoneGuardian/assets/99408493/1db233f5-e55f-4be8-bddc-e207243d981d)|
|:--:|
|*Figure 3: Shader-renderer renders the fluids*|

|![image](https://github.com/Gunmy/GemstoneGuardian/assets/99408493/6e28cb09-7424-484d-81a6-31cae9329d47)|
|:--:|
|*Figure 4: Your very fist battle*|

| Key / input | Does |
|---|---|
| W A S D | Walk |
| Space or click | Continue dialogue, talk, start battles |
| E | Open and close the inventory |
| Escape | Settings menu (or closes whatever is open) |
| Scroll wheel | Zoom (scrolls the inventory when it is open) |
| C | Phase through everything (also in Settings → Hidden secrets) |
| M | Show chunk borders, coordinates and FPS (also in Hidden secrets) |
| N | Reload `chunks.txt` while M is on |

# For developers and AI agents

Everything below is what you need to work on the code. Read it before changing anything.

## Building and running

JavaFX 19 app, Java 17, one module (`GemstoneGuardian`, see `module-info.java`), one package (`game`). `pom.xml` exists but Maven is not installed on the development machine; the JavaFX jars are in the local Maven cache (`~/.m2/repository/org/openjfx/javafx-*/19/javafx-*-19-win.jar`). Build with `javac` straight into `target/classes` (git-ignored) and copy `src/main/resources` on top:

```powershell
$fx = "$env:USERPROFILE\.m2\repository\org\openjfx"
$mp = (@("base","controls","fxml","graphics") | % { "$fx\javafx-$_\19\javafx-$_-19-win.jar" }) -join ";"
javac --release 17 -encoding UTF-8 --module-path $mp -d target\classes (Get-ChildItem src\main\java -Recurse -Filter *.java).FullName
Copy-Item src\main\resources\* target\classes -Recurse -Force
java --module-path "$mp;target\classes" -m GemstoneGuardian/game.app
```

Run from the project root: the map, highscore and settings are read from `src/main/resources/game/reading/` by relative path, not from the classpath. The game has no save files; closing it loses progress. A running game must be closed before `target/classes` is rebuilt.

## How a frame works

`app` loads `Game.fxml`, whose controller (`controller`) owns a `Timeline` that ticks every 20 ms and calls `gameHandler.next(deltaTime)`. Everything is drawn onto an off-screen `tempCanvas`, which is snapshotted onto the visible canvas at the end of the frame (`RCanvas` is just a resizable canvas). The draw order in `gameHandler.next` is:

1. `chunks.drawGround()` — z-level 0, the ground of every tile on screen.
2. "behind" particles.
3. `chunks.drawLayer(z, characters)` for z = 1 … max — trees, flowers, characters, anything above the ground, one level at a time. Each level is drawn row by row from the top of the screen, with characters slotted in between the rows by their feet (`character.getFeetY()`), so whatever is further down is in front. Characters have a `z` too (`setZ`), 1 by default; 2 and up is above the trees (for birds and such).
4. "front" particles, then `gui.draw()` on top of everything.

`camera` follows the main character with easing. `gameHandler.getUnitWidth()/getUnitHeight()` is the size of one chunk in pixels and already includes the zoom; a tile is that divided by `getTileCount()` (9). Positions in the world are in chunks (`10000.3` = chunk 10000, tile 2.7), which is why coordinates are around 10000.

## The world

- `chunks` is one world (outside, underground, house floors — see the `worlds` list in `gameHandler`). It holds `chunk`s in a hashtable by position, generating default ones (all the world's `standardTile`) when asked for a missing one. Its `renderDistance` is how far from the camera tiles are drawn; below 1 the world is "dark" and the rest fades to the background colour.
- `chunk` is 9x9 `tile`s. `tile` knows its `tileType`, spawns ambient particles in `drawGround` (the big `switch` on tile id), and answers `isBlocked(xChange, yChange)` / `isBlockedOut(...)` for movement.
- `tileTypes` is the list of all tile types; the index in the list is the id used in `chunks.txt`, so **never reorder or remove lines, only append** (the `//N` comments give the id). Types:
  - `tileType` — one cell of the sprite sheet, optionally blocked, optionally with a `pressurePlate` lambda run when a character steps on it (doors, stairs, healing pads).
  - `waveType` — an overlay drawn over an underlay type. Options: `wave` (bobs up and down, which also lifts it to z = 1), `z`, `tall` (how many tiles high, grows upwards), `scatter` (random but fixed offset per tile so forests are not a grid). Use `addWaveType`; `addTreeType` is the preset for trees (`treeHeight`, `treeScatter`).
  - `fluidType` — water, lava, toxic, crystal. Drawn by `shader` (per-pixel noise, expensive) or, when the player picks "Fluids: Simple" in settings, by sliding a tile-sized window over the texture's 2x2 cell region (cheap).
  - `solidFrom(left, top, right, bottom)` / `solidTo(...)` right after a type make it one-way: `1` closes that side for entering / leaving. Types without them use the plain blocked flag with no extra checks. Signs use both so you can stand behind them.
- `readChunks` parses `chunks.txt`: a header line `world, x, y, name` (several headers may share one block), then 9 lines of 9 tile ids. Blank lines separate blocks. `N` reloads it in game while borders are shown.

## Characters, items, battle

- `character` has a position, a target it walks towards one tile at a time (`setTarget`, `addMovementToQueue`), a world, health, xp (`level = cbrt(xp)`), effects (fire, poison, healing, confusion), an `inventory` and an equipped `gemstone`. All characters, including NPCs, are created in the `gameHandler` constructor, along with every `event` and `eventListener` (a circle in the world that fires an event and/or dialogue when the main character enters it). `movementEvent` directions are 0 left, 1 up, 2 right, 3 down.
- `inventory` is a list of `itemStack`s: a top part of 8 columns that grows by a row when full (never fewer than `MIN_ROWS`), plus the active part — 4 rock slots (`gemstone`s only) and 4 item slots (anything else). `inventory.give(item)` puts things where they belong and stacks stackables.
- `item` is the base: name, `use` lambda, consumable flag, `look(cellX, cellY, tint)`, `particles(itemParticles)`, `stackable(max)`. `gemstone` (three abilities with uses and hit chance) and `potion` extend it. The catalogues are `gemstones` and `items`; add new ones there. All rocks share one grey-scale texture tinted per gemstone (`imgDict.getTinted`), potions likewise.
- `battle` holds the fight state only (whose turn, waiting for the player, "more" view) and is drawn by `guiBattle`. Switching rock or using an item costs the turn. NPCs fight with `character.autoMaticMove`.
- `dialogueContainer` is the queue of `dialogue`s; the current one is drawn by `guiDialogue`. A dialogue with an item (`new dialogue(name, text, item)`) gets a gold border and the item drawn in it.

## The GUI

Everything on screen is drawn on the canvas by the `gui*` classes; there are no JavaFX controls left. `gui` owns the elements, forwards mouse events (controller → `gui.click/hover/scroll`, falling through to the game when nothing used them) and draws them in order: `guiHud` (active row at the bottom), `guiBattle`, `guiInventory`, `guiDialogue`, `guiSettings`. `guiElement` is the base (bounds, `layout(screenWidth, screenHeight)` every frame, `draw`, `click`, `scroll`, `hover`); `guiPanel` groups children, `guiButton` is a 9-sliced button with optional custom face, `slotGrid` draws any block of inventory slots.

Sizes in the gui classes are written for a 1000 px wide window at "Medium" and multiplied by `gui.s()` (`settings.referenceWidth`, `guiSize.factor`), so the GUI is a share of the screen width. `guiSkin` knows where the GUI textures are and 9-slices panels and buttons (corner and edges are drawn at `border` px and flipped for the other sides). `guiText` draws text in the bundled Silkscreen font (`media/Silkscreen-Regular.ttf`, OFL licence next to it). `guiParticle`/`itemParticles` are the particle effects items give off in slots.

`settings` (GUI size, fluid mode) is saved to `reading/settings.txt` on change. The debug switches (colliders, borders) live in `gameHandler` and are not saved.

## The sprite sheet

`media/tiles.png` is 1500x1600 in 100x100 cells; code addresses cells by pixel position (`x, y` of the top-left corner). Cells are drawn with a one pixel inset so neighbours do not bleed. Row y = 1500 is the GUI row: slot (0), selected slot (100), panel corner/top edge/left edge/middle (200–500), button corner/top edge/left edge/middle (600–900), rock (1000), potion (1100). Items are drawn grey-scale and tinted in code: brightness is kept, hue is replaced (`imgDict.getTinted`); `getSilhouette` makes the white "empty slot" hints.

**The textures are drawn by the project owner.** When something needs a new texture, pick a free cell, draw a labelled draft there and say which cell it is; do not redraw cells the owner has finished (the rock at (1000, 1500) is finished). `media/noise_texture.png` is the shader's noise.

## Conventions and working agreements

- Class names are lower case (`gameHandler`, `tileType`), exception messages are Norwegian, comments are English. Files use CRLF. Validate arguments with `IllegalArgumentException` like the existing code does.
- Build systems, not one-offs: assume more items, rock types, tile kinds and screens will come, and put behaviour behind settings and small helpers (the way `addWaveType`, `solidFrom`, `look(...)` do).
- Before building a large system, ask the owner the design questions first.
- Do not take screenshots to check visuals; build, start the game and ask the owner to look.
- Work on a branch, make small commits with a short imperative title and a body that says what changed and why. Verify each commit compiles.
