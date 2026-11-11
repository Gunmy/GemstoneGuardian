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
    private String ability1description, ability2description, ability3description;
    private int ability1usages, ability2usages, ability3usages;
    private int ability1uses, ability2uses, ability3uses;
    private double ability1chance, ability2chance, ability3chance;
    private Random rdm = new Random();

    //Every gemstone is the same grey rock texture coloured in with color, and can give off particles
    public gemstone(
        String name, String color, itemParticles particles, gameHandler handler,
        String ability1name, abilityType ability1type, String ability1description, BiConsumer<character, character> ability1, int ability1usages, double ability1chance,
        String ability2name, abilityType ability2type, String ability2description, BiConsumer<character, character> ability2, int ability2usages, double ability2chance,
        String ability3name, abilityType ability3type, String ability3description, BiConsumer<character, character> ability3, int ability3usages, double ability3chance) {
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
        this.ability1 = ability1;
        this.ability2 = ability2;
        this.ability3 = ability3;
        this.ability1name = ability1name;
        this.ability1type = ability1type;
        this.ability1description = ability1description;
        this.ability2description = ability2description;
        this.ability3description = ability3description;
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
            handler.getDiealoguecontainer().add(new dialogue(you, "Used " + ability1name));
            handler.getDiealoguecontainer().setSource(ability1type);
            them.hitBy(you, () -> ability1.accept(you, them));
            handler.getDiealoguecontainer().clearSource();
        } else {
            handler.getDiealoguecontainer().add(new dialogue(you, "Failed at using " + ability1name));
        }

        ability1uses-=1;
    }

    public List<Double> chances (character you) {
        return Arrays.asList(ability1chance*(1-you.getConfusion()), ability2chance*(1-you.getConfusion()), ability3chance*(1-you.getConfusion()));
    }

    public boolean ability1usesLeft () {return ability1uses > 0;}
    public List<Integer> howManyAbility1Uses () {return Arrays.asList(ability1uses, ability1usages);}

    public void ability2 (character you, character them) {
        
        if (rdm.nextDouble() <= chances(you).get(1)) {
            handler.getDiealoguecontainer().add(new dialogue(you, "Used " + ability2name));
            handler.getDiealoguecontainer().setSource(ability2type);
            them.hitBy(you, () -> ability2.accept(you, them));
            handler.getDiealoguecontainer().clearSource();
        } else {
            handler.getDiealoguecontainer().add(new dialogue(you, "Failed at using " + ability2name));
        }

        ability2uses-=1;
    }

    public boolean ability2usesLeft () {return ability2uses > 0;}
    public List<Integer> howManyAbility2Uses () {return Arrays.asList(ability2uses, ability2usages);}

    public void ability3 (character you, character them) {
        if (rdm.nextDouble() <= chances(you).get(2)) {
            handler.getDiealoguecontainer().add(new dialogue(you, "Used " + ability3name));
            handler.getDiealoguecontainer().setSource(ability3type);
            them.hitBy(you, () -> ability3.accept(you, them));
            handler.getDiealoguecontainer().clearSource();
        } else {
            handler.getDiealoguecontainer().add(new dialogue(you, "Failed at using " + ability3name));
        }

        ability3uses-=1;

    }

    public boolean ability3usesLeft () {return ability3uses > 0;}
    public List<Integer> howManyAbility3Uses () {return Arrays.asList(ability3uses, ability3usages);}

    public String getAbility1name() {return ability1name;}
    public String getAbility2name() {return ability2name;}
    public String getAbility3name() {return ability3name;}

    public abilityType getAbility1type() {return ability1type;}
    public abilityType getAbility2type() {return ability2type;}
    public abilityType getAbility3type() {return ability3type;}

    @Override
    public List<infoBox> getInfoBoxes () {
        List<infoBox> boxes = new ArrayList<infoBox>();
        boxes.add(new infoBox(ability1type, ability1name, ability1uses + "/" + ability1usages + " uses left, " + Math.round(ability1chance*100) + "% hit", ability1description));
        boxes.add(new infoBox(ability2type, ability2name, ability2uses + "/" + ability2usages + " uses left, " + Math.round(ability2chance*100) + "% hit", ability2description));
        boxes.add(new infoBox(ability3type, ability3name, ability3uses + "/" + ability3usages + " uses left, " + Math.round(ability3chance*100) + "% hit", ability3description));
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

    public boolean hasMovesLeft () {
        return ability1usesLeft() || ability2usesLeft() || ability3usesLeft();
    }


    //Uses left of the three abilities
    @Override
    public String saveState () {return ability1uses + "," + ability2uses + "," + ability3uses;}

    @Override
    public void loadState (String state) {
        String[] uses = state.split(",");
        if (uses.length != 3) return;
        try {
            ability1uses = Math.max(0, Math.min(ability1usages, Integer.parseInt(uses[0])));
            ability2uses = Math.max(0, Math.min(ability2usages, Integer.parseInt(uses[1])));
            ability3uses = Math.max(0, Math.min(ability3usages, Integer.parseInt(uses[2])));
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
