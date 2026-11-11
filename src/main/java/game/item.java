package game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class item implements picture {

    protected String name;
    protected BiConsumer<character, character> useFunct;
    protected boolean oneUse;
    protected int useCount;
    protected gameHandler handler;

    //How the item looks: a grey-scale cell in tiles.png that is tinted with a colour, and particles it gives off
    protected int imgX = guiSkin.ROCK_X;
    protected int imgY = guiSkin.GUI_Y;
    protected String tint = null;
    protected itemParticles particles = itemParticles.NONE;

    //How many fit in one slot. 1 means the item does not stack
    protected int maxStack = 1;

    //Shown in the inventory when the item is hovered: a bit of story, and what the item does
    protected String lore = "";
    protected String details = "";

    public item (String name, BiConsumer<character, character> useFunct, boolean oneUse, gameHandler handler) {
        this.name = name;
        this.useFunct = useFunct;
        this.oneUse = oneUse;
        this.handler = handler;
    }

    public item look (int imgX, int imgY, String tint) {
        this.imgX = imgX;
        this.imgY = imgY;
        this.tint = tint;
        return this;
    }

    //An item that looks like a tile (with its underlay, like the healing pad on its crystal floor) instead of a GUI cell
    protected tileType iconTile;

    public item lookLikeTile (tileType iconTile) {
        this.iconTile = iconTile;
        return this;
    }

    //How much of its box the icon fills, 1 is all of it. Icons that fill their whole cell look big next to the rest
    protected double iconScale = 1;

    public item iconScale (double iconScale) {
        if (iconScale <= 0 || iconScale > 1) throw new IllegalArgumentException("Iconscale må være mellom 0 og 1");
        this.iconScale = iconScale;
        return this;
    }

    public item particles (itemParticles particles) {
        this.particles = particles;
        return this;
    }

    //How much more the item gives off its particles when shaken: held on the cursor and moved around fast,
    //it spawns up to 1 + shakeBoost times as many. 0 (the default) does not react to shaking
    protected double shakeBoost = 0;

    public item shakeParticles (double shakeBoost) {
        if (shakeBoost < 0) throw new IllegalArgumentException("Shakeboost kan ikke være under 0");
        this.shakeBoost = shakeBoost;
        return this;
    }

    public double getShakeBoost () {return this.shakeBoost;}

    //How many particles the item gives off when calm, as a share of its particle rate (1, the default, is the normal
    //rate). With 0 it only gives them off while shaken. shakeThreshold is how shaken it has to be before the shaking
    //counts, so the bit of charge just holding an item gives can be ignored (sand only falls off while it moves)
    protected double idleParticles = 1, shakeThreshold = 0;

    public item idleParticles (double share) {
        if (share < 0) throw new IllegalArgumentException("Share kan ikke være under 0");
        this.idleParticles = share;
        return this;
    }

    public item shakeThreshold (double threshold) {
        if (threshold < 0 || threshold >= 1) throw new IllegalArgumentException("Threshold må være fra 0 til under 1");
        this.shakeThreshold = threshold;
        return this;
    }

    //Whether the item glows while charged. Things like sand do not
    protected boolean glows = true;

    public item glows (boolean glows) {
        this.glows = glows;
        return this;
    }

    //Lightning strikes a shaken item gives off: the chance per frame of one at full shaking.
    //It grows with the square of the shaking, so a lightly shaken item only crackles now and then
    protected double lightningChance = 0;

    public item shakeLightning (double lightningChance) {
        if (lightningChance < 0 || lightningChance > 1) throw new IllegalArgumentException("Lightningchance må være mellom 0 og 1");
        this.lightningChance = lightningChance;
        return this;
    }

    public double getLightningChance () {return this.lightningChance;}

    //Which kind of lightning its strikes are (see itemInfluence) and their colour (its glow colour unless given)
    protected itemInfluence lightningKind = itemInfluence.LIGHTNING;
    protected String lightningColor;

    public item shakeLightning (double lightningChance, itemInfluence kind, String color) {
        if (!kind.isLightning()) throw new IllegalArgumentException(kind + " er ikke lyn");
        shakeLightning(lightningChance);
        this.lightningKind = kind;
        this.lightningColor = color;
        return this;
    }

    public itemInfluence getLightningKind () {return this.lightningKind;}
    public String getLightningColor () {return lightningColor != null ? lightningColor : getGlowColor();}

    //Where its strikes land, the sparks come in every colour of the rainbow instead of its own (meteorite)
    protected boolean rainbowSparks = false;

    public item rainbowSparks () {
        this.rainbowSparks = true;
        return this;
    }

    public boolean hasRainbowSparks () {return this.rainbowSparks;}

    //A rainbow item has no single tint: its colour runs through the hues, RAINBOW_TIME seconds a round. The hue moves
    //in RAINBOW_STEPS steps, so only that many tinted pictures are ever made of it
    public static final double RAINBOW_TIME = 3;
    public static final int RAINBOW_STEPS = 24;
    protected boolean rainbow = false;

    public item rainbow () {
        this.rainbow = true;
        return this;
    }

    //The tint right now: the rainbow's colour of the moment, otherwise the item's own
    public String currentTint () {
        if (!rainbow) return tint;
        double round = (System.nanoTime()/1e9/RAINBOW_TIME) % 1;
        int step = (int) (round*RAINBOW_STEPS);
        javafx.scene.paint.Color c = javafx.scene.paint.Color.hsb(360.0*step/RAINBOW_STEPS, 0.7, 1);
        return String.format("#%02X%02X%02X", (int) Math.round(c.getRed()*255), (int) Math.round(c.getGreen()*255), (int) Math.round(c.getBlue()*255));
    }

    //A stream left behind the item while it is moved on the cursor (see guiTrail): its colour, how wide it is (in gui
    //pixels, scaled with the GUI) and how long a bit of it lasts in seconds. No colour: no trail
    protected String trailColor;
    protected double trailWidth, trailLife;

    public item trail (String color, double width, double life) {
        if (width <= 0 || life <= 0) throw new IllegalArgumentException("Bredde og levetid må være over 0");
        this.trailColor = color;
        this.trailWidth = width;
        this.trailLife = life;
        return this;
    }

    public boolean hasTrail () {return trailColor != null;}
    public String getTrailColor () {return this.trailColor;}
    public double getTrailWidth () {return this.trailWidth;}
    public double getTrailLife () {return this.trailLife;}

    //Turning into another item. An item can heat up from an influence (expose): its heat rises while it is exposed and,
    //HEAT_GRACE seconds after the last exposure, cools at COOL_RATE per second. Two kinds of change hang on it:
    //- transformsInto: at dose heat it becomes another item (sandstone into glass by magma's heat), which starts hot
    //- revertsInto: once it has cooled all the way down it turns back into another item (glass back into sandstone)
    //An item can have several changes (water freezes into ice from cold and turns salty from salt), each heated by its
    //own influences and with its own heat; the first one that is due happens. heatedBy, coolsIn and carriesState set
    //up the change added last. While it is changing, the colour (color) of the change furthest along is laid over the
    //icon, stronger the further along it is.
    //The change happens in latest(), which gives the item this one has become (itself if none); whatever holds items
    //(slots, the cursor, the equipped rock) swaps to it
    public static final double COOL_RATE = 0.35, HEAT_GRACE = 0.2;

    private static class change {
        //What heats it, and how strongly each counts (1 is normal)
        java.util.Map<itemInfluence, Double> by = new java.util.EnumMap<itemInfluence, Double>(itemInfluence.class);
        double dose;
        //Heat lost per second once it is no longer heated (COOL_RATE unless coolsIn is used)
        double coolRate = COOL_RATE;
        String color;
        java.util.function.Supplier<item> into;
        boolean revert;
        //Whether the new item gets this one's state (a rock's uses left), see carriesState
        boolean carryState;

        double heat;
        long heatTime = System.nanoTime(), lastExposed;
        //Only changes that have been heated (or made hot by a change) cool back; a glass made any other way stays glass
        boolean heated = false;

        double getHeat () {
            long now = System.nanoTime();
            double coolingFrom = Math.max(heatTime, lastExposed + (long) (HEAT_GRACE*1e9));
            double cooled = Math.max(0, (now - coolingFrom)/1e9)*coolRate;
            return Math.max(0, heat - cooled);
        }

        void setHeat (double value) {
            heat = Math.max(0, Math.min(dose, value));
            heatTime = System.nanoTime();
        }

        //0 to 1: heating up towards it, or cooling down towards turning back
        double progress () {
            if (!heated) return 0;
            double share = getHeat()/dose;
            return revert ? 1 - share : share;
        }

        boolean due () {
            if (!heated) return false;
            double h = getHeat();
            return revert ? h <= 0 : h >= dose;
        }
    }

    private java.util.List<change> changes = new java.util.ArrayList<change>();
    private item replacement;

    public item transformsInto (itemInfluence by, double dose, String color, java.util.function.Supplier<item> into) {
        return addChange(by, dose, color, into, false);
    }

    public item revertsInto (itemInfluence by, double dose, String color, java.util.function.Supplier<item> into) {
        return addChange(by, dose, color, into, true);
    }

    private item addChange (itemInfluence by, double dose, String color, java.util.function.Supplier<item> into, boolean revert) {
        if (dose <= 0) throw new IllegalArgumentException("Dose må være over 0");
        change c = new change();
        c.by.put(by, 1.0);
        c.dose = dose;
        c.color = color;
        c.into = into;
        c.revert = revert;
        changes.add(c);
        return this;
    }

    private change lastChange () {
        if (changes.isEmpty()) throw new IllegalStateException("Sett transformsInto eller revertsInto først");
        return changes.get(changes.size()-1);
    }

    //Another influence that heats it too, counting factor times as much (ice melts from lightning as well as heat)
    public item heatedBy (itemInfluence influence, double factor) {
        if (factor <= 0) throw new IllegalArgumentException("Factor må være over 0");
        lastChange().by.put(influence, factor);
        return this;
    }

    //How many seconds it takes to cool from full heat to nothing (water stays water for a while)
    public item coolsIn (double seconds) {
        if (seconds <= 0) throw new IllegalArgumentException("Tiden må være over 0");
        change c = lastChange();
        c.coolRate = c.dose/seconds;
        return this;
    }

    //The item it turns into takes over its state (saveState/loadState: a rock's uses left), for changes that are the
    //same thing in another mood (copper and charged copper) rather than something new
    public item carriesState () {
        lastChange().carryState = true;
        return this;
    }

    public boolean heatsWith (itemInfluence influence) {
        for (change c : changes) if (c.by.containsKey(influence)) return true;
        return false;
    }

    //Heated by an influence this much (itemInteractions calls this every frame it is in reach): every change it heats
    public void expose (itemInfluence by, double amount) {
        if (amount <= 0) return;
        for (change c : changes) {
            if (!c.by.containsKey(by)) continue;
            c.setHeat(c.getHeat() + amount*c.by.get(by));
            c.lastExposed = System.nanoTime();
            c.heated = true;
        }
    }

    //The change furthest along, or null
    private change leading () {
        change best = null;
        for (change c : changes) if (best == null || c.progress() > best.progress()) best = c;
        return best;
    }

    //How far along the change furthest along is, 0 to 1
    public double getChangeProgress () {
        change c = leading();
        return c == null ? 0 : c.progress();
    }

    //The item this one has become, after doing any change that is due
    public item latest () {
        if (replacement != null) return replacement.latest();
        change due = null;
        for (change c : changes) {
            if (c.due()) {
                due = c;
                break;
            }
        }
        if (due == null) return this;
        replacement = due.into.get();
        //A rock turning into another form of itself keeps its uses left (all forms have as many uses of each move)
        if (due.carryState || (this instanceof gemstone && replacement instanceof gemstone)) replacement.loadState(saveState());
        //An item that turns back on its own once it cools (revertsInto) starts as hot as the one it came from;
        //one that only turns back from something else (glass by obsidian's cold) starts fresh
        if (!due.revert) {
            for (change back : replacement.changes) {
                if (!back.revert) continue;
                back.heated = true;
                back.setHeat(back.dose);
                back.lastExposed = due.lastExposed;
            }
        }
        return replacement.latest();
    }

    //For savefiles, one number: the heat of the change furthest along plus HEAT_SLOT times which change it is (so the
    //first change saves just its heat, like before there could be several), or -1 when none is heated
    public static final double HEAT_SLOT = 100;

    public double getSavedHeat () {
        change c = leading();
        if (c == null || !c.heated) return -1;
        return changes.indexOf(c)*HEAT_SLOT + c.getHeat();
    }

    public void restoreHeat (double saved) {
        if (changes.isEmpty() || saved < 0) return;
        int index = (int) (saved/HEAT_SLOT);
        if (index >= changes.size()) return;
        change c = changes.get(index);
        c.heated = true;
        c.setHeat(saved - index*HEAT_SLOT);
    }

    //The colour of the change furthest along laid over the icon, stronger the further along it is (x, y, size as for drawIcon)
    public void drawChange (GraphicsContext gc, double x, double y, double size) {
        change c = leading();
        double p = c == null ? 0 : c.progress();
        if (p <= 0.01 || iconTile != null) return;
        double scaled = size*iconScale;
        x += (size - scaled)/2;
        y += (size - scaled)/2;
        gc.save();
        gc.setGlobalAlpha(Math.min(1, 0.8*p));
        gc.drawImage(handler.getImages().getSilhouette(imgX, imgY, c.color), 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, Math.round(x), Math.round(y), Math.round(scaled), Math.round(scaled));
        gc.restore();
    }

    //Items that react to being shaken (more particles or lightning) also glow while they are
    public boolean isShakeable () {return shakeBoost > 0 || lightningChance > 0;}

    //What the item gives off to the items near it on screen (lightning comes from shakeLightning instead): how far it
    //reaches, in item sizes from middle to middle, how strong it is even when the item is calm (0: only while it
    //is charged or held), and how fast it fades with distance: the strength at a distance is (1 - distance/range)
    //to the power of falloff, so 1 fades evenly and higher drops off quickly away from the item. See itemInteractions
    public static class emission {
        public final double range, always, falloff;
        emission (double range, double always, double falloff) {
            this.range = range;
            this.always = always;
            this.falloff = falloff;
        }

        public double strengthAt (double distance) {
            if (distance >= range) return 0;
            return Math.pow(1 - distance/range, falloff);
        }
    }

    private java.util.Map<itemInfluence, emission> emissions = new java.util.EnumMap<itemInfluence, emission>(itemInfluence.class);

    public item emits (itemInfluence influence, double range) {
        return emits(influence, range, 0);
    }

    public item emits (itemInfluence influence, double range, double always) {
        return emits(influence, range, always, 1);
    }

    public item emits (itemInfluence influence, double range, double always, double falloff) {
        if (influence.isLightning()) throw new IllegalArgumentException("Lyn kommer fra shakeLightning");
        if (range <= 0) throw new IllegalArgumentException("Rekkevidde må være over 0");
        if (always < 0 || always > 1) throw new IllegalArgumentException("Always må være mellom 0 og 1");
        if (falloff <= 0) throw new IllegalArgumentException("Falloff må være over 0");
        emissions.put(influence, new emission(range, always, falloff));
        return this;
    }

    public java.util.Map<itemInfluence, emission> getEmissions () {return java.util.Collections.unmodifiableMap(emissions);}

    //A conductor: lightning strikes go for it wherever it is on screen (the closest one), and one that is hit passes
    //the strike on to the nearest conductor within this many item sizes, so a row of them lights up in a chain. 0: not one
    protected double conductRange = 0;

    public item conducts (double conductRange) {
        return conducts(conductRange, itemInfluence.LIGHTNING);
    }

    //A conductor of another kind of lightning (healite conducts the meteorite's COSMIC lightning)
    public item conducts (double conductRange, itemInfluence kind) {
        if (conductRange < 0) throw new IllegalArgumentException("Rekkevidde kan ikke være under 0");
        if (!kind.isLightning()) throw new IllegalArgumentException(kind + " er ikke lyn");
        this.conductRange = conductRange;
        this.conductKind = kind;
        return this;
    }

    protected itemInfluence conductKind = itemInfluence.LIGHTNING;

    public boolean isConductor () {return conductRange > 0;}
    public boolean conductsKind (itemInfluence kind) {return conductRange > 0 && conductKind == kind;}
    public double getConductRange () {return this.conductRange;}

    //How much debris and sparks fly off when lightning hits it, 1 is normal. Items hit all the time want less
    protected double hitDebris = 1;

    public item hitDebris (double hitDebris) {
        if (hitDebris < 0) throw new IllegalArgumentException("Hitdebris kan ikke være under 0");
        this.hitDebris = hitDebris;
        return this;
    }

    public double getHitDebris () {return this.hitDebris;}

    //What charges the item up and how strongly (1 is normal)
    private java.util.Map<itemInfluence, Double> reactions = new java.util.EnumMap<itemInfluence, Double>(itemInfluence.class);

    public item reactsTo (itemInfluence influence, double sensitivity) {
        if (sensitivity <= 0) throw new IllegalArgumentException("Følsomhet må være over 0");
        reactions.put(influence, sensitivity);
        return this;
    }

    public double getSensitivity (itemInfluence influence) {return reactions.getOrDefault(influence, 0.0);}

    //Anything with a charge to show: shaken, charged by others, or giving something off
    public boolean hasEnergy () {return isShakeable() || !reactions.isEmpty() || !emissions.isEmpty() || isConductor() || !changes.isEmpty() || restingGlow > 0;}

    //Run whenever another item charges this one (influence and how much), for gameplay to hook into later
    private java.util.function.BiConsumer<itemInfluence, Double> onCharged;

    public item onCharged (java.util.function.BiConsumer<itemInfluence, Double> onCharged) {
        this.onCharged = onCharged;
        return this;
    }

    //Charged by another item: amount more agitation, but not above ceiling (and never lower than it is)
    public void charge (double amount, double ceiling, itemInfluence by) {
        double now = getAgitation();
        if (amount <= 0 || now >= ceiling) return;
        double next = Math.min(ceiling, now + amount);
        agitation = next;
        agitationTime = System.nanoTime();
        if (onCharged != null) onCharged.accept(by, next - now);
    }

    //The last item that struck this one with lightning, and when. For a while this item's own strikes do not go
    //back at it, so two boltstones can not keep throwing lightning at each other
    public static final double STRUCK_MEMORY = 1.5;
    private item struckBy;
    private long struckTime;

    public void struckBy (item striker) {
        this.struckBy = striker;
        this.struckTime = System.nanoTime();
    }

    public boolean avoids (item other) {
        return other != null && other == struckBy && (System.nanoTime() - struckTime)/1e9 < STRUCK_MEMORY;
    }

    //The colour a shaken item glows in: its tint, white for untinted ones
    public String getGlowColor () {
        if (glowColor != null) return glowColor;
        String now = currentTint();
        return now != null ? now : "#FFFFFF";
    }

    //The colour it glows in, when not its tint (an untinted item glowing gold)
    private String glowColor;
    public item glowColor (String glowColor) {
        this.glowColor = glowColor;
        return this;
    }

    //How shaken the item is, 0 to 1. Held on the cursor it is charged by the shaking (see gui), and wherever it is
    //it settles back to 0 over SHAKE_SETTLE seconds, so a rock that is put down keeps glowing and sparking for a while.
    //Worked out from the time when asked, so items in slots need no updating
    public static final double SHAKE_SETTLE = 1.3;
    private double agitation = 0;
    private long agitationTime = System.nanoTime();

    public double getAgitation () {
        double settled = (System.nanoTime() - agitationTime)/1e9/SHAKE_SETTLE;
        //An item with a resting glow never settles below it; one that turns back once it cools (charged copper)
        //fades it out along with the heat it has left
        double resting = restingGlow*(1 - getChangeProgress());
        return Math.max(resting, agitation - settled);
    }

    //How charged the item always is, even untouched (charged copper keeps glowing a little). 0: settles to nothing
    protected double restingGlow = 0;

    public item restingGlow (double restingGlow) {
        if (restingGlow < 0 || restingGlow > 1) throw new IllegalArgumentException("Restingglow må være mellom 0 og 1");
        this.restingGlow = restingGlow;
        return this;
    }

    //Moves the agitation towards target: up quickly, down no faster than it settles on its own
    public void shake (double target, double deltaTime) {
        double now = getAgitation();
        if (target > now) now += (target - now)*Math.min(1, deltaTime*12);
        agitation = Math.max(0, Math.min(1, now));
        agitationTime = System.nanoTime();
    }

    //The glow of a shaken item in a box of size at (x, y): a soft halo of light in its colour around it...
    //(Plain blending only: a canvas blend mode like ADD is applied to the whole box around what is drawn,
    //which showed up as a square around the rock)
    private static final double GLOW = 0.6;

    public void drawGlowBehind (GraphicsContext gc, double x, double y, double size) {
        double a = hasEnergy() && glows ? getAgitation() : 0;
        if (a <= 0) return;
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(getGlowColor());
        double cx = x + size/2, cy = y + size/2, radius = size*(0.7 + 0.5*a);
        //A ring: faint over the item itself, strongest just outside its edge, fading out from there. It is drawn
        //on top of everything, so it must not hide the item
        double edge = Math.min(0.9, 0.42*size/radius);
        gc.save();
        gc.setFill(new javafx.scene.paint.RadialGradient(0, 0, cx, cy, radius, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.color(c.getRed(), c.getGreen(), c.getBlue(), GLOW*a*0.25)),
            new javafx.scene.paint.Stop(edge, javafx.scene.paint.Color.color(c.getRed(), c.getGreen(), c.getBlue(), GLOW*a)),
            new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0))));
        gc.fillOval(cx - radius, cy - radius, 2*radius, 2*radius);
        gc.restore();
    }

    //An outline drawn behind the icon: its shape in a light version of its colour, pushed out a pixel or two in every
    //direction, so the edge of a glowing item stands out clearer the more it is charged (x, y, size as for drawIcon)
    public void drawGlowOutline (GraphicsContext gc, double x, double y, double size) {
        double a = hasEnergy() && glows ? getAgitation() : 0;
        if (a <= 0.02 || iconTile != null) return;
        javafx.scene.paint.Color light = javafx.scene.paint.Color.web(getGlowColor()).interpolate(javafx.scene.paint.Color.WHITE, 0.4);
        String hex = String.format("#%02X%02X%02X", (int) Math.round(light.getRed()*255), (int) Math.round(light.getGreen()*255), (int) Math.round(light.getBlue()*255));
        javafx.scene.image.Image shape = handler.getImages().getSilhouette(imgX, imgY, hex);
        double scaled = size*iconScale;
        x += (size - scaled)/2;
        y += (size - scaled)/2;
        //One drawn pixel of the 100px cell is scaled/98 screen pixels; the outline is one to two of those, at least 1px
        double step = Math.max(1, Math.round(scaled/98*(1 + a)*2));
        gc.save();
        gc.setGlobalAlpha(Math.min(1, 0.25 + 0.75*a));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                gc.drawImage(shape, 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, Math.round(x + dx*step), Math.round(y + dy*step), Math.round(scaled), Math.round(scaled));
            }
        }
        gc.restore();
    }

    //...and the icon's shape in a light version of its colour faded in over it, so it lights up
    //(x, y, size are the icon's own box, as for drawIcon). Items that look like a tile only get the halo
    public void drawGlowOver (GraphicsContext gc, double x, double y, double size) {
        double a = hasEnergy() && glows ? getAgitation() : 0;
        if (a <= 0 || iconTile != null) return;
        javafx.scene.paint.Color light = javafx.scene.paint.Color.web(getGlowColor()).interpolate(javafx.scene.paint.Color.WHITE, 0.55);
        String hex = String.format("#%02X%02X%02X", (int) Math.round(light.getRed()*255), (int) Math.round(light.getGreen()*255), (int) Math.round(light.getBlue()*255));
        double scaled = size*iconScale;
        x += (size - scaled)/2;
        y += (size - scaled)/2;
        gc.save();
        gc.setGlobalAlpha(0.55*a);
        gc.drawImage(handler.getImages().getSilhouette(imgX, imgY, hex), 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, Math.round(x), Math.round(y), Math.round(scaled), Math.round(scaled));
        gc.restore();
    }

    //Gives off the item's particles for a box of size at (x, y), more of them and more scattered while it is shaken.
    //flingX/flingY is how fast it is being moved (screen pixels per second), for the particles to be thrown along
    public void spawnParticles (gui gui, double x, double y, double size, java.util.Random rdm, double flingX, double flingY) {
        spawnParticles(gui, x, y, size, rdm, flingX, flingY, true);
    }

    //lightning false: only the particles (a rock shown in an elemental rock's slots, whose lightning is the rock's)
    public void spawnParticles (gui gui, double x, double y, double size, java.util.Random rdm, double flingX, double flingY, boolean lightning) {
        double a = hasEnergy() ? getAgitation() : 0;
        double counted = Math.max(0, (a - shakeThreshold)/(1 - shakeThreshold));
        particles.spawn(gui, x, y, size, rdm, idleParticles + shakeBoost*counted, flingX, flingY, counted);

        //Lightning out of the middle of it (see itemInteractions.strike for where it goes)
        if (lightning && lightningChance > 0 && rdm.nextDouble() < lightningChance*a*a) {
            gui.getInteractions().strike(this, x, y, size, flingX, flingY, a, rdm);
        }
    }

    public item stackable (int maxStack) {
        if (maxStack < 1) throw new IllegalArgumentException("Maxstack kan ikke være under 1");
        this.maxStack = maxStack;
        return this;
    }

    //What right clicking the item in the inventory does (null: nothing). Shown in the info panel
    protected java.util.function.Consumer<character> inventoryAction;
    protected String inventoryActionText = "";

    public item onInventoryUse (String text, java.util.function.Consumer<character> inventoryAction) {
        this.inventoryActionText = text;
        this.inventoryAction = inventoryAction;
        return this;
    }

    public boolean hasInventoryAction () {return this.inventoryAction != null;}
    public void inventoryUse (character character) {if (inventoryAction != null) inventoryAction.accept(character);}

    //Items that do nothing in a fight do not go in the item slots of the active row
    protected boolean battleItem = true;

    public item notForBattle () {
        this.battleItem = false;
        return this;
    }

    public boolean isBattleItem () {return this.battleItem;}

    public item lore (String lore) {
        this.lore = lore;
        return this;
    }

    public item details (String details) {
        this.details = details;
        return this;
    }

    public String getLore () {return this.lore;}

    //One box per thing worth knowing. Gemstones make one per ability
    public List<infoBox> getInfoBoxes () {
        List<infoBox> boxes = new ArrayList<infoBox>();
        if (!details.isEmpty()) boxes.add(new infoBox(null, "Effect", "", details));
        if (inventoryAction != null) boxes.add(new infoBox(null, "Right click", "In the inventory", inventoryActionText));
        return boxes;
    }

    public int getMaxStack () {return this.maxStack;}
    public itemParticles getParticles () {return this.particles;}

    //Two items of the same type stack when the type allows it. Items with their own state (like gemstones) never do
    public boolean stacksWith (item other) {
        return maxStack > 1 && other != null && other.getClass() == getClass() && other.name.equals(name);
    }

    //Look options: tintLift makes the tint come out brighter and cleaner (see imgDict.getTinted), iconAlpha draws the
    //icon partly see-through (glass, water)
    protected double tintLift = 0, iconAlpha = 1;

    public item lightTint (double lift) {
        if (lift < 0 || lift >= 1) throw new IllegalArgumentException("Lift må være fra 0 til under 1");
        this.tintLift = lift;
        return this;
    }

    public item seeThrough (double alpha) {
        if (alpha <= 0 || alpha > 1) throw new IllegalArgumentException("Alpha må være over 0 og høyst 1");
        this.iconAlpha = alpha;
        return this;
    }

    //The edge of the item's shape in a colour (see imgDict.getOutline), or null for items that look like a tile
    public Image getOutlineImage (String hex, int thickness) {
        if (iconTile != null) return null;
        return handler.getImages().getOutline(imgX, imgY, hex, thickness);
    }

    public Image getIcon () {
        return handler.getImages().getTinted(imgX, imgY, currentTint(), tintLift);
    }

    //How much bigger the icon is drawn right now, around its middle (a beating heart, see trinket). 1 for most items
    protected double pulseScale () {return 1;}

    public void drawIcon (GraphicsContext gc, double x, double y, double size) {
        double scaled = size*iconScale*pulseScale();
        x += (size - scaled)/2;
        y += (size - scaled)/2;
        if (iconTile != null) {
            iconTile.drawMini(gc, handler.getImages().getImage(0), Math.round(x), Math.round(y), Math.round(scaled), Math.round(scaled));
            return;
        }
        double alpha = gc.getGlobalAlpha();
        gc.setGlobalAlpha(alpha*iconAlpha);
        gc.drawImage(getIcon(), 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, Math.round(x), Math.round(y), Math.round(scaled), Math.round(scaled));
        gc.setGlobalAlpha(alpha);
    }

    //In a box the item looks like it does in a slot: the icon with its particles
    @Override
    public void draw (GraphicsContext gc, gui gui, double x, double y, double size) {
        drawIcon(gc, x, y, size);
        spawnParticles(gui, x - size*0.125, y - size*0.125, size*1.25, gui.getRandom(), 0, 0);
    }

    //I tilfelle jeg utvider programmet og legger til flere funksjoner, som consumables
    public boolean isUsedUp () {
        return oneUse && (useCount > 0);
    }

    //Consumables disappear from the inventory when used
    public boolean isConsumable () {
        return oneUse;
    }

    protected void dialogueAdd(character you) {
        handler.getDiealoguecontainer().add(new dialogue(you, "Used " + getName()));
    }

    public void use (character character, character oppositeCharacter) {
        if (character == oppositeCharacter) throw new IllegalArgumentException("use må ta inn to ulike karakterer");
        handler.getDiealoguecontainer().setSource(this);
        useFunct.accept(character, oppositeCharacter);
        handler.getDiealoguecontainer().clearSource();
        useCount += 1;
        dialogueAdd(character);
    }

    public String getName () {
        return this.name;
    }

    public void restore() {}

    //What a savefile needs to rebuild this item besides its name (made fresh through the itemRegistry).
    //Plain items have none; items that change while played (gemstone uses) override both
    public String saveState () {return "";}
    public void loadState (String state) {}
}
