package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//A cutscene: steps played one after another, each waiting for the one before to be done: lines said (done when the
//player has read them), a character walking (done when it stands still), a wait, or something done at once (run).
//Built with the methods below and played with gameHandler.play(cutscene). While one plays the player can not walk,
//and it waits while a fight is still on screen.
//A savefile never holds half a cutscene: autosave waits for it to end, and saving in the middle of one (quitting)
//finishes it at once with its ending (ending(...)), which puts everything where it is after the last step
public class cutscene {

    private interface step {
        void start ();
        boolean done ();
    }

    private final gameHandler handler;
    private final List<step> steps = new ArrayList<step>();
    private int current = -1;
    private long stepStart;
    private Runnable ending;

    public cutscene (gameHandler handler) {
        this.handler = handler;
    }

    private double stepSeconds () {return (System.nanoTime() - stepStart)/1e9;}

    //Lines said, one after another; the step is done when the last has been read
    public cutscene say (dialogue... lines) {
        steps.add(new step() {
            public void start () {for (dialogue d : lines) handler.getDiealoguecontainer().add(d);}
            public boolean done () {
                dialogueContainer d = handler.getDiealoguecontainer();
                return !d.isActive() && !d.hasNext();
            }
        });
        return this;
    }

    //Something done at once (place a character, sparks, ...)
    public cutscene run (Runnable action) {
        steps.add(new step() {
            public void start () {action.run();}
            public boolean done () {return true;}
        });
        return this;
    }

    //A character walks (movementEvent directions: 0 left, 1 up, 2 right, 3 down). Done when it stands still, or when
    //it should long have been there (someone standing in the way)
    public cutscene walk (character c, Integer... moves) {
        steps.add(new step() {
            public void start () {new movementEvent(handler, c, Arrays.asList(moves), false).trigger();}
            public boolean done () {return c.isStill() || stepSeconds() > 1 + moves.length;}
        });
        return this;
    }

    //Like walk, but through anything in the way (walls, lava, people): out of a mirror on a wall and back into it
    public cutscene walkThrough (character c, Integer... moves) {
        steps.add(new step() {
            public void start () {
                c.setGhost(true);
                new movementEvent(handler, c, Arrays.asList(moves), false).trigger();
            }
            public boolean done () {
                boolean done = c.isStill() || stepSeconds() > 1 + moves.length;
                if (done) c.setGhost(false);
                return done;
            }
        });
        return this;
    }

    //The camera slides over to look at something (a character, a spot) until lookBack, or the end of the cutscene
    public cutscene look (cordinate at) {
        return run(() -> handler.setCameraFocus(at));
    }

    public cutscene lookBack () {
        return run(() -> handler.setCameraFocus(null));
    }

    //Lines said, with something done every frame until the last of them has been read (a storm going on while they talk)
    public cutscene sayWhile (Runnable everyFrame, dialogue... lines) {
        steps.add(new step() {
            public void start () {
                for (dialogue d : lines) handler.getDiealoguecontainer().add(d);
                everyFrame.run();
            }
            public boolean done () {
                dialogueContainer d = handler.getDiealoguecontainer();
                if (!d.isActive() && !d.hasNext()) return true;
                everyFrame.run();
                return false;
            }
        });
        return this;
    }

    //Something done every frame for a while (a storm of sparks)
    public cutscene during (double seconds, Runnable everyFrame) {
        steps.add(new step() {
            public void start () {everyFrame.run();}
            public boolean done () {
                if (stepSeconds() >= seconds) return true;
                everyFrame.run();
                return false;
            }
        });
        return this;
    }

    //Walks through anything to (x, y), wherever it is when the step starts: across first, then up or down
    public cutscene walkThroughTo (character c, double x, double y) {
        steps.add(new step() {
            int moves;
            public void start () {
                int tC = handler.getTileCount();
                long dx = Math.round(x*tC) - Math.round(c.getX()*tC), dy = Math.round(y*tC) - Math.round(c.getY()*tC);
                java.util.List<Integer> path = new java.util.ArrayList<Integer>();
                for (long i = 0; i < Math.abs(dx); i++) path.add(dx < 0 ? 0 : 2);
                for (long i = 0; i < Math.abs(dy); i++) path.add(dy < 0 ? 1 : 3);
                moves = path.size();
                c.setGhost(true);
                new movementEvent(handler, c, path, false).trigger();
            }
            public boolean done () {
                boolean done = c.isStill() || stepSeconds() > 1 + moves;
                if (done) c.setGhost(false);
                return done;
            }
        });
        return this;
    }

    //Waits until something is true (a teleport's black has opened again)
    public cutscene until (java.util.function.BooleanSupplier ready) {
        steps.add(new step() {
            public void start () {}
            public boolean done () {return ready.getAsBoolean();}
        });
        return this;
    }

    public cutscene wait (double seconds) {
        steps.add(new step() {
            public void start () {}
            public boolean done () {return stepSeconds() >= seconds;}
        });
        return this;
    }

    //Puts everything where it is after the last step; run when the cutscene is cut short by saving
    public cutscene ending (Runnable ending) {
        this.ending = ending;
        return this;
    }

    public boolean isOver () {return current >= steps.size();}

    //Over: the camera goes back to the player, whatever the steps left it looking at
    private void over () {handler.setCameraFocus(null);}

    //Starts the next steps whose turn it is. Called every frame by gameHandler
    public void next () {
        if (handler.getBattle().isActive()) return;
        while (!isOver() && (current < 0 || steps.get(current).done())) {
            current++;
            stepStart = System.nanoTime();
            if (!isOver()) steps.get(current).start();
            else over();
        }
    }

    //Cut short: straight to how it ends
    public void finishNow () {
        if (isOver()) return;
        current = steps.size();
        if (ending != null) ending.run();
        over();
    }
}
