# Menus and screens: how they look and work

A guide to every screen in the GUI: what each one looks like, where its parts sit, how it opens and closes, and
what the mouse and keys do. Read this before changing or adding a screen, so new screens match the old ones and fit
on the screen. The README has the deeper systems (items acting on each other, tooltips, the sprite sheet). This file
is the layout and behaviour of each screen.

All numbers below are **before scaling**. Every size in the gui classes is multiplied by `gui.s()` (written `s` in
the code, e.g. `40*s`). The sizes are written for a 1000 px wide window at GUI size "Medium". On short, wide windows
the screen height decides instead: the tallest screen (`settings.fitHeight` = 620, the inventory with a chest above
it) must fit at the largest GUI size. **So the usable screen is about 1000 x 620 units at most, and less on a short
window.** Check any new screen against that height.

---

## 1. The framework (read first)

### Elements, order, clicks
- Everything is drawn on the canvas by `gui*` classes. There are no JavaFX controls.
- `gui` owns the elements and draws them in this order, so the last is on top and gets clicks first:
  `guiHud` → `guiBattle` → `guiInventory` → `guiOutcome` → `guiBattleIntro` → `guiDialogue` → `guiSettings` → `guiTeleports` →
  `guiMainMenu`.
- After the elements it draws: the menu overlay (a main menu sliding over a game that is being left), trails, the
  held item on the cursor, item glows (`itemInteractions.drawGlows`), GUI particles, lightning, outline pulses, the
  battle popups (`guiBattle.drawPopups`), then tooltips and debug text.
- `guiElement` is the base. It has the bounds (`x, y, width, height`), `layout(screenW, screenH)`, `draw`, `click`,
  `scroll`, `hover`, `keyPressed` and `keyTyped`.
  - **`layout` runs every frame**, so positions always follow the window size. Do layout there, never in the
    constructor.
- Clicks: `click` returns true when it used the click. A click nothing uses falls through to the world.
  - The modal menus (inventory, settings, teleports, main menu) take every click while visible, even outside
    their box.
  - A menu that is swapping pages takes no clicks until the old page is gone.
- Keys go to the top visible element that wants them (`keyPressed` / `keyTyped` return true). In the main menu
  everything but escape is swallowed.
- Hover runs every frame, not only when the mouse moves, so tooltips follow things that change under the mouse.
- Who is shown is decided in one place, `gui.updateVisibility()`. It runs every frame from the game's state:
  - the hotbar shows while walking and not in a fight, menu, inventory or end screen;
  - the battle screen shows while a fight is active, no end screen is up and its intro is over;
  - the battle intro shows while `battleIntro.playing()`;
  - the end screen shows while `handler.getOutcome() != null`;
  - the speech box shows while a message is up and no end screen is;
  - the main menu shows in the menu, except while the inventory or settings are open over it.

  When adding a screen, put its rule here rather than toggling visibility from scattered places.

### Building blocks
| Class | What it is |
|---|---|
| `guiSkin` | 9-sliced panels (`drawPanel`) and buttons (`drawButton`, optionally tinted with a hex colour), slots (`drawSlot`, `selected` gives the framed slot) and white empty-slot hints (`drawSlotHint`). `getBorder()` = 20*s, the frame thickness. |
| `guiButton` | 9-sliced button. Its text shrinks to fit (13*s by default), it can be disabled (grey text, no action), and it brightens on hover. Options: `setColor(hex)` (green `#40C040` = picked / save, red = cancel), `setIcon` (top right corner, text moves down), `setFace(gc -> ...)` (custom drawing on top, e.g. the build rows and More). |
| `guiPanel` | A box with child elements. Rarely used; most screens draw their own panel and keep a list of their buttons. |
| `guiTextField` | One line to type in, drawn as a lightened button. Text from `setText` starts selected (yellow), so typing replaces it. It has a blinking caret, `onEnter`, and takes every key but escape and tab. |
| `slotGrid` | Any block of slots: an inventory kind or any `slotGroup`. Covers `setBounds(x, y, slotSize, gap)`, `indexAt`, `draw` (with item particles) and `interactOnly` (items act on each other without being drawn). |
| `mapGrid` + `miniMap` | A grid of mini-map buttons with a label (and sub-label) under each, at most 140 wide. Optional X delete button, and it can scroll (`scrollable()`, with a thin bar at the right). Used by the save list and the teleports menu. |
| `infoBox` / `iconText` | Info boxes: picture, title, subtitle, text, rows, bar. Lines of text can have icons in them (`iconText`). Used by the inventory info panel, move popups, effect lines and tooltips. |
| `guiText` | All text, in the Silkscreen pixel font. Covers `draw(..., align)`, `width`, `height` (about 1.25× the size), `wrap` and `num` (number formatting). `{key}` icons work in any text (`textIcons`, see README). |
| `guiWaiting` | The "waiting" sign: 8 tinted rocks in a ring, lit one after another. |

### The common look
- **Panel:** `drawPanel` with the inner padding `pad = getBorder() + 8*s` (≈ 28 units). Some screens use `+ 4*s`:
  the inventory, the battle panel, info popups.
- **Title:** centred at the panel's top, at `y + border + 4*s`. It is 16*s black (22*s on the main menu's first
  page) and takes a 30*s band (44*s on the main menu).
- **Labels:** small grey (`rgb(60,60,60)`) text at 11*s, centred, in a `guiText.height(11*s) + 6*s` band. Errors are
  dark red `rgb(160,30,30)`.
- **Standard sizes:** a menu column is 320 wide. Buttons are 40 high with gaps of 8. A small Back button is 100×40 in
  the bottom left corner.
- **Placement:** every menu is centred on the screen (`x = (W - width)/2`, `y = (H - height)/2`). The inventory sits
  20 units above centre.

### Motion (keep new screens consistent)
- **Menus slide.** An element given `slideFrom(edge.TOP)` comes down from the top when shown and goes back up when
  hidden, over `MENU_SLIDE_TIME` = 0.5 s with smoothstep easing.
  - The hotbar and the battle's action panel come from the bottom (`edge.BOTTOM`, `SLIDE_TIME` = 0.25 s).
  - When one menu hides as another shows (settings → teleports, 1v1 page → build inventory), the old one leaves
    out the bottom while the new one comes down (`gui.menusSwitch`, `leaveDown`).
- **Page changes inside a menu use `swapPage(() -> current = next)`.** It snapshots the current page as a picture
  (the ghost), which drops out the bottom while the menu, already switched, comes down from the top. Always change
  pages through `swapPage` (each menu's `show(page)`), never by setting `current` directly. No clicks land until the
  ghost is gone.
- `drawnTop()` / `drawnBottom()` can be overridden when an element draws outside its box (the inventory's chest
  above it), so the slide takes all of it off the screen.
- **Clipping:** never call `gc.clip()`. Use `gui.clipTo(gc, x, y, w, h)` inside `save()`/`restore()` (a JavaFX
  opacity bug, see README).
- **Item glows** are drawn by `gui` after everything else. Do not use blend modes such as `ADD`, which show as
  squares.

---

## 2. Main menu (`guiMainMenu`)

It is shown over a real `gameHandler` with no savefile (`inMenu()`): out in space, zoomed all the way out over the planet (`screenTransition.park`), behind it. It comes
down from the top. Pages (`page` enum) switch with `swapPage`, and escape (`back()`) goes one page back.

```
+------------------------------+       MAIN: 320-wide column, title 22*s in a 44 band
|      Gemstone Guardian       |       buttons 320x40, gap 8
|  [   Play daily battles   ]  |       green: straight into the loadout (the inventory, with Play and Back);
|                              |       red "Daily battles: next in 07:12:33" once played -> page DAILY (the result)
|  [       Play story       ]  |
|  [       Play 1v1s        ]  |       Play 1v1s -> page DUEL (handed to guiDuelMenu)
|  [        Settings        ]  |       Settings -> gui.openVisuals() (the Visuals page alone)
|  [       Quit game        ]  |
+------------------------------+
```
- **LOAD** ("Load campaign"): every savefile as a mini map (`mapGrid`, up to 3 columns, scrolls). Each has its name,
  "Lvl 3, 12 min, 10 Nov 2026" under it and a small X to delete. Under the grid are "Start new campaign" and "Back",
  320×40 each. With no saves it reads "No savefiles yet". Clicking a map runs `handler.playWithEffect(save)`, the
  zoom out through space.
- **NEW:** four steps. The first three have one text field each: "Name the campaign", "What is your name?" and "Who
  is your rival?", each with a grey hint line. Below are a 320×40 field, then Next (disabled while the field is empty)
  and Back. The fourth, "Opponents" ("How strong they play. Kept for this campaign"), has the strength picker instead
  of a field (`guiStrengthPicker`: "Judge: Calculated / Trained" 320×40, then "Looks ahead: n actions" with square
  - and + at its ends, the settings' side-button pattern, here 1 to `aiStrength.CAMPAIGN_MAX_DEPTH` = 3, then
  "Difficulty: Easy / Medium / Hard" 320×40 on green / orange / red, `aiStrength.difficulty`: the depth plus one for
  the trained judge, 2 or less Easy, 3 Medium, 4 Hard), then Start and Back. It starts on the trained judge, 2 ahead
  (`aiStrength.CAMPAIGN`, Medium). The choice is saved in the savefile
  (`aiStrength`) and never changes for that campaign. Enter = Next. Back on step 0 returns to LOAD. Each step change
  is a `swapPage`.
- **DELETE** ("Delete campaign?"): shows the save's name, "will be gone for good", then Delete and Cancel.
- **DUEL:** `layout`, `draw`, `click`, `hover` and keys are all handed to `guiDuelMenu`, and the main menu takes its
  bounds from it. `guiMainMenu.returnToDuel` makes the menu open on the 1v1 page when a 1v1 ends.

---

## 3. 1v1 pages (`guiDuelMenu`)

Its own `page` enum: MAIN, CHARACTER, HOSTING, JOINING. `show(page)` swaps through the main menu's `swapPage`. On
the MAIN page the element is **wider than its panel**: `panelWidth` is the left panel, and the best-streak box sits
to its right. `width` covers both, so they are centred together. Draw anything belonging to the left panel against
`mid = x + panelWidth/2`, not `x + width/2`.

```
+--------------------------------------------+   +--------------------+
|                    1v1                     |   | Highest streak: 4  |
|      Open challenges on your network       |   |  +--------------+  |  portrait 112 in a
|  [ Join Bob  Level 13  (o)(o)(o)(o) ]     |   |  |   (sprite)   |  |  framed slot (framed =
|  ... up to 4, or "Looking" + waiting sign  |   |  |              |  |  this build is picked)
|  (red message line when something failed)  |   |  +--------------+  |
|  [           Create challenge          ]   |   |   Name as Boy      |
|  [        Battle random        ] [ AI ]   |   |  [r][r][r][r]      |  4 rock slots, 34
|        Builds (click one to use it)        |   |     [t][t]         |  2 trinket slots
|  [ (r)(r)(r)(r) (t)(t)       ] [ Edit ]    |   |  [Equip loadout ]  |  green when picked
|  [ (r)(r)(r)(r) (t)(t)       ] [ Edit ]    |   +--------------------+
|  [ (r)(r)(r)(r) (t)(t)       ] [ Edit ]    |     230 wide, 16 gap from the panel,
|  [ (?)(?)(?)(?) (?)(?) rolling] [Random]   |     top aligned, only as tall as its content
|  [ (face) Character: Player as Boy     ]   |
|  [Back] [-] [  Battle level: 13  ] [+]    |
+--------------------------------------------+
   inner width 420; build rows 48 high, Edit 70 wide; the picked build is tinted green #40C040
```
- **Builds:** `duelBuild.library.shared()` holds `SLOTS` = 3 editable builds, plus the best-streak build
  (`library.BEST` = index 3, not editable). `library.selected` is the one in use and is saved at once.
  - A build row's face draws its rocks and trinkets as little slots, with the equipped rock framed (`drawSlots`).
  - **Random build** (a fourth row, `library.RANDOM` = index 4, "Random" is a disabled label where Edit is): its
    slots cycle through every handed-out rock and trinket (`drawRandomBuild`, each slot on its own 0.45 s beat).
    Picked like a build (green, saved), but every fight gets a new `duelBuild.random`: rolled in `rollBuild()` when a
    challenge is made or joined or Battle random starts (kept in `inUse` for the hosting page and the handshake), and
    again on each Battle again (`duel.offerRematch`).
  - **Edit** opens the real inventory in build mode (section 6) with a catalogue of every handed-out rock and
    trinket above it.
- **Create challenge / Battle random** are disabled when the build in use has no rock. Battle random runs
  `duel.againstRandom` → `handler.duelWithEffect`.
- **Best streak box (right):** drawn by `drawBest`. Without a record it shows "Highest streak: 0", an empty frame,
  "Win random battles in a row" and a disabled Equip loadout.
- **AI** (70 wide, at the end of Battle random's row like Edit) opens **STRENGTH** ("Computer strength"): a grey "In
  random battles" label, the strength picker (the campaign's Opponents step's first two rows, up to 5 ahead, no difficulty row) and a 100×40 Back
  bottom left. Kept in `settings.txt` (`settings.getDuelAI`).
- **Battle level** (in Back's row, so the page gets no taller): "Battle level: n" with square - and + at its ends,
  0 to `duel.MAX_LEVEL` = 25 (default 13). Both fighters are set to it, in random battles and over the network,
  where the one who made the challenge picks it: it is in the challenge's announcement, shown grey after the name
  on its Join row ("Level 13") and on the hosting page ("Name, level 13"), and sent again with START. The build editor shows the rocks at it. Kept in `settings.txt`
  (`settings.getDuelLevel`).
- **CHARACTER** ("Character settings"):
  - a "Your name" label and a text field (16 characters);
  - "Play as";
  - a grid of 8 columns of 44 slots (gap 6) with every campaign character's portrait, the picked one framed and the
    name as tooltip;
  - Back (100×40, bottom left), which saves the name. Enter also closes.
- **HOSTING / JOINING:** a 320-wide panel showing your name and your four rocks (hosting), "Others on your network
  can join from 1v1" and a 56-unit waiting sign, with Cancel bottom left. `pollConnection()` runs from `layout`
  (every frame) and does the handshake.

---

## 3b. Daily battles (`guiDailyMenu`)

"Play daily battles" (green) opens the daily build straight in the inventory (`guiDailyMenu.editLoadout`, the build
editor of the 1v1 page with the title "Daily battles, 19 Nov 2026"): Play (green, disabled with no rock in the rock
slots) saves the build and zooms into the first fight; Back (red) and escape close it, back on the main menu. Once
today's try is used the button is red and opens the main menu's DAILY page, handed to `guiDailyMenu`: the result.
`guiMainMenu.returnToDaily` opens it on the result after a run.

```
+----------------------------------------------------------+   RESULT: 512 column (+ pad), about 470 high
|               Daily battles, 19 Nov 2026                  |
|                    3 / 5 BEATEN                           |   26, gold when all five are beaten
|  [grey] [grey] [grey] [bounce] [bounce]                   |   5 squares of 84, the one lost to framed
|   Name   Name   Name   Name    Name                        |   11 (shrinks to fit), place 10 grey under
|  [Brilliant 12][Great 5][OK 3][Bad 1][Terrible 0]          |   5 tiles 96x50, gap 8, the grade's colour
|  [=========== bar of the grades ===========]              |   12 high
|  Accuracy 87%    Turns 41    Damage 1830    Best hit 212   |   11 grey
|              ▪▪▪▪ ▪▪  (the build used, 30 slots)          |
|  [ Back ]                  Next daily battles in 07:12:33 |   Back 100x40, countdown 11 gold
+----------------------------------------------------------+
```
- The beaten opponents are grey and still (`ColorAdjust`, as the 1v1 loser), the rest bounce (`character.draw`).
- Motion: the squares pop in 0.12 s apart (grow out of their middle, 0.25 s), the counts and the bar count up from
  0.8 s over 0.6 s. Escape and Back go to the main menu.
- During a run the battle screen's top panel reads "Daily battle 3 / 5" over "Beaten so far: 2" (`drawStreak`), and
  the end screen has one button: "Next battle (3/5)" or "See results" (`fightOutcome.offer` with labels and no back).

---

## 4. Settings (`guiSettings`), opened with escape in a game

It comes down from the top. A page is a title plus one column of 320×40 buttons (gap 8). **Button texts are
refreshed every frame in `updateTexts()`**, so toggles always show their state ("Show borders: On").

- **MAIN** ("Settings"): Back to game, Hidden secrets, Visuals, Save and quit to menu.
- **SECRETS** ("Hidden secrets"): Phase through blocks, Show borders, Darkness in caves, Cheat mode, Teleports (closes settings,
  opens the teleports menu, whose Back reopens Secrets), Give item, Level, Speed, Floor, Back.
  - **It is full:** 10 buttons, about 566 units tall. An eleventh does not fit the 620 budget; add another page.
  - **Side buttons:** Level, Speed and Floor are disabled "value" buttons with a square "-" and "+" (40×40) at each
    end of their row, the label button narrowed between them (`sideButtons` map). Use this pattern for any number
    setting.
- **VISUALS** ("Visuals"): GUI size, Fluids, Back.
  - From the main menu, `gui.openVisuals()` shows only this page (`visualsOnly`), and Back closes the settings.
- Escape closes it (`gui.escape()`: it backs out of whatever is open, and opens the settings when nothing is).

---

## 5. Teleports menu (`guiTeleports`)

Title plus a `mapGrid` of mini maps, as big as fits the screen, with "World (cx, cy)" under each, and a 320×40 Back
under the grid. Clicking a map closes the menu and runs `teleportWithEffect`. The maps are disabled during a fight,
and it reads "No healing pads yet" when empty. It is opened by the Teleports secret (every pad) and by the
teleporter item (the pads you have stood on). `open(targets, title, back)`: `back` runs after Back closes it.

---

## 6. Inventory (`guiInventory`), E / I / chests / build editing

It comes down from the top. **The world behind is dimmed** (`gui.INVENTORY_DIM` 0.3, fading over 0.2 s).

```
            +---------------- chest / catalogue (only with openWith / openForBuild) ---+
            |  Title                                                                   |
            |  [ ][ ][ ][ ][ ][ ][ ][ ]   up to 3 rows showing, wheel scrolls          |
            +--------------------------------------------------------------------------+
 +-----+    +---------------------------------------+    +----------------------+
 | [t] |    | [ ][ ][ ][ ][ ][ ][ ][ ]  top rows:   |    | Item info panel      |
 | [t] |    | [ ][ ][ ][ ][ ][ ][ ][ ]  4 showing,  |    | (hovered or held     |
 +-----+    | [ ][ ][ ][ ][ ][ ][ ][ ]  wheel       |    |  item, else the      |
 trinkets   | [ ][ ][ ][ ][ ][ ][ ][ ]  scrolls     |    |  player's stats)     |
 (column,   | ------------- divider 20 -----------  |    | 260 wide, at least   |
  left)     | [R][R][R][R] [I][I][I][I] active row  |    | as tall as the inv.  |
            +---------------------------------------+    +----------------------+
                     +----- elemental rock: 3 slots (right click the rock) -----+
```
- **Sizes:** slots are 48 with a gap of 4. There are 8 columns (`inventory.COLUMNS`), `VISIBLE_ROWS` = 4 top rows and
  a 20 divider above the active row (4 rocks, 4 items). `pad = border + 4*s`.
  - The trinket panel, the inventory and the info panel (260) are laid out side by side with 8 gaps, and the three
    together are centred.
- **Mouse:** click picks a stack up onto the cursor and puts it down, stacks or swaps (`slotGroup.click`). Right
  click a rock to equip or un-equip it (the equipped one has the framed slot). Right click an item with an inventory
  action to run it (it lights up on hover). The wheel scrolls the top rows, or the chest when over it.
  - Whatever is on the cursor goes back when it closes.
- **With a chest** (`gui.openContainer(group, title)`): the chest panel, as wide as the inventory, sits above it, and
  the pair is centred vertically. Items move between the two. An `itemSource` is an endless catalogue: the Give item
  cheat and the 1v1 build editor.
- **Elemental rock:** its 3 slots open in a small panel under the inventory.
- **Build mode** (`openForBuild`, from the 1v1 page):
  - the top rows and the item slots are covered with the panel's middle texture (they take nothing);
  - only the rock and trinket slots work;
  - Cancel (red) and Save (green), 140×36, sit under the inventory;
  - the elemental slots move into the covered top area.

---

## 7. Hotbar (`guiHud`), while walking

8 slots of 40 (gap 4): the 4 rocks then the 4 items, centred, 12 above the bottom edge. It comes up from the bottom.
Clicking a rock equips it (framed). Hovering shows the item's name as a tooltip. It swallows clicks on itself and is
hidden in fights, menus, the inventory and end screens.

---

## 8. Speech box (`guiDialogue`)

At the bottom, `min(700, W - 40)` wide and at least 110 tall, growing with the lines. Its bottom edge is 70 above
the screen bottom, which leaves room for the battle panel's top. It shows "Name:", the wrapped text (12*s) and
"----- PRESS SPACE TO CONTINUE -----" in grey.
- A special message (`dialogue.highlight`) gets a coloured border inside the frame and a picture on the right
  (about a quarter of the box).
- **Clicks fall through**, so a click anywhere continues.
- Space / click → `dialogueContainer.next()`, which asks the message's `letGo()` first. A message can refuse: the
  end screen does this to skip its animation first.

---

## 9. Battle screen (`guiBattle`)

The element covers the whole screen, and its parts are placed inside. It is shown while a fight is active and no end
screen is up.

```
+--------------------------+        +----------------+        +--------------------------+
| Boy (Lvl 13)    120/120 [P]|      |  Win streak: 3 |        | Girl (Lvl 13)   80/120 [P]|
| [=========red bar======]   |      |    Best: 5     |        | [======red bar====    ]   |
| (o) Rock: Ruby             |      +----------------+        | (o) Rock: Sapphire        |
| (fire) 3   (rounds) 2      |   only in random battles,      | ...one line per effect    |
+--------------------------+     top centre                     +--------------------------+
   280 wide or more (as wide as the longer name line needs, both the same), 56 + a line per effect,
   at (20, 20) and at the right edge

                         (the fighters, in the world)

 +--------------------------------------------------------------------------------------+
 | [ Move 1 (3/5) [80%] (i)] [ Move 2 ... (i)] [ Move 3 ... (i)] [ More  ▪▪▪▪ / ▪▪▪▪ ]  |
 +--------------------------------------------------------------------------------------+
   action panel: full width minus 20 on each side, 12 above the bottom, height 2*pad + 64
```
- **Health boxes** (`drawHealth`):
  - the name and level on the left, the hp numbers on the right, the portrait at the right end;
  - both boxes widen together to fit the longer name line (`healthBoxWidth`, measured at the widest the health can be
    written so they never change size), up to leaving 20 between them (200 for the streak panel); a name too long
    even then is drawn smaller, down to 60%;
  - a red bar that eases to the shown health;
  - a line for the equipped rock, then one `iconText` line per effect, so the box grows downward with effects.
- **Streak panel** (`drawStreak`): only when `battle.getDuel().isAgainstComputer()`, centred between the bars at
  y = 20.
- **Action panel:** slides up from below (0.25 s) when it is your turn (`panelShown()` = waiting for the player) and
  back down when a move is picked.
  - **Moves view:** 3 move buttons and More, each a quarter of the width. A move button shows the name, uses
    "(left/max)" and the hit chance "[80%]", with the ability type's icon top right. It is disabled with no uses
    left.
  - **More button:** its face shows "More" and tiny copies of your rock and item rows (12-unit slots).
  - **More view:** the rock row and item row (48 slots), with Go back and Give up (160×36) under them, and the
    panel taller. Clicking a rock or item uses it (`battle.useItem`).
- **Popups:** take 0.25 s each way and are drawn by `gui` after everything else, cut off at the edge they rise from
  with `clipTo`.
  - A hovered move's `infoBox` rises out from behind its button and sits on it.
  - A hovered rock or item in More shows the inventory's info panel (`guiInventory.drawItemDetails`), rising from
    behind the action panel, centred on it, always in the same spot.
  - Nothing follows the mouse in battle.
  - **Cheat mode** (Hidden secrets, kept in `settings.txt`): a move's popup gets a last row "Win chance 62%" (the
    trained judge) or "Score 0.62" (the calculated one, which is no true chance); a rock or item's panel gets the
    same line at its bottom. The best action's line is green (`HINT_BEST`). The held rock, rocks with no uses left and
    items with no effect in a fight have none. Worked out on a thread at the start of the turn
    (`battle.getHints`), so it appears a moment later. Never in a 1v1 against a person, and a random battle won with
    it on does not count for the streak.
- **While the panel is down** the hotbar items still act on each other off screen (`slotGrid.interactOnly`, offset by
  the screen height).
- **1v1 waiting:** when it is the other player's turn, a 300×64 panel with the waiting sign and "Waiting for X" sits
  where the action panel would be.
- The panel swallows clicks inside it. The rest of the screen passes clicks through (to the speech box).

---

### Battle intro (`battleIntro` + `guiBattleIntro`)

Every fight starts with it (`battle.newBattle` → `battleIntro.forBattle`). It covers the whole screen; the battle
screen and the fight itself (`battle.carryOn`) wait until it is over.

```
   +--------------------------------------------------------------+
   |        Boy                                    Girl           |
   |      ______                                 ______           |
   |     /     /                                 \     \          |
   |    / you /             VS                    	hem \         |
   |   /_____/                                     \_____\        |
   +--------------------------------------------------------------+
     each 78% of the screen height (at most 42% of the width), middles at 26% from each side, feet 2% above the bottom
```
- The world darkens by up to 40%. The fighters bounce as they do in a box (`character.draw`) and are turned 6° towards
  each other (rotated about the feet), each with its name (26, white, shadowed) over its head. VS (64, gold) pops in
  the middle once they are nearly up.
- **Timing:** rise from below the screen over `RISE` 0.5 s, stay `HOLD` 1 s, then the left one slides out to the left
  and the right one out to the right over `LEAVE` 0.5 s (smoothstep). Everything uses real time.
- **1v1:** `gameHandler.duelWithEffect` starts it as the menu world zooms out (`duel.introduce`, stand-ins with the
  players' names and looks). The stand-ins are given the builds, so their trinkets are worn from the start. It stays up through space; the
  fight's game puts its real fighters in, and it leaves as
  that game starts zooming in (`gameHandler.zoomingIn`). The intro is shared between the two games, so it is static.
  There is no "X vs Y" message any more.

---

## 10. End screens: win, lose, 1v1 (`guiOutcome` + `fightOutcome`)

A `fightOutcome` is a dialogue message, so it queues like any other, and `guiOutcome` draws it full screen while it
is the current message. The hotbar, battle screen and speech box are hidden meanwhile. Kinds: WIN, LOSE and DUEL
(any 1v1, network or random).

**Vertical budget (the lesson from the streak work).** The screen is stacked top to bottom:
1. the banner panel (VICTORY / DEFEATED, 54*s text, box = text + 34) with its top at 10% of the screen height;
2. an optional line under it ("X was beaten by");
3. the content panel;
4. at the bottom: the choice buttons (200×40, gap 12, their top at `H - 46 - 12 - 40`) and the pulsing space hint
   at `H - 46`.

The 1v1 panel already ends just above the buttons. **Do not make the content panels taller**: use the horizontal
room instead. The 1v1 panel widens its middle gap from 20 to `STREAK_COLUMN` = 170 for the streak.

```
            +=================+
            |    VICTORY      |   slams down (gold), or DEFEATED fades in (red)
            +=================+
 +-----------------------------------------------------------------------+
 |  [winner portrait]          WIN STREAK          [loser portrait,grey] |
 |      Name                       4                    Name             |
 |     Winner                  NEW BEST!               Beaten            |
 |  [r][r][r][r]                                    [r][r][r][r]         |
 |     [t][t]                                          [t][t]            |
 |  Damage dealt: 120                               Damage dealt: 80     |
 |  Best hit: 40                                    Best hit: 25         |
 |                          Turns: 7                                     |
 +-----------------------------------------------------------------------+
          [ Battle again ]   [ Back to menu ]      (random battles only)
```
- **WIN** (campaign):
  - a white flash (`FLASH`), then VICTORY slams down (`SLAM`) with a bounce, gold sparks and a camera shake, and the
    loser is knocked back;
  - the winning rock, big (96) and glowing, with its name;
  - a 440-wide panel: "+XP" counting up into the level bar (LEVEL UP! pops per level), the drops popping out one by
    one (44 slots, `changeBurst`), and the fight in numbers.
- **LOSE** (campaign): the camera zooms in, the player
  falls over, DEFEATED fades in, and a 440-wide numbers panel follows.
- **DUEL:** the banner is from the viewer's side. The winner is on the left (framed slot), the loser on the right,
  greyed. Each column is 230 and shows the build and per-side numbers. "Turns" is at the bottom.
  - **Random battles add the streak in the middle column** (`drawStreak`). Nothing of it shows until the panel is in
    (`fightOutcome.streakAt()`). Then it is slapped on: it shrinks from 3× to its size over `STREAK_SLAM` 0.18 s,
    squashes, throws sparks and shakes the panel (`streakShake`).
  - `STREAK_NOTE` 0.3 s later the line under it pops in: "NEW BEST!" (pulsing green) or "Lost a streak of X" (red).
  - With cheat mode on in that fight (`fightOutcome.cheated`) the number is **CHEAT!** in red instead, shrunk to fit
    the column, with red sparks: the win did not count.
- **Timing model:**
  - Everything is keyed off `o.time()` (seconds into the screen, skips included). `done()` is when everything has
    played out, and the buttons and "continue" wait for it. **If you add a later beat, extend `done()`**, as the
    streak does.
  - Fire one-off effects with `o.fire("key")`, which is true only the first time.
  - Ease with `fightOutcome.ease`.
- **Space:**
  - Space before `HOLD` (0.6 s) does nothing.
  - Space before `done()` skips to the end (`skipped`).
  - Space after `done()` continues. **With a choice (random battles) it does nothing**: only the buttons lead on.
  - The hint reads "PRESS SPACE TO SKIP" / "PRESS SPACE TO CONTINUE", and is hidden while the buttons are up.
- **The final hit plays in slow motion** before the screen (`dialogue.slowMotion`, `timeScale` 0.35).
- **What the fight sets off** (events, restoring, rematch) waits in `fightOutcome.afterwards(...)` until the screen
  is dismissed.
- **Leaving** (a choice was picked): over `LEAVE_TIME` 0.4 s the screen slides up, the darkening, grey and zoom fade
  and the loser gets up. Only then does the action run (the zoom through space).

---

## 11. Transitions between game and menu

These are not menus, but they move menus around.
- The main menu's game is parked in space (`screenTransition.park`, `isParked()`), all the way zoomed out. It is not
  a trip: the menu shows and takes clicks (`inSpaceTrip()` and `inTransition()` leave it out).
- From the menu, `playWithEffect(save)` and `duelWithEffect(fight)` only zoom in: the menu slides away
  (`MENU_SLIDE_TIME`; a 1v1 waits for its fighters to rise instead, `INTRO_IN_SPACE`), then the new game zooms in
  from space. `quitToMenu()` only zooms out, and the menu takes over in space. From a game, `duelWithEffect` (battle
  again) and `teleportWithEffect(...)` still zoom out and back in (`inSpaceTrip()`).
- During the trip the battle screen hides and returns when nearly landed (`nearlyLanded()`), and the main menu is
  hidden.
- Quitting to the menu slides a fresh main menu over the game (`gui.setMenuOverlay(gui.makeMenu())`). The game is
  rebuilt behind it.

---

## 12. Checklist for a new or changed screen
1. Sizes ×`s`, layout in `layout()` every frame, centred.
2. Check the **height** against about 620 units (less on short windows), and against whatever else is on screen at
   the same time (buttons, hints, the speech box at the bottom).
3. Use `drawPanel`, the title band, 320×40 buttons and grey 11*s labels so it matches.
4. Make page changes with `swapPage`, and showing and hiding through `gui.updateVisibility` / `slideFrom`.
5. Make it modal (`click` returns true) if it is a menu, and add escape handling in `gui.escape()` or the menu's
   `back()`.
6. Stage animations off one clock, and extend the "done" time so buttons wait for the last beat.
7. Start the game and ask the owner to look. Do not screenshot.
