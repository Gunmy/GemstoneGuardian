package game;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

//The world seen from space. Zooming out over a world made with chunks.space() the screen fades to cloud white
//(gameHandler.coverAmount, planetView); from there the map is no
//longer drawn at all, and what you zoom out of is this: the world as a planet, a square in space, made in code and not
//from the map, as big as the map's square (chunks.getSquare). It has its own zoom (planetAcross): at the switch the
//screen shows a tiny bit of it, inside the cloud over the player, so the cloud fills the screen there too, and zooming
//out pulls back quickly to the whole planet. The player is the middle of the planet: it is always centred on the screen, and there is always a cloud over
//its middle (drawLane): along a band through the middle, slow and fast clouds of all sizes take turns over it, each
//arriving just as the one before leaves, so zooming out you come out of clouds that really are on the planet. Away
//from the middle they are spread out with gaps between them, like the rest. Three more bands like it cross the planet.
//The planet is pixel art made once per world (makePlanet: seas, beaches, land, forests, mountains with snow; cold
//blues in the mirror dimension, cloud islands in the open sky for the sky ruins), with its own clouds drifting over
//it, a soft glow of air round its edge, the moon going round it and the sun far off (huge moonstone and sunstone,
//the gemstones' own texture), a black hole (a huge singularity with moonstones and meteorites going round it), meteorites (the
//meteorite gemstone) flying past, in the teleport's starry dark
//(screenTransition.drawStars). All in the art's colours
public class spaceView {

    //Space is the teleport's starry dark (screenTransition): black, with its stars fixed to the screen
    public static final Color SPACE = Color.BLACK;
    private static final Color AIR = Color.web("#8EDCF2");
    //The moon: how far out it circles (times the planet's side, from its middle), how big it is (its radius, times
    //the side), how many seconds one round takes
    public static final double MOON_ORBIT = 1.05, MOON_RADIUS = 0.17, MOON_SECONDS = 300;
    //The sun: how far away it is (times the planet's side, from the planet's middle, towards the top left, where the
    //planet and the moon are lit from), how big (its radius, times the side), and its glow (times its radius)
    public static final double SUN_DISTANCE = 9, SUN_RADIUS = 0.9, SUN_GLOW = 2.4;
    //The planet's picture: this many pixels across (one per chunk of a 64 chunk planet)
    public static final int PLANET_PIXELS = 64;
    //Its clouds: how many, and how fast they drift (planet sides per second)
    private static final int PLANET_CLOUDS = 90;
    private static final double CLOUD_DRIFT = 0.004;
    //The clouds over the middle (drawLane): a chain of LANE_CLOUDS clouds that repeats, slow and fast in turns, each
    //LANE_MIN_HEIGHT to LANE_MAX_HEIGHT high (chunks; their width follows, skyLayer.cloudWidth), at SLOW_SPEED or
    //FAST_SPEED (chunks a second). Each reaches the middle LANE_OVERLAP seconds before the one before it has passed it
    private static final int LANE_CLOUDS = 12;
    private static final double LANE_MIN_HEIGHT = 1.4, LANE_MAX_HEIGHT = 3.2, SLOW_SPEED = 0.25, FAST_SPEED = 0.65, LANE_OVERLAP = 1.2;
    //The bands of such chains across the planet: the one through the middle (over the player) and BANDS more, each
    //with its own clouds, at its own height (share of the side) with its own spot where its clouds meet (share of the
    //side from the left)
    private static final double[][] BANDS = {{0.5, 0.5}, {0.16, 0.3}, {0.34, 0.75}, {0.8, 0.4}};

    //One band: per cloud of its chain seed, height, width, speed, and when (in the chain's own time) its right edge
    //reaches the meeting spot; and how long the whole chain takes
    private static class band {
        double y, meet;
        double[][] clouds;
        double cycle;
    }
    private band[] bands;

    private gameHandler handler;
    private WritableImage moon;
    private java.util.Map<chunks, WritableImage> planets = new java.util.HashMap<chunks, WritableImage>();

    public spaceView (gameHandler handler) {
        this.handler = handler;
    }

    //---- The whole view ----

    //How many of the planet's chunks are across the screen when the map's view would be across chunks across: at the
    //switch (gameHandler.COVER_FULL_CHUNKS) PLANET_START_ACROSS, small enough that the cloud over the player fills the
    //screen, growing as a power of the zoom so that zoomed all the way out (about 250 across) the whole planet and the
    //moon are in view
    public static final double PLANET_START_ACROSS = 0.2, PLANET_ZOOM_POWER = 2.9;
    public static double planetAcross (double across) {
        return PLANET_START_ACROSS*Math.pow(across/gameHandler.COVER_FULL_CHUNKS, PLANET_ZOOM_POWER);
    }

    //The chunks across (as passed to draw) at which the sun with its sunstone and the black hole with its disc are both
    //on a w by h screen, with a little room round them, for a planet side chunks across
    public static double acrossToSeeAll (double side, double w, double h) {
        double sunReach = SUN_DISTANCE*0.7071 + SUN_RADIUS*1.3;
        double holeReach = BLACK_HOLE_RADIUS*BLACK_HOLE_DISC;
        double halfW = Math.max(sunReach, BLACK_HOLE_DISTANCE*0.8 + holeReach) + 0.5;
        double halfH = Math.max(sunReach, BLACK_HOLE_DISTANCE*0.6 + holeReach) + 0.5;
        //Planet chunks across the screen's width that fit both
        double planet = side*Math.max(2*halfW, 2*halfH*w/Math.max(1, h));
        return gameHandler.COVER_FULL_CHUNKS*Math.pow(planet/PLANET_START_ACROSS, 1/PLANET_ZOOM_POWER);
    }

    //Space with its stars, the planet with its clouds and air, and the moon, zoomed out to across chunks
    //From the mirror dimension (chunks.mirror) all of it is seen in a mirror: turned round left to right, the planet
    //(in the mirror dimension's cold blues), the black hole on the left and the sun on the right
    public void draw (GraphicsContext gc, chunks seenFrom, double across) {
        if (!seenFrom.isMirror()) {
            drawScene(gc, seenFrom, across);
            return;
        }
        gc.save();
        gc.translate(handler.getWIDTH(), 0);
        gc.scale(-1, 1);
        drawScene(gc, seenFrom, across);
        gc.restore();
    }

    private void drawScene (GraphicsContext gc, chunks seenFrom, double across) {
        //The planet of the world the player is in (the overworld's for the sky ruins and the mirror dimension,
        //chunks.getPlanet)
        chunks world = seenFrom.getPlanet();
        double w = handler.getWIDTH(), h = handler.getHEIGHT();
        gc.setFill(SPACE);
        gc.fillRect(0, 0, w, h);
        //The Milky Way and a far spiral galaxy, furthest back of all
        galaxyBackdrop.draw(gc, w, h, 1);
        screenTransition.drawStars(handler, gc, 0, 0, w, h, 1);
        gc.setGlobalAlpha(1);
        double[] sq = world.getSquare();
        if (sq == null) return;
        double side = sq[2];
        //A chunk of the planet on the screen, at the planet's own zoom
        double uw = w/planetAcross(across), uh = uw;
        drawNearStars(gc, w, h, uw);

        //The planet's middle (the player) in the middle of the screen
        double left = Math.round(handler.getMidWIDTH() - side/2*uw), top = Math.round(handler.getMidHEIGHT() - side/2*uh);
        double sw = Math.round(side*uw), sh = Math.round(side*uh);

        gc.save();
        gc.setImageSmoothing(false);
        //The air: rings round the square, each wider and fainter
        gc.setFill(AIR);
        double step = Math.max(2, sw*0.012);
        for (int i = 1; i <= 4; i++) {
            gc.setGlobalAlpha(0.28/i);
            double o = i*step;
            gc.fillRect(left - o, top - o, sw + 2*o, step);
            gc.fillRect(left - o, top + sh + o - step, sw + 2*o, step);
            gc.fillRect(left - o, top - o + step, step, sh + 2*o - 2*step);
            gc.fillRect(left + sw + o - step, top - o + step, step, sh + 2*o - 2*step);
        }
        gc.setGlobalAlpha(1);
        gc.drawImage(planet(world, seenFrom.isMirror()), left, top, sw, sh);
        drawPlanetClouds(gc, world, left, top, sw, sh, side);
        drawLane(gc, left, top, sw, sh, side, uw, uh);

        //Meteorites flying past now and then
        //Where the black hole is (drawn further down): meteorites near it strike at it
        holeX = left + sw/2 + BLACK_HOLE_DISTANCE*side*uw*0.8;
        holeY = top + sh/2 + BLACK_HOLE_DISTANCE*side*uh*0.6;
        holeR = BLACK_HOLE_RADIUS*side*uw;
        drawMeteorites(gc, left + sw/2, top + sh/2, side, uw);

        //The moon, circling the middle of the planet: a huge moonstone (gemstones.moonstone)
        double angle = System.nanoTime()/1e9/MOON_SECONDS*2*Math.PI;
        double cx = left + sw/2 + Math.cos(angle)*side*MOON_ORBIT*uw, cy = top + sh/2 + Math.sin(angle)*side*MOON_ORBIT*uh;
        double r = side*MOON_RADIUS;
        gc.drawImage(rock(gemstones.MOON_COLOR), 0, 0, guiSkin.CELL, guiSkin.CELL, cx - r*uw, cy - r*uh, 2*r*uw, 2*r*uh);

        //The black hole, far off to the bottom right: a huge singularity (gemstones.singularity) in a disc of light
        //spiralling into it
        drawBlackHole(gc, holeX, holeY, holeR, w, h);

        //The sun, far off to the top left, in its glow
        double sunX = left + sw/2 - SUN_DISTANCE*side*uw*0.7071, sunY = top + sh/2 - SUN_DISTANCE*side*uh*0.7071;
        double sr = SUN_RADIUS*side*uw, glow = sr*SUN_GLOW;
        if (sunX + glow > 0 && sunY + glow > 0 && sunX - glow < w && sunY - glow < h) {
            gc.setFill(new javafx.scene.paint.RadialGradient(0, 0, sunX, sunY, glow, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#FFE500", 0.45)), new javafx.scene.paint.Stop(0.4, Color.web("#FC9F00", 0.18)),
                new javafx.scene.paint.Stop(1, Color.web("#FC9F00", 0))));
            gc.fillOval(sunX - glow, sunY - glow, 2*glow, 2*glow);
            //A huge sunstone (gemstones.sunstone)
            gc.drawImage(rock(gemstones.SUN_COLOR), 0, 0, guiSkin.CELL, guiSkin.CELL, sunX - sr, sunY - sr, 2*sr, 2*sr);
        }
        gc.restore();
    }

    //---- Stars ----

    //Layers of stars between the far ones fixed to the screen (screenTransition.drawStars) and the planet, the sun and
    //the black hole, so zooming has depth: a layer spreads out from the middle of the screen as the view zooms in, by
    //the planet's zoom to the power of its depth (0 would be fixed like the far stars, 1 would move with the planet),
    //the nearer layers more. Each star spreads out over one doubling of its distance from the middle, then starts again
    //close to the middle, fading in and out at the two ends, each at its own point of that, so a layer always looks the
    //same however far in or out it is. Per layer: depth, stars per million screen pixels, smallest and biggest size
    //(whole pixels), brightness
    private static final double[][] STAR_LAYERS = {
        {0.12, 140, 1, 1, 0.55},
        {0.24, 70, 1, 2, 0.75},
        {0.38, 30, 2, 3, 0.95}};
    private static final Color[] NEAR_STAR_COLORS = {Color.WHITE, Color.WHITE, Color.web("#FFF4C0"), Color.web("#C8E0FF")};
    //Per layer, per star: x and y (-1 to 1 of half the screen), where it is in its doubling (0 to 1), size, twinkle
    //speed and phase, colour
    private double[][][] nearStars;
    private int[][] nearStarColors;
    private double nearStarsW = -1, nearStarsH = -1;

    private void makeNearStars (double w, double h) {
        nearStarsW = w;
        nearStarsH = h;
        Random rnd = new Random(20261115L);
        nearStars = new double[STAR_LAYERS.length][][];
        nearStarColors = new int[STAR_LAYERS.length][];
        for (int l = 0; l < STAR_LAYERS.length; l++) {
            double[] layer = STAR_LAYERS[l];
            int n = (int) Math.round(w*h/1e6*layer[1]);
            nearStars[l] = new double[n][];
            nearStarColors[l] = new int[n];
            for (int i = 0; i < n; i++) {
                double size = layer[2] + Math.floor(rnd.nextDouble()*(layer[3] - layer[2] + 1));
                nearStars[l][i] = new double[] {rnd.nextDouble()*2 - 1, rnd.nextDouble()*2 - 1, rnd.nextDouble(), size,
                    1.5 + rnd.nextDouble()*3, rnd.nextDouble()*2*Math.PI};
                nearStarColors[l][i] = rnd.nextInt(NEAR_STAR_COLORS.length);
            }
        }
    }

    //uw: screen pixels per chunk of the planet (its zoom)
    private void drawNearStars (GraphicsContext gc, double w, double h, double uw) {
        if (w != nearStarsW || h != nearStarsH) makeNearStars(w, h);
        double time = System.nanoTime()/1e9, zoom = Math.log(uw)/Math.log(2);
        double mx = w/2, my = h/2;
        for (int l = 0; l < STAR_LAYERS.length; l++) {
            double[] layer = STAR_LAYERS[l];
            double shift = layer[0]*zoom;
            for (int i = 0; i < nearStars[l].length; i++) {
                double[] st = nearStars[l][i];
                double f = st[2] + shift;
                f -= Math.floor(f);
                double spread = Math.pow(2, f);
                double x = mx + st[0]*mx*spread, y = my + st[1]*my*spread;
                if (x < -3 || y < -3 || x > w + 3 || y > h + 3) continue;
                //Fading in near the middle, out before it starts again
                double in = Math.min(1, f/0.25), out = Math.min(1, (1 - f)/0.15);
                double fade = in*in*(3 - 2*in)*out*out*(3 - 2*out);
                double twinkle = 0.5 + 0.5*Math.sin(time*st[4] + st[5]);
                double a = layer[4]*fade*(0.45 + 0.55*twinkle);
                if (a <= 0.02) continue;
                int size = (int) st[3];
                gc.setGlobalAlpha(a);
                gc.setFill(NEAR_STAR_COLORS[nearStarColors[l][i]]);
                gc.fillRect(Math.floor(x) - (size - 1)/2, Math.floor(y) - (size - 1)/2, size, size);
            }
        }
        gc.setGlobalAlpha(1);
    }

    //---- The planet ----

    //Its clouds, drifting across it and coming back in on the other side, kept inside the square
    private void drawPlanetClouds (GraphicsContext gc, chunks world, double left, double top, double sw, double sh, double side) {
        gc.save();
        gui.clipTo(gc, left, top, sw, sh);
        double time = System.nanoTime()/1e9;
        Random rnd = new Random(world.getName().hashCode()*31L + 7);
        double chunkPx = sw/side;
        for (int i = 0; i < PLANET_CLOUDS; i++) {
            double cw = (2 + rnd.nextDouble()*5)*chunkPx, ch = cw*(0.3 + rnd.nextDouble()*0.2);
            double px = (rnd.nextDouble() + time*CLOUD_DRIFT*(0.7 + rnd.nextDouble()*0.6)) % 1, py = rnd.nextDouble();
            long seed = rnd.nextLong();
            double x = left + px*sw, y = top + py*sh;
            skyLayer.drawCloud(gc, seed, Math.round(x), Math.round(y), cw, ch);
            //The part that has drifted out on the right comes back in on the left
            if (x + cw > left + sw) skyLayer.drawCloud(gc, seed, Math.round(x - sw), Math.round(y), cw, ch);
        }
        gc.restore();
    }

    //The chain of clouds over the middle of the planet, where the player is: slow and fast in turns. A slow one crosses
    //the middle; just as it leaves, a fast one that has caught up with it is over the middle; just as that leaves,
    //the next slow one arrives, and so on. They all move along a band through the middle, coming in at the planet's
    //left edge and leaving at its right, so away from the middle they are spread out with gaps. The line through the
    //middle of the screen runs through the solid part of each cloud (skyLayer.CLOUD_SOLID_LINE)
    private static band makeBand (double y, double meet, long seed) {
        band b = new band();
        b.y = y;
        b.meet = meet;
        b.clouds = new double[LANE_CLOUDS][];
        Random rnd = new Random(seed);
        double at = 0;
        for (int i = 0; i < LANE_CLOUDS; i++) {
            long cloudSeed = rnd.nextLong();
            double h = LANE_MIN_HEIGHT + rnd.nextDouble()*(LANE_MAX_HEIGHT - LANE_MIN_HEIGHT);
            double w = skyLayer.cloudWidth(cloudSeed, h);
            double speed = i % 2 == 0 ? SLOW_SPEED : FAST_SPEED;
            b.clouds[i] = new double[] {cloudSeed, h, w, speed, at};
            at += w/speed - LANE_OVERLAP;
        }
        b.cycle = at;
        return b;
    }

    private void drawLane (GraphicsContext gc, double left, double top, double sw, double sh, double side, double uw, double uh) {
        if (bands == null) {
            bands = new band[BANDS.length];
            for (int i = 0; i < BANDS.length; i++) bands[i] = makeBand(BANDS[i][0], BANDS[i][1], 31337 + 7919L*i);
        }
        double time = System.nanoTime()/1e9;
        gc.save();
        gui.clipTo(gc, left, top, sw, sh);
        for (band b : bands) {
            double meet = side*b.meet, lineY = side*b.y;
            long cycle = (long) Math.floor(time/b.cycle);
            //Enough chains round now to have every cloud that is on the planet
            int reach = (int) Math.ceil(side/SLOW_SPEED/b.cycle) + 1;
            //Slow ones first, so the fast ones pass over them
            for (int pass = 0; pass < 2; pass++) {
                for (long c = cycle - reach; c <= cycle + reach; c++) {
                    for (int i = pass; i < LANE_CLOUDS; i += 2) {
                        double[] cl = b.clouds[i];
                        double h = cl[1], w = cl[2], speed = cl[3];
                        double right = meet + (time - (c*b.cycle + cl[4]))*speed;
                        if (right < 0 || right - w > side) continue;
                        //Its solid line (CLOUD_SOLID_LINE of its height down from its top) along the band
                        double x = left + (right - w)*uw, y = top + (lineY - h*skyLayer.CLOUD_SOLID_LINE)*uh;
                        skyLayer.drawCloud(gc, (long) cl[0], Math.round(x), Math.round(y), w*uw, h*uh);
                    }
                }
            }
        }
        gc.restore();
    }

    //Its picture, in cold blues when seen from the mirror dimension
    private java.util.Map<chunks, WritableImage> coldPlanets = new java.util.HashMap<chunks, WritableImage>();
    private WritableImage planet (chunks world, boolean cold) {
        java.util.Map<chunks, WritableImage> made = cold ? coldPlanets : planets;
        WritableImage image = made.get(world);
        if (image == null) {
            image = makePlanet(world, cold);
            made.put(world, image);
        }
        return image;
    }

    //A smooth noise from 0 to 1 over the planet: a few layers of blended random grids, each finer and fainter
    private static double[][] noise (int n, long seed) {
        double[][] out = new double[n][n];
        Random rnd = new Random(seed);
        double total = 0;
        for (int octave = 0, cells = 4; octave < 4; octave++, cells *= 2) {
            double weight = 1.0/(1 << octave);
            total += weight;
            double[][] grid = new double[cells + 1][cells + 1];
            for (double[] row : grid) for (int i = 0; i < row.length; i++) row[i] = rnd.nextDouble();
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    double gx = x*(double) cells/n, gy = y*(double) cells/n;
                    int ix = (int) gx, iy = (int) gy;
                    double ax = gx - ix, ay = gy - iy;
                    ax = ax*ax*(3 - 2*ax);
                    ay = ay*ay*(3 - 2*ay);
                    double top = grid[iy][ix] + (grid[iy][ix + 1] - grid[iy][ix])*ax;
                    double bottom = grid[iy + 1][ix] + (grid[iy + 1][ix + 1] - grid[iy + 1][ix])*ax;
                    out[y][x] += (top + (bottom - top)*ay)*weight;
                }
            }
        }
        for (double[] row : out) for (int i = 0; i < row.length; i++) row[i] /= total;
        return out;
    }

    //The planet as pixel art. Height from noise: sea (deep, shallow), beach, grass, forest, mountain, snow. Outside the
    //game's own greens and blues, the mirror dimension the same in its cold blues (imgDict.cold), and the sky ruins
    //open sky with islands of snow and stone
    private static WritableImage makePlanet (chunks world, boolean cold) {
        int n = PLANET_PIXELS;
        double[][] height = noise(n, world.getName().hashCode());
        double[][] wood = noise(n, world.getName().hashCode()*7L + 3);
        Color[] ramp;
        double[] levels;
        if (world.isSky()) {
            ramp = new Color[] {Color.web("#79BCCE"), Color.web("#8EDCF2"), Color.web("#D8DCE4"), Color.web("#F0F0F4"), Color.web("#F0F0F4"), Color.web("#C8C8C8"), Color.web("#FFFFFF")};
            levels = new double[] {0.38, 0.55, 0.6, 0.66, 0.7, 0.76};
        } else {
            ramp = new Color[] {Color.web("#3888DD"), Color.web("#4A9CF0"), Color.web("#E0C080"), Color.web("#60A040"), Color.web("#467764"), Color.web("#8A8A8A"), Color.web("#F0F0F4")};
            levels = new double[] {0.4, 0.47, 0.5, 0.6, 0.68, 0.74};
        }
        WritableImage img = new WritableImage(n, n);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                double v = height[y][x];
                int k = 0;
                while (k < levels.length && v >= levels[k]) k++;
                //Forests on part of the grass
                if (k == 3 && wood[y][x] > 0.55) k = 4;
                Color c = ramp[k];
                if (cold) c = imgDict.cold(c);
                //The far side a little darker: lit from the top left
                double shadeBy = (x + y)/(2.0*n);
                c = c.interpolate(SPACE, Math.max(0, shadeBy - 0.6)*0.5);
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    //---- The black hole ----

    //How far away it is (planet sides, to the bottom right), how big (radius, sides), and how far out the rocks going
    //round it reach (times its radius, see drawOrbiters)
    public static final double BLACK_HOLE_DISTANCE = 7, BLACK_HOLE_RADIUS = 0.35, BLACK_HOLE_DISC = 3.2;

    //Where the black hole is on the screen this frame, and how big
    private double holeX, holeY, holeR;
    //Its purple lightning (like the singularity gemstone's): bolts out of its edge, HOLE_SPARKS a second
    private static final double HOLE_SPARKS = 4;

    private void drawBlackHole (GraphicsContext gc, double x, double y, double r, double w, double h) {
        double reach = r*BLACK_HOLE_DISC;
        if (x + reach < 0 || y + reach < 0 || x - reach > w || y - reach > h) return;
        double time = System.nanoTime()/1e9;
        //A dark halo first: it swallows the stars round it
        gc.setFill(new javafx.scene.paint.RadialGradient(0, 0, x, y, reach*0.8, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.color(0, 0, 0, 1)), new javafx.scene.paint.Stop(1, Color.color(0, 0, 0, 0))));
        gc.fillOval(x - reach*0.8, y - reach*0.8, reach*1.6, reach*1.6);
        //The rocks going round it: the far half of each orbit behind the singularity, the near half in front
        drawOrbiters(gc, x, y, r, time, false);
        //The singularity itself, huge, in the middle, with its white edge (one sheet pixel out all round)
        javafx.scene.image.Image edge = handler.getImages().getSilhouette(guiSkin.ROCK_X, guiSkin.GUI_Y, "#FFFFFF");
        double step = Math.max(1, Math.round(2*r/guiSkin.CELL));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) gc.drawImage(edge, 0, 0, guiSkin.CELL, guiSkin.CELL, x - r + dx*step, y - r + dy*step, 2*r, 2*r);
            }
        }
        gc.drawImage(rock(gemstones.SINGULARITY_COLOR), 0, 0, guiSkin.CELL, guiSkin.CELL, x - r, y - r, 2*r, 2*r);
        drawOrbiters(gc, x, y, r, time, true);
    }

    //Moonstones and meteorites (ORBITERS of each) going round the black hole on a tilted disc (flattened to
    //ORBIT_TILT of its width), from just outside it to BLACK_HOLE_DISC times its radius out, the inner ones faster.
    //front: the near half of their orbits (in front of the singularity), else the far half. The meteorites throw their
    //purple lightning now and then, ORBITER_SPARKS a second each: much less than the ones flying past
    private static final int ORBITERS = 20;
    private static final double ORBIT_TILT = 0.45, ORBITER_SPARKS = 0.15;
    private long lastOrbit = System.nanoTime();

    private void drawOrbiters (GraphicsContext gc, double x, double y, double r, double time, boolean front) {
        double dt = 0;
        if (front) {
            long now = System.nanoTime();
            dt = Math.min(0.1, (now - lastOrbit)/1e9);
            lastOrbit = now;
        }
        Random rnd = new Random(4040);
        javafx.scene.image.Image moon = rock(gemstones.MOON_COLOR), meteor = rock(METEORITE_COLOR);
        for (int i = 0; i < 2*ORBITERS; i++) {
            boolean isMoon = i % 2 == 0;
            double dist = r*(1.4 + rnd.nextDouble()*(BLACK_HOLE_DISC - 1.4));
            //Faster the closer in (like real orbits: the square of the time goes with the cube of the distance)
            double speed = 0.35*Math.pow(r*1.4/dist, 1.5);
            double a = rnd.nextDouble()*2*Math.PI + time*speed;
            double size = r*(0.18 + rnd.nextDouble()*0.14);
            double spin = rnd.nextDouble()*360, spinSpeed = 40 + rnd.nextDouble()*80;
            boolean near = Math.sin(a) > 0;
            if (near != front) continue;
            double bx = x + Math.cos(a)*dist, by = y + Math.sin(a)*dist*ORBIT_TILT;
            gc.save();
            gc.translate(bx, by);
            gc.rotate(spin + time*spinSpeed);
            gc.drawImage(isMoon ? moon : meteor, 0, 0, guiSkin.CELL, guiSkin.CELL, -size/2, -size/2, size, size);
            gc.restore();
            if (!isMoon && sparks.nextDouble() < ORBITER_SPARKS*dt*2) {
                bolts.add(new guiLightning(bx, by, sparks.nextDouble()*2*Math.PI, size*(1.5 + sparks.nextDouble()*2), Math.max(1, size*0.06), 5, COSMIC_BOLT, sparks));
            }
        }
    }

    //---- The moon, the sun and the meteorites: gemstones ----

    //The rock texture all gemstones have, in a colour (as the rock items are tinted)
    private javafx.scene.image.Image rock (String hex) {
        return handler.getImages().getTinted(guiSkin.ROCK_X, guiSkin.GUI_Y, hex);
    }

    //Meteorites (the meteorite gemstone, huge) flying past the planet: METEORITES of them, each on its own straight path
    //from METEOR_FAR planet sides out on one side to as far out on the other (further than even the furthest view
    //reaches, so they never appear or vanish on screen), at its own speed, then a rest before the next path. They
    //tumble, shed the sparkles the gemstone gives off behind them, and throw purple lightning like the meteorite gemstone does: now and then
    //on their own (METEOR_SPARKS a second), and at each other when two are within METEOR_REACH sides (METEOR_ARCS a
    //second)
    private static final int METEORITES = 24;
    private static final double METEOR_FAR = 30, METEOR_REACH = 1.6, METEOR_SPARKS = 1.2, METEOR_ARCS = 6;
    public static final String METEORITE_COLOR = "#6A7A9A";
    //The sparkles a meteorite gives off (itemParticles.SPARKLE, as the gemstone does), shed behind it as it flies:
    //METEOR_SHED a second each, living METEOR_SPARKLE_LIFE seconds, kept in the planet's units so zooming leaves them put
    private static final String[] METEOR_SPARKLES = {"#FFFFFF", "#FFF080"};
    private static final double METEOR_SHED = 50, METEOR_SPARKLE_LIFE = 1.1;
    //x, y (planet sides from its middle), drift x, y (sides a second), age, life, size (sides), colour
    private final java.util.List<double[]> sparkles = new java.util.ArrayList<double[]>();
    private static final Color COSMIC_BOLT = Color.web("#B060FF");

    private final java.util.List<guiLightning> bolts = new java.util.ArrayList<guiLightning>();
    private final Random sparks = new Random();
    private long lastFrame = System.nanoTime();

    private void drawMeteorites (GraphicsContext gc, double cx, double cy, double side, double unit) {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastFrame)/1e9);
        lastFrame = now;
        double time = now/1e9;
        javafx.scene.image.Image img = rock(METEORITE_COLOR);
        java.util.List<double[]> seen = new java.util.ArrayList<double[]>();
        for (int i = 0; i < METEORITES; i++) {
            Random rnd = seeded.random(777 + i);
            double speed = 0.6 + rnd.nextDouble()*0.8, cross = 2*METEOR_FAR/speed;
            double period = cross + 5 + rnd.nextDouble()*20;
            double offset = rnd.nextDouble()*period;
            long round = (long) Math.floor((time + offset)/period);
            double since = (time + offset) - round*period;
            if (since > cross) continue;
            //A new path every round: in from far out on one side, past the planet, out on the other
            Random path = seeded.random(round*7919L + i);
            double a = path.nextDouble()*2*Math.PI, miss = (0.7 + path.nextDouble()*1.6)*(path.nextBoolean() ? 1 : -1);
            double dx = -Math.cos(a), dy = -Math.sin(a);
            double sx = Math.cos(a)*METEOR_FAR - dy*miss, sy = Math.sin(a)*METEOR_FAR + dx*miss;
            double along = since*speed;
            double x = cx + (sx + dx*along)*side*unit, y = cy + (sy + dy*along)*side*unit;
            double size = (0.05 + path.nextDouble()*0.05)*side*unit;
            seen.add(new double[] {x, y, size});
            //Its sparkles, shed where it is (in the planet's units)
            double px = sx + dx*along, py = sy + dy*along, sizeSides = size/(side*unit);
            for (int k = (int) (METEOR_SHED*dt + sparks.nextDouble()); k > 0; k--) {
                sparkles.add(new double[] {px + (sparks.nextDouble() - 0.5)*sizeSides*0.8, py + (sparks.nextDouble() - 0.5)*sizeSides*0.8,
                    (sparks.nextDouble() - 0.5)*sizeSides*0.6, (sparks.nextDouble() - 0.5)*sizeSides*0.6,
                    0, METEOR_SPARKLE_LIFE*(0.6 + sparks.nextDouble()*0.8), sizeSides*(0.06 + sparks.nextDouble()*0.06), sparks.nextInt(METEOR_SPARKLES.length)});
            }
            //Only drawn when on the screen
            if (x < -size*2 || y < -size*2 || x > handler.getWIDTH() + size*2 || y > handler.getHEIGHT() + size*2) continue;
            gc.save();
            gc.translate(x, y);
            gc.rotate(time*90 + i*40);
            gc.drawImage(img, 0, 0, guiSkin.CELL, guiSkin.CELL, -size/2, -size/2, size, size);
            gc.restore();
            //Its own purple lightning, now and then
            if (sparks.nextDouble() < METEOR_SPARKS*dt) {
                bolts.add(new guiLightning(x, y, sparks.nextDouble()*2*Math.PI, size*(1.5 + sparks.nextDouble()*2), Math.max(1, size*0.06), 5, COSMIC_BOLT, sparks));
            }
        }
        //The black hole crackles with its own purple lightning, and meteorites near it strike at it
        if (sparks.nextDouble() < HOLE_SPARKS*dt) {
            double a = sparks.nextDouble()*2*Math.PI;
            bolts.add(new guiLightning(holeX + Math.cos(a)*holeR*0.8, holeY + Math.sin(a)*holeR*0.8, a, holeR*(1 + sparks.nextDouble()*1.5), Math.max(1, holeR*0.05), 6, COSMIC_BOLT, sparks));
        }
        double holeReach = METEOR_REACH*side*unit + holeR;
        for (double[] p : seen) {
            if (Math.hypot(p[0] - holeX, p[1] - holeY) > holeReach) continue;
            if (sparks.nextDouble() < METEOR_ARCS*dt) {
                bolts.add(guiLightning.between(p[0], p[1], holeX, holeY, Math.max(1, p[2]*0.08), 8, COSMIC_BOLT, sparks));
            }
        }
        //Two close to each other strike at each other
        double reach = METEOR_REACH*side*unit;
        for (int i = 0; i < seen.size(); i++) {
            for (int j = i + 1; j < seen.size(); j++) {
                double[] p = seen.get(i), q = seen.get(j);
                if (Math.hypot(p[0] - q[0], p[1] - q[1]) > reach) continue;
                if (sparks.nextDouble() < METEOR_ARCS*dt) {
                    bolts.add(guiLightning.between(p[0], p[1], q[0], q[1], Math.max(1, Math.min(p[2], q[2])*0.08), 7, COSMIC_BOLT, sparks));
                }
            }
        }
        //The sparkles: drifting a little, fading out
        for (double[] p : sparkles) {
            p[0] += p[2]*dt;
            p[1] += p[3]*dt;
            p[4] += dt;
        }
        sparkles.removeIf(p -> p[4] >= p[5]);
        for (double[] p : sparkles) {
            double size = Math.max(1, Math.round(p[6]*side*unit));
            double x = cx + p[0]*side*unit, y = cy + p[1]*side*unit;
            if (x < -size || y < -size || x > handler.getWIDTH() || y > handler.getHEIGHT()) continue;
            gc.setGlobalAlpha(Math.max(0, 1 - p[4]/p[5])*0.9);
            gc.setFill(Color.web(METEOR_SPARKLES[(int) p[7]]));
            gc.fillRect(Math.round(x - size/2), Math.round(y - size/2), size, size);
        }
        gc.setGlobalAlpha(1);
        for (guiLightning b : bolts) b.next(dt);
        bolts.removeIf(guiLightning::isDead);
        gc.save();
        for (guiLightning b : bolts) b.draw(gc);
        gc.restore();
    }
}
