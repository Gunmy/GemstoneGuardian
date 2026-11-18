package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

//A solid beam of light coming straight down out of the sky onto a spot (battleAnimation.skyBeam, the sunbeam): a
//column from far above the screen down to (x, y), in layers from the outside in: a faint wide glow, the beam's colour,
//and a white-hot core, with a bright splash where it lands. Each one lives a moment and many follow each other, so
//the beam flickers in width. Drawn in whole screen pixels, like the rest of the art
public class beamParticle extends baseParticle {

    private final Color glow, body;
    private final double width, top;

    //(x, y) where it lands, top how high up it starts (world units above y), width (world units) of its body
    public beamParticle (gameHandler handler, int world, double x, double y, double top, double width, double life, String glowHex, String bodyHex) {
        super(x, y, world, life, 0, width, width, 0, handler, 0, 0);
        this.top = top;
        this.width = width;
        this.glow = Color.web(glowHex);
        this.body = Color.web(bodyHex);
    }

    @Override
    protected void drawSpecial (double rendX, double rendY) {
        GraphicsContext gc = handler.getTempGc();
        double unit = handler.getUnitWidth();
        double w = width*unit, bottom = Math.round(rendY), up = Math.round(rendY - top*handler.getUnitHeight());
        double alpha = gc.getGlobalAlpha();
        //The glow round it, wide and faint
        gc.setGlobalAlpha(alpha*0.3);
        gc.setFill(glow);
        gc.fillRect(Math.round(rendX - w*0.9), up, Math.round(w*1.8), bottom - up);
        //The beam
        gc.setGlobalAlpha(alpha*0.85);
        gc.setFill(body);
        gc.fillRect(Math.round(rendX - w/2), up, Math.round(w), bottom - up);
        //The white-hot core
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.WHITE);
        gc.fillRect(Math.round(rendX - w*0.18), up, Math.max(1, Math.round(w*0.36)), bottom - up);
        //Where it lands: a bright splash, wider than the beam
        gc.setGlobalAlpha(alpha*0.6);
        gc.setFill(body);
        gc.fillRect(Math.round(rendX - w*1.2), Math.round(bottom - w*0.3), Math.round(w*2.4), Math.round(w*0.45));
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.WHITE);
        gc.fillRect(Math.round(rendX - w*0.6), Math.round(bottom - w*0.2), Math.round(w*1.2), Math.max(1, Math.round(w*0.2)));
        gc.setGlobalAlpha(alpha);
    }
}
