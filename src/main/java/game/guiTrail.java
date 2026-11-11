package game;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

//Liquid poured out behind an item that is moved on the cursor (item.trail).
//
//Physics: the liquid is a few hundred particles laid along the path the item takes. They fall with gravity and keep
//a share of the swing, and they hold together like a liquid: particles near each other pull together (surface
//tension) and push apart when they get too close, with a little viscosity on their relative speed. So a thrown stream
//stays one body, stretches, necks and pinches off into drops that wobble as they fall.
//
//Drawing: not the particles but the surface they make (metaballs). Over the box around the liquid, in chunky pixels
//to match the pixel art, every pixel adds up how much liquid is near it; where that passes a threshold is liquid.
//The outline is dark, except where it faces up and left, where it catches the light; just inside the top surface is
//a lighter band, and the body is slightly see-through. Particles fade by giving less liquid, so the stream thins out
public class guiTrail {

    private static class drop {
        double x, y, vx, vy, age, life;
    }

    private List<drop> drops = new ArrayList<drop>();
    private item item;
    private int body, bodyLight, outline, rim;
    //radius is how far one particle's liquid reaches, in screen pixels
    private double radius, life, gravity;
    private Random rdm = new Random();
    private double lastX, lastY, lastVx, lastVy;
    private boolean hasLast = false;

    private static final int MAX_DROPS = 360;
    private static final double THRESHOLD = 0.55;
    //The most pixels the liquid is worked out in per frame
    private static final double MAX_CELLS = 150_000;

    //width is how wide the stream is in screen pixels, gravity in pixels per second squared
    public guiTrail (item item, Color color, double width, double life, double gravity) {
        this.item = item;
        this.radius = width*0.75;
        this.life = life;
        this.gravity = gravity;
        this.body = argb(color, 0.82);
        this.bodyLight = argb(color.interpolate(Color.WHITE, 0.3), 0.88);
        this.outline = argb(color.darker().darker(), 1);
        this.rim = argb(color.interpolate(Color.WHITE, 0.75), 1);
    }

    private static int argb (Color c, double alpha) {
        return ((int) Math.round(alpha*255) << 24) | ((int) Math.round(c.getRed()*255) << 16)
            | ((int) Math.round(c.getGreen()*255) << 8) | (int) Math.round(c.getBlue()*255);
    }

    public item getItem () {return this.item;}

    //The item is at (x, y) moving (vx, vy). start breaks the stream (after a pause); otherwise particles fill in from
    //the last place, so a fast swing leaves no gaps
    public void add (double x, double y, double vx, double vy, boolean start) {
        if (start || !hasLast) {
            lastX = x;
            lastY = y;
            lastVx = vx;
            lastVy = vy;
            hasLast = true;
        }
        double spacing = Math.max(1, radius*0.3);
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(x - lastX, y - lastY)/spacing));
        for (int i = 1; i <= steps && drops.size() < MAX_DROPS; i++) {
            double t = i/(double) steps;
            drop d = new drop();
            d.x = lastX + (x - lastX)*t + (rdm.nextDouble() - 0.5)*radius*0.3;
            d.y = lastY + (y - lastY)*t + (rdm.nextDouble() - 0.5)*radius*0.3;
            //Each keeps a different share of the swing, so the stream stretches and pulls apart
            double share = 0.3 + rdm.nextDouble()*0.3;
            d.vx = (lastVx + (vx - lastVx)*t)*share + (rdm.nextDouble() - 0.5)*radius*2;
            d.vy = (lastVy + (vy - lastVy)*t)*share + (rdm.nextDouble() - 0.5)*radius*2;
            d.life = life*(0.8 + rdm.nextDouble()*0.4);
            drops.add(d);
        }
        lastX = x;
        lastY = y;
        lastVx = vx;
        lastVy = vy;
    }

    //Particles bucketed by where they are, so each only looks at its neighbours
    private Map<Long, List<drop>> buckets (double size) {
        Map<Long, List<drop>> map = new HashMap<Long, List<drop>>();
        for (drop d : drops) map.computeIfAbsent(key((int) Math.floor(d.x/size), (int) Math.floor(d.y/size)), k -> new ArrayList<drop>()).add(d);
        return map;
    }

    private static long key (int bx, int by) {return ((long) bx << 32) ^ (by & 0xFFFFFFFFL);}

    public void next (double deltaTime) {
        if (drops.isEmpty() || deltaTime <= 0) return;
        double dt = Math.min(deltaTime, 0.04);

        //Holding together: pull towards neighbours within reach, push away from ones too close, and even out speeds
        double reach = radius*1.3, rest = radius*0.45;
        double pull = 18, push = 60, viscosity = 2.5;
        Map<Long, List<drop>> map = buckets(reach);
        for (drop a : drops) {
            int bx = (int) Math.floor(a.x/reach), by = (int) Math.floor(a.y/reach);
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    List<drop> near = map.get(key(bx + ox, by + oy));
                    if (near == null) continue;
                    for (drop b : near) {
                        if (b == a) continue;
                        double dx = b.x - a.x, dy = b.y - a.y;
                        double dist = Math.hypot(dx, dy);
                        if (dist >= reach || dist < 0.001) continue;
                        double nx = dx/dist, ny = dy/dist;
                        double force = dist < rest ? -push*(rest - dist)/rest : pull*(dist - rest)/reach*(1 - dist/reach);
                        a.vx += nx*force*radius*dt;
                        a.vy += ny*force*radius*dt;
                        a.vx += (b.vx - a.vx)*viscosity*dt*(1 - dist/reach);
                        a.vy += (b.vy - a.vy)*viscosity*dt*(1 - dist/reach);
                    }
                }
            }
        }

        double drag = Math.exp(-0.8*dt);
        for (drop d : drops) {
            d.age += dt;
            d.vx *= drag;
            d.vy = d.vy*drag + gravity*dt;
            d.x += d.vx*dt;
            d.y += d.vy*dt;
        }
        drops.removeIf(d -> d.age >= d.life);
    }

    public boolean isEmpty () {return drops.isEmpty();}

    //How much liquid one particle gives: all of it while young, less and less over the last half of its life
    private double weight (drop d) {
        double left = 1 - d.age/d.life;
        return Math.min(1, left*2);
    }

    private WritableImage image;
    private int[] pixels = new int[0];
    private float[] field = new float[0];

    public void draw (GraphicsContext gc) {
        if (drops.isEmpty()) return;
        //One drawn pixel of the liquid is this many screen pixels
        int cell = Math.max(2, (int) Math.round(radius/6));

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (drop d : drops) {
            minX = Math.min(minX, d.x);
            minY = Math.min(minY, d.y);
            maxX = Math.max(maxX, d.x);
            maxY = Math.max(maxY, d.y);
        }
        double margin = radius + 2*cell;
        //Only what is on screen is worked out: a wild swing throws drops far off the edges, and the box around all of
        //them would get huge
        double screenW = gc.getCanvas().getWidth(), screenH = gc.getCanvas().getHeight();
        minX = Math.max(minX - margin, -cell);
        minY = Math.max(minY - margin, -cell);
        maxX = Math.min(maxX + margin, screenW + cell);
        maxY = Math.min(maxY + margin, screenH + cell);
        if (maxX <= minX || maxY <= minY) return;
        //Never skipped: if the liquid still covers a very large area, it is drawn in bigger pixels for the moment
        double area = (maxX - minX)*(maxY - minY);
        if (area/(cell*cell) > MAX_CELLS) cell = (int) Math.ceil(Math.sqrt(area/MAX_CELLS));
        int left = (int) Math.floor(minX/cell), top = (int) Math.floor(minY/cell);
        int w = (int) Math.ceil(maxX/cell) - left, h = (int) Math.ceil(maxY/cell) - top;
        if (w <= 0 || h <= 0) return;

        //The field: each particle adds (1 - d²/r²)² within its radius
        if (field.length < w*h) field = new float[w*h];
        java.util.Arrays.fill(field, 0, w*h, 0f);
        double r2 = radius*radius;
        int reachCells = (int) Math.ceil(radius/cell);
        for (drop d : drops) {
            double wgt = weight(d);
            if (wgt <= 0) continue;
            int cx = (int) Math.floor(d.x/cell) - left, cy = (int) Math.floor(d.y/cell) - top;
            for (int y = Math.max(0, cy - reachCells); y <= Math.min(h - 1, cy + reachCells); y++) {
                double py = (top + y + 0.5)*cell - d.y;
                for (int x = Math.max(0, cx - reachCells); x <= Math.min(w - 1, cx + reachCells); x++) {
                    double px = (left + x + 0.5)*cell - d.x;
                    double q = 1 - (px*px + py*py)/r2;
                    if (q > 0) field[y*w + x] += (float) (wgt*q*q);
                }
            }
        }

        //Shading the surface in pixel art: outline (lit on the upper left), a light band under the top, the body
        if (pixels.length < w*h) pixels = new int[w*h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y*w + x;
                if (field[i] < THRESHOLD) {
                    pixels[i] = 0;
                    continue;
                }
                boolean outLeft = x == 0 || field[i - 1] < THRESHOLD;
                boolean outUp = y == 0 || field[i - w] < THRESHOLD;
                boolean outRight = x == w - 1 || field[i + 1] < THRESHOLD;
                boolean outDown = y == h - 1 || field[i + w] < THRESHOLD;
                if (outLeft || outUp) pixels[i] = rim;
                else if (outRight || outDown) pixels[i] = outline;
                else if ((y >= 2 && field[i - 2*w] < THRESHOLD) || (x >= 2 && field[i - 2] < THRESHOLD)) pixels[i] = bodyLight;
                else pixels[i] = body;
            }
        }

        if (image == null || image.getWidth() < w || image.getHeight() < h) {
            image = new WritableImage(Math.max(w, image == null ? 0 : (int) image.getWidth()), Math.max(h, image == null ? 0 : (int) image.getHeight()));
        }
        image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), IntBuffer.wrap(pixels, 0, w*h), w);

        gc.save();
        gc.setImageSmoothing(false);
        gc.setGlobalAlpha(1);
        gc.drawImage(image, 0, 0, w, h, left*cell, top*cell, w*cell, h*cell);
        gc.restore();
    }
}
