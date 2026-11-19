# Move animations

What every rock move shows in battle. For each move: the idea written from the move list alone (without looking at the animations then in the game), what it showed before, and which of the two it uses now. The new kinds of animation are in `battleFx.java` (each drawn in the tiles' own pixels: cracks, pools, crystals, waves, beams, bolts, columns, ice blocks, stretched and crumbling sprites...); the old particle kinds are still in `battleAnimation.java`.

**No rings of dots round a fighter**: no `crater` (the ring of square dots pressed in at the feet), `whirl` (arms of square blocks circling), `orbit`, `swirl`, `ring` or `ripple` in any move. **Shadows go under the fighters** (drawn behind them: `lob` and `drop` draw their shadow as a behind particle and the rock in front).

**Colours: one family per move, from its rock** (and red/pink for health, hearts). No mixed-hue dots: not burn, poison and confusion's colours together for "effects" (a cleanse peels `GRIME`, Reflection throws `MIRROR` glints), not a rock's move borrowing another element's colours. Rainbow colours (`RAINBOW`, `CONFETTI`) only for Rainbow Uranium.

Nothing grows or spreads out of the fighters' feet (no spikes, cubes, mountains, pillars or tile ripples) and no jagged lines are drawn on the ground (no cracks or maze): the owner found them ugly. The new flickering bolts were swapped back for the old lightning. Light pours down from the sky where a move needs it. Kept the old one (it was already a special picture of its own): Moonbeam and Sunbeam (a beam out of the sky), Eclipse (the moon falling out of space), Glint (a real mirror and the target's mirror image stepping out of it) and Collapse (the singularity drawn as in the inventory, with moonstones and meteorites pulled in).

## Rock

### Pebble Strike
- **Idea:** 0.0 A flicks a small grey pebble (0.3 T) in a low `lob` that bounces once on the ground before B, 0.6 s it hits B's shin, pinging off upward with a tiny `shardBurst` (4 grey bits). *Types:* `lob`, `shardBurst`. *Lucky:* the pebble ricochets back and forth between B and the ground 4 times before dropping.
- **Before:** generic particles: lunge, toss, impact, fountain, shake
- **Now:** the idea, built from: lob, shards, hit (lucky version adds: lob, shards)

### Stone Shatter
- **Idea:** 0.0 a 1.2 T grey rock rises above A (shakes 1 px), 0.5 s `lob` high to B, 0.9 s lands: big `shardBurst` (12 bits) + `crater` + `cameraKick shake`, a short `crackLine` (3 segments) runs out from the impact. *Types:* `lob`, `shardBurst`, `crater`, `crackLine`. *Lucky:* the crack runs all the way around B and the ground tile itself lifts and drops.
- **Before:** generic particles: lunge, toss, impact, fountain, quake, shake
- **Now:** the idea, built from: lob, shards, crater, hit, kick (lucky version adds: lob, shards, kick)

### Stone Renewal
- **Idea:** chips of grey stone (the ground's own rubble) slide in along the ground from 2 T around A and click onto A's outline one by one (6 chips, 0.15 s apart), A flashes light grey, the chips blink away. *Types:* `pixelText`-style snapping (cells fly in), `spriteTint`. *Lucky:* the chips build a whole grey shell around A (`encase`) that falls off as fresh stone.
- **Before:** generic particles: rise, swirl, orbit
- **Now:** the idea, built from: whirl, tint, drift (lucky version adds: encase, shards)

## Lava

### Molten Strike
- **Idea:** A hurls a glowing blob (`lob`, 0.4 T, orange, light core); it drips 1–2 px drops along the arc that land and sizzle; on hit a small `pool` (15 cells) spreads under B and `drift` embers rise from it for 1 s. *Types:* `lob`, `pool`, `drift`. *Lucky:* the blob splits mid-air into three blobs that hit B together and leave three joining pools.
- **Before:** generic particles: lunge, toss, stream, impact, rise, fountain, shake
- **Now:** the idea, built from: lob, pool, drift, hit (lucky version adds: lob, pool)

### Eruption
- **Idea:** 0.0 a glowing `crackLine glow` runs A→B, 0.5 s it opens wider (3 px) and steams, 0.8 s a `column` of lava (1 T wide, 3 T tall) bursts up under B with `shardBurst` of molten bits raining back down, `cameraKick shake`; in dark areas `darkness 0.5` + `glowLight flicker` at the column. *Types:* `crackLine`, `column`, `shardBurst`, `glowLight`. *Lucky:* three columns erupt in a line along the crack, the last one twice as tall.
- **Before:** generic particles: rise, meteor, rain, impact, fountain, quake, shake
- **Now:** the idea, built from: shards, pool, glow, kick, hit (lucky version adds: shards, pool)

### Cooling Crust
- **Idea:** a `pool` of lava forms under A and climbs it (`spriteTint` orange from the feet up); then from the edges inward the orange cells turn dark grey crust in steps, cracks show 1 px orange, the crust flakes off (`shardBurst` small, grey) leaving A healed. *Types:* `pool`, `spriteTint`, `shardBurst`. *Lucky:* the crust becomes Grandma's plain grey Rock texture, big, for a beat, before it crumbles.
- **Before:** generic particles: rise
- **Now:** the idea, built from: pool, tint, shards, drift (lucky version adds: encase, shards)

## Obsidian

### Heat Wave Slam
- **Idea:** A `dash`es with `afterimage` in dark purple; on contact a `rowShift` heat haze ripples out from B over a 3×3 T area (0.6 s) while a `tileRipple` (radius 3) pushes out, `drift` smoke curls up. *Types:* `dash`, `afterimage`, `rowShift`, `tileRipple`. *Lucky:* the haze covers the whole screen and B's sprite wobbles in it for a full second.
- **Before:** generic particles: lunge, toss, impact, fountain, quake, shake
- **Now:** the one from before (the new one looked worse in the game)

### Inferno Blast
- **Idea:** `darkness 0.7`; inside A a red-orange `glowLight` grows (A tinted black glass with a glowing core); 0.6 s A's glass cracks (`crystalGrow` of dark obsidian spikes outward, edges orange), 1.0 s a cone of 2 px fire blocks (`drift` sideways, fast) roars to B, B's `glowLight` flickers orange 1 s. *Types:* `darkness`, `glowLight`, `crystalGrow`, `drift`. *Lucky:* the obsidian spikes fly at B as glass shards (`shardBurst cone`) still glowing.
- **Before:** generic particles: beam, stream, rise, quake, shake
- **Now:** the idea, built from: glow, drift, tint (lucky version adds: shards)

### Warm Embrace
- **Idea:** dark obsidian `crystalGrow` spikes rise around A like a cupped hand (curving inward, not a ring: open on the side facing the camera), a warm orange `glowLight pulse` beats 4 times (one per turn of healing), spikes shrink back. *Types:* `crystalGrow`, `glowLight`. *Lucky:* the spikes close fully into a glass egg (`encase`, dark purple, translucent) that glows, then cracks open.
- **Before:** generic particles: orbit, rise
- **Now:** the idea, built from: whirl, glow, drift (lucky version adds: encase, shards)

## Boltstone

### Static Burst
- **Idea:** A's sprite crackles: 3 tiny `bolt`s 0.3 T long jump around A's outline (0.5 s), then a single short bolt leaps to B; B's hair-stand: B's sprite shifts up 1 px and jitters ±1 px for 0.5 s, small yellow `glowLight`. *Types:* `bolt`, `spriteTint`, `glowLight`. *Lucky:* the static leaps back and forth between A and B three times before staying on B.
- **Before:** generic particles: bolt, stream, impact, rise, fountain, shake
- **Now:** the idea, built from: tint, bolt, lift, drift, hit (lucky version adds: bolt)

### Thunderbolt
- **Idea:** `darkness 0.6` for 0.4 s, `shadowDrop` of a dark cloud blob (`cloud`, dark grey) over B, 0.6 s a `bolt` from 6 T above to B with 2 forks, `flash` white 2 frames, `crackLine glow` (yellow) spreads from B's feet, scorch `crater`. *Types:* `darkness`, `cloud`, `bolt`, `flash`, `crackLine`. *Lucky:* the bolt strikes three times on the same spot, each fork pattern different, the last with a full-screen flash.
- **Before:** generic particles: lunge, bolt, impact, quake, shake
- **Now:** the idea, built from: cloud, skybolt, flash, crater, hit, kick (lucky version adds: skybolt, flash)

### Electrocuter
- **Idea:** A raises up 2 px and a vertical `column` of yellow sparks feeds it; 0.5 s a thick `bolt` (4 px core) A→B that stays connected for 0.8 s re-jittering; B flashes between yellow tint and a black "skeleton" (sprite filled black) every 0.06 s; `cameraKick shake` throughout. *Types:* `column`, `bolt`, `spriteTint`, `cameraKick`. *Lucky:* the bolt `chain`s from B into the ground and out through 3 nearby tiles, each tile cracking with `crackLine glow`.
- **Before:** generic particles: bolt, skybolt, impact, orbit, fountain, quake, shake
- **Now:** the idea, built from: tint, bolt, kick, hit (lucky version adds: skybolt)

## Giant Bolder

### Bolder Smash
- **Idea:** A big brown boulder (1.5 T) rolls from A toward B, rotating in 45° steps, `cameraKick shake` small each step, rolling leaves a pressed `wetTrail`-like darker track on the ground; it hits B, B is pushed back 0.3 T (sprite moved) and slides back. *Types:* `lob` (ground roll: height 0), `cameraKick`, `crater`. *Lucky:* the boulder rolls over B, flattening B's sprite to 4 px tall for a beat, and B pops back up.
- **Before:** generic particles: lunge, roll, impact, fountain, quake, shake
- **Now:** the idea, built from: lob, drift, knock, crater, shards, kick, hit (lucky version adds: stretch)

### Crushing Blow
- **Idea:** `shadowDrop` of a huge boulder (shadow grows to 2 T) for 0.8 s, it lands on B (`cameraKick shake` hard, `tileRipple` radius 4, `crater`), boulder bounces off; B's sprite wobbles side to side (rotating ±10°) in steps as it reels. *Types:* `shadowDrop`, `tileRipple`, `crater`. *Lucky:* the boulder is far bigger than the screen; only its shadow fills the view before the crash and a full `flash` in brown.
- **Before:** generic particles: lunge, roll, impact, orbit, quake, shake
- **Now:** the idea, built from: drop, crater, kick, hit, stretch (lucky version adds: drop, flash)

### Mountainous Maul
- **Idea:** the ground under B rises: brown stepped peaks (`crystalGrow` with flat-topped mountain spikes, brown) lift B up 0.5 T and crush down; then `tether` of brown dust blobs travels B→A and A's sprite is tinted light brown (gaining). *Types:* `crystalGrow`, `tether`, `spriteTint`. *Lucky:* a whole mountain range of peaks rolls across the ground from A to B like a `wave`.
- **Before:** generic particles: meteor, impact, back, rise, fountain, quake, shake
- **Now:** the idea, built from: streak, crater, shards, kick, hit, drop, back, tint, drift (lucky version adds: streak, flash)

## Discombobulate Shard

### Mystic Mind Maze
- **Idea:** a maze is drawn on the ground around B: 1 px magenta lines laid out on a 4 px grid, growing outward from B's feet path-by-path (a random walk carve), 0.8 s complete (3×3 T); B's sprite walks 1 px steps left/right/up confusedly inside; maze walls rise 2 px and fade. *Types:* `crackLine` (grid mode, right-angle turns), `spriteTint`. *Lucky:* the maze flips (whole thing mirrored) twice, and B is at a different spot inside each time.
- **Before:** generic particles: toss, stream, orbit, impact
- **Now:** the idea, built from: lob, hit, twin, stretch, shake (lucky version adds: twin)

### Mirage Assault
- **Idea:** `rowShift` over the whole screen; three magenta `mirrorDouble`s of A step out and each `dash` at B from different sides with `afterimage`; only the last is real (the first two pass through B and burst into magenta pixels via `dissolve`); poison-purple and orange blotches stay on B (`spriteTint` blink). *Types:* `rowShift`, `mirrorDouble`, `dash`, `dissolve`. *Lucky:* seven doubles, attacking in sequence, the real one coming out of B's own shadow.
- **Before:** generic particles: stream, rise, fountain, shake
- **Now:** the idea, built from: twin, dash, hit, tint, drift (lucky version adds: twin)

### Mind Warp
- **Idea:** B's sprite is drawn with `stretch` in a sine twist (each 1 px slice offset by sine of row + time, so it corkscrews), a magenta `swirl` spins behind B, the camera pushes in and out 3 px rhythmically, the ground tiles around B swap places in pairs (two 1×1 tiles exchange, 4 swaps). *Types:* `stretch`, `swirl`, `cameraKick`. *Lucky:* the whole screen mirrors left-right for 0.5 s (A and B swap sides) and then snaps back.
- **Before:** generic particles: orbit, swirl, fountain, quake, shake
- **Now:** the idea, built from: stretch, whirl, kick (lucky version adds: whirl, stretch)

## Moonstone

### Moonbeam
- **Idea:** `darkness 0.75`; a pale moon disc (blocky circle, 1 T, moon colour, crater pixels) rises behind A 1 T up; 0.6 s a soft `beam` slants down from the moon onto B, B gets a cool `glowLight`; B's shadow on the ground turns and spins once around B. *Types:* `darkness`, `beam`, `glowLight`. *Lucky:* a full moon (3 T) fills the sky and the beam is 3x wider.
- **Before:** generic particles: skyBeam, shaft, rain, impact, shake
- **Now:** the one from before (it was the cooler one)

### Lunar Tide
- **Idea:** `darkness 0.6`; a moon disc above passes through its phases in 4 steps (shadow edge sweeps across in whole pixels); with each phase a silvery `wave` washes over B (confuse) and pulls back over A (heal, A's `spriteTint` silver). *Types:* `darkness`, `wave`, `spriteTint`. *Lucky:* the wave comes in from both sides at once and meets on B in a splash `column`.
- **Before:** generic particles: rain, swirl, orbit, shaft, rise
- **Now:** the idea, built from: disc (the moon drawn as the Moonstone item with a soft pulsing glow, not a drawn disc with phases: the owner found that ugly), wave, tint, drift (lucky version adds: wave)

### Eclipse
- **Idea:** the moon disc slides over a bright sun disc above B; as it covers it, `darkness` deepens to 0.9 in steps, leaving only a thin corona (1 px ring of moon colour around a black disc) and B lit by a dim rim; 1.5 s the corona `flash`es and the disc drops onto B (`shadowDrop`). *Types:* `darkness`, `glowLight`, `shadowDrop`, `flash`. *Lucky:* total eclipse: the world goes fully black except a moon-coloured outline of every sprite on screen.
- **Before:** generic particles: fromSpace, shaft, impact, orbit, quake, shake
- **Now:** the one from before (it was the cooler one)

## Sunstone

### Sunbeam
- **Idea:** a sun disc (blocky, sun colour, rays as 1 px lines that rotate in 22.5° steps) appears above A, `beam` angles from it to B, a burning spot (`pool` of sun colour, 10 cells) scorches B's tile and `drift` embers rise. *Types:* `beam`, `pool`, `drift`. *Lucky:* the beam is focused like a magnifying glass: a thin 1 px line that turns white-hot and sets a `crackLine glow` on fire around B.
- **Before:** generic particles: skyBeam, shaft, impact, rise, quake, shake
- **Now:** the one from before (it was the cooler one)

### Sunlight
- **Idea:** light breaks through: the screen tint warms (sun colour at alpha 0.15), a `column` of sun light (wide 1.5 T, alternating rows) falls on A from above, slow `drift` motes rise inside it; 5 short warm pulses (one per healing turn) of A's `spriteTint`. *Types:* `column`, `drift`, `spriteTint`. *Lucky:* the clouds part across the whole map: every tile on screen brightens in a sweep from left to right.
- **Before:** generic particles: rain, swirl, rise, orbit
- **Now:** the idea, built from: column, drift, tint (lucky version adds: flash)

### Daybreak
- **Idea:** `darkness 0.85` instantly; the sun disc rises from below the bottom edge of the view in 1 px steps behind A, its light sweeps across the ground from A outward (the darkness pulled back tile by tile like a dawn line); A's status blotches `peelOff` and burn away in the light; ends with a `flash` in sun colour. *Types:* `darkness`, `glowLight`, `peelOff`, `flash`. *Lucky:* the sun rises to the top of the screen and noon light bleaches every colour for a moment.
- **Before:** generic particles: shaft, rise, fountain
- **Now:** the idea, built from: disc (the sun drawn as the Sunstone item, not a drawn disc with rays: the owner found that ugly), peel, flash (lucky version adds: flash, a bigger disc)

## Singularity

### Gravity Well
- **Idea:** a black pixel dot appears 1 T in front of B and grows to a 6 px black disc with a 1 px singularity-colour rim; `swirl` pulls in ground pixels; `rowShift` bands bend toward it; B is dragged 0.4 T toward it (sprite moved) and slammed down 2 px; the dot pops with `tileRipple`. *Types:* `swirl`, `rowShift`, `tileRipple`. *Lucky:* the well also pulls in A's other loose bits: grass tufts from nearby tiles spiral in and vanish.
- **Before:** generic particles: lunge, toss, pull, impact, shake
- **Now:** the singularity with the real Moonstone and Meteorite items turning as they spiral in (`pullItems`), kick, hit. No lift (the owner found the target being lifted ugly) and no whirl of square blocks (lucky version: a wider pull)

### Spaghettify
- **Idea:** black hole forms behind A; B's sprite `stretch`es toward A in thin slices (1 px), the slices stream along a `tether` into A as blobs; B snaps back to shape with an overshoot while A glows singularity colour. *Types:* `stretch`, `tether`, `swirl`. *Lucky:* B becomes a single 1 px noodle all the way across the screen before snapping back.
- **Before:** generic particles: beam, back, singularity, shake, rise
- **Now:** `noodle`: bits of B (a few rows at a time) drawn out up to 0.9 T towards A and eased back while other bits go out, on a smooth noise over rows and time. It goes on without a break for as long as the move plays (it does not restart with the loop) and eases back into shape after. Then tint, drift (lucky version: more hearts; a second noodle would draw the sprite twice)

### Collapse
- **Idea:** A shrinks (sprite scaled to 70% in steps, the `costs`), the whole view's `darkness` 0.9 except the space between fighters; everything on screen (`drift` bits, ground tiles' highlight pixels) is pulled toward a point between them for 1 s, then silence (0.15 s still), then a huge white `flash` + `tileRipple` max radius + `cameraKick shake` hard and B thrown back. *Types:* `darkness`, `swirl`, `flash`, `tileRipple`, `cameraKick`. *Lucky:* after the flash the screen is inverted (every colour flipped) for 0.3 s.
- **Before:** generic particles: fromSpace, singularity, pull, impact, fountain, quake, shake
- **Now:** the one from before (it was the cooler one)

## Mirror Shard

### Glint
- **Idea:** a single bright 4-point star (cross of light pixels, arm length grows 1→6 px→0) appears on A, then at the same frame a thin 1 px `beam` of reflected light flicks to B and a second star blinks on B; B blinks white. Very fast, 0.8 s total. *Types:* `beam`, `spriteTint`. *Lucky:* the light bounces off 3 points on the ground (zigzag `beam`) before reaching B, a star at each bounce.
- **Before:** generic particles: mirrorCopy, stream, shake
- **Now:** the one from before (it was the cooler one)

### Mirror Image
- **Idea:** a pane of mirror (rectangle 1×1.5 T, pale blue, 2 diagonal glare lines) rises from the ground beside A; A's `mirrorDouble` steps out of it and both copies swap places twice; B's sprite turns left-right confusedly; A healed (pale tint). *Types:* `mirrorDouble`, `encase` (as standing pane), `spriteTint`. *Lucky:* the pane folds into two, then four, leaving four copies of A that blink out one by one.
- **Before:** generic particles: orbit, swirl, rise
- **Now:** the idea, built from: twin, tint, stretch, drift (lucky version adds: twin)

### Reflection
- **Idea:** a mirror pane rises in front of A as B's last status (or an incoming dark bolt from B) travels toward A, hits the pane and comes back along the same line to B, now in mirror colour; pane `shatter`s (`shardBurst`, pale blue). *Types:* `encase`, `beam`, `shardBurst`. *Lucky:* the reflected shot hits a second mirror behind B and B sees itself: B's own `mirrorDouble` slaps it.
- **Before:** generic particles: rise, stream, impact, shake
- **Now:** the idea, built from: ray, shards, drift, hit (lucky version adds: twin, hit)

## Meteorite

### Gravitational Slam
- **Idea:** B is lifted 0.5 T off the ground (sprite moved up with a shrinking shadow), hangs 0.3 s, and is slammed back down hard: `cameraKick push` down, `crater`, short `tileRipple`. *Types:* `shadowDrop` (shadow only), `crater`, `tileRipple`. *Lucky:* B is lifted out of the top of the view and comes back down as a `meteorStreak`.
- **Before:** generic particles: fromSpace, impact, orbit, fountain, quake, shake
- **Now:** changed by the owner (lifting them did not make sense): three meteorites sent down from space onto B one after another (0.15 s apart, landing at 0.45, 0.6 and 0.75 s, the last a bit bigger), each with a hit and a few cosmic shards, a camera kick on the last. Built from: streak, hit, shards, kick (lucky version adds: two more streaks, one pale purple)

### Cosmic Crush
- **Idea:** `darkness 0.6`; a few stars (single light pixels) appear; A glows and its sprite drops 1 px (cost); a `meteorStreak` with blue trail falls from the top corner onto B after a growing `shadowDrop`; impact: `crater`, `shardBurst` blue, `flash`. *Types:* `darkness`, `meteorStreak`, `shadowDrop`, `crater`, `flash`. *Lucky:* a shower: 4 smaller meteors land in a pattern around B, then the big one.
- **Before:** generic particles: fromSpace, skybolt, impact, orbit, quake, shake
- **Now:** the idea, built from: tint, streak, crater, shards, flash, skybolt, hit (lucky version adds: streak)

### Orbitin Assault
- **Idea:** 5 meteorite textures rise from A (A's sprite flashes as they tear out: cost) into an `orbit` around A that speeds up in 1 s, the orbit widens until it passes over B, then all `release` into B one after another 0.08 s apart, each with a small `crater`; final `cameraKick shake`. *Types:* `orbit`, `meteorStreak`, `crater`, `cameraKick`. *Lucky:* the orbit becomes a ring of 12 rocks around both fighters before they all crash inward.
- **Before:** generic particles: orbit, fromSpace, impact, fountain, quake, shake
- **Now:** the idea, built from: tint, spin, hit, crater, kick, shards (lucky version adds: spin)

## Magma

### Molten Fury
- **Idea:** the ground under B glows: a `crackLine glow` web (3 cracks from B's feet) splits open, magma bubbles up as `pool` (25 cells, red-orange, darker crust edge cells), `drift` embers; B's sprite tinted orange blinking. *Types:* `crackLine`, `pool`, `drift`, `spriteTint`. *Lucky:* the pool bubbles: 3 bubbles (blocky circles) swell and pop in a `shardBurst` of molten drops.
- **Before:** generic particles: lunge, beam, stream, rise, fountain, shake
- **Now:** the idea, built from: pool, drift, tint (lucky version adds: shards)

### Magma Blast
- **Idea:** A spits a fat stream: a `beam` with wobble, red-orange, that arcs (curved like a hose: drawn as 6 joined segments along a parabola), splashing on B into a big `pool`; `glowLight` in dark. *Types:* `beam`, `pool`, `glowLight`. *Lucky:* the stream sweeps across B in a Z pattern leaving a burning Z on the ground.
- **Before:** generic particles: lunge, toss, impact, rise, fountain, quake, shake
- **Now:** the idea, built from: ray, pool, hit, drift (lucky version adds: ray)

### Volcanic Impact
- **Idea:** a magma bomb rockets up from A out of view (`lob` with huge peak), `shadowDrop` grows over B with a red tinge, `darkness 0.5`, the bomb slams down: `tileRipple`, `crater` filled with `pool`, `crackLine glow` x4 radiating, `cameraKick shake`, ember `drift` 1 s. *Types:* `lob`, `shadowDrop`, `tileRipple`, `pool`, `crackLine`, `glowLight`. *Lucky:* the ground around B becomes a small volcano: a brown cone (stepped) pushes up from the ground and erupts.
- **Before:** generic particles: rise, meteor, rain, fountain, quake, shake
- **Now:** the idea, built from: drop, crater, shards, flash, kick, hit, drift (lucky version adds: drop, shards, kick)

## Uranium

### Gamma Burst
- **Idea:** `darkness 0.5`; A glows green (`glowLight pulse` twice, faster); then 3 thin straight `beam`s (1 px, green) shoot through B at slightly different angles, each extending past B to the edge of the view; B's sprite flickers between normal and a green negative (`spriteTint`). *Types:* `glowLight`, `beam`, `spriteTint`. *Lucky:* the beams pass through B and reveal a bright green X-ray outline of B for 0.5 s.
- **Before:** generic particles: beam, bolt, impact, fountain, orbit, quake, shake
- **Now:** the one from before (the new one looked worse in the game)

### Atomic Blast
- **Idea:** A's glow gathers into a 2 px core above A; 0.5 s the core splits (two cores, then four) in steps; 0.8 s they detonate between A and B: a green `flash`, a `tileRipple` radius 5, a mushroom cloud (`cloud` with a `column` stalk, green-grey) rises 2 T; A is tinted green softly (healing), B tinted orange-green. *Types:* `glowLight`, `flash`, `tileRipple`, `cloud`, `column`. *Lucky:* the cloud's glow lingers: the whole screen stays faint green until the next turn's message.
- **Before:** generic particles: impact, rise, fountain, quake, shake
- **Now:** the one from before (the new one looked worse in the game)

### Fission Wave
- **Idea:** A splits for a moment: two A sprites side by side (1 px apart, then back), a pulsing green `wave` rolls from A to B over the ground leaving glowing green dots on tiles (`wetTrail` in green that stays 1 s); a few dots land on A too (self-poison). *Types:* `mirrorDouble`, `wave`, `glowLight`. *Lucky:* the wave splits as it travels, again and again, into 4 smaller waves that hit B from four angles.
- **Before:** generic particles: stream, bolt, rise, fountain, shake
- **Now:** the idea, built from: twin, wave, tint, drift (lucky version adds: wave)

## Rainbow Uranium

### Gamma Burst
- **Idea:** as Uranium, but 5 beams, each a different rainbow step, fanning; B's tint cycles the six hues. *Types:* `glowLight`, `beam`, `spriteTint`. *Lucky:* the beams bend into a rainbow arc over B that falls on B.
- **Before:** generic particles: beam, bolt, impact, fountain, orbit, quake, shake
- **Now:** the one from before (the new one looked worse in the game)

### Atomic Blast
- **Idea:** as Uranium, but the mushroom cloud is banded in rainbow rows that scroll upward, `tileRipple` tints each ring a different hue. *Types:* `glowLight`, `flash`, `tileRipple`, `cloud`, `column`. *Lucky:* a falling star (`meteorStreak` with rainbow trail) ignites the core instead of A.
- **Before:** generic particles: impact, rise, fountain, quake, shake
- **Now:** the one from before (the new one looked worse in the game)

### Fission Wave
- **Idea:** as Uranium, the wave's crest is striped in rainbow and its trail dots each a different hue. *Types:* `mirrorDouble`, `wave`, `glowLight`. *Lucky:* the wave reflects back from the edge of the view and hits B a second time from behind.
- **Before:** generic particles: stream, bolt, rise, fountain, shake
- **Now:** the idea, built from: twin, wave, tints, drift (lucky version adds: wave)

## Copper

### Conduct
- **Idea:** a copper wire (1 px line, copper with light highlight every 3 px) shoots out of A along the ground to B in a zigzag that snaps straight like a pulled string; a single light pulse (3 px block) races along it; B blinks. *Types:* `tether`, `spriteTint`. *Lucky:* the wire coils around B 3 times (spiral drawn in steps) before the pulse.
- **Before:** generic particles: lunge, toss, bolt, impact, fountain, shake
- **Now:** the idea, built from: lob, bolt, tint, hit (lucky version adds: shards, lob)

### Short Circuit
- **Idea:** the copper wire goes to B, the pulse runs, then the wire frays in the middle: a small `bolt` arcs across the gap, sparks `shardBurst` (yellow-copper), the wire whips wildly (re-drawn with random sag every frame), B jitters. *Types:* `tether`, `bolt`, `shardBurst`. *Lucky:* the short blows a "fuse": the whole screen goes black for 0.2 s (`darkness 1.0`) and flickers back.
- **Before:** generic particles: bolt, impact, orbit, quake, shake
- **Now:** the one from before (the owner liked its lightning better than the new wire)

### Grounding
- **Idea:** A's status blotches drain down A's body (`peelOff` but falling) into a copper spike A pushes into the ground; a `crackLine` of copper lines spreads into the ground and fades; A's sprite gets a light copper tint. *Types:* `peelOff`, `crackLine`, `spriteTint`. *Lucky:* the grounding lines light up every tile edge in a 3 T radius like a circuit board.
- **Before:** generic particles: rise, fountain
- **Now:** the idea, built from: peel, drift, tint

## Charged Copper

### Conduct
- **Idea:** as Copper, but the wire crackles: tiny `bolt` jitter along its length, and 3 pulses instead of 1. *Types:* `tether`, `bolt`, `spriteTint`. *Lucky:* the wire becomes a straight `bolt` that stays on for 1 s.
- **Before:** generic particles: lunge, toss, bolt, impact, fountain, shake
- **Now:** the idea, built from: lob, bolt, tint, hit (lucky version adds: bolt)

### Short Circuit
- **Idea:** as Copper, but the fray becomes a `bolt` with 3 forks, plus `flash` and `glowLight` in dark. *Types:* `tether`, `bolt`, `shardBurst`, `flash`. *Lucky:* the whole wire explodes in a `chain` bolt across nearby tiles.
- **Before:** generic particles: bolt, impact, orbit, quake, shake
- **Now:** the one from before (the owner liked its lightning better than the new wire)

### Grounding
- **Idea:** as Copper, plus the cleansed bad stuff exits as `bolt`s down the ground cracks, which glow `glowLight` copper. *Types:* `peelOff`, `crackLine glow`, `bolt`. *Lucky:* the circuit board pattern covers the whole visible ground and pulses once.
- **Before:** generic particles: rise, orbit
- **Now:** the idea, built from: peel, drift, bolt, tint

## Sandstone

### Sandblast
- **Idea:** a cone of sand: `drift` grains fired sideways from A (fast, 2 px, sand colour + 1 shade darker), filling a widening cone; B's sprite gets "sanded": its edge pixels blink to sand colour in the direction of the blast. *Types:* `drift`, `spriteTint`. *Lucky:* the grains spell out a giant word in sand (`pixelText`, e.g. "OW") that blows away.
- **Before:** generic particles: lunge, beam, stream, impact, fountain, shake
- **Now:** redone from scratch (the cloud of big blocks was a mess): A throws a clump of packed sand (`lob`) that bursts into sand bits (`shards`) and a little heap (`pool`) under B, B flashes sand, hit. Lucky: a second clump

### Sandstorm
- **Idea:** a `cloud` of sand squares rolls in from the side of the view and swallows B, churning; `rowShift` inside it, B's sprite only visible every other frame; grains `drift` sideways across the whole screen. *Types:* `cloud`, `rowShift`, `drift`. *Lucky:* when the storm clears, small sand dunes (stepped bumps) have piled up around B.
- **Before:** generic particles: swirl, orbit, rise, shake
- **Now:** redone from scratch: sand streams from A at B (`drift` TOWARD) while B flashes sand and reels (`stretch` TWIST), sand piling up under B (`pool`). Lucky: a second stream and a bigger heap

### Erosion
- **Idea:** B's outline loses pixels: every 0.1 s some of B's edge pixels turn into sand grains that fall down (`dissolve` downward, slow), a small sand heap grows under B; then the pixels return. *Types:* `dissolve`, `drift`. *Lucky:* B is eroded down to half height before reforming.
- **Before:** generic particles: swirl, rise
- **Now:** redone from scratch: B is sanded away: its colour sweeps to sand (`tint` sweep), grains flake off in two bursts (`shards`), a heap grows under it (`pool`). Lucky: a third burst and a bigger heap

## Glass

### Shard Spray
- **Idea:** A throws a glass pane (1 T) that breaks mid-air into a `shardBurst cone` of 12 triangular glass shards flying at B; each catches light (one random shard shows a 1 px white glint per frame); they stick in the ground around B and blink out. *Types:* `lob`, `shardBurst`. *Lucky:* the shards fall back together into the pane behind B and smash again.
- **Before:** generic particles: lunge, stream, impact, fountain, shake
- **Now:** the idea, built from: lob, shards, hit (lucky version adds: shards, hit)

### Glare
- **Idea:** a glass lens (blocky circle, white, 1 px edge) above A focuses: light lines converge on it, then a wide white `beam` opens on B; `flash` white; B's sprite washed to near white and blinks for 1 s. *Types:* `beam`, `flash`, `spriteTint`. *Lucky:* the glare blinds the screen: full white `flash` held for 0.3 s and fading in 3 steps.
- **Before:** generic particles: beam, impact, orbit, rise, quake
- **Now:** the idea, built from: ray, flash, tint, stretch (lucky version adds: flash)

### Clear Through
- **Idea:** A becomes glass: `spriteTint` pale white at alpha 0.6 so the ground shows through; the status blotches inside it are visible and fall through A's body to the ground (`peelOff` downward) and shatter as tiny dark bits; A returns to normal. *Types:* `spriteTint`, `peelOff`, `shardBurst`. *Lucky:* A stays see-through and a single sparkle crawls along A's edge outline.
- **Before:** generic particles: rise, swirl
- **Now:** the idea, built from: tint, peel, shards (lucky version adds: drift)

## Ice

### Ice Shard
- **Idea:** `crystalGrow` of ice spikes grows from A (0.5 s), the biggest one breaks off and flies at B (`lob` low, spinning), hits with `shardBurst` of ice; small `frostCreep` patch where it struck. *Types:* `crystalGrow`, `lob`, `shardBurst`, `frostCreep`. *Lucky:* every spike breaks off and they fly one after the other.
- **Before:** generic particles: lunge, stream, toss, impact, fountain, shake
- **Now:** the idea, built from: tint, lob, shards, pool, hit (lucky version adds: lob)

### Freeze
- **Idea:** `frostCreep` spreads from A across the ground to B (0.8 s), climbs B's sprite from the feet up, then an `encase` ice block snaps shut around B; holds 0.5 s; cracks lines appear on it (1 px). *Types:* `frostCreep`, `encase`. *Lucky:* the frost freezes every tile on screen white, then thaws.
- **Before:** generic particles: beam, rain, swirl, orbit, quake, shake
- **Now:** A throws the ice (`lob`) and the ice block (`encase`) closes over B where it hits, then breaks into shards; looped. No frost path along the ground (the owner did not like it). Built from: lob, tint, encase, hit, shards (lucky version adds: drift of snow)

### Frostbite
- **Idea:** thin ice "teeth" (`crystalGrow` spikes pointing inward, from above and below B, like a jaw) snap shut on B; B tinted blue, blinks; frost pixels linger on B's edges. *Types:* `crystalGrow`, `spriteTint`, `frostCreep`. *Lucky:* the jaw bites twice and leaves a frost patch in its shape.
- **Before:** generic particles: rain, rise, impact
- **Now:** the idea, built from: crystals, tint, hit, pool (lucky version adds: crystals, hit)

## Water

### Splash
- **Idea:** A stamps; a `wave` (2 T wide) rushes along the ground to B and breaks over B with a `shardBurst` of droplets (blue, round 2 px) and `wetTrail` behind. *Types:* `wave`, `shardBurst`. *Lucky:* the wave is taller than B and B's sprite is hidden in it for 0.3 s.
- **Before:** generic particles: lunge, stream, impact, fountain, shake
- **Now:** the idea, built from: wave, shards, hit (lucky version adds: wave, hide)

### Soak
- **Idea:** a raindrop `drift` (falling, blue, 1×3 px) over A only; A's status blotches wash off (`peelOff` falling) and run off as tiny streams into a `pool` under A that soaks into the ground. *Types:* `drift`, `peelOff`, `pool`. *Lucky:* a single big drop falls with a `shadowDrop`, splashes A clean.
- **Before:** generic particles: rain, rise, fountain
- **Now:** the idea, built from: drift, peel, pool (lucky version adds: drop, shards)

### Tide
- **Idea:** water flows in from the screen edge behind A as a `wave`, rises around A's feet as a `pool` (40 cells), A rocks gently (1 px up/down); the water pulls back out, leaving a `wetTrail`. *Types:* `wave`, `pool`. *Lucky:* a high tide: the whole bottom third of the view fills with water.
- **Before:** generic particles: swirl, rise
- **Now:** the idea, built from: wave, pool, tint, drift (lucky version adds: wave)

## Salt Water

### Brine Splash
- **Idea:** as Splash, but the droplets leave white salt pixels where they land, and B's eyes area blinks white (sting); `spriteTint` B turns briefly while confused. *Types:* `wave`, `shardBurst`, `spriteTint`. *Lucky:* the wave carries a spark: a small `bolt` runs along its crest.
- **Before:** generic particles: lunge, stream, impact, orbit, fountain, shake
- **Now:** the idea, built from: wave, shards, tint, stretch, hit (lucky version adds: bolt)

### Soak
- **Idea:** as Water's Soak, but when the pool dries it leaves a white salt crust (pool cells turned off-white, shrinking). *Types:* `drift`, `peelOff`, `pool`. *Lucky:* the salt crust forms a small salt crystal next to A (`crystalGrow`, white).
- **Before:** generic particles: rain, rise, fountain
- **Now:** the idea, built from: drift, peel, pool (lucky version adds: drift)

### Tide
- **Idea:** as Water's Tide, but the pulling back water leaves a white salt line where the high mark was. *Types:* `wave`, `pool`. *Lucky:* as Water's high tide, with foam (white pixels) on the crest.
- **Before:** generic particles: swirl, rise
- **Now:** the idea, built from: wave, pool, tint, drift (lucky version adds: wave)

## Saltstone

### Salt Spray
- **Idea:** A flicks a fan of salt grains (`drift` fast, cone, white 1–2 px cubes), B's sprite gets white flecks stuck on its edges that blink off one by one. *Types:* `drift`, `spriteTint`. *Lucky:* the grains form white salt crystals (`crystalGrow` small cubes) on the ground around B.
- **Before:** generic particles: lunge, stream, impact, fountain, shake
- **Now:** the idea, built from: drift, tint, hit (lucky version adds: drift, hit)

### Brine Sting
- **Idea:** small cubic salt crystals (`crystalGrow` with square shapes, step-built) grow out of the ground under B and prick up into B; B blinks purple (poison). *Types:* `crystalGrow`, `spriteTint`. *Lucky:* the crystals grow into a whole salt pillar (`column`, white) carrying B up 0.5 T.
- **Before:** generic particles: toss, stream, rise, impact, shake
- **Now:** the idea, built from: lob, shards, tint, hit, drift (lucky version adds: lob, hit)

### Preserve
- **Idea:** salt crust (`encase`, off-white, bumpy edge pixels) builds over A from the feet up, A's status blotches dim inside it, crust cracks off (`shardBurst`, white cubes) and A is fresh. *Types:* `encase`, `peelOff`, `shardBurst`. *Lucky:* the crust is a perfect salt cube that rests a moment, then falls apart into a little salt heap.
- **Before:** generic particles: swirl, rise
- **Now:** the idea, built from: encase, shards, drift (lucky version adds: shards)

## Coal

### Coal Chunk
- **Idea:** A throws a black lump (`lob`, 0.6 T, edge pixels dark grey) that hits B with a `shardBurst` of black dust and a smudge `crater` on the ground; B's sprite has a black smudge for 0.5 s. *Types:* `lob`, `shardBurst`, `crater`, `spriteTint`. *Lucky:* the lump hits B, B is entirely black for a beat with only white eyes blinking.
- **Before:** generic particles: lunge, toss, impact, rise, fountain, quake, shake
- **Now:** the idea, built from: lob, shards, crater, tint, hit (lucky version adds: shards, lob)

### Soot Cloud
- **Idea:** a black `cloud` puffs out of A and drifts onto B, churning; inside, B is hidden; `drift` soot falls from it; when it leaves, B coughs (shakes ±1 px 3 times). *Types:* `cloud`, `drift`. *Lucky:* the cloud spreads over the whole view (`darkness 0.8`), with only B's eyes visible.
- **Before:** generic particles: swirl, rise, orbit
- **Now:** the idea, built from: cloud, hide, drift, knock (lucky version adds: cloud)

### Rekindle
- **Idea:** `darkness 0.7`; the coal's dark core glows: 3 small red `glowLight`s light up inside A one by one, then a single ember `lob`s to B and sets B's tile glowing with `drift` sparks rising. *Types:* `darkness`, `glowLight`, `lob`, `drift`. *Lucky:* the ember turns B's whole tile into a small coal bed (`pool` of glowing red cells).
- **Before:** generic particles: toss, stream, rise, fountain
- **Now:** the idea, built from: glow, lob, pool, drift, tint, hit (lucky version adds: pool)

## Healite

### Revitalize
- **Idea:** a pink `column` rises from A's tile, healing `drift` motes rise inside it; A's sprite is redrawn from the feet up in a pink-tinted sweep row by row (as if refreshed), then a soft pink `glowLight pulse`. *Types:* `column`, `drift`, `spriteTint`. *Lucky:* flowers bloom around A: 4 pixel flowers (plus-shaped, pink) grow on nearby tiles and stay until the end of the move.
- **Before:** generic particles: rise, swirl, orbit, fountain
- **Now:** the idea, built from: whirl, drift, tint, glow (lucky version adds: drift)

### Ressurection
- **Idea:** A `dissolve`s into pink dots that drift up, `darkness 0.6`, the dots gather and `reform` A one pixel at a time, a `tether` pulls pink blobs from B into A at the same time; A's status blotches left behind on the ground as a dark shape that crumbles. *Types:* `dissolve`, `darkness`, `tether`, `peelOff`. *Lucky:* the reform is slow and leaves a pink outline of A in the air for 1 s afterwards.
- **Before:** generic particles: rise, swirl, fountain
- **Now:** the idea, built from: dissolve, back, tint, peel (lucky version adds: twin)

### Transcendence
- **Idea:** `darkness 0.8`; A rises 0.5 T with a pink `glowLight` beneath; a tall `column` of pink light (to the top of the view) flows down onto A; `flash` pink; A lowers down healed with an even pink tint fading. *Types:* `darkness`, `glowLight`, `column`, `flash`. *Lucky:* the light column widens over the whole view and the dark lifts all at once.
- **Before:** generic particles: orbit, rise, fountain
- **Now:** the idea, built from: lift, glow, column, flash (lucky version adds: flash)

## Charged Healite

### Revitalize
- **Idea:** as Healite, the column in violet, motes are 4-point stars. *Types:* `column`, `drift`, `spriteTint`. *Lucky:* the column reaches the sky and a star falls (`meteorStreak`, violet) onto A.
- **Before:** generic particles: rise, swirl, orbit, fountain
- **Now:** the idea, built from: whirl, drift, tint, glow (lucky version adds: streak)

### Ressurection
- **Idea:** as Healite, the dots are white and violet and gather with a `swirl`. *Types:* `dissolve`, `darkness`, `swirl`, `tether`. *Lucky:* the dots form a constellation (lines between them) before reforming A.
- **Before:** generic particles: rise, swirl, fountain
- **Now:** the idea, built from: dissolve, whirl, back, tint, peel (lucky version adds: twin)

### Transcendence
- **Idea:** as Healite, plus the dark sky fills with blinking stars and a `tileRipple` of violet light spreads from A when it lands. *Types:* `darkness`, `glowLight`, `column`, `flash`, `tileRipple`. *Lucky:* the stars all streak down into A at once (many short `meteorStreak`s).
- **Before:** generic particles: orbit, rise, fountain
- **Now:** the idea, built from: lift, glow, column, flash, drift (lucky version adds: streak)

## Pebble

### Stone Skip
- **Idea:** A side-arms a flat pebble that `lob skips` along the ground 3 times, each skip leaving a tiny `tileRipple` (radius 1) — on water tiles a splash — and hits B. *Types:* `lob`, `tileRipple`. *Lucky:* 7 skips, the last one curving around B and hitting from behind.
- **Before:** generic particles: lunge, skip, impact, fountain, shake
- **Now:** the idea, built from: lob, shards, hit (lucky version adds: lob, hit)

### Rock Remedy
- **Idea:** the pebble is heated: it glows in A's hand (grey then orange in steps), A tosses it (`lob`) and it lands on B with a hiss and a small `drift` of steam/embers; B tinted orange. *Types:* `lob`, `drift`, `spriteTint`. *Lucky:* the pebble skips 2 times (hissing on each touch) before hitting.
- **Before:** generic particles: lunge, toss, impact, rise, fountain, shake
- **Now:** the idea, built from: tint, lob, drift, hit (lucky version adds: lob, hit)

### Mineral Mend
- **Idea:** small grey pebbles roll toward A from all over the visible ground (`lob` height 0, 6 pebbles), stacking up into a little cairn beside A; A tinted light grey; the cairn collapses (`shardBurst`). *Types:* `lob`, `spriteTint`, `shardBurst`. *Lucky:* the cairn is taller than A and wobbles before falling.
- **Before:** generic particles: rise, swirl, orbit
- **Now:** the idea, built from: spin, tint, drift (lucky version adds: spin)
