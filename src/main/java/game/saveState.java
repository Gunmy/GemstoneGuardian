package game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Moves a game's progress into a saveFile and back. A loaded game is first built fresh from the campaign
//(gameHandler's constructor, chunks.txt) and then has the save laid over it, so anything the save does not
//mention (a character or event added to the campaign since) simply starts the way the campaign has it.
//
//What is saved:
//- every character by name (see character.saveTo), the player included
//- which campaign event listeners have gone off, by their key; and the fights that were lost and wait to be retried
//- the fight going on, if any: who against and whose turn, started again from the enemy's battleEvent
//- the dialogue on screen and queued, play time and whether the game was finished
//- the names picked for the player and the girl (read by gameHandler itself, since a new campaign has them too)
//- the stack held on the mouse in the inventory, which is in no slot; it goes back into the inventory when loaded
//- what is in every chest, and whether it has been closed once (its first-close moment has happened)
public class saveState {

    private static final String SEPARATOR = "|";

    public static void write (gameHandler handler, saveFile save) {
        save.clearState();
        save.set("playTime", handler.getPlayTime());
        save.set("finished", handler.isFinished());
        handler.writeNames(save);

        character player = handler.getMainCharacter();
        save.set("player.world", player.getWorld());
        save.set("player.x", player.getX());
        save.set("player.y", player.getY());
        save.set("player.level", player.getLevel());

        //Characters by number, with the name inside, so names never have to be written as keys
        List<character> characters = handler.getCharacters();
        save.set("characters", characters.size());
        for (int i = 0; i < characters.size(); i++) {
            save.set("char." + i + ".name", characters.get(i).getId());
            characters.get(i).saveTo(save, "char." + i + ".");
        }

        Set<eventListener> active = new HashSet<eventListener>(handler.getActiveListeners());
        List<String> fired = new ArrayList<String>();
        for (eventListener l : handler.getCampaignListeners()) {
            if (!active.contains(l)) fired.add(l.getKey());
        }
        save.setList("listeners.fired", fired, SEPARATOR);

        Set<eventListener> campaign = new HashSet<eventListener>(handler.getCampaignListeners());
        List<String> retries = new ArrayList<String>();
        for (eventListener l : handler.getActiveListeners()) {
            if (!campaign.contains(l) && l.getEvent() instanceof battleEvent) retries.add(((battleEvent) l.getEvent()).getThem().getId());
        }
        save.setList("listeners.retry", retries, SEPARATOR);

        battle fight = handler.getBattle();
        if (fight.isActive()) {
            save.set("battle.enemy", fight.getThem().getId());
            save.set("battle.playersTurn", fight.isPlayersTurn());
        }

        itemStack held = handler.getGui() == null ? null : handler.getGui().getCursor();
        if (held != null) {
            save.set("cursor.item", held.getItem().getName());
            save.set("cursor.count", held.getCount());
            save.set("cursor.state", held.getItem().saveState());
            save.set("cursor.heat", held.getItem().getSavedHeat());
        }

        List<chest> chests = handler.getChests();
        save.set("chests", chests.size());
        for (int i = 0; i < chests.size(); i++) {
            chest c = chests.get(i);
            String p = "chest." + i + ".";
            save.set(p + "at", c.getPlace().getWorld() + "," + c.getPlace().getX() + "," + c.getPlace().getY());
            save.set(p + "closed", c.hasBeenClosed());
            for (int slot = 0; slot < c.size(); slot++) {
                itemStack stack = c.get(slot);
                if (stack == null) continue;
                String k = p + "slot." + slot;
                save.set(k + ".item", stack.getItem().getName());
                save.set(k + ".count", stack.getCount());
                save.set(k + ".state", stack.getItem().saveState());
                save.set(k + ".heat", stack.getItem().getSavedHeat());
            }
        }

        handler.getDiealoguecontainer().saveTo(save);
        save.write();
    }

    public static void apply (gameHandler handler, saveFile save) {
        if (!save.hasPlayer()) return;
        handler.setPlayTime(save.getDouble("playTime", 0));
        handler.setFinished(save.getBool("finished", false));

        itemRegistry registry = handler.getItemRegistry();
        int count = save.getInt("characters", 0);
        for (int i = 0; i < count; i++) {
            String name = save.getString("char." + i + ".name", "");
            character c = handler.findCharacter(name);
            if (c == null) {
                System.out.println("Karakteren " + name + " finnes ikke lenger, hoppet over");
                continue;
            }
            c.loadFrom(save, "char." + i + ".", registry);
        }

        Set<String> fired = new HashSet<String>(save.getList("listeners.fired", SEPARATOR));
        List<eventListener> active = new ArrayList<eventListener>();
        for (eventListener l : handler.getCampaignListeners()) {
            if (!fired.contains(l.getKey())) active.add(l);
        }
        for (String enemy : save.getList("listeners.retry", SEPARATOR)) {
            battleEvent fight = findFight(handler, enemy);
            if (fight != null) active.add(fight.rearm(handler));
        }
        handler.setActiveListeners(active);
        handler.afterLoad();

        //What was held on the mouse goes back into the player's inventory
        if (save.has("cursor.item")) {
            int heldCount = Math.max(1, save.getInt("cursor.count", 1));
            for (int i = 0; i < heldCount; i++) {
                item held = registry.make(save.getString("cursor.item", ""));
                if (held == null) break;
                held.loadState(save.getString("cursor.state", ""));
                held.restoreHeat(save.getDouble("cursor.heat", -1));
                handler.getMainCharacter().give(held);
            }
        }

        if (save.has("battle.enemy")) {
            battleEvent fight = findFight(handler, save.getString("battle.enemy", ""));
            if (fight != null) {
                fight.trigger();
                handler.getBattle().resumeTurn(save.getBool("battle.playersTurn", true));
            }
        }

        //Chests as they were left. Saves from before chests were saved keep the chests as the campaign fills them
        int chestCount = save.getInt("chests", 0);
        for (int i = 0; i < chestCount; i++) {
            String p = "chest." + i + ".";
            String[] at = save.getString(p + "at", "").split(",");
            if (at.length != 3) continue;
            chest c;
            try {
                c = handler.chestAt(Integer.parseInt(at[0]), Double.parseDouble(at[1]), Double.parseDouble(at[2]));
            } catch (RuntimeException e) {
                continue;
            }
            if (c == null) {
                System.out.println("Kisten ved " + save.getString(p + "at", "") + " finnes ikke lenger");
                continue;
            }
            c.setBeenClosed(save.getBool(p + "closed", false));
            for (int slot = 0; slot < c.size(); slot++) {
                String k = p + "slot." + slot;
                item made = save.has(k + ".item") ? registry.make(save.getString(k + ".item", "")) : null;
                if (made == null) {
                    c.set(slot, null);
                    continue;
                }
                made.loadState(save.getString(k + ".state", ""));
                made.restoreHeat(save.getDouble(k + ".heat", -1));
                c.set(slot, new itemStack(made, Math.max(1, Math.min(made.getMaxStack(), save.getInt(k + ".count", 1)))));
            }
        }

        handler.getDiealoguecontainer().loadFrom(save, handler.getCharacterContainer(), registry);
        handler.jumpToPlayer();
    }

    //The campaign's fight against the character with this name
    private static battleEvent findFight (gameHandler handler, String enemy) {
        for (eventListener l : handler.getCampaignListeners()) {
            if (l.getEvent() instanceof battleEvent && ((battleEvent) l.getEvent()).getThem().getId().equals(enemy)) return (battleEvent) l.getEvent();
        }
        System.out.println("Fant ingen kamp mot " + enemy);
        return null;
    }
}
