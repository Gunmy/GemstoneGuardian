package game;

import static game.battleAnimation.*;

public class gemstones {
    private gameHandler handler;

    //For the moves' animations (battleAnimation): a new one, and whether a part is on the target (T) or the user (U)
    private static battleAnimation a () {return new battleAnimation();}
    private static final boolean T = true, U = false;
    //What a move does (moveEffects): u is the move's user, t its target
    private static moveEffects fx () {return new moveEffects();}
    private static moveEffects.amount theirMaxHP (double share) {return moveEffects.theirMaxHP(share);}
    public gemstones (gameHandler handler) {
        this.handler = handler;
    }

    public gemstone rock () {
        gemstone stone = new gemstone("Rock", "#9A9A9A", itemParticles.NONE, handler, 
        "Pebble Strike", abilityType.ATTACK, fx().damage((u, t) -> 13 + u.getLevel(), "13 + lvl"), 10, 1,
        "Stone Shatter", abilityType.ATTACK, fx().damage((u, t) -> 26  + u.getLevel()*2, "26 + 2 x lvl"), 5, 1,
        "Stone Renewal", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.1 + 20, "20 + 10% maxHP"), 3, 1
        );
        stone.lore("A plain grey stone your grandma kept in a drawer for sixty years. Reliable, if nothing else.");
        //Heated by magma, or by lightning (two strikes, like ice melting), it melts into lava. Its uses left go with it
        stone.transformsInto(itemInfluence.HEAT, 1, "#FF6A20", () -> lava()).heatedBy(itemInfluence.LIGHTNING, 1).carriesState();
        //Pebbles thrown from your hand (now and then a boulder of a pebble rolls at them), a big rock hurled with a shockwave, and grit and hearts round you
        stone.animations(
            a().lunge(0.45, 0.3).toss("#9A9A9A", 5, 0.24, 0.4, 0.6).impact(T, GREY, 24, 0.06).fountain(T, GREY, 10, 0.05, CHUNK).shake(T, 0.035).rare(a().roll("#9A9A9A", 0.8, 1.6, GREY).quake(0.12)),
            a().lunge(0.8, 0.4).toss("#9A9A9A", 1.4, 0.6, 0.5, 1.0).impact(T, GREY, 50, 0.07).fountain(T, GREY, 25, 0.07, CHUNK).quake(0.07).shake(T, 0.07).rare(a().meteor("#9A9A9A", 0.4, 0.7, GREY).quake(0.12)),
            a().rise(U, HEARTS, 14, 0.1, HEART).swirl(U, GREY, 16, 0.05).orbit(U, GREY, 6, 0.06, CHUNK).rare(a().rain(U, HEARTS, 30, 0.1, HEART)));
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
        "Molten Strike", abilityType.FIRE, fx().damage((u, t) -> 13 + u.getLevel(), "13 + lvl").burn((u, t) -> 3, "3", 3), 10, 1,
        "Eruption", abilityType.FIRE, fx().damage((u, t) -> 22 + u.getLevel()*2, "22 + 2 x lvl").burn((u, t) -> 5, "5", 3), 5, 0.9,
        "Cooling Crust", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.1 + 20, "20 + 10% maxHP"), 3, 1
        );
        stone.lore("Grandma's rock, molten. It will set again the moment something cold comes near.");
        //Night with a red glow, like magma while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.5, "#FF2000", 0.18));
        stone.transformsInto(itemInfluence.COLD, LAVA_COOL_DOSE, "#9A9A9A", () -> rock()).carriesState();
        stone.emits(itemInfluence.HEAT, 2.0, 1.0);
        stone.trail("#FF6A20", 16, 0.6);
        //Globs of molten rock thrown at them, an eruption with lava raining down, and a crust of steam with hearts on you
        stone.animations(
            a().lunge(0.6, 0.3).toss("#FF6A20", 3, 0.35, 0.45, 0.8).stream(LAVA, 14, 0.07, FLAME, 0.45, 0.8).impact(T, EMBER, 28, 0.05).rise(T, FIRE, 18, 0.09, FLAME).fountain(T, LAVA, 15, 0.05, FLAME).shake(T, 0.04).rare(a().meteor("#FF6A20", 0.25, 0.35, LAVA).quake(0.08)),
            a().rise(U, FIRE, 22, 0.1, FLAME).meteor("#FF6A20", 0.45, 0.45, LAVA).rain(T, LAVA, 28, 0.1, CHUNK).impact(T, FIRE, 40, 0.06).fountain(U, LAVA, 40, 0.07, CHUNK).quake(0.09).shake(T, 0.06).rare(a().rain(T, LAVA, 90, 0.12, CHUNK)),
            a().rise(U, STEAM, 18, 0.13, null).rise(U, HEARTS, 12, 0.1, HEART).rare(a()));
        return stone;
    }

    public gemstone obsidian () {
        gemstone stone = new gemstone("Obsidian", "#5A4A6A", itemParticles.SMOKE, handler,
        "Heat Wave Slam", abilityType.ATTACK, fx().damage((u, t) -> 10*(1+u.getLevel()/3), "10 x (1 + lvl/3)"), 8, 0.95,
        "Inferno Blast", abilityType.FIRE, fx().burn((u, t) -> 5*(1+u.getLevel()/3), "5 x (1 + lvl/3)", 5), 2, 0.95,
        "Warm Embrace", abilityType.HEAL, fx().healing((u, t) -> u.getMaxHealth()*(0.05+u.getLevel()/150), "(5% + lvl/150) maxHP", 4), 3, 0.95
        );
        stone.lore("Volcanic glass, still warm to the touch. It remembers the fire it came from.");
        //Night, the moves glowing like embers in the dark while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.5));
        //Near magma it heats up and smokes. It cools whatever is next to it: glass back into sandstone, water into ice
        stone.shakeParticles(5).reactsTo(itemInfluence.HEAT, 1).emits(itemInfluence.COLD, 2.0, 0.8);
        //A slam of hot glass with a wave of heat, a roaring jet of fire, and warm embers with hearts round you
        stone.animations(
            a().lunge(0.7, 0.45).toss("#5A4A6A", 1.6, 0.45, 0.35, 0.5).impact(T, OBSIDIAN, 30, 0.06).fountain(T, OBSIDIAN, 18, 0.06, SHARD).quake(0.06).shake(T, 0.06).rare(a().fountain(T, FIRE, 50, 0.07, FLAME)),
            a().beam(FIRE, 0.4).stream(FIRE, 40, 0.08, FLAME, 0.3, 0.15).rise(T, FIRE, 28, 0.1, FLAME).rise(T, SMOKE, 10, 0.13, null).quake(0.04).shake(T, 0.03).rare(a().fountain(T, FIRE, 70, 0.08, FLAME)),
            a().orbit(U, EMBER, 16, 0.06, STAR).rise(U, HEARTS, 12, 0.1, HEART).rare(a().orbit(U, HEARTS, 30, 0.09, HEART)));
        return stone;
    }

    public gemstone boltstone () {
        gemstone stone = new gemstone("Boltstone", "#F0D040", itemParticles.SPARKLE, handler,
        "Static Burst", abilityType.FIRE, fx().burn((u, t) -> 5, "5", 3).damage((u, t) -> 8*(1+u.getLevel()/4), "8 x (1 + lvl/4)"), 8, 1,
        "Thunderbolt", abilityType.LIGHTNING, fx().damage(theirMaxHP(0.15), "15% their maxHP"), 3, 0.8,
        "Electrocuter", abilityType.LIGHTNING, fx().damage(theirMaxHP(0.4), "40% their maxHP"), 1, 0.5
        );
        stone.lore("Struck by lightning so many times it started striking back.");
        stone.shakeParticles(1).shakeLightning(0.6)
            .reactsTo(itemInfluence.RADIATION, 1).reactsTo(itemInfluence.LIGHTNING, 0.8).reactsTo(itemInfluence.CONDUCTION, 1);
        //Crackling sparks that set them burning, a thunderbolt, and a storm of lightning for the Electrocuter (sometimes straight out of the sky)
        stone.animations(
            a().bolt("#FFE500", 4).stream(YELLOW, 18, 0.05, SPARK, 0.25, 0.1).impact(T, YELLOW, 24, 0.05).rise(T, FIRE, 12, 0.08, FLAME).fountain(T, YELLOW, 12, 0.045, SPARK).shake(T, 0.025).rare(a().skybolt("#FFE500", 4)),
            a().lunge(1.0, 0.2).bolt("#FFE500", 8).bolt("#FFFFFF", 3).impact(T, YELLOW, 45, 0.06).quake(0.06).shake(T, 0.05).rare(a().skybolt("#FFFFFF", 8).quake(0.1)),
            a().bolt("#FFE500", 14).bolt("#FFFFFF", 7).skybolt("#FFE500", 3).impact(T, YELLOW, 80, 0.07).orbit(T, YELLOW, 22, 0.07, SPARK).fountain(T, YELLOW, 50, 0.06, SPARK).orbit(U, YELLOW, 12, 0.06, SPARK).quake(0.14).shake(T, 0.09).rare(a().skybolt("#FFFFFF", 16).fountain(T, YELLOW, 40, 0.06, SPARK)));
        return stone;
    }

    public gemstone giantBolder () {
        gemstone stone = new gemstone("Giant Bolder", "#8A6A4A", itemParticles.NONE, handler, 
        "Bolder Smash", abilityType.ATTACK, fx().damage((u, t) -> 7*(1+u.getLevel()/3), "7 x (1 + lvl/3)"), 6, 1,
        "Crushing Blow", abilityType.CONFUSE, fx().damage((u, t) -> 10 + u.getLevel(), "10 + lvl").confuse(0.2, 6), 5, 0.9,
        "Mountainous Maul", abilityType.DRAIN, fx().heal(theirMaxHP(0.2), "20% their maxHP").damage(theirMaxHP(0.2), "20% their maxHP"), 1, 0.6
        );
        stone.lore("Not actually giant. The name was chosen by someone who had just been hit by it.");
        //A giant boulder rolled at them, one that leaves them seeing stars, and one dropped on them that takes their hearts to you
        stone.animations(
            a().lunge(1.1, 0.35).roll("#8A6A4A", 1.1, 0.9, BROWN).impact(T, BROWN, 24, 0.06).fountain(T, BROWN, 25, 0.07, CHUNK).quake(0.1).shake(T, 0.07).rare(a().roll("#8A6A4A", 0.45, 1.5, BROWN).quake(0.14)),
            a().lunge(1.1, 0.35).roll("#8A6A4A", 1.1, 0.8, BROWN).impact(T, BROWN, 20, 0.06).orbit(T, STARS, 22, 0.08, STAR).quake(0.08).shake(T, 0.06).rare(a().meteor("#8A6A4A", 0.3, 0.6, BROWN)),
            a().meteor("#8A6A4A", 0.9, 1.0, BROWN).impact(T, BROWN, 30, 0.06).impact(T, HURT, 14, 0.05).back(HEARTS, 14, 0.1, HEART, 0.6, 0.6).rise(U, HEARTS, 8, 0.09, HEART).fountain(T, BROWN, 30, 0.07, CHUNK).quake(0.12).shake(T, 0.08).rare(a().back(HEARTS, 50, 0.13, HEART, 0.5, 0.8)));
        return stone;
    }

    
    public gemstone discombobulateShard () {
        gemstone stone = new gemstone("Discombobulate Shard", "#D040D0", itemParticles.ARCANE, handler,
            "Mystic Mind Maze", abilityType.CONFUSE, fx().confuse(0.30, 4).damage((u, t) -> 10, "10"), 3, 0.9,
            "Mirage Assault", abilityType.POISON, fx().heal((u, t) -> u.getMaxHealth()*0.03 + 5, "5 + 3% maxHP").poison((u, t) -> 5, "5", 8).burn((u, t) -> 5, "5", 8).damage((u, t) -> 2*u.getLevel(), "2 x lvl"), 2, 0.85,
            "Mind Warp", abilityType.CONFUSE, fx().confuse(0.75, 4), 1, 0.75
        );
        stone.lore("Looking at it for too long makes you forget what you were doing.");
        //Purple stars that spin round their head, a mirage of poison and fire (a little healing for you), and a mind warp
        stone.animations(
            a().toss("#D040D0", 2.5, 0.22, 0.5, 0.7).stream(PURPLE, 14, 0.08, STAR, 0.4, 0.7).orbit(T, PURPLE, 22, 0.08, STAR).impact(T, PURPLE, 16, 0.05).rare(a().orbit(T, PURPLE, 40, 0.09, STAR)),
            a().stream(PURPLE, 10, 0.07, SPARK, 0.4, 0.6).rise(T, POISON, 14, 0.09, BUBBLE).rise(T, FIRE, 12, 0.09, FLAME).rise(U, HEARTS, 6, 0.09, HEART).fountain(T, PURPLE, 20, 0.06, STAR).shake(T, 0.03).rare(a()),
            a().orbit(T, PURPLE, 34, 0.09, STAR).swirl(T, PURPLE, 30, 0.06).orbit(T, STARS, 10, 0.07, STAR).fountain(T, PURPLE, 24, 0.06, STAR).quake(0.04).shake(T, 0.02).rare(a().swirl(T, PURPLE, 90, 0.07).orbit(T, PURPLE, 60, 0.1, STAR)));
        return stone;
    }

    //The moon and the sun as rocks. Seen from space (spaceView) the moon and the sun are these two, huge
    public static final String MOON_COLOR = "#D8DCE4", SUN_COLOR = "#FFC830";

    public gemstone moonstone () {
        gemstone stone = new gemstone("Moonstone", MOON_COLOR, itemParticles.SPARKLE, handler,
            "Moonbeam", abilityType.CONFUSE, fx().damage((u, t) -> 6*(1+u.getLevel()/5), "6 x (1 + lvl/5)").confuse(0.15, 2), 8, 0.95,
            "Lunar Tide", abilityType.CONFUSE, fx().confuse(0.4, 3).heal((u, t) -> u.getMaxHealth()*0.05, "5% maxHP"), 3, 0.9,
            "Eclipse", abilityType.CONFUSE, fx().confuse(0.7, 3).damage(theirMaxHP(0.1), "10% their maxHP"), 1, 0.7
        );
        stone.lore("A piece of the moon that came down one night. It is coldest when the moon is full.");
        //Like obsidian to the rocks round it: cold (glass back into sandstone, water into ice, lava back into rock),
        //and near magma's heat it heats up and smokes
        stone.shakeParticles(5).reactsTo(itemInfluence.HEAT, 1).emits(itemInfluence.COLD, 2.0, 0.8);
        stone.glowColor("#D8F4FF").restingGlow(0.3);
        //Moonlit night while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.6, "#8EDCF2", 0.08));
        //All from the sky: a beam of moonlight straight down (beamParticle) with stars falling in it, a tide of moonlight washing over them (and a
        //little over you), and the moon itself coming down out of space for the eclipse
        stone.animations(
            a().skyBeam(T, 0.28, "#8EDCF2", "#D8F4FF").shaft(T, MOONLIGHT, 0.4).rain(T, STARS, 18, 0.06, STAR).impact(T, MOONLIGHT, 24, 0.05).shake(T, 0.03).rare(a().fromSpace(MOON_COLOR, 0.35, 0.3, MOONLIGHT)),
            a().rain(T, MOONLIGHT, 40, 0.07, DROP).swirl(T, MOONLIGHT, 18, 0.05).orbit(T, STARS, 14, 0.07, STAR).shaft(U, MOONLIGHT, 0.35).rise(U, HEARTS, 8, 0.09, HEART).rare(a().rain(T, STARS, 60, 0.08, STAR)),
            a().fromSpace(MOON_COLOR, 0.9, 0.9, MOONLIGHT).shaft(T, MOONLIGHT, 0.8).impact(T, MOONLIGHT, 60, 0.07).orbit(T, STARS, 20, 0.08, STAR).quake(0.12).shake(T, 0.08).rare(a().fromSpace(MOON_COLOR, 0.2, 0.4, STARS).quake(0.16)));
        return stone;
    }

    public gemstone sunstone () {
        gemstone stone = new gemstone("Sunstone", SUN_COLOR, itemParticles.FIRE, handler,
            "Sunbeam", abilityType.FIRE, fx().damage((u, t) -> 8*(1+u.getLevel()/5), "8 x (1 + lvl/5)").burn((u, t) -> 2, "2", 2), 8, 0.95,
            "Sunlight", abilityType.HEAL, fx().healing((u, t) -> u.getMaxHealth()*0.04, "4% maxHP", 5), 4, 0.95,
            "Daybreak", abilityType.HEAL, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.2, "20% maxHP"), 2, 0.85
        );
        stone.lore("Warm in the hand even at night. It has never once been in the shade.");
        //Like magma to the rocks round it: hot (melts ice, sand into glass, rock into lava), and charged by lightning.
        //Unlike magma nothing cools it: obsidian or ice next to it never turn it into another rock
        stone.shakeParticles(6).emits(itemInfluence.HEAT, 2.0, 0.8).reactsTo(itemInfluence.LIGHTNING, 1);
        stone.glowColor("#FFE500").restingGlow(0.4);
        //The world goes golden while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.tinted("#FFD040", 0.22));
        //All from the sky: a blinding beam of sunlight straight down onto them (beamParticle), warm sunlight raining down
        //on you as it heals, and the dawn light pouring down on you
        stone.animations(
            a().skyBeam(T, 0.35, "#FC9F00", "#FFE500").shaft(T, SUNLIGHT, 0.7).impact(T, GOLD, 50, 0.06).rise(T, FIRE, 12, 0.08, FLAME).quake(0.05).shake(T, 0.05).rare(a().fromSpace(SUN_COLOR, 0.2, 0.5, FIRE).quake(0.12)),
            a().rain(U, SUNLIGHT, 30, 0.06, SPARK).swirl(U, GOLD, 16, 0.05).rise(U, HEARTS, 14, 0.09, HEART).orbit(U, SUNLIGHT, 12, 0.07, STAR).rare(a().rain(U, HEARTS, 50, 0.1, HEART)),
            a().shaft(U, SUNLIGHT, 0.7).rise(U, GOLD, 22, 0.08, STAR).rise(U, CLEAN, 14, 0.06, SPARK).fountain(U, SUNLIGHT, 24, 0.06, SPARK).rare(a().rain(U, GOLD, 60, 0.08, STAR)));
        return stone;
    }

    //A black hole as a rock. Seen from space (spaceView) one hangs far off, huge, pulling a disc of light into it
    public static final String SINGULARITY_COLOR = "#2A2238";

    public gemstone singularity () {
        gemstone stone = new gemstone("Singularity", SINGULARITY_COLOR, itemParticles.ARCANE, handler,
            "Gravity Well", abilityType.ATTACK, fx().damage((u, t) -> 12*(1+u.getLevel()/4), "12 x (1 + lvl/4)"), 8, 0.95,
            "Spaghettify", abilityType.DRAIN, fx().heal(theirMaxHP(0.12), "12% their maxHP").damage(theirMaxHP(0.12), "12% their maxHP"), 3, 0.85,
            "Collapse", abilityType.ATTACK, fx().costs((u, t) -> u.getMaxHealth()*0.1, "10% maxHP").damage(theirMaxHP(0.3), "30% their maxHP"), 1, 0.6
        );
        stone.lore("Heavier than it has any right to be. Light bends a little going past it.");
        //Smaller than the other rocks, with a white edge and a black glow
        stone.iconScale(0.85).outline("#FFFFFF").glowColor("#000000").restingGlow(0.35);
        //Throws the meteorite's purple (cosmic) lightning and takes part like it does: when shaken, or charged by
        //radiation from uranium, by meteorites' lightning or by healite pulling at it
        stone.shakeParticles(1).shakeLightning(0.6, itemInfluence.COSMIC, "#B060FF")
            .reactsTo(itemInfluence.RADIATION, 1).reactsTo(itemInfluence.COSMIC, 0.8).reactsTo(itemInfluence.COSMIC_CONDUCTION, 1);
        //Deep night, darker than obsidian's, with a purple hue while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.75, "#3C2B46", 0.15));
        //Each with the singularity itself: tiny singularities thrown at them that pull everything round them in; a dark
        //tether that stretches them and draws their health over to you; and a huge one falling out of space onto them,
        //a black core swallowing a whole disc of light
        stone.animations(
            a().lunge(0.5, 0.3).toss(SINGULARITY_COLOR, 2.5, 0.35, 0.4, 0.6).vortex(T, VOID, 35, 0.05, 0.7).impact(T, VOID, 24, 0.05).shake(T, 0.04).rare(a().roll(SINGULARITY_COLOR, 0.8, 1.2, VOID)),
            a().beam(VOID, 0.2).back(VOID, 34, 0.06, SPARK, 0.5, 0.15).back(HEARTS, 10, 0.08, HEART, 0.6, 0.4).core(T, 0.22).shake(T, 0.06).rise(U, HEARTS, 8, 0.09, HEART).rare(a().back(HEARTS, 40, 0.1, HEART, 0.5, 0.5)),
            a().fromSpace(SINGULARITY_COLOR, 0.9, 0.9, VOID).core(T, 0.45).vortex(T, ACCRETION, 110, 0.07, 1.6).impact(U, HURT, 10, 0.05).fountain(T, VOID, 30, 0.06, STAR).quake(0.12).shake(T, 0.09).shake(U, 0.03).rare(a().vortex(T, VOID, 120, 0.08, 2.2).quake(0.18)));
        return stone;
    }

    //A piece of the mirrors the crystal boy went through, in the mirror dimension's cold glassy blue
    public gemstone mirror () {
        gemstone stone = new gemstone("Mirror Shard", "#C8E4F4", itemParticles.SPARKLE, handler,
            "Glint", abilityType.ATTACK, fx().damage((u, t) -> 9*(1+u.getLevel()/4), "9 x (1 + lvl/4)"), 8, 0.95,
            "Mirror Image", abilityType.CONFUSE, fx().confuse(0.35, 2).heal((u, t) -> u.getMaxHealth()*0.05, "5% maxHP"), 3, 0.9,
            "Reflection", abilityType.CLEANSE, fx().reflect().damage(theirMaxHP(0.08), "8% their maxHP"), 2, 0.85
        );
        stone.lore("Whatever you see in it is looking back. Sometimes it moves first.");
        stone.glowColor("#F6FCFF").restingGlow(0.25);
        //The world goes the mirror dimension's cold blue while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.tinted("#8EDCF2", 0.2));
        //A mirror shimmering up and a mirror image of them flying out of it at them (mirrorParticle), copies of you circling them so they cannot tell which is real, and
        //your burn, poison and confusion thrown back at them off the mirror, in their own colours
        stone.animations(
            a().mirrorCopy(1.4, 0.55).stream(MIRROR, 8, 0.05, SHARD, 0.35, 0.3).shake(T, 0.03).rare(a().mirrorCopy(0.7, 0.4)),
            a().orbit(T, MIRROR, 24, 0.08, SHARD).swirl(T, MIRROR, 18, 0.05).rise(U, HEARTS, 6, 0.08, HEART).rare(a().orbit(T, MIRROR, 40, 0.09, SHARD)),
            a().rise(U, MIRROR, 20, 0.07, SHARD).stream(FIRE, 10, 0.06, FLAME, 0.4, 0.4).stream(POISON, 10, 0.06, BUBBLE, 0.45, 0.5).stream(PURPLE, 10, 0.06, STAR, 0.5, 0.6).impact(T, MIRROR, 30, 0.05).shake(T, 0.04).rare(a().fountain(T, MIRROR, 40, 0.06, SHARD)));
        return stone;
    }

    public gemstone meteorite () {
        gemstone stone = new gemstone("Meteorite", "#6A7A9A", itemParticles.SPARKLE, handler, 
            "Gravitational Slam", abilityType.ATTACK, fx().damage((u, t) -> 15*(1+u.getLevel()/7), "15 x (1 + lvl/7)").confuse(0.1, 3), 10, 0.99,
            "Cosmic Crush", abilityType.CONFUSE, fx().costs((u, t) -> u.getMaxHealth()*0.05, "5% maxHP").confuse(0.15, 5).damage((u, t) -> 10*(1+u.getLevel()/4), "10 x (1 + lvl/4)"), 3, 1,
            "Orbitin Assault", abilityType.ATTACK, fx().costs((u, t) -> u.getMaxHealth()*0.2, "20% maxHP").damage(theirMaxHP(0.4), "40% their maxHP"), 1, 1
        );
        stone.lore("Fell from the sky one night and has been falling, in spirit, ever since.");
        //Night with a hint of deep space while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.6, "#3A2A80", 0.12));
        //Throws purple (cosmic) lightning, like a boltstone throws its own: when shaken, or charged by radiation from
        //uranium or by healite pulling at it. Where it strikes, the sparks are every colour
        stone.shakeParticles(1).shakeLightning(0.6, itemInfluence.COSMIC, "#B060FF").rainbowSparks()
            .reactsTo(itemInfluence.RADIATION, 1).reactsTo(itemInfluence.COSMIC, 0.8).reactsTo(itemInfluence.COSMIC_CONDUCTION, 1);
        //Everything comes from space: a meteor crashing down on them (sometimes a whole shower), small meteors with purple
        //lightning out of the sky (costing you some health), and a giant meteor with a shower of small ones
        stone.animations(
            a().fromSpace("#6A7A9A", 0.7, 0.6, COSMIC).impact(T, COSMIC, 34, 0.06).orbit(T, STARS, 10, 0.07, STAR).fountain(T, COSMIC, 25, 0.06, STAR).quake(0.1).shake(T, 0.07).rare(a().fromSpace("#6A7A9A", 0.15, 0.45, COSMIC).quake(0.14)),
            a().fromSpace("#6A7A9A", 0.5, 0.3, COSMIC).skybolt("#887BFE", 5).skybolt("#B4A8FF", 2).impact(T, COSMIC, 45, 0.06).impact(U, HURT, 10, 0.05).orbit(T, STARS, 12, 0.07, STAR).quake(0.05).shake(T, 0.05).shake(U, 0.02).rare(a().skybolt("#887BFE", 10).fromSpace("#B4A8FF", 0.2, 0.3, COSMIC)),
            a().orbit(U, COSMIC, 26, 0.09, STAR).fromSpace("#6A7A9A", 0.9, 1.0, COSMIC).fromSpace("#6A7A9A", 0.35, 0.3, COSMIC).impact(T, COSMIC, 70, 0.07).impact(U, HURT, 14, 0.05).fountain(T, COSMIC, 45, 0.07, STAR).quake(0.14).shake(T, 0.08).rare(a().fromSpace("#6A7A9A", 0.12, 0.55, COSMIC)));
        return stone;
    }
    
    public gemstone magma () {
        gemstone stone = new gemstone("Magma", "#FF5020", itemParticles.FIRE, handler,
            "Molten Fury", abilityType.FIRE, fx().burn((u, t) -> u.getLevel()*1.5, "1.5 x lvl", 3), 3, 0.85,
            "Magma Blast", abilityType.FIRE, fx().burn((u, t) -> u.getLevel(), "lvl", 6), 3, 0.8,
            "Volcanic Impact", abilityType.FIRE, fx().burn((u, t) -> u.getLevel()*1.5, "1.5 x lvl", 6), 1, 0.75
        );
        stone.lore("Molten rock that never quite set. Do not store near paper.");
        //Night with a red glow while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.5, "#FF2000", 0.18));
        //Obsidian's cold sets it into coal
        stone.transformsInto(itemInfluence.COLD, 1.5, "#2E2A28", () -> coal());
        stone.shakeParticles(6).emits(itemInfluence.HEAT, 2.0, 0.8).reactsTo(itemInfluence.LIGHTNING, 1);
        //Fire thrown at them, a blast of magma, and a volcano raining magma down: they burn for all three
        stone.animations(
            a().lunge(0.5, 0.25).beam(FIRE, 0.3).stream(FIRE, 26, 0.08, FLAME, 0.35, 0.3).rise(T, FIRE, 26, 0.1, FLAME).fountain(T, FIRE, 15, 0.06, FLAME).shake(T, 0.03).rare(a()),
            a().lunge(0.8, 0.35).toss("#FF5020", 2, 0.5, 0.45, 0.9).impact(T, FIRE, 40, 0.06).rise(T, FIRE, 16, 0.09, FLAME).fountain(T, LAVA, 30, 0.07, CHUNK).quake(0.07).shake(T, 0.05).rare(a().meteor("#FF5020", 0.3, 0.5, LAVA)),
            a().rise(U, FIRE, 16, 0.1, FLAME).meteor("#FF5020", 0.4, 0.4, LAVA).rain(T, LAVA, 34, 0.11, CHUNK).rise(T, FIRE, 28, 0.1, FLAME).rise(T, SMOKE, 10, 0.15, null).fountain(T, LAVA, 50, 0.07, FLAME).quake(0.14).shake(T, 0.08).rare(a().meteor("#FF5020", 0.12, 0.5, LAVA).quake(0.18)));
        return stone;
    }

    public gemstone uranium () {
        gemstone stone = new gemstone("Uranium", "#60E040", itemParticles.TOXIC, handler,
            "Gamma Burst", abilityType.ATTACK, fx().damage((u, t) -> 15*(1+u.getLevel()/7), "15 x (1 + lvl/7)"), 5, 0.9,
            "Atomic Blast", abilityType.FIRE, fx().healing((u, t) -> 5+u.getLevel(), "5 + lvl", 10).burn((u, t) -> 5+u.getLevel(), "5 + lvl", 5).damage((u, t) -> 10, "10"), 2, 0.9,
            "Fission Wave", abilityType.POISON, fx().poisonYou((u, t) -> 2, "2", 5).poison((u, t) -> 5+u.getLevel(), "5 + lvl", 10), 1, 0.9
        );
        stone.lore("Glows faintly in the dark. The previous owner is not available for comment.");
        stone.trembles(0.3).shakeParticles(8).emits(itemInfluence.RADIATION, 2.2).reactsTo(itemInfluence.LIGHTNING, 1).reactsTo(itemInfluence.COSMIC, 1);
        //Hit by the meteorite's purple lightning (straight or through healite) it turns rainbow for a while
        stone.transformsInto(itemInfluence.COSMIC, itemInteractions.HIT_HEAT, "#FF70FF", () -> rainbowUranium()).carriesState();
        //A beam of gamma rays with green lightning in it, an atomic blast (burning them, healing you, now and then a mushroom cloud), and a wave of poison that gets you a little too
        stone.animations(
            a().beam(RADIATION, 0.45).bolt("#60E040", 5).bolt("#B0FF80", 2).impact(T, RADIATION, 36, 0.06).fountain(T, RADIATION, 20, 0.055, SPARK).orbit(U, RADIATION, 14, 0.06, SPARK).quake(0.05).shake(T, 0.04).rare(a().beam(CLEAN, 0.8).skybolt("#60E040", 6)),
            a().impact(T, RADIATION, 60, 0.07).rise(T, FIRE, 14, 0.09, FLAME).rise(U, HEARTS, 8, 0.09, HEART).fountain(T, RADIATION, 50, 0.07, null).quake(0.12).shake(T, 0.07).rare(a().fountain(T, SMOKE, 90, 0.14, null).quake(0.18)),
            a().stream(RADIATION, 18, 0.09, BUBBLE, 0.5, 0.4).bolt("#60E040", 2).rise(T, POISON, 20, 0.09, BUBBLE).rise(U, POISON, 5, 0.07, BUBBLE).fountain(T, POISON, 24, 0.06, BUBBLE).shake(T, 0.03).rare(a()));
        return stone;
    }

    //Uranium hit by cosmic lightning: it runs through every colour, glows, and its moves hit a bit harder. It turns back
    //into uranium about 5 seconds after the last purple strike; every new one fills it up again
    public gemstone rainbowUranium () {
        gemstone stone = new gemstone("Rainbow Uranium", "#60E040", itemParticles.TOXIC, handler,
            "Gamma Burst", abilityType.ATTACK, fx().damage((u, t) -> 17*(1+u.getLevel()/7), "17 x (1 + lvl/7)"), 5, 0.9,
            "Atomic Blast", abilityType.FIRE, fx().healing((u, t) -> 6+u.getLevel(), "6 + lvl", 10).burn((u, t) -> 6+u.getLevel(), "6 + lvl", 5).damage((u, t) -> 12, "12"), 2, 0.9,
            "Fission Wave", abilityType.POISON, fx().poisonYou((u, t) -> 2, "2", 4).poison((u, t) -> 6+u.getLevel(), "6 + lvl", 10), 1, 0.9
        );
        stone.lore("Uranium that caught a falling star's spark. It cannot decide what colour to be.");
        stone.rainbow().restingGlow(0.35);
        stone.trembles(0.3).shakeParticles(8).emits(itemInfluence.RADIATION, 2.2).reactsTo(itemInfluence.LIGHTNING, 1).reactsTo(itemInfluence.COSMIC, 1);
        //A purple hit counts double here, so it always fills it back up to the full 5 seconds
        stone.revertsInto(itemInfluence.COSMIC, 1, "#60E040", () -> uranium()).heatedBy(itemInfluence.COSMIC, 2).coolsIn(5).carriesState();
        //Uranium's moves in every colour
        stone.animations(
            a().beam(RAINBOW, 0.5).bolt("#FF70FF", 5).bolt("#21FF2C", 3).impact(T, RAINBOW, 40, 0.06).fountain(T, RAINBOW, 24, 0.055, SPARK).orbit(U, RAINBOW, 16, 0.06, SPARK).quake(0.05).shake(T, 0.04).rare(a().skybolt("#FF70FF", 8).fountain(T, CONFETTI, 50, 0.06, STAR)),
            a().impact(T, RAINBOW, 64, 0.07).rise(T, FIRE, 14, 0.09, FLAME).rise(U, HEARTS, 8, 0.09, HEART).fountain(T, RAINBOW, 54, 0.07, null).quake(0.12).shake(T, 0.07).rare(a().fountain(T, CONFETTI, 90, 0.1, STAR)),
            a().stream(RAINBOW, 18, 0.09, BUBBLE, 0.5, 0.4).bolt("#FF70FF", 2).rise(T, POISON, 20, 0.09, BUBBLE).rise(U, POISON, 5, 0.07, BUBBLE).fountain(T, RAINBOW, 24, 0.06, BUBBLE).shake(T, 0.03).rare(a()));
        return stone;
    }

    //Draws the boltstone's lightning to it from anywhere on the screen, charges boltstones near it just by being there,
    //and passes strikes on to other copper next to it
    public gemstone copper () {
        gemstone stone = new gemstone("Copper", "#C87533", itemParticles.NONE, handler,
            "Conduct", abilityType.ATTACK, fx().damage((u, t) -> 6*(1+u.getLevel()/4), "6 x (1 + lvl/4)"), 8, 1,
            "Short Circuit", abilityType.LIGHTNING, fx().damage(theirMaxHP(0.12), "12% their maxHP").confuse(0.3, 2), 3, 0.85,
            "Grounding", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.1, "10% maxHP"), 2, 0.9
        );
        stone.lore("Soft, reddish and a little too eager to carry a current.");
        //Pull: 0.46 for a boltstone in the slot next to it, 0.11 with one slot between, full when hovered on top
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        //One lightning hit charges it (its uses left go with it)
        stone.transformsInto(itemInfluence.LIGHTNING, itemInteractions.HIT_HEAT, "#FFE080", () -> chargedCopper()).carriesState();
        //Copper nuggets thrown with sparks (sometimes a rain of copper coins), a short circuit that leaves them seeing stars, and grounding that cleans you off
        stone.animations(
            a().lunge(0.5, 0.25).toss("#C87533", 3.5, 0.24, 0.35, 0.5).bolt("#F0A050", 2.5).impact(T, COPPER, 22, 0.05).fountain(T, COPPER, 12, 0.045, SPARK).shake(T, 0.03).rare(a().rain(T, COPPER, 60, 0.12, CHUNK)),
            a().bolt("#F0A050", 8).bolt("#FFE080", 4).impact(T, COPPER, 38, 0.06).orbit(T, STARS, 12, 0.07, STAR).quake(0.05).shake(T, 0.05).rare(a().skybolt("#F0A050", 8)),
            a().rise(U, CLEAN, 16, 0.07, SPARK).rise(U, HEARTS, 6, 0.09, HEART).fountain(U, COPPER, 14, 0.05, SPARK).rare(a()));
        return stone;
    }

    //Copper just hit by lightning: it glows a little and its moves hit a bit harder. It turns back into copper about
    //5 seconds after the last hit; every new hit charges it fully again
    public gemstone chargedCopper () {
        gemstone stone = new gemstone("Charged Copper", "#F0A050", itemParticles.SPARKLE, handler,
            "Conduct", abilityType.ATTACK, fx().damage((u, t) -> 7*(1+u.getLevel()/4), "7 x (1 + lvl/4)"), 8, 1,
            "Short Circuit", abilityType.LIGHTNING, fx().damage(theirMaxHP(0.15), "15% their maxHP").confuse(0.35, 2), 3, 0.85,
            "Grounding", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.15, "15% maxHP"), 2, 0.9
        );
        stone.lore("Copper still humming from a lightning strike. It will not last.");
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        //A hit counts double here, so it always fills it back up to the full 5 seconds
        stone.revertsInto(itemInfluence.LIGHTNING, 1, "#C87533", () -> copper()).heatedBy(itemInfluence.LIGHTNING, 2).coolsIn(5).carriesState();
        stone.restingGlow(0.35);
        //Copper's moves, crackling harder
        stone.animations(
            a().lunge(0.5, 0.25).toss("#F0A050", 4, 0.24, 0.35, 0.5).bolt("#FFE080", 4).impact(T, YELLOW, 28, 0.05).fountain(T, YELLOW, 16, 0.045, SPARK).shake(T, 0.035).rare(a().rain(T, COPPER, 70, 0.12, CHUNK).skybolt("#FFE080", 4)),
            a().bolt("#FFE080", 11).bolt("#FFFFFF", 4).impact(T, COPPER, 46, 0.06).orbit(T, STARS, 14, 0.07, STAR).quake(0.07).shake(T, 0.06).rare(a().skybolt("#FFFFFF", 10)),
            a().rise(U, CLEAN, 18, 0.07, SPARK).rise(U, HEARTS, 8, 0.09, HEART).orbit(U, YELLOW, 10, 0.06, SPARK).rare(a()));
        return stone;
    }

    //Throws off sand while it is moved: grains that fly out along the swing, tumble and fall
    public gemstone sandstone () {
        gemstone stone = new gemstone("Sandstone", "#FFE8A8", itemParticles.SAND, handler,
            "Sandblast", abilityType.ATTACK, fx().damage((u, t) -> 9*(1+u.getLevel()/4), "9 x (1 + lvl/4)"), 6, 0.95,
            "Sandstorm", abilityType.CONFUSE, fx().confuse(0.4, 3), 3, 0.85,
            "Erosion", abilityType.POISON, fx().poison((u, t) -> 3 + u.getLevel()/2, "3 + lvl/2", 6), 2, 0.9
        );
        stone.lore("Packed sand that never quite remembers being a rock. It leaves a trail wherever it goes.");
        stone.shakeParticles(60).idleParticles(0).shakeThreshold(0.2).glows(false);
        //Next to magma it heats up (glowing orange) and turns into glass
        stone.transformsInto(itemInfluence.HEAT, 1, "#FF8030", () -> glass());
        //A blast of sand, a sandstorm round them (sometimes a whole tornado), and erosion wearing them down
        stone.animations(
            a().lunge(0.6, 0.25).beam(SAND, 0.35).stream(SAND, 60, 0.045, null, 0.25, 0.15).impact(T, SAND, 26, 0.045).fountain(T, SAND, 25, 0.045, null).shake(T, 0.03).rare(a().stream(SAND, 160, 0.05, null, 0.2, 0.1)),
            a().swirl(T, SAND, 80, 0.05).orbit(T, STARS, 10, 0.07, STAR).rise(T, SAND, 14, 0.06, null).shake(T, 0.02).rare(a().swirl(T, SAND, 220, 0.07).quake(0.07)),
            a().swirl(T, SAND, 28, 0.045).rise(T, POISON, 10, 0.08, BUBBLE).rise(T, SAND, 14, 0.05, null).rare(a()));
        return stone;
    }

    //Sandstone that magma has heated, a rock of its own. It stays glass until it is shaken hard enough to break into sand
    public gemstone glass () {
        gemstone stone = new gemstone("Glass", "#F6FCFF", itemParticles.SPARKLE, handler,
            "Shard Spray", abilityType.ATTACK, fx().damage((u, t) -> 12*(1+u.getLevel()/4), "12 x (1 + lvl/4)"), 6, 0.95,
            "Glare", abilityType.CONFUSE, fx().confuse(0.5, 2), 3, 0.8,
            "Clear Through", abilityType.CLEANSE, fx().cleanse(), 2, 1
        );
        stone.lore("Sand that sat too close to the fire. Handle with care, or it is sand again.");
        //Jerked around hard on the cursor, it breaks back into sandstone
        stone.transformsInto(itemInfluence.SHAKE, 1, "#FFE8A8", () -> sandstone());
        stone.lightTint(0.5).seeThrough(0.55);
        //A spray of glass shards, a blinding glare (sometimes a disco), and a clear shine on you that takes everything off
        stone.animations(
            a().lunge(0.5, 0.25).stream(GLASS, 28, 0.08, SHARD, 0.3, 0.3).impact(T, GLASS, 34, 0.05).fountain(T, GLASS, 20, 0.06, SHARD).shake(T, 0.035).rare(a().stream(GLASS, 90, 0.1, SHARD, 0.25, 0.2)),
            a().beam(CLEAN, 0.5).impact(T, GLASS, 64, 0.08).orbit(T, STARS, 16, 0.07, STAR).rise(T, CLEAN, 14, 0.07, SPARK).orbit(U, GLASS, 12, 0.07, SPARK).quake(0.03).rare(a().orbit(T, GLASS, 70, 0.09, STAR).fountain(T, GLASS, 50, 0.07, SPARK).orbit(U, GLASS, 40, 0.08, STAR)),
            a().rise(U, CLEAN, 22, 0.07, SPARK).swirl(U, GLASS, 20, 0.05).rare(a().fountain(U, GLASS, 50, 0.06, SPARK)));
        return stone;
    }

    //Melts into water next to magma or when struck by lightning (two strikes); obsidian freezes the water back
    public gemstone ice () {
        gemstone stone = new gemstone("Ice", "#7CC8FF", itemParticles.FROST, handler,
            "Ice Shard", abilityType.ATTACK, fx().damage((u, t) -> 8*(1+u.getLevel()/4), "8 x (1 + lvl/4)"), 6, 0.95,
            "Freeze", abilityType.CONFUSE, fx().confuse(0.6, 1), 3, 0.85,
            "Frostbite", abilityType.POISON, fx().poison((u, t) -> 4 + u.getLevel()/3, "4 + lvl/3", 4), 2, 0.9
        );
        stone.lore("It never melts in your pocket. Near a fire is another story.");
        stone.transformsInto(itemInfluence.HEAT, 1, "#3C8CE8", () -> water()).heatedBy(itemInfluence.LIGHTNING, 1);
        //It cools things down like obsidian does: glass back into sandstone, water into ice, magma into coal
        stone.emits(itemInfluence.COLD, 2.0, 0.8);
        stone.lightTint(0.45);
        //Ice shards, snow coming down on them as they freeze stiff (sometimes a whole blizzard), and frostbite creeping up them
        stone.animations(
            a().lunge(0.5, 0.25).stream(ICE, 16, 0.09, SHARD, 0.35, 0.35).toss("#7CC8FF", 2, 0.26, 0.35, 0.4).impact(T, ICE, 28, 0.05).fountain(T, ICE, 15, 0.05, SHARD).shake(T, 0.035).rare(a().meteor("#7CC8FF", 0.4, 0.5, ICE)),
            a().beam(ICE, 0.4).rain(T, ICE, 50, 0.07, SNOW).swirl(T, ICE, 24, 0.05).orbit(T, ICE, 10, 0.07, STAR).quake(0.03).shake(T, 0.015).rare(a().rain(T, ICE, 180, 0.08, SNOW).swirl(T, ICE, 80, 0.06).rain(U, ICE, 60, 0.07, SNOW)),
            a().rain(T, ICE, 22, 0.06, SNOW).rise(T, ICE, 14, 0.06, SPARK).impact(T, ICE, 10, 0.05).rare(a()));
        return stone;
    }

    //Melted ice. It drips, and stays water until obsidian freezes it back into ice
    public gemstone water () {
        gemstone stone = new gemstone("Water", "#3C8CE8", itemParticles.WATER, handler,
            "Splash", abilityType.ATTACK, fx().damage((u, t) -> 7*(1+u.getLevel()/4), "7 x (1 + lvl/4)"), 6, 1,
            "Soak", abilityType.CLEANSE, fx().cleanse(), 3, 1,
            "Tide", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.15, "15% maxHP"), 2, 0.95
        );
        stone.lore("A rock that forgot to stay solid. Hold it level.");
        //Obsidian's cold freezes it back into ice
        stone.transformsInto(itemInfluence.COLD, 1, "#7CC8FF", () -> ice());
        //Saltstone next to it (or held over it) salts it
        stone.transformsInto(itemInfluence.SALT, 1, "#E8F2FF", () -> saltWater());
        stone.lightTint(0.3).seeThrough(0.65);
        //Swung around, it pours out behind it in a stream
        stone.trail("#4A9CF0", 16, 0.6);
        //A splash of water at them (sometimes over a rainbow), a soaking that washes you clean, and a tide of hearts round you
        stone.animations(
            a().lunge(0.6, 0.3).stream(WATER, 30, 0.09, DROP, 0.4, 0.7).impact(T, WATER, 34, 0.055).fountain(T, WATER, 25, 0.06, DROP).shake(T, 0.03).rare(a().stream(WATER, 90, 0.06, null, 0.6, 1.4)),
            a().rain(U, WATER, 50, 0.08, DROP).rise(U, CLEAN, 10, 0.06, SPARK).fountain(U, WATER, 40, 0.07, DROP).rare(a().stream(WATER, 60, 0.06, null, 0.7, 1.6)),
            a().swirl(U, WATER, 34, 0.06).rise(U, HEARTS, 12, 0.1, HEART).rare(a().rain(U, HEARTS, 40, 0.1, HEART)));
        return stone;
    }

    //Water that saltstone has salted. Salt water carries a boltstone's lightning like copper does (it draws it and
    //charges boltstones next to it). It does not freeze; healite's healing takes the salt out again
    public gemstone saltWater () {
        gemstone stone = new gemstone("Salt Water", "#5C9CE0", itemParticles.WATER, handler,
            "Brine Splash", abilityType.ATTACK, fx().damage((u, t) -> 7*(1+u.getLevel()/4), "7 x (1 + lvl/4)").confuse(0.15, 2), 6, 1,
            "Soak", abilityType.CLEANSE, fx().cleanse(), 3, 1,
            "Tide", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.15, "15% maxHP"), 2, 0.95
        );
        stone.lore("Water with a bite. It stings the eyes and carries a spark better than it should.");
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        stone.transformsInto(itemInfluence.HEALING, 1, "#3C8CE8", () -> water());
        stone.lightTint(0.35).seeThrough(0.7);
        stone.trail("#6AA8EC", 16, 0.6);
        //Salty brine at them that stings their eyes, the soaking and the tide of water
        stone.animations(
            a().lunge(0.6, 0.3).stream(WATER, 24, 0.09, DROP, 0.4, 0.7).stream(SALT, 16, 0.04, null, 0.4, 0.7).impact(T, WATER, 30, 0.055).orbit(T, STARS, 8, 0.07, STAR).fountain(T, SALT, 15, 0.045, null).shake(T, 0.03).rare(a().stream(WATER, 90, 0.06, null, 0.6, 1.4)),
            a().rain(U, WATER, 50, 0.08, DROP).rise(U, CLEAN, 10, 0.06, SPARK).fountain(U, WATER, 40, 0.07, DROP).rare(a().stream(WATER, 60, 0.06, null, 0.7, 1.6)),
            a().swirl(U, WATER, 34, 0.06).rise(U, HEARTS, 12, 0.1, HEART).rare(a().rain(U, HEARTS, 40, 0.1, HEART)));
        return stone;
    }

    //Gives off salt: water next to it (or held over it) turns into salt water, with white grains flying over
    public gemstone saltstone () {
        gemstone stone = new gemstone("Saltstone", "#F2EEE6", itemParticles.SALT, handler,
            "Salt Spray", abilityType.ATTACK, fx().damage((u, t) -> 8*(1+u.getLevel()/4), "8 x (1 + lvl/4)"), 6, 0.95,
            "Brine Sting", abilityType.POISON, fx().poison((u, t) -> 3 + u.getLevel()/3, "3 + lvl/3", 5), 3, 0.9,
            "Preserve", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.1, "10% maxHP"), 2, 0.9
        );
        stone.lore("A lump of old sea, dried out. Water near it never tastes the same again.");
        stone.emits(itemInfluence.SALT, 2.0, 0.8);
        stone.lightTint(0.5);
        //A spray of salt, a stinging brine that poisons them, and salt preserving you (clean, and a little healed)
        stone.animations(
            a().lunge(0.5, 0.25).stream(SALT, 55, 0.045, null, 0.3, 0.25).impact(T, SALT, 26, 0.045).fountain(T, SALT, 20, 0.045, null).shake(T, 0.03).rare(a().toss("#F2EEE6", 12, 0.2, 0.3, 0.3)),
            a().toss("#F2EEE6", 2, 0.22, 0.4, 0.6).stream(SALT, 18, 0.05, null, 0.35, 0.4).rise(T, POISON, 16, 0.09, BUBBLE).impact(T, SALT, 14, 0.045).shake(T, 0.025).rare(a()),
            a().swirl(U, SALT, 28, 0.05).rise(U, CLEAN, 10, 0.06, SPARK).rise(U, HEARTS, 6, 0.09, HEART).rare(a()));
        return stone;
    }

    //Magma that obsidian has cooled: burnt out and black, still smoking a little. A boltstone's lightning relights it
    public gemstone coal () {
        gemstone stone = new gemstone("Coal", "#3A3634", itemParticles.SMOKE, handler,
            "Coal Chunk", abilityType.ATTACK, fx().damage((u, t) -> 10*(1+u.getLevel()/4), "10 x (1 + lvl/4)"), 3, 0.95,
            "Soot Cloud", abilityType.CONFUSE, fx().confuse(0.35, 2), 3, 0.9,
            "Rekindle", abilityType.FIRE, fx().burn((u, t) -> 3 + u.getLevel()/3, "3 + lvl/3", 4), 1, 0.85
        );
        stone.lore("What is left of magma that met something colder. It remembers being on fire, faintly.");
        //Lightning sets it alight again: two strikes (from a boltstone) and it is magma once more
        stone.transformsInto(itemInfluence.LIGHTNING, 1, "#FF5020", () -> magma());
        //Healite's healing brings it back to life too, as slowly as obsidian sets magma
        stone.heatedBy(itemInfluence.HEALING, 1);
        //A big lump of coal thrown at them (now and then it shines like a diamond), a cloud of soot round them, and coal flaring up again to set them burning
        stone.animations(
            a().lunge(0.8, 0.35).toss("#3A3634", 1.4, 0.5, 0.45, 0.9).impact(T, COAL, 30, 0.06).rise(T, SMOKE, 8, 0.13, null).fountain(T, COAL, 15, 0.06, CHUNK).quake(0.06).shake(T, 0.06).rare(a().fountain(T, CLEAN, 50, 0.06, SPARK).toss("#F6FCFF", 2, 0.4, 0.45, 0.9)),
            a().swirl(T, SMOKE, 60, 0.1).rise(T, SMOKE, 14, 0.15, null).orbit(T, STARS, 8, 0.07, STAR).rare(a().swirl(T, SMOKE, 180, 0.12).swirl(U, SMOKE, 60, 0.1)),
            a().toss("#FF5020", 1.6, 0.28, 0.4, 0.6).stream(EMBER, 18, 0.07, FLAME, 0.35, 0.3).rise(T, FIRE, 24, 0.09, FLAME).rise(T, SMOKE, 8, 0.13, null).fountain(T, EMBER, 24, 0.05, FLAME).rare(a().meteor("#FF5020", 0.4, 0.45, FIRE)));
        return stone;
    }

    public gemstone healite () {
            gemstone stone = new gemstone("Healite", "#FF90B0", itemParticles.SPARKLE, handler,
            "Revitalize", abilityType.HEAL, fx().heal(moveEffects.ownCurrentHP(0.5), "50% current HP"), 1, 0.7,
            "Ressurection", abilityType.CLEANSE, fx().cleanse(), 1, 0.7,
            "Transcendence", abilityType.RESTORE, fx().restore(), 1, 0.7
        );
        stone.lore("A soft pink stone that hums quietly. Good for the soul.");
        //The world goes pink while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.tinted("#FF90B0", 0.22));
        //To the meteorite what copper is to the boltstone: it draws the purple lightning from anywhere on screen,
        //charges meteorites near it just by being there and passes strikes on (to other healite, and on to uranium)
        stone.emits(itemInfluence.COSMIC_CONDUCTION, 3, 1, 1.74).conducts(1.6, itemInfluence.COSMIC).hitDebris(0.3).reactsTo(itemInfluence.COSMIC, 0.5);
        //Its healing turns coal next to it back into magma (hearts fly over while it works)
        stone.emits(itemInfluence.HEALING, 2.0, 0.8);
        stone.transformsInto(itemInfluence.COSMIC, itemInteractions.HIT_HEAT, "#C070FF", () -> chargedHealite()).carriesState();
        //Hearts pouring up round you (sometimes raining down too), a pink shine that takes everything off, and stars that bring your rocks back
        stone.animations(
            a().rise(U, HEARTS, 26, 0.11, HEART).swirl(U, PINK, 18, 0.05).orbit(U, HEARTS, 10, 0.08, HEART).fountain(U, HEARTS, 24, 0.08, HEART).rare(a().rain(U, HEARTS, 70, 0.12, HEART)),
            a().rise(U, CLEAN, 22, 0.07, SPARK).swirl(U, CLEAN, 18, 0.05).fountain(U, CLEAN, 24, 0.06, SPARK).rare(a().fountain(U, PINK, 60, 0.07, STAR)),
            a().orbit(U, PINK, 26, 0.09, STAR).rise(U, CLEAN, 22, 0.06, SPARK).fountain(U, PINK, 30, 0.06, STAR).rare(a().orbit(U, GOLD, 60, 0.1, STAR)));
        return stone;
    }

    //Healite just hit by cosmic lightning: it glows purple and its moves work more often. It turns back into healite
    //about 5 seconds after the last purple strike; every new one fills it up again
    public gemstone chargedHealite () {
        gemstone stone = new gemstone("Charged Healite", "#C878FF", itemParticles.SPARKLE, handler,
            "Revitalize", abilityType.HEAL, fx().heal(moveEffects.ownCurrentHP(0.6), "60% current HP"), 1, 0.8,
            "Ressurection", abilityType.CLEANSE, fx().cleanse(), 1, 0.8,
            "Transcendence", abilityType.RESTORE, fx().restore(), 1, 0.8
        );
        stone.lore("Healite humming with starlight. It will not last.");
        //Pink going purple, like the charge while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.tinted("#E070D0", 0.25));
        stone.emits(itemInfluence.COSMIC_CONDUCTION, 3, 1, 1.74).conducts(1.6, itemInfluence.COSMIC).hitDebris(0.3).reactsTo(itemInfluence.COSMIC, 0.5);
        stone.emits(itemInfluence.HEALING, 2.0, 0.8);
        stone.revertsInto(itemInfluence.COSMIC, 1, "#FF90B0", () -> healite()).heatedBy(itemInfluence.COSMIC, 2).coolsIn(5).carriesState();
        stone.restingGlow(0.35);
        //Healite's moves with starlight in them
        stone.animations(
            a().rise(U, HEARTS, 30, 0.11, HEART).swirl(U, COSMIC, 20, 0.05).orbit(U, HEARTS, 12, 0.08, HEART).fountain(U, COSMIC, 26, 0.07, STAR).rare(a().rain(U, HEARTS, 80, 0.12, HEART).skybolt("#C878FF", 4)),
            a().rise(U, CLEAN, 24, 0.07, SPARK).swirl(U, PINK, 20, 0.05).fountain(U, PINK, 26, 0.06, SPARK).rare(a().fountain(U, PINK, 70, 0.07, STAR)),
            a().orbit(U, COSMIC, 30, 0.09, STAR).rise(U, PINK, 22, 0.06, SPARK).fountain(U, COSMIC, 34, 0.06, STAR).rare(a().orbit(U, GOLD, 70, 0.1, STAR)));
        return stone;
    }

    //Holds two other rocks and becomes them, see elementalRock
    public gemstone elemental () {
        return new elementalRock(handler);
    }

    public gemstone pebble () {
        gemstone stone = new gemstone("Pebble", "#C0C0C0", itemParticles.NONE, handler,
        "Stone Skip", abilityType.ATTACK, fx().damage((u, t) -> u.getLevel()*2+5, "5 + 2 x lvl"), 8, 0.95,
        "Rock Remedy", abilityType.FIRE, fx().damage((u, t) -> u.getLevel()*1.5, "1.5 x lvl").burn((u, t) -> u.getLevel()/2 + 1, "1 + lvl/2", 3), 3, 0.95,
        "Mineral Mend", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.20, "20% maxHP"), 3, 0.95
    );
        stone.lore("Found on a beach. It skips well.");
        //A pebble skipped along the ground at them (now and then a whole run of them), a hot one thrown that sets them burning, and the stone mending you
        stone.animations(
            a().lunge(0.9, 0.3).skip("#C0C0C0", 1.2, 0.2).impact(T, PEBBLE, 16, 0.05).fountain(T, PEBBLE, 10, 0.05, CHUNK).shake(T, 0.03).rare(a().skip("#C0C0C0", 9, 0.2)),
            a().lunge(0.7, 0.3).toss("#FF6A20", 2, 0.24, 0.4, 0.6).impact(T, EMBER, 20, 0.05).rise(T, FIRE, 14, 0.09, FLAME).fountain(T, EMBER, 14, 0.05, FLAME).shake(T, 0.03).rare(a().toss("#FF6A20", 10, 0.2, 0.35, 0.5)),
            a().rise(U, HEARTS, 14, 0.1, HEART).swirl(U, PEBBLE, 14, 0.05).orbit(U, PEBBLE, 6, 0.06, CHUNK).rare(a()));
        return stone;
}

}
