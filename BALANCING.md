# Gemstone balancing — progression gates

The NPCs that block progress, in the order the player meets them. For each one:
which stone(s) the NPC fights with, its level (max HP = 100 + 20 x level), and every
gemstone the player can have picked up by the time they reach it. Stones only come
from chests (beaten NPCs do not drop their stones), so the "you have" list is the
chests that are reachable before the gate.

The goal: with the level filled in below, the player should only just beat the gate.

Source: `gameHandler` constructor (characters, battles, `chestAt(...).put(...)`). Opponents choose with `battleAI` (scored actions, top-p 85%). Bosses carry items they use when it is worth it: King (Healing Potion), Girl (Master) (Healing, Cleansing), Ash Golem, Desert Nomad, Cloud Knight (Healing), Prism Warden (Healing, Cleansing), Stone Guardian (Wool Bandage), Crystal Boy (2 Healing, Cleansing). Used ones stay used if you lose and fight again.

---

- **Girl (Beginner)** — level 0 (100 HP) — uses: Rock
  - You have: Rock (Grandma's chest, with a healing potion)
  - Beaten the first time she drops the Pebble (and a healing potion)
  - You start at level 1, so you meet her at level 1

- **Keeper of Fire** (lava labyrinth, underground, through the secret path behind the tree) — level 2 (140 HP) — uses: Obsidian
  - You have: Rock, Pebble
  - Behind him: Obsidian chest (+ healing potion)
  - Should be level 2 by meeting him

- **Knight** (castle entrance) — level 6 (220 HP) — uses: Giant Bolder
  - You have: Rock, Obsidian, Pebble
  - Behind him: Giant Bolder chest (+ healing potion), castle 1st floor, and a healing pad / checkpoint at (10000.44, 9990.89)
  - XP fights before him: citizens in the park/village, level 3–4; sheep, level 1
  - Should be level 4 by meeting him

- **Paladin** (castle, 1st floor, guards the stairs up) — level 6 (220 HP) — uses: Boltstone
  - Wears: Feather of Hermes Boots (walks x1.75). Beating him gives: Feather of Hermes Boots
  - You have: Rock, Obsidian, Giant Bolder, Pebble
  - Behind him: Boltstone + Copper chest (up the stairs, out the 2nd floor door, in the courtyard). Copper turns into Charged Copper when the Boltstone's lightning hits it
  - Should be level 4 by meeting him

- **King** (castle, 1st floor, guards the way out to the cave) — level 7 (240 HP) — uses: Boltstone
  - Wears: Heart of the Warrior (+10% max health, so 264 HP). Beating him gives: Heart of the Warrior
  - You have: Rock (or Lava), Obsidian, Giant Bolder, Boltstone, Copper (or Charged Copper), Pebble
  - Behind him: the cave, with the Uranium chest (+ healing potion) and a healing pad / checkpoint at (10002.44, 9986.33) just before Cornerstone
  - Should be level 5 by meeting him

- **Cornerstone** (blocks the cave exit) — level 8 (260 HP) — uses: Uranium
  - You have: Rock (or Lava), Obsidian, Giant Bolder, Boltstone, Copper (or Charged Copper), Uranium, Pebble
  - Behind him: nothing new, only the way out of the cave
  - Should be level 6 by meeting him

- **Citizen Peter** (outside the cave, in front of the small house) — level 9 (280 HP) — uses: Pebble
  - XP fight, not a gate on its own, but you pass him before anything else outside the cave
  - You have: Rock (or Lava), Obsidian, Giant Bolder, Boltstone, Copper (or Charged Copper), Uranium, Pebble
  - Behind him: Saltstone chest (+ healing potion) in the house with the door at (10002.44, 9983.67), and a chest with a healing potion in the house at (10003, 9981). Saltstone turns Water into Salt Water once you have Ice
  - Should be level \_\_\_ by meeting him

- **Guardman** (outside the cave, blocks the path north) — level 11 (320 HP) — uses: Pebble
  - Wears: Creatine (+10% damage of his moves). Beating him gives: Creatine
  - You have: Rock (or Lava), Obsidian, Giant Bolder, Boltstone, Copper (or Charged Copper), Uranium, Saltstone, Pebble
  - XP fights before him: citizens Peter, Marcus, Martin, Leidulf Lie, Edna, Johann, Endre, level 8–9
  - Healing pad / checkpoint one tile above where he blocks the path, at (10002.44, 9980.22), reached once he is beaten
  - Should be level 9 by meeting him

- **Weird Crab** (beach) — level 11 (320 HP) — uses: Discombobulate Shard
  - You have: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble
  - XP fights around: Cool Crab, Normal Crab, Eugene Krabs, Magical Crab, Crabby, level 10
  - Should be level 10 by meeting him

- **Cool Crab** (bridge, just past the Weird Crab) — level 10 (300 HP) — uses: Pebble
  - XP fight, the first of the crabs after the Weird Crab
  - You have: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble
  - Should be level 10 by meeting him

- **Sandman** (sandcastle) — level 13 (360 HP) — uses: Discombobulate Shard
  - You have: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble
  - Behind him: Discombobulate Shard + Sandstone chest (sandcastle inside). Sandstone turns into Glass next to Magma (Magma comes much later)
  - Should be level 11 by meeting him

- **Tree Guard** (beach path east) — level 13 (360 HP) — uses: Meteorite
  - You have: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble, Discombobulate Shard, Sandstone (or Glass)
  - Behind him: Ice chest (ice pond). Ice melts into Water next to Magma or lightning; Obsidian freezes it back; Saltstone turns Water into Salt Water
  - Should be level 12 by meeting him

- **Snowman** (snow house) — level 13 (360 HP) — uses: Meteorite
  - You have: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble, Discombobulate Shard, Sandstone (or Glass), Ice (or Water, or Salt Water)
  - Beating him gives: Meteorite (chest in the snowcastle inside)
  - XP fight: Kalle (snowcastle inside), level 13
  - Should be level 13 by meeting him

- **Rock** (path up the mountain) — level 14 (380 HP) — uses: Pebble, Healite
  - You have: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble, Discombobulate Shard, Sandstone (or Glass), Meteorite, Ice (or Water, or Salt Water)
  - Behind him: Healite chest (underground, next to Gemstone Guard — optional XP fight, level 14, Pebble + Healite)
  - Should be level \_\_\_ by meeting him

- **Security Guard** (final castle door, with Guard 4 and 5) — level 15 (400 HP) — uses: Giant Bolder, Healite
  - You have: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble, Discombobulate Shard, Sandstone (or Glass), Meteorite, Ice (or Water, or Salt Water), Healite
  - Behind him: Magma chest (castle maze). Unlocks Glass (Sandstone + Magma) and Coal (Magma + Obsidian/Ice)
  - Should be level \_\_\_ by meeting him

- **Girl (Master)** (final boss) — level 17 (440 HP) — uses: Meteorite, Boltstone, Giant Bolder, Healite
  - Wears: Withered Heart (-5% max health, so 418 HP; +10% damage of her moves). She does not drop it
  - You have: everything above + Magma, Glass, Coal
  - Should be level \_\_\_ by meeting her

---

## After the final boss

Beating the girl still records the highscore. Then the portal behind her opens: the volcano caves, up to the desert, through a sky portal to the sky ruins, through another down into the crystal caverns. Each area has a healing pad.
You have everything from the main game: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble, Discombobulate Shard, Sandstone (or Glass), Meteorite, Ice (or Water, or Salt Water), Healite, Magma, Coal. New gems found on the way: Singularity (right behind the girl, in her room), Sunstone (sky ruins), Moonstone (crystal caverns), Mirror Shard (mirror dimension).

- In the girl's room, top right corner: chest with the **Singularity** and a Healing Potion

- **Ember Wraith** (volcano depths, XP fight) — level 17 (440 HP) — uses: Magma, Obsidian
  - In a side pocket of the caves: chest with a Healing Potion and a Cleansing Potion
  - Should be level \_\_\_ by meeting him

- **Ash Golem** (volcano gate) — level 18 (460 HP) — uses: Giant Bolder, Lava
  - Should be level \_\_\_ by meeting him

- **Dune Scorpion** (desert, XP fight) — level 18 (460 HP) — uses: Sandstone, Discombobulate Shard
  - Should be level \_\_\_ by meeting him

- **Desert Nomad** (desert gate) — level 19 (480 HP, wears Creatine: +10% damage) — uses: Saltstone, Glass
  - Should be level \_\_\_ by meeting him

- In the sky ruins' west pocket: chest with the **Sunstone** and a Healing Potion

- **Storm Harpy** (sky ruins, XP fight) — level 19 (480 HP) — uses: Boltstone, Copper
  - Should be level \_\_\_ by meeting her

- **Cloud Knight** (sky gate) — level 20 (500 HP, wears Feather of Hermes Boots) — uses: Meteorite, Healite
  - Should be level \_\_\_ by meeting him

- In the crystal caverns' north-west pocket: chest with the **Moonstone** and a Healing Potion

- **Crystal Sentinel** (crystal caverns, XP fight) — level 20 (500 HP) — uses: Uranium, Ice
  - Should be level \_\_\_ by meeting it

- **Prism Warden** (crystal throne, last boss) — level 22 (594 HP with the Heart of the Warrior) — uses: Meteorite, Boltstone, Uranium, Healite
  - Beating it gives: the chest behind it, with the Elemental Rock and a Healing Potion
  - Should be level \_\_\_ by meeting it

Next to that chest a passage leads up into the open again: the castle grounds, with the crystal boy's castle a few chunks north. There is a healing pad on the plaza in front of it, and a chest with a Healing Potion and a Cleansing Potion hidden in the trees east of the meadow.

- **Crystal Boy** (castle hall, holds the girl; he kidnaps her right after she is beaten) — level 24 (580 HP) — uses: Mirror Shard (equipped first), Uranium, Meteorite, Healite. When he flees he leaves the Mirror Shard behind and takes Ice instead, so the rematch is with Ice, Uranium, Meteorite, Healite
  - Beating him frees the girl (she follows you from then on); he flees through the mirror behind him into the mirror dimension
  - In the mirror dimension, on the way to him: a **Stone Guardian** (a copy of the castle guards from the start) in the gap of the mirrored village wall, where the girl first stood — level 23 (560 HP) — uses: Giant Bolder, Meteorite, Healite; beaten, it crumbles and the way is open
  - Should be level \_\_\_ by meeting it
  - In the mirror dimension, on the grass by its healing pad: chest with the **Mirror Shard** and a Healing Potion
  - **Rematch** in the mirror dimension (the starting village mirrored), at the top of its path: the same Crystal Boy, healed — beating him there ends the game: you become him, and it begins again
  - Should be level \_\_\_ by meeting him

---

## Notes

- The player starts at level 1 (level = cube root of xp, rounded down).
- After run 3 (tools/ai/rock_scores/3_touchup) every rock's damage, burn and poison amounts were raised 25% (rounded up; heals, self costs and potions left as they were), because fights ran out of moves before anyone fell. A change shared by every rock barely moves the rocks against each other, but healing counts for a little less now; no run has measured it yet. Rock's +2 damage and Healite's bigger level-5 drain from just before are in it too.
- Rock turns into Lava when heated by Magma or struck by lightning twice (Boltstone), and back into Rock next to Ice or Obsidian. Lava keeps the uses left. Lava is hot: it melts Ice faster than one Ice or Obsidian can cool it, but Ice and Obsidian together cool it first.
- Charged Copper, Rainbow Uranium and Charged Healite are temporary states (a few seconds after a lightning hit), so they matter little in battle unless charged right before.
- Every XP-only NPC (citizens, sheep, crabs, Kalle, Gemstone Guard) fights with a Pebble.

---

## Rock directions (for balancing)

Every rock has a main direction (what it is mostly about) and, where one fits, a side direction. A balancing change
should push a rock further into its directions rather than give it something new. These are only notes for
balancing: the game does not use them. Elemental Rock has none, because it becomes the two rocks it holds.

Rocks that turn into other rocks follow one rule:
- **A rock that becomes another rock for good changes its main direction.** It plays differently, not just better.
  The pairs are Rock and Lava, Ice and Water, Water and Salt Water, Sandstone and Glass, and Magma and Coal.
- **A charged form keeps its rock's directions and curve, only stronger.** Its target is the plain rock's target
  plus 3 points (at most 65%). The pairs are Copper and Charged Copper, Uranium and Rainbow Uranium, and Healite and
  Charged Healite. A charged form lasts only a few seconds after a hit.

The directions:
- **Striker:** plain damage that grows with level.
- **Nuker:** hits for a share of their max HP, so it is just as strong at every level. Boltstone gambles (its big hits often miss); Meteorite pays for them with its own HP.
- **Drainer:** takes their HP over to you.
- **Burner:** damage over time (burn, poison).
- **Trickster:** confusion.
- **Healer:** keeps you alive (healing, cleansing).
- **All-rounder:** an attack, a bigger or special one, and a heal or cleanse, nothing standing out.

| Rock | Main | Side |
|---|---|---|
| Rock | All-rounder | Striker |
| Lava | Burner | Striker |
| Pebble | All-rounder | Healer |
| Copper | All-rounder | Trickster |
| Charged Copper | All-rounder | Trickster |
| Saltstone | All-rounder | Burner |
| Coal | All-rounder | Trickster |
| Glass | Striker | Trickster |
| Meteorite | Nuker | Striker |
| Boltstone | Nuker | - |
| Giant Bolder | Drainer | Trickster |
| Singularity | Drainer | Nuker |
| Magma | Burner | - |
| Obsidian | Burner | Healer |
| Uranium | Burner | Striker |
| Rainbow Uranium | Burner | Striker |
| Discombobulate Shard | Trickster | Burner |
| Moonstone | Trickster | - |
| Mirror Shard | Trickster | Striker |
| Ice | Trickster | Striker |
| Sandstone | Trickster | Burner |
| Salt Water | Trickster | Healer |
| Healite | Healer | Drainer |
| Charged Healite | Healer | Drainer |
| Sunstone | Healer | Striker |
| Water | Healer | - |

---

## Rock curves (for balancing)

Each rock has a target curve: its chance to win, at each level from 0 to 25, against a random opponent of the same
level (measured with `tools/ai/score_rocks.py`, targets in `tools/ai/rock_targets.py`). Every target stays between
40% and 65%. Most rocks are at their best around when the player finds them; the late rocks rise towards the end.
The opponent is random, so at every level the rocks average about 50%. If some rocks are strong at a level, others
have to be weak there, and the targets below average 49 to 52% at every level.

The rules for reaching them:
- **A rock always gets better with level.** Every amount grows with it: "a + b x lvl" or a share of max HP (which
  grows with the level). No caps such as "min(lvl, 7)", nothing that shrinks, and no bare constants.
- **No "lvl x lvl" or other terms that outgrow everything.** A rock that outscales the rest would be the only one
  worth taking. With "a + b x lvl" a move's share of the target's health slides from a/100 at level 0 towards b/20,
  so every rock settles down and stays playable at level 50 and beyond.
- **No rock above 65%, at any level** (score_rocks also checks levels 30, 40 and 50).
- **A curve's shape comes from level milestones**, and a rock fades only because others get better than it.

### Level milestones

A move can get better at a level that is a multiple of 5 (`moveEffects.from(level)` in `gemstones.java`): an extra
effect, an extra amount of one it has (damage adds up; a stronger burn, poison or confusion takes over), an extra use
(`moreUses`) or a better hit chance (`hits`). Its info box shows the reached milestones as normal lines and the next
one greyed under "At level N", and the game says "Rock's Move got stronger!" when the player reaches one. A rock peaks
while its milestones are fresh and fades as other rocks reach theirs, so the milestones are placed where each curve
should rise: Lava and Copper at 5, Giant Bolder at 5 and 10, Uranium at 10 and 15, Ice at 10 and 15, Glass and Magma
at 15 and 20, Mirror Shard at 20 and 25, and so on. Rock has none: it fades out by itself.

The shapes, with x the level (0 to 25):

| Shape | Sketch | Formula |
|---|---|---|
| Flat | `────` | constant |
| Linear | `╲` or `╱` | a straight line from a at level 0 to b at level 25 |
| Bell | `_/‾\_` | low + (high − low) · e^(−(x − peak)² / 2w²) |
| Skewed bell | `/‾‾\__` | a bell with a different width w on each side of the peak |
| Valley | `‾\_/‾` | a bell turned over: high − (high − low) · e^(−(x − dip)² / 2w²) |
| Mesa | `_/‾‾‾\_` | rises to high, stays there from one level to another, then falls |
| Logarithmic | `/‾‾‾` | a + (b − a) · ln(1 + x) / ln(26): rises fast early, then levels off |
| Decay | `\___` | floor + (a − floor) · e^(−x / k): falls fast early, then levels off |
| Late bloom | `___/` | a + (b − a) · (e^(x/k) − 1) / (e^(25/k) − 1): weak for a long time, then shoots up |
| S-curve | `__/‾‾` | low + (high − low) / (1 + e^(−(x − switch) / s)): switches from low to high around a level |

| Rock | Shape | Target | Why |
|---|---|---|---|
| Rock | Linear ╲ | 65 → 40 | Grandma's rock: amazing at the start, then it fades out |
| Lava | Bell | 45, peak 60 at level 5, width 4 | Rock turns into lava once the boltstone is found, so it takes over as Rock fades |
| Pebble | Decay | 62, down to 47 by about level 8, then flat | A great start, then just a pebble |
| Obsidian | Skewed bell | 45, peak 62 at level 5, slowly down to 45 | Found at level 2: peaks soon after, fades slowly |
| Giant Bolder | Bell | 45, peak 60 at level 10, width 5 | The mid-early workhorse |
| Copper | Bell | 45, peak 58 at level 5, width 3 | A short moment of glory |
| Charged Copper | Bell | 48, peak 61 at level 5, width 3 | Copper + 3 |
| Boltstone | Flat | 55 | The steady Nuker: shares of max HP do not care about level |
| Discombobulate Shard | Valley | 56, dips to 47 at level 10, width 5 | Confusion is strong early, its 8-round burn and poison late. The dip falls where the strikers peak |
| Uranium | Mesa | 45, up to 60 from level 10 to 20, down to 50 | A long middle reign |
| Rainbow Uranium | Mesa | 48, up to 63 from level 10 to 20, down to 53 | Uranium + 3 |
| Ice | Bell | 45, peak 58 at level 15, width 3 | Seasonal: the snow area |
| Water | Flat | 50 | The neutral reference |
| Salt Water | Logarithmic | 44 → 56 | Needs ice and saltstone; a stinging Trickster that settles above Water |
| Saltstone | Flat | 50 | The neutral reference |
| Coal | Flat | 47 | A leftover form, slightly weak on purpose |
| Sandstone | Logarithmic | 42 → 52 | Catches up quickly, then stays average |
| Meteorite | Logarithmic | 45 → 60 | The HP cost hurts early, then stops mattering |
| Healite | Logarithmic | 44 → 60 | Healing a share of your HP grows with longer fights |
| Charged Healite | Logarithmic | 47 → 63 | Healite + 3 |
| Glass | S-curve | 44 → 58, switching at level 15 | Turns on when Magma becomes available |
| Magma | S-curve | 42 → 62, switching at level 15 | Its burn grows with level; it turns on where it is found |
| Sunstone | Linear ╱ | 42 → 60 | The steady climber |
| Moonstone | Skewed bell | 43, peak 60 at level 20, down to 56 | A late rock that peaks where it is found |
| Singularity | Late bloom | 40 until about level 12, then up to 63 | Endgame power |
| Mirror Shard | Late bloom (steeper) | 40 → 65 | The last rock found, best at the very end |
