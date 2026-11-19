package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The win and lose screens of a fight, covering the screen while a fightOutcome is the message up (see fightOutcome
//for what each shows and when). Clicks fall through like the speech box's, so a click goes on too
public class guiOutcome extends guiElement {

    private static final Color GOLD = Color.web("#FFD040"), DEFEAT = Color.web("#A83030"), XP = Color.web("#60E060");
    private static final Color SHADOW = Color.rgb(0, 0, 0, 0.75), TEXT = Color.BLACK, SOFT = Color.rgb(60, 60, 60);

    //The choice at the end of a random battle (fightOutcome.offer)
    private guiButton againButton, backButton;

    public guiOutcome (gui gui) {
        super(gui);
        againButton = new guiButton(gui, "Battle again", () -> {if (outcome() != null) outcome().again();});
        backButton = new guiButton(gui, "Back to menu", () -> {if (outcome() != null) outcome().back();});
    }

    private boolean choosing () {
        fightOutcome o = outcome();
        return o != null && o.hasChoice() && o.finished() && !o.isLeaving();
    }

    //While there is a choice, clicks are for its buttons only (a click elsewhere does not go on)
    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        if (!choosing()) return false;
        if (againButton.contains(mx, my)) return againButton.click(mx, my, rightClick);
        if (outcome().hasBack() && backButton.contains(mx, my)) return backButton.click(mx, my, rightClick);
        return true;
    }

    @Override
    public void hover (double mx, double my) {
        if (!choosing()) return;
        againButton.hover(mx, my);
        backButton.hover(mx, my);
    }

    private fightOutcome outcome () {return gui.getHandler().getOutcome();}

    @Override
    public void layout (double screenWidth, double screenHeight) {
        setBounds(0, 0, screenWidth, screenHeight);
        double s = gui.s(), bw = 200*s, bh = 40*s, gap = 12*s, by = Math.round(screenHeight - 46*s - 12*s - bh);
        fightOutcome o = outcome();
        if (o != null) {
            againButton.setText(o.againLabel());
            backButton.setText(o.backLabel());
        }
        //One button (no back) sits in the middle
        if (o != null && !o.hasBack()) {
            againButton.setBounds(Math.round(screenWidth/2 - bw/2), by, bw, bh);
        } else {
            againButton.setBounds(Math.round(screenWidth/2 - bw - gap/2), by, bw, bh);
        }
        backButton.setBounds(Math.round(screenWidth/2 + gap/2), by, bw, bh);
    }

    @Override
    public void draw (GraphicsContext gc) {
        fightOutcome o = outcome();
        if (o == null) return;
        double s = gui.s(), t = o.time();
        //Going away (a choice was picked): the darkening fades and everything slides up off the screen
        double away = o.leaving();

        //The world behind goes darker, so the screen stands out
        gc.setFill(Color.rgb(0, 0, 0, (o.youWon ? 0.3 : 0.4)*fightOutcome.ease(t/0.5)*(1 - away)));
        gc.fillRect(0, 0, width, height);
        gc.save();
        gc.translate(0, -Math.round(away*away*height));

        double bottom = drawBanner(gc, o, t, s);
        double reveal = fightOutcome.ease((t - fightOutcome.REVEAL)/0.35);
        gc.save();
        gc.setGlobalAlpha(!o.youWon ? fightOutcome.ease((t - fightOutcome.FADE_START)/fightOutcome.FADE_TIME) : reveal);
        gc.translate(0, Math.round((1 - reveal)*20*s));
        switch (o.type) {
            case WIN: drawWin(gc, o, t, s, bottom); break;
            case LOSE: drawLose(gc, o, s, bottom); break;
            default: drawDuel(gc, o, s, bottom);
        }
        gc.restore();

        if (choosing()) {
            againButton.draw(gc);
            if (o.hasBack()) backButton.draw(gc);
        }

        //Space, once it does something
        //(not with the buttons up: only they lead on)
        if (t >= fightOutcome.HOLD && !o.isLeaving() && !(o.finished() && o.hasChoice())) {
            String hint = !o.finished() ? "PRESS SPACE TO SKIP" : "PRESS SPACE TO CONTINUE";
            double size = 13*s, pulse = 0.55 + 0.45*Math.sin(t*4);
            gc.save();
            gc.setGlobalAlpha(pulse);
            shadowed(gc, hint, width/2, height - 46*s, size, Color.WHITE, s);
            gc.restore();
        }

        gc.restore();

        //The white flash a win starts with, over everything
        if (o.youWon && t < fightOutcome.FLASH) {
            gc.setFill(Color.rgb(255, 255, 255, 1 - t/fightOutcome.FLASH));
            gc.fillRect(0, 0, width, height);
        }
    }

    //---- The banner ----

    //VICTORY slamming down, DEFEATED fading in. Returns the bottom of it and the line
    //under it
    private double drawBanner (GraphicsContext gc, fightOutcome o, double t, double s) {
        //Seen from this player: a 1v1 says VICTORY or DEFEATED like the campaign
        String word = o.youWon ? "VICTORY" : "DEFEATED";
        Color color = o.youWon ? GOLD : DEFEAT;
        double size = 54*s;
        double textH = guiText.height(size);
        double bw = guiText.width(word, size) + 90*s, bh = textH + 34*s;
        double bx = Math.round((width - bw)/2), target = Math.round(height*0.1);
        double by = target, alpha = 1;
        if (o.slams()) {
            double p = Math.min(1, t/fightOutcome.SLAM);
            by = target - (1 - p*p)*(target + bh + 10);
            //A little bounce as it lands
            double after = t - fightOutcome.SLAM;
            if (after > 0 && after < 0.18) by += Math.sin(Math.PI*after/0.18)*7*s;
            if (after >= 0 && o.fire("slam")) {
                for (int i = 0; i <= 6; i++) gui.getInteractions().sparks(bx + bw*i/6, target + bh, s*1.2, GOLD, 18, 1.1, false);
            }
        } else {
            alpha = fightOutcome.ease((t - fightOutcome.FADE_START + 0.3)/fightOutcome.FADE_TIME);
        }
        gc.save();
        gc.setGlobalAlpha(alpha);
        gui.getSkin().drawPanel(gc, bx, Math.round(by), bw, bh);
        double ty = by + (bh - textH)/2;
        guiText.draw(gc, word, width/2 + 3*s, ty + 3*s, size, SHADOW, TextAlignment.CENTER);
        guiText.draw(gc, word, width/2, ty, size, color, TextAlignment.CENTER);
        gc.restore();

        double line = by + bh + 8*s;
        String under = null;
        if (o.type == fightOutcome.kind.WIN) under = o.them.getName() + (o.rock != null ? " was beaten by" : " was beaten");
        if (o.type == fightOutcome.kind.LOSE) under = o.them.getName() + " won the fight";
        if (under != null) {
            gc.save();
            gc.setGlobalAlpha(alpha*fightOutcome.ease((t - fightOutcome.SLAM)/0.3));
            shadowed(gc, under, width/2, line, 14*s, Color.WHITE, s);
            gc.restore();
            line += guiText.height(14*s) + 6*s;
        }
        return target + bh + 8*s + (under != null ? guiText.height(14*s) + 6*s : 0);
    }

    //---- Win: the rock, the xp and the drops ----

    private void drawWin (GraphicsContext gc, fightOutcome o, double t, double s, double top) {
        double cx = width/2;
        top += 6*s;
        //The rock that won, big and glowing
        if (o.rock != null) {
            double size = 96*s;
            double pop = fightOutcome.ease((t - fightOutcome.REVEAL)/0.3);
            double shown = size*(0.6 + 0.4*pop);
            double rx = cx - shown/2, ry = top + (size - shown)/2;
            Color glow = Color.web(o.rock.getGlowColor());
            double pulse = 0.35 + 0.15*Math.sin(t*3);
            gc.setFill(new javafx.scene.paint.RadialGradient(0, 0, cx, top + size/2, size*0.75, false,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, glow.deriveColor(0, 1, 1, pulse)),
                new javafx.scene.paint.Stop(1, glow.deriveColor(0, 1, 1, 0))));
            gc.fillOval(cx - size*0.75, top + size/2 - size*0.75, size*1.5, size*1.5);
            o.rock.drawGlowOutline(gc, rx, ry, shown);
            o.rock.drawIcon(gc, rx, ry, shown);
            o.rock.drawGlowOver(gc, rx, ry, shown);
            if (t >= fightOutcome.REVEAL) o.rock.spawnParticles(gui, rx, ry, shown, gui.getRandom(), 0, 0, true);
            top += size + 4*s;
            shadowed(gc, o.rock.getName(), cx, top, 12*s, Color.WHITE, s);
            top += guiText.height(12*s) + 12*s;
        }

        //The rewards and the fight in numbers, in a panel
        double pad = gui.getSkin().getBorder() + 6*s;
        double pw = 440*s, slot = 44*s;
        double row = guiText.height(16*s);
        double ph = 2*pad + row + 6*s + 22*s + 14*s + guiText.height(11*s) + 4*s + slot + 12*s + guiText.height(11*s);
        double px = Math.round(cx - pw/2), py = Math.round(top);
        gui.getSkin().drawPanel(gc, px, py, pw, ph);
        double y = py + pad, left = px + pad, right = px + pw - pad, inner = pw - 2*pad;

        //The xp counting up, the level bar filling (and starting over at each new level)
        double counted = o.xpGained*fightOutcome.ease((t - fightOutcome.XP_START)/fightOutcome.XP_TIME);
        double xpNow = o.xpBefore + counted;
        int levelBefore = character.levelOf(o.xpBefore), level = character.levelOf(xpNow);
        guiText.draw(gc, "+" + guiText.num(counted) + " {lvl} XP", left, y, 16*s, TEXT);
        guiText.draw(gc, "Level " + level, right, y, 16*s, level > levelBefore ? Color.web("#208020") : TEXT, TextAlignment.RIGHT);
        y += row + 6*s;
        double from = character.xpOfLevel(level), to = character.xpOfLevel(level + 1);
        double share = Math.max(0, Math.min(1, (xpNow - from)/(to - from)));
        double barH = 22*s;
        gc.setFill(Color.rgb(40, 40, 40));
        gc.fillRect(left, y, inner, barH);
        gc.setFill(XP);
        gc.fillRect(left, y, inner*share, barH);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(Math.max(1, s));
        gc.strokeRect(left, y, inner, barH);
        guiText.draw(gc, guiText.num(xpNow - from) + " / " + guiText.num(to - from) + " xp", left + inner/2,
            y + (barH - guiText.height(11*s))/2, 11*s, Color.WHITE, TextAlignment.CENTER);
        //LEVEL UP, once per level gained, popping over the bar
        for (int l = levelBefore + 1; l <= level; l++) {
            double at = fightOutcome.XP_START + fightOutcome.XP_TIME*Math.max(0, Math.min(1, (character.xpOfLevel(l) - o.xpBefore)/Math.max(1e-9, o.xpGained)));
            if (o.fire("level" + l)) gui.getInteractions().sparks(left + inner, y + barH/2, s*1.3, XP, 60, 1.3, false);
            if (l == level) {
                double since = Math.max(0, t - at);
                double size = 22*s*(1 + 0.6*Math.max(0, 1 - since/0.25));
                shadowed(gc, "LEVEL UP!", left + inner/2, y - guiText.height(size) - 2*s, size, XP, s);
            }
        }
        y += barH + 14*s;

        //The drops, popping out one by one
        guiText.draw(gc, o.drops.isEmpty() ? "No drops" : "Drops", left, y, 11*s, SOFT);
        y += guiText.height(11*s) + 4*s;
        double gap = 6*s;
        double rowW = o.drops.size()*(slot + gap) - gap;
        double sx = Math.round(cx - rowW/2);
        for (int i = 0; i < o.drops.size(); i++) {
            double x = sx + i*(slot + gap);
            gui.getSkin().drawSlot(gc, x, y, slot, false);
            double since = t - o.dropTime(i);
            if (since < 0) continue;
            item drop = o.drops.get(i);
            if (o.fire("drop" + i)) gui.getInteractions().changeBurst(drop, x, y, slot);
            double grow = 1 + 0.35*Math.max(0, 1 - since/0.2);
            double icon = slot*0.8*grow;
            drop.drawIcon(gc, x + (slot - icon)/2, y + (slot - icon)/2, icon);
            if (o.dropCounts.get(i) > 1) {
                guiText.draw(gc, String.valueOf(o.dropCounts.get(i)), x + slot*0.9, y + slot*0.62, slot*0.3, TEXT, TextAlignment.RIGHT);
            }
        }
        y += slot + 12*s;

        drawNumbers(gc, o.numbers, cx, y, s, TEXT);
    }

    //---- Lose: the fight in numbers ----

    private void drawLose (GraphicsContext gc, fightOutcome o, double s, double top) {
        double pad = gui.getSkin().getBorder() + 6*s;
        double pw = 440*s, ph = 2*pad + guiText.height(11*s);
        double px = Math.round(width/2 - pw/2), py = Math.round(top + 12*s);
        gui.getSkin().drawPanel(gc, px, py, pw, ph);
        drawNumbers(gc, o.numbers, width/2, py + pad, s, TEXT);
    }

    //Turns, the damage you did and your best hit, in one line
    private void drawNumbers (GraphicsContext gc, fightOutcome.stats n, double cx, double y, double s, Color color) {
        String line = "Turns: " + n.turns + "     Damage dealt: " + guiText.num(n.yourDamage) + "     Best hit: " + guiText.num(n.yourBest);
        guiText.draw(gc, line, cx, y, 11*s, color, TextAlignment.CENTER);
    }

    //---- 1v1: the winner and the loser side by side ----

    private void drawDuel (GraphicsContext gc, fightOutcome o, double s, double top) {
        double pad = gui.getSkin().getBorder() + 8*s;
        //A random battle has the streak in a column between the two fighters, so the panel is wider, not taller: under
        //it are the Battle again and Back to menu buttons
        boolean streak = o.streakAfter >= 0;
        double column = 230*s, gapBetween = (streak ? STREAK_COLUMN : 20)*s, portrait = 88*s, slot = 30*s, slotGap = 4*s;
        double line = guiText.height(11*s) + 4*s;
        double ph = 2*pad + portrait + 8*s + guiText.height(14*s) + 4*s + line + 8*s + 2*(slot + slotGap) + 8*s + 2*line + 6*s + line;
        double pw = 2*column + gapBetween + 2*pad;
        double shake = streakShake(o, s);
        double px = Math.round(width/2 - pw/2 + shake), py = Math.round(top + 10*s + shake*0.5);
        gui.getSkin().drawPanel(gc, px, py, pw, ph);

        fightOutcome.stats n = o.numbers;
        boolean youWon = o.youWon;
        //The winner on the left
        drawFighter(gc, o.winner(), true, px + pad, py + pad, column, portrait, slot, slotGap, s,
            youWon ? n.yourDamage : n.theirDamage, youWon ? n.yourBest : n.theirBest);
        drawFighter(gc, o.loser(), false, px + pad + column + gapBetween, py + pad, column, portrait, slot, slotGap, s,
            youWon ? n.theirDamage : n.yourDamage, youWon ? n.theirBest : n.yourBest);
        double turnsY = py + ph - pad - guiText.height(11*s);
        guiText.draw(gc, "Turns: " + n.turns, px + pw/2, turnsY, 11*s, SOFT, TextAlignment.CENTER);
        if (streak) {
            gc.save();
            gc.translate(px - Math.round(width/2 - pw/2), 0);
            drawStreak(gc, o, s, py + pad, turnsY - 6*s);
            gc.restore();
        }
    }

    //The column between the fighters (in s) and the streak's number size
    private static final double STREAK_COLUMN = 170, STREAK_SIZE = 50;

    //A random battle's streak in the middle of the panel, between the winner and the loser, from top to bottom (the
    //Turns line): "WIN STREAK" over a big number, centred in that height. Nothing of it shows until the panel is in;
    //then it is slapped on (fightOutcome.streakAt): it comes down from three times its size and lands with sparks and a
    //shake of the panel (streakShake), and NEW BEST! (pulsing green) or the streak that was lost (red) pops in under it.
    //With cheat mode on in the fight the number is CHEAT! in red, as big as fits the column (it did not count)
    private void drawStreak (GraphicsContext gc, fightOutcome o, double s, double top, double bottom) {
        double since = o.time() - o.streakAt();
        if (since < 0) return;
        double cx = width/2;
        double labelH = guiText.height(12*s), base = STREAK_SIZE*s, numberH = guiText.height(base), underH = guiText.height(14*s);
        boolean note = o.hasStreakNote();
        double blockH = labelH + 2*s + numberH + (note ? 2*s + underH : 0);
        double y = Math.round(top + (bottom - top - blockH)/2);
        double numberMid = y + labelH + 2*s + numberH/2;

        //Coming down: big and faint to its size, faster and faster, then a little squash as it lands
        double p = Math.min(1, since/fightOutcome.STREAK_SLAM);
        double scale = 1 + 2*(1 - p*p);
        double after = since - fightOutcome.STREAK_SLAM;
        if (after > 0 && after < 0.15) scale *= 1 - 0.08*Math.sin(Math.PI*after/0.15);
        gc.save();
        gc.setGlobalAlpha(gc.getGlobalAlpha()*Math.min(1, since/0.08));
        gc.translate(cx, numberMid);
        gc.scale(scale, scale);
        gc.translate(-cx, -numberMid);

        guiText.draw(gc, "WIN STREAK", cx, y, 12*s, SOFT, TextAlignment.CENTER);
        y += labelH + 2*s;
        String number = o.cheated ? "CHEAT!" : String.valueOf(o.streakAfter);
        Color color = o.cheated ? DEFEAT : o.youWon ? Color.web("#D8A000") : DEFEAT;
        double numberSize = base;
        while (numberSize > 10*s && guiText.width(number, numberSize) > (STREAK_COLUMN - 12)*s) numberSize -= s;
        double numberY = y + (numberH - guiText.height(numberSize))/2;
        guiText.draw(gc, number, cx + 3*s, numberY + 3*s, numberSize, SHADOW, TextAlignment.CENTER);
        guiText.draw(gc, number, cx, numberY, numberSize, color, TextAlignment.CENTER);
        gc.restore();
        if (after >= 0 && o.fire("streak")) {
            boolean gold = o.youWon && !o.cheated;
            gui.getInteractions().sparks(cx, numberMid, s*1.3, gold ? GOLD : DEFEAT, gold ? (o.newBest ? 60 : 30) : 15, 1.2, false);
        }
        y += numberH + 2*s;

        //The line under it pops in a moment after
        double noteSince = since - fightOutcome.STREAK_SLAM - fightOutcome.STREAK_NOTE;
        if (!note || noteSince < 0) return;
        double pop = 1 + 0.5*Math.max(0, 1 - noteSince/0.15);
        double size = (o.youWon ? 14 : 11)*s*pop;
        double ny = y + (guiText.height((o.youWon ? 14 : 11)*s) - guiText.height(size))/2;
        gc.save();
        if (o.youWon) gc.setGlobalAlpha(gc.getGlobalAlpha()*(0.7 + 0.3*Math.sin(o.time()*5)));
        if (o.youWon && o.fire("best")) gui.getInteractions().sparks(cx, y + underH/2, s*1.1, XP, 30, 1.0, false);
        String text = o.youWon ? "NEW BEST!" : "Lost a streak of " + o.streakBefore;
        guiText.draw(gc, text, cx, ny, size, o.youWon ? Color.web("#30A030") : DEFEAT, TextAlignment.CENTER);
        gc.restore();
    }

    //The panel's shake as the streak lands on it, in pixels (0 before and after)
    private double streakShake (fightOutcome o, double s) {
        if (o.streakAfter < 0) return 0;
        double after = o.time() - o.streakAt() - fightOutcome.STREAK_SLAM;
        if (after < 0 || after > 0.25) return 0;
        return Math.sin(after*70)*5*s*(1 - after/0.25);
    }

    private void drawFighter (GraphicsContext gc, character c, boolean winner, double x, double y, double w, double portrait,
                              double slot, double slotGap, double s, double damage, double best) {
        double cx = x + w/2;
        double frame = portrait + 10*s;
        gui.getSkin().drawSlot(gc, Math.round(cx - frame/2), Math.round(y), frame, winner);
        //The loser's portrait greyed out
        if (!winner) gc.setEffect(new ColorAdjust(0, -1, -0.35, 0));
        gc.drawImage(c.getIcon(), 0, 0, guiSkin.CELL, guiSkin.CELL, Math.round(cx - portrait/2), Math.round(y + 5*s), Math.round(portrait), Math.round(portrait));
        gc.setEffect(null);
        y += frame + 6*s;
        guiText.draw(gc, c.getName(), cx, y, 14*s, winner ? TEXT : SOFT, TextAlignment.CENTER);
        y += guiText.height(14*s) + 4*s;
        guiText.draw(gc, winner ? "Winner" : "Beaten", cx, y, 11*s, winner ? Color.web("#A07800") : SOFT, TextAlignment.CENTER);
        y += guiText.height(11*s) + 12*s;

        //Their build: the four rocks (the equipped one framed), the two trinkets under them
        inventory inv = c.getInventory();
        double rowW = inventory.ACTIVE*(slot + slotGap) - slotGap;
        for (int i = 0; i < inventory.ACTIVE; i++) {
            itemStack stack = inv.get(inventory.slotKind.ROCK, i);
            drawSmallSlot(gc, stack, inventory.slotKind.ROCK, cx - rowW/2 + i*(slot + slotGap), y, slot, stack != null && stack.getItem() == c.getGemstone(), !winner);
        }
        y += slot + slotGap;
        double trinketW = inventory.TRINKETS*(slot + slotGap) - slotGap;
        for (int i = 0; i < inventory.TRINKETS; i++) {
            drawSmallSlot(gc, inv.get(inventory.slotKind.TRINKET, i), inventory.slotKind.TRINKET, cx - trinketW/2 + i*(slot + slotGap), y, slot, false, !winner);
        }
        y += slot + 12*s;

        guiText.draw(gc, "Damage dealt: " + guiText.num(damage), cx, y, 11*s, TEXT, TextAlignment.CENTER);
        y += guiText.height(11*s) + 4*s;
        guiText.draw(gc, "Best hit: " + guiText.num(best), cx, y, 11*s, TEXT, TextAlignment.CENTER);
    }

    private void drawSmallSlot (GraphicsContext gc, itemStack stack, inventory.slotKind kind, double x, double y, double size, boolean selected, boolean grey) {
        x = Math.round(x);
        y = Math.round(y);
        gui.getSkin().drawSlot(gc, x, y, size, selected);
        if (stack == null) {
            gui.getSkin().drawSlotHint(gc, kind, x, y, size);
            return;
        }
        if (grey) gc.setEffect(new ColorAdjust(0, -1, -0.2, 0));
        stack.getItem().drawIcon(gc, x + size*0.1, y + size*0.1, size*0.8);
        gc.setEffect(null);
    }

    //Text with a dark shadow under it, readable on the world
    private void shadowed (GraphicsContext gc, String text, double cx, double y, double size, Color color, double s) {
        guiText.draw(gc, text, cx + 2*s, y + 2*s, size, SHADOW, TextAlignment.CENTER);
        guiText.draw(gc, text, cx, y, size, color, TextAlignment.CENTER);
    }
}
