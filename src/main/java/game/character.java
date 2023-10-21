package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;

public class character implements cordinate, entity {

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

    private List<item> inventory = new ArrayList<item>();

    private List<List<Integer>> queuedMovement = new ArrayList<List<Integer>>();
    private Stack<Pair<Double, Double>> pathMovemet;

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

        

        this.xp += xp;

        handler.getDiealoguecontainer().add(new dialogue("Game", msg));
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
            handler.getDiealoguecontainer().add(new dialogue("Game", dialogue));
            takeDmg(dmg);
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

    public List<item> getInventory () {
        return inventory.stream().collect(Collectors.toList());
    }

    public gemstone getGemstone () {
        return this.equipped;
    }

    public void equipGemstone (gemstone equipped) {
        if (!inventory.contains(equipped)) throw new IllegalArgumentException(equipped + " er ikke i inventory: " + inventory);
        this.equipped = equipped;
    }

    public void deEquip () {
        this.equipped = null;
    }

    public String getName () {
        return this.name;
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

        handler.getDiealoguecontainer().add(new dialogue("Game", getName() + " took " + dmg + " damage!\n" + getName() + " now has " + getHealth() + " health left"));
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

        handler.getDiealoguecontainer().add(new dialogue("Game", getName() + " healed " + (this.health-temp) + "!\n" + getName() + " now has " + getHealth() + " health left"));

    }

    public double getMaxHealth () {
        return 100 + getLevel() * 20;
    }

    public void give (item recievedItem) {
        inventory.add(recievedItem);
        if (equipped == null && recievedItem instanceof gemstone) equipGemstone((gemstone) recievedItem);
    }

    @Override
    public double getX() {return x;}

    @Override
    public double getY() {return y;}

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

    
    public Boolean pathFindToCoord(double x, double y) {
        pathMovemet = calcPath(x, y, this.world);
        return pathMovemet != null;
    }

    private Stack<Pair<Double, Double>> calcPath(double x, double y, int world) {
        //Kan kun søke etter en firkant i verdenen vi er i
        if (world != getWorld()) return null;

        //Maks antall sjekker
        int maxCheck = 10000;
        int currentCheck = 0;
        //Radius
        double maxDistance = 13;

        HashMap<Pair<Double, Double>, Pair<Double, Double>> visited = new HashMap<>();
        Queue<Pair<Double, Double>> queue = new LinkedList<>();

        double step = 1.0/handler.getTileCount();
        List<Double> xMove = Arrays.asList(step, 0.0, -step, 0.0);
        List<Double> yMove = Arrays.asList(0.0, -step, 0.0, step);

        //Start
        Pair<Double, Double> start = new Pair<>(this.xTarget, this.yTarget);
        queue.offer(start);
        visited.put(start, null);
    
        while (!queue.isEmpty() && currentCheck < maxCheck) {
            Pair<Double, Double> pos = queue.poll();
            currentCheck++;

            if (handler.getWorld(getWorld()).getTile(pos.getKey(), pos.getValue()).equals(handler.getWorld(getWorld()).getTile(x, y))) {
                //Lagre veien        
                Stack<Pair<Double, Double>> path = new Stack<>();

                //Legger til slutten
                path.push(new Pair<>(x, y));

                while (visited.get(pos) != null) {
                    pos = visited.get(pos);
                    path.push(pos);
                }
                
                return path;
            }

            for (int i = 0; i < 4; i++) {
                Pair<Double, Double> checkPos = new Pair<>(pos.getKey()+xMove.get(i), pos.getValue()+yMove.get(i));

                if (!handler.getWorld(getWorld()).getTile(checkPos.getKey(), checkPos.getValue()).isBlocked() 
                && !visited.keySet().contains(checkPos)
                && (Math.sqrt(Math.pow(checkPos.getKey()-x, 2)+Math.pow(checkPos.getValue()-y, 2))) < maxDistance) {
                    visited.put(checkPos, pos);
                    queue.offer(checkPos);
                }
            }
        }
        return null;
    }

    public void addMovementToQueue (int xChange, int yChange) {
        if (xChange < -1 || xChange > 1 || yChange < -1 || yChange > 1) throw new IllegalArgumentException("Endring kan ikke være større enn 1 eller mindre enn -1");
        queuedMovement.add(Arrays.asList(xChange, yChange));
    }

    public boolean setTarget (double xChange, double yChange) {
        if (targetSameAsCoords()) {
            
            if (xChange != 0 && !getStandingTile(xChange/handler.getTileCount(), 0).isBlocked()) { //  
                if (this.x + xChange/handler.getTileCount() < 0) throw new IllegalStateException("Koordinat kan ikke være under 0");
                xTarget = this.x + xChange/handler.getTileCount();
                return true;
            } else if (yChange != 0 && !getStandingTile(0, yChange/handler.getTileCount()).isBlocked()) { // 
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

        if (pathMovemet != null && pathMovemet.size() > 0 && targetSameAsCoords()) {
            Pair<Double, Double> xyTarget = pathMovemet.pop();
            xTarget = xyTarget.getKey();
            yTarget = xyTarget.getValue();
        }

        //For backwards compatability
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
        for (item item : inventory) {
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
            for (item item : getInventory()) {
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
