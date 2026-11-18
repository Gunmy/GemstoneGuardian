package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//What a move looks like in battle: particles and lightning between and around the two fighters, given off for as long
//as the move's message is on screen (gameHandler.next plays the animations of the dialogue being shown, see dialogue
//.animate) and fading out after it is clicked away. Made of parts, each giving off so many particles a second:
//- stream: things flying from the user to the target in an arc (rocks, shards, drops, fire), back: the other way
//- beam: a solid ray from the user into the target, with a white hot core, a pulsing glow and sparks flying off it
//- bolt: lightning from the user to the target, skybolt: out of the sky onto the target
//- shaft: a column of light pouring down out of the sky onto one of them (the moon's and the sun's moves)
//- skyBeam: a solid beam of light straight down out of the sky onto one of them (beamParticle, the sunbeam)
//- mirrorCopy: a mirror shimmering up, a cold blue mirror image of the target flying out of it at them and shattering
//  (mirrorParticle, the Mirror Shard's Glint)
//- vortex: things spiralling into one of them, faster and faster (a black hole's pull), core: a black core over them
//- pull: moonstones and meteorites spiralling into one of them, tumbling (the singularity's pull); pullItems: the same
//  with the real Moonstone and Meteorite items turning, singularity: the
//  singularity itself over them, drawn as the inventory draws it
//- impact: sparks bursting out of one of them
//- rise: things floating up off one of them (hearts, bubbles, flames, smoke)
//- orbit: things circling one of them's head (stars when confused)
//- rain: things falling onto one of them from above
//- swirl: a spiral round one of them
//- ring: a shockwave along the ground at one of them's feet
//- toss: copies of the rock (its own tinted icon) thrown in arcs, skip: one skipping along the ground, bouncing
//- roll: a big copy of the rock rolling along the ground into the target, kicking up dust
//- meteor: a big copy of the rock coming down out of the sky onto the target, with a trail of fire behind it
//- fromSpace: a copy of the rock falling from far above the screen, burning up, with a flash where it lands
//- mood: the world goes towards night or a colour while it plays (see mood; rocks set theirs with gemstone.mood)
//- lunge: the user's sprite steps at the target now and then (a throw, a punch), shake: the target's shakes when hit
//- quake: the camera shaking, hard at first and settling
//- fountain: a spray of sparks shooting up off one of them and raining back down
//- formation: a big pixel picture made of particles (a heart, a skull, a bolt...) floating up over one of them
//- shout: a word thrown up off one of them once
//- number: once, a damage (or healing) number thrown up off them that grows as it goes and falls back (forNumber)
//Each move has a small chance (lucky) of its surprise version: its own extra parts if it has some (rare), and always a
//jackpot of gold confetti and a word ("WOW!") on top. Everything gives off RATE_SCALE times as much and
//SIZE_SCALE times as big as written, so the whole lot can be turned up or down in one place.
//Every gemstone move has one (gemstones, gemstone.animations), the rest fall back on their abilityType (forType).
//Burn, poison and healing ticks have theirs (forEffect), and a failed move a puff of smoke (failed)
public class battleAnimation {

    //Everything a part gives off, times this many, times this big
    public static final double RATE_SCALE = 1.7, SIZE_SCALE = 1.0;
    //How often a move comes out lucky
    public static final double LUCKY_CHANCE = 0.08;

    //---- Colours (all from the game's own palette) ----
    public static final String[] GREY = {"#9A9A9A", "#BEBEBE", "#6E6E6E", "#D0D0C0"};
    public static final String[] PEBBLE = {"#C0C0C0", "#E0E0E0", "#9A9A9A"};
    public static final String[] BROWN = {"#8A6A4A", "#A08860", "#6E5A48", "#B99B65"};
    public static final String[] FIRE = {"#FF0800", "#FC9F00", "#FFFA00", "#FF6A20"};
    public static final String[] LAVA = {"#FF6A20", "#F21C00", "#FC9F00", "#FFAF30"};
    public static final String[] EMBER = {"#FC9F00", "#FFE500", "#FF6A20"};
    public static final String[] SMOKE = {"#6E6E6E", "#8A8A8A", "#505050", "#3C3C3C"};
    public static final String[] STEAM = {"#E0D8D8", "#F0F0F4", "#C8C8C8"};
    public static final String[] YELLOW = {"#FFE500", "#FFFA00", "#FFFFFF", "#F0D040"};
    public static final String[] HEARTS = {"#FF4060", "#FF90B0", "#FF2050", "#FFC0D0"};
    public static final String[] POISON = {"#60D040", "#21FF2C", "#8CFF90", "#3A8A2A"};
    public static final String[] PURPLE = {"#D060FF", "#887BFE", "#B4A8FF", "#D040D0"};
    public static final String[] STARS = {"#FFE500", "#FFFFFF", "#FFFA00"};
    public static final String[] ICE = {"#7CC8FF", "#D8F4FF", "#FFFFFF", "#A0D0E0"};
    public static final String[] WATER = {"#3C8CE8", "#79BCCE", "#A0D0E0", "#4A9CF0"};
    public static final String[] SAND = {"#FFE8A8", "#E0C080", "#C8A46E", "#B99B65"};
    public static final String[] SALT = {"#F2EEE6", "#FFFFFF", "#D8D8D0"};
    public static final String[] COPPER = {"#C87533", "#F0A050", "#FFE080"};
    public static final String[] COAL = {"#3A3634", "#2E2A28", "#6E6E6E"};
    public static final String[] RADIATION = {"#60E040", "#B0FF80", "#21FF2C", "#D8FFD8"};
    public static final String[] COSMIC = {"#887BFE", "#B4A8FF", "#6A7A9A", "#FFFFFF"};
    public static final String[] RAINBOW = {"#FF0800", "#FC9F00", "#FFE500", "#21FF2C", "#3888DD", "#887BFE", "#FF70FF"};
    public static final String[] CLEAN = {"#FFFFFF", "#D8F4FF", "#8EDCF2"};
    public static final String[] PINK = {"#FF90B0", "#FFC0D0", "#FFFFFF", "#C878FF"};
    public static final String[] OBSIDIAN = {"#5A4A6A", "#8A6A9A", "#3C2B46"};
    public static final String[] GLASS = {"#F6FCFF", "#D8F4FF", "#FFFFFF", "#B8C0D0"};
    public static final String[] HURT = {"#FF0800", "#CC0D00", "#FF4060"};
    public static final String[] GOLD = {"#FFE500", "#FFFA00", "#FC9F00", "#FFFFFF", "#E0C040"};
    public static final String[] VOID = {"#887BFE", "#B4A8FF", "#3C2B46", "#D040D0", "#FFFFFF"};
    public static final String[] ACCRETION = {"#FC9F00", "#FFE500", "#FF6A20", "#887BFE", "#FFFFFF"};
    public static final String[] MIRROR = {"#C8E4F4", "#F6FCFF", "#FFFFFF", "#8EDCF2", "#B8C0D0"};
    public static final String[] MOONLIGHT = {"#D8F4FF", "#FFFFFF", "#F0F0F4", "#D8DCE4"};
    public static final String[] SUNLIGHT = {"#FFFA00", "#FFE500", "#FFFFFF", "#FC9F00"};
    //Charged healite's light: lilac and white
    public static final String[] LILAC = {"#E0C0FF", "#C878FF", "#FFFFFF", "#F0E0FF"};
    public static final String[] CONFETTI = {"#FF0800", "#FC9F00", "#FFE500", "#21FF2C", "#3888DD", "#887BFE", "#FF70FF", "#FFFFFF"};

    //---- Shapes (pixel pictures, made in code) ----
    public static final boolean[][] HEART = guiParticle.HEART;
    public static final boolean[][] STAR = guiParticle.pixels(".X.", "XXX", ".X.");
    public static final boolean[][] SPARK = guiParticle.pixels("X.X", ".X.", "X.X");
    public static final boolean[][] DROP = guiParticle.pixels(".X.", ".X.", "XXX", "XXX", ".X.");
    public static final boolean[][] SHARD = guiParticle.pixels("X..", "XX.", ".XX", "..X");
    public static final boolean[][] BUBBLE = guiParticle.pixels(".XX.", "X..X", "X..X", ".XX.");
    public static final boolean[][] FLAME = guiParticle.pixels(".X.", ".XX", "XXX", "XXX", ".X.");
    public static final boolean[][] CHUNK = guiParticle.pixels(".XX.", "XXXX", "XXXX", ".XX.");
    public static final boolean[][] SNOW = guiParticle.pixels("X.X", ".X.", "X.X");

    //Big pictures for formation, a particle per X
    public static final boolean[][] BIG_HEART = guiParticle.pixels(
        ".XX...XX.", "XXXX.XXXX", "XXXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "...XXX...", "....X....");
    public static final boolean[][] BIG_STAR = guiParticle.pixels(
        "....X....", "...XXX...", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", ".XX...XX.", "X.......X");
    public static final boolean[][] SKULL = guiParticle.pixels(
        ".XXXXX.", "XXXXXXX", "X..X..X", "XXXXXXX", ".XX.XX.", ".X.X.X.");
    public static final boolean[][] BOLT_SIGN = guiParticle.pixels(
        "...XX", "..XX.", ".XXXX", "..XX.", ".XX..", "XX...");
    public static final boolean[][] FIRE_SIGN = guiParticle.pixels(
        "...X...", "..XX...", "..XXX..", ".XXXXX.", "XXXXXXX", "XXXXXXX", ".XXXXX.");
    public static final boolean[][] SNOWFLAKE = guiParticle.pixels(
        "X..X..X", ".X.X.X.", "..XXX..", "XXXXXXX", "..XXX..", ".X.X.X.", "X..X..X");
    public static final boolean[][] QUESTION = guiParticle.pixels(
        ".XXX.", "X...X", "...X.", "..X..", ".....", "..X..");
    public static final boolean[][] CROSS = guiParticle.pixels(
        "..X..", "..X..", "XXXXX", "..X..", "..X..");
    public static final boolean[][] RING = guiParticle.pixels(
        "..XXX..", ".X...X.", "X.....X", "X.....X", "X.....X", ".X...X.", "..XXX..");
    public static final boolean[][] SMILE = guiParticle.pixels(
        ".XXXXX.", "X.....X", "X.X.X.X", "X.....X", "X.X.X.X", "X..X..X", ".XXXXX.");
    public static final boolean[][] RADIATION_SIGN = guiParticle.pixels(
        ".XX.XX.", "XXX.XXX", "XX...XX", "...X...", ".......", "..XXX..", ".XXXXX.");

    //One part of an animation: what it gives off each frame, between the user and the target
    interface part {
        void spawn (scene at, double dt);
    }

    //Where the two fighters are this frame, for the parts
    static class scene {
        gameHandler handler;
        character user, target;
        Random rdm;
        double age;
        double t;
        int world;
        double[] carry;
        //How long the move's clock runs before it starts again (battleFx.loopOf)
        double loop = battleFx.LOOP;
        //The age in the frame before (battleFx.when)
        double prev;
        //Whatever a part needs to keep between frames (when it last let go of something...)
        double[] memo;
        //A path a part keeps between frames (where a falling rock started and lands, and whether it has landed)
        double[][] paths;
        void keepPath (double x0, double y0, double x1, double y1) {paths[index] = new double[] {x0, y0, x1, y1, 0};}
        double[] path () {return paths[index];}
        int index;

        //How many to give off this frame at that rate a second (the leftover share is kept for the next frame)
        int count (double rate, double dt) {
            carry[index] += rate*RATE_SCALE*dt;
            int n = (int) carry[index];
            carry[index] -= n;
            return n;
        }

        //Exactly that rate, not scaled (rings, rolls and the like, which are one thing at a time)
        int once (double rate, double dt) {
            carry[index] += rate*dt;
            int n = (int) carry[index];
            carry[index] -= n;
            return n;
        }

        character who (boolean onTarget) {return onTarget ? target : user;}
        //The middle of a fighter's body (its sprite is a tile drawn half a tile up from its place)
        double cx (character c) {return c.getX() + 0.5*t;}
        double cy (character c) {return c.getY() + 0.05*t;}
        double headY (character c) {return c.getY() - 0.35*t;}
        double feetY (character c) {return c.getY() + 0.42*t;}

        String pick (String[] colors) {return colors[rdm.nextInt(colors.length)];}

        void add (battleParticle p) {handler.addFrontParticle(p.scale(SIZE_SCALE));}

        //A rock of this colour as a picture (the sheet's rock, tinted like the rock items are)
        javafx.scene.image.Image rock (String hex) {return handler.getImages().getTinted(guiSkin.ROCK_X, guiSkin.GUI_Y, hex);}
    }

    private List<part> parts = new ArrayList<part>();

    //---- The parts. Sizes are in tiles, rates in particles (or strikes) a second ----

    //Things flying from the user to the target in an arc (arc: how high above the straight line, in tiles), taking
    //travel seconds, with a little spread. They land and fade on the target
    public battleAnimation stream (String[] colors, double rate, double size, boolean[][] shape, double travel, double arc) {
        return flight(false, colors, rate, size, shape, travel, arc);
    }

    //The same from the target to the user (a drain: hearts taken from them)
    public battleAnimation back (String[] colors, double rate, double size, boolean[][] shape, double travel, double arc) {
        return flight(true, colors, rate, size, shape, travel, arc);
    }

    private battleAnimation flight (boolean reverse, String[] colors, double rate, double size, boolean[][] shape, double travel, double arc) {
        parts.add((at, dt) -> {
            for (int i = at.count(rate, dt); i > 0; i--) {
                character from = reverse ? at.target : at.user, to = reverse ? at.user : at.target;
                double x0 = at.cx(from) + (at.rdm.nextDouble() - 0.5)*0.3*at.t, y0 = at.cy(from) + (at.rdm.nextDouble() - 0.5)*0.4*at.t;
                double x1 = at.cx(to) + (at.rdm.nextDouble() - 0.5)*0.4*at.t, y1 = at.cy(to) + (at.rdm.nextDouble() - 0.5)*0.5*at.t;
                double time = travel*(0.85 + at.rdm.nextDouble()*0.3);
                //Thrown up so it comes down on the target after time seconds: y0 + vy*T + g*T*T/2 = y1
                double g = arc > 0 ? 8*arc*at.t/(time*time) : 0;
                double vx = (x1 - x0)/time, vy = (y1 - y0)/time - g*time/2;
                battleParticle p = new battleParticle(at.handler, at.world, x0, y0, vx, vy, time*1.15, size*at.t*(0.8 + at.rdm.nextDouble()*0.4), at.pick(colors)).gravity(g);
                if (shape != null) p.shape(shape);
                else p.spin(at.rdm.nextDouble()*2 - 1);
                at.add(p);
            }
        });
        return this;
    }

    //A solid ray from the user's hand into the target: every frame it is filled in along its length with short-lived
    //small particles, a thin white hot core in the middle and the glow (colors) round it, the glow's width pulsing
    //(width in tiles, how far they spread). Sparks fly off it sideways now and then
    private static final double BEAM_RATE = 600;
    private static final int BEAM_LIGHTS = 6;
    public battleAnimation beam (String[] colors, double width) {
        parts.add((at, dt) -> {
            double x0 = at.cx(at.user) + Math.signum(at.target.getX() - at.user.getX())*0.3*at.t, y0 = at.cy(at.user) - 0.05*at.t;
            double x1 = at.cx(at.target), y1 = at.cy(at.target);
            double length = Math.hypot(x1 - x0, y1 - y0);
            if (length <= 0) return;
            double nx = -(y1 - y0)/length, ny = (x1 - x0)/length;
            double pulse = 0.75 + 0.25*Math.sin(at.age*24);
            //Enough to fill the ray, the more the longer it is: BEAM_RATE specks a second per tile of it, so the same
            //whatever the frame rate (it used to be a number per frame, thousands at once on a long beam)
            int pieces = at.count(length/at.t*BEAM_RATE, dt);
            for (int i = 0; i < pieces; i++) {
                double f = at.rdm.nextDouble(), side = (at.rdm.nextDouble() - 0.5)*width*at.t*pulse;
                boolean core = i % 3 == 0;
                double x = x0 + (x1 - x0)*f + nx*(core ? side*0.2 : side), y = y0 + (y1 - y0)*f + ny*(core ? side*0.2 : side);
                //Small specks: the width only spreads them out
                double size = (core ? 0.04 : 0.045 + at.rdm.nextDouble()*0.035)*at.t;
                battleParticle p = new battleParticle(at.handler, at.world, x, y, (x1 - x0)/length*0.8*at.t, (y1 - y0)/length*0.8*at.t,
                    0.06 + at.rdm.nextDouble()*0.06, size, core ? "#FFFFFF" : at.pick(colors));
                //In the dark only one in BEAM_LIGHTS lights up round it: each light is a soft circle much bigger
                //than the speck, so a few of them light the beam as well as all of them, for far less drawing
                if (at.rdm.nextInt(BEAM_LIGHTS) != 0) p.noLight();
                at.add(p);
            }
            for (int i = at.count(14, dt); i > 0; i--) {
                double f = at.rdm.nextDouble(), out = (at.rdm.nextBoolean() ? 1 : -1)*(0.8 + at.rdm.nextDouble()*1.4)*at.t;
                at.add(new battleParticle(at.handler, at.world, x0 + (x1 - x0)*f, y0 + (y1 - y0)*f, nx*out, ny*out,
                    0.3 + at.rdm.nextDouble()*0.3, 0.04*at.t, at.pick(colors)).drag(3).grow(-0.6).shape(SPARK));
            }
        });
        return this;
    }

    //Lightning from the user to the target, rate strikes a second, ending somewhere on their body
    public battleAnimation bolt (String color, double rate) {
        parts.add((at, dt) -> {
            for (int i = at.count(rate, dt); i > 0; i--) {
                at.handler.strike(at.world, at.cx(at.user), at.cy(at.user) - 0.1*at.t,
                    at.cx(at.target) + (at.rdm.nextDouble() - 0.5)*0.5*at.t, at.cy(at.target) + (at.rdm.nextDouble() - 0.5)*0.6*at.t, color);
            }
        });
        return this;
    }

    //Lightning striking down out of the sky onto the target, rate strikes a second
    public battleAnimation skybolt (String color, double rate) {
        parts.add((at, dt) -> {
            for (int i = at.count(rate, dt); i > 0; i--) {
                double x = at.cx(at.target) + (at.rdm.nextDouble() - 0.5)*0.6*at.t;
                at.handler.strike(at.world, x + (at.rdm.nextDouble() - 0.5)*1.2*at.t, at.headY(at.target) - 3*at.t,
                    x, at.cy(at.target) + (at.rdm.nextDouble() - 0.5)*0.4*at.t, color);
            }
        });
        return this;
    }

    //A shaft of light coming down out of the sky onto one of them: specks pouring straight down a column from far
    //above the screen (width in tiles), brightest in the middle, with a pulse, splashing a little where it lands
    public battleAnimation shaft (boolean onTarget, String[] colors, double width) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            double pulse = 0.75 + 0.25*Math.sin(at.age*14);
            double bottom = at.feetY(c), top = bottom - 6*at.t;
            for (int i = at.count(160, dt); i > 0; i--) {
                boolean core = i % 3 == 0;
                double x = at.cx(c) + (at.rdm.nextDouble() - 0.5)*width*at.t*pulse*(core ? 0.3 : 1);
                double y = top + at.rdm.nextDouble()*(bottom - top);
                double speed = (4 + at.rdm.nextDouble()*3)*at.t;
                double life = Math.min(0.35, (bottom - y)/speed + 0.05);
                at.add(new battleParticle(at.handler, at.world, x, y, 0, speed, life, (core ? 0.05 : 0.035)*at.t, core ? "#FFFFFF" : at.pick(colors)).grow(-0.3));
            }
            for (int i = at.count(14, dt); i > 0; i--) {
                double angle = Math.PI + at.rdm.nextDouble()*Math.PI, speed = (0.5 + at.rdm.nextDouble())*at.t;
                at.add(new battleParticle(at.handler, at.world, at.cx(c), bottom, Math.cos(angle)*speed, Math.sin(angle)*speed, 0.4, 0.04*at.t, at.pick(colors)).gravity(2*at.t).drag(2));
            }
        });
        return this;
    }

    //A solid beam of light straight down out of the sky onto one of them (beamParticle), width tiles wide, flickering:
    //a new one every moment, each a little wider or narrower, glowing in glow round a body of body
    public battleAnimation skyBeam (boolean onTarget, double width, String glow, String body) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.once(30, dt); i > 0; i--) {
                double w = width*at.t*(0.8 + at.rdm.nextDouble()*0.4);
                at.handler.addFrontParticle(new beamParticle(at.handler, at.world, at.cx(c), at.feetY(c), 8*at.t, w, 0.09, glow, body));
            }
        });
        return this;
    }

    //How far the mirror always stands from both fighters, in tiles
    public static final double MIRROR_GAP = 2.0;

    //A mirror shimmering up between them (MIRROR_GAP from both) every every seconds, and out of it a cold blue mirror image of the
    //target (mirrorParticle) flying at them, shattering into glass where it hits. The copy takes travel seconds
    public battleAnimation mirrorCopy (double every, double travel) {
        parts.add((at, dt) -> {
            double side = Math.signum(at.target.getX() - at.user.getX());
            //Never on top of either of them: halfway between, and if that is less than MIRROR_GAP from them, moved up off
            //the line between them until it is that far from both
            double ux = at.cx(at.user), uy = at.feetY(at.user), tx = at.cx(at.target), ty = at.feetY(at.target);
            double half = Math.hypot(tx - ux, ty - uy)/2, gap = MIRROR_GAP*at.t;
            double up = half < gap ? Math.sqrt(gap*gap - half*half) : 0;
            double mx = (ux + tx)/2, my = (uy + ty)/2 - up;
            double mirrorLife = travel + 0.35;
            if (at.once(1/every, dt) > 0) {
                at.memo[at.index] = at.age;
                //Standing in the world: behind the fighters and anything tall, like the mirrors on the walls
                at.handler.addBehindParticle(new mirrorParticle(at.handler, at.world, mx, my, 0.55*at.t, mirrorLife));
                //It appears like the cutscenes' mirrors do
                at.handler.mirrorAppears(mx, my - 0.55*at.t, at.world);
            }
            double since = at.age - at.memo[at.index];
            //...and goes like them, blown apart
            if (since >= mirrorLife - 0.08 && since - dt < mirrorLife - 0.08) at.handler.mirrorVanishes(mx, my - 0.55*at.t, at.world);
            //The copy steps out once the mirror stands
            if (since >= 0.2 && since - dt < 0.2) {
                at.handler.addFrontParticle(new mirrorParticle(at.handler, at.world, mx, my - 0.5*at.t, at.cx(at.target) - side*0.1*at.t, at.cy(at.target),
                    0.9*at.t, travel, at.target.getTintedIcon("#C8E4F4")));
            }
            //It shatters on them
            if (since >= 0.2 + travel && since - dt < 0.2 + travel) {
                for (int i = 0; i < 26; i++) {
                    double angle = at.rdm.nextDouble()*2*Math.PI, speed = (0.6 + at.rdm.nextDouble()*1.4)*at.t;
                    at.add(new battleParticle(at.handler, at.world, at.cx(at.target), at.cy(at.target), Math.cos(angle)*speed, Math.sin(angle)*speed - 0.4*at.t,
                        0.5 + at.rdm.nextDouble()*0.3, 0.05*at.t, at.pick(MIRROR)).shape(SHARD).gravity(2.5*at.t).drag(2).spin(at.rdm.nextDouble()*2 - 1));
                }
            }
        });
        return this;
    }

    //A black hole's pull on one of them: things spiralling in from radius (tiles) round them, faster and faster,
    //shrinking into the middle (battleParticle.spiral)
    public battleAnimation vortex (boolean onTarget, String[] colors, double rate, double size, double radius) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(rate, dt); i > 0; i--) {
                double life = 0.7 + at.rdm.nextDouble()*0.5;
                at.add(new battleParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, life, size*at.t*(0.6 + at.rdm.nextDouble()*0.8), at.pick(colors))
                    .spiral(at.cx(c), at.cy(c), radius*at.t*(0.6 + at.rdm.nextDouble()*0.5), at.rdm.nextDouble()*2*Math.PI, 1.2 + at.rdm.nextDouble(), 0.55).grow(-0.8));
            }
        });
        return this;
    }

    //A black core over one of them, size tiles across, wobbling: black squares that give off no light, so it eats
    //into the night round it
    public battleAnimation core (boolean onTarget, double size) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(40, dt); i > 0; i--) {
                double wobble = (at.rdm.nextDouble() - 0.5)*size*0.25*at.t;
                at.add(new battleParticle(at.handler, at.world, at.cx(c) + wobble, at.cy(c) + wobble, 0, 0, 0.12, size*at.t*(0.8 + at.rdm.nextDouble()*0.3), "#000000")
                    .spin(at.rdm.nextDouble()*0.5));
            }
        });
        return this;
    }

    //The singularity over one of them, size tiles across, drawn as the inventory draws it (the Singularity item: its
    //texture, white outline and dark glow). One every half of CORE_LIFE, so never more than two at once: one fully
    //there while the one before fades
    private static final double CORE_LIFE = 1.2;
    public battleAnimation singularity (boolean onTarget, double size) {
        //The item to draw, made once per game
        item[] held = new item[1];
        gameHandler[] heldFor = new gameHandler[1];
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            if (heldFor[0] != at.handler) {
                heldFor[0] = at.handler;
                held[0] = at.handler.getItemRegistry() == null ? null : at.handler.getItemRegistry().make("Singularity");
            }
            if (held[0] == null) return;
            for (int i = at.once(2/CORE_LIFE, dt); i > 0; i--) {
                at.add(new battleParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, CORE_LIFE, size*at.t, gemstones.SINGULARITY_COLOR)
                    .item(held[0]));
            }
        });
        return this;
    }

    //The singularity's pull on one of them: moonstones and meteorites (the gemstones' own colours, as round the black
    //hole in space) spiralling in from radius (tiles) round them, faster and faster, tumbling and shrinking as they go
    public battleAnimation pull (boolean onTarget, double rate, double size, double radius) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(rate, dt); i > 0; i--) {
                double life = 0.8 + at.rdm.nextDouble()*0.5;
                String hex = at.rdm.nextBoolean() ? gemstones.MOON_COLOR : spaceView.METEORITE_COLOR;
                at.add(new battleParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, life, size*at.t*(0.7 + at.rdm.nextDouble()*0.6), hex)
                    .image(at.rock(hex)).spin((at.rdm.nextDouble() - 0.5)*4)
                    .spiral(at.cx(c), at.cy(c), radius*at.t*(0.6 + at.rdm.nextDouble()*0.5), at.rdm.nextDouble()*2*Math.PI, 1.2 + at.rdm.nextDouble(), 0.55).grow(-0.8));
            }
        });
        return this;
    }

    //The same pull with the real Moonstone and Meteorite items (their own textures, as in the inventory) turning as
    //they spiral in, instead of tinted rocks
    public battleAnimation pullItems (boolean onTarget, double rate, double size, double radius) {
        item[] held = new item[2];
        gameHandler[] heldFor = new gameHandler[1];
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            if (heldFor[0] != at.handler) {
                heldFor[0] = at.handler;
                itemRegistry registry = at.handler.getItemRegistry();
                held[0] = registry == null ? null : registry.make("Moonstone");
                held[1] = registry == null ? null : registry.make("Meteorite");
            }
            if (held[0] == null || held[1] == null) return;
            for (int i = at.count(rate, dt); i > 0; i--) {
                double life = 0.8 + at.rdm.nextDouble()*0.5;
                at.add(new battleParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, life, size*at.t*(0.7 + at.rdm.nextDouble()*0.6), gemstones.MOON_COLOR)
                    .item(held[at.rdm.nextInt(2)]).spin((at.rdm.nextBoolean() ? 1 : -1)*(1 + at.rdm.nextDouble()*2))
                    .spiral(at.cx(c), at.cy(c), radius*at.t*(0.6 + at.rdm.nextDouble()*0.5), at.rdm.nextDouble()*2*Math.PI, 1.2 + at.rdm.nextDouble(), 0.55).grow(-0.8));
            }
        });
        return this;
    }

    //Sparks bursting out of one of them in every direction, slowing down and falling a little
    public battleAnimation impact (boolean onTarget, String[] colors, double rate, double size) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(rate, dt); i > 0; i--) {
                double angle = at.rdm.nextDouble()*2*Math.PI, speed = (0.6 + at.rdm.nextDouble()*1.6)*at.t;
                at.add(new battleParticle(at.handler, at.world, at.cx(c) + (at.rdm.nextDouble() - 0.5)*0.3*at.t, at.cy(c) + (at.rdm.nextDouble() - 0.5)*0.3*at.t,
                    Math.cos(angle)*speed, Math.sin(angle)*speed - 0.3*at.t, 0.35 + at.rdm.nextDouble()*0.4, size*at.t*(0.6 + at.rdm.nextDouble()*0.8), at.pick(colors))
                    .gravity(2.5*at.t).drag(2.5).spin(at.rdm.nextDouble()*2 - 1).grow(-0.6));
            }
        });
        return this;
    }

    //Things floating up off one of them, swaying a little and fading at the top
    public battleAnimation rise (boolean onTarget, String[] colors, double rate, double size, boolean[][] shape) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(rate, dt); i > 0; i--) {
                double x = at.cx(c) + (at.rdm.nextDouble() - 0.5)*0.7*at.t, y = at.cy(c) + (at.rdm.nextDouble() - 0.2)*0.5*at.t;
                battleParticle p = new battleParticle(at.handler, at.world, x, y, (at.rdm.nextDouble() - 0.5)*0.15*at.t, -(0.35 + at.rdm.nextDouble()*0.35)*at.t,
                    0.8 + at.rdm.nextDouble()*0.6, size*at.t*(0.7 + at.rdm.nextDouble()*0.6), at.pick(colors));
                if (shape != null) p.shape(shape);
                else p.grow(-0.4);
                at.add(p);
            }
        });
        return this;
    }

    //Things circling one of them's head, round and round while the message is up
    public battleAnimation orbit (boolean onTarget, String[] colors, double rate, double size, boolean[][] shape) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(rate, dt); i > 0; i--) {
                //Placed on the ring where it is now and sent along it: short-lived, so one after another they go round
                double angle = at.age*4 + at.rdm.nextDouble()*2*Math.PI, r = 0.33*at.t;
                double x = at.cx(c) + Math.cos(angle)*r, y = at.headY(c) + Math.sin(angle)*r*0.35;
                double speed = 4*r;
                battleParticle p = new battleParticle(at.handler, at.world, x, y, -Math.sin(angle)*speed, Math.cos(angle)*speed*0.35,
                    0.25 + at.rdm.nextDouble()*0.2, size*at.t, at.pick(colors));
                if (shape != null) p.shape(shape);
                at.add(p);
            }
        });
        return this;
    }

    //Things falling onto one of them from above (meteors, lava, snow, rain)
    public battleAnimation rain (boolean onTarget, String[] colors, double rate, double size, boolean[][] shape) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(rate, dt); i > 0; i--) {
                double x = at.cx(c) + (at.rdm.nextDouble() - 0.5)*0.9*at.t, y = at.headY(c) - (0.9 + at.rdm.nextDouble()*0.5)*at.t;
                double fall = at.feetY(c) - y, time = 0.45 + at.rdm.nextDouble()*0.2;
                battleParticle p = new battleParticle(at.handler, at.world, x, y, (at.rdm.nextDouble() - 0.5)*0.2*at.t, fall/time*0.6,
                    time, size*at.t*(0.7 + at.rdm.nextDouble()*0.6), at.pick(colors)).gravity(fall/(time*time)*0.8);
                if (shape != null) p.shape(shape);
                else p.spin(at.rdm.nextDouble()*2 - 1);
                at.add(p);
            }
        });
        return this;
    }

    //A spiral round one of them: particles set off round the body and drift up while they go round
    public battleAnimation swirl (boolean onTarget, String[] colors, double rate, double size) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(rate, dt); i > 0; i--) {
                double angle = at.age*6 + at.rdm.nextDouble()*0.8, r = (0.35 + at.rdm.nextDouble()*0.15)*at.t;
                double x = at.cx(c) + Math.cos(angle)*r, y = at.feetY(c) - at.rdm.nextDouble()*0.8*at.t;
                double speed = 5*r;
                at.add(new battleParticle(at.handler, at.world, x, y, -Math.sin(angle)*speed, -0.25*at.t,
                    0.3 + at.rdm.nextDouble()*0.25, size*at.t*(0.7 + at.rdm.nextDouble()*0.6), at.pick(colors)).drag(1.5));
            }
        });
        return this;
    }

    //A shockwave along the ground at one of them's feet: now and then a ring of particles flies out flat from there
    public battleAnimation ring (boolean onTarget, String[] colors, double ringsPerSecond, double size) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int n = at.once(ringsPerSecond, dt); n > 0; n--) {
                int count = 24;
                for (int i = 0; i < count; i++) {
                    double angle = 2*Math.PI*i/count, speed = 1.4*at.t;
                    at.add(new battleParticle(at.handler, at.world, at.cx(c), at.feetY(c), Math.cos(angle)*speed, Math.sin(angle)*speed*0.3,
                        0.45, size*at.t, at.pick(colors)).drag(3).grow(-0.5));
                }
            }
        });
        return this;
    }

    //Copies of the rock thrown from the user's hand in arcs, spinning, onto the target
    public battleAnimation toss (String hex, double rate, double size, double travel, double arc) {
        parts.add((at, dt) -> {
            for (int i = at.count(rate, dt); i > 0; i--) {
                double x0 = at.cx(at.user) + (at.target.getX() > at.user.getX() ? 0.3 : -0.3)*at.t, y0 = at.cy(at.user) - 0.1*at.t;
                double x1 = at.cx(at.target) + (at.rdm.nextDouble() - 0.5)*0.4*at.t, y1 = at.cy(at.target) + (at.rdm.nextDouble() - 0.5)*0.4*at.t;
                double time = travel*(0.85 + at.rdm.nextDouble()*0.3);
                double g = 8*arc*at.t/(time*time);
                at.add(new battleParticle(at.handler, at.world, x0, y0, (x1 - x0)/time, (y1 - y0)/time - g*time/2, time*1.1,
                    size*at.t*(0.8 + at.rdm.nextDouble()*0.4), hex).gravity(g).image(at.rock(hex)).spin((at.rdm.nextDouble() - 0.5)*3));
            }
        });
        return this;
    }

    //A rock skipped along the ground at the target: thrown low, it bounces a few times on the way
    public battleAnimation skip (String hex, double rate, double size) {
        parts.add((at, dt) -> {
            for (int i = at.count(rate, dt); i > 0; i--) {
                double ground = at.feetY(at.user);
                double x0 = at.cx(at.user), x1 = at.cx(at.target), time = 0.9;
                at.add(new battleParticle(at.handler, at.world, x0, ground - 0.2*at.t, (x1 - x0)/time, -1.2*at.t, time, size*at.t, hex)
                    .gravity(9*at.t).bounce(ground, 0.65).image(at.rock(hex)).spin(2));
            }
        });
        return this;
    }

    //A big copy of the rock rolling along the ground from the user into the target, every every seconds, turning as
    //it rolls and kicking up dust (in dust's colours) behind it
    public battleAnimation roll (String hex, double every, double size, String[] dust) {
        double travel = 0.7;
        parts.add((at, dt) -> {
            double s = size*at.t*SIZE_SCALE;
            double x0 = at.cx(at.user), x1 = at.cx(at.target), y = at.feetY(at.user) - s/2 + 0.05*at.t;
            if (at.once(1/every, dt) > 0) {
                double turns = (x1 - x0)/(Math.PI*s);
                at.handler.addFrontParticle(new battleParticle(at.handler, at.world, x0, y, (x1 - x0)/travel, 0, travel*1.15, s, hex).image(at.rock(hex)).spin(turns*1.15));
                at.memo[at.index] = at.age;
            }
            //Dust from under it while it is on its way
            double since = at.age - at.memo[at.index];
            if (since >= 0 && since < travel) {
                double x = x0 + (x1 - x0)*since/travel;
                for (int i = 0; i < 3; i++) {
                    at.add(new battleParticle(at.handler, at.world, x - Math.signum(x1 - x0)*s*0.4, y + s*0.4, -Math.signum(x1 - x0)*at.rdm.nextDouble()*0.5*at.t,
                        -(0.2 + at.rdm.nextDouble()*0.4)*at.t, 0.4 + at.rdm.nextDouble()*0.3, 0.06*at.t, at.pick(dust)).drag(2).grow(0.8));
                }
            }
        });
        return this;
    }

    //A big copy of the rock falling out of the sky onto the target every every seconds, from high up behind the user,
    //with a trail of fire (trail's colours) behind it
    public battleAnimation meteor (String hex, double every, double size, String[] trail) {
        double travel = 0.55;
        parts.add((at, dt) -> {
            double s = size*at.t*SIZE_SCALE;
            double side = at.target.getX() > at.user.getX() ? -1 : 1;
            double x0 = at.cx(at.target) + side*1.4*at.t, y0 = at.headY(at.target) - 2.2*at.t;
            double x1 = at.cx(at.target), y1 = at.cy(at.target);
            if (at.once(1/every, dt) > 0) {
                at.handler.addFrontParticle(new battleParticle(at.handler, at.world, x0, y0, (x1 - x0)/travel, (y1 - y0)/travel, travel, s, hex).image(at.rock(hex)).spin(1.5));
                at.memo[at.index] = at.age;
            }
            double since = at.age - at.memo[at.index];
            if (since >= 0 && since < travel) {
                double x = x0 + (x1 - x0)*since/travel, y = y0 + (y1 - y0)*since/travel;
                for (int i = 0; i < 4; i++) {
                    at.add(new battleParticle(at.handler, at.world, x + (at.rdm.nextDouble() - 0.5)*s*0.5, y + (at.rdm.nextDouble() - 0.5)*s*0.5,
                        0, -0.1*at.t, 0.3 + at.rdm.nextDouble()*0.25, s*0.2*(0.6 + at.rdm.nextDouble()*0.6), at.pick(trail)).grow(-0.7));
                }
            }
        });
        return this;
    }

    //A rock falling from space onto the target every every seconds: from far above the screen, almost straight down
    //and fast, burning up into a long trail (trail's colours) as it comes, with a flash where it lands
    public battleAnimation fromSpace (String hex, double every, double size, String[] trail) {
        double travel = 0.7;
        parts.add((at, dt) -> {
            double s = size*at.t*SIZE_SCALE;
            double side = at.target.getX() > at.user.getX() ? -1 : 1;
            if (at.once(1/every, dt) > 0) {
                double x1 = at.cx(at.target) + (at.rdm.nextDouble() - 0.5)*0.2*at.t, y1 = at.cy(at.target);
                double x0 = x1 + side*0.9*at.t, y0 = y1 - 6*at.t;
                at.handler.addFrontParticle(new battleParticle(at.handler, at.world, x0, y0, (x1 - x0)/travel, (y1 - y0)/travel, travel, s, hex).image(at.rock(hex)).spin(2));
                at.memo[at.index] = at.age;
                at.keepPath(x0, y0, x1, y1);
            }
            double since = at.age - at.memo[at.index];
            double[] path = at.path();
            if (path == null) return;
            if (since >= 0 && since < travel) {
                double x = path[0] + (path[2] - path[0])*since/travel, y = path[1] + (path[3] - path[1])*since/travel;
                for (int i = 0; i < 6; i++) {
                    at.add(new battleParticle(at.handler, at.world, x + (at.rdm.nextDouble() - 0.5)*s*0.4, y - at.rdm.nextDouble()*s*0.8,
                        0, -0.05*at.t, 0.35 + at.rdm.nextDouble()*0.3, s*0.22*(0.6 + at.rdm.nextDouble()*0.6), at.pick(trail)).grow(-0.7));
                }
            } else if (since >= travel && path[4] == 0) {
                //The flash where it lands, once
                path[4] = 1;
                for (int i = 0; i < 18; i++) {
                    double angle = at.rdm.nextDouble()*Math.PI, speed = (0.8 + at.rdm.nextDouble()*1.4)*at.t;
                    at.add(new battleParticle(at.handler, at.world, path[2], path[3], Math.cos(angle)*speed, -Math.sin(angle)*speed, 0.4 + at.rdm.nextDouble()*0.3,
                        s*0.18, at.pick(trail)).gravity(3*at.t).drag(2));
                }
            }
        });
        return this;
    }

    //The user steps at the target and back every every seconds, as if throwing or hitting (far: how far, in tiles)
    public battleAnimation lunge (double every, double far) {
        parts.add((at, dt) -> {
            double phase = (at.age % every)/every;
            //A quick step forward in the first third, back over the rest
            double step = phase < 0.3 ? Math.sin(phase/0.3*Math.PI/2) : Math.cos((phase - 0.3)/0.7*Math.PI/2);
            double dir = Math.signum(at.target.getX() - at.user.getX());
            at.user.nudge(dir*step*far*at.t, -Math.abs(step)*0.05*at.t);
        });
        return this;
    }

    //The target (or the user, onTarget false) shaking from side to side, hard tiles a time
    public battleAnimation shake (boolean onTarget, double hard) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            c.nudge(Math.sin(at.age*55)*hard*at.t*(0.6 + 0.4*Math.sin(at.age*7)), 0);
        });
        return this;
    }

    //The camera shaking, hard (tiles) at first and settling over a second or so
    public battleAnimation quake (double hard) {
        parts.add((at, dt) -> {
            double amount = hard*at.t*(0.25 + 0.75*Math.exp(-at.age*2.5));
            at.handler.getCamera().shake(Math.sin(at.age*47)*amount, Math.cos(at.age*61)*amount*0.6);
        });
        return this;
    }

    //A spray of sparks shooting up off one of them, spreading out and raining back down
    public battleAnimation fountain (boolean onTarget, String[] colors, double rate, double size, boolean[][] shape) {
        parts.add((at, dt) -> {
            character c = at.who(onTarget);
            for (int i = at.count(rate, dt); i > 0; i--) {
                double spread = (at.rdm.nextDouble() - 0.5)*1.6*at.t;
                battleParticle p = new battleParticle(at.handler, at.world, at.cx(c) + (at.rdm.nextDouble() - 0.5)*0.2*at.t, at.cy(c),
                    spread, -(2.2 + at.rdm.nextDouble()*1.4)*at.t, 0.9 + at.rdm.nextDouble()*0.5, size*at.t*(0.7 + at.rdm.nextDouble()*0.6), at.pick(colors))
                    .gravity(4.5*at.t).spin(at.rdm.nextDouble()*2 - 1);
                if (shape != null) p.shape(shape);
                at.add(p);
            }
        });
        return this;
    }

    //A big pixel picture (a particle for every X of shape) over one of them every every seconds, floating up as it
    //fades, each particle in one of the colours
    public battleAnimation formation (boolean onTarget, boolean[][] shape, String[] colors, double every, double cell) {
        parts.add((at, dt) -> {
            if (at.once(1/every, dt) == 0) return;
            character c = at.who(onTarget);
            int rows = shape.length, cols = shape[0].length;
            double step = cell*at.t*SIZE_SCALE;
            double left = at.cx(c) - (cols - 1)*step/2, top = at.headY(c) - 0.4*at.t - rows*step;
            for (int r = 0; r < rows; r++) {
                for (int col = 0; col < cols; col++) {
                    if (!shape[r][col]) continue;
                    at.handler.addFrontParticle(new battleParticle(at.handler, at.world, left + col*step, top + r*step,
                        (at.rdm.nextDouble() - 0.5)*0.05*at.t, -0.35*at.t, 1.1 + at.rdm.nextDouble()*0.2, step*0.95, at.pick(colors)));
                }
            }
        });
        return this;
    }

    //A word (LUCKY!, WOW!...) thrown up off one of them once, bigger than a damage number
    public battleAnimation shout (boolean onTarget, String text, String hex) {
        parts.add((at, dt) -> {
            if (at.memo[at.index] > -50) return;
            at.memo[at.index] = at.age;
            character c = at.who(onTarget);
            at.add(new battleParticle(at.handler, at.world, at.cx(c), at.headY(c) - 0.5*at.t, 0, -1.4*at.t, 1.6, 0.22*at.t, hex)
                .text(text).gravity(1.2*at.t).grow(1.2));
        });
        return this;
    }

    //A number thrown up off the target once (the first frame): small at first, growing as it goes up, then falling
    public battleAnimation number (String text, String hex) {
        parts.add((at, dt) -> {
            if (at.memo[at.index] > -50) return;
            at.memo[at.index] = at.age;
            character c = at.target;
            double side = (at.rdm.nextDouble() - 0.5)*0.6*at.t;
            at.add(new battleParticle(at.handler, at.world, at.cx(c) + side, at.headY(c), side*1.2, -2.0*at.t, 1.3, 0.16*at.t, hex)
                .text(text).gravity(3.2*at.t).grow(1.6));
        });
        return this;
    }

    //The surprise version of this move: these parts too, when it comes out lucky (see playing.roll)
    private battleAnimation rare;
    //One of the drawn kinds (battleFx: cracks, crystals, pools, waves...) as a part
    public battleAnimation then (part p) {
        parts.add(p);
        return this;
    }

    //A mood of the move's own, over its rock's (a thunderbolt darkening the sky of a rock that has none)
    private mood own;
    public battleAnimation dark (mood m) {
        this.own = m;
        return this;
    }

    public battleAnimation rare (battleAnimation extra) {
        this.rare = extra;
        return this;
    }

    //What every lucky move gets on top: a burst of sparks in the colours of the rock it came from (colors) and a word
    private static final String[] WORDS = {"WOW!", "LUCKY!", "BOOM!", "NICE!", "WHOA!", "EPIC!"};
    private static battleAnimation jackpot (Random rdm, String[] colors) {
        return new battleAnimation().fountain(true, colors, 40, 0.06, null)
            .shout(false, WORDS[rdm.nextInt(WORDS.length)], colors[1]).impact(false, colors, 20, 0.05);
    }

    //The colours of the rock a move key (M:rock:move) is from: its colour, a lighter one and white; every colour only
    //for rainbow uranium. Gold when it is from no rock
    private static String[] rockColors (character user, String key) {
        String[] parts = key.split(":");
        if (parts.length < 2 || user == null || user.getHandler() == null || user.getHandler().getItemRegistry() == null) return GOLD;
        item it = user.getHandler().getItemRegistry().make(parts[1]);
        if (it == null) return GOLD;
        if (parts[1].equals("Rainbow Uranium")) return CONFETTI;
        String hex = it.getTint() == null ? "#FFE500" : it.getTint();
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex), light = c.interpolate(javafx.scene.paint.Color.WHITE, 0.5);
        return new String[] {hex, toHex(light), "#FFFFFF", toHex(c.interpolate(javafx.scene.paint.Color.WHITE, 0.25))};
    }

    private static String toHex (javafx.scene.paint.Color c) {
        return String.format("#%02X%02X%02X", (int) Math.round(c.getRed()*255), (int) Math.round(c.getGreen()*255), (int) Math.round(c.getBlue()*255));
    }

    //---- Damage numbers ----
    public static final String DAMAGE = "#FF4040", BURN = "#FC9F00", POISONED = "#A8E040", HEALED = "#FF90B0";

    private static String numberColor (boolean heal, picture cause) {
        return heal ? HEALED : cause == abilityType.FIRE ? BURN : cause == abilityType.POISON ? POISONED : DAMAGE;
    }

    //The number for damage done (or healing, heal true) by what caused it (the dialogue's source: burn and poison
    //get their own colour)
    public static battleAnimation forNumber (double amount, boolean heal, picture cause) {
        return new battleAnimation().number((heal ? "+" : "-") + guiText.num(amount) + " " + textIcons.forAmount(heal, cause), numberColor(heal, cause));
    }

    //Its key for a 1v1: N:colour:text
    public static String numberKey (double amount, boolean heal, picture cause) {
        return "N:" + numberColor(heal, cause) + ":" + (heal ? "+" : "-") + guiText.num(amount) + " " + textIcons.forAmount(heal, cause);
    }

    //---- Mood ----
    //How a move colours the world while it plays: dark (0 to 1) how far towards night, tint a colour laid over
    //everything with tintStrength (0 to 1), glow (0 to 1) how strongly the move's particles shine. Every rock's moves
    //set its mood (gemstone.mood, DAY when it has none) and it stays after the move until the next move sets another;
    //gameHandler eases from the one to the other (dark straight into pink, never through day), and back to DAY when
    //the fight is over. Drawn over the world and the fighters, under the moves' particles, so those stand out
    public static class mood {
        public final double dark, tintStrength, glow;
        public final javafx.scene.paint.Color tint;

        public mood (double dark, String tint, double tintStrength, double glow) {
            this.dark = dark;
            this.tint = javafx.scene.paint.Color.web(tint);
            this.tintStrength = tintStrength;
            this.glow = glow;
        }

        public static final mood DAY = new mood(0, "#FFFFFF", 0, 0);
        //Night, with the moves glowing
        public static mood night (double dark) {return new mood(dark, "#FFFFFF", 0, 1);}
        //Night with a coloured glow over it (magma's red)
        public static mood night (double dark, String tint, double tintStrength) {return new mood(dark, tint, tintStrength, 1);}
        //Day in a colour (healite's pink)
        public static mood tinted (String tint, double tintStrength) {return new mood(0, tint, tintStrength, 0.4);}
    }

    //Sets the mood while it plays
    public battleAnimation mood (mood m) {
        parts.add((at, dt) -> at.handler.setBattleMood(m));
        return this;
    }

    //A copy with a mood on top (its surprise version kept)
    public battleAnimation withMood (mood m) {
        battleAnimation copy = new battleAnimation();
        copy.parts.addAll(parts);
        copy.rare = rare;
        copy.own = own;
        mood used = own != null ? own : m;
        //In the dark both fighters keep a soft light, so a move without glowing bits of its own does not lose them
        if (used.dark > 0) copy.parts.add(battleFx.aura("#FFF4E0"));
        return copy.mood(used);
    }

    //---- Luck ----
    //A move that worked at a low hit chance gets a word thrown off its user: at or below LUCKY (80%) "LUCKY!", at or
    //below VERY_LUCKY (60%) "VERY LUCKY!", at or below SUPER_LUCKY (50%) "SUPER LUCKY!". A move that failed gets "UNLUCKY..."
    public static final double LUCKY = 0.8, VERY_LUCKY = 0.6, SUPER_LUCKY = 0.5;

    //The key of the word for a move used at chance (0 to 1) that worked or not, null when there is none. Played with
    //playing like any key, so a 1v1 shows it on both sides
    public static String luckKey (double chance, boolean worked) {
        if (!worked) return shoutKey("UNLUCKY...", "#A0A0A0");
        if (chance <= SUPER_LUCKY) return shoutKey("SUPER LUCKY!", "#FF70FF");
        if (chance <= VERY_LUCKY) return shoutKey("VERY LUCKY!", "#40C0FF");
        if (chance <= LUCKY) return shoutKey("LUCKY!", "#60E060");
        return null;
    }

    //A word thrown off the user: S:colour:text
    public static String shoutKey (String text, String hex) {return "S:" + hex + ":" + text;}

    //---- Playing ----

    //An animation playing between two fighters, for as long as its message is on screen. key says which it is, so a
    //1v1 can send it over (see duel): M:rock name:move (ending :R when lucky), E:effect, F (failed), N:colour:number,
    //S:colour:word (thrown off the user)
    public static class playing {
        private final battleAnimation animation;
        private final character user, target;
        private final String key;
        private final double[] carry, memo;
        private final double[][] paths;
        private final double loop;
        private double age = 0;
        private final Random rdm = new Random();

        public playing (battleAnimation animation, character user, character target, String key) {
            //A lucky one (key ending :R, see roll) plays its surprise too
            if (key.endsWith(":R")) {
                battleAnimation all = new battleAnimation();
                all.parts.addAll(animation.parts);
                if (animation.rare != null) all.parts.addAll(animation.rare.parts);
                all.parts.addAll(jackpot(new Random(key.hashCode()), rockColors(user, key)).parts);
                animation = all;
            }
            this.animation = animation;
            this.user = user;
            this.target = target;
            this.key = key;
            this.carry = new double[animation.parts.size()];
            this.memo = new double[animation.parts.size()];
            this.paths = new double[animation.parts.size()][];
            this.loop = battleFx.loopOf(animation.parts);
            //Every part lets go of its first one straight away
            java.util.Arrays.fill(carry, 0.999);
            java.util.Arrays.fill(memo, -100);
        }

        //A move used: lucky now and then (its key then ends :R, so a 1v1 plays the same surprise on both sides)
        public static playing roll (battleAnimation animation, character user, character target, String key, Random rdm) {
            return new playing(animation, user, target, rdm.nextDouble() < LUCKY_CHANCE ? key + ":R" : key);
        }

        public character getUser () {return this.user;}
        public character getTarget () {return this.target;}
        public String getKey () {return this.key;}

        public void next (gameHandler handler, double dt) {
            if (user == null || target == null || user.getWorld() != handler.getCurrentWorld()) return;
            //The age before this frame, exactly as it was (the first frame from just before 0, so beats at 0 fire)
            double before = age == 0 ? -1e-9 : age;
            age += dt;
            scene at = new scene();
            at.prev = before;
            at.handler = handler;
            at.user = user;
            at.target = target;
            at.rdm = rdm;
            at.age = age;
            at.loop = loop;
            at.t = 1.0/handler.getTileCount();
            at.world = user.getWorld();
            at.carry = carry;
            at.memo = memo;
            at.paths = paths;
            for (int i = 0; i < animation.parts.size(); i++) {
                at.index = i;
                animation.parts.get(i).spawn(at, dt);
            }
        }
    }

    //The animation a key names (made again on the other side of a 1v1), null if unknown
    public static battleAnimation fromKey (String key, itemRegistry registry) {
        if (key.equals("F")) return failed();
        if (key.startsWith("N:")) {
            String[] f = key.split(":", 3);
            return f.length == 3 ? new battleAnimation().number(f[2], f[1]) : null;
        }
        if (key.startsWith("S:")) {
            String[] f = key.split(":", 3);
            return f.length == 3 ? new battleAnimation().shout(false, f[2], f[1]) : null;
        }
        if (key.startsWith("E:")) {
            try {
                return forEffect(abilityType.valueOf(key.substring(2)));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (key.startsWith("M:")) {
            //A lucky one's :R stays on the key (playing reads it), the move is what comes before it
            if (key.endsWith(":R")) key = key.substring(0, key.length() - 2);
            int split = key.lastIndexOf(':');
            if (split <= 2) return null;
            item rock = registry.make(key.substring(2, split));
            if (!(rock instanceof gemstone)) return null;
            try {
                return ((gemstone) rock).getAnimation(Integer.parseInt(key.substring(split + 1)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    //---- Ready-made ones ----

    //A move that failed: a puff of grey smoke off the user, and a question mark over them
    public static battleAnimation failed () {
        return new battleAnimation().rise(false, SMOKE, 10, 0.09, null);
    }

    //A burn, poison or healing tick on whoever it is on (the user and the target are the same character)
    public static battleAnimation forEffect (abilityType kind) {
        switch (kind) {
            case FIRE:   return new battleAnimation().rise(false, FIRE, 22, 0.07, FLAME).rise(false, SMOKE, 5, 0.08, null).impact(false, EMBER, 6, 0.04)
                             .fountain(false, EMBER, 8, 0.04, FLAME);
            case POISON: return new battleAnimation().rise(false, POISON, 14, 0.07, BUBBLE).rise(false, POISON, 6, 0.04, null)
                             .shake(false, 0.015);
            default:     return new battleAnimation().rise(false, HEARTS, 10, 0.08, HEART);
        }
    }

    //What a move does when its rock has nothing of its own for it
    public static battleAnimation forType (abilityType kind) {
        switch (kind) {
            case FIRE:      return new battleAnimation().stream(FIRE, 14, 0.06, FLAME, 0.4, 0.3).rise(true, FIRE, 14, 0.07, FLAME).impact(true, EMBER, 10, 0.04);
            case POISON:    return new battleAnimation().stream(POISON, 8, 0.06, DROP, 0.45, 0.5).rise(true, POISON, 12, 0.07, BUBBLE);
            case CONFUSE:   return new battleAnimation().orbit(true, PURPLE, 16, 0.07, STAR).swirl(true, PURPLE, 12, 0.04);
            case LIGHTNING: return new battleAnimation().bolt("#FFE500", 6).impact(true, YELLOW, 25, 0.05);
            case HEAL:      return new battleAnimation().rise(false, HEARTS, 12, 0.08, HEART);
            case DRAIN:     return new battleAnimation().impact(true, HURT, 15, 0.05).back(HEARTS, 8, 0.08, HEART, 0.6, 0.4);
            case CLEANSE:   return new battleAnimation().rise(false, CLEAN, 14, 0.06, SPARK).swirl(false, CLEAN, 12, 0.04);
            case RESTORE:   return new battleAnimation().orbit(false, PINK, 12, 0.07, STAR).rise(false, PINK, 12, 0.05, SPARK);
            default:        return new battleAnimation().stream(GREY, 5, 0.1, CHUNK, 0.35, 0.4).impact(true, GREY, 15, 0.05);
        }
    }
}
