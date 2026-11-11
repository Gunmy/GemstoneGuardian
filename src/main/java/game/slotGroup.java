package game;

//A run of item slots that a slotGrid can show and the player can click items in and out of.
//The player's inventory is three of these (see inventory.group), a chest is one (itemContainer),
//and the give item cheat is one that never runs out (itemSource)
public interface slotGroup {

    int size ();

    itemStack get (int i);

    void set (int i, itemStack stack);

    boolean accepts (item item);

    //Which empty-slot picture to show (rock and item slots have one, the rest are blank)
    default inventory.slotKind hint () {return inventory.slotKind.TOP;}

    //What a click on slot i does with what is on the cursor. Returns what is on the cursor after.
    //The standard is: pick up, put down, put onto a stack of the same, or swap. Right clicks do nothing
    default itemStack click (int i, itemStack cursor, boolean rightClick) {
        if (rightClick) return cursor;
        itemStack inSlot = get(i);

        if (cursor == null) {
            if (inSlot != null) set(i, null);
            return inSlot;
        }
        if (!accepts(cursor.getItem())) return cursor;
        if (inSlot == null) {
            set(i, cursor);
            return null;
        }
        if (inSlot.stacksWith(cursor)) {
            inSlot.takeFrom(cursor);
            return cursor.isEmpty() ? null : cursor;
        }
        set(i, cursor);
        return inSlot;
    }

    //Whether the items in these slots take part in the items' interactions (see itemInteractions): charging each
    //other, being struck, turning into other items. A catalogue like the give item menu stays out of it
    default boolean takesPart () {return true;}

    //Run when the screen showing these slots is closed (a chest uses it for what happens after it is first looked in)
    default void closed () {}
}
