package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//A rock that holds up to two other rocks in its three slots and becomes them. Right clicking it in the inventory opens
//its slots under the inventory (guiInventory). Move n is move n of the rock in slot n: a rock in slot 1 gives its
//first move, one in slot 3 its third, and the slot left empty gives no move. Using a move uses up the rock inside.
//It also takes on everything else about the rocks inside: their particles, the lightning they throw, what they give
//off (radiation, salt, healing...), what they react to and conduct. A rock that heats things (magma, lava) and one that
//cools them (obsidian, ice) cancel out: neither heats nor cools anything, inside or out.
//For the items' interactions (itemInteractions.place) the rocks inside are placed where the elemental rock is, a little
//apart, so they act on each other as if lying next to each other, but only when their slots are next to each other
//(slots 1 and 3 with the middle empty do nothing to each other), and whatever reaches the elemental rock reaches them.
//They change like they would anywhere (healite charging, copper charging): the elemental rock then has the new one's moves
public class elementalRock extends gemstone {

    public static final int SLOTS = 3, MAX_ROCKS = 2;
    private gemstone[] inside = new gemstone[SLOTS];

    public elementalRock (gameHandler handler) {
        super("Elemental Rock", "#C8B8FF", itemParticles.NONE, handler,
            "Empty", abilityType.ATTACK, "", (c1, c2) -> {}, 0, 0,
            "Empty", abilityType.ATTACK, "", (c1, c2) -> {}, 0, 0,
            "Empty", abilityType.ATTACK, "", (c1, c2) -> {}, 0, 0);
        rainbow();
        lore("A hollow stone with three sockets. It becomes whatever you set in it.");
        onInventoryUse("Opens or closes its three rock slots, under the inventory", c -> handler.getGui().toggleElemental(this));
    }

    public gemstone getInside (int slot) {return inside[slot];}

    //The rocks inside with their slot, for itemInteractions
    public List<gemstone> getInsideList () {
        List<gemstone> list = new ArrayList<gemstone>();
        for (gemstone g : inside) if (g != null) list.add(g);
        return list;
    }

    private int count () {return getInsideList().size();}

    //Whether a rock inside heats things and another cools them, so the two cancel out
    public boolean heatAndColdCancel () {
        boolean heat = false, cold = false;
        for (gemstone g : getInsideList()) {
            if (g.getEmissions().containsKey(itemInfluence.HEAT)) heat = true;
            if (g.getEmissions().containsKey(itemInfluence.COLD)) cold = true;
        }
        return heat && cold;
    }

    //A rock inside that has turned into another (healite charged by a meteorite) is swapped for what it became
    public void swapChanged () {
        for (int i = 0; i < SLOTS; i++) {
            if (inside[i] != null && inside[i].latest() != inside[i] && inside[i].latest() instanceof gemstone) {
                inside[i] = (gemstone) inside[i].latest();
            }
        }
    }

    //Its three slots as a slotGroup, for the menu under the inventory. Only rocks, never another elemental rock, and at
    //most two of them: with two in, the third slot stays empty (swapping one for another still works)
    public slotGroup slots () {
        elementalRock self = this;
        return new slotGroup() {
            @Override public int size () {return SLOTS;}
            @Override public itemStack get (int i) {return inside[i] == null ? null : new itemStack(inside[i]);}
            @Override public void set (int i, itemStack stack) {inside[i] = stack == null ? null : (gemstone) stack.getItem();}
            @Override public boolean accepts (item item) {return item instanceof gemstone && !(item instanceof elementalRock) && item != self;}
            @Override public inventory.slotKind hint () {return inventory.slotKind.ROCK;}
            //The rocks inside take part through the elemental rock (see itemInteractions.place), not from the menu
            @Override public boolean takesPart () {return false;}
            @Override public itemStack click (int i, itemStack cursor, boolean rightClick) {
                if (!rightClick && cursor != null && inside[i] == null && count() >= MAX_ROCKS) return cursor;
                return slotGroup.super.click(i, cursor, rightClick);
            }
        };
    }

    //Moves ---------------------------------------------------------------------------------------------------------

    private static final List<Integer> NO_USES = Arrays.asList(0, 0);

    @Override public String getAbility1name () {return inside[0] == null ? "Empty" : inside[0].getAbility1name();}
    @Override public String getAbility2name () {return inside[1] == null ? "Empty" : inside[1].getAbility2name();}
    @Override public String getAbility3name () {return inside[2] == null ? "Empty" : inside[2].getAbility3name();}

    @Override public abilityType getAbility1type () {return inside[0] == null ? abilityType.ATTACK : inside[0].getAbility1type();}
    @Override public abilityType getAbility2type () {return inside[1] == null ? abilityType.ATTACK : inside[1].getAbility2type();}
    @Override public abilityType getAbility3type () {return inside[2] == null ? abilityType.ATTACK : inside[2].getAbility3type();}

    @Override public List<Integer> howManyAbility1Uses () {return inside[0] == null ? NO_USES : inside[0].howManyAbility1Uses();}
    @Override public List<Integer> howManyAbility2Uses () {return inside[1] == null ? NO_USES : inside[1].howManyAbility2Uses();}
    @Override public List<Integer> howManyAbility3Uses () {return inside[2] == null ? NO_USES : inside[2].howManyAbility3Uses();}

    @Override public boolean ability1usesLeft () {return inside[0] != null && inside[0].ability1usesLeft();}
    @Override public boolean ability2usesLeft () {return inside[1] != null && inside[1].ability2usesLeft();}
    @Override public boolean ability3usesLeft () {return inside[2] != null && inside[2].ability3usesLeft();}

    @Override public void ability1 (character you, character them) {if (inside[0] != null) inside[0].ability1(you, them);}
    @Override public void ability2 (character you, character them) {if (inside[1] != null) inside[1].ability2(you, them);}
    @Override public void ability3 (character you, character them) {if (inside[2] != null) inside[2].ability3(you, them);}

    @Override
    public List<Double> chances (character you) {
        List<Double> chances = new ArrayList<Double>();
        for (int i = 0; i < SLOTS; i++) chances.add(inside[i] == null ? 0.0 : inside[i].chances(you).get(i));
        return chances;
    }

    @Override
    public boolean hasMovesLeft () {return ability1usesLeft() || ability2usesLeft() || ability3usesLeft();}

    //Move n's box from the rock in slot n, or an empty one
    @Override
    public List<infoBox> getInfoBoxes () {
        List<infoBox> boxes = new ArrayList<infoBox>();
        for (int i = 0; i < SLOTS; i++) {
            if (inside[i] == null) boxes.add(new infoBox(null, "Empty", "Slot " + (i+1), "Put a rock in slot " + (i+1) + " to get its move " + (i+1)));
            else boxes.add(inside[i].getInfoBoxes().get(i));
        }
        if (inventoryAction != null) boxes.add(new infoBox(null, "Right click", "In the inventory", inventoryActionText));
        return boxes;
    }

    @Override
    public void restore () {
        for (gemstone g : getInsideList()) g.restore();
    }

    //Everything else about the rocks inside ------------------------------------------------------------------------

    //Its own particles are the ones of the rocks inside (and their lightning, which comes out of the elemental rock)
    @Override
    public void spawnParticles (gui gui, double x, double y, double size, java.util.Random rdm, double flingX, double flingY) {
        for (gemstone g : getInsideList()) g.spawnParticles(gui, x, y, size, rdm, flingX, flingY);
    }

    //Shaken on the cursor, the rocks inside are shaken with it
    @Override
    public void shake (double target, double deltaTime) {
        super.shake(target, deltaTime);
        for (gemstone g : getInsideList()) g.shake(target, deltaTime);
    }

    //To everything outside it acts as the rocks inside (itemInteractions places them for meeting each other only) ----

    //Shaken on the cursor if a rock inside would be, and the shaking goes to them
    @Override
    public boolean isShakeable () {
        for (gemstone g : getInsideList()) if (g.isShakeable()) return true;
        return false;
    }

    //Changes from what changes a rock inside (lightning charging copper, heat melting ice, shaking breaking glass)
    @Override
    public boolean heatsWith (itemInfluence influence) {
        for (gemstone g : getInsideList()) if (g.heatsWith(influence)) return true;
        return false;
    }

    //...and passes it on to the rocks it changes
    @Override
    public void expose (itemInfluence by, double amount) {
        for (gemstone g : getInsideList()) if (g.heatsWith(by)) g.expose(by, amount);
    }

    //Gives off what the rocks inside give off (heat and cold cancel out when both are there)
    @Override
    public java.util.Map<itemInfluence, emission> getEmissions () {
        java.util.Map<itemInfluence, emission> all = new java.util.EnumMap<itemInfluence, emission>(itemInfluence.class);
        boolean cancel = heatAndColdCancel();
        for (gemstone g : getInsideList()) {
            for (java.util.Map.Entry<itemInfluence, emission> e : g.getEmissions().entrySet()) {
                if (cancel && (e.getKey() == itemInfluence.HEAT || e.getKey() == itemInfluence.COLD)) continue;
                all.putIfAbsent(e.getKey(), e.getValue());
            }
        }
        return java.util.Collections.unmodifiableMap(all);
    }

    //Reacts to what the rocks inside react to, and a charge goes to the ones that react to it
    @Override
    public double getSensitivity (itemInfluence influence) {
        double most = 0;
        for (gemstone g : getInsideList()) most = Math.max(most, g.getSensitivity(influence));
        return most;
    }

    //A charge (or the glow of being at work) goes to the rocks inside it is about: the ones that react to it, that change
    //from it, or that give it off (healite healing, obsidian cooling, magma heating, saltstone salting glow while they do)
    @Override
    public void charge (double amount, double ceiling, itemInfluence by) {
        super.charge(amount, ceiling, by);
        for (gemstone g : getInsideList()) {
            if (g.getSensitivity(by) > 0 || g.heatsWith(by) || g.getEmissions().containsKey(by)) g.charge(amount, ceiling, by);
        }
    }

    //Conducts what the rocks inside conduct (copper and healite inside: both kinds of lightning)
    @Override
    public boolean isConductor () {
        for (gemstone g : getInsideList()) if (g.isConductor()) return true;
        return false;
    }

    @Override
    public boolean conductsKind (itemInfluence kind) {
        for (gemstone g : getInsideList()) if (g.conductsKind(kind)) return true;
        return false;
    }

    //The rock inside that conducts this kind of lightning (it is the one that passes a strike on), or null
    public gemstone conductorOf (itemInfluence kind) {
        for (gemstone g : getInsideList()) if (g.conductsKind(kind)) return g;
        return null;
    }

    @Override
    public double getConductRange () {
        double most = 0;
        for (gemstone g : getInsideList()) most = Math.max(most, g.getConductRange());
        return most;
    }

    //Struck from outside, the rocks inside remember who struck them, so they do not strike straight back
    @Override
    public void struckBy (item striker) {
        super.struckBy(striker);
        for (gemstone g : getInsideList()) g.struckBy(striker);
    }


    //As charged as the most charged rock inside, so it glows when they do
    @Override
    public double getAgitation () {
        double a = super.getAgitation();
        for (gemstone g : getInsideList()) a = Math.max(a, g.getAgitation());
        return a;
    }

    @Override
    public boolean hasEnergy () {
        for (gemstone g : getInsideList()) if (g.hasEnergy()) return true;
        return false;
    }

    //Swung around, it pours out like the first rock inside that does (water, lava)
    private gemstone trailing () {
        for (gemstone g : getInsideList()) if (g.hasTrail()) return g;
        return null;
    }
    @Override public boolean hasTrail () {return trailing() != null;}
    @Override public String getTrailColor () {return trailing() == null ? null : trailing().getTrailColor();}
    @Override public double getTrailWidth () {return trailing() == null ? 0 : trailing().getTrailWidth();}
    @Override public double getTrailLife () {return trailing() == null ? 0 : trailing().getTrailLife();}

    //Saving: each slot as name~state~heat, slots split by | (an empty slot is blank). The rocks are made again by name
    @Override
    public String saveState () {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < SLOTS; i++) {
            if (i > 0) out.append('|');
            if (inside[i] != null) out.append(inside[i].getName()).append('~').append(inside[i].saveState()).append('~').append(inside[i].getSavedHeat());
        }
        return out.toString();
    }

    @Override
    public void loadState (String state) {
        String[] slots = state.split("\\|", -1);
        for (int i = 0; i < SLOTS && i < slots.length; i++) {
            inside[i] = null;
            if (slots[i].isEmpty()) continue;
            String[] parts = slots[i].split("~", -1);
            item made = handler.getItemRegistry().make(parts[0]);
            if (!(made instanceof gemstone) || made instanceof elementalRock) continue;
            if (parts.length > 1) made.loadState(parts[1]);
            if (parts.length > 2) {
                try {
                    made.restoreHeat(Double.parseDouble(parts[2]));
                } catch (NumberFormatException e) {}
            }
            inside[i] = (gemstone) made;
        }
    }
}
