package game;

//A fixed number of slots that hold anything, like a chest. Shown above the inventory with gui.openContainer
public class itemContainer implements slotGroup {

    private itemStack[] slots;

    public itemContainer (int size) {
        if (size < 1) throw new IllegalArgumentException("En container må ha minst én plass");
        slots = new itemStack[size];
    }

    @Override public int size () {return slots.length;}
    @Override public itemStack get (int i) {return slots[i];}
    @Override public void set (int i, itemStack stack) {slots[i] = stack;}
    @Override public boolean accepts (item item) {return true;}

    //Puts an item onto a stack of the same with room, or in the first free slot. False when it is full
    public boolean give (item item) {
        itemStack given = new itemStack(item);
        for (itemStack stack : slots) {
            if (stack != null && stack.stacksWith(given) && stack.getSpace() > 0) {
                stack.takeFrom(given);
                return true;
            }
        }
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = given;
                return true;
            }
        }
        return false;
    }
}
