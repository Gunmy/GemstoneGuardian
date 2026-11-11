package game;

import java.util.function.Consumer;

//A mirror: every mirror is a way into the mirror dimension (world 5) or back out of it. The crystal boy, the player's
//reflection, comes and goes through them.
//The one in the girl's room (tile 194, gameHandler.kidnapping) changes with the story, set by gameHandler every frame:
//hidden before she is beaten, then it appears (shut: solid), opens (it looks the same, but they walk into it) and is
//gone once they have (blown apart: only the blue brick over lava it hung on is left). Hidden and gone draw nothing and
//can be walked on.
//The others (made with alwaysOpen) are always there and open, and walking into one takes you through it (its
//pressure plate): the one behind the crystal boy in his hall (195) and the one back out in the mirror dimension (196)
public class mirrorType extends waveType {

    public enum state {HIDDEN, MIRROR, OPEN, GONE}
    private state now = state.HIDDEN;

    public void setState (state now) {this.now = now;}
    public state getState () {return this.now;}

    public mirrorType (String name, int x, int y, int id, tileType underlay) {
        this(name, x, y, id, underlay, null);
    }

    public mirrorType (String name, int x, int y, int id, tileType underlay, Consumer<character> through) {
        super(name, x, y, 100, 100, id, true, through, underlay, false, 1, 1, 0);
    }

    //Always there and open
    public mirrorType alwaysOpen () {
        this.now = state.OPEN;
        return this;
    }

    @Override
    public boolean isBlocked (double xChange, double yChange) {
        return now == state.MIRROR;
    }

    @Override
    protected void drawSpecial (gameHandler handler, double realx, double realy, int x, int y) {
        if (now == state.MIRROR || now == state.OPEN) super.drawSpecial(handler, realx, realy, x, y);
    }
}
