package game;

//How strong the computer's fighters play: how a position is judged (the calculated estimate or the trained valueNet)
//and how many actions it looks ahead (depth, see battleSim.values). Every fighter the game plays uses the same one:
//in a campaign the one picked when it was started (saved in the savefile, like a difficulty), in a random 1v1 the
//one set on the 1v1 page (settings). Cheat mode (battle.getHint) judges the player's actions with it too
public final class aiStrength {

    public static final int MIN_DEPTH = 1, MAX_DEPTH = 5;

    //The standard: the trained net, 3 actions ahead (new campaigns, the 1v1 page, and savefiles with no choice saved)
    public static final aiStrength DEFAULT = new aiStrength(true, 3);
    //What a new campaign starts on (the trained net, 2 ahead), and how far ahead one can be set to look at most
    public static final aiStrength CAMPAIGN = new aiStrength(true, 2);
    public static final int CAMPAIGN_MAX_DEPTH = 3;

    //What a strength comes to, shown when a campaign is started: points are the depth, one more for the trained
    //judge (when there is a trained net): 2 or less is Easy, 3 Medium, 4 or more Hard
    public enum difficulty {
        EASY("Easy", "#40C040"), MEDIUM("Medium", "#E09020"), HARD("Hard", "#E04040");
        public final String label, color;
        difficulty (String label, String color) {
            this.label = label;
            this.color = color;
        }
    }

    public difficulty difficulty () {
        int points = depth + (givesChance() ? 1 : 0);
        return points <= 2 ? difficulty.EASY : points == 3 ? difficulty.MEDIUM : difficulty.HARD;
    }

    public final boolean trained;
    public final int depth;
    //A judge given outright (aiTrain plays with nets being trained), else null
    private final battleSim.judge given;

    public aiStrength (boolean trained, int depth) {
        this(trained, depth, null);
    }

    private aiStrength (boolean trained, int depth, battleSim.judge given) {
        this.trained = trained;
        this.depth = Math.max(MIN_DEPTH, Math.min(MAX_DEPTH, depth));
        this.given = given;
    }

    //Playing with this judge, depth actions ahead
    public static aiStrength with (battleSim.judge judge, int depth) {
        return new aiStrength(judge instanceof valueNet, depth, judge);
    }

    //The trained judge, or the calculated one when there is no trained net in the game (valueNet.shared)
    public battleSim.judge judge () {
        if (given != null) return given;
        if (trained && valueNet.shared() != null) return valueNet.shared();
        return battleSim.CALCULATED;
    }

    //True when the judge gives a real chance to win (shown as a percentage), not a calculated score
    public boolean givesChance () {return given != null ? given instanceof valueNet : trained && valueNet.shared() != null;}

    public aiStrength withTrained (boolean trained) {return new aiStrength(trained, depth);}
    public aiStrength withDepth (int depth) {return new aiStrength(trained, depth);}

    public String judgeLabel () {
        if (!trained) return "Calculated";
        return valueNet.shared() != null ? "Trained" : "Trained (not trained yet)";
    }

    //The one the computer plays with in this game: the 1v1's, the campaign's, or the default in the menu
    public static aiStrength of (gameHandler handler) {
        if (handler == null) return DEFAULT;
        //A fight of a run (the daily battles) plays at its stage's strength
        if (handler.getDuel() != null && handler.getDuel().getStrength() != null) return handler.getDuel().getStrength();
        if (handler.getDuel() != null) return handler.getSettings().getDuelAI();
        if (handler.getSave() != null) return load(handler.getSave());
        return DEFAULT;
    }

    //In a savefile
    public static final String JUDGE_KEY = "ai.trained", DEPTH_KEY = "ai.depth";

    public void saveTo (saveFile save) {
        save.set(JUDGE_KEY, trained);
        save.set(DEPTH_KEY, depth);
    }

    public static aiStrength load (saveFile save) {
        return new aiStrength(save.getBool(JUDGE_KEY, DEFAULT.trained), save.getInt(DEPTH_KEY, DEFAULT.depth));
    }
}
