package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;


import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class character implements cordinate, entity, picture {

    private String name;
    private double x, y;
    private int imgX, imgY, height, width;
    private gameHandler handler;
    private double xTarget;
    private double yTarget;
    private int world;
    private double speed;
    //Walks this many times faster than its speed, for the speed cheat. Not saved
    private double speedMultiplier = 1;
    public double getSpeedMultiplier () {return this.speedMultiplier;}
    public void setSpeedMultiplier (double multiplier) {this.speedMultiplier = multiplier;}
    private double health = 100;
    //The health the player has been told about. Damage and heal messages move it when they are shown,
    //so health bars change on the message instead of when the move is done
    private double shownHealth = 100;
    private gemstone equipped;

    private double xp = 0;

    private inventory inventory = new inventory();

    private List<List<Integer>> queuedMovement = new ArrayList<List<Integer>>();

    //What the character can leave behind when beaten: each entry is rolled `rolls` times with `chance` each
    private List<drop> drops = new ArrayList<drop>();

    private static class drop {
        Supplier<item> item;
        double chance;
        int rolls;

        drop (Supplier<item> item, double chance, int rolls) {
            this.item = item;
            this.chance = chance;
            this.rolls = rolls;
        }
    }

    public void addDrop (Supplier<item> item, double chance, int rolls) {
        if (chance < 0 || chance > 1) throw new IllegalArgumentException("Chance må være mellom 0 og 1");
        if (rolls < 1) throw new IllegalArgumentException("Rolls kan ikke være under 1");
        drops.add(new drop(item, chance, rolls));
    }

    //Rolls every drop and returns the items won this time
    public List<item> rollDrops () {
        Random rdm = new Random();
        List<item> won = new ArrayList<item>();
        for (drop drop : drops) {
            for (int i = 0; i < drop.rolls; i++) {
                if (rdm.nextDouble() < drop.chance) won.add(drop.item.get());
            }
        }
        return won;
    }

    public character (String name, double x, double y, int imgX, int imgY, int height, int width, int world, gameHandler handler, double speed) {
        this.name = name;
        checkForNeg(x, y);
        this.x = x;
        this.y = y;
        this.imgX = imgX;
        this.imgY = imgY;
        this.height = height;
        this.width = width;
        this.handler = handler;

        this.xTarget = x;
        this.yTarget = y;
        this.world = world;
        this.speed = speed;

        setSpawn();
        this.homeWorld = world;
        this.homeX = x;
        this.homeY = y;
    }

    //Where the code places the character (never changes). Saved as "home", so a save made before the character was
    //moved in the code can tell (movedSince), and the character is not left behind at its old place
    private int homeWorld;
    private double homeX, homeY;
    public int getHomeWorld () {return homeWorld;}
    public double getHomeX () {return homeX;}
    public double getHomeY () {return homeY;}
    //Whether the last load put the character back at its home because the code had moved it (see movedSince)
    private boolean movedHome = false;
    public boolean wasMovedHome () {return movedHome;}

    //Whether the save's home for this character is somewhere else than where the code places it now. Saves from before
    //homes were saved compare the spawn instead: it is the code's place for everyone but the player (healing pads move
    //the player's), so the player is left out of that
    private boolean movedSince (saveFile save, String p) {
        String key = save.has(p + "home") ? p + "home" : this == handler.getMainCharacter() ? null : p + "spawn";
        if (key == null) return false;
        String[] place = save.getString(key, "").split(",");
        if (place.length != 3) return false;
        try {
            double half = 0.5/handler.getTileCount();
            return Integer.parseInt(place[0]) != homeWorld
                || Math.abs(Double.parseDouble(place[1]) - homeX) > half || Math.abs(Double.parseDouble(place[2]) - homeY) > half;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void checkForNeg (double x, double y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Kan ikke ha negative koordinater");
        }
    }

    public character (String name, double x, double y, int imgX, int imgY, int height, int width, int world, gameHandler handler, double speed, int lvl) {
        this(name, x, y, imgX, imgY, height, width, world, handler, speed);
        if (lvl < 0) throw new IllegalArgumentException("Lvl kan ikke være under 0");
        this.xp = LvlToxp(lvl);
        this.health = getMaxHealth();
    }

    public void kill () {
        health = 0;
    }

    private int worldSpawn;
    private double xSpawn;
    private double ySpawn;

    public void setSpawn () {
        this.worldSpawn = this.world;
        this.xSpawn = this.x;
        this.ySpawn = this.y;
    }

    //Moves straight to the tile at (x, y) in the given world and does what that tile does, like stepping onto it.
    //The camera jumps along instead of sliding all the way there
    public void teleport (int world, double x, double y) {
        queuedMovement.clear();
        setWorld(world);
        setX(x);
        setY(y);
        if (handler.getCamera().getBound() == this) {
            handler.getCamera().setX(x + 0.5/handler.getTileCount());
            handler.getCamera().setY(y + 0.5/handler.getTileCount());
        }
        standingTileDoFunction();
    }

    //Every teleport target (healing pad) stood on, in the order they were first stood on. The teleporter lists these
    private List<worldTile> visitedTeleports = new ArrayList<worldTile>();

    public void visitTeleport (tile target) {
        visitTeleportIn(this.world, target);
    }

    public List<worldTile> getVisitedTeleports () {return new ArrayList<worldTile>(visitedTeleports);}

    //True when an item with that name is in the inventory
    public boolean carries (String itemName) {
        return inventory.getItems().stream().anyMatch(i -> i.getName().equals(itemName));
    }

    public void goToSpawn () {
        setX(xSpawn);
        setY(ySpawn);
        setWorld(worldSpawn);
    }

    public void giveXp (double xp) {
        if (xp < 0) throw new IllegalArgumentException("xp kan ikke være under 0");
        String msg = getName() + " got " + xp + " xp!\n";

        if (getLevel() + 1 <= xpToLvl(this.xp+xp)) {
            msg += getName() + " leveled up to level " + xpToLvl(this.xp+xp) + "!";
        } else {
            msg += getName() + " need " + (LvlToxp(getLevel()+1)-(this.xp+xp)) + " more xp to level up!";
        }

        

        boolean leveledUp = getLevel() + 1 <= xpToLvl(this.xp+xp);
        this.xp += xp;

        dialogue message = new dialogue("Game", msg);
        if (leveledUp) message.highlight(dialogue.LEVEL_BORDER, picture.of(handler.getImages().getTinted(guiSkin.LEVEL_UP_X, guiSkin.ICON_Y, "#60E060")));
        handler.getDiealoguecontainer().add(message);
    }

    //Effects, kunne blitt gjort en del kortere
    //men ettersom det kun er 4 effekter, er det ikke
    //så mye poeng å bruke mye tid på det
    private double fireDamage;
    private int fireDamageTimes;
    private double poisonDamage;
    private int poisonDamageTimes;
    private double healing;
    private int healingTimes;
    private double confusion;
    private int confusionTimes;
    public void clearEffects () {
        fireDamage = 0;
        fireDamageTimes = 0;
        healing = 0;
        healingTimes = 0;
        poisonDamage = 0;
        poisonDamageTimes = 0;
        confusion = 0;
        confusionTimes = 0;
    }

    //What the inventory's info panel shows about the character when no item is hovered: level and xp, health,
    //the equipped rock and one box per effect on it
    public List<infoBox> getStatBoxes () {
        List<infoBox> boxes = new ArrayList<infoBox>();
        int lvl = getLevel();
        picture levelIcon = picture.of(handler.getImages().getTinted(guiSkin.LEVEL_UP_X, guiSkin.ICON_Y, "#60E060"));
        //The bar and count are the xp into this level, out of what this level takes
        double from = LvlToxp(lvl), to = LvlToxp(lvl+1);
        boxes.add(new infoBox(levelIcon, "Level " + lvl, Math.round(xp) + " xp in all", "")
            .bar((xp - from)/(to - from), Color.web("#60E060"), Math.round(xp - from) + " / " + Math.round(to - from) + " xp"));
        boxes.add(new infoBox(abilityType.HEAL, "Health", "", "")
            .bar(getHealth()/getMaxHealth(), Color.RED, Math.round(getHealth()) + " / " + Math.round(getMaxHealth())));
        gemstone stone = getGemstone();
        if (stone == null) {
            boxes.add(new infoBox(stone, "Equipped rock", "", "None. Right click a rock to equip it"));
        } else {
            //Each move with its uses left out of what it has when full
            //Only the icon: the stone itself (with its particles and lightning) is in its slot already
            picture still = (gc, g, x, y, size) -> stone.drawIcon(gc, x, y, size);
            infoBox rock = new infoBox(still, "Equipped rock", stone.getName(), "");
            List<String> names = Arrays.asList(stone.getAbility1name(), stone.getAbility2name(), stone.getAbility3name());
            List<List<Integer>> uses = Arrays.asList(stone.howManyAbility1Uses(), stone.howManyAbility2Uses(), stone.howManyAbility3Uses());
            for (int i = 0; i < 3; i++) rock.row(names.get(i), uses.get(i).get(0) + " / " + uses.get(i).get(1));
            boxes.add(rock);
        }
        boxes.addAll(getEffectBoxes());
        return boxes;
    }

    //One box per effect on the character right now: what it is (title, with its ability type as the picture), the turns
    //left (subtitle) and how strong it is (text). Also listed under the health bars in a fight
    //short: the wording for the one-line list under a health bar
    public List<infoBox> getEffectBoxes () {return getEffectBoxes(false);}

    public List<infoBox> getEffectBoxes (boolean short_) {
        List<infoBox> boxes = new ArrayList<infoBox>();
        String dmg = short_ ? " dmg/turn" : " damage per turn";
        if (fireDamageTimes > 0) boxes.add(new infoBox(abilityType.FIRE, "Burning", turns(fireDamageTimes), Math.round(fireDamage) + dmg));
        if (poisonDamageTimes > 0) boxes.add(new infoBox(abilityType.POISON, "Poisoned", turns(poisonDamageTimes), Math.round(poisonDamage) + dmg));
        if (healingTimes > 0) boxes.add(new infoBox(abilityType.HEAL, "Healing", turns(healingTimes), Math.round(healing) + (short_ ? " hp/turn" : " health per turn")));
        if (confusionTimes > 0) boxes.add(new infoBox(abilityType.CONFUSE, "Confused", turns(confusionTimes), Math.round(confusion*100) + (short_ ? "% less hit" : "% lower hit chance")));
        return boxes;
    }

    //The health the messages have shown so far (the health bar's number)
    public double getShownHealth () {
        getHealth();
        return this.shownHealth;
    }

    private static String turns (int n) {return n == 1 ? "1 turn left" : n + " turns left";}

    public void addConfusion (double confuse, int times) {
        if (confuse < 0) throw new IllegalArgumentException("Confuse kan ikke være under 0");
        if (times < 0) throw new IllegalArgumentException("Times kan ikke være under 0");

        if (confuse > confusion || (confuse == confusion && times > confusionTimes)) {
            handler.getDiealoguecontainer().add(new dialogue("Game", getName() + " is more confused"));
            confusion = confuse;
            confusionTimes = times;
        }
    }

    public void addFireDamage (double dmg, int times) {
        if (dmg < 0) throw new IllegalArgumentException("Firedmg kan ikke være under 0");
        if (times < 0) throw new IllegalArgumentException("Times kan ikke være under 0");

        if (dmg > fireDamage || (dmg == fireDamage && times > fireDamageTimes)) {
            fireDamage = dmg;
            fireDamageTimes = times;
        }
    }

    public void addPoisonDamage (double dmg, int times) {
        if (dmg < 0) throw new IllegalArgumentException("Poisondmg kan ikke være under 0");
        if (times < 0) throw new IllegalArgumentException("Times kan ikke være under 0");

        if (dmg > poisonDamage || (dmg == poisonDamage && times > poisonDamageTimes)) {
            poisonDamage = dmg;
            poisonDamageTimes = times;
        }
    }

    public void addHealing (double heal, int times) {
        if (heal < 0) throw new IllegalArgumentException("Heal kan ikke være under 0");
        if (times < 0) throw new IllegalArgumentException("Times kan ikke være under 0");

        if (heal > healing || (heal == healing && times > healingTimes)) {
            healing = heal;
            healingTimes = times;
        }
    }

    public void updateEffects () {

        String dialogue = "";
        double dmg = 0;

        if (fireDamageTimes > 0) {
            if ((new Random()).nextBoolean()) {
                fireDamageTimes-=1;
            }
            dialogue += getName() + " is burning (" + fireDamage + " dmg)\n";
            dmg += fireDamage;
        }

        if (poisonDamageTimes > 0) {
            poisonDamageTimes-=1;
            dialogue += getName() + " is being poisoned (" + poisonDamage + " dmg)\n";
            dmg += poisonDamage;
        }

        if (healingTimes > 0) {
            healingTimes-=1;
            dialogue += getName() + " is healing (" + healing + " health)\n";
            dmg -= healing;
        }

        if (dialogue != "") {
            abilityType cause = fireDamageTimes > 0 ? abilityType.FIRE : poisonDamageTimes > 0 ? abilityType.POISON : abilityType.HEAL;
            handler.getDiealoguecontainer().setSource(cause);
            handler.getDiealoguecontainer().add(new dialogue("Game", dialogue));
            takeDmg(dmg);
            handler.getDiealoguecontainer().clearSource();
        }

        if (confusionTimes > 0) {
            confusionTimes-=1;
        }
    }

    public double getConfusion () {
        if (confusionTimes > 0) {
            return confusion;
        } else {
            return 0;
        }
    }

    private int xpToLvl (double xp) {
        return (int) Math.floor(Math.cbrt(xp));
    }

    private double LvlToxp (int lvl) {
        return Math.pow(lvl, 3);
    }

    public int getLevel () {
        return xpToLvl(this.xp);
    }

    //Sets the xp to the start of a level (the cheat in hidden secrets). Health stays, but not above the new max
    public void setLevel (int lvl) {
        this.xp = LvlToxp(Math.max(0, lvl));
        this.health = Math.min(this.health, getMaxHealth());
        this.shownHealth = this.health;
    }

    public void resetHealth () {
        this.health = getMaxHealth();
    }

    public inventory getInventory () {
        return this.inventory;
    }

    //Uses up one of a consumable item
    public void consume (item item) {
        inventory.consume(item);
    }

    public void addMovementToQueue (int xChange, int yChange) {
        if (xChange < -1 || xChange > 1 || yChange < -1 || yChange > 1) throw new IllegalArgumentException("Endring kan ikke være større enn 1 eller mindre enn -1");
        queuedMovement.add(Arrays.asList(xChange, yChange));
    }

    //Only a rock in one of the active rock slots can be equipped. One that is moved out is unequipped
    public gemstone getGemstone () {
        //The equipped rock may have turned into another (magma into coal, and back), maybe more than once: the new one
        //stays equipped. Its slot is only swapped when the slot is drawn, so until then a slot holding an earlier form
        //of it counts too (in a fight the rock slots are not always on screen)
        if (equipped != null && equipped.latest() != equipped && equipped.latest() instanceof gemstone) equipped = (gemstone) equipped.latest();
        if (equipped != null && !inventory.inRockSlotAsLatest(equipped)) equipped = null;
        return this.equipped;
    }

    public void equipGemstone (gemstone equipped) {
        if (!inventory.inRockSlot(equipped)) throw new IllegalArgumentException(equipped.getName() + " ligger ikke i et rock slot");
        this.equipped = equipped;
    }

    public void deEquip () {
        this.equipped = null;
    }

    //The name shown in the game. The player and the girl can be renamed when a campaign starts
    public String getName () {
        return this.displayName != null ? this.displayName : this.name;
    }

    //The name the character was made with in the code. Never changes, so lookups and savefiles use it
    public String getId () {
        return this.name;
    }

    private String displayName;

    public void setDisplayName (String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) throw new IllegalArgumentException("Navnet kan ikke være tomt");
        this.displayName = displayName.trim();
    }

    public gameHandler getHandler () {return this.handler;}

    //The character's sprite as a picture, for dialogue boxes and health bars
    public Image getIcon () {
        return handler.getImages().getTinted(imgX, imgY, null);
    }

    //In a box the character bounces the same way it does in the world
    @Override
    public void draw (GraphicsContext gc, gui gui, double x, double y, double size) {
        double time = (double) System.nanoTime();
        double division = 100000000;
        double heightOffset = Math.sin(time/division)*size*0.03;
        double widthOffset = Math.sin(time/division)*size*0.03;
        gc.drawImage(getIcon(), 1, 1, guiSkin.CELL-2, guiSkin.CELL-2, x + widthOffset/2, y - heightOffset, size - widthOffset, size + heightOffset);
        drawWorn(gc, x + widthOffset/2, y - heightOffset, size - widthOffset, size + heightOffset, false);
    }

    public double getHealth () {
        //Taking off a trinket can drop the max below what the character has (and what its health bar shows)
        double max = getMaxHealth();
        if (health > max) health = max;
        if (shownHealth > max) shownHealth = max;
        return this.health;
    }

    public double getHealthPercentage() {
        return getHealth()/getMaxHealth();
    }

    public double getShownHealthPercentage() {
        return shownHealth/getMaxHealth();
    }

    public void syncShownHealth () {
        this.shownHealth = this.health;
    }

    public void takeDmg (double dmg) {
        if (dmg == 0) return;
        else if (dmg < 0) {
            heal(-dmg);
            return;
        }
        dmg *= hitMultiplier;
        getHealth();
        this.health -= dmg;
        if (health < 0) {
            this.health = 0;
        }

        double after = this.health;
        handler.getDiealoguecontainer().add(new dialogue("Game", getName() + " took " + dmg + " damage!\n" + getName() + " now has " + getHealth() + " health left")
            .highlight(dialogue.DAMAGE_BORDER, handler.getDiealoguecontainer().getSource())
            .onShow(() -> shownHealth = after));
    }

    public void heal (double dec) {
        if (dec == 0) return;
        if (dec < 0) {
            takeDmg(-dec);
            return;
        }
        double temp = this.health;
        this.health += dec;
        if (health > getMaxHealth()) {
            this.health = getMaxHealth();
        }

        double after = this.health;
        handler.getDiealoguecontainer().add(new dialogue("Game", getName() + " healed " + (this.health-temp) + "!\n" + getName() + " now has " + getHealth() + " health left")
            .highlight(dialogue.DAMAGE_BORDER, handler.getDiealoguecontainer().getSource())
            .onShow(() -> shownHealth = after));

    }

    public double getMaxHealth () {
        return (100 + getLevel() * 20)*wornProduct(trinket::getHealth);
    }

    //Trinkets ---------------------------------------------------------------------------------------------------
    //What the character wears (its inventory's trinket slots) multiplies its stats, and is drawn on it

    //One stat of every worn trinket multiplied together (1 with nothing worn)
    private double wornProduct (java.util.function.ToDoubleFunction<trinket> stat) {
        double product = 1;
        for (trinket t : inventory.getWorn()) product *= stat.applyAsDouble(t);
        return product;
    }

    //How many tiles further this character sees in the dark (goggles), all worn added up
    public int getSightBonus () {
        int tiles = 0;
        for (trinket t : inventory.getWorn()) tiles += t.getSight();
        return tiles;
    }

    //Whether something worn makes the character give off light
    public boolean isLit () {
        for (trinket t : inventory.getWorn()) if (t.doesLightWearer()) return true;
        return false;
    }

    //The light round a lit character in the world: a soft warm glow, brightest in the middle, behind the sprite
    public static final double LIGHT_RADIUS = 1.6;
    private void drawLight (GraphicsContext gc, double cx, double cy, double tile) {
        double r = tile*LIGHT_RADIUS;
        gc.setFill(new javafx.scene.paint.RadialGradient(0, 0, cx, cy, r, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.web("#FFF0B0", 0.45)), new javafx.scene.paint.Stop(1, Color.web("#FFF0B0", 0))));
        gc.fillOval(cx - r, cy - r, 2*r, 2*r);
    }

    //What the instant damage of this character's moves is multiplied by
    public double getDamageMultiplier () {return wornProduct(trinket::getDamage);}

    //While a move of attacker's runs, the instant damage this character takes is multiplied by the attacker's
    //damage multiplier. Burn and poison tick later, outside the move, so they are left as they are, and so is
    //what a move costs the attacker itself
    private double hitMultiplier = 1;
    public void hitBy (character attacker, Runnable move) {
        double before = hitMultiplier;
        hitMultiplier = attacker.getDamageMultiplier();
        try {
            move.run();
        } finally {
            hitMultiplier = before;
        }
    }

    //Puts a trinket on, in the first free trinket slot (for characters made in code). False when both are taken
    //A character at full health stays at full health (a heart raises the max)
    public boolean wear (trinket worn) {
        for (int i = 0; i < game.inventory.TRINKETS; i++) {
            if (inventory.get(game.inventory.slotKind.TRINKET, i) == null) {
                boolean full = getHealth() >= getMaxHealth();
                inventory.set(game.inventory.slotKind.TRINKET, i, new itemStack(worn));
                if (full) health = shownHealth = getMaxHealth();
                return true;
            }
        }
        return false;
    }

    //Where each place a trinket is worn sits on this character's sprite: the middle of the trinket, as a share of the
    //sprite's width and height from its top left. The defaults fit the people (big head, body under it); other
    //characters (sheep, crabs...) set their own with setAnchor
    private java.util.Map<trinket.place, double[]> anchors = new java.util.EnumMap<trinket.place, double[]>(trinket.place.class);
    {
        anchors.put(trinket.place.HEAD, new double[] {0.5, 0.12});
        anchors.put(trinket.place.CHEST, new double[] {0.58, 0.68});
        anchors.put(trinket.place.SHOULDER, new double[] {0.86, 0.5});
        anchors.put(trinket.place.EYES, new double[] {0.5, 0.45});
    }
    public character setAnchor (trinket.place place, double shareX, double shareY) {
        anchors.put(place, new double[] {shareX, shareY});
        return this;
    }

    //How big a worn trinket is, as a share of the sprite's width
    public static final double WORN_SIZE = 0.34;

    //Draws what the character wears over its sprite, drawn at (x, y) w by h (bobbing included, so they bob with it),
    //each with its glow behind it like in a slot (the feather's gold). In the world (inWorld) they also give off their
    //particles there, as world particles at the place they are worn, so they stay with the character
    private void drawWorn (GraphicsContext gc, double x, double y, double w, double h, boolean inWorld) {
        double size = w*WORN_SIZE;
        java.util.Random rdm = new java.util.Random();
        for (trinket t : inventory.getWorn()) {
            double[] at = anchors.get(t.getPlace());
            double own = size*t.getWornScale();
            double ix = x + at[0]*w - own/2, iy = y + at[1]*h - own/2;
            t.drawGlowBehind(gc, ix, iy, own);
            t.drawIcon(gc, ix, iy, own);
            if (inWorld) spawnWornParticles(t, at, rdm);
        }
    }

    //A worn trinket's particles, in the world: from where it sits on the sprite (the sprite is a tile, drawn half a
    //tile up), moving and sized as in a slot but scaled to the worn size of a tile
    private void spawnWornParticles (trinket t, double[] at, java.util.Random rdm) {
        itemParticles kind = t.getParticles();
        String[] colors = kind.getColors();
        if (colors.length == 0 || rdm.nextDouble() >= kind.getRate()) return;
        double tile = 1.0/handler.getTileCount();
        double scale = tile*WORN_SIZE*t.getWornScale()/48;
        double wx = x + at[0]*tile + (rdm.nextDouble() - 0.5)*tile*0.2;
        double wy = y - tile/2 + at[1]*tile + (rdm.nextDouble() - 0.5)*tile*0.2;
        double life = kind.getLife();
        double size = Math.max(0.004, kind.getSize()*scale);
        handler.addFrontParticle(new coloredParticle(wx, wy, world, life, 0, size, size, 0, handler,
            kind.getVx()*scale*life, kind.getVy()*scale*life, colors[rdm.nextInt(colors.length)]));
    }

    public void give (item recievedItem) {
        inventory.give(recievedItem);
        if (equipped == null && recievedItem instanceof gemstone) equipGemstone((gemstone) recievedItem);
    }

    @Override
    public double getX() {return x;}

    @Override
    public double getY() {return y;}

    //The sprite is drawn half a tile up, so the feet are in the middle of the tile
    public double getFeetY () {return y + 0.5/handler.getTileCount();}

    //The z-level the character is drawn on. 1 is on the ground, between the trees and flowers.
    //2 and up is above everything on the levels below, like a bird flying over the trees
    private int z = 1;
    public int getZ () {return this.z;}
    public void setZ (int z) {
        if (z < 1) throw new IllegalArgumentException("Z kan ikke være under 1");
        this.z = z;
    }

    //Characters with this on are drawn on top of the black of a screen transition (a teleport), so they
    //stay in sight while the world around them goes dark. Off for everyone but the player
    private boolean showInTransition = false;
    public character showInTransition (boolean show) {this.showInTransition = show; return this;}
    public boolean showsInTransition () {return this.showInTransition;}

    public void draw () {
        if (handler.getCurrentWorld() == world && !gone) {
            double time = (double) System.nanoTime();
            double division = 100000000;
            //Statues do not bob
            double bob = statue ? 0 : Math.sin(time/division);
            double heightOffset = bob*handler.getUnitHeight()/handler.getTileCount()*0.03;
            double widthOffset = bob*handler.getUnitWidth()/handler.getTileCount()*0.03;
    
    
            camera cam = handler.getCamera();
    
            double renderx = handler.getMidWIDTH() + widthOffset/2 + ((x-cam.getX()))*handler.getUnitWidth();
            double rendery = handler.getMidHEIGHT() - heightOffset + ((y-cam.getY()))*handler.getUnitHeight()-handler.getUnitHeight()/(handler.getTileCount()*2);
        
            if (handler.infoOn()) {
                handler.getTempGc().setFill(Color.WHITE);
                handler.getTempGc().fillRect(
                    handler.getMidWIDTH() + ((x-cam.getX()))*handler.getUnitWidth(), handler.getMidHEIGHT() + ((y-cam.getY()))*handler.getUnitHeight(), 
                    handler.getUnitWidth()/handler.getTileCount(), handler.getUnitHeight()/handler.getTileCount()
                );
            }

            if (isLit()) {
                double tileW = handler.getUnitWidth()/handler.getTileCount();
                drawLight(handler.getTempGc(), renderx + tileW/2, rendery + tileW/2, tileW);
            }
            if (statue) {
                //Its own picture in stone greys (a cell sized image, drawn from (0, 0)), and nothing worn on top
                handler.getTempGc().drawImage(handler.getImages().getTinted(imgX, imgY, STONE),
                    0, 0, this.width, this.height, renderx, rendery,
                    handler.getUnitWidth()/handler.getTileCount(), handler.getUnitHeight()/handler.getTileCount());
                return;
            }
            handler.getTempGc().drawImage(handler.getImages().getImage(0), 
            this.imgX, this.imgY, this.width, this.height, 
              renderx,   rendery, 
              (handler.getUnitWidth()/handler.getTileCount()-widthOffset), 
            (handler.getUnitHeight()/handler.getTileCount()+heightOffset));
            drawWorn(handler.getTempGc(), renderx, rendery,
                handler.getUnitWidth()/handler.getTileCount()-widthOffset, handler.getUnitHeight()/handler.getTileCount()+heightOffset, true);

            handler.getTempGc().setFill(Color.WHITE);
            handler.getTempGc().setFont(new Font("Silkscreen", 15));
            handler.getTempGc().setTextAlign(TextAlignment.CENTER);
            if (getLevel() == 0) {
                handler.getTempGc().fillText(getName(),  handler.getMidWIDTH()+ ((x+0.055-cam.getX()))*handler.getUnitWidth(), 
                handler.getMidHEIGHT() + ((y-0.05-cam.getY()))*handler.getUnitHeight());
            } else {
                handler.getTempGc().fillText(getName() + " (Lvl " + getLevel() + ")",  handler.getMidWIDTH()+ ((x+0.055-cam.getX()))*handler.getUnitWidth(), 
                handler.getMidHEIGHT() + ((y-0.05-cam.getY()))*handler.getUnitHeight());
            }
        }
    }

    public boolean isDead() {return false;}

    private boolean targetSameAsCoords () {
        int tileCount = handler.getTileCount();
        return x == xTarget && y*tileCount == yTarget*tileCount;
    }

    //Both the tile you stand on has to let you out, and the tile you go to has to let you in
    private boolean canStep (double xChange, double yChange) {
        if (ghost) return true;
        return !getStandingTile().isBlockedOut(xChange, yChange)
        && !getStandingTile(xChange/handler.getTileCount(), yChange/handler.getTileCount()).isBlocked(xChange, yChange);
    }

    public boolean setTarget (double xChange, double yChange) {
        if (targetSameAsCoords()) {

            if (xChange != 0 && canStep(xChange, 0)) { //
                if (this.x + xChange/handler.getTileCount() < 0) throw new IllegalStateException("Koordinat kan ikke være under 0");
                xTarget = this.x + xChange/handler.getTileCount();
                return true;
            } else if (yChange != 0 && canStep(0, yChange)) { //
                if (this.y + yChange/handler.getTileCount() < 0) throw new IllegalStateException("Koordinat kan ikke være under 0");
                yTarget = this.y + yChange/handler.getTileCount();
                return true;
            }
        }
        return false;
    }

    public int getWorld() {return this.world;}

    //True while walking from one tile to the next
    public boolean isMoving () {return !targetSameAsCoords();}
    //Gone from the world (a beaten villain): not drawn and not in anyone's way, but still there for savefiles
    private boolean gone = false;
    public boolean isGone () {return this.gone;}
    public void setGone (boolean gone) {this.gone = gone;}

    //Turned to stone (the crystal boy's beaten servants): drawn in greys, standing perfectly still, no name over it
    public static final String STONE = "#C8C8C8";
    private boolean statue = false;
    public boolean isStatue () {return this.statue;}
    public void setStatue (boolean statue) {this.statue = statue;}

    //Following another character (the girl, freed, follows the player): it walks onto the tile the leader has just
    //left, starting when the leader starts its step and at the leader's speed (getTilesPerSecond), so it is always one
    //tile behind, and when the leader is far away or in another world (a teleport, a door) it is put right where the leader
    //is. A follower is in no one's way (tile.isBlocked). Saved as the leader's name, put back by gameHandler.afterLoad
    private character leader;
    private int leaderWorld;
    private long leaderTileX, leaderTileY;
    public character getLeader () {return this.leader;}
    public void follow (character leader) {
        this.leader = leader;
        if (leader == null) return;
        leaderWorld = leader.world;
        leaderTileX = Math.round(leader.xTarget*handler.getTileCount());
        leaderTileY = Math.round(leader.yTarget*handler.getTileCount());
    }
    private String savedLeader;
    public String getSavedLeader () {return this.savedLeader;}

    //Called every frame by gameHandler
    public void followStep () {
        if (leader == null) return;
        int tC = handler.getTileCount();
        //The tile the leader is on or stepping onto: the follower starts its step the moment the leader starts its own,
        //so it keeps exactly one tile behind
        long lx = Math.round(leader.xTarget*tC), ly = Math.round(leader.yTarget*tC);
        long fx = Math.round(x*tC), fy = Math.round(y*tC);
        boolean far = leader.world != world || Math.abs(lx - fx) + Math.abs(ly - fy) > 3;
        if (far) {
            placeAt(leader.world, leader.x, leader.y);
        } else if ((lx != leaderTileX || ly != leaderTileY) && leader.world == leaderWorld) {
            //The leader stepped off (leaderTileX, leaderTileY): walk onto it
            long dx = leaderTileX - fx, dy = leaderTileY - fy;
            if (Math.abs(dx) + Math.abs(dy) == 1) {
                setGhost(true);
                addMovementToQueue((int) dx, (int) dy);
            } else if (dx != 0 || dy != 0) {
                placeAt(leader.world, leaderTileX/(double) tC, leaderTileY/(double) tC);
            }
        }
        if (isStill()) setGhost(false);
        leaderWorld = leader.world;
        leaderTileX = lx;
        leaderTileY = ly;
    }

    //Walks through anything (walls, lava, people) while set: a cutscene's walkThrough, like stepping out of a mirror
    private boolean ghost = false;
    public void setGhost (boolean ghost) {this.ghost = ghost;}

    //How many tiles a second it walks: its own speed, the speed cheat and what it wears (the Feather of Hermes Boots).
    //A follower walks exactly as fast as its leader, so it keeps one tile behind
    public double getTilesPerSecond () {
        if (leader != null) return leader.getTilesPerSecond();
        return speed*speedMultiplier*wornProduct(trinket::getSpeed);
    }

    //Changes what it looks like to another cell of the sheet (the player becomes the crystal boy). Saved
    public void setSprite (int imgX, int imgY) {
        this.imgX = imgX;
        this.imgY = imgY;
    }
    public boolean looksLike (int imgX, int imgY) {return this.imgX == imgX && this.imgY == imgY;}

    //Standing on a tile with no steps left to take
    public boolean isStill () {return !isMoving() && queuedMovement.isEmpty();}

    public void move (double deltaTime) {

        double tPS = getTilesPerSecond();
        double speed = deltaTime*tPS/handler.getTileCount();
        if (Math.abs(xTarget-x) <= speed) {
            x = Math.round(xTarget*handler.getTileCount())/(double)handler.getTileCount();
            xTarget = x;
            standingTileDoFunction();
        } else {
            x += Math.signum(xTarget-x)*speed;
        }
        
        if (Math.abs(yTarget-y) <= speed) {
            y = Math.round(yTarget*handler.getTileCount())/(double)handler.getTileCount();
            yTarget = y;
            standingTileDoFunction();
        } else {
            y += Math.signum(yTarget-y)*speed;
        }    
        if (queuedMovement.size() > 0 && targetSameAsCoords()) {
            List<Integer> coords = queuedMovement.get(0);
            if (setTarget(coords.get(0), coords.get(1))) {
                queuedMovement.remove(0);

            }
        }
    }

    public tileType getStandingTileType () {
        return handler.getWorld(this.world).getTileType(x, y);
    }

    public tile getStandingTile () {
        return handler.getWorld(this.world).getTile(x, y);
    }

    public tile getTargetTile () {
        return handler.getWorld(this.world).getTile(xTarget, yTarget);
    }

    public tile getStandingTile (double xChange, double yChange) {
        return handler.getWorld(this.world).getTile(x+xChange, y+yChange);
    }

    public tileType getStandingTileType (double xChange, double yChange) {
        return handler.getWorld(this.world).getTileType(x+xChange, y+yChange);
    }

    public void standingTileDoFunction () {
        getStandingTileType().doFunction(this);
    }

    public void setWorld (int world) {
        if (handler.getWorld(world) == null) throw new IllegalArgumentException("Verden finnes ikke");
        this.world = world;}
    private void setX (double x) {
        if (x < 0) throw new IllegalArgumentException("Koordinat kan ikke være negativ");
        this.x = x; this.xTarget = x;
    }
    private void setY (double y) {
        if (y < 0) throw new IllegalArgumentException("Koordinat kan ikke være negativ");
        this.y = y; this.yTarget = y;
    }

    //Puts the character straight at (x, y) in the given world, standing still, without doing what the tile does
    public void placeAt (int world, double x, double y) {
        queuedMovement.clear();
        setWorld(world);
        setX(x);
        setY(y);
    }

    public void fullHeal () {
        heal(getMaxHealth()-this.health);
    }

    public void restoreItems () {
        for (item item : inventory.getItems()) {
            item.restore();
        }
    }

    public void restore () {
        if (getHealth() < getMaxHealth()) {fullHeal();}
        clearEffects();
        restoreItems();
    }

    //Everything about the character that changes while playing, under prefix. What it looks like, its speed
    //and its drops come from the code that made it, so they are not saved
    public void saveTo (saveFile save, String p) {
        save.set(p + "x", x);
        save.set(p + "y", y);
        save.set(p + "xTarget", xTarget);
        save.set(p + "yTarget", yTarget);
        save.set(p + "world", world);
        save.set(p + "z", z);
        save.set(p + "health", health);
        save.set(p + "xp", xp);
        save.set(p + "spawn", worldSpawn + "," + xSpawn + "," + ySpawn);
        save.set(p + "home", homeWorld + "," + homeX + "," + homeY);
        save.set(p + "gone", gone);
        save.set(p + "statue", statue);
        save.set(p + "sprite", imgX + "," + imgY);
        save.set(p + "follows", leader == null ? "" : leader.getId());
        save.set(p + "effects", fireDamage + "," + fireDamageTimes + "," + poisonDamage + "," + poisonDamageTimes + ","
            + healing + "," + healingTimes + "," + confusion + "," + confusionTimes);

        List<String> queue = new ArrayList<String>();
        for (List<Integer> move : queuedMovement) queue.add(move.get(0) + "," + move.get(1));
        save.setList(p + "queue", queue, ";");

        List<String> visited = new ArrayList<String>();
        for (worldTile t : visitedTeleports) visited.add(t.getWorld() + "," + t.getX() + "," + t.getY());
        save.setList(p + "visited", visited, ";");

        int equippedSlot = -1;
        for (int i = 0; i < game.inventory.ACTIVE; i++) {
            itemStack stack = inventory.get(game.inventory.slotKind.ROCK, i);
            if (stack != null && stack.getItem().latest() == getGemstone()) equippedSlot = i;
        }
        save.set(p + "equipped", equippedSlot);
        inventory.saveTo(save, p + "inv.");
    }

    //Puts the character back the way saveTo found it. Anything missing in the save keeps what the code gave it
    public void loadFrom (saveFile save, String p, itemRegistry registry) {
        if (!save.has(p + "x")) return;
        int savedWorld = save.getInt(p + "world", world);
        //Saved in a world that has been taken out since: back to the spawn (below), or where the code put it
        boolean lost = handler.getWorld(savedWorld) == null;
        int codeWorld = world;
        double codeX = x, codeY = y;
        if (!lost) world = savedWorld;
        x = Math.max(0, save.getDouble(p + "x", x));
        y = Math.max(0, save.getDouble(p + "y", y));
        xTarget = Math.max(0, save.getDouble(p + "xTarget", x));
        yTarget = Math.max(0, save.getDouble(p + "yTarget", y));
        z = Math.max(1, save.getInt(p + "z", z));
        gone = save.getBool(p + "gone", gone);
        statue = save.getBool(p + "statue", statue);
        String[] sprite = save.getString(p + "sprite", "").split(",");
        if (sprite.length == 2) {
            try {
                imgX = Integer.parseInt(sprite[0]);
                imgY = Integer.parseInt(sprite[1]);
            } catch (NumberFormatException e) {
                System.out.println("Ugyldig utseende for " + name);
            }
        }
        savedLeader = save.getString(p + "follows", "");
        xp = Math.max(0, save.getDouble(p + "xp", xp));
        health = Math.max(0, Math.min(getMaxHealth(), save.getDouble(p + "health", health)));
        shownHealth = health;

        String[] spawn = save.getString(p + "spawn", "").split(",");
        if (spawn.length == 3) {
            try {
                int w = Integer.parseInt(spawn[0]);
                if (handler.getWorld(w) != null) {
                    worldSpawn = w;
                    xSpawn = Double.parseDouble(spawn[1]);
                    ySpawn = Double.parseDouble(spawn[2]);
                }
            } catch (NumberFormatException e) {
                System.out.println("Ugyldig spawn for " + name);
            }
        }
        if (lost) {
            boolean spawnHere = handler.getWorld(worldSpawn) != null;
            world = spawnHere ? worldSpawn : codeWorld;
            x = xTarget = spawnHere ? xSpawn : codeX;
            y = yTarget = spawnHere ? ySpawn : codeY;
        }
        //Moved in the code since the save was made: it stands where the code puts it now, not where it stood then
        boolean moved = movedSince(save, p);
        movedHome = moved;
        if (moved) {
            world = worldSpawn = homeWorld;
            x = xTarget = xSpawn = homeX;
            y = yTarget = ySpawn = homeY;
        }

        String[] effects = save.getString(p + "effects", "").split(",");
        if (effects.length == 8) {
            try {
                fireDamage = Double.parseDouble(effects[0]);
                fireDamageTimes = Integer.parseInt(effects[1]);
                poisonDamage = Double.parseDouble(effects[2]);
                poisonDamageTimes = Integer.parseInt(effects[3]);
                healing = Double.parseDouble(effects[4]);
                healingTimes = Integer.parseInt(effects[5]);
                confusion = Double.parseDouble(effects[6]);
                confusionTimes = Integer.parseInt(effects[7]);
            } catch (NumberFormatException e) {
                clearEffects();
            }
        }

        queuedMovement.clear();
        if (!moved) for (String move : save.getList(p + "queue", ";")) {
            String[] xy = move.split(",");
            try {
                addMovementToQueue(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
            } catch (RuntimeException e) {
                System.out.println("Ugyldig bevegelse for " + name + ": " + move);
            }
        }

        visitedTeleports.clear();
        for (String visited : save.getList(p + "visited", ";")) {
            String[] wxy = visited.split(",");
            try {
                int w = Integer.parseInt(wxy[0]);
                tile t = handler.getWorld(w).getTile(Double.parseDouble(wxy[1]), Double.parseDouble(wxy[2]));
                //Pads taken off the map since are forgotten
                if (t.getTileType().isTeleportTarget()) visitTeleportIn(w, t);
            } catch (RuntimeException e) {
                System.out.println("Ugyldig teleport for " + name + ": " + visited);
            }
        }

        inventory.loadFrom(save, p + "inv.", registry);
        equipped = null;
        int equippedSlot = save.getInt(p + "equipped", -1);
        if (equippedSlot >= 0 && equippedSlot < game.inventory.ACTIVE) {
            itemStack stack = inventory.get(game.inventory.slotKind.ROCK, equippedSlot);
            if (stack != null) equipped = (gemstone) stack.getItem();
        }
    }

    private void visitTeleportIn (int world, tile target) {
        worldTile visited = new worldTile(world, target);
        if (!visitedTeleports.contains(visited)) visitedTeleports.add(visited);
    }

    public void autoMaticMove (character them) {
        if (them == this) throw new IllegalArgumentException("Kan ikke angripe seg selv");
        if (getGemstone() != null) {
            if (getGemstone().hasMovesLeft()) {
                Random rdm = new Random();

                List<Integer> weights = new ArrayList<Integer>();
                if (getGemstone().ability1usesLeft()) {weights.add(1);}
                if (getGemstone().ability2usesLeft()) {weights.add(2);}
                if (getGemstone().ability3usesLeft()) {weights.add(3);}

                int moveNr = weights.get(rdm.nextInt(weights.size()));
                if (moveNr == 1) {getGemstone().ability1(this, them);}
                else if (moveNr == 2) {getGemstone().ability2(this, them);}
                else if (moveNr == 3) {getGemstone().ability3(this, them);}
            } else {
                getGemstone().use(this, them);
                
            }
        } else {
            for (item item : getInventory().getItems()) {
                if (item instanceof gemstone && ((gemstone) item).hasMovesLeft()) {
                    item.use(this, them);
                }
            }
            if (getGemstone() == null) {
                handler.getDiealoguecontainer().add(new dialogue(this, "Hit " + them.getName()));
                them.hitBy(this, () -> them.takeDmg(getLevel()*2+5));
            }
        }
    }
    
}
