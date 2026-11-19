package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javafx.scene.canvas.GraphicsContext;

//A build (duelBuild) on screen and its editing, shared by the pages that pick builds (the 1v1 page, guiDuelMenu, and
//the daily battles, guiDailyMenu):
//- draw: its rocks in a row of little slots (the equipped one framed), its trinkets half a slot after them if asked.
//  The items are made once per build and slot and kept (clear after the build changes)
//- edit: the real inventory over the menu on the menu game's main character, with every handed out rock and trinket
//  in a catalogue above it (itemSource); the build is what is in the rock and trinket slots when Save is pressed
public class buildSlots {

    private final gui gui;
    private final Map<String, item> shown = new HashMap<String, item>();

    public buildSlots (gui gui) {
        this.gui = gui;
    }

    public void clear () {shown.clear();}

    //A build's item for its little slot: slot 0-3 a rock, -1 and -2 the trinkets
    public item item (duelBuild build, int slot) {
        String key = System.identityHashCode(build) + "/" + slot;
        if (!shown.containsKey(key)) {
            itemRegistry registry = gui.getHandler().getItemRegistry();
            shown.put(key, slot >= 0 ? build.makeRock(slot, registry) : build.makeTrinket(-1 - slot, registry));
        }
        return shown.get(key);
    }

    //How wide draw's row is
    public static double rowWidth (double slot, double gap, boolean trinkets) {
        return duelBuild.ROCKS*(slot + gap) - gap + (trinkets ? duelBuild.TRINKETS*(slot + gap) + slot/2 : 0);
    }

    public void draw (GraphicsContext gc, duelBuild build, double sx, double sy, double slot, double gap, boolean trinkets) {
        guiSkin skin = gui.getSkin();
        int count = duelBuild.ROCKS + (trinkets ? duelBuild.TRINKETS : 0);
        for (int i = 0; i < count; i++) {
            boolean rock = i < duelBuild.ROCKS;
            double x = sx + i*(slot + gap) + (rock ? 0 : slot/2);
            skin.drawSlot(gc, x, sy, slot, rock && equippedSlot(build) == i);
            item in = item(build, rock ? i : -1 - (i - duelBuild.ROCKS));
            if (in != null) {
                in.drawIcon(gc, x + slot*0.1, sy + slot*0.1, slot*0.8);
            } else {
                skin.drawSlotHint(gc, rock ? inventory.slotKind.ROCK : inventory.slotKind.TRINKET, x, sy, slot);
            }
        }
    }

    //The rock slot a build has equipped (the first with a rock for builds saved before that was kept), -1 for none
    public static int equippedSlot (duelBuild build) {
        if (build.getEquipped() != duelBuild.FIRST) return build.getEquipped();
        for (int i = 0; i < duelBuild.ROCKS; i++) if (build.getRock(i) != null) return i;
        return -1;
    }

    //Opens the build in the inventory, shown at level (so what the rocks do is worked out as in the fight). saved runs
    //after the build has been changed to what is in the slots
    public static void edit (gui gui, duelBuild build, int level, Runnable saved) {
        edit(gui, build, level, saved, "Rocks and trinkets", "Save", "Cancel", null);
    }

    //With its own title and button words, and when the save button can be pressed (canSave gets the build as it is in
    //the slots now; null: always). The daily battles open on this with Play
    public static void edit (gui gui, duelBuild build, int level, Runnable saved, String title, String saveLabel, String backLabel,
                             java.util.function.Predicate<duelBuild> canSave) {
        gameHandler handler = gui.getHandler();
        //The main character of the game behind the menu holds the build while it is edited (it is not used for
        //anything else there)
        character holder = handler.getMainCharacter();
        build.giveTo(holder, handler.getItemRegistry());
        holder.setLevel(level);
        holder.clearEffects();
        holder.resetHealth();
        holder.syncShownHealth();
        List<Supplier<item>> choices = new ArrayList<Supplier<item>>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (Supplier<item> maker : handler.getItemRegistry().getMakers()) {
            item made = maker.get();
            if (!seen.add(made.getName())) continue;
            //Only the forms found in chests or dropped in the campaign; the others are made by turning those
            if (!handler.isHandedOut(made.getName())) continue;
            if (made instanceof gemstone || made instanceof trinket) choices.add(maker);
        }
        java.util.function.BooleanSupplier can = canSave == null ? null : () -> canSave.test(inSlots(holder));
        gui.openBuildInventory(new itemSource(choices), title, () -> {
            for (int i = 0; i < duelBuild.ROCKS; i++) {
                itemStack stack = holder.getInventory().get(inventory.slotKind.ROCK, i);
                build.setRock(i, stack != null ? stack.getItem() : null);
            }
            for (int i = 0; i < duelBuild.TRINKETS; i++) {
                itemStack stack = holder.getInventory().get(inventory.slotKind.TRINKET, i);
                build.setTrinket(i, stack != null ? stack.getItem() : null);
            }
            //Which rock slot is equipped (right click a rock to equip it, as in the campaign), or none
            build.setEquipped(duelBuild.NONE);
            for (int i = 0; i < duelBuild.ROCKS; i++) {
                itemStack stack = holder.getInventory().get(inventory.slotKind.ROCK, i);
                if (stack != null && stack.getItem() == holder.getGemstone()) build.setEquipped(i);
            }
            saved.run();
        }, saveLabel, backLabel, can);
    }

    //What is in the holder's rock and trinket slots now, as a build (only the names: for canSave)
    private static duelBuild inSlots (character holder) {
        duelBuild now = new duelBuild();
        for (int i = 0; i < duelBuild.ROCKS; i++) {
            itemStack stack = holder.getInventory().get(inventory.slotKind.ROCK, i);
            if (stack != null) now.setRock(i, stack.getItem().getName());
        }
        return now;
    }
}
