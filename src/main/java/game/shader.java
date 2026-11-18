package game;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;



public class shader {

    private String path = "/game/media/";
    private List<byte[]> images  = new ArrayList<byte[]>();
    private List<Integer> imagesWidth  = new ArrayList<>();
    private List<Integer> imagesHeight  = new ArrayList<>();

    private GraphicsContext gc;


    //The pictures' pixels, decoded once and shared by every game started after (the shader only reads them)
    private static final Map<String, Object[]> decoded = new HashMap<String, Object[]>();

    public void addImg (String name) {
        Object[] img;
        synchronized (decoded) {
            img = decoded.get(name);
            if (img == null) {
                Image temp = new Image(path + name);
                int width = (int) temp.getWidth();
                int height = (int) temp.getHeight();
                byte[] buffer = new byte[width * height * 4];

                temp.getPixelReader().getPixels(
                    0,
                    0,
                    width,
                    height,
                    PixelFormat.getByteBgraInstance(),
                    buffer,
                    0,
                    width * 4
                );
                img = new Object[] {width, height, buffer};
                decoded.put(name, img);
            }
        }

        imagesWidth.add((Integer) img[0]);
        imagesHeight.add((Integer) img[1]);
        images.add((byte[]) img[2]);
    }

    public shader (GraphicsContext gc) {
        addImg("tiles.png");
        addImg("noise_texture.png");
               
        this.gc = gc;
    }

    public byte[] getImage(Integer n) {
        if (n < 0 || n >= images.size()) throw new IllegalArgumentException("Item finnes ikke");
        return images.get(n);
    }

    public Integer getImageWidth(Integer n) {
        if (n < 0 || n >= images.size()) throw new IllegalArgumentException("Item finnes ikke");
        return imagesWidth.get(n);
    }

    public Integer getImageHeight(Integer n) {
        if (n < 0 || n >= images.size()) throw new IllegalArgumentException("Item finnes ikke");
        return imagesHeight.get(n);
    }

    public List<List<List<Double>>> getPixels(int n, int x, int y, int width, int height) {
        List<List<List<Double>>> pixels = new ArrayList<List<List<Double>>>();

        byte[] img = getImage(n);

        int xs = (x+getImageWidth(n))%getImageWidth(n);
        int ys = (y+getImageHeight(n))%getImageHeight(n);


        for (int h = ys; h < ys+height; h++) {
            List<List<Double>> row = new ArrayList<List<Double>>();
            int pixely = (h*4)%(getImageHeight(n)*4);

            for (int w = xs; w < xs+width; w++) {
                List<Double> values = new ArrayList<Double>();

                for (int i = 0; i < 3; i++) {
                    int pixelx = (w*4 + i)%(getImageWidth(n)*4);
                    if (img[pixely*getImageWidth(n)+pixelx] < 0) values.add(256 + (double) img[pixely*getImageWidth(n)+pixelx]);
                    else values.add((double) img[pixely*getImageWidth(n)+pixelx]);
                }
                int pixelx = (w*4 + 3)%(getImageWidth(n)*4);
                if (img[pixely*getImageWidth(n)+pixelx] != 0) values.add(1+img[pixely*getImageWidth(n)+pixelx]/256.0);
                else values.add(0.0);                
                row.add(values);
            }
            pixels.add(row);
        }
        return pixels;
    }


    //Moving water drawn from two layers of the noise texture sliding over each other. The pattern is one piece over the
    //whole world (tile (tx, ty) shows the noise from (tx, ty) times the shader tile size on), so it repeats every 128
    //pixels of noise. It is worked out once into a small image (see fluidFrame) and every fluid tile is a single drawImage
    //of its part of it. It used to be worked out again for every tile, every frame, with a fillRoundRect per pixel:
    //196 canvas calls per water tile, which made zooming out over the sea lag badly
    public void drawShader (int tilex, int tiley, double rx, double ry, double rw, double rh, double redWeight, double greenWeight, double blueWeight, double speed, boolean inverse) {
        int noiseW = getImageWidth(1), noiseH = getImageHeight(1);
        int size = noiseW/9;
        drawFrame(fluidFrame(size, redWeight, greenWeight, blueWeight, speed, inverse), tilex, tiley, rx, ry, rw, rh);
    }

    //The tile's part of a pattern made for the whole world (one pixel per noise pixel, repeating every noise texture,
    //a shader tile wider and taller so every tile's part is in one piece), drawn with its pixels kept sharp
    private void drawFrame (WritableImage frame, int tilex, int tiley, double rx, double ry, double rw, double rh) {
        int noiseW = getImageWidth(1), noiseH = getImageHeight(1);
        int size = noiseW/9;
        boolean smoothing = gc.isImageSmoothing();
        if (smoothing) gc.setImageSmoothing(false);
        gc.drawImage(frame, Math.floorMod(tilex*size, noiseW), Math.floorMod(tiley*size, noiseH), size, size, rx, ry, rw, rh);
        if (smoothing) gc.setImageSmoothing(true);
    }

    //The water pattern for one kind of fluid as it is right now, one pixel per noise pixel. It is a shader tile wider and
    //taller than the noise (the pattern repeated round), so that every tile's part of it is in one piece. Only made again
    //when the layers have slid a whole pixel
    private static class fluidFrame {
        WritableImage image;
        int[] pixels;
        int ox1 = Integer.MIN_VALUE, oy1, ox2, oy2;
    }
    private Map<String, fluidFrame> fluidFrames = new HashMap<String, fluidFrame>();

    private WritableImage fluidFrame (int size, double redWeight, double greenWeight, double blueWeight, double speed, boolean inverse) {
        int noiseW = getImageWidth(1), noiseH = getImageHeight(1);
        String key = redWeight + "," + greenWeight + "," + blueWeight + "," + speed + "," + inverse;
        fluidFrame f = fluidFrames.get(key);
        if (f == null) {
            f = new fluidFrame();
            f.image = new WritableImage(noiseW + size, noiseH + size);
            f.pixels = new int[(noiseW + size)*(noiseH + size)];
            fluidFrames.put(key, f);
        }

        //How far each layer has slid, in whole noise pixels
        double sec = System.nanoTime()/1_000_000_000.0*speed;
        int ox1 = (int) Math.floor(Math.cos(sec*Math.PI)*size);
        int oy1 = (int) Math.floor(Math.sin(sec*Math.PI)*size/2);
        int ox2 = (int) Math.floor((Math.sin(-sec*Math.PI)+3)*size);
        int oy2 = (int) Math.floor(Math.cos(-sec*Math.PI)*size/2);
        if (ox1 == f.ox1 && oy1 == f.oy1 && ox2 == f.ox2 && oy2 == f.oy2) return f.image;
        f.ox1 = ox1; f.oy1 = oy1; f.ox2 = ox2; f.oy2 = oy2;

        byte[] noise = getImage(1);
        int w = noiseW + size, h = noiseH + size;
        for (int y = 0; y < h; y++) {
            int row1 = Math.floorMod(y + oy1, noiseH)*noiseW*4;
            int row2 = Math.floorMod(y + oy2, noiseH)*noiseW*4;
            for (int x = 0; x < w; x++) {
                //The red of the noise (the bytes are blue, green, red, alpha)
                int val1 = noise[row1 + Math.floorMod(x + ox1, noiseW)*4 + 2] & 0xFF;
                int val2 = noise[row2 + Math.floorMod(x + ox2, noiseW)*4 + 2] & 0xFF;
                f.pixels[y*w + x] = fluidColor(val1, val2, redWeight, greenWeight, blueWeight, inverse);
            }
        }
        f.image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), f.pixels, 0, w);
        return f.image;
    }


    //The third way to draw fluids: waves crossing each other, drawn in a few flat colour bands like pixel art, with bright
    //lines where the waves meet and a sparkle here and there. In an inverse fluid (lava) the same lines are glowing cracks
    //in a dark crust and the sparkles are embers. Made the same way as the shader, one image for the whole world, and
    //only made again ten times a second, which also gives it the steppy movement of drawn animation
    public void drawRipples (int tilex, int tiley, double rx, double ry, double rw, double rh, double redWeight, double greenWeight, double blueWeight, double speed, boolean inverse) {
        drawFrame(rippleFrame(redWeight, greenWeight, blueWeight, speed, inverse), tilex, tiley, rx, ry, rw, rh);
    }

    private static final int RIPPLE_STEPS_PER_SECOND = 10;

    private WritableImage rippleFrame (double redWeight, double greenWeight, double blueWeight, double speed, boolean inverse) {
        int noiseW = getImageWidth(1), noiseH = getImageHeight(1);
        int size = noiseW/9;
        String key = "ripples," + redWeight + "," + greenWeight + "," + blueWeight + "," + speed + "," + inverse;
        fluidFrame f = fluidFrames.get(key);
        if (f == null) {
            f = new fluidFrame();
            f.image = new WritableImage(noiseW + size, noiseH + size);
            f.pixels = new int[(noiseW + size)*(noiseH + size)];
            fluidFrames.put(key, f);
        }

        int step = (int) (System.nanoTime()/(1_000_000_000L/RIPPLE_STEPS_PER_SECOND));
        if (step == f.ox1) return f.image;
        f.ox1 = step;
        double t = step/(double) RIPPLE_STEPS_PER_SECOND*speed*2;

        int[] palette = ripplePalette(redWeight, greenWeight, blueWeight, inverse);
        byte[] noise = getImage(1);
        int w = noiseW + size, h = noiseH + size;
        //Whole waves over the noise texture across and down, so the pattern repeats with it and the tiles meet
        double ku = 2*Math.PI/noiseW, kv = 2*Math.PI/noiseH;
        for (int y = 0; y < h; y++) {
            double v = y*kv;
            for (int x = 0; x < w; x++) {
                double u = x*ku;
                int nx = Math.floorMod(x, noiseW), ny = Math.floorMod(y, noiseH);
                //The noise roughens the edges of the bands, so they are not smooth sine curves
                double grain = ((noise[(ny*noiseW + nx)*4 + 2] & 0xFF)/255.0 - 0.5)*0.35;

                double wave = Math.sin(2*u + v - t)
                    + 0.7*Math.sin(-u + 3*v + 1.3*t)
                    + 0.5*Math.sin(4*u - 2*v + 0.7*t)
                    + 0.25*Math.sin(u + v + 0.3*t);
                //Near zero is where the waves cancel out: the bright lines
                double ridge = Math.abs(wave + grain);

                int band;
                if (ridge < 0.12) band = 3;
                else if (ridge < 0.38) band = 2;
                else if (ridge < 1.0) band = 1;
                else band = 0;

                //A few pixels flash now and then, each at its own time
                int hash = (nx*73856093) ^ (ny*19349663);
                hash ^= hash >>> 13;
                hash *= 0x5bd1e995;
                hash ^= hash >>> 15;
                if ((hash & 0xFF) < 4 && Math.sin(t*3 + (hash >>> 8)) > 0.85) band = 4;

                f.pixels[y*w + x] = palette[band];
            }
        }
        f.image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), f.pixels, 0, w);
        return f.image;
    }

    //Five colours from dark to bright (the last is the sparkle) from the fluid's colour weights. An inverse fluid is drawn
    //255 minus the weights by the shader, so its colour is worked out the same way: a hot colour, with a dark crust under it
    private static int[] ripplePalette (double redWeight, double greenWeight, double blueWeight, boolean inverse) {
        double[][] colors;
        if (!inverse) {
            double r = redWeight*255, g = greenWeight*255, b = blueWeight*255;
            colors = new double[][] {
                {r*0.42, g*0.42, b*0.5},
                {r*0.6, g*0.6, b*0.68},
                {r*0.8, g*0.82, b*0.86},
                {r + (255-r)*0.55, g + (255-g)*0.55, b + (255-b)*0.55},
                {255, 255, 255}
            };
        } else {
            double r = 255 - 200*redWeight, g = 255 - 200*greenWeight, b = 255 - 200*blueWeight;
            colors = new double[][] {
                {r*0.32, g*0.1, b*0.08},
                {r*0.5, g*0.2, b*0.1},
                {r*0.9, g*0.7, b*0.5},
                {r, g + (255-g)*0.45, b + (255-b)*0.35},
                {255, 250, 200}
            };
        }
        int[] palette = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            palette[i] = 0xFF000000 | (clamp((int) colors[i][0]) << 16) | (clamp((int) colors[i][1]) << 8) | clamp((int) colors[i][2]);
        }
        return palette;
    }

    //The colour of one pixel of water from the two noise values under it
    private static int fluidColor (int val1, int val2, double redWeight, double greenWeight, double blueWeight, boolean inverse) {
        if (val1 > 150 && !inverse) val1 = 150;
        if (val2 > 150 && !inverse) val2 = 150;
        if (val1 > 200 && inverse) val1 = 200;
        if (val2 > 200 && inverse) val2 = 200;

        int val = val1 + val2;
        if (val < 150 && !inverse) val *= 2;
        if (val < 140 && !inverse) val = 140;
        if (val > 255) val = 255;

        int r, g, b;
        if (!inverse) {
            if (val < 220) {r = (int) (val*redWeight); g = (int) (val*greenWeight); b = (int) (val*blueWeight);}
            else           {r = val; g = val; b = val;}
        } else {
            if (val < 220) {r = (int) (255-val*redWeight); g = (int) (255-val*greenWeight); b = (int) (255-val*blueWeight);}
            else           {r = 255-val; g = 255-val; b = 255-val;}
        }
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    //Color.rgb threw on values outside 0 to 255, here they are kept inside
    private static int clamp (int v) {return v < 0 ? 0 : v > 255 ? 255 : v;}


    public void drawImg (int x, int y, int width, int height, double rx, double ry, double rw, double rh, double roundNess) {
        
        double ph = rh/height;
        double pw = rw/width;

        List<List<List<Double>>> lst = getPixels(0, x, y, width, height);
        for (int h = 0; h < lst.size(); h++) {
            for (int w = 0; w < lst.get(h).size(); w++) {
                gc.setFill(Color.rgb((int) (double) lst.get(h).get(w).get(2), (int) (double) lst.get(h).get(w).get(1), (int) (double) lst.get(h).get(w).get(0), lst.get(h).get(w).get(3)));
                
                if (roundNess > 1) {
                    double dist = (roundNess-1)/2;
                    gc.fillRoundRect(Math.floor(rx+(w+dist)*pw), Math.floor(ry+(h+dist)*ph), Math.ceil(pw*(1-dist*2)), Math.ceil(ph*(1-dist*2)), roundNess*pw, roundNess*ph);
                
                }
                else gc.fillRoundRect(Math.floor(rx+w*pw), Math.floor(ry+h*ph), Math.ceil(pw), Math.ceil(ph), roundNess*pw, roundNess*ph);
            }
        }
    }


    public void drawImg (int x, int y, int width, int height, double rx, double ry, double rw, double rh, double roundNess, double opacity) {
        gc.setGlobalAlpha(opacity);
        drawImg(x, y, width, height, rx, ry, rw, rh, roundNess);
        gc.setGlobalAlpha(1);

    }

    public void rotate (double degrees, double centerx, double centery) {
        gc.save();
        Rotate r = new Rotate(degrees, centerx, centery);
        gc.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
    }

    public void resetRotate () {
        gc.restore();
    }


}
