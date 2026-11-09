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

    public item woolBandage () {
        return new item("Wool Bandage", (c1, c2) -> {c1.heal(c1.getMaxHealth()*0.15);}, true, handler)
            .look(guiSkin.BANDAGE_X, guiSkin.GUI_Y, null)
            .stackable(16)
            .lore("Wool from a friendly sheep, wrapped tight. Stops the bleeding, mostly.")
            .details("Heals: 15% of maxHP");
    }
}
