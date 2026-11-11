package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

//One lightning strike on the GUI layer, made in code: a jagged line from where it starts out to a length,
//bent a random amount at every joint, sometimes with a short branch splitting off. Drawn as a glow in the
//item's colour with a white core, flickering, and gone in a fraction of a second. Shaken items give these off
//(item.shakeLightning)
public class guiLightning {

    //Each line is a list of points, x then y
    private List<double[]> lines = new ArrayList<double[]>();
    private double life, age, width;
    private Color glow;
    private Random rdm;
    //Where the main bolt ends, for the burst at the end of long strikes
    private double endX, endY;

    //From (x, y) towards angle (radians), length long, width is the core's thickness in pixels.
    //segments is how many bends the main bolt has (long strikes get more, so they stay jagged)
    public guiLightning (double x, double y, double angle, double length, double width, int segments, Color glow, Random rdm) {
        this.rdm = rdm;
        this.glow = glow;
        this.width = Math.max(1, width);
        this.life = 0.12 + rdm.nextDouble()*0.12 + (segments > 10 ? 0.1 : 0);
        bolt(x, y, angle, length, Math.max(2, segments), true);
    }

    //A strike from (x1, y1) that ends right on (x2, y2), jagged on the way, for strikes going for an item
    public static guiLightning between (double x1, double y1, double x2, double y2, double width, int segments, Color glow, Random rdm) {
        guiLightning strike = new guiLightning(glow, width, segments, rdm);
        strike.aimed(x1, y1, x2, y2, Math.max(2, segments));
        return strike;
    }

    private guiLightning (Color glow, double width, int segments, Random rdm) {
        this.rdm = rdm;
        this.glow = glow;
        this.width = Math.max(1, width);
        this.life = 0.12 + rdm.nextDouble()*0.12 + (segments > 10 ? 0.1 : 0);
    }

    //Points along the straight line pushed out sideways a random amount, the last one exactly on the end
    private void aimed (double x1, double y1, double x2, double y2, int segments) {
        double dx = x2 - x1, dy = y2 - y1;
        double length = Math.hypot(dx, dy), angle = Math.atan2(dy, dx);
        double nx = -dy/Math.max(1, length), ny = dx/Math.max(1, length);
        double[] points = new double[(segments + 1)*2];
        int branches = 0;
        for (int i = 0; i <= segments; i++) {
            double t = i/(double) segments;
            double side = (i == 0 || i == segments) ? 0 : (rdm.nextDouble() - 0.5)*length/segments*1.4;
            double px = x1 + dx*t + nx*side, py = y1 + dy*t + ny*side;
            points[2*i] = px;
            points[2*i + 1] = py;
            if (i > 0 && i < segments - 1 && branches < 2 && rdm.nextDouble() < 0.2) {
                branches++;
                double branchAngle = angle + (rdm.nextBoolean() ? 1 : -1)*(0.5 + rdm.nextDouble()*0.6);
                bolt(px, py, branchAngle, length*(0.15 + rdm.nextDouble()*0.2), 2 + rdm.nextInt(3), false);
            }
        }
        lines.add(points);
        endX = x2;
        endY = y2;
    }

    public double getEndX () {return this.endX;}
    public double getEndY () {return this.endY;}

    //A jagged line of the given number of segments. The main bolt can split off up to two branches
    private void bolt (double x, double y, double angle, double length, int segments, boolean main) {
        double[] points = new double[(segments + 1)*2];
        points[0] = x;
        points[1] = y;
        double step = length/segments;
        int branches = 0;
        for (int i = 1; i <= segments; i++) {
            //Bends a lot at every joint, but keeps heading roughly the same way
            double bend = angle + (rdm.nextDouble() - 0.5)*1.6;
            x += Math.cos(bend)*step;
            y += Math.sin(bend)*step;
            points[2*i] = x;
            points[2*i + 1] = y;
            if (main && branches < 2 && i < segments - 1 && rdm.nextDouble() < 0.25) {
                branches++;
                double side = angle + (rdm.nextBoolean() ? 1 : -1)*(0.5 + rdm.nextDouble()*0.6);
                bolt(x, y, side, length*(0.25 + rdm.nextDouble()*0.25), 2 + rdm.nextInt(3), false);
            }
        }
        lines.add(points);
        if (main) {
            endX = x;
            endY = y;
        }
    }

    public void next (double deltaTime) {age += deltaTime;}

    public boolean isDead () {return age >= life;}

    public void draw (GraphicsContext gc) {
        //Flickers: some frames almost out, and fading towards the end
        double fade = 1 - age/life;
        double flicker = rdm.nextDouble() < 0.25 ? 0.3 : 1;
        double alpha = Math.max(0, fade*flicker);

        gc.save();
        gc.setLineCap(StrokeLineCap.SQUARE);
        gc.setLineJoin(StrokeLineJoin.MITER);
        for (int pass = 0; pass < 2; pass++) {
            //The glow first, wide and see-through, then the white core
            boolean core = pass == 1;
            gc.setStroke(core ? Color.WHITE : glow);
            gc.setGlobalAlpha(core ? alpha : alpha*0.55);
            gc.setLineWidth(core ? width : width*3);
            for (double[] points : lines) {
                for (int i = 0; i + 3 < points.length; i += 2) {
                    gc.strokeLine(Math.round(points[i]), Math.round(points[i+1]), Math.round(points[i+2]), Math.round(points[i+3]));
                }
            }
        }
        gc.restore();
    }
}
