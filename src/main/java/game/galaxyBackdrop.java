package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

//The furthest back of space (spaceView): the Milky Way, a band of soft violet and blue light across the sky with a
//warm bright core, dark dust lanes winding along it and fine star dust in it, and a small spiral galaxy off in the
//top right. Fixed to the screen, behind every layer of stars. In blocks of BLOCK screen pixels with a few steps of
//brightness and a dither between them, like the rest of the art.
//The picture is worked out once per screen size, on its own thread so it never holds up a frame, and fades in when
//it is ready. Shared by every game
public final class galaxyBackdrop {

    private galaxyBackdrop () {}

    //Screen pixels per block of light, how many steps of brightness, how bright at most (0 to 1)
    private static final int BLOCK = 3, STEPS = 7;
    private static final double STRENGTH = 0.3;
    //The band: its angle (degrees, rising to the right), half its width (share of the screen's height), where along
    //it its core is (-1 to 1 of half its length) and how wide that is
    private static final double BAND_ANGLE = -28, BAND_WIDTH = 0.17, CORE_AT = -0.25, CORE_SIZE = 0.35;
    //The spiral galaxy: its middle (share of the screen), radius (share of the height), tilt (how flat it looks) and turn
    private static final double SPIRAL_X = 0.84, SPIRAL_Y = 0.2, SPIRAL_R = 0.075, SPIRAL_FLAT = 0.42, SPIRAL_TURN = 25;
    //Seconds it takes to fade in once ready
    private static final double FADE_IN = 1.2;

    private static WritableImage image;
    private static int[] ready;
    private static int readyW, readyH, madeW = -1, madeH = -1;
    private static long shownSince;

    //Draws it over the whole w by h screen (alpha 1: all of it), starting to make it first if needed
    public static void draw (GraphicsContext gc, double w, double h, double alpha) {
        int iw = (int) Math.ceil(w), ih = (int) Math.ceil(h);
        if (iw <= 0 || ih <= 0) return;
        synchronized (galaxyBackdrop.class) {
            if (iw != madeW || ih != madeH) {
                madeW = iw;
                madeH = ih;
                //One worker at a time; while the window is being resized it catches up with the last size
                if (!working) {
                    working = true;
                    Thread t = new Thread(galaxyBackdrop::work, "galaxy");
                    t.setDaemon(true);
                    t.start();
                }
            }
            if (ready != null) {
                WritableImage img = new WritableImage(readyW, readyH);
                img.getPixelWriter().setPixels(0, 0, readyW, readyH, PixelFormat.getIntArgbInstance(), ready, 0, readyW);
                //Only the first one fades in; after a resize the new one takes over from the one before at once
                if (image == null) shownSince = System.nanoTime();
                image = img;
                ready = null;
            }
        }
        if (image == null) return;
        double fade = Math.min(1, (System.nanoTime() - shownSince)/1e9/FADE_IN);
        gc.save();
        gc.setImageSmoothing(false);
        gc.setGlobalAlpha(alpha*fade*fade*(3 - 2*fade));
        gc.drawImage(image, 0, 0, w, h);
        gc.restore();
    }

    //---- Making the picture ----

    private static boolean working = false;

    private static void work () {
        while (true) {
            int cw, ch;
            synchronized (galaxyBackdrop.class) {
                cw = madeW;
                ch = madeH;
            }
            int[] px = make(cw, ch);
            synchronized (galaxyBackdrop.class) {
                if (cw == madeW && ch == madeH) {
                    ready = px;
                    readyW = cw;
                    readyH = ch;
                    working = false;
                    return;
                }
            }
        }
    }

    //The 4x4 dither, so a block between two steps of brightness takes one or the other in a fine pattern
    private static final double[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private static int[] make (int w, int h) {
        int[] px = new int[w*h];
        java.util.Arrays.fill(px, 0xFF000000);
        int bw = (w + BLOCK - 1)/BLOCK, bh = (h + BLOCK - 1)/BLOCK;
        double ang = Math.toRadians(BAND_ANGLE), ca = Math.cos(ang), sa = Math.sin(ang);
        double half = Math.hypot(w, h)/2;
        double spiralAng = Math.toRadians(SPIRAL_TURN), cs = Math.cos(spiralAng), ss = Math.sin(spiralAng);
        for (int by = 0; by < bh; by++) {
            for (int bx = 0; bx < bw; bx++) {
                double x = (bx + 0.5)*BLOCK, y = (by + 0.5)*BLOCK;
                double[] light = new double[2];
                band(x - w/2.0, y - h/2.0, ca, sa, half, h, light);
                spiral(x - SPIRAL_X*w, y - SPIRAL_Y*h, cs, ss, h, light);
                double v = Math.min(1, light[0])*STRENGTH;
                //A few steps of brightness, dithered
                double stepped = Math.floor(v*STEPS + BAYER[(by & 3)*4 + (bx & 3)]/16.0)/STEPS;
                if (stepped <= 0) continue;
                int c = colour(stepped, Math.min(1, light[1]));
                for (int py = by*BLOCK; py < Math.min(h, by*BLOCK + BLOCK); py++)
                    for (int qx = bx*BLOCK; qx < Math.min(w, bx*BLOCK + BLOCK); qx++) px[py*w + qx] = c;
            }
        }
        dust(px, w, h, ca, sa, half);
        return px;
    }

    //The band's light at (x, y) from the middle of the screen: adds the brightness to out[0] and how warm (core) it
    //is to out[1]
    private static void band (double x, double y, double ca, double sa, double half, double h, double[] out) {
        //Along the band (s, -1 to 1) and across it (d, in band half widths)
        double s = (x*ca + y*sa)/half, across = -x*sa + y*ca;
        double width = BAND_WIDTH*h*(1 + 0.35*Math.sin(s*2.3 + 1.1) + 0.25*fbm(s*3, 7.7));
        //Wavy, so it is not a ruler-straight stripe
        double d = (across - 0.18*width*Math.sin(s*3.1)) / width;
        double glow = Math.exp(-d*d*1.6);
        //Clumps of light along it
        double clump = 0.55 + 0.75*fbm(s*6 + 3, d*1.5 + 5);
        //The dust lanes: dark winding streaks a little off the middle, eating the light
        double lane = Math.exp(-Math.pow((d - 0.15 - 0.35*fbm(s*4, 2.2) + 0.2)/0.22, 2));
        double lane2 = Math.exp(-Math.pow((d + 0.45 + 0.3*fbm(s*5, 9.1))/0.14, 2));
        double dark = 1 - 0.75*lane*(0.6 + 0.6*fbm(s*9, d*3)) - 0.5*lane2*fbm(s*7 + 4, 1.3);
        double core = Math.exp(-Math.pow((s - CORE_AT)/CORE_SIZE, 2))*Math.exp(-d*d*2.5);
        double v = glow*clump*Math.max(0, dark)*(0.55 + 0.45*(1 - Math.abs(s))) + core*0.55*Math.max(0.25, dark);
        out[0] += Math.max(0, v);
        out[1] += core;
    }

    //The spiral galaxy's light at (x, y) from its middle
    private static void spiral (double x, double y, double cs, double ss, double h, double[] out) {
        double r0 = SPIRAL_R*h;
        //Turned, then flattened as if tilted away
        double u = (x*cs + y*ss)/r0, v = (-x*ss + y*cs)/(r0*SPIRAL_FLAT);
        double r = Math.hypot(u, v);
        if (r > 1.6) return;
        double theta = Math.atan2(v, u);
        //Two arms winding out
        double arm = Math.cos(2*theta - 4.5*Math.log(r + 0.05));
        arm = Math.pow(Math.max(0, arm), 2.5);
        double disc = Math.exp(-r*2.6);
        double core = Math.exp(-r*r*40);
        out[0] += disc*(0.25 + 0.9*arm) + core*0.6;
        out[1] += core*1.2 + disc*0.25;
    }

    //Fine star dust in the band, thickest where it is brightest: single pixels, a few brighter
    private static void dust (int[] px, int w, int h, double ca, double sa, double half) {
        java.util.Random rnd = new java.util.Random(4417L);
        int tries = w*h/240;
        double[] light = new double[2];
        for (int i = 0; i < tries; i++) {
            int x = rnd.nextInt(w), y = rnd.nextInt(h);
            light[0] = 0;
            light[1] = 0;
            band(x - w/2.0, y - h/2.0, ca, sa, half, h, light);
            if (rnd.nextDouble() > light[0]*0.9) continue;
            double b = 0.15 + 0.3*rnd.nextDouble()*rnd.nextDouble();
            int c = mix(px[y*w + x], light[1] > 0.4 ? 0xFFFFE6C0 : 0xFFDDE4FF, b);
            px[y*w + x] = c;
        }
    }

    //Black, deep violet, violet, lavender, pale: the brightness v (0 to 1), pulled towards a warm gold by warm
    private static final int[] COLD = {0xFF000000, 0xFF140F33, 0xFF2A1F5E, 0xFF473A8E, 0xFF6F64B8, 0xFFA69CE0, 0xFFE2DCFF};
    private static final int[] WARM = {0xFF000000, 0xFF2A140E, 0xFF5A2E1E, 0xFF94563A, 0xFFCC8E5C, 0xFFF0C690, 0xFFFFF0D0};

    private static int colour (double v, double warm) {
        double at = v*(COLD.length - 1);
        int i = Math.min(COLD.length - 2, (int) at);
        double f = at - i;
        int cold = mix(COLD[i], COLD[i + 1], f), hot = mix(WARM[i], WARM[i + 1], f);
        return mix(cold, hot, Math.min(1, warm*0.5));
    }

    private static int mix (int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 255)*(1 - t) + ((b >> 16) & 255)*t);
        int g = (int) Math.round(((a >> 8) & 255)*(1 - t) + ((b >> 8) & 255)*t);
        int bl = (int) Math.round((a & 255)*(1 - t) + (b & 255)*t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    //---- Noise ----

    //Smooth noise, about -0.5 to 0.5 summed over a few sizes
    private static double fbm (double x, double y) {
        double sum = 0, amp = 0.5;
        for (int o = 0; o < 4; o++) {
            sum += amp*(noise(x, y) - 0.5);
            x *= 2.03;
            y *= 2.03;
            amp *= 0.5;
        }
        return sum*1.9;
    }

    private static double noise (double x, double y) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double fx = x - ix, fy = y - iy;
        double sx = fx*fx*(3 - 2*fx), sy = fy*fy*(3 - 2*fy);
        double a = hash(ix, iy), b = hash(ix + 1, iy), c = hash(ix, iy + 1), d = hash(ix + 1, iy + 1);
        return (a + (b - a)*sx) + ((c + (d - c)*sx) - (a + (b - a)*sx))*sy;
    }

    private static double hash (int x, int y) {
        long n = x*374761393L + y*668265263L;
        n = (n ^ (n >> 13))*1274126177L;
        n ^= n >> 16;
        return (n & 0xFFFFFF)/(double) 0xFFFFFF;
    }
}
