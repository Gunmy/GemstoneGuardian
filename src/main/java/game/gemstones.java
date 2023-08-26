package game;

public class gemstones {
    private gameHandler handler;
    public gemstones (gameHandler handler) {
        this.handler = handler;
    }

    public gemstone rock () {
        return new gemstone("▢ Rock", handler, 
        "Pebble Strike", (c1, c2) -> {c2.takeDmg(13 + c1.getLevel());}, 10, 1,
        "Stone Shatter", (c1, c2) -> {c2.takeDmg(26  + c1.getLevel()*2);}, 5, 1,
        "Stone Renewal", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.1 + 20);}, 3, 1
        );
    }

    public gemstone obsidian () {
        return new gemstone("▢ Obsidian", handler,
        "Heat Wave Slam", (c1, c2) -> {c2.takeDmg(10*(1+c1.getLevel()/3));}, 8, 0.95,
        "Inferno Blast", (c1, c2) -> {c2.addFireDamage(5*(1+c1.getLevel()/3), 5);}, 2, 0.95,
        "Warm Embrace", (c1, c2) -> {c1.addHealing(c1.getMaxHealth()*(0.05+c1.getLevel()/150), 4);}, 3, 0.95
        );
    }

    public gemstone boltstone () {
        return new gemstone("▢ Boltstone", handler,
        "Static Burst", (c1, c2) -> {c2.addFireDamage(5, 3); c2.takeDmg(8*(1+c1.getLevel()/4));}, 8, 1,
        "Thunderbolt", (c1, c2) -> {c2.takeDmg(c2.getMaxHealth()*0.15);}, 3, 0.8,
        "Electrocuter", (c1, c2) -> {c2.takeDmg(c2.getMaxHealth()*0.4);}, 1, 0.5
        );
    }

    public gemstone giantBolder () {
        return new gemstone("▢ Giant Bolder", handler, 
        "Bolder Smash", (c1, c2) -> {c2.takeDmg(7*(1+c1.getLevel()/3));}, 6, 1,
        "Crushing Blow",(c1, c2) -> {c2.takeDmg(10 + c1.getLevel()); c2.addConfusion(0.2, 6);}, 5, 0.9,
        "Mountainous Maul", (c1, c2) -> {c1.heal(c2.getMaxHealth()*0.2); c2.takeDmg(c2.getMaxHealth()*0.2);}, 1, 0.6
        );
    }

    
    public gemstone discombobulateShard () {
        return new gemstone("▢ Discombobulate Shard", handler,
            "Mystic Mind Maze", (c1, c2) -> {c2.addConfusion(0.30, 4); c2.takeDmg(10);}, 3, 0.9,
            "Mirage Assault", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.03 + 5); c2.addPoisonDamage(5, 8); c2.addFireDamage(5, 8); c2.takeDmg(2*c1.getLevel());}, 2, 0.85,
            "Mind Warp",  (c1, c2) -> {c2.addConfusion(0.75, 4);}, 1, 0.75
        );
    }

    public gemstone meteorite () {
        return new gemstone("▢ Meteorite", handler, 
            "Gravitational Slam", (c1, c2) -> {c2.takeDmg(15*(1+c1.getLevel()/7)); c2.addConfusion(0.1, 3);}, 10, 0.99,
            "Cosmic Crush", (c1, c2) -> {c1.takeDmg(c1.getMaxHealth()*0.05); c2.addConfusion(0.15, 5); c2.takeDmg(10*(1+c1.getLevel()/4));}, 3, 1,
            "Orbitin Assault", (c1, c2) -> {c1.takeDmg(c1.getMaxHealth()*0.2); c2.takeDmg(c2.getMaxHealth()*0.4);}, 1, 1
        );
    }
    
    public gemstone magma () {
        return new gemstone("▢ Magma", handler,
            "Molten Fury", (c1, c2) -> {c2.addFireDamage(c1.getLevel()*1.5, 3);}, 3, 0.85,
            "Magma Blast", (c1, c2) -> {c2.addFireDamage(c1.getLevel(), 6);}, 3, 0.8,
            "Volcanic Impact",  (c1, c2) -> {c2.addFireDamage(c1.getLevel()*1.5, 6);}, 1, 0.75
        );
    }

    public gemstone uranium () {
        return new gemstone("▢ Uranium", handler,
            "Gamma Burst", (c1, c2) -> {c2.takeDmg(15*(1+c1.getLevel()/7));}, 5, 0.9,
            "Atomic Blast", (c1, c2) -> {c1.addHealing(5+c1.getLevel(), 10); c2.addFireDamage(5+c1.getLevel(), 5); c2.takeDmg(10);}, 2, 0.9,
            "Fission Wave", (c1, c2) -> {c1.addPoisonDamage(2, 5); c2.addPoisonDamage(5+c1.getLevel(), 10);}, 1, 0.9
        );
    }

    public gemstone healite () {
            return new gemstone("▢ Healite", handler,
            "Revitalize", (c1, c2) -> {c1.heal(c1.getHealth()*0.5);}, 1, 0.7,
            "Ressurection", (c1, c2) -> {c1.clearEffects();}, 1, 0.7,
            "Transcendence", (c1, c2) -> {c1.restoreItems();}, 1, 0.7
        );
    }

    public gemstone pebble () {
        return new gemstone("▢ Pebble", handler,
        "Stone Skip", (c1, c2) -> {c2.takeDmg(c1.getLevel()*2+5);}, 8, 0.95,
        "Rock Remedy", (c1, c2) -> {c2.takeDmg(c1.getLevel()*1.5); c2.addFireDamage(c1.getLevel()/2 + 1, 3);}, 3, 0.95,
        "Mineral Mend", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.20);}, 3, 0.95
    );
}

}
