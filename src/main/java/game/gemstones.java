package game;

import static game.battleAnimation.*;
import static game.battleFx.*;

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
        "Pebble Strike", abilityType.ATTACK, fx().damage((u, t) -> 11.5 + u.getLevel()*2, "11.5 + 2 x lvl"), 10, 1,
        "Stone Shatter", abilityType.ATTACK, fx().damage((u, t) -> 19 + u.getLevel()*3.5, "19 + 3.5 x lvl"), 5, 1,
        "Stone Renewal", abilityType.HEAL, fx().heal((u, t) -> 9 + u.getMaxHealth()*0.09, "9 + 9% maxHP"), 3, 1
        );
        stone.lore("A plain grey stone your grandma kept in a drawer for sixty years. Reliable, if nothing else.");
        //Heated by magma, or by lightning (two strikes, like ice melting), it melts into lava. Its uses left go with it
        stone.transformsInto(itemInfluence.HEAT, 1, "#FF6A20", () -> lava()).heatedBy(itemInfluence.LIGHTNING, 1).carriesState();
        //A pebble flicked in a hop that bounces off the ground (a second one after it), a big rock lobbed high that leaves a crater, and the ground's grit whirling in to mend you
        stone.animations(
            a().then(lob("#9A9A9A", 0, 0.3, 0.6, 0.6, 1, false)).then(shards(T, GREY, 0.6, 5, 1.2, false, false)).then(hit(0.6)).then(lob("#9A9A9A", 1.1, 0.26, 0.5, 0.55, 1, false)).then(shards(T, GREY, 1.65, 4, 1.2, false, false)).then(hit(1.65)).rare(a().then(lob("#9A9A9A", 0.2, 0.34, 0.9, 1.1, 4, false)).then(shards(T, GREY, 1.3, 10, 1.6, false, false))),
            a().then(lob("#9A9A9A", 0.1, 0.9, 1.8, 0.75, 0, false)).then(shards(T, GREY, 0.85, 12, 1.8, false, false)).then(hit(0.85)).then(kick(0.85, 0.12)).rare(a().then(lob("#9A9A9A", 0.9, 1.2, 2.2, 0.7, 0, false)).then(shards(T, GREY, 1.6, 16, 2.2, false, false)).then(kick(1.6, 0.18))),
            a().then(tint(U, "#D0D0C0", 0.7, 0.6, 0.6, false, true)).then(drift(U, HEARTS, 0.6, 1.0, UP, 10)).rare(a().then(encase(U, "#9A9A9A", 0.3, 1.2, 0.85, false)).then(shards(U, GREY, 1.5, 12, 1.3, false, false))));
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
        "Molten Strike", abilityType.FIRE, fx().damage((u, t) -> 4 + u.getLevel()*2, "4 + 2 x lvl").burn((u, t) -> 1 + u.getLevel()*0.5, "1 + 0.5 x lvl", 3)
            .from(5).burn(theirMaxHP(0.025), "2.5% their maxHP", 3), 10, 1,
        "Eruption", abilityType.FIRE, fx().damage((u, t) -> 14.5 + u.getLevel()*2.5, "14.5 + 2.5 x lvl").burn((u, t) -> 4 + u.getLevel()*1, "4 + 1 x lvl", 3)
            .from(5).moreUses(1), 5, 0.9,
        "Cooling Crust", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.1, "10% maxHP"), 3, 1
        );
        stone.lore("Grandma's rock, molten. It will set again the moment something cold comes near.");
        //Night with a red glow, like magma while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.5, "#FF2000", 0.18));
        stone.transformsInto(itemInfluence.COLD, LAVA_COOL_DOSE, "#9A9A9A", () -> rock()).carriesState();
        stone.emits(itemInfluence.HEAT, 2.0, 1.0);
        stone.trail("#FF6A20", 16, 0.6);
        //A glowing blob thrown that leaves a pool of lava under them, lava bursting up round them, and lava climbing you and setting to a crust
        stone.animations(
            a().then(lob("#FF6A20", 0, 0.4, 0.8, 0.6, 0, false)).then(pool(T, LAVA, 0.6, 18, 1.6, null)).then(drift(T, EMBER, 0.6, 1.2, UP, 12)).then(hit(0.6)).rare(a().then(lob("#FF6A20", 0.08, 0.32, 1.1, 0.55, 0, false)).then(lob("#FF6A20", 0.16, 0.3, 0.5, 0.5, 0, false)).then(pool(T, LAVA, 0.7, 34, 1.5, null))),
            a().then(shards(T, LAVA, 0.6, 16, 2.6, false, false)).then(pool(T, LAVA, 0.6, 26, 1.4, null)).then(glow(T, "#FF6A20", 0.6, 1.0, 2.5, 0, true)).then(kick(0.6, 0.12)).then(hit(0.6)).rare(a().then(shards(T, LAVA, 1.0, 20, 3.2, false, false)).then(pool(T, LAVA, 1.0, 40, 1.4, null))),
            a().then(pool(U, LAVA, 0, 30, 2.0, "#5A5050")).then(tint(U, "#FF6A20", 0, 0.7, 0.5, false, true)).then(tint(U, "#6E6E6E", 0.9, 0.6, 0.5, false, false)).then(shards(U, SMOKE, 1.5, 8, 1.0, false, false)).then(drift(U, STEAM, 0.6, 1.2, UP, 10)).then(drift(U, HEARTS, 1.2, 0.8, UP, 8)).rare(a().then(encase(U, "#9A9A9A", 0.4, 1.0, 0.9, false)).then(shards(U, GREY, 1.4, 14, 1.4, false, false))));
        return stone;
    }

    public gemstone obsidian () {
        gemstone stone = new gemstone("Obsidian", "#5A4A6A", itemParticles.SMOKE, handler,
        "Heat Wave Slam", abilityType.ATTACK, fx().damage((u, t) -> 12.5 + u.getLevel()*2, "12.5 + 2 x lvl")
            .from(5).burn(theirMaxHP(0.04), "4% their maxHP", 2), 8, 0.95,
        "Inferno Blast", abilityType.FIRE, fx().burn((u, t) -> 4.5 + u.getLevel()*1, "4.5 + 1 x lvl", 5)
            .from(5).moreUses(1), 2, 0.95,
        "Warm Embrace", abilityType.HEAL, fx().healing((u, t) -> u.getMaxHealth()*0.035, "3.5% maxHP", 4), 3, 0.95
        );
        stone.lore("Volcanic glass, still warm to the touch. It remembers the fire it came from.");
        //Night, the moves glowing like embers in the dark while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.5));
        //Near magma it heats up and smokes. It cools whatever is next to it: glass back into sandstone, water into ice
        stone.shakeParticles(5).reactsTo(itemInfluence.HEAT, 1).emits(itemInfluence.COLD, 2.0, 0.8);
        //A slam of hot glass, a glow waking in you before a cone of fire roars at them, and warm embers whirling round you
        stone.animations(
            a().lunge(0.7, 0.45).toss("#5A4A6A", 1.6, 0.45, 0.35, 0.5).impact(T, OBSIDIAN, 30, 0.06).fountain(T, OBSIDIAN, 18, 0.06, SHARD).quake(0.06).shake(T, 0.06).rare(a().fountain(T, FIRE, 50, 0.07, FLAME)),
            a().then(glow(U, "#FF4010", 0, 0.8, 1.5, 2, false)).then(drift(U, FIRE, 0.8, 0.9, TOWARD, 60)).then(glow(T, "#FC9F00", 1.0, 1.2, 2.2, 0, true)).then(tint(T, "#FC9F00", 1.0, 0.8, 0.5, true, false)).then(drift(T, FIRE, 1.0, 1.0, UP, 12)).rare(a().then(shards(U, OBSIDIAN, 1.0, 12, 3.0, true, true))),
            a().then(glow(U, "#FF8040", 0.2, 1.8, 2.0, 4, false)).then(drift(U, EMBER, 0.3, 1.5, UP, 10)).then(drift(U, HEARTS, 0.6, 1.2, UP, 8)).rare(a().then(encase(U, "#5A4A6A", 0.2, 1.8, 0.55, false)).then(shards(U, OBSIDIAN, 2.0, 14, 1.4, false, true))));
        return stone;
    }

    public gemstone boltstone () {
        gemstone stone = new gemstone("Boltstone", "#F0D040", itemParticles.SPARKLE, handler,
        "Static Burst", abilityType.FIRE, fx().burn(theirMaxHP(0.02), "2% their maxHP", 3).damage(theirMaxHP(0.075), "7.5% their maxHP"), 8, 1,
        "Thunderbolt", abilityType.LIGHTNING, fx().damage(theirMaxHP(0.195), "19.5% their maxHP")
            .from(10).moreUses(1), 3, 0.8,
        "Electrocuter", abilityType.LIGHTNING, fx().damage(theirMaxHP(0.525), "52.5% their maxHP")
            .from(20).hits(0.6), 1, 0.5
        );
        stone.lore("Struck by lightning so many times it started striking back.");
        stone.shakeParticles(1).shakeLightning(0.6)
            .reactsTo(itemInfluence.RADIATION, 1).reactsTo(itemInfluence.LIGHTNING, 0.8).reactsTo(itemInfluence.CONDUCTION, 1);
        //Static crackling on you before it jumps to them, a storm cloud and lightning out of it, and you crackling yellow before lightning is held on them while they flash between yellow and black
        stone.animations(
            a().then(tint(U, "#FFE500", 0, 0.5, 0.4, true, false)).bolt("#FFE500", 5).then(tint(T, "#FFE500", 0.55, 0.5, 0.5, true, false)).then(lift(T, 0.55, 0.05, 0.4)).then(drift(T, FIRE, 0.6, 0.8, UP, 10)).then(hit(0.55)).rare(a().bolt("#FFE500", 5).bolt("#FFFFFF", 5)),
            a().then(cloud(STORM, 0, 1.4, ABOVE, 1.4)).skybolt("#FFE500", 3).then(flash("#FFFFFF", 0.6, 0.6)).then(hit(0.6)).then(kick(0.6, 0.1)).dark(mood.night(0.55)).rare(a().skybolt("#FFE500", 3).skybolt("#FFFFFF", 3).then(flash("#FFFFFF", 1.3, 0.9))),
            a().then(tint(U, "#FFE500", 0, 0.5, 0.5, true, false)).bolt("#FFE500", 5).then(tint(T, "#FFE500", 0.5, 0.9, 0.7, true, false)).then(tint(T, "#000000", 0.56, 0.84, 0.8, true, false)).then(kick(0.5, 0.1)).then(kick(0.8, 0.1)).then(kick(1.1, 0.1)).then(hit(0.5)).dark(mood.night(0.5)).rare(a().skybolt("#FFFFFF", 3)));
        return stone;
    }

    public gemstone giantBolder () {
        gemstone stone = new gemstone("Giant Bolder", "#8A6A4A", itemParticles.NONE, handler, 
        "Bolder Smash", abilityType.ATTACK, fx().damage((u, t) -> 11 + u.getLevel()*1, "11 + 1 x lvl")
            .from(5).damage(theirMaxHP(0.075), "7.5% their maxHP").moreUses(1).from(10).damage(theirMaxHP(0.02), "2% their maxHP"), 6, 1,
        "Crushing Blow", abilityType.CONFUSE, fx().damage((u, t) -> 12.5 + u.getLevel()*0.5, "12.5 + 0.5 x lvl").confuse(0.15, 6), 5, 0.9,
        "Mountainous Maul", abilityType.DRAIN, fx().heal(theirMaxHP(0.09), "9% their maxHP").damage(theirMaxHP(0.165), "16.5% their maxHP")
            .from(5).moreUses(1), 2, 0.7
        );
        stone.lore("Not actually giant. The name was chosen by someone who had just been hit by it.");
        //A quarter bigger than every other rock: it hangs a little over its box
        stone.iconScale(1.25);
        //The boulder rolled into them, knocking them back into a crater, a shadow growing before it crushes down on them and leaves them reeling, and big boulders crashing down on them out of the sky before their hearts fly to you
        stone.animations(
            a().then(lob("#8A6A4A", 0, 1.0, 0, 0.7, -1, false)).then(drift(U, BROWN, 0.1, 0.6, TOWARD, 30)).then(knock(T, 0.7, 0.35, 0.5)).then(shards(T, BROWN, 0.7, 10, 1.5, false, false)).then(kick(0.7, 0.12)).then(hit(0.7)).rare(a().then(stretch(T, 0.7, 0.6, SQUASH))),
            a().then(drop(T, "#8A6A4A", 0, 0.8, 1.5)).then(kick(0.95, 0.15)).then(hit(0.95)).then(stretch(T, 1.05, 0.8, TWIST)).rare(a().then(drop(T, "#8A6A4A", 0, 0.8, 3.0)).then(flash("#8A6A4A", 0.95, 0.6))),
            a().then(streak(T, "#8A6A4A", 0, 0.4, 0.9, BROWN)).then(shards(T, BROWN, 0.4, 10, 1.6, false, false)).then(kick(0.4, 0.12)).then(hit(0.4)).then(streak(T, "#8A6A4A", 0.45, 0.4, 1.1, BROWN)).then(shards(T, BROWN, 0.85, 12, 1.8, false, false)).then(kick(0.85, 0.14)).then(hit(0.85)).then(drop(T, "#8A6A4A", 0.5, 0.75, 1.6)).then(shards(T, BROWN, 1.45, 16, 2.2, false, false)).then(kick(1.45, 0.2)).then(hit(1.45)).back(HEARTS, 14, 0.1, HEART, 0.6, 0.6).then(tint(U, "#B99B65", 1.6, 0.6, 0.5, false, true)).then(drift(U, HEARTS, 1.6, 0.8, UP, 8)).rare(a().then(streak(T, "#A08860", 0.2, 0.4, 0.8, BROWN)).then(streak(T, "#6E5A48", 0.65, 0.4, 0.8, BROWN)).then(flash("#8A6A4A", 1.45, 0.5))));
        return stone;
    }

    
    public gemstone discombobulateShard () {
        gemstone stone = new gemstone("Discombobulate Shard", "#D040D0", itemParticles.ARCANE, handler,
            "Mystic Mind Maze", abilityType.CONFUSE, fx().confuse(0.25, 4).damage((u, t) -> 11 + u.getLevel()*1, "11 + 1 x lvl")
                .from(15).damage(theirMaxHP(0.045), "4.5% their maxHP"), 3, 0.9,
            "Mirage Assault", abilityType.POISON, fx().heal((u, t) -> 3.5 + u.getMaxHealth()*0.025, "3.5 + 2.5% maxHP").poison((u, t) -> 4 + u.getLevel()*0.5, "4 + 0.5 x lvl", 8).burn((u, t) -> 4 + u.getLevel()*0.5, "4 + 0.5 x lvl", 8).damage((u, t) -> u.getLevel()*2, "2 x lvl")
                .from(20).moreUses(1).from(25).poison(theirMaxHP(0.035), "3.5% their maxHP", 8), 2, 0.85,
            "Mind Warp", abilityType.CONFUSE, fx().confuse(0.5, 4)
                .from(25).moreUses(1), 1, 0.75
        );
        stone.lore("Looking at it for too long makes you forget what you were doing.");
        //A shard that hits them and leaves copies of them swaying round them, mirror images of you stepping out before you dash at them, and them twisted like a corkscrew in a whirl
        stone.animations(
            a().then(lob("#D040D0", 0, 0.22, 0.6, 0.4, 0, false)).then(hit(0.4)).then(twin(T, "#D040D0", 0.4, 1.2, 0.5)).then(stretch(T, 0.4, 1.2, TWIST)).shake(T, 0.02).rare(a().then(twin(T, "#D040D0", 1.0, 1.0, 0.8))),
            a().then(twin(U, "#D040D0", 0, 1.0, 0.9)).then(twin(U, "#887BFE", 0.1, 1.0, 0.9)).then(dash(0.3, "#D040D0")).then(hit(0.52)).then(tint(T, "#D060FF", 0.55, 0.8, 0.5, true, false)).then(tint(T, "#B4A8FF", 0.58, 0.8, 0.5, true, false)).then(drift(T, PURPLE, 0.6, 1.0, UP, 10)).then(drift(U, HEARTS, 0.8, 0.8, UP, 6)).rare(a().then(twin(U, "#D060FF", 0.2, 1.2, 1.3)).then(twin(U, "#B4A8FF", 0.3, 1.2, 1.6))),
            a().then(stretch(T, 0, 1.6, TWIST)).then(kick(0.4, 0.06)).then(kick(0.9, 0.06)).then(kick(1.4, 0.06)).rare(a().then(stretch(T, 1.6, 1.0, TWIST))));
        return stone;
    }

    //The moon and the sun as rocks. Seen from space (spaceView) the moon and the sun are these two, huge
    public static final String MOON_COLOR = "#D8DCE4", SUN_COLOR = "#FFC830";

    public gemstone moonstone () {
        gemstone stone = new gemstone("Moonstone", MOON_COLOR, itemParticles.SPARKLE, handler,
            "Moonbeam", abilityType.CONFUSE, fx().damage((u, t) -> 9 + u.getLevel()*1.5, "9 + 1.5 x lvl").confuse(0.15, 2)
                .from(15).damage(theirMaxHP(0.045), "4.5% their maxHP"), 8, 0.95,
            "Lunar Tide", abilityType.CONFUSE, fx().confuse(0.45, 3).heal((u, t) -> u.getMaxHealth()*0.08, "8% maxHP"), 3, 0.9,
            "Eclipse", abilityType.CONFUSE, fx().confuse(0.7, 3).damage(theirMaxHP(0.165), "16.5% their maxHP")
                .from(20).moreUses(1), 1, 0.7
        );
        stone.lore("A piece of the moon that came down one night. It is coldest when the moon is full.");
        //Like obsidian to the rocks round it: cold (glass back into sandstone, water into ice, lava back into rock),
        //and near magma's heat it heats up and smokes
        stone.shakeParticles(5).reactsTo(itemInfluence.HEAT, 1).emits(itemInfluence.COLD, 2.0, 0.8);
        stone.glowColor("#D8F4FF").restingGlow(0.3);
        //Moonlit night while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.6, "#8EDCF2", 0.08));
        //The moon pouring its light down on them, the moon over you going through its phases as a silver tide washes over them, and the moon itself coming down out of space for the eclipse
        stone.animations(
            a().skyBeam(T, 0.28, "#8EDCF2", "#D8F4FF").shaft(T, MOONLIGHT, 0.4).rain(T, STARS, 18, 0.06, STAR).impact(T, MOONLIGHT, 24, 0.05).shake(T, 0.03).rare(a().fromSpace(MOON_COLOR, 0.35, 0.3, MOONLIGHT)),
            a().then(disc(U, MOON, 0, 2.4, 0.4, 4)).then(wave(MOONLIGHT, 0.3, 0.7, 2.4, false, false)).then(tint(T, "#D8F4FF", 1.0, 0.5, 0.5, true, false)).then(tint(U, "#D8F4FF", 1.3, 0.6, 0.5, false, true)).then(drift(U, HEARTS, 1.3, 0.8, UP, 6)).rare(a().then(wave(MOONLIGHT, 1.0, 0.6, 3.0, false, false))),
            a().fromSpace(MOON_COLOR, 0.9, 0.9, MOONLIGHT).shaft(T, MOONLIGHT, 0.8).impact(T, MOONLIGHT, 60, 0.07).quake(0.12).shake(T, 0.08).rare(a().fromSpace(MOON_COLOR, 0.2, 0.4, STARS).quake(0.16)));
        return stone;
    }

    public gemstone sunstone () {
        gemstone stone = new gemstone("Sunstone", SUN_COLOR, itemParticles.FIRE, handler,
            "Sunbeam", abilityType.FIRE, fx().damage((u, t) -> 2 + u.getLevel()*2, "2 + 2 x lvl").burn((u, t) -> 2 + u.getLevel()*0.5, "2 + 0.5 x lvl", 2)
                .from(10).damage(theirMaxHP(0.02), "2% their maxHP").from(20).damage(theirMaxHP(0.035), "3.5% their maxHP"), 8, 0.95,
            "Sunlight", abilityType.HEAL, fx().healing((u, t) -> u.getMaxHealth()*0.03, "3% maxHP", 5)
                .from(15).moreUses(1), 4, 0.95,
            "Daybreak", abilityType.HEAL, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.17, "17% maxHP")
                .from(5).moreUses(1).from(25).moreUses(1), 2, 0.85
        );
        stone.lore("Warm in the hand even at night. It has never once been in the shade.");
        //Like magma to the rocks round it: hot (melts ice, sand into glass, rock into lava), and charged by lightning.
        //Unlike magma nothing cools it: obsidian or ice next to it never turn it into another rock
        stone.shakeParticles(6).emits(itemInfluence.HEAT, 2.0, 0.8).reactsTo(itemInfluence.LIGHTNING, 1);
        stone.glowColor("#FFE500").restingGlow(0.4);
        //The world goes golden while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.tinted("#FFD040", 0.22));
        //A sunbeam out of the sky, sunlight pouring down on you as it heals, and the sun rising behind you and burning everything off you
        stone.animations(
            a().skyBeam(T, 0.35, "#FC9F00", "#FFE500").shaft(T, SUNLIGHT, 0.7).impact(T, GOLD, 50, 0.06).rise(T, FIRE, 12, 0.08, FLAME).quake(0.05).shake(T, 0.05).rare(a().fromSpace(SUN_COLOR, 0.2, 0.5, FIRE).quake(0.12)),
            a().then(column(U, SUNLIGHT, 0, 2.0, 1.4, 3.5, true, 0.55)).then(drift(U, GOLD, 0.2, 1.8, UP, 12)).then(tint(U, "#FFE500", 0.4, 0.3, 0.4, false, false)).then(tint(U, "#FFE500", 0.9, 0.3, 0.4, false, false)).then(tint(U, "#FFE500", 1.4, 0.3, 0.4, false, false)).then(drift(U, HEARTS, 0.5, 1.2, UP, 8)).rare(a().then(flash("#FFE500", 1.0, 0.4))),
            a().then(disc(U, SUN, 0, 2.2, 0.5, 0)).then(peel(U, GRIME, 0.4, 1.4, false)).then(flash("#FFE500", 1.6, 0.35)).rare(a().then(flash("#FFFFFF", 1.0, 0.7)).then(disc(U, SUN, 0, 2.2, 0.9, 0))));
        return stone;
    }

    //A black hole as a rock. Seen from space (spaceView) one hangs far off, huge, pulling a disc of light into it
    public static final String SINGULARITY_COLOR = "#2A2238";

    public gemstone singularity () {
        gemstone stone = new gemstone("Singularity", SINGULARITY_COLOR, itemParticles.ARCANE, handler,
            "Gravity Well", abilityType.ATTACK, fx().damage((u, t) -> 10 + u.getLevel()*2, "10 + 2 x lvl")
                .from(20).damage(theirMaxHP(0.04), "4% their maxHP"), 8, 0.95,
            "Spaghettify", abilityType.DRAIN, fx().heal(theirMaxHP(0.05), "5% their maxHP").damage(theirMaxHP(0.09), "9% their maxHP")
                .from(15).heal(theirMaxHP(0.02), "2% their maxHP").damage(theirMaxHP(0.04), "4% their maxHP"), 3, 0.85,
            "Collapse", abilityType.ATTACK, fx().costs((u, t) -> u.getMaxHealth()*0.1, "10% maxHP").damage(theirMaxHP(0.4), "40% their maxHP")
                .from(25).moreUses(1).hits(0.75), 1, 0.6
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
        //A well with moonstones and meteorites spiralling into it, bits of them drawn out towards you and back on a smooth noise, and a black core swallowing a whole disc of light
        stone.animations(
            a().singularity(T, 0.18).pullItems(T, 14, 0.14, 1.1).then(kick(1.08, 0.1)).then(hit(1.08)).rare(a().pullItems(T, 14, 0.16, 2.0)),
            a().then(noodle(T, 0.9)).then(tint(U, "#887BFE", 0.6, 0.5, 0.5, false, true)).then(drift(U, HEARTS, 0.6, 0.8, UP, 6)).rare(a().then(drift(U, HEARTS, 0.6, 1.2, UP, 14))),
            a().fromSpace(SINGULARITY_COLOR, 0.9, 0.9, VOID).singularity(T, 0.45).pull(T, 22, 0.14, 1.6).impact(U, HURT, 10, 0.05).fountain(T, VOID, 30, 0.06, STAR).quake(0.12).shake(T, 0.09).shake(U, 0.03).rare(a().pull(T, 34, 0.15, 2.2).quake(0.18)));
        return stone;
    }

    //A piece of the mirrors the crystal boy went through, in the mirror dimension's cold glassy blue
    public gemstone mirror () {
        gemstone stone = new gemstone("Mirror Shard", "#C8E4F4", itemParticles.SPARKLE, handler,
            "Glint", abilityType.ATTACK, fx().damage((u, t) -> 7 + u.getLevel()*2, "7 + 2 x lvl")
                .from(10).damage(theirMaxHP(0.015), "1.5% their maxHP").from(20).damage(theirMaxHP(0.04), "4% their maxHP"), 8, 0.95,
            "Mirror Image", abilityType.CONFUSE, fx().confuse(0.3, 2).heal((u, t) -> u.getMaxHealth()*0.045, "4.5% maxHP"), 3, 0.9,
            "Reflection", abilityType.CLEANSE, fx().reflect().damage(theirMaxHP(0.125), "12.5% their maxHP")
                .from(25).damage(theirMaxHP(0.075), "7.5% their maxHP").moreUses(1), 2, 0.85
        );
        stone.lore("Whatever you see in it is looking back. Sometimes it moves first.");
        stone.glowColor("#F6FCFF").restingGlow(0.25);
        //The world goes the mirror dimension's cold blue while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.tinted("#8EDCF2", 0.2));
        //A mirror image of them flying out of a mirror at them, mirror images of you swapping sides round you, and a beam off the mirror that throws your troubles back at them in shards
        stone.animations(
            a().mirrorCopy(1.4, 0.55).stream(MIRROR, 8, 0.05, SHARD, 0.35, 0.3).shake(T, 0.03).rare(a().mirrorCopy(0.7, 0.4)),
            a().then(twin(U, "#C8E4F4", 0, 1.6, 0.9)).then(twin(U, "#F6FCFF", 0.4, 1.4, 1.2)).then(tint(T, "#C8E4F4", 0.6, 0.6, 0.5, true, false)).then(stretch(T, 0.6, 0.8, TWIST)).then(drift(U, HEARTS, 0.8, 0.8, UP, 6)).rare(a().then(twin(U, "#8EDCF2", 0.2, 1.6, 1.6)).then(twin(U, "#B8C0D0", 0.6, 1.2, 2.0))),
            a().then(ray(MIRROR, 0, 0.6, 3, FROM_USER)).then(shards(U, MIRROR, 0.6, 12, 2.6, true, true)).then(drift(U, MIRROR, 0.5, 0.9, TOWARD, 60)).then(hit(0.9)).rare(a().then(twin(T, "#C8E4F4", 1.0, 0.8, 0.6)).then(hit(1.4))));
        return stone;
    }

    public gemstone meteorite () {
        gemstone stone = new gemstone("Meteorite", "#6A7A9A", itemParticles.SPARKLE, handler, 
            "Gravitational Slam", abilityType.ATTACK, fx().damage(theirMaxHP(0.09), "9% their maxHP").confuse(0.05, 3)
                .from(5).damage(theirMaxHP(0.01), "1% their maxHP").from(15).damage(theirMaxHP(0.02), "2% their maxHP"), 10, 0.99,
            "Cosmic Crush", abilityType.CONFUSE, fx().costs((u, t) -> u.getMaxHealth()*0.05, "5% maxHP").confuse(0.1, 5).damage(theirMaxHP(0.175), "17.5% their maxHP")
                .from(10).moreUses(1).from(20).damage(theirMaxHP(0.03), "3% their maxHP"), 3, 1,
            "Orbitin Assault", abilityType.ATTACK, fx().costs((u, t) -> u.getMaxHealth()*0.2, "20% maxHP").damage(theirMaxHP(0.5), "50% their maxHP"), 1, 1
        );
        stone.lore("Fell from the sky one night and has been falling, in spirit, ever since.");
        //Night with a hint of deep space while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.6, "#3A2A80", 0.12));
        //Throws purple (cosmic) lightning, like a boltstone throws its own: when shaken, or charged by radiation from
        //uranium or by healite pulling at it. Where it strikes, the sparks are every colour
        stone.shakeParticles(1).shakeLightning(0.6, itemInfluence.COSMIC, "#B060FF")
            .reactsTo(itemInfluence.RADIATION, 1).reactsTo(itemInfluence.COSMIC, 0.8).reactsTo(itemInfluence.COSMIC_CONDUCTION, 1);
        //Three meteorites sent down from space onto them one after another, a meteor streaking down onto them out of a growing shadow (costing you some health), and meteorites torn out of you circling faster and faster before they all crash into them
        stone.animations(
            a().then(streak(T, "#6A7A9A", 0, 0.45, 0.45, COSMIC)).then(hit(0.45)).then(shards(T, COSMIC, 0.45, 5, 1.2, false, false))
                .then(streak(T, "#6A7A9A", 0.15, 0.45, 0.45, COSMIC)).then(hit(0.6)).then(shards(T, COSMIC, 0.6, 5, 1.2, false, false))
                .then(streak(T, "#6A7A9A", 0.3, 0.45, 0.55, COSMIC)).then(hit(0.75)).then(shards(T, COSMIC, 0.75, 8, 1.5, false, false)).then(kick(0.75, 0.12))
                .rare(a().then(streak(T, "#B4A8FF", 0.45, 0.4, 0.35, COSMIC)).then(streak(T, "#6A7A9A", 0.55, 0.4, 0.35, COSMIC)).then(hit(0.95))),
            a().then(tint(U, "#887BFE", 0, 0.4, 0.5, true, false)).then(streak(T, "#6A7A9A", 0.3, 0.6, 0.7, COSMIC)).then(shards(T, COSMIC, 0.9, 10, 1.6, false, false)).then(flash("#B4A8FF", 0.9, 0.5)).skybolt("#887BFE", 3).then(hit(0.9)).rare(a().then(streak(T, "#6A7A9A", 0.0, 0.4, 0.35, COSMIC)).then(streak(T, "#6A7A9A", 0.15, 0.4, 0.35, COSMIC)).then(streak(T, "#B4A8FF", 0.3, 0.4, 0.35, COSMIC))),
            a().then(tint(U, "#FF4060", 0, 0.2, 0.6, false, false)).then(spin(U, "#6A7A9A", 0, 1.0, 5, 0.3, 1.0, true)).then(hit(1.15)).then(hit(1.3)).then(kick(1.4, 0.15)).then(shards(T, COSMIC, 1.3, 12, 1.8, false, false)).rare(a().then(spin(U, "#B4A8FF", 0, 1.0, 12, 0.22, 1.6, true))));
        return stone;
    }
    
    public gemstone magma () {
        gemstone stone = new gemstone("Magma", "#FF5020", itemParticles.FIRE, handler,
            "Molten Fury", abilityType.FIRE, fx().burn((u, t) -> 7.5 + u.getLevel()*1.5, "7.5 + 1.5 x lvl", 3)
                .from(15).burn(theirMaxHP(0.135), "13.5% their maxHP", 3), 3, 0.85,
            "Magma Blast", abilityType.FIRE, fx().burn((u, t) -> 6 + u.getLevel()*1.5, "6 + 1.5 x lvl", 6)
                .from(20).moreUses(1), 3, 0.8,
            "Volcanic Impact", abilityType.FIRE, fx().burn((u, t) -> 7.5 + u.getLevel()*2, "7.5 + 2 x lvl", 6)
                .from(15).burn(theirMaxHP(0.14), "14% their maxHP", 6).from(20).moreUses(1), 1, 0.75
        );
        stone.lore("Molten rock that never quite set. Do not store near paper.");
        //Night with a red glow while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.night(0.5, "#FF2000", 0.18));
        //Obsidian's cold sets it into coal
        stone.transformsInto(itemInfluence.COLD, 1.5, "#2E2A28", () -> coal());
        stone.shakeParticles(6).emits(itemInfluence.HEAT, 2.0, 0.8).reactsTo(itemInfluence.LIGHTNING, 1);
        //Magma bubbling up round them, a hose of magma that pools round them, and a magma bomb out of the sky that leaves a crater
        stone.animations(
            a().then(pool(T, LAVA, 0.3, 26, 1.6, null)).then(drift(T, EMBER, 0.3, 1.4, UP, 14)).then(tint(T, "#FF6A20", 0.3, 1.0, 0.45, true, false)).rare(a().then(shards(T, LAVA, 1.0, 12, 1.6, false, false)).then(shards(T, LAVA, 1.4, 12, 1.6, false, false))),
            a().then(ray(LAVA, 0, 0.7, 5, FROM_USER)).then(pool(T, LAVA, 0.5, 40, 1.6, null)).then(hit(0.5)).then(drift(T, FIRE, 0.5, 1.0, UP, 12)).rare(a().then(ray(LAVA, 0, 0.7, 3, THROUGH, -10, 10))),
            a().then(drop(T, "#FF5020", 0.1, 0.7, 0.9)).then(shards(T, LAVA, 0.95, 16, 2.4, false, false)).then(flash("#FF5020", 0.95, 0.35)).then(kick(0.95, 0.15)).then(hit(0.95)).then(drift(T, EMBER, 1.0, 1.0, UP, 14)).rare(a().then(drop(T, "#FF5020", 1.0, 0.4, 1.2)).then(shards(T, LAVA, 1.6, 16, 2.4, false, false)).then(kick(1.6, 0.18))));
        return stone;
    }

    public gemstone uranium () {
        gemstone stone = new gemstone("Uranium", "#60E040", itemParticles.TOXIC, handler,
            "Gamma Burst", abilityType.ATTACK, fx().damage((u, t) -> 7.5 + u.getLevel()*2, "7.5 + 2 x lvl")
                .from(10).moreUses(1), 5, 0.9,
            "Atomic Blast", abilityType.FIRE, fx().healing((u, t) -> u.getMaxHealth()*0.015 + u.getLevel()*0.5, "1.5% maxHP + 0.5 x lvl", 10).burn((u, t) -> 4 + u.getLevel()*1, "4 + 1 x lvl", 5).damage((u, t) -> 7.5 + u.getLevel()*1, "7.5 + 1 x lvl")
                .from(15).moreUses(1), 2, 0.9,
            "Fission Wave", abilityType.POISON, fx().poisonYou((u, t) -> 2, "2", 5).poison((u, t) -> 4 + u.getLevel()*1, "4 + 1 x lvl", 10), 1, 0.9
        );
        stone.lore("Glows faintly in the dark. The previous owner is not available for comment.");
        stone.trembles(0.3).shakeParticles(8).emits(itemInfluence.RADIATION, 2.2).reactsTo(itemInfluence.LIGHTNING, 1).reactsTo(itemInfluence.COSMIC, 1);
        //Hit by the meteorite's purple lightning (straight or through healite) it turns rainbow for a while
        stone.transformsInto(itemInfluence.COSMIC, itemInteractions.HIT_HEAT, "#FF70FF", () -> rainbowUranium()).carriesState();
        //You glowing before three gamma rays go straight through them, an atomic blast between you with a mushroom cloud (burning them, healing you), and a wave of poison rolling over the ground at them
        stone.animations(
            a().beam(RADIATION, 0.45).bolt("#60E040", 5).bolt("#B0FF80", 2).impact(T, RADIATION, 36, 0.06).fountain(T, RADIATION, 20, 0.055, SPARK).quake(0.05).shake(T, 0.04).rare(a().beam(RADIATION, 0.8).skybolt("#60E040", 6)),
            a().impact(T, RADIATION, 60, 0.07).rise(T, RADIATION, 14, 0.09, FLAME).rise(U, HEARTS, 8, 0.09, HEART).fountain(T, RADIATION, 50, 0.07, null).quake(0.12).shake(T, 0.07).rare(a().fountain(T, SMOKE, 90, 0.14, null).quake(0.18)),
            a().then(twin(U, "#60E040", 0, 0.4, 0.3)).then(wave(RADIATION, 0.2, 0.8, 2.2, false, true)).then(tint(T, "#60D040", 1.0, 0.7, 0.5, true, false)).then(drift(T, POISON, 1.0, 0.9, UP, 10)).then(drift(U, POISON, 1.0, 0.6, DOWN, 6)).rare(a().then(wave(RADIATION, 1.1, 0.7, 3.2, false, true))));
        return stone;
    }

    //Uranium hit by cosmic lightning: it runs through every colour, glows, and its moves hit a bit harder. It turns back
    //into uranium about 5 seconds after the last purple strike; every new one fills it up again
    public gemstone rainbowUranium () {
        gemstone stone = new gemstone("Rainbow Uranium", "#60E040", itemParticles.TOXIC, handler,
            "Gamma Burst", abilityType.ATTACK, fx().damage((u, t) -> 8.5 + u.getLevel()*2, "8.5 + 2 x lvl")
                .from(10).moreUses(1), 5, 0.9,
            "Atomic Blast", abilityType.FIRE, fx().healing((u, t) -> u.getMaxHealth()*0.015 + u.getLevel()*0.5, "1.5% maxHP + 0.5 x lvl", 10).burn((u, t) -> 4.5 + u.getLevel()*1, "4.5 + 1 x lvl", 5).damage((u, t) -> 8.5 + u.getLevel()*1, "8.5 + 1 x lvl")
                .from(15).moreUses(1), 2, 0.9,
            "Fission Wave", abilityType.POISON, fx().poisonYou((u, t) -> 2, "2", 4).poison((u, t) -> 4.5 + u.getLevel()*1, "4.5 + 1 x lvl", 10), 1, 0.9
        );
        stone.lore("Uranium that caught a falling star's spark. It cannot decide what colour to be.");
        stone.rainbow().restingGlow(0.35);
        stone.trembles(0.3).shakeParticles(8).emits(itemInfluence.RADIATION, 2.2).reactsTo(itemInfluence.LIGHTNING, 1).reactsTo(itemInfluence.COSMIC, 1);
        //A purple hit counts double here, so it always fills it back up to the full 5 seconds
        stone.revertsInto(itemInfluence.COSMIC, 1, "#60E040", () -> uranium()).heatedBy(itemInfluence.COSMIC, 2).coolsIn(5).carriesState();
        //Uranium's moves in every colour
        stone.animations(
            a().beam(RAINBOW, 0.5).bolt("#FF70FF", 5).bolt("#21FF2C", 3).impact(T, RAINBOW, 40, 0.06).fountain(T, RAINBOW, 24, 0.055, SPARK).quake(0.05).shake(T, 0.04).rare(a().skybolt("#FF70FF", 8).fountain(T, CONFETTI, 50, 0.06, STAR)),
            a().impact(T, RAINBOW, 64, 0.07).rise(T, FIRE, 14, 0.09, FLAME).rise(U, HEARTS, 8, 0.09, HEART).fountain(T, RAINBOW, 54, 0.07, null).quake(0.12).shake(T, 0.07).rare(a().fountain(T, CONFETTI, 90, 0.1, STAR)),
            a().then(twin(U, "#FF70FF", 0, 0.4, 0.3)).then(wave(RAINBOW, 0.2, 0.8, 2.6, false, true)).then(tints(T, RAINBOW, 1.0, 0.7, 0.5)).then(drift(T, POISON, 1.0, 0.9, UP, 10)).then(drift(U, POISON, 1.0, 0.6, DOWN, 6)).rare(a().then(wave(CONFETTI, 1.1, 0.7, 3.4, false, true))));
        return stone;
    }

    //Draws the boltstone's lightning to it from anywhere on the screen, charges boltstones near it just by being there,
    //and passes strikes on to other copper next to it
    public gemstone copper () {
        gemstone stone = new gemstone("Copper", "#C87533", itemParticles.NONE, handler,
            "Conduct", abilityType.ATTACK, fx().damage((u, t) -> 11 + u.getLevel()*1.5, "11 + 1.5 x lvl"), 8, 1,
            "Short Circuit", abilityType.LIGHTNING, fx().damage((u, t) -> 14 + u.getLevel()*1.5, "14 + 1.5 x lvl").confuse(0.35, 2)
                .from(5).damage(theirMaxHP(0.07), "7% their maxHP").moreUses(1), 3, 0.85,
            "Grounding", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.12, "12% maxHP"), 2, 0.9
        );
        stone.lore("Soft, reddish and a little too eager to carry a current.");
        //Pull: 0.46 for a boltstone in the slot next to it, 0.11 with one slot between, full when hovered on top
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        //One lightning hit charges it (its uses left go with it)
        stone.transformsInto(itemInfluence.LIGHTNING, itemInteractions.HIT_HEAT, "#FFE080", () -> chargedCopper()).carriesState();
        //A copper nugget thrown with a spark after it, a short circuit that leaves them seeing stars, and your troubles draining off you into the ground
        stone.animations(
            a().then(lob("#C87533", 0, 0.24, 0.5, 0.4, 0, false)).bolt("#F0A050", 5).then(tint(T, "#F0A050", 0.4, 0.3, 0.6, true, false)).then(hit(0.4)).rare(a().then(shards(T, COPPER, 0.6, 14, 1.4, false, false)).then(lob("#C87533", 0.6, 0.24, 0.8, 0.4, 0, false)).then(lob("#C87533", 0.75, 0.24, 0.3, 0.4, 0, false))),
            a().bolt("#F0A050", 8).bolt("#FFE080", 4).impact(T, COPPER, 38, 0.06).quake(0.05).shake(T, 0.05).rare(a().skybolt("#F0A050", 8)),
            a().then(peel(U, GRIME, 0, 1.4, true)).then(drift(U, COPPER, 0.5, 0.8, DOWN, 16)).then(tint(U, "#F0A050", 0.9, 0.5, 0.4, false, true)).rare(a()));
        return stone;
    }

    //Copper just hit by lightning: it glows a little and its moves hit a bit harder. It turns back into copper about
    //5 seconds after the last hit; every new hit charges it fully again
    public gemstone chargedCopper () {
        gemstone stone = new gemstone("Charged Copper", "#F0A050", itemParticles.SPARKLE, handler,
            "Conduct", abilityType.ATTACK, fx().damage((u, t) -> 11.5 + u.getLevel()*1.5, "11.5 + 1.5 x lvl"), 8, 1,
            "Short Circuit", abilityType.LIGHTNING, fx().damage((u, t) -> 15 + u.getLevel()*1.5, "15 + 1.5 x lvl").confuse(0.35, 2)
                .from(5).damage(theirMaxHP(0.07), "7% their maxHP").moreUses(1), 3, 0.85,
            "Grounding", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.13, "13% maxHP"), 2, 0.9
        );
        stone.lore("Copper still humming from a lightning strike. It will not last.");
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        //A hit counts double here, so it always fills it back up to the full 5 seconds
        stone.revertsInto(itemInfluence.LIGHTNING, 1, "#C87533", () -> copper()).heatedBy(itemInfluence.LIGHTNING, 2).coolsIn(5).carriesState();
        stone.restingGlow(0.35);
        //Copper's moves, crackling harder
        stone.animations(
            a().then(lob("#F0A050", 0, 0.24, 0.5, 0.4, 0, false)).bolt("#FFE080", 5).then(tint(T, "#FFE080", 0.5, 0.4, 0.7, true, false)).then(hit(0.5)).bolt("#FFE080", 5).rare(a().bolt("#FFFFFF", 5)),
            a().bolt("#FFE080", 11).bolt("#FFFFFF", 4).impact(T, COPPER, 46, 0.06).quake(0.07).shake(T, 0.06).rare(a().skybolt("#FFFFFF", 10)),
            a().then(peel(U, GRIME, 0, 1.4, true)).then(drift(U, YELLOW, 0.5, 0.8, DOWN, 16)).bolt("#FFE080", 5).then(tint(U, "#FFE080", 0.9, 0.5, 0.4, false, true)).rare(a()));
        return stone;
    }

    //Throws off sand while it is moved: grains that fly out along the swing, tumble and fall
    public gemstone sandstone () {
        gemstone stone = new gemstone("Sandstone", "#FFE8A8", itemParticles.SAND, handler,
            "Sandblast", abilityType.ATTACK, fx().damage((u, t) -> 10 + u.getLevel()*3.5, "10 + 3.5 x lvl")
                .from(15).moreUses(1), 6, 0.95,
            "Sandstorm", abilityType.CONFUSE, fx().confuse(0.5, 3), 3, 0.85,
            "Erosion", abilityType.POISON, fx().poison((u, t) -> 5 + u.getLevel()*1, "5 + 1 x lvl", 6)
                .from(10).moreUses(1), 2, 0.9
        );
        stone.lore("Packed sand that never quite remembers being a rock. It leaves a trail wherever it goes.");
        stone.shakeParticles(60).idleParticles(0).shakeThreshold(0.2).glows(false);
        //Next to magma it heats up (glowing orange) and turns into glass
        stone.transformsInto(itemInfluence.HEAT, 1, "#FF8030", () -> glass());
        //A clump of packed sand thrown at them that bursts into a little heap, sand streaming at them while they reel, and
        //them being sanded away: their colour going to sand, grains flaking off and a heap piling up under them
        stone.animations(
            a().then(lob("#E0C080", 0, 0.35, 0.5, 0.4, 0, false)).then(shards(T, SAND, 0.4, 14, 1.4, false, false)).then(pool(T, SAND, 0.4, 16, 1.4, null)).then(tint(T, "#FFE8A8", 0.4, 0.4, 0.5, true, false)).then(hit(0.4))
                .rare(a().then(lob("#E0C080", 0.5, 0.3, 0.7, 0.4, 0, false)).then(shards(T, SAND, 0.9, 14, 1.4, false, false)).then(hit(0.9))),
            a().then(drift(U, SAND, 0, 1.4, TOWARD, 70)).then(tint(T, "#E0C080", 0.3, 1.1, 0.45, true, false)).then(stretch(T, 0.3, 1.1, TWIST)).then(pool(T, SAND, 0.6, 18, 1.2, null))
                .rare(a().then(drift(U, SAND, 0.2, 1.4, TOWARD, 70)).then(pool(T, SAND, 1.0, 30, 1.2, null))),
            a().then(tint(T, "#C8A46E", 0, 1.4, 0.5, false, true)).then(shards(T, SAND, 0.2, 8, 1.2, false, false)).then(shards(T, SAND, 0.7, 8, 1.2, false, false)).then(pool(T, SAND, 0.2, 26, 1.5, null))
                .rare(a().then(shards(T, SAND, 1.1, 12, 1.2, false, false)).then(pool(T, SAND, 0.4, 44, 1.5, null))));
        return stone;
    }

    //Sandstone that magma has heated, a rock of its own. It stays glass until it is shaken hard enough to break into sand
    public gemstone glass () {
        gemstone stone = new gemstone("Glass", "#F6FCFF", itemParticles.SPARKLE, handler,
            "Shard Spray", abilityType.ATTACK, fx().damage((u, t) -> 15 + u.getLevel()*3.5, "15 + 3.5 x lvl")
                .from(15).damage(theirMaxHP(0.04), "4% their maxHP").from(20).moreUses(1), 6, 0.95,
            "Glare", abilityType.CONFUSE, fx().confuse(0.9, 2), 3, 0.8,
            "Clear Through", abilityType.CLEANSE, fx().cleanse(), 2, 1
        );
        stone.lore("Sand that sat too close to the fire. Handle with care, or it is sand again.");
        //Jerked around hard on the cursor, it breaks back into sandstone
        stone.transformsInto(itemInfluence.SHAKE, 1, "#FFE8A8", () -> sandstone());
        stone.lightTint(0.5).seeThrough(0.55);
        //A spray of glass shards that catch the light, a blinding beam of light out of the sky, and you turning to clear glass as everything falls through you
        stone.animations(
            a().then(lob("#F6FCFF", 0, 0.5, 0.6, 0.3, 0, false)).then(shards(U, GLASS, 0.3, 12, 2.4, true, true)).then(hit(0.55)).rare(a().then(shards(U, GLASS, 0.9, 14, 2.6, true, true)).then(hit(1.15))),
            a().then(ray(CLEAN, 0, 0.8, 6, FROM_SKY)).then(flash("#FFFFFF", 0.5, 0.7)).then(tint(T, "#FFFFFF", 0.5, 1.0, 0.7, true, false)).then(stretch(T, 0.6, 0.8, TWIST)).rare(a().then(flash("#FFFFFF", 0.8, 0.9)).then(flash("#FFFFFF", 1.1, 0.6))),
            a().then(tint(U, "#F6FCFF", 0, 1.6, 0.55, false, false)).then(peel(U, GRIME, 0.2, 1.4, true)).then(shards(U, COAL, 1.4, 6, 0.8, false, false)).rare(a().then(drift(U, GLASS, 0, 1.6, UP, 20))));
        return stone;
    }

    //Melts into water next to magma or when struck by lightning (two strikes); obsidian freezes the water back
    public gemstone ice () {
        gemstone stone = new gemstone("Ice", "#7CC8FF", itemParticles.FROST, handler,
            "Ice Shard", abilityType.ATTACK, fx().damage((u, t) -> 15 + u.getLevel()*1.5, "15 + 1.5 x lvl")
                .from(5).damage(theirMaxHP(0.025), "2.5% their maxHP").from(10).damage(theirMaxHP(0.04), "4% their maxHP").moreUses(1).from(15).damage(theirMaxHP(0.015), "1.5% their maxHP"), 6, 0.95,
            "Freeze", abilityType.CONFUSE, fx().confuse(0.85, 2), 3, 0.85,
            "Frostbite", abilityType.POISON, fx().poison((u, t) -> 9 + u.getLevel()*1, "9 + 1 x lvl", 4), 2, 0.9
        );
        stone.lore("It never melts in your pocket. Near a fire is another story.");
        stone.transformsInto(itemInfluence.HEAT, 1, "#3C8CE8", () -> water()).heatedBy(itemInfluence.LIGHTNING, 1);
        //It cools things down like obsidian does: glass back into sandstone, water into ice, magma into coal
        stone.emits(itemInfluence.COLD, 2.0, 0.8);
        stone.lightTint(0.45);
        //An ice shard thrown at them, ice thrown at them that freezes them in a block of ice where it hits, and a jaw of ice biting shut on them
        stone.animations(
            a().then(tint(U, "#D8F4FF", 0, 0.4, 0.5, false, true)).then(lob("#A0D0E0", 0.5, 0.35, 0.4, 0.35, 0, false)).then(shards(T, ICE, 0.85, 10, 1.6, false, true)).then(pool(T, ICE, 0.85, 12, 1.4, null)).then(hit(0.85)).rare(a().then(lob("#A0D0E0", 0.6, 0.3, 0.6, 0.35, 0, false)).then(lob("#A0D0E0", 0.7, 0.3, 0.2, 0.35, 0, false))),
            a().then(lob("#A0D0E0", 0, 0.35, 0.4, 0.35, 0, false)).then(tint(T, "#D8F4FF", 0.35, 0.3, 0.6, false, false)).then(encase(T, "#A0D0E0", 0.35, 1.0, 0.7, false)).then(hit(0.35)).then(shards(T, ICE, 1.35, 14, 1.4, false, true)).rare(a().then(drift(T, ICE, 0.35, 1.2, DOWN, 30))),
            a().then(crystals(T, "#D8F4FF", 0, 6, 0.45, JAW, 1.0)).then(tint(T, "#7CC8FF", 0.25, 0.6, 0.5, true, false)).then(hit(0.25)).then(pool(T, ICE, 0.3, 14, 1.3, null)).rare(a().then(crystals(T, "#D8F4FF", 1.1, 6, 0.45, JAW, 0.8)).then(hit(1.35))));
        return stone;
    }

    //Melted ice. It drips, and stays water until obsidian freezes it back into ice
    public gemstone water () {
        gemstone stone = new gemstone("Water", "#3C8CE8", itemParticles.WATER, handler,
            "Splash", abilityType.ATTACK, fx().damage((u, t) -> 9 + u.getLevel()*2.5, "9 + 2.5 x lvl")
                .from(20).moreUses(1), 6, 1,
            "Soak", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.05, "5% maxHP"), 3, 1,
            "Tide", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.17, "17% maxHP")
                .from(10).moreUses(1), 3, 0.95
        );
        stone.lore("A rock that forgot to stay solid. Hold it level.");
        //Obsidian's cold freezes it back into ice
        stone.transformsInto(itemInfluence.COLD, 1, "#7CC8FF", () -> ice());
        //Saltstone next to it (or held over it) salts it
        stone.transformsInto(itemInfluence.SALT, 1, "#E8F2FF", () -> saltWater());
        stone.lightTint(0.3).seeThrough(0.65);
        //Swung around, it pours out behind it in a stream
        stone.trail("#4A9CF0", 16, 0.6);
        //A wave rolling at them and breaking over them, rain washing you clean into a puddle, and the tide coming in round your feet
        stone.animations(
            a().then(wave(WATER, 0, 0.6, 1.6, false, true)).then(shards(T, WATER, 0.6, 12, 1.6, false, false)).then(hit(0.6)).rare(a().then(wave(WATER, 0, 0.6, 3.0, false, true)).then(hide(T, 0.5, 0.3))),
            a().then(drift(U, WATER, 0, 1.4, DOWN, 30)).then(peel(U, GRIME, 0.3, 1.3, true)).then(pool(U, WATER, 0.8, 24, 1.4, null)).rare(a().then(drop(U, "#3C8CE8", 0, 0.5, 0.5)).then(shards(U, WATER, 0.65, 14, 1.6, false, false))),
            a().then(wave(WATER, 0, 0.6, 2.4, true, true)).then(pool(U, WATER, 0.5, 40, 1.8, null)).then(tint(U, "#79BCCE", 0.9, 0.6, 0.4, false, true)).then(drift(U, HEARTS, 0.9, 1.0, UP, 8)).rare(a().then(wave(WATER, 0.2, 0.8, 5.0, true, true))));
        return stone;
    }

    //Water that saltstone has salted. Salt water carries a boltstone's lightning like copper does (it draws it and
    //charges boltstones next to it). It does not freeze; healite's healing takes the salt out again
    public gemstone saltWater () {
        gemstone stone = new gemstone("Salt Water", "#5C9CE0", itemParticles.WATER, handler,
            "Brine Splash", abilityType.ATTACK, fx().damage((u, t) -> 7.5 + u.getLevel()*2.5, "7.5 + 2.5 x lvl").confuse(0.35, 2)
                .from(5).confuse(0.45, 2), 6, 1,
            "Soak", abilityType.CLEANSE, fx().cleanse(), 3, 1,
            "Tide", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.145, "14.5% maxHP")
                .from(10).moreUses(1), 2, 0.95
        );
        stone.lore("Water with a bite. It stings the eyes and carries a spark better than it should.");
        stone.emits(itemInfluence.CONDUCTION, 3, 1, 1.74).conducts(1.6).hitDebris(0.3).reactsTo(itemInfluence.LIGHTNING, 0.5);
        stone.transformsInto(itemInfluence.HEALING, 1, "#3C8CE8", () -> water());
        stone.lightTint(0.35).seeThrough(0.7);
        stone.trail("#6AA8EC", 16, 0.6);
        //Water's moves, with salt: a stinging wave, a puddle that dries to a crust of salt, and a tide that leaves a white line
        stone.animations(
            a().then(wave(WATER, 0, 0.6, 1.6, false, true)).then(shards(T, SALT, 0.6, 12, 1.6, false, false)).then(tint(T, "#FFFFFF", 0.6, 0.5, 0.7, true, false)).then(stretch(T, 0.7, 0.7, TWIST)).then(hit(0.6)).rare(a().bolt("#FFFFFF", 5)),
            a().then(drift(U, WATER, 0, 1.4, DOWN, 30)).then(peel(U, GRIME, 0.3, 1.3, true)).then(pool(U, WATER, 0.8, 24, 2.0, "#F2EEE6")).rare(a().then(drift(U, SALT, 1.4, 1.0, DOWN, 20))),
            a().then(wave(WATER, 0, 0.6, 2.4, true, true)).then(pool(U, WATER, 0.5, 40, 2.2, "#F2EEE6")).then(tint(U, "#79BCCE", 0.9, 0.6, 0.4, false, true)).then(drift(U, HEARTS, 0.9, 1.0, UP, 8)).rare(a().then(wave(SALT, 0.2, 0.8, 5.0, true, true))));
        return stone;
    }

    //Gives off salt: water next to it (or held over it) turns into salt water, with white grains flying over
    public gemstone saltstone () {
        gemstone stone = new gemstone("Saltstone", "#F2EEE6", itemParticles.SALT, handler,
            "Salt Spray", abilityType.ATTACK, fx().damage((u, t) -> 11.5 + u.getLevel()*3.5, "11.5 + 3.5 x lvl")
                .from(20).moreUses(1), 6, 0.95,
            "Brine Sting", abilityType.POISON, fx().poison((u, t) -> 4 + u.getLevel()*1, "4 + 1 x lvl", 5)
                .from(10).moreUses(1), 3, 0.9,
            "Preserve", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.11, "11% maxHP"), 2, 0.9
        );
        stone.lore("A lump of old sea, dried out. Water near it never tastes the same again.");
        stone.emits(itemInfluence.SALT, 2.0, 0.8);
        stone.lightTint(0.5);
        //A fan of salt grains flicked at them, a stinging clump of salt thrown at them, and a crust of salt over you that cracks off clean
        stone.animations(
            a().then(drift(T, SALT, 0, 0.8, TOWARD, 70)).then(tint(T, "#FFFFFF", 0.4, 0.6, 0.5, true, false)).then(hit(0.4)).rare(a().then(drift(T, SALT, 0.6, 0.8, TOWARD, 70)).then(hit(1.0))),
            a().then(lob("#F2EEE6", 0, 0.24, 0.5, 0.4, 0, false)).then(shards(T, SALT, 0.4, 10, 1.4, false, false)).then(tint(T, "#60D040", 0.4, 0.8, 0.5, true, false)).then(hit(0.4)).then(drift(T, POISON, 0.5, 0.9, UP, 10)).rare(a().then(lob("#F2EEE6", 0.5, 0.24, 0.8, 0.4, 0, false)).then(hit(0.9))),
            a().then(encase(U, "#F2EEE6", 0, 1.2, 0.8, false)).then(shards(U, SALT, 1.2, 12, 1.3, false, false)).then(drift(U, HEARTS, 1.2, 0.8, UP, 6)).rare(a().then(shards(U, SALT, 1.3, 16, 1.6, false, false))));
        return stone;
    }

    //Magma that obsidian has cooled: burnt out and black, still smoking a little. A boltstone's lightning relights it
    public gemstone coal () {
        gemstone stone = new gemstone("Coal", "#3A3634", itemParticles.SMOKE, handler,
            "Coal Chunk", abilityType.ATTACK, fx().damage((u, t) -> 20 + u.getLevel()*4, "20 + 4 x lvl")
                .from(20).moreUses(1), 4, 0.95,
            "Soot Cloud", abilityType.CONFUSE, fx().confuse(0.4, 2), 3, 0.9,
            "Rekindle", abilityType.FIRE, fx().burn((u, t) -> 4.5 + u.getLevel()*1, "4.5 + 1 x lvl", 4)
                .from(10).moreUses(1), 1, 0.85
        );
        stone.lore("What is left of magma that met something colder. It remembers being on fire, faintly.");
        //Lightning sets it alight again: two strikes (from a boltstone) and it is magma once more
        stone.transformsInto(itemInfluence.LIGHTNING, 1, "#FF5020", () -> magma());
        //Healite's healing brings it back to life too, as slowly as obsidian sets magma
        stone.heatedBy(itemInfluence.HEALING, 1);
        //A lump of coal that leaves them sooty (now and then it shines like a diamond), a cloud of soot swallowing them, and the coal's glow waking in you before an ember sets them burning
        stone.animations(
            a().then(lob("#3A3634", 0, 0.55, 0.9, 0.6, 0, false)).then(shards(T, COAL, 0.6, 10, 1.6, false, false)).then(tint(T, "#2E2A28", 0.6, 0.5, 0.6, false, false)).then(hit(0.6)).rare(a().then(shards(T, CLEAN, 0.6, 14, 1.8, false, true)).then(lob("#F6FCFF", 0.9, 0.4, 0.9, 0.6, 0, false))),
            a().then(cloud(SMOKE, 0, 1.8, ENGULF, 1.6)).then(hide(T, 0.6, 0.9)).then(drift(T, SMOKE, 0.6, 1.2, DOWN, 14)).then(knock(T, 1.5, 0.08, 0.1)).then(knock(T, 1.7, 0.08, 0.1)).rare(a().then(cloud(SMOKE, 0.1, 1.8, ENGULF, 2.6))),
            a().then(glow(U, "#FF2000", 0, 0.9, 1.2, 3, false)).then(lob("#FF5020", 0.6, 0.25, 0.6, 0.5, 0, false)).then(pool(T, new String[] {"#CC0D00", "#3A3634"}, 1.1, 18, 1.4, null)).then(drift(T, EMBER, 1.1, 1.0, UP, 12)).then(tint(T, "#FC9F00", 1.1, 0.6, 0.5, true, false)).then(hit(1.1)).dark(mood.night(0.55)).rare(a().then(pool(T, new String[] {"#FF2000", "#3A3634"}, 1.1, 40, 1.6, null))));
        return stone;
    }

    public gemstone healite () {
            gemstone stone = new gemstone("Healite", "#FF90B0", itemParticles.SPARKLE, handler,
            "Revitalize", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.75, "75% maxHP")
                .from(5).heal((u, t) -> u.getMaxHealth()*0.14, "14% maxHP").from(10).hits(0.85).from(15).heal((u, t) -> u.getMaxHealth()*0.2, "20% maxHP").from(20).heal((u, t) -> u.getMaxHealth()*0.14, "14% maxHP"), 2, 0.75,
            "Ressurection", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.34, "34% maxHP").heal(theirMaxHP(0.1), "10% their maxHP").damage(theirMaxHP(0.125), "12.5% their maxHP")
                .from(5).heal(theirMaxHP(0.08), "8% their maxHP").damage(theirMaxHP(0.1), "10% their maxHP").from(15).moreUses(1).from(25).heal((u, t) -> u.getMaxHealth()*0.28, "28% maxHP"), 1, 0.7,
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
        //Light and hearts whirling round you, you crumbling into pink light and coming back whole while their strength runs into you, and a column of light pouring down on you from the sky
        stone.animations(
            a().then(drift(U, HEARTS, 0.1, 1.6, UP, 16)).then(tint(U, "#FFC0D0", 0.3, 1.0, 0.6, false, true)).then(glow(U, "#FF90B0", 0, 1.8, 2.0, 2, false)).rare(a().then(drift(U, HEARTS, 0, 1.8, DOWN, 30))),
            a().then(dissolve(U, 0, 1.6, UP, true, "#FF90B0")).back(HEARTS, 12, 0.1, HEART, 0.6, 0.5).then(tint(T, "#FF4060", 0.4, 0.5, 0.4, true, false)).then(peel(U, GRIME, 0, 0.9, true)).rare(a().then(twin(U, "#FFC0D0", 1.5, 1.0, 0.0))),
            a().then(lift(U, 0, 0.4, 1.0)).then(glow(U, "#FF90B0", 0, 1.6, 2.0, 0, false)).then(column(U, PINK, 0.2, 1.4, 1.0, 6, true, 0.6)).then(flash("#FFC0D0", 1.3, 0.5)).dark(mood.night(0.5)).rare(a().then(flash("#FFFFFF", 1.3, 0.8))));
        return stone;
    }

    //Healite just hit by cosmic lightning: it glows purple and its moves work more often. It turns back into healite
    //about 5 seconds after the last purple strike; every new one fills it up again
    public gemstone chargedHealite () {
        gemstone stone = new gemstone("Charged Healite", "#C878FF", itemParticles.SPARKLE, handler,
            "Revitalize", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.75, "75% maxHP")
                .from(5).heal((u, t) -> u.getMaxHealth()*0.14, "14% maxHP").from(10).hits(0.95).from(15).heal((u, t) -> u.getMaxHealth()*0.2, "20% maxHP").from(20).heal((u, t) -> u.getMaxHealth()*0.14, "14% maxHP"), 2, 0.85,
            "Ressurection", abilityType.CLEANSE, fx().cleanse().heal((u, t) -> u.getMaxHealth()*0.34, "34% maxHP").heal(theirMaxHP(0.1), "10% their maxHP").damage(theirMaxHP(0.125), "12.5% their maxHP")
                .from(5).heal(theirMaxHP(0.08), "8% their maxHP").damage(theirMaxHP(0.1), "10% their maxHP").from(15).moreUses(1).from(25).heal((u, t) -> u.getMaxHealth()*0.28, "28% maxHP"), 1, 0.8,
            "Transcendence", abilityType.RESTORE, fx().restore(), 1, 0.8
        );
        stone.lore("Healite humming with starlight. It will not last.");
        //Pink going purple, like the charge while its moves play (battleAnimation.mood)
        stone.mood(battleAnimation.mood.tinted("#E070D0", 0.25));
        stone.emits(itemInfluence.COSMIC_CONDUCTION, 3, 1, 1.74).conducts(1.6, itemInfluence.COSMIC).hitDebris(0.3).reactsTo(itemInfluence.COSMIC, 0.5);
        stone.emits(itemInfluence.HEALING, 2.0, 0.8);
        stone.revertsInto(itemInfluence.COSMIC, 1, "#FF90B0", () -> healite()).heatedBy(itemInfluence.COSMIC, 2).coolsIn(5).carriesState();
        stone.restingGlow(0.35);
        //Healite's moves in violet, with starlight in them
        stone.animations(
            a().then(drift(U, COSMIC, 0.1, 1.6, UP, 16)).then(drift(U, HEARTS, 0.2, 1.4, UP, 10)).then(tint(U, "#E0C0FF", 0.3, 1.0, 0.6, false, true)).then(glow(U, "#C878FF", 0, 1.8, 2.2, 2, false)).rare(a().then(streak(U, "#C878FF", 0.8, 0.5, 0.4, COSMIC))),
            a().then(dissolve(U, 0, 1.6, UP, true, "#C878FF")).back(HEARTS, 12, 0.1, HEART, 0.6, 0.5).then(tint(T, "#FF4060", 0.4, 0.5, 0.4, true, false)).then(peel(U, GRIME, 0, 0.9, true)).rare(a().then(twin(U, "#E0C0FF", 1.5, 1.0, 0.0))),
            a().then(lift(U, 0, 0.4, 1.0)).then(glow(U, "#C878FF", 0, 1.6, 2.2, 0, false)).then(column(U, COSMIC, 0.2, 1.4, 1.0, 6, true, 0.6)).then(flash("#E0C0FF", 1.3, 0.5)).then(drift(U, LILAC, 0, 1.6, DOWN, 20)).dark(mood.night(0.55)).rare(a().then(streak(U, "#FFFFFF", 0.2, 0.4, 0.2, COSMIC)).then(streak(U, "#C878FF", 0.4, 0.4, 0.2, COSMIC)).then(streak(U, "#FFFFFF", 0.6, 0.4, 0.2, COSMIC))));
        return stone;
    }

    //Holds two other rocks and becomes them, see elementalRock
    public gemstone elemental () {
        return new elementalRock(handler);
    }

    public gemstone pebble () {
        gemstone stone = new gemstone("Pebble", "#C0C0C0", itemParticles.NONE, handler,
        "Stone Skip", abilityType.ATTACK, fx().damage((u, t) -> 12.5 + u.getLevel()*2.5, "12.5 + 2.5 x lvl")
            .from(20).moreUses(1), 8, 0.95,
        "Rock Remedy", abilityType.FIRE, fx().damage((u, t) -> 8.5 + u.getLevel()*1, "8.5 + 1 x lvl").burn((u, t) -> 3 + u.getLevel()*0.5, "3 + 0.5 x lvl", 3), 3, 0.95,
        "Mineral Mend", abilityType.HEAL, fx().heal((u, t) -> u.getMaxHealth()*0.16, "16% maxHP")
            .from(10).moreUses(1), 3, 0.95
    );
        stone.lore("Found on a beach. It skips well.");
        //Half the size of the other rocks
        stone.iconScale(0.5);
        //A flat pebble skipped along the ground at them (now and then a long run of skips), a pebble heated in your hand and tossed so it hisses on them, and little pebbles gathering round you to mend you
        stone.animations(
            a().then(lob("#C0C0C0", 0, 0.22, 0.15, 0.9, 3, true)).then(shards(T, PEBBLE, 0.9, 5, 1.2, false, false)).then(hit(0.9)).rare(a().then(lob("#C0C0C0", 1.0, 0.2, 0.15, 1.4, 7, true)).then(hit(2.4))),
            a().then(tint(U, "#FF6A20", 0, 0.3, 0.4, true, false)).then(lob("#FF6A20", 0.3, 0.24, 0.6, 0.5, 0, false)).then(drift(T, STEAM, 0.8, 1.0, UP, 10)).then(drift(T, EMBER, 0.8, 0.8, UP, 8)).then(tint(T, "#FC9F00", 0.8, 0.6, 0.5, true, false)).then(hit(0.8)).rare(a().then(lob("#FF6A20", 1.2, 0.22, 0.2, 0.8, 2, true)).then(hit(2.0))),
            a().then(spin(U, "#C0C0C0", 0, 1.2, 6, 0.16, 1.2, false)).then(tint(U, "#E0E0E0", 0.6, 0.6, 0.5, false, true)).then(drift(U, HEARTS, 0.6, 1.0, UP, 8)).rare(a().then(spin(U, "#E0E0E0", 0.2, 1.0, 6, 0.14, 1.6, false))));
        return stone;
}

}
