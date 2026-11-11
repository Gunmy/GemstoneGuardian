package game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

//Slots that never run out: one per kind of item, and taking from one makes a fresh item.
//Anything put into it is thrown away. Used by the give item cheat with every registered item
public class itemSource implements slotGroup {

    private List<Supplier<item>> makers;
    //One made item per slot to show and to tell about when hovered, never handed out
    private List<itemStack> shown = new ArrayList<itemStack>();

    public itemSource (List<Supplier<item>> makers) {
        this.makers = makers;
        for (Supplier<item> maker : makers) shown.add(new itemStack(maker.get()));
    }

    @Override public int size () {return shown.size();}
    @Override public itemStack get (int i) {return shown.get(i);}
    //The slots always show their item
    @Override public void set (int i, itemStack stack) {}
    @Override public boolean accepts (item item) {return true;}
    //A catalogue: its items never charge, strike or melt each other
    @Override public boolean takesPart () {return false;}

    //Empty hand: left click takes one, right click a full stack. Holding the same item: left adds one, right fills the stack.
    //Holding anything else (or a full stack): it is thrown away
    @Override
    public itemStack click (int i, itemStack cursor, boolean rightClick) {
        item fresh = makers.get(i).get();
        int amount = rightClick ? fresh.getMaxStack() : 1;

        if (cursor == null) return new itemStack(fresh, amount);

        itemStack more = new itemStack(fresh, Math.min(amount, Math.max(1, cursor.getSpace())));
        if (cursor.stacksWith(more) && cursor.getSpace() > 0) {
            cursor.takeFrom(more);
            return cursor;
        }
        return null;
    }
}
