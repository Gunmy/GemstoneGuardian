package game;

import java.util.List;
import java.util.concurrent.CompletableFuture;

//The turn based fight. Holds the state only, the GUI (guiBattle) draws it and calls back in here
public class battle {

    private boolean activeBattle = false;

    private character you, them;

    private event youWin, theyWin;

    private boolean opponentDoneMove = true;

    //True while the player is choosing what to do
    private boolean waitingForPlayer = false;
    //True while the rocks and items are shown instead of the abilities
    private boolean moreView = false;

    //Health bars that slide towards the health the messages have shown so far
    private double yourBar, theirBar;

    //To save the event in case player lose
    private double x, y, r;
    private int world;

    public battle () {
        resetBattle();
    }

    //A 1v1 against another player (see duel), null in the campaign. The host plays the other player's moves as the
    //opponent's turn; the other player's fight only shows what the host sends and sends its moves
    private duel duel;
    public void setDuel (duel duel) {this.duel = duel;}
    private boolean mirror () {return duel != null && !duel.isHost();}

    //A 1v1 waiting for the other player to pick their move (its messages read): guiBattle shows the waiting sign
    public boolean waitingForOther (gameHandler handler) {
        if (duel == null || duel.isAgainstComputer() || !activeBattle || waitingForPlayer) return false;
        if (handler.getDiealoguecontainer().isActive() || handler.getDiealoguecontainer().hasNext()) return false;
        return duel.isHost() ? !opponentDoneMove : !duel.isMyTurn();
    }
    public duel getDuel () {return this.duel;}

    //The other player left: the fight stops where it is
    public void endDuel () {resetBattle();}

    public character getTurnHaver () {
        if (opponentDoneMove) return you;
        return them;
    }

    public character getNotTurnHaver () {
        if (!opponentDoneMove) return you;
        return them;
    }

    public character getYou () {return this.you;}
    public character getThem () {return this.them;}
    public double getYourBar () {return this.yourBar;}
    public double getTheirBar () {return this.theirBar;}

    public boolean isActive () {return this.activeBattle;}
    public boolean isWaitingForPlayer () {return this.waitingForPlayer;}
    public boolean inMoreView () {return this.moreView;}

    //What the computer will do on its turn: battleAI thinks on a thread of its own, and the fight waits for it
    //(opponentMove) while the game keeps drawing
    private CompletableFuture<battleSim.action> aiPlan;

    //Cheat mode: how good each of the player's actions is this turn (getHints), worked out on a thread. cheated: it
    //was on in this fight, so a random battle won does not count for the streak
    private CompletableFuture<battleAI.hints> hints;
    private boolean cheated = false;

    //Cheat mode's scores for this turn: null while they are worked out, when it is off, outside the player's turn,
    //and always in a 1v1 against another person
    public battleAI.hints getHints (gameHandler handler) {
        if (!activeBattle || !waitingForPlayer || you == null || !handler.getSettings().cheatMode()) return null;
        if (duel != null && !duel.isAgainstComputer()) return null;
        //Never in a run (the daily battles): they are graded
        if (graded()) return null;
        cheated = true;
        if (hints == null) hints = battleAI.hintsFor(you, them, aiStrength.of(handler));
        if (!hints.isDone()) return null;
        if (hints.isCompletedExceptionally()) {
            //Said once, then left off for this turn
            try {
                hints.join();
            } catch (RuntimeException e) {
                System.out.println("Juksemodus feilet: " + e);
            }
            hints = CompletableFuture.completedFuture(null);
            return null;
        }
        return hints.join();
    }

    //---- Grading the player's actions (in a run, fightRun) ----

    //Each of the player's turns: every action scored by the standard judge (the trained one, 3 ahead), worked out on a
    //thread from the start of the turn, never shown. The action taken is put with the scores (taken) and graded once
    //they are done (gradeTaken, every frame), by how much chance it gave up against the best one
    private CompletableFuture<battleAI.hints> turnScores;
    private static class taken {
        final CompletableFuture<battleAI.hints> scores;
        final int kind, index;
        taken (CompletableFuture<battleAI.hints> scores, int kind, int index) {
            this.scores = scores;
            this.kind = kind;
            this.index = index;
        }
    }
    private final java.util.List<taken> toGrade = new java.util.ArrayList<taken>();
    //How long the end of a fight waits for the last scores before they are left ungraded
    private static final long LAST_GRADE_WAIT_MS = 2000;

    private boolean graded () {return duel != null && duel.getRun() != null;}

    private void scoreTurn () {
        if (graded() && you != null && them != null) turnScores = battleAI.hintsFor(you, them, aiStrength.DEFAULT);
    }

    //The action about to be done, worked out before it runs (the rocks and items move about after)
    private void take (int kind, int index) {
        if (turnScores != null && index >= 0) toGrade.add(new taken(turnScores, kind, index));
        turnScores = null;
    }

    private void takeItem (item used) {
        int k = battleSim.rockOrder(you).indexOf(used);
        if (k >= 0) {take(battleSim.SWITCH, k); return;}
        k = battleSim.itemOrder(you).indexOf(used);
        take(battleSim.ITEM, k);
    }

    //Grades the taken actions whose scores are done (all of them at the end of the fight, waiting a little)
    private void gradeTaken (boolean last) {
        if (!graded()) {toGrade.clear(); return;}
        java.util.Iterator<taken> it = toGrade.iterator();
        while (it.hasNext()) {
            taken t = it.next();
            if (!t.scores.isDone() && last) {
                try {
                    t.scores.get(LAST_GRADE_WAIT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    //Left ungraded
                }
            }
            if (!t.scores.isDone() && !last) continue;
            it.remove();
            if (!t.scores.isDone() || t.scores.isCompletedExceptionally()) continue;
            battleAI.hints h = t.scores.join();
            double mine = h.of(t.kind, t.index);
            if (Double.isNaN(mine) || h.values.length < 2) continue;
            double best = Double.NEGATIVE_INFINITY, worst = Double.POSITIVE_INFINITY;
            for (double v : h.values) {
                best = Math.max(best, v);
                worst = Math.min(worst, v);
            }
            if (best - worst < fightRun.UNGRADED_SPREAD) continue;
            duel.getRun().graded(best - mine);
        }
    }

    //The fight in numbers, for the win and lose screens (fightOutcome)
    private fightOutcome.stats numbers = new fightOutcome.stats();

    //Runs one side's action and counts the damage it did to the other (a hit, for the best hit)
    private void yourAction (Runnable action) {
        double before = them.getHealth();
        action.run();
        numbers.yourHit(before - them.getHealth(), true);
        numbers.turns++;
    }

    private void theirAction (Runnable action) {
        double before = you.getHealth();
        action.run();
        numbers.theirHit(before - you.getHealth(), true);
    }

    public void newBattle (character you, character them, event youWin, event theyWin, double x, double y, double r, int world) {
        if (you == them) throw new IllegalArgumentException("Kan ikke slåss mot seg selv");
        activeBattle = true;
        numbers = new fightOutcome.stats();
        aiPlan = null;
        hints = null;
        turnScores = null;
        toGrade.clear();
        cheated = false;
        this.you = you;
        this.them = them;
        this.youWin = youWin;
        this.theyWin = theyWin;
        this.x = x;
        this.y = y;
        this.r = r;
        this.world = world;
        you.syncShownHealth();
        them.syncShownHealth();
        this.yourBar = you.getShownHealthPercentage();
        this.theirBar = them.getShownHealthPercentage();
        //The two fighters rise up giant first; nobody moves until they are gone (carryOn)
        battleIntro.forBattle(you, them);
    }

    //Whose turn it is, for savefiles: true when the player moves next
    public boolean isPlayersTurn () {return this.opponentDoneMove;}

    //Continues a saved fight (started again with its battleEvent) on the turn it was saved on
    public void resumeTurn (boolean playersTurn) {
        this.opponentDoneMove = playersTurn;
        this.waitingForPlayer = false;
        this.moreView = false;
        this.aiPlan = null;
        this.hints = null;
    }

    public void showMore () {moreView = true;}
    public void showActions () {moreView = false;}

    private void endTurn () {
        hints = null;
        waitingForPlayer = false;
        moreView = false;
        opponentDoneMove = false;
    }

    //n is 0, 1 or 2
    public void useAbility (int n) {
        if (!waitingForPlayer) return;
        gemstone stone = you.getGemstone();
        if (stone == null) return;

        if (mirror()) {
            boolean left = stone.usesLeft(n, you);
            if (left) {duel.sendAction("ability", n); endTurn();}
            return;
        }

        dialogueContainer messages = you.getHandler().getDiealoguecontainer();
        if (n >= 0 && n < 3 && stone.usesLeft(n, you)) {take(battleSim.MOVE, n); yourAction(() -> messages.grouped(() -> stone.useMove(n, you, them))); endTurn();}
    }

    //Using a rock equips it, using anything else does what the item does. Both take the turn
    public void useItem (item item) {
        if (!waitingForPlayer) return;
        if (item instanceof gemstone && you.getGemstone() == item) return;
        if (mirror()) {
            //Only rocks are in a 1v1 build: using one equips it
            int slot = game.duel.rockSlot(you, item);
            if (slot >= 0) {duel.sendAction("rock", slot); endTurn();}
            return;
        }

        takeItem(item);
        yourAction(() -> you.getHandler().getDiealoguecontainer().grouped(() -> {
            item.use(you, them);
            if (item.isConsumable()) you.consume(item);
        }));
        endTurn();
    }

    public void giveUp () {
        if (!waitingForPlayer) return;
        if (mirror()) {
            duel.sendAction("giveup", 0);
            endTurn();
            return;
        }
        //Giving up is not graded
        turnScores = null;
        you.kill();
        endTurn();
    }

    //False while waiting for the other player in a 1v1
    private boolean opponentMove () {
        //Against the computer (or in the campaign) battleAI plays the opponent's turn
        if (duel != null && !duel.isAgainstComputer()) {
            String[] action = duel.takeAction();
            if (action == null) {
                duel.waitingForThem();
                return false;
            }
            theirAction(() -> playTheirs(action));
            duel.theirMoveDone();
            opponentDoneMove = true;
            return true;
        }
        if (aiPlan == null) aiPlan = battleAI.plan(them, you, aiStrength.of(them.getHandler()));
        if (!aiPlan.isDone()) return false;
        battleSim.action chosen;
        try {
            chosen = aiPlan.join();
        } catch (RuntimeException e) {
            System.out.println("battleAI feilet: " + e);
            chosen = new battleSim.action(battleSim.PUNCH, 0);
        }
        aiPlan = null;
        battleSim.action doing = chosen;
        theirAction(() -> them.getHandler().getDiealoguecontainer().grouped(() -> battleAI.perform(doing, them, you)));
        opponentDoneMove = true;
        return true;
    }

    //The other player's move in a 1v1 (ACT kind n, see duel), played for them like the player's own
    private void playTheirs (String[] action) {
        String kind = action[1];
        int n;
        try {
            n = Integer.parseInt(action[2]);
        } catch (NumberFormatException e) {
            return;
        }
        dialogueContainer messages = them.getHandler().getDiealoguecontainer();
        gemstone stone = them.getGemstone();
        if (kind.equals("ability") && stone != null) {
            if (n >= 0 && n < 3 && stone.usesLeft(n, them)) messages.grouped(() -> stone.useMove(n, them, you));
        } else if (kind.equals("rock") && n >= 0 && n < inventory.ACTIVE) {
            itemStack stack = them.getInventory().get(inventory.slotKind.ROCK, n);
            if (stack != null && stack.getItem() != stone) messages.grouped(() -> stack.getItem().use(them, you));
        } else if (kind.equals("giveup")) {
            them.kill();
        }
    }

    private void resetBattle () {
        activeBattle = false;
        waitingForPlayer = false;
        moreView = false;
        this.you = null;
        this.them = null;
        this.youWin = null;
        this.theyWin = null;
        this.x = 0;
        this.y = 0;
        this.r = 0;
        this.world = 0;
        opponentDoneMove = true;
        aiPlan = null;
        hints = null;
    }

    //The message with the hit that ended the fight is played in slow motion
    private void finalBlow (gameHandler handler) {
        dialogue last = handler.getDiealoguecontainer().last();
        if (last != null) last.slowMotion();
    }

    private boolean checkForDead (gameHandler handler) {
        if (duel != null && (you.getHealth() <= 0.5 || them.getHealth() <= 0.5)) {
            //A 1v1 ends with the winner's screen, and both go back to the menu once it is read (no xp, drops or
            //second try). The guest is sent it too (duel.forward)
            finalBlow(handler);
            fightOutcome outcome = fightOutcome.duel(you, them, them.getHealth() <= 0.5, numbers);
            if (graded()) {
                //A fight of a run: counted, then on to the next one or to how it went
                gradeTaken(true);
                duel.runFightOver(outcome);
            } else if (duel.isAgainstComputer()) {
                duel.countStreak(outcome, cheated);
                duel.offerRematch(outcome);
            }
            handler.getDiealoguecontainer().add(outcome);
            duel.finish();
            resetBattle();
            return true;
        }
        if (you.getHealth() <= 0.5 || them.getHealth() <= 0.5) {

            finalBlow(handler);
            //What the fight sets off waits for its screen to be gone (fightOutcome.afterwards)
            character you = this.you, them = this.them;
            event youWin = this.youWin, theyWin = this.theyWin;
            double x = this.x, y = this.y, r = this.r;
            int world = this.world;
            if (you.getHealth() <= 0.5) {
                handler.getDiealoguecontainer().add(fightOutcome.lose(you, them, numbers).afterwards(() -> {
                    if (theyWin != null) {
                        theyWin.trigger();
                    }
                    you.restore();
                    handler.getDiealoguecontainer().add(new dialogue("Game", "Restoring winner..."));
                    them.restore();

                    handler.addEventListener(new battleEvent(you, them, this, youWin, theyWin, x, y, r, world).rearm(handler));
                }));
            } else {
                //The xp and the drops are given now and shown on the win screen, before what the win sets off
                double xpBefore = you.getXp(), xp = Math.pow(them.getLevel()+1, 2);
                you.addXp(xp);
                List<item> dropped = them.rollDrops();
                for (item drop : dropped) you.give(drop);
                handler.getDiealoguecontainer().add(fightOutcome.win(you, them, numbers, xpBefore, xp, dropped).afterwards(() -> {
                    you.announceMilestones(character.levelOf(xpBefore), you.getLevel());
                    if (youWin != null) {
                        youWin.trigger();
                    }
                }));

            }

            resetBattle();
            return true;
        }
        return false;
    }

    public void carryOn (gameHandler handler) {
        gradeTaken(false);
        //The fight waits for its intro (battleIntro) to be over
        if (battleIntro.playing()) return;

        if (mirror()) {
            //The other player's fight: the panel comes up when the host says it is our turn and its messages are read
            if (!handler.getDiealoguecontainer().isActive() && !handler.getDiealoguecontainer().hasNext() && duel.isMyTurn()) {
                waitingForPlayer = true;
            }
            if (you != null) {
                yourBar += (you.getShownHealthPercentage()-yourBar)*0.1;
                theirBar += (them.getShownHealthPercentage()-theirBar)*0.1;
            }
            return;
        }

        if (!checkForDead(handler) && !handler.getDiealoguecontainer().isActive() && !waitingForPlayer) {
            if (opponentDoneMove) {
                waitingForPlayer = true;
                scoreTurn();
            } else {
                if (opponentMove() && !checkForDead(handler)) {
                    //Each one's burning, poison and healing with the damage it does
                    //Burning and poison count as damage done by the other side, but not as a hit
                    double yourBefore = you.getHealth(), theirBefore = them.getHealth();
                    handler.getDiealoguecontainer().grouped(() -> you.updateEffects());
                    handler.getDiealoguecontainer().grouped(() -> them.updateEffects());
                    numbers.theirHit(yourBefore - you.getHealth(), false);
                    numbers.yourHit(theirBefore - them.getHealth(), false);
                }
            }
        }

        if (you != null) {
            yourBar += (you.getShownHealthPercentage()-yourBar)*0.1;
            theirBar += (them.getShownHealthPercentage()-theirBar)*0.1;
        }
    }

}
