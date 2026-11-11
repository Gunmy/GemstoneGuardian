package game;

//Every item that is not a gemstone, see gemstones for those
public class items {
    private gameHandler handler;
    public items (gameHandler handler) {
        this.handler = handler;
    }

    public item healingPotion () {
        return new potion("Healing Potion", "#FF4060", itemParticles.HEAL,
            (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.3);}, handler)
            .lore("Red and sweet. Mends wounds from the inside.")
            .details("Heals: 30% of maxHP");
    }

    public item cleansingPotion () {
        return new potion("Cleansing Potion", "#40C0FF", itemParticles.SPARKLE,
            (c1, c2) -> {c1.clearEffects();}, handler)
            .lore("Clear as spring water. Washes every curse away.")
            .details("Removes fire, poison and confusion from you");
    }

    //Given by the healing pad in the castle outside. Looks like a healing pad, crystal floor and green sparks included
    public item teleporter () {
        return new item("Teleporter", (c1, c2) -> {}, false, handler)
            .lookLikeTile(handler.getTileTypes().get(91))
            .iconScale(0.8)
            .particles(itemParticles.HEAL_PAD)
            .notForBattle()
            .onInventoryUse("Teleports you to a healing pad you have stood on", c -> {
                handler.getGui().closeInventory();
                handler.getGui().openTeleports(c.getVisitedTeleports(), "Teleporter", null);
            })
            .lore("A sliver of a healing pad that still remembers every pad it has touched.");
    }

    //Trinkets, worn in the two trinket slots left of the inventory (see trinket). Their icons are drafts in tiles.png:
    //heart (1300, 1500), withered heart (1400, 1500), feather (1000, 1600), syringe (1100, 1600)

    //Worn on the head. Walk 1.75 times as fast
    public trinket featherOfHermesBoots () {
        trinket t = new trinket("Feather of Hermes Boots", trinket.place.HEAD, handler).speed(1.75).wornScale(1.4);
        t.look(1000, 1600, null).particles(itemParticles.SPARKLE).glowColor("#FFC830").restingGlow(0.5)
            .lore("A single feather plucked from a pair of winged boots. Your feet forget how heavy they are.")
            .details("Walking speed: x1.75");
        return t;
    }

    //Worn on the chest, beating. Heat or lightning withers it, healite's healing brings it back
    public trinket heartOfTheWarrior () {
        trinket t = new trinket("Heart of the Warrior", trinket.place.CHEST, handler).health(1.1).beats();
        t.look(guiSkin.HEART_X, guiSkin.GUI_Y, null)
            .lore("Still beating. It belonged to someone who never once backed down from a fight.")
            .details("Max health: +10%");
        t.transformsInto(itemInfluence.HEAT, 1, "#6E4B3C", () -> witheredHeart()).heatedBy(itemInfluence.LIGHTNING, 1);
        return t;
    }

    //The heart of the warrior burnt out: weaker, but angrier. Healite heals it back
    public trinket witheredHeart () {
        trinket t = new trinket("Withered Heart", trinket.place.CHEST, handler).health(0.95).damage(1.1);
        t.look(1400, 1500, null).particles(itemParticles.SMOKE)
            .lore("Scorched and still. It does not beat any more, but it has not forgiven whoever did this.")
            .details("Max health: -5%. Damage of your moves: +10% (not burn or poison)");
        t.transformsInto(itemInfluence.HEALING, 1, "#DC283C", () -> heartOfTheWarrior());
        return t;
    }

    //Worn in the shoulder
    public trinket creatine () {
        trinket t = new trinket("Creatine", trinket.place.SHOULDER, handler).damage(1.1).wornScale(1.4);
        t.look(1100, 1600, null)
            .lore("A syringe of something the label calls a supplement. Nobody reads the small print.")
            .details("Damage of your moves: +10% (not burn or poison)");
        return t;
    }

    public item woolBandage () {
        return new item("Wool Bandage", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.15);}, true, handler)
            .look(guiSkin.BANDAGE_X, guiSkin.GUI_Y, null)
            .stackable(16)
            .lore("Wool from a friendly sheep, wrapped tight. Stops the bleeding, mostly.")
            .details("Heals: 15% of maxHP");
    }
}
