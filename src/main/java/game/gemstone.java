package game;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

public class gemstone extends item {
    private BiConsumer<character, character> ability1, ability2, ability3;
    private String ability1name, ability2name, ability3name;
    private int ability1usages, ability2usages, ability3usages;
    private int ability1uses, ability2uses, ability3uses;
    private double ability1chance, ability2chance, ability3chance;
    private Random rdm = new Random();

    public gemstone(
        String name, gameHandler handler, 
        String ability1name, BiConsumer<character, character> ability1, int ability1usages, double ability1chance,
        String ability2name, BiConsumer<character, character> ability2, int ability2usages, double ability2chance, 
        String ability3name, BiConsumer<character, character> ability3, int ability3usages, double ability3chance) {
        super(name, null, false, handler);
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
            ability1.accept(you, them);
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
            ability2.accept(you, them);
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
            ability3.accept(you, them);
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
