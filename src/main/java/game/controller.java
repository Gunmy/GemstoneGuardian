package game;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class controller {
    @FXML private AnchorPane Pane;

    //Everything on screen is drawn on the canvas, so the mouse goes to the GUI first and then to the game
    @FXML private void mouseClick (MouseEvent e) {
        boolean rightClick = e.getButton() == MouseButton.SECONDARY;
        if (!handler.getGui().click(e.getX(), e.getY(), rightClick)) dialogueContainer.next();
    }

    @FXML private void mouseMoved (MouseEvent e) {handler.getGui().hover(e.getX(), e.getY());}

    @FXML private void scroll (ScrollEvent e) {
        if (!handler.getGui().scroll(e.getX(), e.getY(), e.getDeltaY())) handler.zoom(e.getDeltaY());
    }


    @FXML private void keyPressed (KeyEvent e) {
             if (e.getCode() == KeyCode.W) {handler.Wpress();}
        else if (e.getCode() == KeyCode.S) {handler.Spress();}
        else if (e.getCode() == KeyCode.A) {handler.Apress();}
        else if (e.getCode() == KeyCode.D) {handler.Dpress();}
        else if (e.getCode() == KeyCode.SPACE) {dialogueContainer.next();}
        else if (e.getCode() == KeyCode.E) {handler.getGui().toggleInventory();}
        else if (e.getCode() == KeyCode.ESCAPE) {handler.getGui().escape();}
        else if (e.getCode() == KeyCode.M) {handler.Mpress();}
        else if (e.getCode() == KeyCode.N) {handler.Npress();}
        else if (e.getCode() == KeyCode.C) {handler.Cpress();}
    }


    @FXML private void keyReleased (KeyEvent e) {
             if (e.getCode() == KeyCode.W) {handler.Wrelease();}
        else if (e.getCode() == KeyCode.S) {handler.Srelease();}
        else if (e.getCode() == KeyCode.A) {handler.Arelease();}
        else if (e.getCode() == KeyCode.D) {handler.Drelease();}
    }

    private gameHandler handler;
    private dialogueContainer dialogueContainer;
    private battle battleHandler;
    private deltaTime deltaT;

    @FXML protected void initialize () {
        //Bryter delvis med model-view-controller prinsippet ettersom jeg har redefinert canvas
        RCanvas canvas = new RCanvas();
        canvas.setFocusTraversable(true);
        Pane.getChildren().add(canvas);

        //Tre kontrollere initialiseres
        dialogueContainer = new dialogueContainer();
        battleHandler = new battle();
        handler = new gameHandler(canvas, dialogueContainer, battleHandler, new imgDict());

        //Game loop
        deltaT = new deltaTime();
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            canvas.resize(Pane.getWidth(), Pane.getHeight());

            String debug = handler.collidersOn() ? "" : "Colliders off";
            if (handler.infoOn()) {
                debug = "X: " + Math.round(handler.getMainCharacter().getX()*100)/100.0 + "    " +
                        "Y: " + Math.round(handler.getMainCharacter().getY()*100)/100.0 + "    " +
                        "FPS: " + (int)(1/deltaT.getSeconds()) + "    " + debug;
            }
            handler.getGui().setDebugText(debug);

            deltaT.next();
            handler.next(deltaT.getSeconds());
        }));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }
}
