package game;

import java.util.Random;

import javafx.scene.paint.Color;

//Particle effects an item can give off wherever it is shown (inventory, active bar, battle).
//Speeds and sizes are for a 48px slot and scale with the slot
public enum itemParticles {
    NONE   (0,    new String[] {},                              0,   0, 0,   0),
    FIRE   (0.25, new String[] {"#FF4000", "#FF9000", "#FFE060"}, 0, -30, 0.6, 3),
    SMOKE  (0.10, new String[] {"#606060", "#909090"},           0, -20, 0.8, 3),
    SPARKLE(0.08, new String[] {"#FFFFFF", "#FFF080"},           0,  -5, 0.5, 2),
    TOXIC  (0.12, new String[] {"#40FF40", "#A0FF60"},           0, -15, 0.7, 2),
    FROST  (0.10, new String[] {"#B0F0FF", "#FFFFFF"},           0,  10, 0.7, 2),
    ARCANE (0.12, new String[] {"#C040FF", "#FF60E0"},           0, -12, 0.6, 2),
    HEAL   (0.08, new String[] {"#FF6080", "#FFB0C0", "#FFFFFF"}, 0, -12, 0.8, 2);

    private final double rate;   //chance per frame that a particle appears
    private final String[] colors;
    private final double vx, vy; //pixels per second
    private final double life;   //seconds
    private final double size;   //pixels

    itemParticles (double rate, String[] colors, double vx, double vy, double life, double size) {
        this.rate = rate;
        this.colors = colors;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.size = size;
    }

    //Maybe adds one particle somewhere on the item drawn at (x, y) with the given size
    public void spawn (gui gui, double x, double y, double slotSize, Random rdm) {
        if (this == NONE || rdm.nextDouble() >= rate) return;

        double scale = slotSize/48;
        Color color = Color.web(colors[rdm.nextInt(colors.length)]);
        gui.addParticle(new guiParticle(
            x + slotSize*(0.2 + rdm.nextDouble()*0.6),
            y + slotSize*(0.3 + rdm.nextDouble()*0.4),
            (vx + (rdm.nextDouble()-0.5)*15)*scale,
            vy*scale,
            life,
            Math.max(1, size*scale),
            color
        ));
    }
}
