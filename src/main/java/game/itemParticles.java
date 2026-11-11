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
    HEAL   (0.08, new String[] {"#FF6080", "#FFB0C0", "#FFFFFF"}, 0, -12, 0.8, 2),
    HEAL_PAD(0.06, new String[] {"#21FF2C"},                    0, -10, 0.9, 3),  //the green the healing pads give off
    //Grains that fall with gravity and tumble as they go
    SAND   (0.05, new String[] {"#E8D29A", "#D8BC7A", "#C8A060", "#F2E2B8"}, 0, 0, 1.2, 3.5, 700, 14, 0.6),
    //White grains of salt drifting slowly down
    SALT   (0.06, new String[] {"#FFFFFF", "#F4F4F4", "#E6E2DA"}, 0, 12, 0.7, 2),
    //Drops that drip off the item and fall: tall, not spinning
    WATER  (0.07, new String[] {"#3C8CE8", "#6CB0F8", "#A8D8FF"}, 0, 10, 0.9, 2.5, 500, 0, 1.5),
    //Water's drops, molten
    LAVA   (0.07, new String[] {"#FF4A10", "#FF8A20", "#FFC040"}, 0, 10, 0.9, 2.5, 500, 0, 1.5);

    private final double rate;   //chance per frame that a particle appears
    private final String[] colors;
    private final double vx, vy; //pixels per second
    private final double life;   //seconds
    private final double size;   //pixels
    //Physics (for a 48px slot): gravity in pixels per second squared, the most it spins in radians per second either
    //way, and the shape of a grain (height/width). 0 gravity and spin is the plain drifting square
    private final double gravity, spin, aspect;

    itemParticles (double rate, String[] colors, double vx, double vy, double life, double size) {
        this(rate, colors, vx, vy, life, size, 0, 0, 1);
    }

    itemParticles (double rate, String[] colors, double vx, double vy, double life, double size, double gravity, double spin, double aspect) {
        this.rate = rate;
        this.colors = colors;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.size = size;
        this.gravity = gravity;
        this.spin = spin;
        this.aspect = aspect;
    }

    public String[] getColors () {return colors.clone();}
    //For particles given off in the world (a worn trinket, see character.drawWorn): chance per frame, speed in pixels
    //per second and size in pixels for a 48px slot, and how long they live
    public double getRate () {return this.rate;}
    public double getVx () {return this.vx;}
    public double getVy () {return this.vy;}
    public double getLife () {return this.life;}
    public double getSize () {return this.size;}

    //Maybe adds one particle somewhere on the item drawn at (x, y) with the given size
    public void spawn (gui gui, double x, double y, double slotSize, Random rdm) {
        spawn(gui, x, y, slotSize, rdm, 1);
    }

    //With the rate multiplied (a shaken item): on average rate x multiplier particles this frame
    public void spawn (gui gui, double x, double y, double slotSize, Random rdm, double multiplier) {
        spawn(gui, x, y, slotSize, rdm, multiplier, 0, 0, 0);
    }

    //A shaken item being swung: the particles also get part of the item's velocity (screen pixels per second),
    //and scatter more the harder it is shaken (shaking 0 to 1)
    public void spawn (gui gui, double x, double y, double slotSize, Random rdm, double multiplier, double itemVx, double itemVy, double shaking) {
        if (this == NONE) return;
        double expected = rate*multiplier;
        int count = (int) Math.floor(expected) + (rdm.nextDouble() < expected - Math.floor(expected) ? 1 : 0);
        double scatter = shaking*120*slotSize/48;
        for (int i = 0; i < count; i++) {
            guiParticle p = spawnOne(gui, x, y, slotSize, rdm);
            //Each takes between half and all of the swing, so they spread out along it
            double share = 0.5 + rdm.nextDouble()*0.5;
            p.fling(itemVx*share + (rdm.nextDouble()-0.5)*2*scatter, itemVy*share + (rdm.nextDouble()-0.5)*2*scatter);
        }
    }

    private guiParticle spawnOne (gui gui, double x, double y, double slotSize, Random rdm) {
        double scale = slotSize/48;
        Color color = Color.web(colors[rdm.nextInt(colors.length)]);
        guiParticle particle = new guiParticle(
            x + slotSize*(0.2 + rdm.nextDouble()*0.6),
            y + slotSize*(0.3 + rdm.nextDouble()*0.4),
            (vx + (rdm.nextDouble()-0.5)*15)*scale,
            vy*scale,
            life*(gravity > 0 ? 0.7 + rdm.nextDouble()*0.6 : 1),
            Math.max(1, size*scale),
            color
        );
        if (gravity != 0 || spin != 0) {
            particle.physics(gravity*scale, (rdm.nextDouble()*2 - 1)*spin, rdm.nextDouble()*Math.PI, aspect);
        }
        gui.addParticle(particle);
        return particle;
    }
}
