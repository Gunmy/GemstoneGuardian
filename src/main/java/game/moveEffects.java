package game;

import java.util.ArrayList;
import java.util.List;

//What a move does, written once: the list of effects is both what happens when the move is used (apply) and the
//lines its info box shows (lines). Effects happen in the order they are added.
//Every amount has a formula ("13 + lvl", shown as written, lvl = the user's level, maxHP = the user's max health)
//and a way to work it out for the user (and target) right now. A box shows each line as the worked-out number with
//the effect's icon after it, the turns with the rounds icon, and the formula in grey after that.
//Lines about the target come first, then a "You" section with what the move does to its user
//Level milestones: from(level) makes everything added after it count only once the user is at that level (a multiple
//of 5): an extra effect, an extra amount of one it has (damage adds up; a stronger burn, poison or confusion takes
//over), an extra use (moreUses) or a new hit chance (hits). The box shows the reached ones as normal lines and the
//next one greyed, with its level
public class moveEffects {

    //An amount worked out for the move's user (you) and target (them)
    public interface amount {
        double of (character you, character them);
        //True when it needs the target, which is not known outside a fight: then only the formula is shown
        default boolean needsThem () {return false;}
        //For a share of the user's health as it is when the move is used (ownCurrentHP): that share, else -1. The
        //battle simulator works these out when the move is used, not once at the start (battleSim.outcome)
        default double currentShare () {return -1;}
    }

    //share (0 to 1) of the target's max health
    public static amount theirMaxHP (double share) {
        return new amount() {
            public double of (character you, character them) {return them.getMaxHealth()*share;}
            public boolean needsThem () {return true;}
        };
    }

    //A share (0 to 1) of the target's max health that depends on the user (its level, say)
    public static amount theirMaxHP (java.util.function.ToDoubleFunction<character> share) {
        return new amount() {
            public double of (character you, character them) {return them.getMaxHealth()*share.applyAsDouble(you);}
            public boolean needsThem () {return true;}
        };
    }

    //share (0 to 1) of the user's health as it is when the move is used
    public static amount ownCurrentHP (double share) {
        return new amount() {
            public double of (character you, character them) {return you.getHealth()*share;}
            public double currentShare () {return share;}
        };
    }

    //"Rounds" (how many turns an effect lasts) is shown with the restore icon, the circular arrow, in grey
    public static final picture ROUNDS = (gc, gui, x, y, size) ->
        picture.of(gui.getHandler().getImages().getTinted(abilityType.RESTORE.getCellX(), guiSkin.ICON_Y, "#A8A8A8")).draw(gc, gui, x, y, size);

    private enum kind {
        DAMAGE(abilityType.ATTACK), HEAL(abilityType.HEAL), BURN(abilityType.FIRE), POISON(abilityType.POISON),
        HEALING(abilityType.HEAL), CONFUSION(abilityType.CONFUSE), CLEANSE(abilityType.CLEANSE), RESTORE(abilityType.RESTORE),
        REFLECT(abilityType.CLEANSE),
        //Milestones only: not done in the fight, they change the move's uses and hit chance (gemstone)
        USES(abilityType.RESTORE), HITS(abilityType.ATTACK);
        final abilityType icon;
        kind (abilityType icon) {this.icon = icon;}
    }

    private static class effect {
        final kind kind;
        final boolean onYou;
        final amount amount;
        final String formula;
        final int turns;
        //The level the user needs for it (0: always)
        final int from;
        effect (kind kind, boolean onYou, amount amount, String formula, int turns, int from) {
            this.kind = kind;
            this.onYou = onYou;
            this.amount = amount;
            this.formula = formula;
            this.turns = turns;
            this.from = from;
        }
    }

    private final List<effect> effects = new ArrayList<effect>();
    //The level what is added now needs (from)
    private int adding = 0;

    private moveEffects add (kind kind, boolean onYou, amount amount, String formula, int turns) {
        effects.add(new effect(kind, onYou, amount, formula, turns, adding));
        return this;
    }

    //Milestones: from(15) and everything after it counts once the user is level 15 or higher
    public moveEffects from (int level) {
        if (level < 0 || level % 5 != 0) throw new IllegalArgumentException("Milestones are at multiples of 5: " + level);
        adding = level;
        return this;
    }
    //count more uses when full (a milestone)
    public moveEffects moreUses (int count) {return add(kind.USES, true, (u, t) -> count, null, 0);}
    //The move's hit chance becomes chance (a milestone; the latest reached counts)
    public moveEffects hits (double chance) {return add(kind.HITS, true, (u, t) -> chance, null, 0);}

    private static int levelOf (character you) {return you == null ? 0 : you.getLevel();}
    private static boolean reached (effect e, character you) {return e.from <= levelOf(you);}
    private static boolean inFight (effect e) {return e.kind != kind.USES && e.kind != kind.HITS;}

    //The extra uses the user has reached
    public int extraUses (character you) {
        int extra = 0;
        for (effect e : effects) if (e.kind == kind.USES && reached(e, you)) extra += (int) e.amount.of(you, null);
        return extra;
    }

    //The hit chance for the user: the latest reached hits, else base
    public double chance (double base, character you) {
        double chance = base;
        for (effect e : effects) if (e.kind == kind.HITS && reached(e, you)) chance = e.amount.of(you, null);
        return chance;
    }

    //The level of the next milestone for the user, -1 if there is none left
    public int nextMilestone (character you) {
        int next = -1;
        for (effect e : effects) if (e.from > levelOf(you) && (next < 0 || e.from < next)) next = e.from;
        return next;
    }

    //Whether a milestone is at a level above after and at most upTo
    public boolean hasMilestone (int after, int upTo) {
        for (effect e : effects) if (e.from > after && e.from <= upTo) return true;
        return false;
    }

    //Damage to the target, made bigger by what the user wears (character.hitBy)
    public moveEffects damage (amount amount, String formula) {return add(kind.DAMAGE, false, amount, formula, 0);}
    //Damage the move costs its user
    public moveEffects costs (amount amount, String formula) {return add(kind.DAMAGE, true, amount, formula, 0);}
    //Heals the user at once
    public moveEffects heal (amount amount, String formula) {return add(kind.HEAL, true, amount, formula, 0);}
    //Sets the target burning: amount per turn, for turns
    public moveEffects burn (amount amount, String formula, int turns) {return add(kind.BURN, false, amount, formula, turns);}
    public moveEffects poison (amount amount, String formula, int turns) {return add(kind.POISON, false, amount, formula, turns);}
    public moveEffects poisonYou (amount amount, String formula, int turns) {return add(kind.POISON, true, amount, formula, turns);}
    //Heals the user amount per turn, for turns
    public moveEffects healing (amount amount, String formula, int turns) {return add(kind.HEALING, true, amount, formula, turns);}
    //Lowers the target's hit chance by share (0 to 1), for turns
    public moveEffects confuse (double share, int turns) {return add(kind.CONFUSION, false, (u, t) -> share, guiText.num(share*100) + "%", turns);}
    //Removes every effect on the user
    public moveEffects cleanse () {return add(kind.CLEANSE, true, null, null, 0);}
    //Restores the uses of all the user's items
    public moveEffects restore () {return add(kind.RESTORE, true, null, null, 0);}
    //Turns the user's burn, poison and confusion onto the target (character.reflectOnto)
    public moveEffects reflect () {return add(kind.REFLECT, true, null, null, 0);}

    //What the effects would do if they hit, worked out for you against them (battleAI scores moves and items by it).
    //Damage to them is made bigger by what you wear, as in a fight. Amounts are health, shares are 0 to 1
    public static class outcome {
        //heal is a fixed amount; healShare a share of the user's health when it is used (ownCurrentHP)
        public double damage, selfDamage, heal, healShare, burn, poison, selfPoison, healing, confusion;
        public int burnTurns, poisonTurns, selfPoisonTurns, healingTurns, confusionTurns;
        public boolean cleanse, restore, reflect;
    }

    public outcome outcome (character you, character them) {
        outcome o = new outcome();
        for (effect e : effects) {
            if (!inFight(e) || !reached(e, you)) continue;
            double v = e.amount == null ? 0 : e.amount.of(you, them);
            switch (e.kind) {
                case DAMAGE:
                    if (e.onYou) o.selfDamage += v;
                    else o.damage += v*you.getDamageMultiplier();
                    break;
                case HEAL:
                    if (e.amount != null && e.amount.currentShare() >= 0) o.healShare += e.amount.currentShare();
                    else o.heal += v;
                    break;
                //A milestone's burn, poison, healing or confusion takes over when it is stronger, as it would in the fight
                case BURN:      if (v >= o.burn) {o.burn = v; o.burnTurns = e.turns;} break;
                case POISON:
                    if (e.onYou) {if (v >= o.selfPoison) {o.selfPoison = v; o.selfPoisonTurns = e.turns;}}
                    else if (v >= o.poison) {o.poison = v; o.poisonTurns = e.turns;}
                    break;
                case HEALING:   if (v >= o.healing) {o.healing = v; o.healingTurns = e.turns;} break;
                case CONFUSION: if (v >= o.confusion) {o.confusion = v; o.confusionTurns = e.turns;} break;
                case CLEANSE:   o.cleanse = true; break;
                case RESTORE:   o.restore = true; break;
                case REFLECT:   o.reflect = true; break;
            }
        }
        return o;
    }

    //Every reached damage on the same fighter is added up and dealt as one hit (one number), where the first of them
    //is in the list; healing the same. So a milestone's extra damage joins the move's damage instead of hitting again
    public void apply (character you, character them) {
        boolean[] done = new boolean[effects.size()];
        for (int i = 0; i < effects.size(); i++) {
            effect e = effects.get(i);
            if (done[i] || !inFight(e) || !reached(e, you) || !strongest(e, you, them)) continue;
            character on = e.onYou ? you : them;
            switch (e.kind) {
                case DAMAGE:    on.takeDmg(sum(e, i, done, you, them)); break;
                case HEAL:      on.heal(sum(e, i, done, you, them)); break;
                case BURN:      on.addFireDamage(e.amount.of(you, them), e.turns); break;
                case POISON:    on.addPoisonDamage(e.amount.of(you, them), e.turns); break;
                case HEALING:   on.addHealing(e.amount.of(you, them), e.turns); break;
                case CONFUSION: on.addConfusion(e.amount.of(you, them), e.turns); break;
                case CLEANSE:   on.clearEffects(); break;
                case RESTORE:   on.restoreItems(); break;
                case REFLECT:   you.reflectOnto(them); break;
                default: break;
            }
        }
    }

    //e's amount and that of every later reached effect of the same kind on the same fighter, those marked done
    private double sum (effect e, int at, boolean[] done, character you, character them) {
        double total = e.amount.of(you, them);
        for (int j = at + 1; j < effects.size(); j++) {
            effect other = effects.get(j);
            if (other.kind != e.kind || other.onYou != e.onYou || !reached(other, you)) continue;
            total += other.amount.of(you, them);
            done[j] = true;
        }
        return total;
    }

    //Whether e is the one to put on: a burn, poison, healing or confusion is put on only if no other reached one of the
    //same kind on the same fighter is stronger (the later one when they are as strong), as outcome works it out.
    //Everything else always is
    private boolean strongest (effect e, character you, character them) {
        if (e.kind != kind.BURN && e.kind != kind.POISON && e.kind != kind.HEALING && e.kind != kind.CONFUSION) return true;
        double mine = e.amount.of(you, them);
        boolean after = false;
        for (effect other : effects) {
            if (other == e) {after = true; continue;}
            if (other.kind != e.kind || other.onYou != e.onYou || !reached(other, you)) continue;
            double theirs = other.amount.of(you, them);
            if (theirs > mine || (after && theirs == mine)) return false;
        }
        return true;
    }

    //The info box lines, worked out for you (null: formulas only) and them (null outside a fight)
    public List<iconText> lines (character you, character them) {
        List<iconText> theirs = new ArrayList<iconText>(), yours = new ArrayList<iconText>();
        for (effect e : effects) if (inFight(e) && reached(e, you)) (e.onYou ? yours : theirs).add(line(e, you, them));
        List<iconText> lines = new ArrayList<iconText>(theirs);
        if (!yours.isEmpty()) {
            lines.add(iconText.divider("You"));
            lines.addAll(yours);
        }
        //The next milestone, greyed, with its formulas
        int next = nextMilestone(you);
        if (next >= 0) {
            lines.add(iconText.divider("At level " + next));
            for (effect e : effects) {
                if (e.from != next) continue;
                if (e.kind == kind.USES) {
                    double more = e.amount.of(you, null);
                    lines.add(new iconText().grey("+" + guiText.num(more) + (more == 1 ? " use" : " uses")));
                }
                else if (e.kind == kind.HITS) lines.add(new iconText().grey(guiText.num(e.amount.of(you, null)*100) + "% hit"));
                else lines.add(new iconText().grey(e.onYou ? "you:" : "").appendGreyed(line(e, null, null)));
            }
        }
        return lines;
    }

    private static iconText line (effect e, character you, character them) {
        iconText line = new iconText();
        if (e.kind == kind.CLEANSE) return line.icon(e.kind.icon).gap().grey("removes effects");
        if (e.kind == kind.RESTORE) return line.icon(e.kind.icon).gap().grey("restores item uses");
        if (e.kind == kind.REFLECT) return line.icon(e.kind.icon).gap().grey("turns your effects on them");

        String shown = null;
        if (you != null && (them != null || !e.amount.needsThem())) {
            double value = e.amount.of(you, them);
            //What the user wears makes their damage bigger, like character.hitBy does in the fight
            if (e.kind == kind.DAMAGE && !e.onYou) value *= you.getDamageMultiplier();
            shown = e.kind == kind.CONFUSION ? guiText.num(value*100) + "%" : guiText.num(value);
        }
        line.text(shown != null ? shown : e.formula).icon(e.kind.icon);
        if (e.turns > 0) line.gap().text(String.valueOf(e.turns)).icon(ROUNDS);
        if (shown != null && e.formula != null && !e.formula.equals(shown)) line.gap().grey("(" + e.formula + ")");
        return line;
    }

    //The line for an effect on a character: amount per turn (or share for confusion) and the turns left
    public static iconText status (abilityType icon, String amount, int turns) {
        return new iconText().text(amount).icon(icon).gap().text(String.valueOf(turns)).icon(ROUNDS);
    }
}
