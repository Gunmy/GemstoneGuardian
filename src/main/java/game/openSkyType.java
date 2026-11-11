package game;

import java.util.function.Consumer;

//Open sky in a sky world: blocked, and drawn as nothing, so the sky behind the world (skyLayer, its blue and drifting
//clouds) shows through. Its cell is only used for the small maps (teleports menu)
public class openSkyType extends tileType {

    public openSkyType (String name, int x, int y, int width, int height, int id, boolean block, Consumer<character> pressurePlate) {
        super(name, x, y, width, height, id, block, pressurePlate);
    }

    @Override
    protected void drawSpecial (gameHandler handler, double realx, double realy, int x, int y) {}
}
