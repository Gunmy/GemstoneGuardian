package game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

//Draws battleIntro over the whole screen: the world darkens a little, the two fighters, giant and bouncing as they do
//in a box (character.draw), turned a little towards each other (rotated about their feet, the tops towards the
//middle), each with their name over their head, and VS between them
public class guiBattleIntro extends guiElement {

    //The fighter's size: this share of the screen height, at most this share of its width
    private static final double HEIGHT_SHARE = 0.78, WIDTH_SHARE = 0.42;
    //Where each one's middle is, as a share of the width from its own side
    private static final double MIDDLE_SHARE = 0.26;
    //How many degrees each one is turned, the top towards the middle
    private static final double TURN = 6;
    private static final double DARKEN = 0.4;
    private static final Color VS = Color.web("#FFD040");

    public guiBattleIntro (gui gui) {
        super(gui);
    }

    @Override
    public void layout (double screenWidth, double screenHeight) {
        setBounds(0, 0, screenWidth, screenHeight);
        battleIntro intro = battleIntro.current();
        gameHandler handler = gui.getHandler();
        //A 1v1's intro waits for the fight's game to zoom in from space, and leaves as it does
        if (intro != null && intro.waiting() && handler.getBattle().isActive() && (!handler.inSpaceTrip() || handler.zoomingIn())) {
            intro.landing();
        }
    }

    @Override
    public void draw (GraphicsContext gc) {
        battleIntro intro = battleIntro.current();
        if (intro == null) return;
        double s = gui.s();
        double risen = intro.risen(), gone = intro.gone();
        double shown = risen*(1 - gone);

        gc.setFill(Color.rgb(0, 0, 0, DARKEN*shown));
        gc.fillRect(0, 0, width, height);

        double size = Math.round(Math.min(height*HEIGHT_SHARE, width*WIDTH_SHARE));
        //Feet a little above the bottom, coming up from below the screen
        double rest = height - size - 0.02*height;
        double y = rest + (1 - risen)*(height - rest + 0.05*height);
        //Out at the sides: far enough that all of it is off the screen
        double out = gone*(width*MIDDLE_SHARE + size);
        drawFighter(gc, intro.getLeft(), width*MIDDLE_SHARE - out, y, size, TURN, s);
        drawFighter(gc, intro.getRight(), width*(1 - MIDDLE_SHARE) + out, y, size, -TURN, s);

        //VS pops in once they are up, and fades as they go
        double pop = Math.max(0, Math.min(1, (intro.time() - battleIntro.RISE*0.6)/0.2));
        if (pop > 0 && shown > 0) {
            double big = 64*s*(1 + 0.6*(1 - pop));
            double vy = height*0.5 - guiText.height(big)/2;
            gc.setGlobalAlpha(Math.min(pop, 1 - gone));
            shadowed(gc, "VS", width/2, vy, big, VS, 4*s);
            gc.setGlobalAlpha(1);
        }
    }

    //middle is the fighter's middle across, top its top. turn is in degrees, clockwise (> 0 turns the top to the right)
    private void drawFighter (GraphicsContext gc, character fighter, double middle, double top, double size, double turn, double s) {
        if (fighter == null) return;
        double feet = top + size;
        gc.save();
        gc.setImageSmoothing(false);
        //Turned about the feet, so they stay put and the head goes in
        gc.translate(middle, feet);
        gc.rotate(turn);
        fighter.draw(gc, gui, -size/2, -size, size);
        gc.restore();

        double nameSize = 26*s;
        double ny = top - guiText.height(nameSize) - 4*s;
        //Over the turned head: the top moved in by the turn
        shadowed(gc, fighter.getName(), middle + Math.sin(Math.toRadians(turn))*size, ny, nameSize, Color.WHITE, 3*s);
    }

    private static void shadowed (GraphicsContext gc, String text, double x, double y, double size, Color color, double drop) {
        guiText.draw(gc, text, x + drop, y + drop, size, Color.rgb(0, 0, 0, 0.75), TextAlignment.CENTER);
        guiText.draw(gc, text, x, y, size, color, TextAlignment.CENTER);
    }
}
