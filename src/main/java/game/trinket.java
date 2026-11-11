package game;

import java.util.List;

//An item worn in one of a character's trinket slots (inventory.slotKind.TRINKET, two of them). While worn it changes
//the character's stats (multipliers, so several trinkets stack) and is drawn on the character at its place
//(character.setAnchor says where that is on each sprite), bobbing with it. Any character can wear them: the player
//through the inventory, others with character.wear in the gameHandler constructor. Not usable in battle
public class trinket extends item {

    public enum place {HEAD, CHEST, SHOULDER}

    private place wornAt;
    private double speed = 1, health = 1, damage = 1;
    private boolean beats = false;

    public trinket (String name, place wornAt, gameHandler handler) {
        super(name, (c1, c2) -> {}, false, handler);
        this.wornAt = wornAt;
        notForBattle();
    }

    public place getPlace () {return this.wornAt;}

    //Walking speed is multiplied by this while worn
    public trinket speed (double speed) {this.speed = speed; return this;}
    public double getSpeed () {return this.speed;}

    //Max health is multiplied by this while worn
    public trinket health (double health) {this.health = health; return this;}
    public double getHealth () {return this.health;}

    //The instant damage of the wearer's moves is multiplied by this while worn (burn and poison are not)
    public trinket damage (double damage) {this.damage = damage; return this;}
    public double getDamage () {return this.damage;}

    //How big it is drawn on a character, times character.WORN_SIZE
    private double wornScale = 1;
    public trinket wornScale (double wornScale) {this.wornScale = wornScale; return this;}
    public double getWornScale () {return this.wornScale;}

    //Beats like a heart: the icon grows and shrinks twice quickly (lub-dub), then rests, wherever it is drawn
    public trinket beats () {this.beats = true; return this;}

    public static final double BEAT_PERIOD = 0.9;

    @Override
    protected double pulseScale () {
        if (!beats) return 1;
        double t = (System.nanoTime()/1e9) % BEAT_PERIOD;
        return 1 + 0.12*bump(t, 0.08, 0.05) + 0.08*bump(t, 0.28, 0.05);
    }

    private static double bump (double t, double at, double width) {
        double d = (t - at)/width;
        return Math.exp(-d*d);
    }

    //Where it is worn, then what it does
    @Override
    public List<infoBox> getInfoBoxes () {
        List<infoBox> boxes = super.getInfoBoxes();
        String where;
        switch (wornAt) {
            case HEAD:  where = "on the head"; break;
            case CHEST: where = "on the chest"; break;
            default:    where = "on the shoulder"; break;
        }
        boxes.add(0, new infoBox(null, "Trinket", "Worn " + where, "Put it in one of the two trinket slots left of the inventory to wear it"));
        return boxes;
    }
}
