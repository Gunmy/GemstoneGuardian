package game;

import java.util.function.BiConsumer;

//A drink with an effect. All potions share the potion texture, each type has its own colour
public class potion extends item {

    public potion (String name, String color, itemParticles particles, BiConsumer<character, character> effect, gameHandler handler) {
        super(name, effect, true, handler);
        look(guiSkin.POTION_X, guiSkin.GUI_Y, color);
        particles(particles);
        stackable(16);
    }
}
