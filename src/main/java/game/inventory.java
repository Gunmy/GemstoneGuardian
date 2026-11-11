package game;

import java.util.ArrayList;
import java.util.List;

//What a character carries. The active part is 4 rock slots and 4 item slots that are used in battle,
//the top part is rows of 8 that grow as they fill up (there is always one free row). The TRINKETS trinket slots hold
//what the character wears (see trinket)
public class inventory {

    public static final int COLUMNS = 8;
    public static final int ACTIVE = 4;
    public static final int MIN_ROWS = 4;
    public static final int TRINKETS = 2;

    public enum slotKind {TOP, ROCK, ITEM, TRINKET}

    private List<itemStack> top = new ArrayList<itemStack>();
    private itemStack[] rocks = new itemStack[ACTIVE];
    private itemStack[] items = new itemStack[ACTIVE];
    private itemStack[] trinkets = new itemStack[TRINKETS];

    public inventory () {
        trimRows();
    }

    public int getRows () {return top.size()/COLUMNS;}

    public int size (slotKind kind) {
        switch (kind) {
            case TOP:     return top.size();
            case TRINKET: return TRINKETS;
            default:      return ACTIVE;
        }
    }

    //One kind of slots as a slotGroup, so it can be shown and clicked like any other slots
    public slotGroup group (slotKind kind) {
        inventory inv = this;
        return new slotGroup() {
            @Override public int size () {return inv.size(kind);}
            @Override public itemStack get (int i) {return inv.get(kind, i);}
            @Override public void set (int i, itemStack stack) {inv.set(kind, i, stack);}
            @Override public boolean accepts (item item) {return inv.accepts(kind, item);}
            @Override public slotKind hint () {return kind;}
        };
    }

    public itemStack get (slotKind kind, int i) {
        switch (kind) {
            case ROCK: return rocks[i];
            case ITEM: return items[i];
            case TRINKET: return trinkets[i];
            default:   return top.get(i);
        }
    }

    //What is worn in the trinket slots, as it is now (an item that has changed counts as what it became)
    public List<trinket> getWorn () {
        List<trinket> worn = new ArrayList<trinket>();
        for (itemStack stack : trinkets) {
            if (stack != null && stack.getItem().latest() instanceof trinket) worn.add((trinket) stack.getItem().latest());
        }
        return worn;
    }

    public void set (slotKind kind, int i, itemStack stack) {
        if (stack != null && !accepts(kind, stack.getItem())) throw new IllegalArgumentException(stack.getItem().getName() + " kan ikke ligge i " + kind);
        switch (kind) {
            case ROCK: rocks[i] = stack; break;
            case ITEM: items[i] = stack; break;
            case TRINKET: trinkets[i] = stack; break;
            default:   top.set(i, stack); trimRows(); break;
        }
    }

    //Rocks go in the rock slots, everything else in the item slots. The top part takes anything
    public boolean accepts (slotKind kind, item item) {
        switch (kind) {
            case ROCK: return item instanceof gemstone;
            case ITEM: return !(item instanceof gemstone) && item.isBattleItem();
            case TRINKET: return item instanceof trinket;
            default:   return true;
        }
    }

    //Never less than MIN_ROWS rows. More rows are only added when every slot is taken,
    //and empty rows at the end beyond the minimum are removed again
    private void trimRows () {
        int lastUsed = -1;
        int used = 0;
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i) != null) {
                lastUsed = i;
                used++;
            }
        }
        int rows = Math.max(MIN_ROWS, lastUsed/COLUMNS + 1);
        if (used == rows*COLUMNS) rows++;

        while (top.size() > rows*COLUMNS) top.remove(top.size()-1);
        while (top.size() < rows*COLUMNS) top.add(null);
    }

    //Puts an item where it belongs: onto a stack of the same item if there is room,
    //otherwise in the first free slot of its kind in the active part, otherwise in the top part
    public void give (item item) {
        itemStack given = new itemStack(item);

        if (item.getMaxStack() > 1) {
            for (slotKind kind : new slotKind[] {slotKind.ITEM, slotKind.TOP}) {
                if (!accepts(kind, item)) continue;
                for (int i = 0; i < size(kind); i++) {
                    itemStack stack = get(kind, i);
                    if (stack != null && stack.stacksWith(given) && stack.getSpace() > 0) {
                        stack.takeFrom(given);
                        return;
                    }
                }
            }
        }

        slotKind active = item instanceof gemstone ? slotKind.ROCK : slotKind.ITEM;
        for (slotKind kind : new slotKind[] {active, slotKind.TOP}) {
            if (!accepts(kind, item)) continue;
            for (int i = 0; i < size(kind); i++) {
                if (get(kind, i) == null) {
                    set(kind, i, given);
                    return;
                }
            }
        }
        //The top part always has a free row, so this is never reached
        throw new IllegalStateException("Ingen plass i inventory");
    }

    public List<itemStack> getStacks () {
        List<itemStack> all = new ArrayList<itemStack>();
        for (slotKind kind : slotKind.values()) {
            for (int i = 0; i < size(kind); i++) {
                if (get(kind, i) != null) all.add(get(kind, i));
            }
        }
        return all;
    }

    //One item per stack, for code that only cares about what is carried
    public List<item> getItems () {
        List<item> all = new ArrayList<item>();
        for (itemStack stack : getStacks()) all.add(stack.getItem());
        return all;
    }

    public boolean inRockSlot (item item) {
        for (itemStack stack : rocks) {
            if (stack != null && stack.getItem() == item) return true;
        }
        return false;
    }

    //Whether a rock slot holds this item, or an earlier form of it that has turned into it but not been swapped yet
    public boolean inRockSlotAsLatest (item item) {
        for (itemStack stack : rocks) {
            if (stack != null && (stack.getItem() == item || stack.getItem().latest() == item)) return true;
        }
        return false;
    }

    public boolean contains (item item) {
        return getItems().contains(item);
    }

    public boolean isEmpty () {
        return getStacks().isEmpty();
    }

    //Every slot as prefix.KIND.i.item / .count / .state, and how many top slots there are
    public void saveTo (saveFile save, String prefix) {
        for (slotKind kind : slotKind.values()) {
            save.set(prefix + kind + ".size", size(kind));
            for (int i = 0; i < size(kind); i++) {
                itemStack stack = get(kind, i);
                if (stack == null) continue;
                String key = prefix + kind + "." + i;
                save.set(key + ".item", stack.getItem().getName());
                save.set(key + ".count", stack.getCount());
                save.set(key + ".state", stack.getItem().saveState());
                save.set(key + ".heat", stack.getItem().getSavedHeat());
            }
        }
    }

    //Replaces everything carried with what the save holds. Items the game no longer has are left out
    public void loadFrom (saveFile save, String prefix, itemRegistry registry) {
        top.clear();
        int topSize = Math.max(0, save.getInt(prefix + slotKind.TOP + ".size", 0));
        for (int i = 0; i < topSize; i++) top.add(null);
        for (int i = 0; i < ACTIVE; i++) {
            rocks[i] = null;
            items[i] = null;
        }
        for (int i = 0; i < TRINKETS; i++) trinkets[i] = null;
        for (slotKind kind : slotKind.values()) {
            for (int i = 0; i < size(kind); i++) {
                String key = prefix + kind + "." + i;
                if (!save.has(key + ".item")) continue;
                item made = registry.make(save.getString(key + ".item", ""));
                if (made == null) {
                    System.out.println("Fant ikke " + save.getString(key + ".item", "") + " fra lagringen");
                    continue;
                }
                if (!accepts(kind, made)) {
                    System.out.println(made.getName() + " kan ikke ligge i " + kind + ", hoppet over");
                    continue;
                }
                made.loadState(save.getString(key + ".state", ""));
                made.restoreHeat(save.getDouble(key + ".heat", -1));
                int count = Math.max(1, Math.min(made.getMaxStack(), save.getInt(key + ".count", 1)));
                itemStack stack = new itemStack(made, count);
                switch (kind) {
                    case ROCK: rocks[i] = stack; break;
                    case ITEM: items[i] = stack; break;
                    case TRINKET: trinkets[i] = stack; break;
                    default:   top.set(i, stack); break;
                }
            }
        }
        trimRows();
    }

    //Uses up one of the item, removing the stack when it runs out
    public void consume (item item) {
        for (slotKind kind : slotKind.values()) {
            for (int i = 0; i < size(kind); i++) {
                itemStack stack = get(kind, i);
                if (stack != null && stack.getItem() == item) {
                    stack.remove(1);
                    if (stack.isEmpty()) set(kind, i, null);
                    return;
                }
            }
        }
    }
}
