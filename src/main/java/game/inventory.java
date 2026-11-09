package game;

import java.util.ArrayList;
import java.util.List;

//What a character carries. The active part is 4 rock slots and 4 item slots that are used in battle,
//the top part is rows of 8 that grow as they fill up (there is always one free row)
public class inventory {

    public static final int COLUMNS = 8;
    public static final int ACTIVE = 4;
    public static final int MIN_ROWS = 4;

    public enum slotKind {TOP, ROCK, ITEM}

    private List<itemStack> top = new ArrayList<itemStack>();
    private itemStack[] rocks = new itemStack[ACTIVE];
    private itemStack[] items = new itemStack[ACTIVE];

    public inventory () {
        trimRows();
    }

    public int getRows () {return top.size()/COLUMNS;}

    public int size (slotKind kind) {
        return kind == slotKind.TOP ? top.size() : ACTIVE;
    }

    public itemStack get (slotKind kind, int i) {
        switch (kind) {
            case ROCK: return rocks[i];
            case ITEM: return items[i];
            default:   return top.get(i);
        }
    }

    public void set (slotKind kind, int i, itemStack stack) {
        if (stack != null && !accepts(kind, stack.getItem())) throw new IllegalArgumentException(stack.getItem().getName() + " kan ikke ligge i " + kind);
        switch (kind) {
            case ROCK: rocks[i] = stack; break;
            case ITEM: items[i] = stack; break;
            default:   top.set(i, stack); trimRows(); break;
        }
    }

    //Rocks go in the rock slots, everything else in the item slots. The top part takes anything
    public boolean accepts (slotKind kind, item item) {
        switch (kind) {
            case ROCK: return item instanceof gemstone;
            case ITEM: return !(item instanceof gemstone);
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

    public boolean contains (item item) {
        return getItems().contains(item);
    }

    public boolean isEmpty () {
        return getStacks().isEmpty();
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
