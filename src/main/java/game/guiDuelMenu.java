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
//- character: the name you fight under and who you play as (any campaign character's looks)
//The main page's bottom row has the battle level both fight at (settings.getDuelLevel) beside Back
//- strength: how strong the computer plays in random battles (aiStrength, kept in the settings), opened with the AI
//  button beside Battle random
//- hosting / joining: waiting for the other player (with the waiting sign), then the handshake and into the fight
//- to the right of it on the main page, the best streak of random battles: who the player was then (big), the build it was
//  won with and Equip loadout, which picks that build like a fourth one that can not be edited (duelBuild.library.BEST)
//Editing a build opens the real inventory with every rock (the Elemental Rock too: right click it to put rocks in) and
//trinket in a catalogue above it (itemSource): the build is
//what is in the four rock slots and the two trinket slots when it is closed. Rocks there act on each other as always
public class guiDuelMenu extends guiElement {

    private enum page {MAIN, CHARACTER, HOSTING, JOINING, STRENGTH}
    private page current = page.MAIN;

    //Back to the main menu's first page
    private Runnable leave;

    private duelBuild.library library = duelBuild.library.shared();
    private guiTextField nameField;
    //The level both fight at: its text, with a - and a + at the ends of its row
    private guiButton levelButton, levelDown, levelUp;
    private guiButton createButton, randomButton, characterButton, backButton, cancelButton, characterBackButton, equipBestButton;
    private guiButton strengthButton, strengthBackButton;
    private guiStrengthPicker strengthPicker;
    private double strengthLabelY;
    private guiButton[] buildButtons = new guiButton[duelBuild.SLOTS], editButtons = new guiButton[duelBuild.SLOTS];
    //Under the builds: the random build (duelBuild.library.RANDOM), its slots cycling through every rock and trinket,
    //with a "Random" label where Edit is
    private guiButton randomBuildButton, randomLabel;
    private List<item> rockPool, trinketPool;
    //The build the coming fight is fought with: library.current(), or a new random one each fight. Rolled when a
    //challenge is made, joined or a random battle starts, so the hosting page and the handshake show and send the same
    private duelBuild inUse;
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
    //The main panel's width (the whole element is wider on the main page, with the best streak box to its right) and
    //that box: where it is and its portrait and slots
    private double panelWidth, bestX, bestY, bestW, bestH, bestPortrait, bestSlotsY, bestSlot, bestGap;

    public guiDuelMenu (gui gui, Runnable leave) {
        super(gui);
        this.leave = leave;

        nameField = new guiTextField(gui, 16);
        nameField.setText(library.player);
        nameField.onEnter(() -> closeCharacter());

        createButton = new guiButton(gui, "Create challenge", () -> createChallenge());
        randomButton = new guiButton(gui, "Battle random", () -> battleRandom());
        strengthButton = new guiButton(gui, "AI", () -> show(page.STRENGTH));
        strengthBackButton = new guiButton(gui, "Back", () -> show(page.MAIN));
        settings options = gui.getHandler().getSettings();
        strengthPicker = new guiStrengthPicker(gui, () -> options.getDuelAI(), chosen -> options.setDuelAI(chosen));
        characterButton = new guiButton(gui, "", () -> {
            if (lookChoices == null) lookChoices = duel.choices(gui.getHandler());
            nameField.setText(library.player);
            show(page.CHARACTER);
        });
        characterButton.setFace(gc -> drawCharacterButton(gc));
        characterBackButton = new guiButton(gui, "Back", () -> closeCharacter());
        levelButton = new guiButton(gui, "", () -> {});
        levelButton.setEnabled(false);
        levelDown = new guiButton(gui, "-", () -> options.setDuelLevel(options.getDuelLevel() - 1));
        levelUp = new guiButton(gui, "+", () -> options.setDuelLevel(options.getDuelLevel() + 1));
        equipBestButton = new guiButton(gui, "Equip loadout", () -> {
            if (library.bestBuild == null) return;
            library.selected = duelBuild.library.BEST;
            library.save();
        });
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
        randomBuildButton = new guiButton(gui, "", () -> {
            library.selected = duelBuild.library.RANDOM;
            library.save();
        });
        randomBuildButton.setFace(gc -> drawRandomBuild(gc));
        randomLabel = new guiButton(gui, "Random", () -> {});
        randomLabel.setEnabled(false);
        for (int i = 0; i < MAX_CHALLENGES; i++) {
            final int n = i;
            guiButton join = new guiButton(gui, "", () -> {
                if (n < shownChallenges.size()) join(shownChallenges.get(n));
            });
            join.setFace(gc -> drawChallenge(gc, n));
            joinButtons.add(join);
        }
    }

    //Another page: the main menu (which draws this one) switches pages (guiElement.swapPage)
    private void show (page next) {gui.getMainMenu().swapPage(() -> current = next);}

    //Escape: out of a page, or back to the main menu
    public void back () {
        switch (current) {
            case CHARACTER: closeCharacter(); break;
            case STRENGTH: show(page.MAIN); break;
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
        show(page.MAIN);
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
        holder.setLevel(gui.getHandler().getSettings().getDuelLevel());
        holder.clearEffects();
        holder.resetHealth();
        holder.syncShownHealth();
        List<Supplier<item>> choices = new ArrayList<Supplier<item>>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (Supplier<item> maker : handler.getItemRegistry().getMakers()) {
            item made = maker.get();
            if (!seen.add(made.getName())) continue;
            //Only the forms found in chests or dropped in the campaign; the others are made by turning those
            if (!handler.isHandedOut(made.getName())) continue;
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

    //The random build's row: each slot shows rock after rock (trinket after trinket), every slot on its own beat so the
    //row keeps rolling
    private void drawRandomBuild (GraphicsContext gc) {
        if (rockPool == null) {
            List<String> rocks = new ArrayList<String>(), trinkets = new ArrayList<String>();
            duelBuild.handedOut(gui.getHandler(), rocks, trinkets);
            itemRegistry registry = gui.getHandler().getItemRegistry();
            rockPool = new ArrayList<item>();
            trinketPool = new ArrayList<item>();
            for (String r : rocks) rockPool.add(registry.make(r));
            for (String w : trinkets) trinketPool.add(registry.make(w));
            rockPool.removeIf(i -> i == null);
            trinketPool.removeIf(i -> i == null);
        }
        double s = gui.s();
        guiButton b = randomBuildButton;
        double slot = b.height - 16*s, gap = 3*s;
        double rowWidth = (duelBuild.ROCKS + duelBuild.TRINKETS)*(slot + gap) - gap + slot/2;
        double sx = b.x + (b.width - rowWidth)/2, sy = b.y + (b.height - slot)/2;
        double seconds = System.nanoTime()/1e9;
        guiSkin skin = gui.getSkin();
        for (int i = 0; i < duelBuild.ROCKS + duelBuild.TRINKETS; i++) {
            boolean rock = i < duelBuild.ROCKS;
            double x = sx + i*(slot + gap) + (rock ? 0 : slot/2);
            skin.drawSlot(gc, x, sy, slot, false);
            List<item> pool = rock ? rockPool : trinketPool;
            if (pool.isEmpty()) continue;
            //A new item every 0.45 s, each slot a little after the one before it, picked by a hash so neighbours differ
            long beat = (long) Math.floor(seconds/0.45 - i*0.3);
            long h = (beat*31 + i*7919)*0x9E3779B97F4A7C15L;
            int pick = (int) Math.floorMod(h >>> 17, (long) pool.size());
            pool.get(pick).drawIcon(gc, x + slot*0.1, sy + slot*0.1, slot*0.8);
        }
    }

    //The build to fight with now: a new random one when the random build is picked
    private duelBuild rollBuild () {
        inUse = library.isRandom() ? duelBuild.random(gui.getHandler()) : library.current();
        return inUse;
    }

    //Whether the picked build can fight (the random build always can)
    private boolean canFight () {return library.isRandom() || library.current().canFight();}

    //A challenge on its Join button: "Join name" on the left, their four rocks on the right
    private void drawChallenge (GraphicsContext gc, int n) {
        if (n >= shownChallenges.size()) return;
        double s = gui.s();
        guiButton b = joinButtons.get(n);
        duelLink.challenge c = shownChallenges.get(n);
        String join = "Join " + c.player;
        guiText.draw(gc, join, b.x + 12*s, b.y + (b.height - guiText.height(12*s))/2, 12*s, Color.BLACK);
        //The level the one who made it picked, small and grey after the name
        guiText.draw(gc, "Level " + c.level, b.x + 12*s + guiText.width(join, 12*s) + 10*s, b.y + (b.height - guiText.height(11*s))/2, 11*s, Color.rgb(60, 60, 60));
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
        duelBuild build = rollBuild();
        if (!build.canFight()) {
            message = "Put a rock in the build first";
            return;
        }
        try {
            hosting = new duelLink.host(library.player, build.encode(), gui.getHandler().getSettings().getDuelLevel());
            iAmHost = true;
            stopFinder();
            show(page.HOSTING);
        } catch (java.io.IOException e) {
            message = "Could not open a challenge: " + e.getMessage();
        }
    }

    //A 1v1 against the computer: a random campaign character (not the one you play as) with a random build
    //(duelBuild.random), played by battleAI
    private void battleRandom () {
        duelBuild build = rollBuild();
        if (!build.canFight()) {
            message = "Put a rock in the build first";
            return;
        }
        duel fight = duel.againstRandom(gui.getHandler(), library.player, library.character, build, gui.getHandler().getSettings().getDuelLevel());
        if (fight == null) {
            message = "No free place for the fight was found";
            return;
        }
        stopFinder();
        launch(fight);
    }

    private void join (duelLink.challenge challenge) {
        duelBuild build = rollBuild();
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
        show(page.JOINING);
    }

    private void cancelWait () {
        if (hosting != null) hosting.stop();
        hosting = null;
        if (joining != null) joining.close();
        joining = null;
        show(page.MAIN);
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
                    joining.send("HELLO", duelBuild.enc(library.player), inUse.encode(), duelBuild.enc(library.character));
                    int level = gui.getHandler().getSettings().getDuelLevel();
                    joining.send("START", spot.encode(), String.valueOf(hostBegins), String.valueOf(level));
                    launch(new duel(joining, true, library.player, library.character, inUse, otherName, otherLook, otherBuild, spot, hostBegins, level));
                    return;
                }
            } else if (m[0].equals("START") && !iAmHost && otherBuild != null && m.length >= 3) {
                duel.spot spot = duel.spot.decode(m[1]);
                if (spot == null) {
                    lost("The other game sent something odd");
                    return;
                }
                //The host's level (an older game sends none: the old fixed one)
                int level = duel.DEFAULT_LEVEL;
                try {
                    if (m.length >= 4) level = Math.max(0, Math.min(duel.MAX_LEVEL, Integer.parseInt(m[3])));
                } catch (NumberFormatException e) {
                    level = duel.DEFAULT_LEVEL;
                }
                launch(new duel(joining, false, library.player, library.character, inUse, otherName, otherLook, otherBuild, spot, Boolean.parseBoolean(m[2]), level));
                return;
            }
        }
        if (joining != null && joining.isClosed()) lost(otherName == null ? "The other player left" : otherName + " left");
    }

    private void lost (String why) {
        if (joining != null) joining.close();
        joining = null;
        message = why;
        show(page.MAIN);
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
            case STRENGTH:  return "Computer strength";
            case HOSTING:   return "Waiting for a challenger";
            case JOINING:   return "Joining " + otherName;
            default:        return "1v1";
        }
    }

    private List<guiElement> elements () {
        List<guiElement> list = new ArrayList<guiElement>();
        switch (current) {
            case CHARACTER: list.add(nameField); list.add(characterBackButton); break;
            case STRENGTH: list.addAll(strengthPicker.elements()); list.add(strengthBackButton); break;
            case HOSTING:
            case JOINING: list.add(cancelButton); break;
            default:
                for (int i = 0; i < shownChallenges.size(); i++) list.add(joinButtons.get(i));
                list.add(createButton);
                list.add(randomButton);
                list.add(strengthButton);
                for (int i = 0; i < duelBuild.SLOTS; i++) {
                    list.add(buildButtons[i]);
                    list.add(editButtons[i]);
                }
                list.add(randomBuildButton);
                list.add(randomLabel);
                list.add(characterButton);
                list.add(equipBestButton);
                list.add(backButton);
                list.add(levelDown);
                list.add(levelButton);
                list.add(levelUp);
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
                panelWidth = width;
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
            case STRENGTH: {
                //A label, the strength's two rows and Back, in a 320 wide column
                double buttonWidth = 320*s;
                width = buttonWidth + 2*pad;
                panelWidth = width;
                height = 2*pad + titleHeight + labelHeight + guiStrengthPicker.height(s) + 2*gap + buttonHeight;
                place(screenWidth, screenHeight);
                double top = y + pad + titleHeight;
                strengthLabelY = top;
                strengthPicker.layout(x + pad, top + labelHeight, buttonWidth, s);
                strengthBackButton.setBounds(x + pad, Math.round(y + height - pad - buttonHeight), smallWidth, buttonHeight);
                break;
            }
            case HOSTING:
            case JOINING: {
                pollConnection();
                double sign = 56*s, buttonWidth = 320*s;
                width = buttonWidth + 2*pad;
                panelWidth = width;
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
                panelWidth = inner + 2*pad;
                height = 2*pad + titleHeight + labelHeight + challengeRows*(buttonHeight + gap) + (message != null ? labelHeight : 0)
                    + buttonHeight + gap + buttonHeight + 2*gap + labelHeight + (duelBuild.SLOTS + 1)*(buildHeight + gap) + gap + buttonHeight + 2*gap + buttonHeight;
                //The best streak box, beside the panel at its top and as tall as what is in it
                double boxGap = 16*s;
                bestW = 230*s;
                bestPortrait = 112*s;
                bestSlot = 34*s;
                bestGap = 4*s;
                bestH = 2*pad + guiText.height(16*s) + 8*s + bestPortrait + 10*s + 6*s + guiText.height(12*s) + 12*s
                    + 2*bestSlot + bestGap + 12*s + buttonHeight;
                width = panelWidth + boxGap + bestW;
                place(screenWidth, screenHeight);
                bestX = x + panelWidth + boxGap;
                bestY = y;
                bestSlotsY = bestY + bestH - pad - buttonHeight - 12*s - (2*bestSlot + bestGap);
                equipBestButton.setBounds(Math.round(bestX + pad), Math.round(bestY + bestH - pad - buttonHeight), bestW - 2*pad, buttonHeight);
                equipBestButton.setEnabled(library.bestBuild != null);
                equipBestButton.setColor(library.selected == duelBuild.library.BEST && library.bestBuild != null ? "#40C040" : null);
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
                createButton.setEnabled(canFight());
                top += buttonHeight + gap;
                //The AI button (the computer's strength) sits at the end of Battle random's row, as Edit does by a build
                randomButton.setBounds(x + pad, Math.round(top), inner - editWidth - gap, buttonHeight);
                randomButton.setEnabled(canFight());
                strengthButton.setBounds(Math.round(x + pad + inner - editWidth), Math.round(top), editWidth, buttonHeight);
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
                randomBuildButton.setColor(library.isRandom() ? "#40C040" : null);
                randomBuildButton.setBounds(x + pad, Math.round(top), inner - editWidth - gap, buildHeight);
                randomLabel.setBounds(Math.round(x + pad + inner - editWidth), Math.round(top), editWidth, buildHeight);
                top += buildHeight + gap;
                top += gap;

                characterButton.setBounds(x + pad, Math.round(top), inner, buttonHeight);

                double bottom = Math.round(y + height - pad - buttonHeight);
                backButton.setBounds(x + pad, bottom, smallWidth, buttonHeight);
                //The battle level, in the rest of Back's row: - and + at the ends (the settings' side-button pattern)
                int level = gui.getHandler().getSettings().getDuelLevel();
                double levelX = x + pad + smallWidth + gap, levelW = inner - smallWidth - gap;
                levelDown.setBounds(Math.round(levelX), bottom, buttonHeight, buttonHeight);
                levelUp.setBounds(Math.round(levelX + levelW - buttonHeight), bottom, buttonHeight, buttonHeight);
                levelButton.setBounds(Math.round(levelX + buttonHeight + gap), bottom, levelW - 2*(buttonHeight + gap), buttonHeight);
                levelButton.setText("Battle level: " + level);
                levelDown.setEnabled(level > 0);
                levelUp.setEnabled(level < duel.MAX_LEVEL);
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
        gui.getSkin().drawPanel(gc, x, y, panelWidth, height);
        double mid = x + panelWidth/2;
        guiText.draw(gc, title(), mid, y + border + 4*s, 16*s, Color.BLACK, TextAlignment.CENTER);
        double textY = y + pad + 30*s;

        switch (current) {
            case CHARACTER: {
                guiText.draw(gc, "Your name", mid, nameLabelY, 11*s, grey, TextAlignment.CENTER);
                guiText.draw(gc, "Play as", mid, lookLabelY, 11*s, grey, TextAlignment.CENTER);
                for (int i = 0; i < lookChoices.size(); i++) {
                    double sx = lookX + (i % LOOK_COLUMNS)*(lookSlot + lookGap), sy = lookY + (i / LOOK_COLUMNS)*(lookSlot + lookGap);
                    character c = lookChoices.get(i);
                    gui.getSkin().drawSlot(gc, sx, sy, lookSlot, c.getId().equals(library.character));
                    gc.drawImage(c.getIcon(), 0, 0, guiSkin.CELL, guiSkin.CELL, Math.round(sx + lookSlot*0.1), Math.round(sy + lookSlot*0.1), Math.round(lookSlot*0.8), Math.round(lookSlot*0.8));
                }
                break;
            }
            case STRENGTH:
                guiText.draw(gc, "In random battles", mid, strengthLabelY, 11*s, grey, TextAlignment.CENTER);
                break;
            case HOSTING:
                guiText.draw(gc, library.player + ", level " + gui.getHandler().getSettings().getDuelLevel(), mid, textY, 12*s, Color.BLACK, TextAlignment.CENTER);
                double slot = 26*s, gap = 3*s, rowWidth = duelBuild.ROCKS*(slot + gap) - gap;
                drawSlots(gc, inUse != null ? inUse : library.current(), mid - rowWidth/2, textY + guiText.height(12*s) + 4*s, slot, gap, false);
                guiText.draw(gc, "Others on your network can join from 1v1", mid, textY + guiText.height(12*s) + 36*s, 11*s, grey, TextAlignment.CENTER);
                guiWaiting.draw(gc, gui, mid, waitY, 56*s);
                break;
            case JOINING:
                guiText.draw(gc, "Waiting for their game", mid, textY, 12*s, Color.BLACK, TextAlignment.CENTER);
                guiWaiting.draw(gc, gui, mid, waitY, 56*s);
                break;
            default:
                guiText.draw(gc, "Open challenges on your network", mid, challengesLabelY, 11*s, grey, TextAlignment.CENTER);
                if (shownChallenges.isEmpty()) {
                    double textTop = waitY - guiText.height(12*s)/2;
                    if (finder != null && !finder.isListening()) {
                        guiText.draw(gc, "Can not look for challenges here", mid, textTop, 12*s, Color.BLACK, TextAlignment.CENTER);
                    } else {
                        //Still looking: the waiting sign beside the words
                        double w = guiText.width("Looking", 12*s);
                        guiText.draw(gc, "Looking", mid + 16*s, textTop, 12*s, Color.BLACK, TextAlignment.CENTER);
                        guiWaiting.draw(gc, gui, mid - w/2 - 8*s, waitY, 28*s);
                    }
                }
                if (message != null) guiText.draw(gc, message, mid, messageY, 11*s, Color.rgb(160, 30, 30), TextAlignment.CENTER);
                guiText.draw(gc, "Builds (click one to use it)", mid, buildsLabelY, 11*s, grey, TextAlignment.CENTER);
                drawBest(gc);
        }

        for (guiElement element : elements()) element.draw(gc);
    }

    //The best streak box: "Highest streak: n", who the player was then, big, with their name, and the build it was won
    //with (rocks over trinkets). Equip loadout under it (in elements)
    private void drawBest (GraphicsContext gc) {
        double s = gui.s();
        double pad = gui.getSkin().getBorder() + 8*s;
        Color grey = Color.rgb(60, 60, 60);
        gui.getSkin().drawPanel(gc, bestX, bestY, bestW, bestH);
        double cx = bestX + bestW/2, top = bestY + pad;
        guiText.draw(gc, "Highest streak: " + library.bestStreak, cx, top, 16*s, library.bestStreak > 0 ? Color.web("#A07800") : Color.BLACK, TextAlignment.CENTER);
        top += guiText.height(16*s) + 8*s;
        double frame = bestPortrait + 10*s;
        gui.getSkin().drawSlot(gc, Math.round(cx - frame/2), Math.round(top), frame, library.selected == duelBuild.library.BEST && library.bestBuild != null);
        character look = library.bestBuild != null ? lookOf(library.bestCharacter) : null;
        if (look != null) {
            gc.drawImage(look.getIcon(), 0, 0, guiSkin.CELL, guiSkin.CELL, Math.round(cx - bestPortrait/2), Math.round(top + 5*s), Math.round(bestPortrait), Math.round(bestPortrait));
        }
        top += frame + 6*s;
        if (library.bestBuild == null) {
            guiText.draw(gc, "Win random battles in a row", cx, top, 11*s, grey, TextAlignment.CENTER);
            return;
        }
        String who = library.bestPlayer + (look != null ? " as " + look.getName() : "");
        double size = 12*s;
        while (size > 7 && guiText.width(who, size) > bestW - 2*pad) size -= 0.5;
        guiText.draw(gc, who, cx, top, size, Color.BLACK, TextAlignment.CENTER);

        //The build: the four rocks (the equipped one framed), the two trinkets under them
        duelBuild build = library.bestBuild;
        double rowW = duelBuild.ROCKS*(bestSlot + bestGap) - bestGap;
        drawSlots(gc, build, cx - rowW/2, bestSlotsY, bestSlot, bestGap, false);
        guiSkin skin = gui.getSkin();
        double trinketW = duelBuild.TRINKETS*(bestSlot + bestGap) - bestGap;
        for (int i = 0; i < duelBuild.TRINKETS; i++) {
            double tx = Math.round(cx - trinketW/2 + i*(bestSlot + bestGap)), ty = Math.round(bestSlotsY + bestSlot + bestGap);
            skin.drawSlot(gc, tx, ty, bestSlot, false);
            item in = shownItem(build, -1 - i);
            if (in != null) in.drawIcon(gc, tx + bestSlot*0.1, ty + bestSlot*0.1, bestSlot*0.8);
            else skin.drawSlotHint(gc, inventory.slotKind.TRINKET, tx, ty, bestSlot);
        }
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
        if (current == page.MAIN && randomBuildButton.contains(mx, my)) gui.setTooltip("A new random build every battle");
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
