package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

public class gemstone extends item {
    private BiConsumer<character, character> ability1, ability2, ability3;
    private String ability1name, ability2name, ability3name;
    private abilityType ability1type, ability2type, ability3type;
    //What each move does, also shown in its info box (moveEffects)
    private moveEffects ability1effects, ability2effects, ability3effects;
    private int ability1usages, ability2usages, ability3usages;
    private int ability1uses, ability2uses, ability3uses;
    private double ability1chance, ability2chance, ability3chance;
    private Random rdm = new Random();

    //What each move looks like in battle (null: the default for its type, battleAnimation.forType). Set in gemstones
    private battleAnimation[] animations = new battleAnimation[3];
    public gemstone animations (battleAnimation move1, battleAnimation move2, battleAnimation move3) {
        animations[0] = move1;
        animations[1] = move2;
        animations[2] = move3;
        return this;
    }

    //What move n (0, 1 or 2) does (battleAI scores it), null if there is none
    public moveEffects effectsOf (int n) {
        return n == 0 ? ability1effects : n == 1 ? ability2effects : n == 2 ? ability3effects : null;
    }

    //Uses left of move n for you, and how many it has when full: its own uses and the extra ones of the level
    //milestones you have reached (moveEffects.moreUses). The rock counts the uses taken off its own, so reaching a
    //milestone gives the extra use at once, and going down a level takes it away again
    public List<Integer> usesOf (int n, character you) {
        moveEffects fx = effectsOf(n);
        int extra = fx == null ? 0 : fx.extraUses(you);
        int left = n == 0 ? ability1uses : n == 1 ? ability2uses : ability3uses;
        int full = n == 0 ? ability1usages : n == 1 ? ability2usages : ability3usages;
        return Arrays.asList(Math.max(0, left + extra), full + extra);
    }

    public boolean usesLeft (int n, character you) {return usesOf(n, you).get(0) > 0;}

    //Uses move n on them
    public void useMove (int n, character you, character them) {
        if (n == 0) ability1(you, them);
        else if (n == 1) ability2(you, them);
        else ability3(you, them);
    }

    //How the world looks while its moves play (battleAnimation.mood): night for obsidian, pink for healite...
    private battleAnimation.mood mood = battleAnimation.mood.DAY;
    public gemstone mood (battleAnimation.mood mood) {
        this.mood = mood;
        return this;
    }

    //n is 0, 1 or 2. Always with the rock's mood (DAY when it has none, so the fight goes back to day)
    public battleAnimation getAnimation (int n) {
        if (n < 0 || n > 2) return null;
        if (animations[n] != null) return animations[n].withMood(mood);
        abilityType[] types = {ability1type, ability2type, ability3type};
        return battleAnimation.forType(types[n]).withMood(mood);
    }

    //The message of a move used, with its animation playing while it is up (or the puff of a failed one)
    //With the move's hit chance, a word for the luck of it is thrown off the user too (battleAnimation.luckKey)
    private dialogue used (character you, character them, String text, int n, boolean worked) {
        battleAnimation shown = worked ? getAnimation(n) : battleAnimation.failed();
        String key = worked ? "M:" + getName() + ":" + n : "F";
        abilityType[] types = {ability1type, ability2type, ability3type};
        dialogue message = new dialogue(you, text + " " + textIcons.of(types[n]));
        if (!worked) message.animate(new battleAnimation.playing(shown, you, them, key));
        else message.animate(battleAnimation.playing.roll(shown, you, them, key, rdm));
        String luck = battleAnimation.luckKey(chances(you).get(n), worked);
        if (luck != null) message.animate(new battleAnimation.playing(battleAnimation.fromKey(luck, null), you, them, luck));
        return message;
    }

    //Every gemstone is the same grey rock texture coloured in with color, and can give off particles
    public gemstone(
        String name, String color, itemParticles particles, gameHandler handler,
        String ability1name, abilityType ability1type, moveEffects ability1effects, int ability1usages, double ability1chance,
        String ability2name, abilityType ability2type, moveEffects ability2effects, int ability2usages, double ability2chance,
        String ability3name, abilityType ability3type, moveEffects ability3effects, int ability3usages, double ability3chance) {
        super(name, null, false, handler);
        look(guiSkin.ROCK_X, guiSkin.GUI_Y, color);
        particles(particles);
        this.useFunct = (c1, c2) -> {
            if (c1.getGemstone() == this) {
                c1.deEquip();
            } else {
                c1.equipGemstone(this);
            }
        };
        this.ability1effects = ability1effects;
        this.ability2effects = ability2effects;
        this.ability3effects = ability3effects;
        this.ability1 = ability1effects::apply;
        this.ability2 = ability2effects::apply;
        this.ability3 = ability3effects::apply;
        this.ability1name = ability1name;
        this.ability1type = ability1type;
        this.ability2type = ability2type;
        this.ability3type = ability3type;
        this.ability2name = ability2name;
        this.ability3name = ability3name;
        this.ability1usages = ability1usages;
        this.ability1uses = ability1usages;

        this.ability2usages = ability2usages;
        this.ability2uses = ability2usages;

        this.ability3usages = ability3usages;
        this.ability3uses = ability3usages;

        this.ability1chance = ability1chance;
        this.ability2chance = ability2chance;
        this.ability3chance = ability3chance;
    }
    
    public void ability1 (character you, character them) {
        if (you == them) throw new IllegalArgumentException("Kan ikke slåss mot seg selv");
        if (rdm.nextDouble() <= chances(you).get(0)) {
            handler.getDiealoguecontainer().add(used(you, them, "Used " + ability1name, 0, true));
            handler.getDiealoguecontainer().setSource(ability1type);
            them.hitBy(you, () -> ability1.accept(you, them));
            handler.getDiealoguecontainer().clearSource();
        } else {
            handler.getDiealoguecontainer().add(used(you, them, "Failed at using " + ability1name, 0, false));
        }

        ability1uses-=1;
    }

    //The hit chances for you: each move's own, or the one of a level milestone you have reached, times what your
    //confusion leaves
    public List<Double> chances (character you) {
        double clear = 1 - you.getConfusion();
        return Arrays.asList(baseChance(0, you)*clear, baseChance(1, you)*clear, baseChance(2, you)*clear);
    }

    //Move n's hit chance for you without confusion
    public double baseChance (int n, character you) {
        double own = n == 0 ? ability1chance : n == 1 ? ability2chance : ability3chance;
        moveEffects fx = effectsOf(n);
        return fx == null ? own : fx.chance(own, you);
    }

    public void ability2 (character you, character them) {
        
        if (rdm.nextDouble() <= chances(you).get(1)) {
            handler.getDiealoguecontainer().add(used(you, them, "Used " + ability2name, 1, true));
            handler.getDiealoguecontainer().setSource(ability2type);
            them.hitBy(you, () -> ability2.accept(you, them));
            handler.getDiealoguecontainer().clearSource();
        } else {
            handler.getDiealoguecontainer().add(used(you, them, "Failed at using " + ability2name, 1, false));
        }

        ability2uses-=1;
    }


    public void ability3 (character you, character them) {
        if (rdm.nextDouble() <= chances(you).get(2)) {
            handler.getDiealoguecontainer().add(used(you, them, "Used " + ability3name, 2, true));
            handler.getDiealoguecontainer().setSource(ability3type);
            them.hitBy(you, () -> ability3.accept(you, them));
            handler.getDiealoguecontainer().clearSource();
        } else {
            handler.getDiealoguecontainer().add(used(you, them, "Failed at using " + ability3name, 2, false));
        }

        ability3uses-=1;

    }


    public String getAbility1name() {return ability1name;}
    public String getAbility2name() {return ability2name;}
    public String getAbility3name() {return ability3name;}

    public abilityType getAbility1type() {return ability1type;}
    public abilityType getAbility2type() {return ability2type;}
    public abilityType getAbility3type() {return ability3type;}

    //Outside a fight the numbers are worked out for the player, with no target
    @Override
    public List<infoBox> getInfoBoxes () {return getInfoBoxes(handler.getMainCharacter(), null);}

    //One box per move, its effects worked out for you against them (null when there is no target)
    public List<infoBox> getInfoBoxes (character you, character them) {
        List<infoBox> boxes = new ArrayList<infoBox>();
        String[] names = {ability1name, ability2name, ability3name};
        abilityType[] types = {ability1type, ability2type, ability3type};
        for (int n = 0; n < 3; n++) {
            List<Integer> uses = usesOf(n, you);
            boxes.add(new infoBox(types[n], names[n], uses.get(0) + "/" + uses.get(1) + " uses left, " + guiText.num(baseChance(n, you)*100) + "% hit", "").lines(effectsOf(n).lines(you, them)));
        }
        return boxes;
    }

    @Override
    protected void dialogueAdd(character you) {
        if (you.getGemstone() == this) {
            handler.getDiealoguecontainer().add(new dialogue("Game", you.getName() + " equipped " + getName()));
        } else {
            handler.getDiealoguecontainer().add(new dialogue("Game",  you.getName() + " unequipped " + getName()));
        }
    }

    public boolean hasMovesLeft (character you) {
        return usesLeft(0, you) || usesLeft(1, you) || usesLeft(2, you);
    }


    private static final int MOST_EXTRA = 50;

    //Uses left of the three abilities (of their own uses: below 0 when extra ones from milestones were taken)
    @Override
    public String saveState () {return ability1uses + "," + ability2uses + "," + ability3uses;}

    @Override
    public void loadState (String state) {
        String[] uses = state.split(",");
        if (uses.length != 3) return;
        try {
            //Below 0 when extra uses from level milestones were taken
            ability1uses = Math.max(-MOST_EXTRA, Math.min(ability1usages, Integer.parseInt(uses[0])));
            ability2uses = Math.max(-MOST_EXTRA, Math.min(ability2usages, Integer.parseInt(uses[1])));
            ability3uses = Math.max(-MOST_EXTRA, Math.min(ability3usages, Integer.parseInt(uses[2])));
        } catch (NumberFormatException e) {
            System.out.println("Ugyldige bruk i lagret " + getName() + ": " + state);
        }
    }

    public void restore() {

        if (
            (ability1uses < ability1usages) || (ability2uses < ability2usages) || (ability3uses < ability3usages)
        ) {
            handler.getDiealoguecontainer().add(new dialogue("Game", name + " restored"));
        }

        if (ability1uses < ability1usages) {ability1uses = ability1usages;}
        if (ability2uses < ability2usages) {ability2uses = ability2usages;}
        if (ability3uses < ability3usages) {ability3uses = ability3usages;}

    }

    
}
