package game;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;



public class shader {

    private String path = "/game/media/";
    private List<byte[]> images  = new ArrayList<byte[]>();
    private List<Integer> imagesWidth  = new ArrayList<>();
    private List<Integer> imagesHeight  = new ArrayList<>();

    private GraphicsContext gc;


    public void addImg (String name) {
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

        imagesWidth.add(width);
        imagesHeight.add(height);
        images.add(buffer);
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


    public void drawShader (int tilex, int tiley, double rx, double ry, double rw, double rh, double roundNess, double redWeight, double greenWeight,double blueWeight, double speed, boolean inverse) {
        int tilePerShader = 9;
        int shaderHeight = getImageWidth(1)/tilePerShader;
        int shaderWidth = shaderHeight;
 
        int shadertilex = (tilex)*shaderWidth;
        int shadertiley = (tiley)*shaderHeight;

        double sec = System.nanoTime()/1_000_000_000.0*speed;

        List<List<List<Double>>> lst1 = getPixels(1, 
        (int) (shadertilex+Math.cos(sec*Math.PI)*shaderWidth), 
        (int) (shadertiley+Math.sin(sec*Math.PI)*shaderHeight/2), 
        shaderWidth, 
        shaderHeight);


        List<List<List<Double>>> lst2 = getPixels(1, 
        (int) (shadertilex+(Math.sin(-sec*Math.PI)+3)*shaderWidth), 
        (int) (shadertiley+Math.cos(-sec*Math.PI)*shaderHeight/2), 
        shaderWidth, 
        shaderHeight);



        double ph = rh/shaderHeight;
        double pw = rw/shaderWidth;

        //gc.setGlobalAlpha(0.5);


        for (int h = 0; h < lst1.size(); h++) {
            for (int w = 0; w < lst1.get(h).size(); w++) {

                double val1 = lst1.get(h).get(w).get(2);
                double val2 = lst2.get(h).get(w).get(2);

                if (val1 > 150 && !inverse) val1 = 150;
                if (val2 > 150 && !inverse) val2 = 150;

                if (val1 > 200 && inverse) val1 = 200;
                if (val2 > 200 && inverse) val2 = 200;


                int val = (int) (double) (val1+val2);
                if (val < 150 && !inverse) val*=2;
                //if (val < 200) val=150;
                
                if (val < 140 && !inverse) val = 140;

                if (val > 255) val = 255;

                if (!inverse) {
                    if (val < 220) gc.setFill(Color.rgb((int) (val*redWeight), (int) (val*greenWeight), (int) (val*blueWeight), 1));
                    else           gc.setFill(Color.rgb(val, val, val, 1));
                } else {
                    if (val < 220) gc.setFill(Color.rgb((int) (255-val*redWeight), (int) (255-val*greenWeight), (int) (255-val*blueWeight), 1));
                    else           gc.setFill(Color.rgb(255-val, 255-val, 255-val, 1));
                }

                
                if (roundNess > 1) {
                    double dist = (roundNess-1)/2;
                    gc.fillRoundRect(Math.floor(rx+(w+dist)*pw), Math.floor(ry+(h+dist)*ph), Math.ceil(pw*(1-dist*2)), Math.ceil(ph*(1-dist*2)), roundNess*pw, roundNess*ph);
                
                }
                else gc.fillRoundRect(Math.floor(rx+w*pw), Math.floor(ry+h*ph), Math.ceil(pw), Math.ceil(ph), roundNess*pw, roundNess*ph);
            }
        }

        //gc.setGlobalAlpha(1);



    }


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
