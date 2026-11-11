package game;

public class gemstones {
    private gameHandler handler;
    public gemstones (gameHandler handler) {
        this.handler = handler;
    }

    public gemstone rock () {
        gemstone stone = new gemstone("Rock", "#9A9A9A", itemParticles.NONE, handler, 
        "Pebble Strike", abilityType.ATTACK, "Damage: 13 + lvl", (c1, c2) -> {c2.takeDmg(13 + c1.getLevel());}, 10, 1,
        "Stone Shatter", abilityType.ATTACK, "Damage: 26 + 2 x lvl", (c1, c2) -> {c2.takeDmg(26  + c1.getLevel()*2);}, 5, 1,
        "Stone Renewal", abilityType.HEAL, "Heals: 20 + 10% of maxHP", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.1 + 20);}, 3, 1
        );
        stone.lore("A plain grey stone your grandma kept in a drawer for sixty years. Reliable, if nothing else.");
        //Heated by magma, or by lightning (two strikes, like ice melting), it melts into lava. Its uses left go with it
        stone.transformsInto(itemInfluence.HEAT, 1, "#FF6A20", () -> lava()).heatedBy(itemInfluence.LIGHTNING, 1).carriesState();
        return stone;
    }

    //Grandma's rock melted by magma or lightning: the same three moves, burning. Same uses as the rock, so the uses
    //left carry over both ways. It drips and pours like water does, in orange.
    //It is hot: it melts ice (and turns sand into glass) like magma does. Ice or obsidian cools it back into rock, but
    //one cold stone alone is slower at it (LAVA_COOL_DOSE at their 0.8 against lava's 1.0 heat) than the lava is at
    //melting ice, so ice next to lava melts first. Ice and obsidian together cool it before the ice is gone
    public static final double LAVA_COOL_DOSE = 1.4;

    public gemstone lava () {
        gemstone stone = new gemstone("Lava", "#FF6A20", itemParticles.LAVA, handler,
        "Molten Strike", abilityType.FIRE, "Damage: 13 + lvl. Burn: 3 per turn, 3 turns", (c1, c2) -> {c2.takeDmg(13 + c1.getLevel()); c2.addFireDamage(3, 3);}, 10, 1,
        "Eruption", abilityType.FIRE, "Damage: 22 + 2 x lvl. Burn: 5 per turn, 3 turns", (c1, c2) -> {c2.takeDmg(22 + c1.getLevel()*2); c2.addFireDamage(5, 3);}, 5, 0.9,
        "Cooling Crust", abilityType.HEAL, "Heals: 20 + 10% of maxHP", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.1 + 20);}, 3, 1
        );
        stone.lore("Grandma's rock, molten. It will set again the moment something cold comes near.");
        stone.transformsInto(itemInfluence.COLD, LAVA_COOL_DOSE, "#9A9A9A", () -> rock()).carriesState();
        stone.emits(itemInfluence.HEAT, 2.0, 1.0);
        stone.trail("#FF6A20", 16, 0.6);
        return stone;
    }

    public gemstone obsidian () {
        gemstone stone = new gemstone("Obsidian", "#5A4A6A", itemParticles.SMOKE, handler,
        "Heat Wave Slam", abilityType.ATTACK, "Damage: 10 x (1 + lvl/3)", (c1, c2) -> {c2.takeDmg(10*(1+c1.getLevel()/3));}, 8, 0.95,
        "Inferno Blast", abilityType.FIRE, "Burn: 5 x (1 + lvl/3) per turn, 5 turns", (c1, c2) -> {c2.addFireDamage(5*(1+c1.getLevel()/3), 5);}, 2, 0.95,
        "Warm Embrace", abilityType.HEAL, "Heals: (5% + lvl/150) of maxHP per turn, 4 turns", (c1, c2) -> {c1.addHealing(c1.getMaxHealth()*(0.05+c1.getLevel()/150), 4);}, 3, 0.95
        );
        stone.lore("Volcanic glass, still warm to the touch. It remembers the fire it came from.");
        //Near magma it heats up and smokes. It cools whatever is next to it: glass back into sandstone, water into ice
        stone.shakeParticles(5).reactsTo(itemInfluence.HEAT, 1).emits(itemInfluence.COLD, 2.0, 0.8);
        return stone;
    }

    public gemstone boltstone () {
        gemstone stone = new gemstone("Boltstone", "#F0D040", itemParticles.SPARKLE, handler,
        "Static Burst", abilityType.FIRE, "Damage: 8 x (1 + lvl/4). Burn: 5 per turn, 3 turns", (c1, c2) -> {c2.addFireDamage(5, 3); c2.takeDmg(8*(1+c1.getLevel()/4));}, 8, 1,
        "Thunderbolt", abilityType.LIGHTNING, "Damage: 15% of their maxHP", (c1, c2) -> {c2.takeDmg(c2.getMaxHealth()*0.15);}, 3, 0.8,
        "Electrocuter", abilityType.LIGHTNING, "Damage: 40% of their maxHP", (c1, c2) -> {c2.takeDmg(c2.getMaxHealth()*0.4);}, 1, 0.5
        );
        stone.lore("Struck by lightning so many times it started striking back.");
        stone.shakeParticles(1).shakeLightning(0.6)
            .reactsTo(itemInfluence.RADIATION, 1).reactsTo(itemInfluence.LIGHTNING, 0.8).reactsTo(itemInfluence.CONDUCTION, 1);
        return stone;
    }

    public gemstone giantBolder () {
        gemstone stone = new gemstone("Giant Bolder", "#8A6A4A", itemParticles.NONE, handler, 
        "Bolder Smash", abilityType.ATTACK, "Damage: 7 x (1 + lvl/3)", (c1, c2) -> {c2.takeDmg(7*(1+c1.getLevel()/3));}, 6, 1,
        "Crushing Blow", abilityType.CONFUSE, "Damage: 10 + lvl. Confusion: 20% for 6 turns", (c1, c2) -> {c2.takeDmg(10 + c1.getLevel()); c2.addConfusion(0.2, 6);}, 5, 0.9,
        "Mountainous Maul", abilityType.DRAIN, "Damage: 20% of their maxHP, and heals you the same", (c1, c2) -> {c1.heal(c2.getMaxHealth()*0.2); c2.takeDmg(c2.getMaxHealth()*0.2);}, 1, 0.6
        );
        stone.lore("Not actually giant. The name was chosen by someone who had just been hit by it.");
        return stone;
    }

    
    public gemstone discombobulateShard () {
        gemstone stone = new gemstone("Discombobulate Shard", "#D040D0", itemParticles.ARCANE, handler,
            "Mystic Mind Maze", abilityType.CONFUSE, "Damage: 10. Confusion: 30% for 4 turns", (c1, c2) -> {c2.addConfusion(0.30, 4); c2.takeDmg(10);}, 3, 0.9,
            "Mirage Assault", abilityType.POISON, "Heals you: 5 + 3% of maxHP. Damage: 2 x lvl. Poison 5 and burn 5 per turn, 8 turns", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.03 + 5); c2.addPoisonDamage(5, 8); c2.addFireDamage(5, 8); c2.takeDmg(2*c1.getLevel());}, 2, 0.85,
            "Mind Warp", abilityType.CONFUSE, "Confusion: 75% for 4 turns", (c1, c2) -> {c2.addConfusion(0.75, 4);}, 1, 0.75
        );
        stone.lore("Looking at it for too long makes you forget what you were doing.");
        return stone;
    }

    public gemstone meteorite () {
        gemstone stone = new gemstone("Meteorite", "#6A7A9A", itemParticles.SPARKLE, handler, 
            "Gravitational Slam", abilityType.ATTACK, "Damage: 15 x (1 + lvl/7). Confusion: 10% for 3 turns", (c1, c2) -> {c2.takeDmg(15*(1+c1.getLevel()/7)); c2.addConfusion(0.1, 3);}, 10, 0.99,
            "Cosmic Crush", abilityType.CONFUSE, "Costs you 5% of maxHP. Damage: 10 x (1 + lvl/4). Confusion: 15% for 5 turns", (c1, c2) -> {c1.takeDmg(c1.getMaxHealth()*0.05); c2.addConfusion(0.15, 5); c2.takeDmg(10*(1+c1.getLevel()/4));}, 3, 1,
            "Orbitin Assault", abilityType.ATTACK, "Costs you 20% of maxHP. Damage: 40% of their maxHP", (c1, c2) -> {c1.takeDmg(c1.getMaxHealth()*0.2); c2.takeDmg(c2.getMaxHealth()*0.4);}, 1, 1
        );
        stone.lore("Fell from the sky one night and has been falling, in spirit, ever since.");
        //Throws purple (cosmic) lightning, like a boltstone throws its own: when shaken, or charged by radiation from
        //uranium or by healite pulling at it. Where it strikes, the sparks are every colour
        stone.shakeParticles(1).shakeLightning(0.6, itemInfluence.COSMIC, "#B060FF").rainbowSparks()
            .reactsTo(itemInfluence.RADIATION, 1).reactsTo(itemInfluence.COSMIC, 0.8).reactsTo(itemInfluence.COSMIC_CONDUCTION, 1);
        return stone;
    }
    
    public gemstone magma () {
        gemstone stone = new gemstone("Magma", "#FF5020", itemParticles.FIRE, handler,
            "Molten Fury", abilityType.FIRE, "Burn: 1.5 x lvl per turn, 3 turns", (c1, c2) -> {c2.addFireDamage(c1.getLevel()*1.5, 3);}, 3, 0.85,
            "Magma Blast", abilityType.FIRE, "Burn: lvl per turn, 6 turns", (c1, c2) -> {c2.addFireDamage(c1.getLevel(), 6);}, 3, 0.8,
            "Volcanic Impact", abilityType.FIRE, "Burn: 1.5 x lvl per turn, 6 turns", (c1, c2) -> {c2.addFireDamage(c1.getLevel()*1.5, 6);}, 1, 0.75
        );
        stone.lore("Molten rock that never quite set. Do not store near paper.");
        //Obsidian's cold sets it into coal
        stone.transformsInto(itemInfluence.COLD, 1.5, "#2E2A28", () -> coal());
        stone.shakeParticles(6).emits(itemInfluence.HEAT, 2.0, 0.8).reactsTo(itemInfluence.LIGHTNING, 1);
        return stone;
    }

    public gemstone uranium () {
        gemstone stone = new gemstone("Uranium", "#60E040", itemParticles.TOXIC, handler,
            "Gamma Burst", abilityType.ATTACK, "Damage: 15 x (1 + lvl/7)", (c1, c2) -> {c2.takeDmg(15*(1+c1.getLevel()/7));}, 5, 0.9,
            "Atomic Blast", abilityType.FIRE, "Damage: 10. Burn: 5 + lvl per turn, 5 turns. Heals you: 5 + lvl per turn, 10 turns", (c1, c2) -> {c1.addHealing(5+c1.getLevel(), 10); c2.addFireDamage(5+c1.getLevel(), 5); c2.takeDmg(10);}, 2, 0.9,
            "Fission Wave", abilityType.POISON, "Poison: 5 + lvl per turn, 10 turns. Poisons you: 2 per turn, 5 turns", (c1, c2) -> {c1.addPoisonDamage(2, 5); c2.addPoisonDamage(5+c1.getLevel(), 10);}, 1, 0.9
        );
        stone.lore("Glows faintly in the dark. The previous owner is not available for comment.");
        stone.shakeParticles(8).emits(itemInfluence.RADIATION, 2.2).reactsTo(itemInfluence.LIGHTNING, 1).reactsTo(itemInfluence.COSMIC, 1);
        //Hit by the meteorite's purple lightning (straight or through healite) it turns rainbow for a while
        stone.transformsInto(itemInfluence.COSMIC, itemInteractions.HIT_HEAT, "#FF70FF", () -> rainbowUranium()).carriesState();
        return stone;
    }

    //Uranium hit by cosmic lightning: it runs through every colour, glows, and its moves hit a bit harder. It turns back
    //into uranium about 5 seconds after the last purple strike; every new one fills it up again
    public gemstone rainbowUranium () {
        gemstone stone = new gemstone("Rainbow Uranium", "#60E040", itemParticles.TOXIC, handler,
            "Gamma Burst", abilityType.ATTACK, "Damage: 17 x (1 + lvl/7)", (c1, c2) -> {c2.takeDmg(17*(1+c1.getLevel()/7));}, 5, 0.9,
            "Atomic Blast", abilityType.FIRE, "Damage: 12. Burn: 6 + lvl per turn, 5 turns. Heals you: 6 + lvl per turn, 10 turns", (c1, c2) -> {c1.addHealing(6+c1.getLevel(), 10); c2.addFireDamage(6+c1.getLevel(), 5); c2.takeDmg(12);}, 2, 0.9,
            "Fission Wave", abilityType.POISON, "Poison: 6 + lvl per turn, 10 turns. Poisons you: 2 per turn, 4 turns", (c1, c2) -> {c1.addPoisonDamage(2, 4); c2.addPoisonDamage(6+c1.getLevel(), 10);}, 1, 0.9
        );
        stone.lore("Uranium that caught a falling star's spark. It cannot decide what colour to be.");
        stone.rainbow().restingGlow(0.35);
        stone.shakeParticles(8).emits(itemInfluence.RADIATION, 2.2).reactsTo(itemInfluence.LIGHTNING, 1).reactsTo(itemInfluence.COSMIC, 1);
        //A purple hit counts double here, so it always fills it back up to the full 5 seconds
        stone.revertsInto(itemInfluence.COSMIC, 1, "#60E040", () -> uranium()).heatedBy(itemInfluence.COSMIC, 2).coolsIn(5).carriesState();
        return stone;
    }

    //Draws the boltstone's lightning to it from anywhere on the screen, charges boltstones near it just by being there,
    //and passes strikes on to other copper next to it
    public gemstone copper () {
        gemstone stone = new gemstone("Copper", "#C87533", itemParticles.NONE, handler,
            "Conduct", abilityType.ATTACK, "Damage: 6 x (1 + lvl/4)", (c1, c2) -> {c2.takeDmg(6*(1+c1.getLevel()/4));}, 8, 1,
            "Short Circuit", abilityType.LIGHTNING, "Damage: 12% of their maxHP. Confuses them: 30%, 2 turns", (c1, c2) -> {c2.takeDmg(c2.getMaxHealth()*0.12); c2.addConfusion(0.3, 2);}, 3, 0.85,
            "Grounding", abilityType.CLEANSE, "Removes your effects and heals you: 10% of maxHP", (c1, c2) -> {c1.clearEffects(); c1.heal(c1.getMaxHealth()*0.1);}, 2, 0.9
        );
        stone.lore("Soft, reddish and a little too eager to carry a current.");
        //Pull: 0.46 for a boltstone in the slot next to it, 0.11 with one slot between, full when hovered on top
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        //One lightning hit charges it (its uses left go with it)
        stone.transformsInto(itemInfluence.LIGHTNING, itemInteractions.HIT_HEAT, "#FFE080", () -> chargedCopper()).carriesState();
        return stone;
    }

    //Copper just hit by lightning: it glows a little and its moves hit a bit harder. It turns back into copper about
    //5 seconds after the last hit; every new hit charges it fully again
    public gemstone chargedCopper () {
        gemstone stone = new gemstone("Charged Copper", "#F0A050", itemParticles.SPARKLE, handler,
            "Conduct", abilityType.ATTACK, "Damage: 7 x (1 + lvl/4)", (c1, c2) -> {c2.takeDmg(7*(1+c1.getLevel()/4));}, 8, 1,
            "Short Circuit", abilityType.LIGHTNING, "Damage: 15% of their maxHP. Confuses them: 35%, 2 turns", (c1, c2) -> {c2.takeDmg(c2.getMaxHealth()*0.15); c2.addConfusion(0.35, 2);}, 3, 0.85,
            "Grounding", abilityType.CLEANSE, "Removes your effects and heals you: 15% of maxHP", (c1, c2) -> {c1.clearEffects(); c1.heal(c1.getMaxHealth()*0.15);}, 2, 0.9
        );
        stone.lore("Copper still humming from a lightning strike. It will not last.");
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        //A hit counts double here, so it always fills it back up to the full 5 seconds
        stone.revertsInto(itemInfluence.LIGHTNING, 1, "#C87533", () -> copper()).heatedBy(itemInfluence.LIGHTNING, 2).coolsIn(5).carriesState();
        stone.restingGlow(0.35);
        return stone;
    }

    //Throws off sand while it is moved: grains that fly out along the swing, tumble and fall
    public gemstone sandstone () {
        gemstone stone = new gemstone("Sandstone", "#FFE8A8", itemParticles.SAND, handler,
            "Sandblast", abilityType.ATTACK, "Damage: 9 x (1 + lvl/4)", (c1, c2) -> {c2.takeDmg(9*(1+c1.getLevel()/4));}, 6, 0.95,
            "Sandstorm", abilityType.CONFUSE, "Confuses them: 40%, 3 turns", (c1, c2) -> {c2.addConfusion(0.4, 3);}, 3, 0.85,
            "Erosion", abilityType.POISON, "Poison: 3 + lvl/2 per turn, 6 turns", (c1, c2) -> {c2.addPoisonDamage(3 + c1.getLevel()/2, 6);}, 2, 0.9
        );
        stone.lore("Packed sand that never quite remembers being a rock. It leaves a trail wherever it goes.");
        stone.shakeParticles(60).idleParticles(0).shakeThreshold(0.2).glows(false);
        //Next to magma it heats up (glowing orange) and turns into glass
        stone.transformsInto(itemInfluence.HEAT, 1, "#FF8030", () -> glass());
        return stone;
    }

    //Sandstone that magma has heated, a rock of its own. It stays glass until it is shaken hard enough to break into sand
    public gemstone glass () {
        gemstone stone = new gemstone("Glass", "#F6FCFF", itemParticles.SPARKLE, handler,
            "Shard Spray", abilityType.ATTACK, "Damage: 12 x (1 + lvl/4)", (c1, c2) -> {c2.takeDmg(12*(1+c1.getLevel()/4));}, 6, 0.95,
            "Glare", abilityType.CONFUSE, "Confuses them: 50%, 2 turns", (c1, c2) -> {c2.addConfusion(0.5, 2);}, 3, 0.8,
            "Clear Through", abilityType.CLEANSE, "Removes your effects", (c1, c2) -> {c1.clearEffects();}, 2, 1
        );
        stone.lore("Sand that sat too close to the fire. Handle with care, or it is sand again.");
        //Jerked around hard on the cursor, it breaks back into sandstone
        stone.transformsInto(itemInfluence.SHAKE, 1, "#FFE8A8", () -> sandstone());
        stone.lightTint(0.5).seeThrough(0.55);
        return stone;
    }

    //Melts into water next to magma or when struck by lightning (two strikes); obsidian freezes the water back
    public gemstone ice () {
        gemstone stone = new gemstone("Ice", "#7CC8FF", itemParticles.FROST, handler,
            "Ice Shard", abilityType.ATTACK, "Damage: 8 x (1 + lvl/4)", (c1, c2) -> {c2.takeDmg(8*(1+c1.getLevel()/4));}, 6, 0.95,
            "Freeze", abilityType.CONFUSE, "Confuses them: 60%, 1 turn", (c1, c2) -> {c2.addConfusion(0.6, 1);}, 3, 0.85,
            "Frostbite", abilityType.POISON, "Damage over time: 4 + lvl/3 per turn, 4 turns", (c1, c2) -> {c2.addPoisonDamage(4 + c1.getLevel()/3, 4);}, 2, 0.9
        );
        stone.lore("It never melts in your pocket. Near a fire is another story.");
        stone.transformsInto(itemInfluence.HEAT, 1, "#3C8CE8", () -> water()).heatedBy(itemInfluence.LIGHTNING, 1);
        //It cools things down like obsidian does: glass back into sandstone, water into ice, magma into coal
        stone.emits(itemInfluence.COLD, 2.0, 0.8);
        stone.lightTint(0.45);
        return stone;
    }

    //Melted ice. It drips, and stays water until obsidian freezes it back into ice
    public gemstone water () {
        gemstone stone = new gemstone("Water", "#3C8CE8", itemParticles.WATER, handler,
            "Splash", abilityType.ATTACK, "Damage: 7 x (1 + lvl/4)", (c1, c2) -> {c2.takeDmg(7*(1+c1.getLevel()/4));}, 6, 1,
            "Soak", abilityType.CLEANSE, "Removes your effects", (c1, c2) -> {c1.clearEffects();}, 3, 1,
            "Tide", abilityType.HEAL, "Heals you: 15% of maxHP", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.15);}, 2, 0.95
        );
        stone.lore("A rock that forgot to stay solid. Hold it level.");
        //Obsidian's cold freezes it back into ice
        stone.transformsInto(itemInfluence.COLD, 1, "#7CC8FF", () -> ice());
        //Saltstone next to it (or held over it) salts it
        stone.transformsInto(itemInfluence.SALT, 1, "#E8F2FF", () -> saltWater());
        stone.lightTint(0.3).seeThrough(0.65);
        //Swung around, it pours out behind it in a stream
        stone.trail("#4A9CF0", 16, 0.6);
        return stone;
    }

    //Water that saltstone has salted. Salt water carries a boltstone's lightning like copper does (it draws it and
    //charges boltstones next to it). It does not freeze; healite's healing takes the salt out again
    public gemstone saltWater () {
        gemstone stone = new gemstone("Salt Water", "#5C9CE0", itemParticles.WATER, handler,
            "Brine Splash", abilityType.ATTACK, "Damage: 7 x (1 + lvl/4). Confuses them: 15%, 2 turns", (c1, c2) -> {c2.takeDmg(7*(1+c1.getLevel()/4)); c2.addConfusion(0.15, 2);}, 6, 1,
            "Soak", abilityType.CLEANSE, "Removes your effects", (c1, c2) -> {c1.clearEffects();}, 3, 1,
            "Tide", abilityType.HEAL, "Heals you: 15% of maxHP", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.15);}, 2, 0.95
        );
        stone.lore("Water with a bite. It stings the eyes and carries a spark better than it should.");
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        stone.transformsInto(itemInfluence.HEALING, 1, "#3C8CE8", () -> water());
        stone.lightTint(0.35).seeThrough(0.7);
        stone.trail("#6AA8EC", 16, 0.6);
        return stone;
    }

    //Gives off salt: water next to it (or held over it) turns into salt water, with white grains flying over
    public gemstone saltstone () {
        gemstone stone = new gemstone("Saltstone", "#F2EEE6", itemParticles.SALT, handler,
            "Salt Spray", abilityType.ATTACK, "Damage: 8 x (1 + lvl/4)", (c1, c2) -> {c2.takeDmg(8*(1+c1.getLevel()/4));}, 6, 0.95,
            "Brine Sting", abilityType.POISON, "Poison: 3 + lvl/3 per turn, 5 turns", (c1, c2) -> {c2.addPoisonDamage(3 + c1.getLevel()/3, 5);}, 3, 0.9,
            "Preserve", abilityType.CLEANSE, "Removes your effects and heals you: 10% of maxHP", (c1, c2) -> {c1.clearEffects(); c1.heal(c1.getMaxHealth()*0.1);}, 2, 0.9
        );
        stone.lore("A lump of old sea, dried out. Water near it never tastes the same again.");
        stone.emits(itemInfluence.SALT, 2.0, 0.8);
        stone.lightTint(0.5);
        return stone;
    }

    //Magma that obsidian has cooled: burnt out and black, still smoking a little. A boltstone's lightning relights it
    public gemstone coal () {
        gemstone stone = new gemstone("Coal", "#3A3634", itemParticles.SMOKE, handler,
            "Coal Chunk", abilityType.ATTACK, "Damage: 10 x (1 + lvl/4)", (c1, c2) -> {c2.takeDmg(10*(1+c1.getLevel()/4));}, 3, 0.95,
            "Soot Cloud", abilityType.CONFUSE, "Confuses them: 35%, 2 turns", (c1, c2) -> {c2.addConfusion(0.35, 2);}, 3, 0.9,
            "Rekindle", abilityType.FIRE, "Burn: 3 + lvl/3 per turn, 4 turns", (c1, c2) -> {c2.addFireDamage(3 + c1.getLevel()/3, 4);}, 1, 0.85
        );
        stone.lore("What is left of magma that met something colder. It remembers being on fire, faintly.");
        //Lightning sets it alight again: two strikes (from a boltstone) and it is magma once more
        stone.transformsInto(itemInfluence.LIGHTNING, 1, "#FF5020", () -> magma());
        //Healite's healing brings it back to life too, as slowly as obsidian sets magma
        stone.heatedBy(itemInfluence.HEALING, 1);
        return stone;
    }

    public gemstone healite () {
            gemstone stone = new gemstone("Healite", "#FF90B0", itemParticles.SPARKLE, handler,
            "Revitalize", abilityType.HEAL, "Heals: 50% of your current HP", (c1, c2) -> {c1.heal(c1.getHealth()*0.5);}, 1, 0.7,
            "Ressurection", abilityType.CLEANSE, "Removes every effect on you", (c1, c2) -> {c1.clearEffects();}, 1, 0.7,
            "Transcendence", abilityType.RESTORE, "Restores the uses of all your items", (c1, c2) -> {c1.restoreItems();}, 1, 0.7
        );
        stone.lore("A soft pink stone that hums quietly. Good for the soul.");
        //To the meteorite what copper is to the boltstone: it draws the purple lightning from anywhere on screen,
        //charges meteorites near it just by being there and passes strikes on (to other healite, and on to uranium)
        stone.emits(itemInfluence.COSMIC_CONDUCTION, 3, 1, 1.74).conducts(1.6, itemInfluence.COSMIC).hitDebris(0.3).reactsTo(itemInfluence.COSMIC, 0.5);
        //Its healing turns coal next to it back into magma (hearts fly over while it works)
        stone.emits(itemInfluence.HEALING, 2.0, 0.8);
        stone.transformsInto(itemInfluence.COSMIC, itemInteractions.HIT_HEAT, "#C070FF", () -> chargedHealite()).carriesState();
        return stone;
    }

    //Healite just hit by cosmic lightning: it glows purple and its moves work more often. It turns back into healite
    //about 5 seconds after the last purple strike; every new one fills it up again
    public gemstone chargedHealite () {
        gemstone stone = new gemstone("Charged Healite", "#C878FF", itemParticles.SPARKLE, handler,
            "Revitalize", abilityType.HEAL, "Heals: 60% of your current HP", (c1, c2) -> {c1.heal(c1.getHealth()*0.6);}, 1, 0.8,
            "Ressurection", abilityType.CLEANSE, "Removes every effect on you", (c1, c2) -> {c1.clearEffects();}, 1, 0.8,
            "Transcendence", abilityType.RESTORE, "Restores the uses of all your items", (c1, c2) -> {c1.restoreItems();}, 1, 0.8
        );
        stone.lore("Healite humming with starlight. It will not last.");
        stone.emits(itemInfluence.COSMIC_CONDUCTION, 3, 1, 1.74).conducts(1.6, itemInfluence.COSMIC).hitDebris(0.3).reactsTo(itemInfluence.COSMIC, 0.5);
        stone.emits(itemInfluence.HEALING, 2.0, 0.8);
        stone.revertsInto(itemInfluence.COSMIC, 1, "#FF90B0", () -> healite()).heatedBy(itemInfluence.COSMIC, 2).coolsIn(5).carriesState();
        stone.restingGlow(0.35);
        return stone;
    }

    //Holds two other rocks and becomes them, see elementalRock
    public gemstone elemental () {
        return new elementalRock(handler);
    }

    public gemstone pebble () {
        gemstone stone = new gemstone("Pebble", "#C0C0C0", itemParticles.NONE, handler,
        "Stone Skip", abilityType.ATTACK, "Damage: 5 + 2 x lvl", (c1, c2) -> {c2.takeDmg(c1.getLevel()*2+5);}, 8, 0.95,
        "Rock Remedy", abilityType.FIRE, "Damage: 1.5 x lvl. Burn: 1 + lvl/2 per turn, 3 turns", (c1, c2) -> {c2.takeDmg(c1.getLevel()*1.5); c2.addFireDamage(c1.getLevel()/2 + 1, 3);}, 3, 0.95,
        "Mineral Mend", abilityType.HEAL, "Heals: 20% of maxHP", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.20);}, 3, 0.95
    );
        stone.lore("Found on a beach. It skips well.");
        return stone;
}

}
