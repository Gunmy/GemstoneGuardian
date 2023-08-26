package game;

import java.util.List;

public class movementEvent implements event {
    private List<Integer> moveList;
    //0 left, 1 up, 2 right, 3 bottom
    private character targetCharacter;
    private boolean bindCam;
    private double bindTime;
    private gameHandler handler;

    public movementEvent (gameHandler handler, character targetCharacter, List<Integer> moveList, boolean bindCam) {
        this.targetCharacter = targetCharacter;
        this.moveList = moveList;
        this.bindCam = bindCam;
        this.handler = handler;
        for (Integer move : moveList) {
            if (move > 3 || move < 0) throw new IllegalArgumentException("Move kan ikke være større enn 3 eller mindre enn 0");
        }
    }

    public movementEvent (gameHandler handler, character targetCharacter, List<Integer> moveList, boolean bindCam, double bindTime) {
        this(handler, targetCharacter, moveList, bindCam);
        this.bindTime = bindTime;
    }

    public void trigger() {
        for (Integer move : moveList) {
            switch (move) {
                case 0:
                    targetCharacter.addMovementToQueue(-1, 0);
                    break;
                case 1:
                    targetCharacter.addMovementToQueue(0, -1);
                    break;
                case 2:
                    targetCharacter.addMovementToQueue(1, 0);
                    break;
                case 3:
                    targetCharacter.addMovementToQueue(0, 1);
                    break;
            }
        }

        if (bindCam) {
            handler.getCamera().bindTo(targetCharacter);
            handler.getCamera().setBindTime(bindTime);
        }
    }


}
