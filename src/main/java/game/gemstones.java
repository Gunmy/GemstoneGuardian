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
        return stone;
    }

    public gemstone obsidian () {
        gemstone stone = new gemstone("Obsidian", "#5A4A6A", itemParticles.SMOKE, handler,
        "Heat Wave Slam", abilityType.ATTACK, "Damage: 10 x (1 + lvl/3)", (c1, c2) -> {c2.takeDmg(10*(1+c1.getLevel()/3));}, 8, 0.95,
        "Inferno Blast", abilityType.FIRE, "Burn: 5 x (1 + lvl/3) per turn, 5 turns", (c1, c2) -> {c2.addFireDamage(5*(1+c1.getLevel()/3), 5);}, 2, 0.95,
        "Warm Embrace", abilityType.HEAL, "Heals: (5% + lvl/150) of maxHP per turn, 4 turns", (c1, c2) -> {c1.addHealing(c1.getMaxHealth()*(0.05+c1.getLevel()/150), 4);}, 3, 0.95
        );
        stone.lore("Volcanic glass, still warm to the touch. It remembers the fire it came from.");
        return stone;
    }

    public gemstone boltstone () {
        gemstone stone = new gemstone("Boltstone", "#F0D040", itemParticles.SPARKLE, handler,
        "Static Burst", abilityType.FIRE, "Damage: 8 x (1 + lvl/4). Burn: 5 per turn, 3 turns", (c1, c2) -> {c2.addFireDamage(5, 3); c2.takeDmg(8*(1+c1.getLevel()/4));}, 8, 1,
        "Thunderbolt", abilityType.LIGHTNING, "Damage: 15% of their maxHP", (c1, c2) -> {c2.takeDmg(c2.getMaxHealth()*0.15);}, 3, 0.8,
        "Electrocuter", abilityType.LIGHTNING, "Damage: 40% of their maxHP", (c1, c2) -> {c2.takeDmg(c2.getMaxHealth()*0.4);}, 1, 0.5
        );
        stone.lore("Struck by lightning so many times it started striking back.");
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
        return stone;
    }
    
    public gemstone magma () {
        gemstone stone = new gemstone("Magma", "#FF5020", itemParticles.FIRE, handler,
            "Molten Fury", abilityType.FIRE, "Burn: 1.5 x lvl per turn, 3 turns", (c1, c2) -> {c2.addFireDamage(c1.getLevel()*1.5, 3);}, 3, 0.85,
            "Magma Blast", abilityType.FIRE, "Burn: lvl per turn, 6 turns", (c1, c2) -> {c2.addFireDamage(c1.getLevel(), 6);}, 3, 0.8,
            "Volcanic Impact", abilityType.FIRE, "Burn: 1.5 x lvl per turn, 6 turns", (c1, c2) -> {c2.addFireDamage(c1.getLevel()*1.5, 6);}, 1, 0.75
        );
        stone.lore("Molten rock that never quite set. Do not store near paper.");
        return stone;
    }

    public gemstone uranium () {
        gemstone stone = new gemstone("Uranium", "#60E040", itemParticles.TOXIC, handler,
            "Gamma Burst", abilityType.ATTACK, "Damage: 15 x (1 + lvl/7)", (c1, c2) -> {c2.takeDmg(15*(1+c1.getLevel()/7));}, 5, 0.9,
            "Atomic Blast", abilityType.FIRE, "Damage: 10. Burn: 5 + lvl per turn, 5 turns. Heals you: 5 + lvl per turn, 10 turns", (c1, c2) -> {c1.addHealing(5+c1.getLevel(), 10); c2.addFireDamage(5+c1.getLevel(), 5); c2.takeDmg(10);}, 2, 0.9,
            "Fission Wave", abilityType.POISON, "Poison: 5 + lvl per turn, 10 turns. Poisons you: 2 per turn, 5 turns", (c1, c2) -> {c1.addPoisonDamage(2, 5); c2.addPoisonDamage(5+c1.getLevel(), 10);}, 1, 0.9
        );
        stone.lore("Glows faintly in the dark. The previous owner is not available for comment.");
        return stone;
    }

    public gemstone healite () {
            gemstone stone = new gemstone("Healite", "#FF90B0", itemParticles.SPARKLE, handler,
            "Revitalize", abilityType.HEAL, "Heals: 50% of your current HP", (c1, c2) -> {c1.heal(c1.getHealth()*0.5);}, 1, 0.7,
            "Ressurection", abilityType.CLEANSE, "Removes every effect on you", (c1, c2) -> {c1.clearEffects();}, 1, 0.7,
            "Transcendence", abilityType.RESTORE, "Restores the uses of all your items", (c1, c2) -> {c1.restoreItems();}, 1, 0.7
        );
        stone.lore("A soft pink stone that hums quietly. Good for the soul.");
        return stone;
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
