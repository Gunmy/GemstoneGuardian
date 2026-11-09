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
    private double health = 100;
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
        if (equipped != null && !inventory.inRockSlot(equipped)) equipped = null;
        return this.equipped;
    }

    public void equipGemstone (gemstone equipped) {
        if (!inventory.inRockSlot(equipped)) throw new IllegalArgumentException(equipped.getName() + " ligger ikke i et rock slot");
        this.equipped = equipped;
    }

    public void deEquip () {
        this.equipped = null;
    }

    public String getName () {
        return this.name;
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
    }

    public double getHealth () {
        return this.health;
    }

    public double getHealthPercentage() {
        return getHealth()/getMaxHealth();
    }

    public void takeDmg (double dmg) {
        if (dmg == 0) return;
        else if (dmg < 0) {
            heal(-dmg);
            return;
        }
        this.health -= dmg;
        if (health < 0) {
            this.health = 0;
        }

        handler.getDiealoguecontainer().add(new dialogue("Game", getName() + " took " + dmg + " damage!\n" + getName() + " now has " + getHealth() + " health left")
            .highlight(dialogue.DAMAGE_BORDER, handler.getDiealoguecontainer().getSource()));
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

        handler.getDiealoguecontainer().add(new dialogue("Game", getName() + " healed " + (this.health-temp) + "!\n" + getName() + " now has " + getHealth() + " health left")
            .highlight(dialogue.DAMAGE_BORDER, handler.getDiealoguecontainer().getSource()));

    }

    public double getMaxHealth () {
        return 100 + getLevel() * 20;
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

    public void draw () {
        if (handler.getCurrentWorld() == world) {
            double time = (double) System.nanoTime();
            double division = 100000000;
            double heightOffset = Math.sin(time/division)*handler.getUnitHeight()/handler.getTileCount()*0.03;
            double widthOffset = Math.sin(time/division)*handler.getUnitWidth()/handler.getTileCount()*0.03;
    
    
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

            handler.getTempGc().drawImage(handler.getImages().getImage(0), 
            this.imgX, this.imgY, this.width, this.height, 
              renderx,   rendery, 
              (handler.getUnitWidth()/handler.getTileCount()-widthOffset), 
            (handler.getUnitHeight()/handler.getTileCount()+heightOffset));

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

    public void move (double deltaTime) {

        double tPS = speed; //tilespersecond
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
                them.takeDmg(getLevel()*2+5);
            }
        }
    }
    
}
