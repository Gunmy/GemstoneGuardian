package game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import game.battleAnimation.part;
import game.battleAnimation.scene;

//The drawn kinds of a move's animation (battleAnimation.then): each one thing that draws itself in the tiles' own
//pixels (fxParticle), told in beats on the move's own clock, which starts again every LOOP seconds while its message
//is up. t0 is when in that clock a beat starts. Sizes in tiles, the art's pixels (a tile is 20 of them) where it says
//px. onTarget: on the target (T), else on the user (U).
//- lob: the rock thrown in an arc (hops: bouncing on the way), turning in quarter turns, its shadow under it
//- drop: a shadow growing on the ground, then the rock dropping onto it; streak: a rock falling slantwise out of the
//  sky with a trail of blocks behind it
//- dash: the user stepping back and lunging at the target, leaving copies of itself in the rock's colour behind
//- spin: copies of the rock going round one of them, then let go into the target; whirl: arms of blocks spiralling in
//- crack: a jagged crack running along the ground (from the user to the target, or out from one of them), glowing
//  if hot; maze: a maze carved into the ground round one of them
//- ripple: a wave running out over the tiles from one of them; crater: a ring pressed into the ground, with rubble
//- pool: a liquid spreading cell by cell on the ground, shimmering (and setting to a crust); frost: frost creeping
//  over the ground from the user to the target
//- wave: a wall of water rolling along the ground from the user to the target, leaving the ground wet
//- crystals: spikes growing out step by step (outward, cupped round, mountains, cubes, or a jaw biting shut)
//- shards: bits flying out, bouncing once on the ground and blinking out
//- ray: a stepped beam with light running along it (from the user, from the sky, or straight through the target);
//  zap: a jagged bolt flickering (forks, out of the sky)
//- column: a pillar rising out of the ground (or light coming down) on one of them
//- encase: a block (ice, glass, salt) built up over one of them from the feet; tint: one of them flashing a colour
//- stretch: one of them smeared out towards the other, twisted, or squashed flat; twin: a mirror image of one of
//  them stepping out to the side (twin); dissolve: one of them crumbling into its pixels (and coming back together)
//- peel: dark blotches lifting off one of them
//- drift: bits rising, falling or blown at the target; cloud: a churning cloud of blocks rolling onto the target (or
//  a mushroom cloud)
//- disc: the moon (the Moonstone item, glowing) or the sun (the Sunstone item) in the sky over one of them
//- lift: one of them raised off the ground and slammed down; knock: pushed back and sliding back; hide: not drawn
//- flash: the screen one colour for a moment; kick: the camera pushed away from the user; glow: a light in the dark
//- hit: what most hits do on landing: the target flashing white and the camera pushed
final class battleFx {

    private battleFx () {}

    //What a cleanse takes off (peel): dark grime, one colour family (not burn, poison and confusion's colours mixed,
    //which looked like random rainbow dots)
    static final String[] GRIME = {"#3C3634", "#504846", "#2E2A28", "#645A56"};
    //A storm cloud's greys
    static final String[] STORM = {"#3C3C3C", "#505050", "#2E2A28", "#6E6E6E"};

    //The move's clock starts again while its message stays up: REST seconds after its last beat has ended (at least
    //MIN_LOOP after it started), so each move loops on its own length (loopOf); LOOP when it has no beats
    static final double LOOP = 3.2, REST = 0.35, MIN_LOOP = 1.4;
    //Every beat, and every one of their particles (fxParticle), plays this many times as fast as written, so the
    //moves are quick and snappy; the times written in the recipes are in this faster clock
    static final double SPEED = 1.4;

    //A part that knows when it is over (end: seconds into the move's clock), so the move knows how long it is
    static final class beat implements part {
        final double end;
        final part inner;
        beat (double end, part inner) {this.end = end; this.inner = inner;}
        @Override public void spawn (scene at, double dt) {inner.spawn(at, dt);}
    }
    static part beat (double end, part inner) {return new beat(end, inner);}
    static double endOf (part p) {return p instanceof beat ? ((beat) p).end : 0;}

    //How long a move's clock runs before it starts again
    static double loopOf (java.util.List<part> parts) {
        double end = 0;
        for (part p : parts) end = Math.max(end, endOf(p));
        return end <= 0 ? LOOP : Math.max(MIN_LOOP, end + REST);
    }

    //True in the frame the clock passes t0 (once every loop)
    static boolean when (scene at, double dt, double t0) {
        double now = at.age*SPEED, prev = at.prev*SPEED;
        return Math.floor((prev - t0)/at.loop) != Math.floor((now - t0)/at.loop);
    }

    //How far into the beat from t0 the clock is now (negative before it)
    static double since (scene at, double t0) {return (at.age*SPEED) % at.loop - t0;}

    private static double side (scene at) {return Math.signum(at.target.getX() - at.user.getX()) == 0 ? 1 : Math.signum(at.target.getX() - at.user.getX());}
    private static double px (scene at) {return at.t/20;}

    //---- Drawing in the art's pixels ----

    static Color col (String hex) {return Color.web(hex);}
    static Color lighter (String hex, double k) {return Color.web(hex).interpolate(Color.WHITE, k);}
    static Color darker (String hex, double k) {return Color.web(hex).interpolate(Color.BLACK, k);}

    //A square of n art pixels centred on (wx, wy)
    static void dot (GraphicsContext gc, fxParticle p, double wx, double wy, double n) {
        double a = n/p.getHandler().getTileCount()/20/2;
        double x0 = p.sx(wx - a), x1 = p.sx(wx + a), y0 = p.sy(wy - a), y1 = p.sy(wy + a);
        gc.fillRect(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    //A rectangle in the world, in whole screen pixels
    static void box (GraphicsContext gc, fxParticle p, double wx, double wy, double ww, double wh) {
        double x0 = p.sx(wx), y0 = p.sy(wy), x1 = p.sx(wx + ww), y1 = p.sy(wy + wh);
        gc.fillRect(Math.min(x0, x1), Math.min(y0, y1), Math.max(p.pixel(), Math.abs(x1 - x0)), Math.max(p.pixel(), Math.abs(y1 - y0)));
    }

    //A blocky ellipse round (cx, cy), rows of art pixels
    static void ellipse (GraphicsContext gc, fxParticle p, double cx, double cy, double rx, double ry) {
        double a = p.getHandler() == null ? 0 : 1.0/p.getHandler().getTileCount()/20;
        if (rx <= 0 || ry <= 0) return;
        for (double dy = -ry + a/2; dy < ry; dy += a) {
            double half = rx*Math.sqrt(Math.max(0, 1 - dy*dy/(ry*ry)));
            half = Math.round(half/a)*a;
            if (half <= 0) continue;
            box(gc, p, cx - half, cy + dy - a/2, 2*half, a);
        }
    }

    //A filled triangle in art pixels
    static void tri (GraphicsContext gc, fxParticle p, double ax, double ay, double bx, double by, double cx, double cy) {
        double a = 1.0/p.getHandler().getTileCount()/20;
        double minX = Math.floor(Math.min(ax, Math.min(bx, cx))/a)*a, maxX = Math.max(ax, Math.max(bx, cx));
        double minY = Math.floor(Math.min(ay, Math.min(by, cy))/a)*a, maxY = Math.max(ay, Math.max(by, cy));
        double d = (by - cy)*(ax - cx) + (cx - bx)*(ay - cy);
        if (Math.abs(d) < 1e-12) return;
        for (double y = minY; y <= maxY; y += a) {
            double runFrom = Double.NaN;
            for (double x = minX; x <= maxX + a; x += a) {
                double qx = x + a/2, qy = y + a/2;
                double l1 = ((by - cy)*(qx - cx) + (cx - bx)*(qy - cy))/d, l2 = ((cy - ay)*(qx - cx) + (ax - cx)*(qy - cy))/d;
                boolean in = x <= maxX && l1 >= 0 && l2 >= 0 && l1 + l2 <= 1;
                if (in && Double.isNaN(runFrom)) runFrom = x;
                if (!in && !Double.isNaN(runFrom)) {
                    box(gc, p, runFrom, y, x - runFrom, a);
                    runFrom = Double.NaN;
                }
            }
        }
    }

    //A line of n art pixel squares from (ax, ay) to (bx, by)
    static void line (GraphicsContext gc, fxParticle p, double ax, double ay, double bx, double by, double n) {
        double a = 1.0/p.getHandler().getTileCount()/20;
        double len = Math.hypot(bx - ax, by - ay);
        int steps = Math.max(1, (int) Math.ceil(len/a));
        for (int i = 0; i <= steps; i++) {
            double f = i/(double) steps;
            dot(gc, p, ax + (bx - ax)*f, ay + (by - ay)*f, n);
        }
    }

    //---- The rock and the fighters as pictures ----

    static Image rock (gameHandler h, String hex) {return h.getImages().getTinted(guiSkin.ROCK_X, guiSkin.GUI_Y, hex);}

    //The rock's picture centred on (wx, wy), size tiles across, turned angle degrees
    static void drawRock (GraphicsContext gc, fxParticle p, Image img, double wx, double wy, double size, double angle) {
        double s = Math.max(2, Math.round(p.px(size)));
        gc.save();
        gc.translate(p.sx(wx), p.sy(wy));
        gc.rotate(angle);
        gc.drawImage(img, 0, 0, guiSkin.CELL, guiSkin.CELL, -s/2, -s/2, s, s);
        gc.restore();
    }

    //Where a fighter's sprite is drawn now: its top left in the world (with its nudge), a tile across
    static double spriteX (character c) {return c.getX() + c.getNudgeX();}
    static double spriteY (character c, double t) {return c.getY() + c.getNudgeY() - 0.5*t;}

    //The sprite's pixels (guiSkin.CELL square, ARGB), read once from the sheet
    private static final java.util.Map<Long, int[]> spritePixels = new java.util.HashMap<Long, int[]>();
    static int[] pixels (gameHandler h, character c) {
        long key = c.getImgX()*100000L + c.getImgY();
        int[] got = spritePixels.get(key);
        if (got != null) return got;
        int n = guiSkin.CELL;
        int[] out = new int[n*n];
        javafx.scene.image.PixelReader r = h.getImages().getImage(0).getPixelReader();
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) out[y*n + x] = r.getArgb(c.getImgX() + x, c.getImgY() + y);
        spritePixels.put(key, out);
        return out;
    }

    //---- Thrown and falling ----

    //The rock thrown from the user's hand to the target, size tiles, peak tiles over the line, in travel seconds,
    //bouncing hops times on the ground on the way (each hop half as high), low: skipping flat over the ground.
    //hops below 0: rolled along the ground instead, turning as it rolls
    static part lob (String hex, double t0, double size, double peak, double travel, int hops, boolean low) {
        return beat(t0 + travel, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            double s = side(at), t = at.t;
            double x0 = at.cx(at.user) + s*0.3*t, y0 = at.cy(at.user) - 0.1*t, x1 = at.cx(at.target), y1 = at.cy(at.target);
            double g0 = at.feetY(at.user), g1 = at.feetY(at.target);
            Image img = rock(at.handler, hex);
            double spinDir = s;
            //Drawn twice: its shadow behind the fighters (on the ground, under them), the rock in front
            for (int layer = 0; layer < 2; layer++) {
            boolean shadow = layer == 0;
            fxParticle thrown = new fxParticle(at.handler, at.world, x0, y0, x1, y1, travel, (gc, p, f, age) -> {
                if (hops < 0) {
                    double ground = g0 + (g1 - g0)*f, x = at.cx(at.user) + (x1 - at.cx(at.user))*f, y = ground - size*t*0.45;
                    if (shadow) {
                        gc.setFill(Color.color(0, 0, 0, 0.35));
                        ellipse(gc, p, x, ground, size*t*0.5, size*t*0.16);
                        return;
                    }
                    double turned = Math.abs(x - at.cx(at.user))/(Math.PI*size*t)*360*spinDir;
                    drawRock(gc, p, img, x, y, size*t, Math.floor(turned/45)*45);
                    return;
                }
                int n = hops + 1;
                double seg = Math.min(n - 1e-9, f*n);
                int k = (int) seg;
                double u = seg - k;
                double fa = k/(double) n, fb = (k + 1)/(double) n;
                double ax = x0 + (x1 - x0)*fa, bx = x0 + (x1 - x0)*fb;
                double ay = k == 0 ? y0 : g0 + (g1 - g0)*fa - size*t*0.4, by = k == n - 1 ? y1 : g0 + (g1 - g0)*fb - size*t*0.4;
                double h = (low ? 0.15 : peak)*t*Math.pow(0.5, k);
                double x = ax + (bx - ax)*u, y = ay + (by - ay)*u - 4*h*u*(1 - u);
                double ground = g0 + (g1 - g0)*f;
                double above = Math.max(0, ground - y), shrink = Math.max(0.35, 1 - above/(peak*t + 3*t)*1.5);
                if (shadow) {
                    gc.setFill(Color.color(0, 0, 0, 0.35));
                    ellipse(gc, p, x, ground, size*t*0.45*shrink, size*t*0.18*shrink);
                    return;
                }
                drawRock(gc, p, img, x, y, size*t, Math.floor(age/0.06)*90*spinDir);
            });
            if (shadow) at.handler.addBehindParticle(thrown);
            else at.handler.addFrontParticle(thrown);
            }
        });
    }

    //A shadow growing on one of them's ground for wait seconds, then the rock (size tiles) dropping onto it from far
    //above in 0.15 s
    static part drop (boolean onTarget, String hex, double t0, double wait, double size) {
        return beat(t0 + wait + 0.2, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, x = at.cx(c), ground = at.feetY(c);
            Image img = rock(at.handler, hex);
            //The shadow behind the fighters (on the ground, under them), the rock in front
            at.handler.addBehindParticle(new fxParticle(at.handler, at.world, x, ground, x, ground, wait + 0.2, (gc, p, f, age) -> {
                double grow = Math.min(1, age/wait);
                gc.setFill(Color.color(0, 0, 0, 0.25 + 0.3*grow));
                ellipse(gc, p, x, ground, size*t*(0.15 + 0.4*grow), size*t*(0.06 + 0.16*grow));
            }));
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, x, ground, x, ground, wait + 0.2, (gc, p, f, age) -> {
                if (age < wait) return;
                double u = Math.min(1, (age - wait)/0.15);
                double y = ground - size*t*0.45 - 7*t*(1 - u);
                drawRock(gc, p, img, x, y, size*t, 0);
            }));
        });
    }

    //A rock (size tiles) falling slantwise onto one of them out of the sky over travel seconds, leaving a trail of
    //blocks behind it, each smaller and darker (trail colours, light to dark)
    static part streak (boolean onTarget, String hex, double t0, double travel, double size, String[] trail) {
        return beat(t0 + travel, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, x1 = at.cx(c), y1 = at.cy(c);
            double from = at.rdm.nextBoolean() ? 1 : -1;
            double x0 = x1 + from*4*t, y0 = y1 - 8*t;
            Image img = rock(at.handler, hex);
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, x0, y0, x1, y1, travel, (gc, p, f, age) -> {
                for (int k = 9; k >= 1; k--) {
                    double fk = f - k*0.03/travel;
                    if (fk < 0) continue;
                    gc.setFill(Color.web(trail[Math.min(trail.length - 1, k*trail.length/10)]));
                    dot(gc, p, x0 + (x1 - x0)*fk, y0 + (y1 - y0)*fk, Math.max(1, Math.round(size*20*0.5*(1 - k/10.0))));
                }
                drawRock(gc, p, img, x0 + (x1 - x0)*f, y0 + (y1 - y0)*f, size*t, Math.floor(age/0.05)*45*from);
            }).light((lights, p, f, scale, strength) -> p.glow(lights, scale, x0 + (x1 - x0)*f, y0 + (y1 - y0)*f, 1.2, trail[0], 0.8*strength)));
        });
    }

    //The user stepping back a little, lunging at the target (stopping half a tile short) and stepping back, leaving
    //copies of itself in hex behind it as it goes (null: none). About 0.6 s
    static part dash (double t0, String hex) {
        double[] lastStamp = {-1};
        return beat(t0 + 0.6, (at, dt) -> {
            double tau = since(at, t0);
            if (tau < 0 || tau > 0.6) return;
            double t = at.t, s = side(at);
            double reach = Math.max(0, Math.abs(at.cx(at.target) - at.cx(at.user)) - 0.6*t);
            double off;
            if (tau < 0.1) off = -0.2*t*tau/0.1;
            else if (tau < 0.22) off = -0.2*t + (reach + 0.2*t)*Math.sin((tau - 0.1)/0.12*Math.PI/2);
            else if (tau < 0.32) off = reach;
            else off = reach*Math.cos((tau - 0.32)/0.28*Math.PI/2);
            at.user.nudge(s*off, 0);
            if (hex != null && tau >= 0.1 && tau <= 0.26 && (lastStamp[0] < 0 || at.age - lastStamp[0] >= 0.04 || at.age < lastStamp[0])) {
                lastStamp[0] = at.age;
                character u = at.user;
                double sy = spriteY(u, t);
                Image ghost = at.handler.getImages().getSilhouette(u.getImgX(), u.getImgY(), hex);
                double ox = at.user.getX() + s*off;
                at.handler.addBehindParticle(new fxParticle(at.handler, at.world, ox, sy, ox, sy, 0.16, (gc, p, f, age) -> {
                    p.alpha(gc, f < 0.33 ? 0.6 : f < 0.66 ? 0.35 : 0.15);
                    double w = p.px(t);
                    gc.drawImage(ghost, 0, 0, guiSkin.CELL, guiSkin.CELL, p.sx(ox), p.sy(sy), w, w);
                }));
            }
        });
    }

    //copies of the rock (count, size tiles) going round one of them on a flat ring radius tiles wide, faster and
    //faster, from t0 for dur seconds; then (release) let go one after the other into the target
    static part spin (boolean onTarget, String hex, double t0, double dur, int count, double size, double radius, boolean release) {
        return beat(t0 + dur + (release ? count*0.08 + 0.2 : 0), (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t;
            Image img = rock(at.handler, hex);
            double tx = at.cx(at.target), ty = at.cy(at.target);
            for (int layer = 0; layer < 2; layer++) {
                boolean front = layer == 1;
                fxParticle.painter paint = (gc, p, f, age) -> {
                    double cx = at.cx(c), cy = at.cy(c) + 0.2*t;
                    for (int i = 0; i < count; i++) {
                        double let = release ? dur + i*0.08 : 1e9;
                        double a = i*2*Math.PI/count + age*age*2.2 + age*2;
                        double r = radius*t*(0.6 + 0.4*Math.min(1, age/dur));
                        double x = cx + Math.cos(a)*r, y = cy + Math.sin(a)*r*0.45 - 0.25*t;
                        if (age >= let) {
                            double u = Math.min(1, (age - let)/0.18);
                            if (u >= 1) continue;
                            x = x + (tx - x)*u;
                            y = y + (ty - y)*u;
                            if (!front) continue;
                        } else if ((Math.sin(a) > 0) != front) continue;
                        drawRock(gc, p, img, x, y, size*t, Math.floor(age/0.06)*90);
                    }
                };
                double life = dur + (release ? count*0.08 + 0.2 : 0);
                fxParticle fp = new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), tx, ty, life, paint);
                if (front) at.handler.addFrontParticle(fp);
                else at.handler.addBehindParticle(fp);
            }
        });
    }

    //Arms (count) of 2 px blocks turning round one of them and pulled in, radius tiles, colours dark to light from
    //the outside in
    static part whirl (boolean onTarget, String[] colors, double t0, double dur, int arms, double radius) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t;
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur, (gc, p, f, age) -> {
                double env = Math.min(1, Math.min(age/0.2, (dur - age)/0.3));
                for (int arm = 0; arm < arms; arm++) {
                    for (int i = 0; i < 12; i++) {
                        double k = ((i/12.0) + age*0.9) % 1;
                        double r = radius*t*(1 - k)*env;
                        double a = arm*2*Math.PI/arms + (1 - k)*3 + age*5;
                        gc.setFill(Color.web(colors[Math.min(colors.length - 1, (int) (k*colors.length))]));
                        dot(gc, p, at.cx(c) + Math.cos(a)*r, at.cy(c) + Math.sin(a)*r*0.6, 2);
                    }
                }
            }));
        });
    }

    //---- On the ground ----

    public static final int LINE = 0, OUT_T = 1, OUT_U = 2;

    //A jagged crack growing along the ground at 8 tiles a second: LINE from the user's feet to the target's, or count
    //cracks length tiles out from the target's feet (OUT_T) or the user's (OUT_U); glow: hot (its core in hex,
    //lighting the dark along it). It stays hold seconds and goes from its start end
    static part crack (int mode, String hex, double t0, int count, double length, boolean glow, double hold) {
        boolean fromTarget = mode != LINE;
        return beat(t0 + hold + 0.6, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            double t = at.t;
            character from = mode == OUT_T ? at.target : at.user;
            double sx = at.cx(from), sy = at.feetY(from);
            double ex = at.cx(at.target), ey = at.feetY(at.target);
            for (int n = 0; n < (fromTarget ? count : 1); n++) {
                List<double[]> path = new ArrayList<double[]>();
                List<double[]> branches = new ArrayList<double[]>();
                Random r = new Random(at.rdm.nextLong());
                double x = sx, y = sy, total = 0;
                double base = fromTarget ? r.nextDouble()*2*Math.PI : 0;
                path.add(new double[] {x, y, 0});
                for (int k = 0; k < 40; k++) {
                    double toward = fromTarget ? base : Math.atan2(ey - y, ex - x);
                    double a = toward + (r.nextDouble() - 0.5)*Math.toRadians(70);
                    double len = (0.3 + r.nextDouble()*0.3)*t;
                    if (!fromTarget && Math.hypot(ex - x, ey - y) < len*1.3) {x = ex; y = ey;}
                    else {x += Math.cos(a)*len; y += Math.sin(a)*len*0.6;}
                    total += len;
                    path.add(new double[] {x, y, total});
                    if (k % 3 == 2 && r.nextBoolean()) {
                        double b = a + (r.nextBoolean() ? 1 : -1)*(0.6 + r.nextDouble()*0.5);
                        branches.add(new double[] {x, y, x + Math.cos(b)*0.25*t, y + Math.sin(b)*0.15*t, x + Math.cos(b + 0.3)*0.45*t, y + Math.sin(b + 0.3)*0.27*t, total});
                    }
                    if ((fromTarget && total >= length*t) || (!fromTarget && x == ex && y == ey)) break;
                }
                double full = total, grow = full/(8*t), life = grow + hold + 0.3;
                Color core = glow ? col(hex) : darker(hex, 0.6), edge = glow ? lighter(hex, 0.6) : lighter(hex, 0.3);
                fxParticle cr = new fxParticle(at.handler, at.world, sx, sy, ex, ey, life, (gc, p, f, age) -> {
                    double shown = Math.min(full, age*8*t), gone = Math.max(0, (age - grow - hold)/0.3*full);
                    for (int i = 1; i < path.size(); i++) {
                        double[] a0 = path.get(i - 1), a1 = path.get(i);
                        if (a0[2] > shown) break;
                        double fr = Math.min(1, (shown - a0[2])/Math.max(1e-9, a1[2] - a0[2]));
                        if (a1[2] < gone) continue;
                        double bx = a0[0] + (a1[0] - a0[0])*fr, by = a0[1] + (a1[1] - a0[1])*fr;
                        gc.setFill(edge);
                        line(gc, p, a0[0], a0[1] - t/20, bx, by - t/20, 1);
                        gc.setFill(core);
                        line(gc, p, a0[0], a0[1], bx, by, 2);
                    }
                    gc.setFill(core);
                    for (double[] b : branches) {
                        if (b[6] > shown || b[6] < gone) continue;
                        line(gc, p, b[0], b[1], b[2], b[3], 1);
                        line(gc, p, b[2], b[3], b[4], b[5], 1);
                    }
                });
                if (glow) cr.light((lights, p, f, scale, strength) -> {
                    double age = f*p.getLife(), shown = Math.min(full, age*8*t);
                    for (int i = 1; i < path.size(); i += 2) {
                        double[] a1 = path.get(i);
                        if (a1[2] > shown) break;
                        p.glow(lights, scale, a1[0], a1[1], 0.8, hex, 0.55*strength*(1 - Math.max(0, f - 0.7)/0.3));
                    }
                });
                at.handler.addBehindParticle(cr);
            }
        });
    }

    //A maze carved into the ground round one of them, passage by passage, over dur seconds (lines of hex)
    static part maze (boolean onTarget, String hex, double t0, double dur) {
        return beat(t0 + dur + 0.4, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, cell = 4*t/20;
            int w = 13, h = 9;
            double left = at.cx(c) - w*cell/2, top = at.feetY(c) - h*cell/2;
            //A random walk that carves the maze, kept as the order its passages were carved in
            Random r = new Random(at.rdm.nextLong());
            boolean[] seen = new boolean[w*h];
            List<int[]> carved = new ArrayList<int[]>();
            java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<int[]>();
            int start = (h/2)*w + w/2;
            seen[start] = true;
            stack.push(new int[] {w/2, h/2});
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!stack.isEmpty()) {
                int[] cur = stack.peek();
                List<int[]> next = new ArrayList<int[]>();
                for (int[] d : dirs) {
                    int nx = cur[0] + d[0], ny = cur[1] + d[1];
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h && !seen[ny*w + nx]) next.add(new int[] {nx, ny});
                }
                if (next.isEmpty()) {stack.pop(); continue;}
                int[] n = next.get(r.nextInt(next.size()));
                seen[n[1]*w + n[0]] = true;
                carved.add(new int[] {cur[0], cur[1], n[0], n[1]});
                stack.push(n);
            }
            Color line = col(hex), dim = darker(hex, 0.5);
            at.handler.addBehindParticle(new fxParticle(at.handler, at.world, at.cx(c), at.feetY(c), 0, 0, dur + 0.4, (gc, p, f, age) -> {
                int shown = (int) Math.min(carved.size(), carved.size()*age/(dur*0.7));
                p.alpha(gc, age > dur ? Math.max(0, 1 - (age - dur)/0.4) : 1);
                for (int i = 0; i < shown; i++) {
                    int[] e = carved.get(i);
                    gc.setFill(i > shown - 4 ? lighter(hex, 0.6) : (age > dur*0.7 ? line : dim));
                    line(gc, p, left + (e[0] + 0.5)*cell, top + (e[1] + 0.5)*cell, left + (e[2] + 0.5)*cell, top + (e[3] + 0.5)*cell, 1);
                }
            }));
        });
    }

    //A wave running out over the tiles from one of them, a tile every 0.07 s out to maxR tiles: the tiles on its
    //front lit up and lifted, the ring behind it darkened
    static part ripple (boolean onTarget, String hex, double t0, int maxR) {
        return beat(t0 + maxR*0.07 + 0.15, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t;
            long cx = (long) Math.floor(at.cx(c)/t), cy = (long) Math.floor(at.feetY(c)/t);
            Color lit = lighter(hex, 0.5);
            at.handler.addBehindParticle(new fxParticle(at.handler, at.world, at.cx(c), at.feetY(c), 0, 0, maxR*0.07 + 0.15, (gc, p, f, age) -> {
                int r = (int) (age/0.07);
                double fade = 1 - r/(double) (maxR + 1);
                for (int ring = 0; ring < 2; ring++) {
                    int rr = r - ring;
                    if (rr < 1 || rr > maxR) continue;
                    for (int dx = -rr; dx <= rr; dx++) {
                        int dy = rr - Math.abs(dx);
                        for (int sgn = -1; sgn <= 1; sgn += 2) {
                            if (dy == 0 && sgn == 1) continue;
                            double x = (cx + dx)*t, y = (cy + sgn*dy)*t;
                            if (ring == 0) {
                                p.alpha(gc, 0.35*fade);
                                gc.setFill(lit);
                                box(gc, p, x, y - 2*t/20, t, t);
                                p.alpha(gc, 0.3*fade);
                                gc.setFill(Color.BLACK);
                                box(gc, p, x, y + t - 2*t/20, t, 2*t/20);
                            } else {
                                p.alpha(gc, 0.22*fade);
                                gc.setFill(Color.BLACK);
                                box(gc, p, x, y, t, t);
                            }
                        }
                    }
                }
            }));
        });
    }

    //A ring pressed into the ground under one of them (radius tiles), with bits of rubble, fading out in steps
    static part crater (boolean onTarget, String hex, double t0, double radius) {
        return beat(t0 + 0.4, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, x = at.cx(c), y = at.feetY(c), a = t/20;
            double[][] rubble = new double[9][];
            for (int i = 0; i < rubble.length; i++) {
                double ang = at.rdm.nextDouble()*2*Math.PI, d = radius*t*(1 + at.rdm.nextDouble()*0.6);
                rubble[i] = new double[] {x + Math.cos(ang)*d, y + Math.sin(ang)*d*0.5, 1 + at.rdm.nextInt(2)};
            }
            Color dark = darker(hex, 0.55), rim = lighter(hex, 0.25);
            at.handler.addBehindParticle(new fxParticle(at.handler, at.world, x, y, x, y, 1.3, (gc, p, f, age) -> {
                p.alpha(gc, f < 0.5 ? 0.85 : f < 0.75 ? 0.5 : 0.25);
                double rx = radius*t, ry = rx*0.5;
                int steps = (int) Math.max(16, rx/a*5);
                for (int i = 0; i < steps; i++) {
                    double ang = i*2*Math.PI/steps;
                    gc.setFill(Math.sin(ang) < 0 ? dark : rim);
                    dot(gc, p, x + Math.cos(ang)*rx, y + Math.sin(ang)*ry, 2);
                }
                gc.setFill(dark);
                for (double[] b : rubble) dot(gc, p, b[0], b[1], b[2]);
            }));
        });
    }

    //A liquid spreading on the ground under one of them: up to cells cells of 4 px, a new ring every 0.04 s, in
    //colors (body, then edge), a few light pixels hopping about on it; going away edge first at the end. crust (not
    //null): it sets into that colour from the edge inward in the second half
    static part pool (boolean onTarget, String[] colors, double t0, int cells, double dur, String crust) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, cell = 4*t/20, x = at.cx(c), y = at.feetY(c);
            //The order the cells come in: a random growth out from the middle, squashed flat
            Random r = new Random(at.rdm.nextLong());
            List<int[]> order = new ArrayList<int[]>();
            Set<Long> have = new HashSet<Long>();
            List<int[]> front = new ArrayList<int[]>();
            order.add(new int[] {0, 0});
            have.add(0L);
            front.add(new int[] {0, 0});
            for (int guard = 0; order.size() < cells && !front.isEmpty() && guard < 5000; guard++) {
                int[] f0 = front.get(r.nextInt(front.size()));
                int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {-2, 0}};
                int[] d = nb[r.nextInt(nb.length)];
                int nx = f0[0] + Integer.signum(d[0]), ny = f0[1] + d[1];
                if (Math.abs(ny) > 1 + Math.abs(nx)/3 + cells/30) continue;
                long key = nx*1000L + ny;
                if (have.contains(key)) continue;
                have.add(key);
                int[] n = {nx, ny};
                order.add(n);
                front.add(n);
                if (front.size() > 12) front.remove(0);
            }
            int total = order.size();
            Color body = col(colors[0]), edge = col(colors[Math.min(1, colors.length - 1)]), shine = lighter(colors[0], 0.6);
            Color crustC = crust == null ? null : col(crust);
            fxParticle pool = new fxParticle(at.handler, at.world, x, y, x, y, dur, (gc, p, f, age) -> {
                int grown = (int) Math.min(total, 1 + age/0.04*3);
                int shrink = age > dur - 0.35 ? (int) (total*(age - (dur - 0.35))/0.35) : 0;
                int shown = Math.max(0, grown - shrink);
                Set<Long> on = new HashSet<Long>();
                for (int i = 0; i < shown; i++) on.add(order.get(i)[0]*1000L + order.get(i)[1]);
                double crustAt = crustC == null ? 2 : Math.max(0, (f - 0.45)/0.4);
                for (int i = 0; i < shown; i++) {
                    int[] q = order.get(i);
                    boolean isEdge = !on.contains((q[0] + 1)*1000L + q[1]) || !on.contains((q[0] - 1)*1000L + q[1])
                        || !on.contains(q[0]*1000L + q[1] + 1) || !on.contains(q[0]*1000L + q[1] - 1);
                    boolean set = crustC != null && i >= total*(1 - crustAt);
                    gc.setFill(set ? (isEdge ? crustC.darker() : crustC) : isEdge ? edge : body);
                    box(gc, p, x + q[0]*cell - cell/2, y + q[1]*cell - cell/2, cell, cell);
                }
                //Shimmer: a few light pixels that hop about every 0.1 s
                Random sh = new Random(p.seed + (long) (age/0.1));
                gc.setFill(shine);
                for (int i = 0; i < 3 && shown > 0; i++) {
                    int[] q = order.get(sh.nextInt(shown));
                    dot(gc, p, x + q[0]*cell + (sh.nextInt(3) - 1)*t/20, y + q[1]*cell, 1);
                }
            });
            if (crust == null) pool.light((lights, p, f, scale, strength) -> p.glow(lights, scale, x, y, 1.6, colors[0], 0.6*strength*(1 - f)));
            at.handler.addBehindParticle(pool);
        });
    }

    //Frost creeping over the ground from the user's feet to the target's over dur seconds: pale cells with a white
    //edge, a band that wanders a little
    static part frost (String hex, double t0, double dur, double hold) {
        return beat(t0 + dur + hold + 0.3, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            double t = at.t, cell = 4*t/20;
            double x0 = at.cx(at.user), y0 = at.feetY(at.user), x1 = at.cx(at.target), y1 = at.feetY(at.target);
            double len = Math.hypot(x1 - x0, y1 - y0);
            int steps = (int) Math.ceil(len/cell);
            Random r = new Random(at.rdm.nextLong());
            List<double[]> cells = new ArrayList<double[]>();
            double wander = 0;
            for (int i = 0; i <= steps; i++) {
                wander = Math.max(-2, Math.min(2, wander + r.nextInt(3) - 1));
                double fx = i/(double) steps;
                int width = 1 + r.nextInt(3);
                for (int k = -width; k <= width; k++) cells.add(new double[] {x0 + (x1 - x0)*fx, y0 + (y1 - y0)*fx + (wander + k)*cell, fx, Math.abs(k) == width ? 1 : 0});
            }
            Color pale = lighter(hex, 0.45), rim = Color.WHITE;
            at.handler.addBehindParticle(new fxParticle(at.handler, at.world, x0, y0, x1, y1, dur + hold + 0.3, (gc, p, f, age) -> {
                double reach = Math.min(1, age/dur);
                double fade = age > dur + hold ? 1 - (age - dur - hold)/0.3 : 1;
                p.alpha(gc, Math.max(0, fade)*0.85);
                for (double[] q : cells) {
                    if (q[2] > reach) continue;
                    gc.setFill(q[3] > 0 ? rim : pale);
                    box(gc, p, q[0] - cell/2, q[1] - cell/2, cell, cell);
                }
            }));
        });
    }

    //A wall of water rolling over the ground from behind the user (fromBehind) or from the user to the target in
    //travel seconds, width tiles from top to bottom: a crest of light blocks bobbing on its front, the body behind it,
    //and the ground left wet behind it, drying tile by tile
    static part wave (String[] colors, double t0, double travel, double width, boolean fromBehind, boolean wet) {
        return beat(t0 + travel + 0.2, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            double t = at.t, s = side(at);
            double x0 = fromBehind ? at.cx(at.user) - s*3*t : at.cx(at.user) + s*0.3*t, x1 = fromBehind ? at.cx(at.user) + s*0.5*t : at.cx(at.target) + s*0.4*t;
            double y = (at.feetY(at.user) + at.feetY(at.target))/2 - 0.1*t;
            Color body = col(colors[0]), deep = col(colors[Math.min(1, colors.length - 1)]), crest = lighter(colors[0], 0.6);
            double a = t/20;
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, x0, y, x1, y, travel + 0.2, (gc, p, f, age) -> {
                double u = Math.min(1, age/travel), x = x0 + (x1 - x0)*u;
                double fade = age > travel ? 1 - (age - travel)/0.2 : 1;
                p.alpha(gc, Math.max(0, fade)*0.9);
                for (double yy = -width*t/2; yy < width*t/2; yy += a*2) {
                    double bob = Math.round(Math.sin(yy/t*6 + age*14)*2)*a;
                    double frontX = x + s*bob;
                    gc.setFill(deep);
                    box(gc, p, frontX - s*0.9*t, y + yy, s*0.5*t, a*2);
                    gc.setFill(body);
                    box(gc, p, frontX - s*0.45*t, y + yy, s*0.3*t, a*2);
                    gc.setFill(crest);
                    box(gc, p, frontX - s*0.15*t, y + yy, s*0.15*t, a*2);
                }
            }));
            if (!wet) return;
            Color damp = darker(colors[0], 0.35);
            at.handler.addBehindParticle(new fxParticle(at.handler, at.world, x0, y, x1, y, travel + 1.2, (gc, p, f, age) -> {
                double u = Math.min(1, age/travel);
                int tiles = (int) Math.ceil(Math.abs(x1 - x0)/t);
                for (int i = 0; i < tiles; i++) {
                    double fi = (i + 0.5)/tiles;
                    if (fi > u) break;
                    double dry = age - fi*travel;
                    double alpha = 0.3*Math.max(0, 1 - dry/1.0);
                    if (alpha <= 0) continue;
                    p.alpha(gc, alpha);
                    gc.setFill(damp);
                    box(gc, p, s > 0 ? x0 + i*t : x0 - (i + 1)*t, y - width*t/2, t, width*t);
                }
            }));
        });
    }

    //---- Things growing ----

    public static final int OUT = 0, CUPPED = 1, MOUNTAINS = 2, CUBES = 3, JAW = 4;

    //count spikes (up to len tiles) growing out of one of them's ground step by step (2 px every 0.05 s), in hex with a
    //lit side and a dark side, for life seconds, then shrinking back. shape: OUT (fanning up and out), CUPPED (curving
    //up round them like a hand), MOUNTAINS (wide flat-topped peaks under them), CUBES (salt cubes stepping up), JAW
    //(teeth from above and below biting shut on them)
    static part crystals (boolean onTarget, String hex, double t0, int count, double len, int shape, double life) {
        return beat(t0 + life, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, x = at.cx(c), ground = at.feetY(c), mid = at.cy(c);
            Random r = new Random(at.rdm.nextLong());
            double[][] spikes = new double[count][];
            for (int i = 0; i < count; i++) {
                double bx, by, ang, l, w;
                double k = count == 1 ? 0.5 : i/(double) (count - 1);
                switch (shape) {
                    case CUPPED: bx = x + (k - 0.5)*1.4*t; by = ground; ang = -Math.PI/2 - (k - 0.5)*-1.3; l = len*(0.7 + r.nextDouble()*0.3); w = 4; break;
                    case MOUNTAINS: bx = x + (k - 0.5)*1.8*t; by = ground + 0.05*t; ang = -Math.PI/2; l = len*(0.5 + r.nextDouble()*0.5); w = 12; break;
                    case CUBES: bx = x + (k - 0.5)*1.2*t + (r.nextDouble() - 0.5)*0.2*t; by = ground + (r.nextDouble() - 0.5)*0.2*t; ang = -Math.PI/2; l = len*(0.4 + r.nextDouble()*0.6); w = 0; break;
                    case JAW: boolean top = i % 2 == 0; bx = x + ((i/2 + 0.5)/Math.max(1, count/2) - 0.5)*0.9*t; by = top ? mid - 0.9*t : mid + 0.6*t; ang = top ? Math.PI/2 : -Math.PI/2; l = len; w = 3; break;
                    default: bx = x + (r.nextDouble() - 0.5)*0.8*t; by = ground; ang = -Math.PI/2 + (r.nextDouble() - 0.5)*2.2; l = len*(0.6 + r.nextDouble()*0.4); w = 3 + r.nextInt(4); break;
                }
                spikes[i] = new double[] {bx, by, ang, l, w, r.nextDouble()};
            }
            Color body = col(hex), lit = lighter(hex, 0.5), shade = darker(hex, 0.35);
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, x, ground, x, ground, life, (gc, p, f, age) -> {
                double grow = Math.min(1, Math.floor(age/0.05)*2*t/20/(len*t));
                if (age > life - 0.25) grow *= Math.max(0, (life - age)/0.25);
                if (shape == JAW) grow = Math.min(1, age/0.25)*Math.min(1, Math.max(0, (life - age)/0.2));
                for (double[] sp : spikes) {
                    double l = sp[3]*t*grow;
                    if (l <= 0) continue;
                    double bx = sp[0], by = sp[1], ang = sp[2];
                    if (shape == CUBES) {
                        double sz = Math.round(l/(t/20))*(t/20);
                        gc.setFill(shade);
                        box(gc, p, bx - sz/2, by - sz, sz, sz);
                        gc.setFill(body);
                        box(gc, p, bx - sz/2, by - sz, sz - t/20, sz - t/20);
                        gc.setFill(lit);
                        box(gc, p, bx - sz/2, by - sz, sz - t/20, t/20);
                        continue;
                    }
                    double half = sp[4]*t/20/2;
                    double nx = -Math.sin(ang), ny = Math.cos(ang);
                    double tipX = bx + Math.cos(ang)*l, tipY = by + Math.sin(ang)*l;
                    if (shape == MOUNTAINS) {
                        //A flat top: two triangles and a box
                        gc.setFill(shade);
                        tri(gc, p, bx - half*2, by, bx + half*2, by, bx + half*0.4, by - l);
                        gc.setFill(body);
                        tri(gc, p, bx - half*2, by, bx + half*0.1, by, bx - half*0.3, by - l);
                        gc.setFill(lit);
                        box(gc, p, bx - half*0.3, by - l, half*0.7, t/20);
                        continue;
                    }
                    gc.setFill(lit);
                    tri(gc, p, bx + nx*half, by + ny*half, bx, by, tipX, tipY);
                    gc.setFill(body);
                    tri(gc, p, bx, by, bx - nx*half, by - ny*half, tipX, tipY);
                    //A smaller one out of its side once it is half grown
                    if (grow > 0.5 && sp[5] > 0.4) {
                        double ca = ang + (sp[5] > 0.7 ? 0.7 : -0.7), cl = l*0.45, mx = bx + Math.cos(ang)*l*0.45, my = by + Math.sin(ang)*l*0.45;
                        gc.setFill(shade);
                        tri(gc, p, mx - nx*half*0.6, my - ny*half*0.6, mx + nx*half*0.6, my + ny*half*0.6, mx + Math.cos(ca)*cl, my + Math.sin(ca)*cl);
                    }
                }
            }));
        });
    }

    //count bits flying out of one of them (cone: within 35 degrees of away from the user, else every way), falling,
    //bouncing once on the ground and blinking out; colors picked per bit; triangles when sharp
    static part shards (boolean onTarget, String[] colors, double t0, int count, double speed, boolean cone, boolean sharp) {
        return beat(t0 + 1.1, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, x = at.cx(c), y = at.cy(c), ground = at.feetY(c) + 0.05*t, s = side(at);
            double[][] bits = new double[count][];
            for (int i = 0; i < count; i++) {
                double ang = cone ? (s > 0 ? 0 : Math.PI) + (at.rdm.nextDouble() - 0.5)*Math.toRadians(70) - 0.4*s*0 : at.rdm.nextDouble()*2*Math.PI;
                double v = speed*t*(0.6 + at.rdm.nextDouble()*0.8);
                bits[i] = new double[] {Math.cos(ang)*v, Math.sin(ang)*v - 1.4*t, 2 + at.rdm.nextInt(3), at.rdm.nextInt(colors.length), at.rdm.nextDouble()};
            }
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, x, y, x, y, 1.1, (gc, p, f, age) -> {
                if (age > 0.7 && ((int) ((age - 0.7)/0.07)) % 2 == 1) return;
                for (double[] b : bits) {
                    //Flies, falls, bounces once off the ground at a third of its speed
                    double g = 6*t, vx = b[0], vy = b[1];
                    double hit = (-vy + Math.sqrt(vy*vy + 2*g*(ground - y)))/g;
                    double bx, by;
                    if (age < hit) {bx = x + vx*age; by = y + vy*age + g*age*age/2;}
                    else {
                        double a2 = age - hit, v2 = -(vy + g*hit)*0.35;
                        bx = x + vx*hit + vx*0.4*a2;
                        by = Math.min(ground, ground + v2*a2 + g*a2*a2/2);
                    }
                    gc.setFill(Color.web(colors[(int) b[3]]));
                    if (sharp) {
                        double sz = b[2]*t/20, ang = b[4]*6 + age*8;
                        tri(gc, p, bx + Math.cos(ang)*sz, by + Math.sin(ang)*sz, bx + Math.cos(ang + 2.4)*sz*0.6, by + Math.sin(ang + 2.4)*sz*0.6,
                            bx + Math.cos(ang - 2.4)*sz*0.6, by + Math.sin(ang - 2.4)*sz*0.6);
                    } else dot(gc, p, bx, by, b[2]);
                }
            }));
        });
    }

    //---- Beams and bolts ----

    public static final int FROM_USER = 0, FROM_SKY = 1, THROUGH = 2;

    //A stepped beam to the target (a dark edge, the body, a light core with light running along it) widthPx art
    //pixels wide, opening and closing, for dur seconds: FROM_USER (out of the user's hand), FROM_SKY (slanting down
    //from the sky over the user, where a disc would be), THROUGH (one per angle in angles, degrees off the line, going
    //on through the target to the edge of the screen)
    static part ray (String[] colors, double t0, double dur, double widthPx, int from, double... angles) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            double t = at.t, s = side(at);
            double tx = at.cx(at.target), ty = at.cy(at.target);
            double ux = at.cx(at.user) + s*0.3*t, uy = at.cy(at.user) - 0.05*t;
            if (from == FROM_SKY) {ux = at.cx(at.user) - s*0.2*t; uy = at.cy(at.user) - 1.7*t;}
            double[] offs = angles.length == 0 ? new double[] {0} : angles;
            Color runner = Color.WHITE;
            for (int ai = 0; ai < offs.length; ai++) {
                double off = offs[ai];
                //A fan of several beams takes the colours in turn; one beam its body and core from the first two
                String main = colors[offs.length > 1 ? ai % colors.length : 0];
                Color edge = darker(main, 0.35), body = col(main), core = offs.length > 1 ? lighter(main, 0.5) : col(colors[Math.min(colors.length - 1, 1)]);
                double base = Math.atan2(ty - uy, tx - ux) + Math.toRadians(off);
                double len = Math.hypot(tx - ux, ty - uy);
                double sx = from == THROUGH ? tx - Math.cos(base)*len : ux, sy = from == THROUGH ? ty - Math.sin(base)*len : uy;
                double ex = from == THROUGH ? tx + Math.cos(base)*10*t : tx, ey = from == THROUGH ? ty + Math.sin(base)*10*t : ty;
                double ox = sx, oy = sy;
                fxParticle beam = new fxParticle(at.handler, at.world, sx, sy, ex, ey, dur, (gc, p, f, age) -> {
                    double open = Math.min(1, Math.min(age/0.1, (dur - age)/0.15));
                    double w = Math.max(1, Math.round(widthPx*open + ((int) (age*25) % 2 == 0 ? 0 : (widthPx > 3 ? 1 : 0))));
                    gc.setFill(edge);
                    line(gc, p, ox, oy, ex, ey, w);
                    gc.setFill(body);
                    line(gc, p, ox, oy, ex, ey, Math.max(1, w - 2));
                    gc.setFill(core);
                    line(gc, p, ox, oy, ex, ey, Math.max(1, Math.min(2, w - 4)));
                    //Light running along it in 2 px dashes
                    gc.setFill(runner);
                    double L = Math.hypot(ex - ox, ey - oy), a = t/20, step = 7*a;
                    for (double d = (age*14*t) % step; d < L; d += step) {
                        double fx = d/L;
                        dot(gc, p, ox + (ex - ox)*fx, oy + (ey - oy)*fx, Math.min(2, w));
                    }
                }).light((lights, p, f, scale, strength) -> {
                    for (int i = 1; i <= 4; i++) p.glow(lights, scale, ox + (ex - ox)*i/4.0, oy + (ey - oy)*i/4.0, 1, main, 0.6*strength);
                });
                at.handler.addFrontParticle(beam);
            }
        });
    }

    //A jagged bolt (hex, thick px core) flickering to a new shape every 0.04 s for dur seconds: from the user's hand to
    //the target, or (fromSky) straight down onto them out of the sky; forks: side branches that end in the air
    static part zap (String hex, double t0, double dur, double thick, int forks, boolean fromSky) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            double t = at.t, s = side(at);
            double tx = at.cx(at.target), ty = fromSky ? at.feetY(at.target) : at.cy(at.target);
            double sx = fromSky ? tx + (at.rdm.nextDouble() - 0.5)*t : at.cx(at.user) + s*0.3*t, sy = fromSky ? ty - 7*t : at.cy(at.user) - 0.05*t;
            Color glowC = col(hex), core = Color.WHITE;
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, sx, sy, tx, ty, dur, (gc, p, f, age) -> {
                Random r = new Random(p.seed + (long) (age/0.04));
                int joints = 8;
                double[] xs = new double[joints + 1], ys = new double[joints + 1];
                double nx = -(ty - sy), ny = tx - sx, nl = Math.hypot(nx, ny);
                nx /= nl; ny /= nl;
                for (int i = 0; i <= joints; i++) {
                    double fx = i/(double) joints, j = (i == 0 || i == joints) ? 0 : (r.nextDouble() - 0.5)*0.6*t;
                    xs[i] = sx + (tx - sx)*fx + nx*j;
                    ys[i] = sy + (ty - sy)*fx + ny*j;
                }
                int[] forkAt = new int[forks];
                double[] forkX = new double[forks], forkY = new double[forks];
                for (int k = 0; k < forks; k++) {
                    forkAt[k] = 1 + r.nextInt(joints - 1);
                    double ang = Math.atan2(ty - sy, tx - sx) + (r.nextBoolean() ? 1 : -1)*(0.5 + r.nextDouble()*0.6);
                    forkX[k] = xs[forkAt[k]] + Math.cos(ang)*0.6*t;
                    forkY[k] = ys[forkAt[k]] + Math.sin(ang)*0.6*t;
                }
                for (int pass = 0; pass < 2; pass++) {
                    gc.setFill(pass == 0 ? glowC : core);
                    double n = pass == 0 ? thick + 2 : thick;
                    for (int i = 1; i <= joints; i++) line(gc, p, xs[i - 1], ys[i - 1], xs[i], ys[i], n);
                    for (int k = 0; k < forks; k++) line(gc, p, xs[forkAt[k]], ys[forkAt[k]], forkX[k], forkY[k], Math.max(1, n - 1));
                }
            }).light((lights, p, f, scale, strength) -> {
                p.glow(lights, scale, tx, ty, 2.2, hex, strength);
                p.glow(lights, scale, (sx + tx)/2, (sy + ty)/2, 1.6, hex, 0.7*strength);
            }));
        });
    }

    //A pillar on one of them, width tiles wide and up to height tiles tall, of 2 px rows (colors in turn) running
    //upward: rising out of the ground in 0.2 s and going from the bottom up at the end (fromSky: light pouring down
    //from the top of the screen onto them instead). alpha: how see-through
    static part column (boolean onTarget, String[] colors, double t0, double dur, double width, double height, boolean fromSky, double alpha) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, x = at.cx(c), ground = at.feetY(c) + 0.05*t, a = t/20;
            fxParticle col = new fxParticle(at.handler, at.world, x, ground, x, ground, dur, (gc, p, f, age) -> {
                double h = height*t*Math.min(1, age/0.2);
                double cut = age > dur - 0.25 ? h*(age - (dur - 0.25))/0.25 : 0;
                double top = ground - h, bottom = ground - cut;
                if (fromSky) {top = ground - height*t + cut; bottom = ground - height*t + h;}
                p.alpha(gc, alpha);
                double scroll = (age*10) % 2;
                for (double yy = top; yy < bottom; yy += 2*a) {
                    int row = (int) Math.floor((yy - ground)/(2*a) + scroll);
                    gc.setFill(Color.web(colors[Math.floorMod(row, colors.length)]));
                    box(gc, p, x - width*t/2, yy, width*t, Math.min(2*a, bottom - yy));
                }
                gc.setFill(lighter(colors[0], 0.6));
                box(gc, p, x - width*t/2, top, a, bottom - top);
                gc.setFill(darker(colors[0], 0.3));
                box(gc, p, x + width*t/2 - a, top, a, bottom - top);
            }).light((lights, p, f, scale, strength) -> {
                for (int i = 0; i < 3; i++) p.glow(lights, scale, x, ground - height*t*(i + 0.5)/3, width*1.8, colors[0], 0.7*strength);
            });
            at.handler.addFrontParticle(col);
        });
    }

    //A block of hex built over one of them from the feet up (a 2 px row every 0.02 s), with a light border and glare,
    //see-through at alpha, for dur seconds; then gone at once (shards on top make it shatter) or melting from the top
    static part encase (boolean onTarget, String hex, double t0, double dur, double alpha, boolean melt) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, a = t/20;
            Color body = col(hex), rim = lighter(hex, 0.6);
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur, (gc, p, f, age) -> {
                double left = spriteX(c) - 2*a, top = spriteY(c, t) - 2*a, w = t + 4*a, h = t + 4*a;
                double built = Math.min(h, Math.floor(age/0.02)*2*a);
                double melted = melt && age > dur - 0.4 ? h*(age - (dur - 0.4))/0.4 : 0;
                double from = top + h - built, to = top + h;
                from = Math.max(from, top + melted);
                if (to <= from) return;
                p.alpha(gc, alpha);
                gc.setFill(body);
                box(gc, p, left, from, w, to - from);
                p.alpha(gc, Math.min(1, alpha + 0.25));
                gc.setFill(rim);
                box(gc, p, left, from, a, to - from);
                box(gc, p, left + w - a, from, a, to - from);
                box(gc, p, left, from, w, a);
                if (built >= h) {
                    gc.setFill(Color.WHITE);
                    for (int i = 0; i < 3; i++) dot(gc, p, left + (4 + i)*a, top + (3 + i)*a + melted*0, 1);
                    for (int i = 0; i < 2; i++) dot(gc, p, left + (8 + i)*a, top + (3 + i)*a, 1);
                }
            }));
        });
    }

    //One of them in a flat colour at alpha for dur seconds (blink: on and off every 0.06 s; sweep: filling from the
    //feet up as it goes)
    static part tint (boolean onTarget, String hex, double t0, double dur, double alpha, boolean blink, boolean sweep) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t;
            Image sil = at.handler.getImages().getSilhouette(c.getImgX(), c.getImgY(), hex);
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur, (gc, p, f, age) -> {
                if (c.isVeiled()) return;
                if (blink && ((int) (age/0.06)) % 2 == 1) return;
                double x = p.sx(spriteX(c)), y = p.sy(spriteY(c, t)), w = Math.round(p.px(t));
                p.alpha(gc, alpha*(dur > 0.3 ? Math.min(1, (dur - age)/0.15) : 1));
                if (sweep) {
                    double shown = Math.round(Math.min(1, f*1.6)*guiSkin.CELL);
                    if (shown <= 0) return;
                    double sy = guiSkin.CELL - shown;
                    gc.drawImage(sil, 0, sy, guiSkin.CELL, shown, x, y + w*sy/guiSkin.CELL, w, w*shown/guiSkin.CELL);
                } else gc.drawImage(sil, 0, 0, guiSkin.CELL, guiSkin.CELL, x, y, w, w);
            }));
        });
    }

    public static final int SMEAR = 0, TWIST = 1, SQUASH = 2;

    //One of them drawn bent out of shape for dur seconds (its real sprite hidden meanwhile): SMEAR (pulled out in
    //strips towards the other one, snapping back), TWIST (each row swung side to side, a corkscrew), SQUASH (pressed
    //flat on the ground, popping back up)
    static part stretch (boolean onTarget, double t0, double dur, int mode) {
        return beat(t0 + dur, (at, dt) -> {
            character c = at.who(onTarget), other = at.who(!onTarget);
            if (!when(at, dt, t0)) return;
            double t = at.t, a = t/20;
            Random r = new Random(at.rdm.nextLong());
            double[] pull = new double[guiSkin.CELL];
            for (int i = 0; i < pull.length; i++) pull[i] = 0.3 + 0.7*r.nextDouble();
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur, (gc, p, f, age) -> {
                Image sheet = at.handler.getImages().getImage(0);
                double x = spriteX(c), y = spriteY(c, t);
                int n = guiSkin.CELL;
                double rowH = p.px(t)/n;
                if (mode == SQUASH) {
                    double sq = age < dur - 0.15 ? 0.2 : 0.2 + 1.0*Math.sin((age - (dur - 0.15))/0.15*Math.PI*0.75)/0.7;
                    double w = p.px(t)*(1 + (1 - Math.min(1, sq))*0.4), h = p.px(t)*Math.min(1.15, sq);
                    gc.drawImage(sheet, c.getImgX(), c.getImgY(), n, n, Math.round(p.sx(x + t/2) - w/2), Math.round(p.sy(y + t) - h), Math.round(w), Math.round(h));
                    return;
                }
                double dir = Math.signum(other.getX() - c.getX());
                for (int row = 0; row < n; row++) {
                    double off;
                    if (mode == TWIST) off = Math.round(Math.sin(row*0.45 + age*12)*3)*a;
                    else {
                        //Out over 70% of the time, then snapping back past its place and settling
                        double out = f < 0.7 ? f/0.7 : -0.15*Math.sin((f - 0.7)/0.3*Math.PI);
                        off = dir*Math.round(out*pull[row]*2.2*t/a)*a;
                    }
                    double top = p.sy(y), sy = Math.round(top + row*rowH), w = Math.round(p.px(t)), h = Math.max(1, Math.round(top + (row + 1)*rowH) - sy);
                    double sx = p.sx(x);
                    if (mode == SMEAR && Math.abs(off) > 0) {
                        //The strip from its place to where it is pulled to, the row's middle pixel stretched over it
                        double from = Math.min(sx + w/2, sx + w/2 + p.px(off)), len = Math.abs(p.px(off));
                        gc.drawImage(sheet, c.getImgX() + n/2, c.getImgY() + row, 1, 1, from, sy, len, h);
                    }
                    gc.drawImage(sheet, c.getImgX(), c.getImgY() + row, n, 1, Math.round(sx + p.px(off)), sy, w, h);
                }
            }).veils(c));
        });
    }

    //Smoothstep: 0 to 1 with no jump in speed at either end
    private static double smooth (double x) {return x*x*(3 - 2*x);}

    //One of them with bits (a few rows at a time) drawn out towards the other one, reach tiles at most, and eased
    //back in while other bits go out, on a smooth noise over rows and time. Not a beat: it goes on for as long as the
    //move plays, without starting again with the move's clock, and eases back into shape when the move is over
    //(the same stand-in is kept alive each frame, see held)
    static part noodle (boolean onTarget, double reach) {
        java.util.Map<double[], fxParticle> held = new java.util.WeakHashMap<double[], fxParticle>();
        //How long the stand-in lives on after the move's last frame, easing back into shape meanwhile
        final double tail = 0.35;
        return (at, dt) -> {
            character c = at.who(onTarget), other = at.who(!onTarget);
            fxParticle kept = held.get(at.carry);
            if (kept != null && !kept.isDead()) {
                kept.liveUntil(kept.getAge() + tail);
                return;
            }
            double t = at.t, a = t/20;
            int n = guiSkin.CELL;
            long seed = at.rdm.nextLong();
            double[] pull = new double[n];
            Random r = new Random(seed);
            for (int i = 0; i < n; i++) pull[i] = 0.75 + 0.25*r.nextDouble();
            fxParticle made = new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, tail, (gc, p, f, age) -> {
                Image sheet = at.handler.getImages().getImage(0);
                double x = spriteX(c), y = spriteY(c, t);
                double rowH = p.px(t)/n;
                double dir = Math.signum(other.getX() - c.getX());
                //In over its first quarter second, out over the tail once the move is no longer keeping it alive
                double env = smooth(Math.max(0, Math.min(1, Math.min(age/0.25, (p.getLife() - age)/tail))));
                for (int row = 0; row < n; row++) {
                    //Only the high parts of the noise are out, so a few bits stretch at a time
                    double v = noise(seed, row/3.0, age/0.3);
                    double out = smooth(Math.max(0, Math.min(1, (v - 0.5)/0.35)));
                    double off = dir*Math.round(out*env*pull[row]*reach*t/a)*a;
                    double top = p.sy(y), sy = Math.round(top + row*rowH), w = Math.round(p.px(t)), h = Math.max(1, Math.round(top + (row + 1)*rowH) - sy);
                    double sx = p.sx(x);
                    if (Math.abs(off) > 0) {
                        //The strip from its place to where it is pulled to, the row's middle pixel stretched over it
                        double from = Math.min(sx + w/2, sx + w/2 + p.px(off)), len = Math.abs(p.px(off));
                        gc.drawImage(sheet, c.getImgX() + n/2, c.getImgY() + row, 1, 1, from, sy, len, h);
                    }
                    gc.drawImage(sheet, c.getImgX(), c.getImgY() + row, n, 1, Math.round(sx + p.px(off)), sy, w, h);
                }
            }).veils(c);
            held.put(at.carry, made);
            at.handler.addFrontParticle(made);
        };
    }

    //Smooth value noise in two dimensions (0 to 1): a random value at every whole (x, y), blended smoothly between
    private static double noise (long seed, double x, double y) {
        long x0 = (long) Math.floor(x), y0 = (long) Math.floor(y);
        double fx = smooth(x - x0), fy = smooth(y - y0);
        double v00 = hash(seed, x0, y0), v10 = hash(seed, x0 + 1, y0), v01 = hash(seed, x0, y0 + 1), v11 = hash(seed, x0 + 1, y0 + 1);
        return (v00*(1 - fx) + v10*fx)*(1 - fy) + (v01*(1 - fx) + v11*fx)*fy;
    }
    private static double hash (long seed, long x, long y) {
        long h = seed ^ (x*0x9E3779B97F4A7C15L) ^ (y*0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33;
        return (h >>> 11)/(double) (1L << 53);
    }

    //A mirror image of one of them in hex (turned round left to right, see-through) sliding out to the side by
    //offset tiles and back again (or, shatter, staying out and gone) over dur seconds
    static part twin (boolean onTarget, String hex, double t0, double dur, double offset) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t;
            Image img = c.getTintedIcon(hex);
            double out = at.rdm.nextBoolean() ? 1 : -1;
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur, (gc, p, f, age) -> {
                double slide = Math.sin(Math.min(1, f)*Math.PI);
                double x = p.sx(spriteX(c) + out*offset*t*slide), y = p.sy(spriteY(c, t)), w = Math.round(p.px(t));
                p.alpha(gc, 0.7);
                gc.drawImage(img, 0, 0, guiSkin.CELL, guiSkin.CELL, x + w, y, -w, w);
            }));
        });
    }

    public static final int UP = 0, DOWN = 1, TOWARD = 2;

    //One of them crumbling into its own pixels over dur seconds (its sprite hidden): the pixels lift off one by one
    //and drift UP, fall DOWN into a heap, or fly TOWARD the other one, fading; reform: in the second half they come
    //back to their places. hex (not null): the pixels in that colour
    static part dissolve (boolean onTarget, double t0, double dur, int way, boolean reform, String hex) {
        return beat(t0 + dur, (at, dt) -> {
            character c = at.who(onTarget), other = at.who(!onTarget);
            if (!when(at, dt, t0)) return;
            double t = at.t, a = t/20;
            int[] px = pixels(at.handler, c);
            int n = guiSkin.CELL;
            List<double[]> dots = new ArrayList<double[]>();
            Random r = new Random(at.rdm.nextLong());
            for (int i = 0; i < px.length; i++) {
                if ((px[i] >>> 24) < 128) continue;
                dots.add(new double[] {i % n, i / n, r.nextDouble()*0.45, (r.nextDouble() - 0.5), r.nextDouble(), px[i]});
            }
            Color fixed = hex == null ? null : col(hex);
            double ox = spriteX(c), oy = spriteY(c, t), tx = at.cx(other), ty = at.cy(other);
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur, (gc, p, f, age) -> {
                double half = reform ? dur/2 : dur;
                double phase = reform && age > half ? (dur - age)/half : age/half;
                for (double[] d : dots) {
                    double left = (phase - d[2])/0.55;
                    double x = ox + (d[0] + 0.5)*a, y = oy + (d[1] + 0.5)*a;
                    if (left > 0) {
                        double m = Math.min(1, left);
                        if (way == UP) {x += d[3]*0.6*t*m; y -= (0.6 + d[4])*t*m;}
                        else if (way == DOWN) {x += d[3]*0.5*t*m; y = Math.min(oy + t - a*(1 + d[4]*3), y + 1.2*t*m*m);}
                        else {x += (tx - x)*m; y += (ty - y)*m - Math.sin(m*Math.PI)*0.4*t*d[4];}
                        if (way != DOWN && m >= 1 && !reform) continue;
                    }
                    int argb = (int) (long) d[5];
                    gc.setFill(fixed != null && left > 0 ? fixed : Color.rgb((argb >> 16) & 255, (argb >> 8) & 255, argb & 255));
                    p.alpha(gc, left > 0 && way != DOWN ? Math.max(0.2, 1 - Math.min(1, left)*0.7) : 1);
                    dot(gc, p, x, y, 1);
                }
            }).veils(c));
        });
    }


    //Dark blotches (colors, one per blotch) showing on one of them and then lifting off one by one, fluttering away
    //sideways in 1 px steps and drifting up (or falling, down) as they shrink
    static part peel (boolean onTarget, String[] colors, double t0, double dur, boolean down) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, a = t/20;
            int count = 5;
            double[][] b = new double[count][];
            for (int i = 0; i < count; i++) b[i] = new double[] {(4 + at.rdm.nextInt(12))*a, (3 + at.rdm.nextInt(14))*a, 0.25 + i*0.12*dur/1.2, 3 + at.rdm.nextInt(3), at.rdm.nextBoolean() ? 1 : -1};
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur, (gc, p, f, age) -> {
                double x0 = spriteX(c), y0 = spriteY(c, t);
                for (int i = 0; i < count; i++) {
                    double[] q = b[i];
                    double show = Math.min(1, age/0.2), lift = Math.max(0, age - q[2]);
                    double size = Math.max(0, q[3] - lift*4);
                    if (size < 1 || show <= 0) continue;
                    double x = x0 + q[0] + q[4]*Math.round(Math.sin(lift*14)*1.5 + lift*4)*a, y = y0 + q[1] + (down ? 1 : -1)*lift*0.9*t;
                    p.alpha(gc, show*0.85);
                    gc.setFill(darker(colors[i % colors.length], 0.25));
                    dot(gc, p, x, y, Math.round(size));
                    gc.setFill(col(colors[i % colors.length]));
                    dot(gc, p, x - a, y - a, Math.max(1, Math.round(size) - 2));
                }
            }));
        });
    }

    //Bits (2 to 4 px, colors) for dur seconds, rate a second (never more than about 40 at once): UP rising off one of
    //them, DOWN falling on them from above, TOWARD blown from the user at the target in a widening cone
    static part drift (boolean onTarget, String[] colors, double t0, double dur, int way, double rate) {
        return beat(t0 + dur + 0.6, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, s = side(at);
            Random r = new Random(at.rdm.nextLong());
            int count = (int) Math.min(rate*dur, 40/0.6*dur);
            double[][] bits = new double[count][];
            for (int i = 0; i < count; i++) {
                double born = r.nextDouble()*dur*0.85, life = way == TOWARD ? 0.4 + r.nextDouble()*0.2 : 0.6 + r.nextDouble()*0.6;
                bits[i] = new double[] {born, life, r.nextDouble() - 0.5, r.nextDouble(), 2 + r.nextInt(3), r.nextInt(colors.length), r.nextDouble()};
            }
            double ux = at.cx(at.user) + s*0.3*t, uy = at.cy(at.user), tx = at.cx(at.target), ty = at.cy(at.target);
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur + 1.2, (gc, p, f, age) -> {
                double cx = at.cx(c), cy = at.cy(c);
                for (double[] b : bits) {
                    double u = (age - b[0])/b[1];
                    if (u < 0 || u > 1) continue;
                    double x, y;
                    if (way == UP) {x = cx + b[2]*0.8*t + Math.round(Math.sin(u*9 + b[6]*6))*t/20; y = cy + 0.35*t - u*(0.8 + b[3])*t;}
                    else if (way == DOWN) {x = cx + b[2]*1.0*t; y = cy - 1.2*t + u*1.6*t;}
                    else {
                        double spread = b[2]*0.9*t*u;
                        x = ux + (tx - ux)*u*1.1;
                        y = uy + (ty - uy)*u + spread;
                    }
                    gc.setFill(Color.web(colors[(int) b[5]]));
                    p.alpha(gc, u > 0.7 ? (1 - u)/0.3 : 1);
                    dot(gc, p, x, y, b[4]);
                }
            }));
        });
    }

    public static final int ENGULF = 0, ROLL_IN = 1, MUSHROOM = 2, ABOVE = 3;

    //A churning cloud of blocks (6 to 12 px, colors, the edge ones lighter) for dur seconds: ENGULF puffing out of the
    //user and drifting onto the target, ROLL_IN rolling in from the side onto the target, MUSHROOM rising between
    //them on a stalk (a mushroom cloud), ABOVE rolling in over the target's head (a storm cloud). hide the target
//while it is engulfed
    static part cloud (String[] colors, double t0, double dur, int way, double size) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            double t = at.t, s = side(at);
            int count = 16;
            double[][] sq = new double[count][];
            for (int i = 0; i < count; i++) {
                double ang = at.rdm.nextDouble()*2*Math.PI, d = Math.sqrt(at.rdm.nextDouble());
                sq[i] = new double[] {Math.cos(ang)*d*0.6*size, Math.sin(ang)*d*0.4*size, 6 + at.rdm.nextInt(7), at.rdm.nextDouble()*6, d, at.rdm.nextInt(colors.length)};
            }
            double ux = at.cx(at.user), uy = at.cy(at.user), tx = at.cx(at.target), ty = at.cy(at.target);
            double mx = (ux + tx)/2, my = (at.feetY(at.user) + at.feetY(at.target))/2;
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, tx, ty, ux, uy, dur, (gc, p, f, age) -> {
                double x, y;
                double thin = age > dur - 0.4 ? (age - (dur - 0.4))/0.4 : 0;
                if (way == MUSHROOM) {
                    double rise = Math.min(1, age/0.6);
                    x = mx; y = my - (0.4 + 1.6*rise)*t;
                    //The stalk
                    gc.setFill(Color.web(colors[0]));
                    for (double yy = y; yy < my; yy += 2*t/20) box(gc, p, x - 0.15*t - Math.round(Math.sin(yy/t*5 + age*6))*t/20, yy, 0.3*t, 2*t/20);
                } else if (way == ROLL_IN || way == ABOVE) {
                    double u = Math.min(1, age/0.5);
                    x = tx + s*(1 - u)*4*t; y = way == ABOVE ? ty - 1.9*t : ty;
                } else {
                    double u = Math.min(1, age/0.6);
                    x = ux + (tx - ux)*u; y = uy + (ty - uy)*u;
                }
                double grow = Math.min(1, age/0.3);
                for (int i = 0; i < count; i++) {
                    double[] q = sq[i];
                    if (i < count*thin) continue;
                    double wob = age*2 + q[3];
                    double qx = x + (q[0] + Math.cos(wob)*0.08)*t*grow, qy = y + (q[1] + Math.sin(wob)*0.06)*t*grow;
                    gc.setFill(q[4] > 0.7 ? lighter(colors[(int) q[5]], 0.3) : Color.web(colors[(int) q[5]]));
                    dot(gc, p, qx, qy, Math.round(q[2]*grow));
                }
            }));
        });
    }

    public static final int MOON = 0, SUN = 1;

    //The Moonstone (with a soft glow round it, pulsing) or the Sunstone item in the sky over one of them (radius tiles)
    //for dur seconds, rising into place; phases: no longer drawn (the moon is the Moonstone item now)
    static part disc (boolean onTarget, int kind, double t0, double dur, double radius, int phases) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            double t = at.t, s = side(at), a = t/20;
            //The moon a little to the side, the sun straight over them
            double x = at.cx(c) - (kind == SUN ? 0 : s*0.2*t), y = at.cy(c) - 1.7*t;
            String hex = kind == SUN ? gemstones.SUN_COLOR : gemstones.MOON_COLOR;
            Color halo = lighter(hex, 0.4);
            //The Sunstone or Moonstone item, drawn as in the inventory; the moon with a soft glow round it, pulsing
            item stone = at.handler.getItemRegistry() == null ? null : at.handler.getItemRegistry().make(kind == SUN ? "Sunstone" : "Moonstone");
            if (stone == null) return;
            fxParticle d = new fxParticle(at.handler, at.world, x, y, x, y, dur, (gc, p, f, age) -> {
                double rise = Math.min(1, age/0.35), fade = Math.max(0, Math.min(1, (dur - age)/0.25));
                double yy = y + (1 - rise)*0.8*t;
                double r = radius*t;
                if (kind == MOON) {
                    double pulse = 1 + 0.08*Math.sin(age*5);
                    gc.setFill(halo);
                    for (int k = 3; k >= 1; k--) {
                        p.alpha(gc, fade*0.16);
                        ellipse(gc, p, x, yy, r*(0.9 + 0.35*k)*pulse, r*(0.9 + 0.35*k)*pulse);
                    }
                }
                p.alpha(gc, fade);
                double size = Math.round(p.px(2.4*r));
                stone.drawIcon(gc, Math.round(p.sx(x) - size/2), Math.round(p.sy(yy) - size/2), size);
            }).light((lights, p, f, scale, strength) -> p.glow(lights, scale, x, y, radius*6, hex, strength));
            at.handler.addFrontParticle(d);
        });
    }

    //---- Moving the fighters ----

    //One of them raised up tiles off the ground in 0.25 s, held for hang seconds, and slammed down in 0.08 s; its
    //shadow stays on the ground, smaller the higher it is
    static part lift (boolean onTarget, double t0, double up, double hang) {
        return beat(t0 + 0.33 + hang, (at, dt) -> {
            character c = at.who(onTarget);
            double tau = since(at, t0), t = at.t;
            if (tau < 0 || tau > 0.33 + hang) return;
            double h;
            if (tau < 0.25) h = Math.sin(tau/0.25*Math.PI/2);
            else if (tau < 0.25 + hang) h = 1 + 0.06*Math.sin((tau - 0.25)*20);
            else h = Math.max(0, 1 - (tau - 0.25 - hang)/0.08);
            c.nudge(0, -h*up*t);
            if (when(at, dt, t0)) {
                double x = at.cx(c), ground = at.feetY(c);
                at.handler.addBehindParticle(new fxParticle(at.handler, at.world, x, ground, x, ground, 0.33 + hang, (gc, p, f, age) -> {
                    double hh = age < 0.25 ? Math.sin(age/0.25*Math.PI/2) : age < 0.25 + hang ? 1 : Math.max(0, 1 - (age - 0.25 - hang)/0.08);
                    gc.setFill(Color.color(0, 0, 0, 0.35));
                    ellipse(gc, p, x, ground, t*0.4*(1 - 0.45*hh), t*0.15*(1 - 0.45*hh));
                }));
            }
        });
    }

    //One of them pushed away from the other by far tiles in 0.08 s and sliding back over back seconds
    static part knock (boolean onTarget, double t0, double far, double back) {
        return beat(t0 + 0.08 + back, (at, dt) -> {
            character c = at.who(onTarget), o = at.who(!onTarget);
            double tau = since(at, t0);
            if (tau < 0 || tau > 0.08 + back) return;
            double d = tau < 0.08 ? tau/0.08 : 1 - (tau - 0.08)/back;
            c.nudge(Math.signum(c.getX() - o.getX())*d*far*at.t, 0);
        });
    }

    //One of them not drawn from t0 for dur seconds (hidden in a cloud)
    static part hide (boolean onTarget, double t0, double dur) {
        return beat(t0 + dur, (at, dt) -> {
            double tau = since(at, t0);
            if (!when(at, dt, t0)) return;
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(at.who(onTarget)), at.cy(at.who(onTarget)), 0, 0, dur, (gc, p, f, age) -> {})
                .veils(at.who(onTarget)));
        });
    }

    //---- The screen ----

    //The whole screen hex, swelling to alpha and fading again over 0.35 s
    static part flash (String hex, double t0, double alpha) {
        return beat(t0 + 0.35, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            Color c = col(hex);
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(at.target), at.cy(at.target), 0, 0, 0.35, (gc, p, f, age) -> {
                gc.setGlobalAlpha(alpha*Math.sin(Math.min(1, f)*Math.PI));
                gc.setFill(c);
                gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
            }));
        });
    }

    //The camera pushed away from the user by hard tiles at t0, easing back over 0.3 s
    static part kick (double t0, double hard) {
        return beat(t0 + 0.3, (at, dt) -> {
            double tau = since(at, t0);
            if (tau < 0 || tau > 0.3) return;
            double k = hard*at.t*(1 - tau/0.3);
            at.handler.getCamera().shake(side(at)*k, k*0.3);
        });
    }

    //A light in the dark on one of them, radius tiles, for dur seconds: pulsing pulses times, or flickering
    static part glow (boolean onTarget, String hex, double t0, double dur, double radius, int pulses, boolean flicker) {
        return beat(t0 + dur, (at, dt) -> {
            if (!when(at, dt, t0)) return;
            character c = at.who(onTarget);
            at.handler.addFrontParticle(new fxParticle(at.handler, at.world, at.cx(c), at.cy(c), 0, 0, dur, (gc, p, f, age) -> {})
                .light((lights, p, f, scale, strength) -> {
                    double env = Math.min(1, Math.min(f/0.1, (1 - f)/0.2));
                    double k = pulses > 0 ? 0.6 + 0.4*Math.abs(Math.sin(Math.PI*pulses*f)) : 1;
                    if (flicker) k *= 0.9 + 0.1*((int) (f*p.getLife()/0.05) % 2);
                    p.glow(lights, scale, at.cx(c), at.cy(c), radius*k, hex, env*strength);
                }));
        });
    }

    //A soft light on both fighters for as long as the move plays (added to every move of a dark mood), so they are
    //never lost in the dark: a new light every 0.5 s living 1 s, the one going out under the next (lights take the
    //brighter, so it never blinks)
    static part aura (String hex) {
        return (at, dt) -> {
            if (Math.floor(at.prev/0.5) == Math.floor(at.age/0.5)) return;
            boolean first = at.age < 0.3;
            scene s = at;
            for (character c : new character[] {at.user, at.target}) {
                at.handler.addFrontParticle(new fxParticle(at.handler, at.world, s.cx(c), s.cy(c), 0, 0, 1.0, (gc, p, f, age) -> {})
                    .light((lights, p, f, scale, strength) -> {
                        double env = Math.min(first ? Math.min(1, f/0.3) : 1, Math.min(1, (1 - f)/0.5));
                        p.glow(lights, scale, s.cx(c), s.cy(c), 1.7, hex, 0.75*env*strength);
                    }));
            }
        };
    }

    //What a hit does: the target white for a moment and the camera pushed away from the user
    static part hit (double t0) {
        part white = tint(true, "#FFFFFF", t0, 0.07, 0.9, false, false), push = kick(t0, 0.06);
        return beat(t0 + 0.3, (at, dt) -> {
            white.spawn(at, dt);
            push.spawn(at, dt);
        });
    }

    //One of them flashing through colors in turn, each for 0.06 s, from t0 for dur seconds
    static part tints (boolean onTarget, String[] colors, double t0, double dur, double alpha) {
        int n = (int) Math.max(1, Math.round(dur/0.06));
        part[] each = new part[n];
        for (int i = 0; i < n; i++) each[i] = tint(onTarget, colors[i % colors.length], t0 + i*0.06, 0.06, alpha, false, false);
        return all(each);
    }

    //Several parts as one
    static part all (part... parts) {
        double end = 0;
        for (part p : parts) end = Math.max(end, endOf(p));
        return beat(end, (at, dt) -> {for (part p : parts) p.spawn(at, dt);});
    }
}
