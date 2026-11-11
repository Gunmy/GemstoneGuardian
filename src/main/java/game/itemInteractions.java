package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.paint.Color;

//How items on screen affect each other. Every frame each item drawn (in a slot, on the hotbar, on the cursor)
//is noted with where it is, and the next frame works with those places:
//- radiation and heat (item.emits) charge the items near the one giving them off that react to them (item.reactsTo),
//  the closer the more, at PROXIMITY_RATE per second
//- lightning strikes (item.shakeLightning) aim for any item within reach most of the time (SEEK_CHANCE). Whatever
//  they hit bursts into debris in its own colours (pieces breaking off, only for show), and items that react to
//  lightning are charged by HIT_CHARGE
//A charge is the same agitation shaking gives (item.getAgitation), so a charged item glows, gives off more particles,
//strikes lightning and passes its own influences on. Chains are kept in check: an item is never charged by another
//above TRANSFER times that one's own level, so a loop dies out once nothing is shaking or holding it, and never above
//EXTERNAL_CAP (only shaking takes an item all the way). Held items give off at least HELD_EMIT, so just holding
//uranium over a boltstone gets it going.
//Conductors (copper, item.conducts) draw lightning: a strike goes for the closest one anywhere on screen, and a
//conductor that is hit passes it on to the nearest conductor next to it, each one once per chain, a little weaker
//every hop (LIGHTNING_TRANSFER), and now and then (SIDE_STRIKE_CHANCE) also strikes a random other item next to it,
//which takes the hit but does not pass it on
public class itemInteractions {

    private static class placed {
        item item;
        double cx, cy, size;
        boolean held;
        //A rock inside an elemental rock: which one and in which of its slots. Not drawn, no glow of its own
        elementalRock group;
        int slot = -1;
    }

    //How far apart the rocks inside an elemental rock are placed, per slot, as a share of its size
    public static final double INSIDE_SPACING = 0.22;

    public static final double EXTERNAL_CAP = 0.9;
    //TRANSFER x LIGHTNING_TRANSFER is how much of a loop's strength survives one round (boltstone to uranium and back),
    //so it has to stay below 1 for chains to die out on their own
    public static final double TRANSFER = 0.95, LIGHTNING_TRANSFER = 0.95;
    public static final double PROXIMITY_RATE = 2.5, HIT_CHARGE = 0.5;
    //How much heat a lightning hit gives an item that changes from it (half of what turns ice into water)
    public static final double HIT_HEAT = 0.5;
    public static final double HELD_EMIT = 0.7;
    public static final double SEEK_CHANCE = 0.9;
    //How often a strike from an item held on the cursor goes for a conductor (otherwise always): swinging a boltstone
    //around should throw lightning about, not only into the copper
    public static final double HELD_CONDUCTOR_CHANCE = 0.35;
    //How often a struck conductor also strikes a random item next to it (that item does not pass it on)
    public static final double SIDE_STRIKE_CHANCE = 0.5;
    //A share of the strikes, at full charge, that are long ones (up to half the screen) bursting at their end
    public static final double LONG_STRIKE_CHANCE = 0.15;

    private gui gui;
    private List<placed> filling = new ArrayList<placed>();
    private List<placed> shown = new ArrayList<placed>();

    public itemInteractions (gui gui) {
        this.gui = gui;
    }

    //Notes an item drawn in a box of size at (x, y) this frame
    public void place (item item, double x, double y, double size, boolean held) {
        placed p = new placed();
        p.item = item;
        p.cx = x + size/2;
        p.cy = y + size/2;
        p.size = size;
        p.held = held;
        filling.add(p);
        //The rocks inside an elemental rock are placed with it, a little apart, by slot
        if (item instanceof elementalRock) {
            elementalRock e = (elementalRock) item;
            e.swapChanged();
            for (int i = 0; i < elementalRock.SLOTS; i++) {
                if (e.getInside(i) == null) continue;
                placed in = new placed();
                in.item = e.getInside(i);
                in.cx = p.cx + (i - 1)*size*INSIDE_SPACING;
                in.cy = p.cy;
                in.size = size;
                in.held = held;
                in.group = e;
                in.slot = i;
                filling.add(in);
            }
        }
    }

    //Elemental rocks (see elementalRock). The rocks inside only meet each other, and only in neighbouring slots, as if
    //lying in neighbouring inventory slots (SLOT_STEP sizes apart). Everything outside meets the elemental rock instead,
    //which acts as the rocks inside: it gives off what they give off, reacts to and conducts what they do, and passes on
    //to them what reaches it. Their lightning comes out of the elemental rock and can go anywhere a rock's would

    //How far apart neighbouring slots are, in slot sizes (a 48px slot and a 4px gap)
    public static final double SLOT_STEP = 52.0/48;

    private static boolean neighboursInside (placed a, placed b) {
        return a.group != null && a.group == b.group && Math.abs(a.slot - b.slot) == 1;
    }

    //Radiation, heat, salt...: between items outside, or between neighbours inside one elemental rock
    private static boolean interacts (placed a, placed b) {
        if (a.group == null && b.group == null) return true;
        return neighboursInside(a, b);
    }

    //How far b is from a, in sizes of a
    private static double apart (placed a, placed b) {
        return neighboursInside(a, b) ? SLOT_STEP : Math.hypot(b.cx - a.cx, b.cy - a.cy)/a.size;
    }

    //Lightning from a can land on p: anything outside (from inside an elemental rock it goes out through it), but a
    //rock inside only from its neighbour inside
    private static boolean strikeCan (placed from, placed p) {
        if (p.group == null) return true;
        return from != null && neighboursInside(from, p);
    }

    //What an item is to everything outside: a rock inside is its elemental rock
    private static item face (placed p, item item) {
        return p != null && p.group != null ? p.group : item;
    }

    private placed placedOf (item item) {
        for (placed p : shown) if (p.item == item) return p;
        return null;
    }

    //At the start of drawing a frame: the places noted last frame are the ones worked with from now on
    public void newFrame () {
        shown = filling;
        filling = new ArrayList<placed>();
        shownMenus = fillingMenus;
        fillingMenus = new java.util.HashMap<elementalRock, double[]>();
    }

    //Where an open elemental rock's slots are drawn (guiInventory notes them every frame): per slot its middle x, y,
    //and the slot size last. What happens between the rocks inside is drawn there, and nowhere while it is closed
    private java.util.Map<elementalRock, double[]> fillingMenus = new java.util.HashMap<elementalRock, double[]>();
    private java.util.Map<elementalRock, double[]> shownMenus = new java.util.HashMap<elementalRock, double[]>();

    public void showInside (elementalRock rock, int slot, double x, double y, double size) {
        double[] at = fillingMenus.computeIfAbsent(rock, r -> new double[2*elementalRock.SLOTS + 1]);
        at[2*slot] = x + size/2;
        at[2*slot + 1] = y + size/2;
        at[2*elementalRock.SLOTS] = size;
    }

    //A rock inside as it is shown in its open elemental rock's slots (a copy of its place there), or null
    private placed inMenu (placed inside) {
        if (inside == null || inside.group == null) return null;
        double[] at = shownMenus.get(inside.group);
        if (at == null) return null;
        placed p = new placed();
        p.item = inside.item;
        p.cx = at[2*inside.slot];
        p.cy = at[2*inside.slot + 1];
        p.size = at[2*elementalRock.SLOTS];
        return p;
    }

    //The glow around every charged item drawn so far this frame, over everything else
    public void drawGlows (javafx.scene.canvas.GraphicsContext gc) {
        for (placed p : filling) {
            //A rock inside an elemental rock glows where it is shown, in the open slots under the inventory
            placed seen = p.group == null ? p : inMenu(p);
            if (seen != null) seen.item.drawGlowBehind(gc, seen.cx - seen.size/2, seen.cy - seen.size/2, seen.size);
        }
    }

    private boolean isHeld (item item) {
        for (placed p : shown) {
            if (p.item == item) return p.held;
        }
        return false;
    }

    private double emitLevel (placed p) {
        return Math.max(p.item.getAgitation(), p.held ? HELD_EMIT : 0);
    }

    //Radiation and heat, once per frame
    public void next (double deltaTime) {
        for (placed source : shown) {
            for (Map.Entry<itemInfluence, item.emission> emission : source.item.getEmissions().entrySet()) {
                double level = Math.max(emitLevel(source), emission.getValue().always);
                if (level <= 0) continue;
                //A heating and a cooling rock inside one elemental rock cancel out
                boolean warmth = emission.getKey() == itemInfluence.HEAT || emission.getKey() == itemInfluence.COLD;
                if (warmth && source.group != null && source.group.heatAndColdCancel()) continue;
                item.emission how = emission.getValue();
                for (placed target : shown) {
                    if (target.item == source.item || !interacts(source, target)) continue;
                    double sensitivity = target.item.getSensitivity(emission.getKey());
                    boolean heats = target.item.heatsWith(emission.getKey());
                    if (sensitivity <= 0 && !heats) continue;
                    double distance = apart(source, target);
                    double falloff = how.strengthAt(distance);
                    if (falloff <= 0) continue;
                    //Heats it towards changing into something else (sand into glass)
                    if (heats) {
                        target.item.expose(emission.getKey(), level*falloff*deltaTime);
                        if (emission.getKey() == itemInfluence.COLD) coldStream(source, target, falloff, deltaTime);
                        if (emission.getKey() == itemInfluence.HEALING) healStream(source, target, falloff, deltaTime);
                        if (emission.getKey() == itemInfluence.SALT) stream(source, target, falloff, deltaTime, itemInfluence.SALT, SALT_COLORS, 0);
                        //Something hot at work (magma melting ice, turning sand into glass): embers fly to what it heats
                        if (emission.getKey() == itemInfluence.HEAT) heatStream(source, target, falloff, deltaTime);
                    }
                    if (sensitivity <= 0) continue;
                    double ceiling = Math.min(EXTERNAL_CAP, TRANSFER*level*falloff*sensitivity);
                    target.item.charge(PROXIMITY_RATE*sensitivity*falloff*deltaTime, ceiling, emission.getKey());
                }
            }
        }
    }

    //Cold at work (obsidian, ice): frosty sparks, white and light blue with a little purple, fly from it to what it is
    //turning, and both glow while it lasts
    private static final String[] COLD_COLORS = {"#FFFFFF", "#FFFFFF", "#D8F2FF", "#A8DCFF", "#7CC8FF", "#B890FF"};
    public static final double COLD_GLOW = 0.6;

    //Heat at work (magma, lava): embers, yellow, orange and red, fly from it to what it is heating, and both glow
    private static final String[] HEAT_COLORS = {"#FFF0A0", "#FFD040", "#FF9020", "#FF5A10", "#E03008"};

    private void heatStream (placed source, placed target, double falloff, double deltaTime) {
        stream(source, target, falloff, deltaTime, itemInfluence.HEAT, HEAT_COLORS, 0);
    }

    private void coldStream (placed source, placed target, double falloff, double deltaTime) {
        stream(source, target, falloff, deltaTime, itemInfluence.COLD, COLD_COLORS, 0);
    }

    //Healing at work (healite bringing coal back): pink and red hearts of 5x5 pixels and little 3x3 pixel squares fly
    //from it to the coal, and both glow while it lasts
    private static final String[] HEAL_COLORS = {"#FFFFFF", "#FFC0D8", "#FF90B0", "#FF6090", "#FF4060", "#E02848"};
    private static final double HEART_SHARE = 0.5;
    //Salt at work (saltstone salting water): white grains fly from it to the water
    private static final String[] SALT_COLORS = {"#FFFFFF", "#FFFFFF", "#F4F4F4", "#E6E2DA"};

    private void healStream (placed source, placed target, double falloff, double deltaTime) {
        stream(source, target, falloff, deltaTime, itemInfluence.HEALING, HEAL_COLORS, HEART_SHARE);
    }

    //Particles flying from source to target while one changes the other, both glowing (up to COLD_GLOW).
    //heartShare of them are hearts (guiParticle.HEART), the rest plain squares
    private void stream (placed source, placed target, double falloff, double deltaTime, itemInfluence by, String[] colors, double heartShare) {
        source.item.charge(2*falloff*deltaTime, COLD_GLOW, by);
        target.item.charge(2*falloff*deltaTime, COLD_GLOW, by);
        //Between the rocks inside an elemental rock it flies between their open slots, and is not seen while closed
        if (source.group != null) {
            source = inMenu(source);
            target = inMenu(target);
            if (source == null || target == null) return;
        }
        Random rdm = gui.getRandom();
        double scale = source.size/48;
        int count = (int) Math.floor(2*falloff) + (rdm.nextDouble() < 2*falloff - Math.floor(2*falloff) ? 1 : 0);
        for (int i = 0; i < count; i++) {
            //From somewhere on the obsidian, to land on the target in about a third of a second
            double sx = source.cx + (rdm.nextDouble() - 0.5)*source.size*0.5, sy = source.cy + (rdm.nextDouble() - 0.5)*source.size*0.5;
            double tx = target.cx + (rdm.nextDouble() - 0.5)*target.size*0.4, ty = target.cy + (rdm.nextDouble() - 0.5)*target.size*0.4;
            double time = 0.3 + rdm.nextDouble()*0.15;
            Color color = Color.web(colors[rdm.nextInt(colors.length)]);
            if (heartShare > 0) {
                //One GUI pixel per cell: a 5x5 heart, or a 3x3 square
                double pixel = Math.max(1, Math.round(scale));
                guiParticle p = rdm.nextDouble() < heartShare
                    ? new guiParticle(sx, sy, (tx - sx)/time, (ty - sy)/time, time, pixel, color).shape(guiParticle.HEART)
                    : new guiParticle(sx, sy, (tx - sx)/time, (ty - sy)/time, time, 3*pixel, color);
                gui.addParticle(p);
                continue;
            }
            gui.addParticle(new guiParticle(sx, sy, (tx - sx)/time, (ty - sy)/time, time, Math.max(1, Math.round((2 + rdm.nextInt(2))*scale)), color));
        }
    }

    //The nearest item within reach of (x, y), other than the one striking. Lightning goes for any of them
    private placed nearestItem (item source, double x, double y, double reach) {
        return nearest(source, x, y, reach, null, null);
    }

    //Also used for conductors of one kind of lightning only (conductsKind, null: any item), leaving out the ones a
    //chain has passed already
    private placed nearest (item source, double x, double y, double reach, itemInfluence conductsKind, java.util.Set<item> passed) {
        return nearest(source, x, y, reach, conductsKind, passed, false);
    }

    //outsideOnly: leaves out the neighbours inside an elemental rock (its long strikes go out of it)
    private placed nearest (item source, double x, double y, double reach, itemInfluence conductsKind, java.util.Set<item> passed, boolean outsideOnly) {
        placed best = null;
        double bestDistance = reach;
        placed from = placedOf(source);
        item face = face(from, source);
        for (placed p : shown) {
            if (p.item == source || p.item == face || source.avoids(p.item) || face.avoids(p.item)) continue;
            if (!strikeCan(from, p) || (outsideOnly && p.group != null)) continue;
            if (conductsKind != null && !p.item.conductsKind(conductsKind)) continue;
            if (passed != null && passed.contains(p.item)) continue;
            //A neighbour inside lies one slot away, like in the inventory
            double d = from != null && neighboursInside(from, p) ? SLOT_STEP*p.size : Math.hypot(p.cx - x, p.cy - y);
            if (d < bestDistance) {
                best = p;
                bestDistance = d;
            }
        }
        return best;
    }

    //A lightning strike out of source, drawn in a box of size at (x, y) and charged a (0 to 1).
    //flingX/flingY is how fast source is being swung, strikes that find nothing to hit go along with it
    public void strike (item source, double x, double y, double size, double flingX, double flingY, double a, Random rdm) {
        double scale = size/48;
        Color color = Color.web(source.getLightningColor());
        itemInfluence kind = source.getLightningKind();
        boolean rainbow = source.hasRainbowSparks();
        double cx = x + size/2, cy = y + size/2;

        //A rock inside an elemental rock strikes out of it, as the elemental rock
        placed from = placedOf(source);
        item face = face(from, source);
        boolean isLong = rdm.nextDouble() < LONG_STRIKE_CHANCE*a;
        double reach = isLong ? gui.getHandler().getWIDTH()*(0.15 + rdm.nextDouble()*0.35) : size*(0.6 + rdm.nextDouble()*1.2)*(0.6 + 0.6*a);
        double width = isLong ? Math.round(Math.max(2, 3*scale)) : Math.round(Math.max(1, 2*scale));
        int segments = isLong ? 12 + rdm.nextInt(8) : 6 + rdm.nextInt(5);

        //A conductor anywhere on screen draws the strike, however far (only some of the time while it is held)
        placed conductor = isHeld(source) && rdm.nextDouble() >= HELD_CONDUCTOR_CHANCE ? null : nearest(source, cx, cy, Double.MAX_VALUE, kind, null, isLong);
        placed target = conductor != null ? conductor : nearest(source, cx, cy, reach, null, null, isLong);
        guiLightning strike;
        if (conductor != null) {
            double distance = Math.hypot(conductor.cx - cx, conductor.cy - cy);
            int bends = Math.max(segments, (int) (distance/(size*0.35)));
            strike = guiLightning.between(cx, cy, conductor.cx, conductor.cy, width, bends, color, rdm);
        } else if (target != null && rdm.nextDouble() < SEEK_CHANCE) {
            strike = guiLightning.between(cx, cy, target.cx, target.cy, width, segments, color, rdm);
        } else {
            double angle = Math.hypot(flingX, flingY) > 300*scale
                ? Math.atan2(flingY, flingX) + (rdm.nextDouble() - 0.5)*1.6
                : rdm.nextDouble()*2*Math.PI;
            strike = new guiLightning(cx, cy, angle, reach, width, segments, color, rdm);
            //It can still land on something by chance
            target = nearestItem(source, strike.getEndX(), strike.getEndY(), size*0.5);
        }
        //From inside an elemental rock: what goes outside comes out of the elemental rock (all of its long strikes).
        //A strike at the neighbour inside, and a short one that finds nothing, are the rock's own: drawn in the open
        //slots under the inventory, where the rocks inside are shown, and not at all while they are closed
        if (from != null && from.group != null && (target != null ? target.group != null : !isLong)) {
            placed mine = inMenu(from);
            if (mine != null) {
                double ms = mine.size/size;
                placed theirs = target != null ? inMenu(target) : null;
                if (theirs != null) gui.addLightning(guiLightning.between(mine.cx, mine.cy, theirs.cx, theirs.cy, Math.max(1, Math.round(width*ms)), segments, color, rdm));
                else if (target == null) gui.addLightning(new guiLightning(mine.cx, mine.cy, rdm.nextDouble()*2*Math.PI, reach*ms, Math.max(1, Math.round(width*ms)), segments, color, rdm));
            }
        } else {
            gui.addLightning(strike);
            //Out of a rock inside: the rock itself is seen striking in the open slots
            if (from != null && from.group != null) menuSpark(from, color, rdm);
        }

        if (isLong) burst(strike.getEndX(), strike.getEndY(), scale, color, 22 + rdm.nextInt(14), 1, rdm, rainbow);
        if (target != null) {
            java.util.Set<item> passed = new java.util.HashSet<item>();
            passed.add(source);
            passed.add(face);
            //Inside it is the rock that struck, outside it is the elemental rock
            hit(target.group != null ? source : face, target, a, scale, color, !isLong, passed, rdm, kind, rainbow);
        }
    }

    //A strike of one kind of lightning landing on target: charges it if it reacts to that kind, heats what changes from
    //it, chips bits off it, and a conductor of that kind sends it on. rainbow: the sparks are every colour (meteorite)
    private void hit (item striker, placed target, double a, double scale, Color color, boolean sparks, java.util.Set<item> passed, Random rdm, itemInfluence kind, boolean rainbow) {
        passed.add(target.item);
        double sensitivity = target.item.getSensitivity(kind);
        target.item.struckBy(striker);
        //A strike heats what changes from it (ice melting, copper charging, uranium turning rainbow)
        if (target.item.heatsWith(kind)) target.item.expose(kind, HIT_HEAT);
        if (sensitivity > 0) target.item.charge(HIT_CHARGE*sensitivity, Math.min(EXTERNAL_CAP, LIGHTNING_TRANSFER*a*sensitivity), kind);
        double amount = target.item.getHitDebris();
        //An elemental rock struck from outside: the rocks inside that the strike reaches (that conduct it, react to it
        //or change from it) are seen taking it in the open slots
        if (target.item instanceof elementalRock) {
            for (gemstone in : ((elementalRock) target.item).getInsideList()) {
                if (in.conductsKind(kind) || in.getSensitivity(kind) > 0 || in.heatsWith(kind)) menuSpark(placedOf(in), color, rdm);
            }
        }
        //A rock inside an elemental rock is seen being hit in the open slots, and not at all while they are closed
        placed seen = target.group == null ? target : inMenu(target);
        if (seen != null) {
            double seenScale = seen.size/48;
            if (sparks) burst(seen.cx, seen.cy, seenScale, color, (int) Math.round((5 + rdm.nextInt(5))*amount), 0.5, rdm, rainbow);
            debris(seen, amount, rdm);
        }

        if (!target.item.conductsKind(kind)) return;
        double weaker = a*LIGHTNING_TRANSFER;
        if (weaker < 0.05) return;
        double reach = target.item.getConductRange()*target.size;

        //Now and then a side strike at a random item close by that is not a conductor: it is hit like any other,
        //but being no conductor it does not pass the strike on
        if (rdm.nextDouble() < SIDE_STRIKE_CHANCE) {
            List<placed> close = new ArrayList<placed>();
            for (placed p : shown) {
                if (p.item.conductsKind(kind) || passed.contains(p.item) || target.item.avoids(p.item)) continue;
                if (p.item == target.group || !strikeCan(target, p)) continue;
                double d = neighboursInside(target, p) ? SLOT_STEP*target.size : Math.hypot(p.cx - target.cx, p.cy - target.cy);
                if (d < reach) close.add(p);
            }
            if (!close.isEmpty()) {
                placed side = close.get(rdm.nextInt(close.size()));
                drawChain(target, side, scale, color, rdm);
                hit(side.group != null ? target.item : passer(target, kind), side, weaker, scale, color, sparks, passed, rdm, kind, rainbow);
            }
        }

        placed next = nearest(target.item, target.cx, target.cy, reach, kind, passed);
        if (next == null) return;
        drawChain(target, next, scale, color, rdm);
        hit(next.group != null ? target.item : passer(target, kind), next, weaker, scale, color, sparks, passed, rdm, kind, rainbow);
    }

    //Who passes a strike on from target, as the item it then hits remembers it (item.avoids): an elemental rock passes it
    //through the rock inside that conducts it, so a boltstone side-struck by the healite inside only keeps clear of
    //the healite, and still strikes the elemental rock for the copper inside. Anything else passes it as itself
    private static item passer (placed target, itemInfluence kind) {
        if (target.item instanceof elementalRock) {
            gemstone conductor = ((elementalRock) target.item).conductorOf(kind);
            if (conductor != null) return conductor;
        }
        return face(target, target.item);
    }

    //A strike passed on from one item to the next. Between two rocks inside one elemental rock it is drawn in its open
    //slots (or not at all); out of a rock inside it comes out of the elemental rock
    private void drawChain (placed from, placed to, double scale, Color color, Random rdm) {
        if (to.group != null && to.group == from.group) {
            placed a = inMenu(from), b = inMenu(to);
            if (a != null && b != null) gui.addLightning(guiLightning.between(a.cx, a.cy, b.cx, b.cy, Math.round(Math.max(1, 2*a.size/48)), 5 + rdm.nextInt(3), color, rdm));
            return;
        }
        if (to.group != null) return;
        gui.addLightning(guiLightning.between(from.cx, from.cy, to.cx, to.cy, Math.round(Math.max(1, 2*scale)), 5 + rdm.nextInt(3), color, rdm));
        if (from.group != null) menuSpark(from, color, rdm);
    }

    //A small strike crackling out of a rock inside an elemental rock, in its open slots (nothing while they are closed):
    //shows which rock inside the elemental rock's lightning comes from or goes through
    private void menuSpark (placed inside, Color color, Random rdm) {
        placed seen = inMenu(inside);
        if (seen == null) return;
        double length = seen.size*(0.35 + rdm.nextDouble()*0.3);
        gui.addLightning(new guiLightning(seen.cx, seen.cy, rdm.nextDouble()*2*Math.PI, length, Math.max(1, Math.round(seen.size/48)), 4 + rdm.nextInt(3), color, rdm));
    }

    //Bits of an item that is hit flying off: chunky pieces in its own colour (darker and lighter) and in the colours of
    //its particles, thrown out every way and falling. Rocks with no particles of their own still shed chips
    private void debris (placed hit, double amount, Random rdm) {
        double scale = hit.size/48;
        Color base = Color.web(hit.item.getGlowColor());
        List<Color> colors = new ArrayList<Color>();
        colors.add(base);
        colors.add(base.darker());
        colors.add(base.brighter());
        for (String c : hit.item.getParticles().getColors()) colors.add(Color.web(c));
        int count = (int) Math.round((10 + rdm.nextInt(8))*amount);
        for (int i = 0; i < count; i++) {
            double angle = rdm.nextDouble()*2*Math.PI;
            double speed = (80 + rdm.nextDouble()*180)*scale;
            //From around the edge of the item, like pieces chipping off it
            double startX = hit.cx + Math.cos(angle)*hit.size*0.25, startY = hit.cy + Math.sin(angle)*hit.size*0.25;
            gui.addParticle(new guiParticle(startX, startY, 0, 70*scale, 0.4 + rdm.nextDouble()*0.5,
                Math.max(2, Math.round((2 + rdm.nextInt(3))*scale)), colors.get(rdm.nextInt(colors.size())))
                .fling(Math.cos(angle)*speed, Math.sin(angle)*speed - 60*scale));
        }
    }

    //An item that has just turned into another, in a box of size at (x, y): a burst in its new colours
    //A big burst of sparks, and the new item's outline jumping out from it twice, the second ring fainter and later
    public void changeBurst (item became, double x, double y, double size) {
        Random rdm = gui.getRandom();
        Color color = Color.web(became.getGlowColor());
        burst(x + size/2, y + size/2, size/48, color, 70 + rdm.nextInt(30), 1.3, rdm);
        Color light = color.interpolate(Color.WHITE, 0.5);
        String hex = String.format("#%02X%02X%02X", (int) Math.round(light.getRed()*255), (int) Math.round(light.getGreen()*255), (int) Math.round(light.getBlue()*255));
        javafx.scene.image.Image outline = became.getOutlineImage(hex, 4);
        if (outline == null) return;
        //The icon fills 80% of its box (see slotGrid.drawStack)
        double icon = size*0.8;
        gui.addOutlinePulse(new guiOutlinePulse(outline, x + size/2, y + size/2, icon, 0.4, 2.0, 1.0, 0));
        gui.addOutlinePulse(new guiOutlinePulse(outline, x + size/2, y + size/2, icon, 0.45, 2.6, 0.55, 0.12));
    }

    //Sparks flying out every way from (x, y), slowed down by air drag. power scales how fast they go
    private void burst (double x, double y, double scale, Color color, int count, double power, Random rdm) {
        burst(x, y, scale, color, count, power, rdm, false);
    }

    //rainbow: each spark in a random bright hue (and some white) instead of the colour
    private void burst (double x, double y, double scale, Color color, int count, double power, Random rdm, boolean rainbow) {
        Color[] colors = {Color.WHITE, color, color, color.brighter()};
        for (int i = 0; i < count; i++) {
            if (rainbow) colors = new Color[] {rdm.nextDouble() < 0.15 ? Color.WHITE : Color.hsb(rdm.nextDouble()*360, 0.75, 1)};
            double angle = rdm.nextDouble()*2*Math.PI;
            double speed = (60 + rdm.nextDouble()*260)*scale*power;
            gui.addParticle(new guiParticle(x, y, 0, 25*scale, 0.35 + rdm.nextDouble()*0.45,
                Math.max(1, Math.round((1 + rdm.nextInt(3))*scale)), colors[rdm.nextInt(colors.length)])
                .fling(Math.cos(angle)*speed, Math.sin(angle)*speed));
        }
    }
}
