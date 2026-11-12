package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//The 1v1 pages of the main menu (guiMainMenu hands its 1v1 page to this). See duel for the fight itself.
//- main: the challenges open on the local network to join (on top), create a challenge, the three builds (each shown
//  as its rocks and trinkets in little slots, like the More button in battle: click one to use it, Edit to change it),
//  and character settings. Back in the bottom left corner
//- character: the name you fight under and who you play as (any campaign character's looks; both fight at duel.LEVEL)
//- hosting / joining: waiting for the other player (with the waiting sign), then the handshake and into the fight
//Editing a build opens the real inventory with every rock (the Elemental Rock too: right click it to put rocks in) and
//trinket in a catalogue above it (itemSource): the build is
//what is in the four rock slots and the two trinket slots when it is closed. Rocks there act on each other as always
public class guiDuelMenu extends guiElement {

    private enum page {MAIN, CHARACTER, HOSTING, JOINING}
    private page current = page.MAIN;

    //Back to the main menu's first page
    private Runnable leave;

    private duelBuild.library library = duelBuild.library.shared();
    private guiTextField nameField;
    private guiButton createButton, characterButton, backButton, cancelButton, characterBackButton;
    private guiButton[] buildButtons = new guiButton[duelBuild.SLOTS], editButtons = new guiButton[duelBuild.SLOTS];
    private static final int MAX_CHALLENGES = 4;
    private List<guiButton> joinButtons = new ArrayList<guiButton>();
    private List<duelLink.challenge> shownChallenges = new ArrayList<duelLink.challenge>();
    //A line under the challenges when something went wrong
    private String message;

    //Listens for challenges while the 1v1 page is open. One for the whole game: the port can only be listened on once
    private static duelLink.finder finder;
    private duelLink.host hosting;
    //The connection while the handshake goes on, and who is on the other end
    private duelLink joining;
    private boolean iAmHost;
    private String otherName, otherLook;
    private duelBuild otherBuild;

    private List<character> lookChoices;
    private static final int LOOK_COLUMNS = 8;
    private double lookSlot, lookGap, lookX, lookY;

    //One made item per name, to draw the builds' little slots with
    private Map<String, item> shown = new HashMap<String, item>();

    //Where the labels and the waiting sign go, worked out in layout
    private double challengesLabelY, waitY, messageY, buildsLabelY, nameLabelY, lookLabelY;

    public guiDuelMenu (gui gui, Runnable leave) {
        super(gui);
        this.leave = leave;

        nameField = new guiTextField(gui, 16);
        nameField.setText(library.player);
        nameField.onEnter(() -> closeCharacter());

        createButton = new guiButton(gui, "Create challenge", () -> createChallenge());
        characterButton = new guiButton(gui, "", () -> {
            if (lookChoices == null) lookChoices = duel.choices(gui.getHandler());
            nameField.setText(library.player);
            current = page.CHARACTER;
        });
        characterButton.setFace(gc -> drawCharacterButton(gc));
        characterBackButton = new guiButton(gui, "Back", () -> closeCharacter());
        backButton = new guiButton(gui, "Back", () -> back());
        cancelButton = new guiButton(gui, "Cancel", () -> cancelWait());
        for (int i = 0; i < duelBuild.SLOTS; i++) {
            final int n = i;
            buildButtons[i] = new guiButton(gui, "", () -> {
                library.selected = n;
                library.save();
            });
            buildButtons[i].setFace(gc -> drawBuild(gc, n));
            editButtons[i] = new guiButton(gui, "Edit", () -> editBuild(n));
        }
        for (int i = 0; i < MAX_CHALLENGES; i++) {
            final int n = i;
            guiButton join = new guiButton(gui, "", () -> {
                if (n < shownChallenges.size()) join(shownChallenges.get(n));
            });
            join.setFace(gc -> drawChallenge(gc, n));
            joinButtons.add(join);
        }
    }

    //Escape: out of a page, or back to the main menu
    public void back () {
        switch (current) {
            case CHARACTER: closeCharacter(); break;
            case HOSTING:
            case JOINING: cancelWait(); break;
            default:
                stopFinder();
                leave.run();
        }
    }

    private void closeCharacter () {
        String name = nameField.getText().trim();
        if (!name.isEmpty()) library.player = name;
        library.save();
        current = page.MAIN;
    }

    //---- The builds ----

    //The main character of the game behind the menu holds the build while it is edited (it is not used for anything
    //else there): the inventory opens on it with every rock and trinket above it
    private void editBuild (int slot) {
        library.selected = slot;
        duelBuild build = library.get(slot);
        gameHandler handler = gui.getHandler();
        character holder = handler.getMainCharacter();
        build.giveTo(holder, handler.getItemRegistry());
        //Shown at the level of the fight, so what the rocks do is worked out as it will be there
        holder.setLevel(duel.LEVEL);
        holder.clearEffects();
        holder.resetHealth();
        holder.syncShownHealth();
        List<Supplier<item>> choices = new ArrayList<Supplier<item>>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (Supplier<item> maker : handler.getItemRegistry().getMakers()) {
            item made = maker.get();
            if (!seen.add(made.getName())) continue;
            if (made instanceof gemstone || made instanceof trinket) choices.add(maker);
        }
        gui.openBuildInventory(new itemSource(choices), "Rocks and trinkets", () -> {
            for (int i = 0; i < duelBuild.ROCKS; i++) {
                itemStack stack = holder.getInventory().get(inventory.slotKind.ROCK, i);
                build.setRock(i, stack != null ? stack.getItem() : null);
            }
            for (int i = 0; i < duelBuild.TRINKETS; i++) {
                itemStack stack = holder.getInventory().get(inventory.slotKind.TRINKET, i);
                build.setTrinket(i, stack != null ? stack.getItem() : null);
            }
            //Which rock slot is equipped (right click a rock to equip it, as in the campaign), or none
            build.setEquipped(duelBuild.NONE);
            for (int i = 0; i < duelBuild.ROCKS; i++) {
                itemStack stack = holder.getInventory().get(inventory.slotKind.ROCK, i);
                if (stack != null && stack.getItem() == holder.getGemstone()) build.setEquipped(i);
            }
            shown.clear();
            library.save();
        });
    }

    //A build's item for its little slot: slot 0-3 a rock, -1 and -2 the trinkets. Made once per build and slot
    private item shownItem (duelBuild build, int slot) {
        String key = System.identityHashCode(build) + "/" + slot;
        if (!shown.containsKey(key)) {
            itemRegistry registry = gui.getHandler().getItemRegistry();
            shown.put(key, slot >= 0 ? build.makeRock(slot, registry) : build.makeTrinket(-1 - slot, registry));
        }
        return shown.get(key);
    }

    //A build on its button: its four rocks and two trinkets in little slots (the equipped rock framed), in the middle
    private void drawBuild (GraphicsContext gc, int n) {
        double s = gui.s();
        guiButton b = buildButtons[n];
        double slot = b.height - 16*s, gap = 3*s;
        double rowWidth = (duelBuild.ROCKS + duelBuild.TRINKETS)*(slot + gap) - gap + slot/2;
        drawSlots(gc, library.get(n), b.x + (b.width - rowWidth)/2, b.y + (b.height - slot)/2, slot, gap, true);
    }

    //A build's rocks in a row of little slots from (sx, sy), and its trinkets half a slot after them if asked for
    private void drawSlots (GraphicsContext gc, duelBuild build, double sx, double sy, double slot, double gap, boolean trinkets) {
        guiSkin skin = gui.getSkin();
        int count = duelBuild.ROCKS + (trinkets ? duelBuild.TRINKETS : 0);
        for (int i = 0; i < count; i++) {
            boolean rock = i < duelBuild.ROCKS;
            double x = sx + i*(slot + gap) + (rock ? 0 : slot/2);
            skin.drawSlot(gc, x, sy, slot, rock && equippedSlot(build) == i);
            item in = shownItem(build, rock ? i : -1 - (i - duelBuild.ROCKS));
            if (in != null) {
                in.drawIcon(gc, x + slot*0.1, sy + slot*0.1, slot*0.8);
            } else {
                skin.drawSlotHint(gc, rock ? inventory.slotKind.ROCK : inventory.slotKind.TRINKET, x, sy, slot);
            }
        }
    }

    //A challenge on its Join button: "Join name" on the left, their four rocks on the right
    private void drawChallenge (GraphicsContext gc, int n) {
        if (n >= shownChallenges.size()) return;
        double s = gui.s();
        guiButton b = joinButtons.get(n);
        duelLink.challenge c = shownChallenges.get(n);
        guiText.draw(gc, "Join " + c.player, b.x + 12*s, b.y + (b.height - guiText.height(12*s))/2, 12*s, Color.BLACK);
        double slot = b.height - 12*s, gap = 3*s;
        double rowWidth = duelBuild.ROCKS*(slot + gap) - gap;
        drawSlots(gc, decoded(c.build), b.x + b.width - 8*s - rowWidth, b.y + (b.height - slot)/2, slot, gap, false);
    }

    //Builds sent over the network, decoded once each
    private Map<String, duelBuild> decodedBuilds = new HashMap<String, duelBuild>();
    private duelBuild decoded (String line) {
        return decodedBuilds.computeIfAbsent(line, duelBuild::decode);
    }

    //The rock slot a build has equipped (the first with a rock for builds saved before that was kept), -1 for none
    private static int equippedSlot (duelBuild build) {
        if (build.getEquipped() != duelBuild.FIRST) return build.getEquipped();
        for (int i = 0; i < duelBuild.ROCKS; i++) if (build.getRock(i) != null) return i;
        return -1;
    }

    //The character settings button: the portrait of who you play as on the left, as big as the button allows, and
    //"Character: name as who" next to it, both in the middle of its height
    private void drawCharacterButton (GraphicsContext gc) {
        double s = gui.s();
        guiButton b = characterButton;
        character look = lookOf(library.character);
        double portrait = Math.round(b.height - gui.getSkin().getBorder()*0.9);
        double px = Math.round(b.x + gui.getSkin().getBorder()*0.6), py = Math.round(b.y + (b.height - portrait)/2);
        if (look != null) gc.drawImage(look.getIcon(), 0, 0, guiSkin.CELL, guiSkin.CELL, px, py, portrait, portrait);
        String text = "Character: " + library.player + " as " + (look != null ? look.getName() : library.character);
        double left = px + portrait + 8*s, room = b.x + b.width - gui.getSkin().getBorder()*0.6 - left;
        double size = 13*s;
        while (size > 7 && guiText.width(text, size) > room) size -= 0.5;
        guiText.draw(gc, text, left + room/2, b.y + (b.height - guiText.height(size))/2, size, Color.BLACK, TextAlignment.CENTER);
    }

    //---- Challenges ----

    private void createChallenge () {
        duelBuild build = library.current();
        if (!build.canFight()) {
            message = "Put a rock in the build first";
            return;
        }
        try {
            hosting = new duelLink.host(library.player, build.encode());
            iAmHost = true;
            stopFinder();
            current = page.HOSTING;
        } catch (java.io.IOException e) {
            message = "Could not open a challenge: " + e.getMessage();
        }
    }

    private void join (duelLink.challenge challenge) {
        duelBuild build = library.current();
        if (!build.canFight()) {
            message = "Put a rock in the build first";
            return;
        }
        duelLink link = duelLink.connect(challenge.address, challenge.port);
        if (link == null) {
            message = "Could not reach " + challenge.player;
            return;
        }
        joining = link;
        iAmHost = false;
        otherName = challenge.player;
        otherBuild = null;
        link.send("HELLO", duelBuild.enc(library.player), build.encode(), duelBuild.enc(library.character));
        stopFinder();
        current = page.JOINING;
    }

    private void cancelWait () {
        if (hosting != null) hosting.stop();
        hosting = null;
        if (joining != null) joining.close();
        joining = null;
        current = page.MAIN;
    }

    //While hosting or joining: someone joining, then the handshake. The joiner says HELLO with its name, build and looks;
    //the host answers with its own and START: the place, where they stand and who begins. Then both go into the fight
    private void pollConnection () {
        if (hosting != null && hosting.getJoined() != null) {
            joining = hosting.getJoined();
            hosting.stop();
            hosting = null;
        }
        if (joining == null) return;
        String[] m;
        while ((m = joining.poll()) != null) {
            if (m[0].equals("HELLO") && m.length >= 4) {
                otherName = duelBuild.dec(m[1]);
                otherBuild = duelBuild.decode(m[2]);
                otherLook = duelBuild.dec(m[3]);
                if (iAmHost) {
                    duel.spot spot = duel.pickSpot(gui.getHandler());
                    if (spot == null) {
                        lost("No free place for the fight was found");
                        return;
                    }
                    boolean hostBegins = Math.random() < 0.5;
                    joining.send("HELLO", duelBuild.enc(library.player), library.current().encode(), duelBuild.enc(library.character));
                    joining.send("START", spot.encode(), String.valueOf(hostBegins));
                    launch(new duel(joining, true, library.player, library.character, library.current(), otherName, otherLook, otherBuild, spot, hostBegins));
                    return;
                }
            } else if (m[0].equals("START") && !iAmHost && otherBuild != null && m.length >= 3) {
                duel.spot spot = duel.spot.decode(m[1]);
                if (spot == null) {
                    lost("The other game sent something odd");
                    return;
                }
                launch(new duel(joining, false, library.player, library.character, library.current(), otherName, otherLook, otherBuild, spot, Boolean.parseBoolean(m[2])));
                return;
            }
        }
        if (joining != null && joining.isClosed()) lost(otherName == null ? "The other player left" : otherName + " left");
    }

    private void lost (String why) {
        if (joining != null) joining.close();
        joining = null;
        message = why;
        current = page.MAIN;
    }

    private void launch (duel fight) {
        joining = null;
        gui.getHandler().duelWithEffect(fight);
    }

    private void updateFinder () {
        if (current == page.MAIN && finder == null) finder = new duelLink.finder();
        shownChallenges = finder == null ? new ArrayList<duelLink.challenge>() : finder.list();
        if (shownChallenges.size() > MAX_CHALLENGES) shownChallenges = shownChallenges.subList(0, MAX_CHALLENGES);
    }

    //Only the menu of the game behind it stops the finder (not the copy that slides over a fight that is ending)
    private void stopFinder () {
        if (finder != null && gui.getHandler().inMenu()) {
            finder.stop();
            finder = null;
        }
    }

    //---- Looks ----

    private character lookAt (double mx, double my) {
        for (int i = 0; i < lookChoices.size(); i++) {
            double sx = lookX + (i % LOOK_COLUMNS)*(lookSlot + lookGap), sy = lookY + (i / LOOK_COLUMNS)*(lookSlot + lookGap);
            if (sx <= mx && mx < sx + lookSlot && sy <= my && my < sy + lookSlot) return lookChoices.get(i);
        }
        return null;
    }

    //The campaign character with that id, null if there is none
    private character lookOf (String id) {
        for (character c : gui.getHandler().getCharacters()) if (c.getId().equals(id)) return c;
        return null;
    }

    //---- Drawing ----

    public String title () {
        switch (current) {
            case CHARACTER: return "Character settings";
            case HOSTING:   return "Waiting for a challenger";
            case JOINING:   return "Joining " + otherName;
            default:        return "1v1";
        }
    }

    private List<guiElement> elements () {
        List<guiElement> list = new ArrayList<guiElement>();
        switch (current) {
            case CHARACTER: list.add(nameField); list.add(characterBackButton); break;
            case HOSTING:
            case JOINING: list.add(cancelButton); break;
            default:
                for (int i = 0; i < shownChallenges.size(); i++) list.add(joinButtons.get(i));
                list.add(createButton);
                for (int i = 0; i < duelBuild.SLOTS; i++) {
                    list.add(buildButtons[i]);
                    list.add(editButtons[i]);
                }
                list.add(characterButton);
                list.add(backButton);
        }
        return list;
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        double s = gui.s();
        double pad = gui.getSkin().getBorder() + 8*s;
        double buttonHeight = 40*s, gap = 8*s;
        double titleHeight = 30*s;
        double textHeight = guiText.height(12*s) + 10*s;
        double labelHeight = guiText.height(11*s) + 6*s;
        double smallWidth = 100*s;

        switch (current) {
            case CHARACTER: {
                if (lookChoices == null) lookChoices = duel.choices(gui.getHandler());
                lookSlot = 44*s;
                lookGap = 6*s;
                double gridWidth = LOOK_COLUMNS*lookSlot + (LOOK_COLUMNS - 1)*lookGap;
                int rows = Math.max(1, (lookChoices.size() + LOOK_COLUMNS - 1)/LOOK_COLUMNS);
                width = gridWidth + 2*pad;
                height = 2*pad + titleHeight + labelHeight + buttonHeight + 2*gap + labelHeight + rows*(lookSlot + lookGap) + gap + buttonHeight;
                place(screenWidth, screenHeight);
                double top = y + pad + titleHeight;
                nameLabelY = top;
                top += labelHeight;
                nameField.setBounds(x + pad, Math.round(top), gridWidth, buttonHeight);
                top += buttonHeight + 2*gap;
                lookLabelY = top;
                top += labelHeight;
                lookX = Math.round(x + pad);
                lookY = Math.round(top);
                characterBackButton.setBounds(x + pad, Math.round(y + height - pad - buttonHeight), smallWidth, buttonHeight);
                break;
            }
            case HOSTING:
            case JOINING: {
                pollConnection();
                double sign = 56*s, buttonWidth = 320*s;
                width = buttonWidth + 2*pad;
                height = 2*pad + titleHeight + 2*textHeight + 30*s + sign + 2*gap + buttonHeight;
                place(screenWidth, screenHeight);
                waitY = y + pad + titleHeight + 2*textHeight + 30*s + gap + sign/2;
                cancelButton.setBounds(x + pad, Math.round(y + height - pad - buttonHeight), smallWidth, buttonHeight);
                break;
            }
            default: {
                updateFinder();
                double inner = 420*s, editWidth = 70*s, buildHeight = 48*s;
                int challengeRows = Math.max(1, shownChallenges.size());
                width = inner + 2*pad;
                height = 2*pad + titleHeight + labelHeight + challengeRows*(buttonHeight + gap) + (message != null ? labelHeight : 0)
                    + buttonHeight + 2*gap + labelHeight + duelBuild.SLOTS*(buildHeight + gap) + gap + buttonHeight + 2*gap + buttonHeight;
                place(screenWidth, screenHeight);
                double top = y + pad + titleHeight;

                challengesLabelY = top;
                top += labelHeight;
                for (int i = 0; i < shownChallenges.size(); i++) {
                    duelLink.challenge c = shownChallenges.get(i);
                    joinButtons.get(i).setText("");
                    joinButtons.get(i).setBounds(x + pad, Math.round(top + i*(buttonHeight + gap)), inner, buttonHeight);
                }
                waitY = top + buttonHeight/2;
                top += challengeRows*(buttonHeight + gap);
                if (message != null) {
                    messageY = top;
                    top += labelHeight;
                }
                createButton.setBounds(x + pad, Math.round(top), inner, buttonHeight);
                createButton.setEnabled(library.current().canFight());
                top += buttonHeight + 2*gap;

                buildsLabelY = top;
                top += labelHeight;
                for (int i = 0; i < duelBuild.SLOTS; i++) {
                    //The build in use is tinted green
                    buildButtons[i].setColor(i == library.selected ? "#40C040" : null);
                    buildButtons[i].setBounds(x + pad, Math.round(top), inner - editWidth - gap, buildHeight);
                    editButtons[i].setBounds(Math.round(x + pad + inner - editWidth), Math.round(top), editWidth, buildHeight);
                    top += buildHeight + gap;
                }
                top += gap;

                characterButton.setBounds(x + pad, Math.round(top), inner, buttonHeight);

                backButton.setBounds(x + pad, Math.round(y + height - pad - buttonHeight), smallWidth, buttonHeight);
            }
        }
    }

    private void place (double screenWidth, double screenHeight) {
        x = Math.round((screenWidth - width)/2);
        y = Math.round((screenHeight - height)/2);
    }

    @Override
    public void draw (GraphicsContext gc) {
        double s = gui.s();
        double border = gui.getSkin().getBorder();
        double pad = border + 8*s;
        Color grey = Color.rgb(60, 60, 60);
        gui.getSkin().drawPanel(gc, x, y, width, height);
        guiText.draw(gc, title(), x + width/2, y + border + 4*s, 16*s, Color.BLACK, TextAlignment.CENTER);
        double textY = y + pad + 30*s;

        switch (current) {
            case CHARACTER: {
                guiText.draw(gc, "Your name", x + width/2, nameLabelY, 11*s, grey, TextAlignment.CENTER);
                guiText.draw(gc, "Play as (everyone fights at level " + duel.LEVEL + ")", x + width/2, lookLabelY, 11*s, grey, TextAlignment.CENTER);
                for (int i = 0; i < lookChoices.size(); i++) {
                    double sx = lookX + (i % LOOK_COLUMNS)*(lookSlot + lookGap), sy = lookY + (i / LOOK_COLUMNS)*(lookSlot + lookGap);
                    character c = lookChoices.get(i);
                    gui.getSkin().drawSlot(gc, sx, sy, lookSlot, c.getId().equals(library.character));
                    gc.drawImage(c.getIcon(), 0, 0, guiSkin.CELL, guiSkin.CELL, Math.round(sx + lookSlot*0.1), Math.round(sy + lookSlot*0.1), Math.round(lookSlot*0.8), Math.round(lookSlot*0.8));
                }
                break;
            }
            case HOSTING:
                guiText.draw(gc, library.player, x + width/2, textY, 12*s, Color.BLACK, TextAlignment.CENTER);
                double slot = 26*s, gap = 3*s, rowWidth = duelBuild.ROCKS*(slot + gap) - gap;
                drawSlots(gc, library.current(), x + (width - rowWidth)/2, textY + guiText.height(12*s) + 4*s, slot, gap, false);
                guiText.draw(gc, "Others on your network can join from 1v1", x + width/2, textY + guiText.height(12*s) + 36*s, 11*s, grey, TextAlignment.CENTER);
                guiWaiting.draw(gc, gui, x + width/2, waitY, 56*s);
                break;
            case JOINING:
                guiText.draw(gc, "Waiting for their game", x + width/2, textY, 12*s, Color.BLACK, TextAlignment.CENTER);
                guiWaiting.draw(gc, gui, x + width/2, waitY, 56*s);
                break;
            default:
                guiText.draw(gc, "Open challenges on your network", x + width/2, challengesLabelY, 11*s, grey, TextAlignment.CENTER);
                if (shownChallenges.isEmpty()) {
                    double textTop = waitY - guiText.height(12*s)/2;
                    if (finder != null && !finder.isListening()) {
                        guiText.draw(gc, "Can not look for challenges here", x + width/2, textTop, 12*s, Color.BLACK, TextAlignment.CENTER);
                    } else {
                        //Still looking: the waiting sign beside the words
                        double w = guiText.width("Looking", 12*s);
                        guiText.draw(gc, "Looking", x + width/2 + 16*s, textTop, 12*s, Color.BLACK, TextAlignment.CENTER);
                        guiWaiting.draw(gc, gui, x + width/2 - w/2 - 8*s, waitY, 28*s);
                    }
                }
                if (message != null) guiText.draw(gc, message, x + width/2, messageY, 11*s, Color.rgb(160, 30, 30), TextAlignment.CENTER);
                guiText.draw(gc, "Builds (click one to use it)", x + width/2, buildsLabelY, 11*s, grey, TextAlignment.CENTER);
        }

        for (guiElement element : elements()) element.draw(gc);
    }

    @Override
    public boolean click (double mx, double my, boolean rightClick) {
        for (guiElement element : elements()) {
            if (element.contains(mx, my)) return element.click(mx, my, rightClick);
        }
        if (current == page.CHARACTER) {
            character c = lookAt(mx, my);
            if (c != null) {
                library.character = c.getId();
                library.save();
            }
        }
        return true;
    }

    @Override
    public void hover (double mx, double my) {
        for (guiElement element : elements()) element.hover(mx, my);
        if (current == page.CHARACTER) {
            character c = lookAt(mx, my);
            if (c != null) gui.setTooltip(c.getName());
        }
    }

    @Override
    public boolean keyPressed (KeyCode code) {
        if (code == KeyCode.ESCAPE) {
            back();
            return true;
        }
        if (current == page.CHARACTER) nameField.keyPressed(code);
        return true;
    }

    @Override
    public boolean keyTyped (String typed) {
        if (current == page.CHARACTER) nameField.keyTyped(typed);
        return true;
    }
}
