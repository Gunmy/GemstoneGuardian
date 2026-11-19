package game;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The daily battles from the main menu (see daily and fightRun). "Play daily battles" opens the loadout straight in
//the inventory (editLoadout: the daily build with every rock and trinket above it, buildSlots.edit), with Play (into
//the first fight; the build is kept for the next day) and Back. Once today's try is used the button opens the result
//instead, the main menu's daily page, handed to this:
//- result: how today's try went, from what was saved (so it is the same after the run and from the menu later): how
//  many of the five were beaten, the five opponents on squares (the beaten ones grey and still, the others bouncing),
//  with their names and places, how many of each grade the player's actions got (fightRun.grade) with a bar of them,
//  the accuracy and the fight in numbers, the build used, and the time left until the next day's. Back
public class guiDailyMenu extends guiElement {

    private Runnable leave;
    private buildSlots slots;
    private guiButton resultBackButton;
    //When the result page was opened: its squares pop in and its counts count up from then
    private long shownAt = System.nanoTime();

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final Color SOFT = Color.rgb(60, 60, 60), GOLD = Color.web("#A07800");

    //Layout, worked out in layout
    private double pad, labelY, square, squareGap, squaresY, tileW, tileH, tileGap, tilesY, barY, statsY, loadoutY, loadoutSlot;

    public guiDailyMenu (gui gui, Runnable leave) {
        super(gui);
        this.leave = leave;
        slots = new buildSlots(gui);
        resultBackButton = new guiButton(gui, "Back", () -> leave.run());
    }

    //True once today's try is used: the main menu's button shows the result then, else it opens the loadout
    public static boolean hasResult () {return daily.shared().playedToday() && daily.shared().getResult() != null;}

    //The result page from the start (its squares pop in again)
    public void open () {
        shownAt = System.nanoTime();
        slots.clear();
    }

    public void back () {leave.run();}

    //The daily build in the inventory: Play (only with a rock in it) saves it and goes into the first fight, Back
    //closes it, back to the main menu
    public static void editLoadout (gui gui) {
        daily d = daily.shared();
        String title = "Daily battles, " + DATE.format(daily.today());
        buildSlots.edit(gui, d.getBuild(), duel.DEFAULT_LEVEL, () -> {
            d.buildChanged();
            duelBuild.library names = duelBuild.library.shared();
            d.play(gui.getHandler(), names.player, names.character);
        }, title, "Play", "Back", build -> build.canFight());
    }

    private double time () {return (System.nanoTime() - shownAt)/1e9;}

    private List<guiElement> currentElements () {
        List<guiElement> list = new ArrayList<guiElement>();
        list.add(resultBackButton);
        return list;
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        double s = gui.s();
        pad = gui.getSkin().getBorder() + 8*s;
        double titleHeight = 30*s, buttonHeight = 40*s;
        //The result: everything centred in a 512 wide column
        double column = 512*s;
        square = 84*s;
        squareGap = (column - daily.STAGES*square)/(daily.STAGES - 1);
        tileGap = 8*s;
        tileW = (column - 4*tileGap)/5;
        tileH = 50*s;
        loadoutSlot = 30*s;
        double beatenHeight = guiText.height(26*s) + 10*s;
        double namesHeight = 6*s + guiText.height(11*s) + guiText.height(10*s) + 12*s;
        width = column + 2*pad;
        height = 2*pad + titleHeight + beatenHeight + square + namesHeight + tileH + 10*s + 12*s + 12*s
            + guiText.height(11*s) + 12*s + loadoutSlot + 14*s + buttonHeight;
        place(screenWidth, screenHeight);
        double top = y + pad + titleHeight;
        labelY = top;
        squaresY = top + beatenHeight;
        tilesY = squaresY + square + namesHeight;
        barY = tilesY + tileH + 10*s;
        statsY = barY + 12*s + 12*s;
        loadoutY = statsY + guiText.height(11*s) + 12*s;
        resultBackButton.setBounds(x + pad, Math.round(y + height - pad - buttonHeight), 100*s, buttonHeight);
    }

    private void place (double screenWidth, double screenHeight) {
        x = Math.round((screenWidth - width)/2);
        y = Math.round((screenHeight - height)/2);
    }

    @Override
    public void draw (GraphicsContext gc) {
        double s = gui.s();
        double border = gui.getSkin().getBorder();
        gui.getSkin().drawPanel(gc, x, y, width, height);
        daily d = daily.shared();
        String title = "Daily battles, " + DATE.format(d.getPlayed() != null ? d.getPlayed() : daily.today());
        guiText.draw(gc, title, x + width/2, y + border + 4*s, 16*s, Color.BLACK, TextAlignment.CENTER);
        drawResult(gc, d.getResult(), s);
        for (guiElement element : currentElements()) element.draw(gc);
    }

    //---- The result ----

    private void drawResult (GraphicsContext gc, fightRun run, double s) {
        if (run == null) return;
        double t = time();
        double mid = x + width/2, left = x + pad;
        int n = run.stages.size();

        //How far: gold when all five are beaten
        String far = run.beaten() + " / " + n + " BEATEN";
        guiText.draw(gc, far, mid, labelY, 26*s, run.allBeaten() ? Color.web("#E0A800") : Color.BLACK, TextAlignment.CENTER);

        //The five on their squares, popping in one after the other
        for (int i = 0; i < n; i++) {
            fightRun.stage st = run.stages.get(i);
            double pop = fightOutcome.ease((t - 0.1 - i*0.12)/0.25);
            if (pop <= 0) continue;
            double sx = left + i*(square + squareGap), cx = sx + square/2;
            boolean beaten = i < run.beaten();
            gc.save();
            gc.setGlobalAlpha(pop);
            //Grows out of its middle as it pops in
            gc.translate(cx, squaresY + square/2);
            gc.scale(0.6 + 0.4*pop, 0.6 + 0.4*pop);
            gc.translate(-cx, -(squaresY + square/2));
            gui.getSkin().drawSlot(gc, Math.round(sx), Math.round(squaresY), square, i == run.beaten() && !run.allBeaten());
            character who = lookOf(st.look);
            double figure = square*0.78, fx = Math.round(cx - figure/2), fy = Math.round(squaresY + (square - figure)/2);
            if (who != null) {
                if (beaten) {
                    //Beaten: grey and still
                    gc.setEffect(new ColorAdjust(0, -1, -0.35, 0));
                    gc.drawImage(who.getIcon(), 0, 0, guiSkin.CELL, guiSkin.CELL, fx, fy, figure, figure);
                    gc.setEffect(null);
                } else {
                    who.draw(gc, gui, fx, fy, figure);
                }
            }
            gc.restore();
            if (pop < 1) continue;
            double ny = squaresY + square + 6*s;
            String name = st.name;
            double size = 11*s;
            while (size > 7*s && guiText.width(name, size) > square + squareGap - 4*s) size -= 0.5*s;
            guiText.draw(gc, name, cx, ny, size, beaten ? SOFT : Color.BLACK, TextAlignment.CENTER);
            String where = st.placeName;
            double wsize = 10*s;
            while (wsize > 7*s && guiText.width(where, wsize) > square + squareGap - 4*s) wsize -= 0.5*s;
            guiText.draw(gc, where, cx, ny + guiText.height(11*s), wsize, SOFT, TextAlignment.CENTER);
        }

        //The grades: a tile each, counting up, then a bar of them all
        double count = fightOutcome.ease((t - 0.8)/0.6);
        fightRun.grade[] grades = fightRun.grade.values();
        int total = run.graded();
        for (int i = 0; i < grades.length; i++) {
            fightRun.grade g = grades[i];
            double tx = left + i*(tileW + tileGap);
            Color c = Color.web(g.color);
            gc.setFill(c.deriveColor(0, 1, 1, 0.22));
            gc.fillRect(Math.round(tx), Math.round(tilesY), Math.round(tileW), Math.round(tileH));
            gc.setFill(c);
            gc.fillRect(Math.round(tx), Math.round(tilesY), Math.round(tileW), Math.round(4*s));
            int shown = (int) Math.round(run.count(g)*count);
            guiText.draw(gc, String.valueOf(shown), tx + tileW/2, tilesY + 8*s, 18*s, c.darker(), TextAlignment.CENTER);
            guiText.draw(gc, g.label, tx + tileW/2, tilesY + 10*s + guiText.height(18*s), 10*s, SOFT, TextAlignment.CENTER);
        }
        double barW = width - 2*pad, barH = 12*s;
        gc.setFill(Color.rgb(0, 0, 0, 0.15));
        gc.fillRect(Math.round(left), Math.round(barY), Math.round(barW), Math.round(barH));
        if (total > 0) {
            double bx = left;
            for (fightRun.grade g : grades) {
                double w = barW*run.count(g)/total*count;
                gc.setFill(Color.web(g.color));
                gc.fillRect(Math.round(bx), Math.round(barY), Math.round(bx + w) - Math.round(bx), Math.round(barH));
                bx += w;
            }
        }

        //The numbers
        fightOutcome.stats sum = run.total();
        String accuracy = total == 0 ? "Accuracy -" : "Accuracy " + Math.round(run.accuracy()) + "%";
        String line = accuracy + "    Turns " + sum.turns + "    Damage " + guiText.num(sum.yourDamage) + "    Best hit " + guiText.num(sum.yourBest);
        guiText.draw(gc, line, mid, statsY, 11*s, SOFT, TextAlignment.CENTER);

        //The build it was fought with
        double row = buildSlots.rowWidth(loadoutSlot, 3*s, true);
        slots.draw(gc, run.build, mid - row/2, loadoutY, loadoutSlot, 3*s, true);

        //Until the next day's
        double by = resultBackButton.y + (resultBackButton.height - guiText.height(11*s))/2;
        guiText.draw(gc, "Next daily battles in " + daily.countdown(), x + width - pad, by, 11*s, GOLD, TextAlignment.RIGHT);
    }

    //The campaign character with that id in the menu's game (its looks), null if there is none
    private character lookOf (String id) {
        for (character c : gui.getHandler().getCharacters()) if (c.getId().equals(id)) return c;
        return null;
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        for (guiElement element : currentElements()) {
            if (element.contains(mx, my)) return element.click(mx, my, rightClick);
        }
        return true;
    }

    @Override
    public void hover (double mx, double my) {
        for (guiElement element : currentElements()) element.hover(mx, my);
    }

    @Override
    public boolean keyPressed (KeyCode code) {
        if (code == KeyCode.ESCAPE) back();
        return true;
    }

    @Override
    public boolean keyTyped (String typed) {return true;}
}
