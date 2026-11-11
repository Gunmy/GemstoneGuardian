# Gemstone balancing — progression gates

The NPCs that block progress, in the order the player meets them. For each one:
which stone(s) the NPC fights with, its level (max HP = 100 + 20 x level), and every
gemstone the player can have picked up by the time they reach it. Stones only come
from chests (beaten NPCs do not drop their stones), so the "you have" list is the
chests that are reachable before the gate.

The goal: with the level filled in below, the player should only just beat the gate.

Source: `gameHandler` constructor (characters, battles, `chestAt(...).put(...)`).

---

- **Girl (Beginner)** — level 0 (100 HP) — uses: Rock
  - You have: Rock (Grandma's chest, with a healing potion)
  - Should be level 0 by meeting her

- **Keeper of Fire** (lava labyrinth, underground, through the secret path behind the tree) — level 2 (140 HP) — uses: Obsidian
  - You have: Rock
  - Behind him: Obsidian chest (+ healing potion)
  - Should be level 2 by meeting him

- **Knight** (castle entrance) — level 6 (220 HP) — uses: Giant Bolder
  - You have: Rock, Obsidian
  - Behind him: Giant Bolder chest (+ healing potion), castle 1st floor, and a healing pad / checkpoint at (10000.44, 9990.89)
  - XP fights before him: citizens in the park/village, level 3–4; sheep, level 1
  - Should be level 4 by meeting him

- **Paladin** (castle, 1st floor, guards the stairs up) — level 6 (220 HP) — uses: Boltstone
  - Wears: Feather of Hermes Boots (walks x1.75). Beating him gives: Feather of Hermes Boots
  - You have: Rock, Obsidian, Giant Bolder
  - Behind him: Boltstone + Copper chest (up the stairs, out the 2nd floor door, in the courtyard). Copper turns into Charged Copper when the Boltstone's lightning hits it
  - Should be level 4 by meeting him

- **King** (castle, 1st floor, guards the way out to the cave) — level 7 (240 HP) — uses: Boltstone
  - Wears: Heart of the Warrior (+10% max health, so 264 HP). Beating him gives: Heart of the Warrior
  - You have: Rock (or Lava), Obsidian, Giant Bolder, Boltstone, Copper (or Charged Copper)
  - Behind him: the cave, with the Uranium chest (+ healing potion) and a healing pad / checkpoint at (10002.44, 9986.33) just before Cornerstone
  - Should be level 5 by meeting him

- **Cornerstone** (blocks the cave exit) — level 8 (260 HP) — uses: Uranium
  - You have: Rock (or Lava), Obsidian, Giant Bolder, Boltstone, Copper (or Charged Copper), Uranium
  - Behind him: nothing new, only the way out of the cave
  - Should be level 6 by meeting him

- **Citizen Peter** (outside the cave, in front of the small house) — level 9 (280 HP) — uses: Pebble
  - XP fight, not a gate on its own, but you pass him before anything else outside the cave
  - You have: Rock (or Lava), Obsidian, Giant Bolder, Boltstone, Copper (or Charged Copper), Uranium
  - Behind him: Saltstone chest (+ healing potion) in the house with the door at (10002.44, 9983.67), and Pebble chest (+ healing potion) in the house at (10003, 9981). Saltstone turns Water into Salt Water once you have Ice
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
You have everything from the main game: Rock (or Lava), Obsidian, Boltstone, Copper, Giant Bolder, Uranium, Saltstone, Pebble, Discombobulate Shard, Sandstone (or Glass), Meteorite, Ice (or Water, or Salt Water), Healite, Magma, Coal. No new gems.

- **Ember Wraith** (volcano depths, XP fight) — level 17 (440 HP) — uses: Magma, Obsidian
  - In a side pocket of the caves: chest with a Healing Potion and a Cleansing Potion
  - Should be level \_\_\_ by meeting him

- **Ash Golem** (volcano gate) — level 18 (460 HP) — uses: Giant Bolder, Lava
  - Should be level \_\_\_ by meeting him

- **Dune Scorpion** (desert, XP fight) — level 18 (460 HP) — uses: Sandstone, Discombobulate Shard
  - Should be level \_\_\_ by meeting him

- **Desert Nomad** (desert gate) — level 19 (480 HP, wears Creatine: +10% damage) — uses: Saltstone, Glass
  - Should be level \_\_\_ by meeting him

- **Storm Harpy** (sky ruins, XP fight) — level 19 (480 HP) — uses: Boltstone, Copper
  - Should be level \_\_\_ by meeting her

- **Cloud Knight** (sky gate) — level 20 (500 HP, wears Feather of Hermes Boots) — uses: Meteorite, Healite
  - Should be level \_\_\_ by meeting him

- **Crystal Sentinel** (crystal caverns, XP fight) — level 20 (500 HP) — uses: Uranium, Ice
  - Should be level \_\_\_ by meeting it

- **Prism Warden** (crystal throne, last boss) — level 22 (594 HP with the Heart of the Warrior) — uses: Meteorite, Boltstone, Uranium, Healite
  - Beating it gives: the chest behind it, with the Elemental Rock and a Healing Potion
  - Should be level \_\_\_ by meeting it

Next to that chest a passage leads up into the open again: the castle grounds, with the crystal boy's castle a few chunks north. There is a healing pad on the plaza in front of it, and a chest with a Healing Potion and a Cleansing Potion hidden in the trees east of the meadow.

- **Crystal Boy** (castle hall, holds the girl; he kidnaps her right after she is beaten) — level 24 (580 HP) — uses: Ice, Uranium, Meteorite, Healite
  - Beating him frees the girl (she follows you from then on); he flees through the mirror behind him into the mirror dimension
  - In the mirror dimension, on the way to him: a **Stone Guardian** (a copy of the castle guards from the start) in the gap of the mirrored village wall, where the girl first stood — level 23 (560 HP) — uses: Giant Bolder, Meteorite, Healite; beaten, it crumbles and the way is open
  - Should be level \_\_\_ by meeting it
  - **Rematch** in the mirror dimension (the starting village mirrored), at the top of its path: the same Crystal Boy, healed — beating him there ends the game: you become him, and it begins again
  - Should be level \_\_\_ by meeting him

---

## Notes

- The player starts at level 0 (level = cube root of xp, rounded down).
- Rock turns into Lava when heated by Magma or struck by lightning twice (Boltstone), and back into Rock next to Ice or Obsidian. Lava keeps the uses left. Lava is hot: it melts Ice faster than one Ice or Obsidian can cool it, but Ice and Obsidian together cool it first.
- Charged Copper, Rainbow Uranium and Charged Healite are temporary states (a few seconds after a lightning hit), so they matter little in battle unless charged right before.
- Every XP-only NPC (citizens, sheep, crabs, Kalle, Gemstone Guard) fights with a Pebble.
