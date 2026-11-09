package game;

//One slot's worth of items: an item and how many of it. Items that can not stack always have a count of 1
public class itemStack {

    private item item;
    private int count;

    public itemStack (item item, int count) {
        if (count < 1) throw new IllegalArgumentException("Count kan ikke være under 1");
        if (count > item.getMaxStack()) throw new IllegalArgumentException("Count kan ikke være over " + item.getMaxStack());
        this.item = item;
        this.count = count;
    }

    public itemStack (item item) {
        this(item, 1);
    }

    public item getItem () {return this.item;}
    public int getCount () {return this.count;}

    public boolean stacksWith (itemStack other) {
        return other != null && item.stacksWith(other.getItem());
    }

    public int getSpace () {return item.getMaxStack() - count;}

    //Moves as much as fits over from the other stack, returns how many were moved
    public int takeFrom (itemStack other) {
        int moved = Math.min(getSpace(), other.getCount());
        count += moved;
        other.count -= moved;
        return moved;
    }

    public void remove (int n) {
        if (n > count) throw new IllegalArgumentException("Kan ikke fjerne flere enn det er");
        count -= n;
    }

    public boolean isEmpty () {return count <= 0;}
}
