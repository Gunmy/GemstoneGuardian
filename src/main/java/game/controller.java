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

public class controller implements gameLauncher {
    @FXML private AnchorPane Pane;

    //Everything on screen is drawn on the canvas, so the mouse goes to the GUI first and then to the game
    @FXML private void mouseClick (MouseEvent e) {
        boolean rightClick = e.getButton() == MouseButton.SECONDARY;
        if (handler.getGui().click(e.getX(), e.getY(), rightClick) || handler.inMenu()) return;
        //Then the world (opening a chest next to you), and otherwise on to the next line of dialogue
        if (!handler.clickWorld(e.getX(), e.getY())) dialogueContainer.next();
    }

    @FXML private void mouseMoved (MouseEvent e) {handler.getGui().hover(e.getX(), e.getY());}

    @FXML private void scroll (ScrollEvent e) {
        if (!handler.getGui().scroll(e.getX(), e.getY(), e.getDeltaY())) handler.zoom(e.getDeltaY());
    }

    //Letters typed, for text fields. Comes after keyPressed for the same key
    @FXML private void keyTyped (KeyEvent e) {handler.getGui().keyTyped(e.getCharacter());}

    @FXML private void keyPressed (KeyEvent e) {
        //A GUI screen that wants the keys (the main menu, a text field) gets them first
        if (handler.getGui().keyPressed(e.getCode())) return;
             if (e.getCode() == KeyCode.W) {handler.Wpress();}
        else if (e.getCode() == KeyCode.S) {handler.Spress();}
        else if (e.getCode() == KeyCode.A) {handler.Apress();}
        else if (e.getCode() == KeyCode.D) {handler.Dpress();}
        else if (e.getCode() == KeyCode.SPACE) {dialogueContainer.next();}
        else if (e.getCode() == KeyCode.E) {handler.getGui().toggleInventory();}
        else if (e.getCode() == KeyCode.I) {handler.getGui().toggleGiveItems();}
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

    private RCanvas canvas;
    private imgDict images;
    private gameHandler handler;
    private dialogueContainer dialogueContainer;
    private deltaTime deltaT;

    //A game asked for in the middle of a frame, started before the next one
    private Runnable pendingSwitch;

    @FXML protected void initialize () {
        //Bryter delvis med model-view-controller prinsippet ettersom jeg har redefinert canvas
        canvas = new RCanvas();
        canvas.setFocusTraversable(true);
        Pane.getChildren().add(canvas);

        //The pictures are loaded once and shared by every game started after
        images = new imgDict();
        start(null);

        //Game loop
        deltaT = new deltaTime();
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            if (pendingSwitch != null) {
                Runnable run = pendingSwitch;
                pendingSwitch = null;
                run.run();
            }
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

    //A whole new game: the campaign built fresh, with the save laid over it (null is the main menu)
    private void start (saveFile save) {
        dialogueContainer = new dialogueContainer();
        handler = new gameHandler(canvas, dialogueContainer, new battle(), images, this, save);
        canvas.requestFocus();
    }

    @Override
    public void openMenu () {
        pendingSwitch = () -> {
            start(null);
            handler.openFromBlack(null);
        };
    }

    @Override
    public void play (saveFile save) {
        if (save == null) throw new IllegalArgumentException("Lagringen kan ikke være null");
        pendingSwitch = () -> {
            //The menu page that was left stays on top for a moment, so it does not jump to another page
            guiElement leaving = handler.getGui().getMainMenu();
            start(save);
            handler.openFromBlack(leaving);
            //A new campaign shows up in the list right away
            if (!save.exists()) handler.saveGame();
        };
    }

    //Called when the window is closed
    public void shutdown () {
        if (handler != null) handler.saveGame();
    }
}
