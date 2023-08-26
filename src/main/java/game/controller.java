package game;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class controller {
    @FXML private Label xyBox;
    @FXML private TextArea dialogueBox;
    @FXML private AnchorPane Pane;

    @FXML private Button button1;
    @FXML private Button button2;
    @FXML private Button button3;
    @FXML private Button button4;

    @FXML private ProgressBar progressBar1;
    @FXML private ProgressBar progressBar2;

    @FXML private ListView<item> inventory;

    @FXML private void button1click () {battleHandler.button1click();}
    @FXML private void button2click () {battleHandler.button2click();}
    @FXML private void button3click () {battleHandler.button3click();}
    @FXML private void button4click () {battleHandler.button4click();}

    @FXML private void mouseClick () {dialogueContainer.next();}


    @FXML private void keyPressed (KeyEvent e) {
             if (e.getCode() == KeyCode.W) {handler.Wpress();} 
        else if (e.getCode() == KeyCode.S) {handler.Spress();} 
        else if (e.getCode() == KeyCode.A) {handler.Apress();} 
        else if (e.getCode() == KeyCode.D) {handler.Dpress();}
        else if (e.getCode() == KeyCode.SPACE) {dialogueContainer.next();}
        else if (e.getCode() == KeyCode.M) {handler.Mpress();}
        else if (e.getCode() == KeyCode.N) {handler.Npress();}
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
        Pane.getChildren().forEach(c -> c.setViewOrder(0));
        Pane.getChildren().add(canvas);
        canvas.setViewOrder(1);

        //Tre kontrollere initialiseres
        dialogueContainer = new dialogueContainer(dialogueBox);
        battleHandler = new battle(button1, button2, button3, button4, progressBar1, progressBar2, inventory);
        handler = new gameHandler(canvas, dialogueContainer, battleHandler, new imgDict());
        
        //Game loop
        deltaT = new deltaTime();  
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            canvas.resize(Pane.getWidth(), Pane.getHeight());

            inventory.setLayoutX(Pane.getWidth()/2-inventory.getWidth()/2);
            inventory.setLayoutY(Pane.getHeight()/2-inventory.getHeight()/2);
            progressBar2.setLayoutX(Pane.getWidth()-progressBar2.getWidth()-progressBar1.getLayoutX());



            
            deltaT.next();
            handler.next(deltaT.getSeconds()); 

            if (handler.infoOn()) {
                xyBox.setText(
                    "X: " + Math.round(handler.getMainCharacter().getX()*100)/100.0 + "  \t" +
                    "Y: " + Math.round(handler.getMainCharacter().getY()*100)/100.0 + "  \t" +
                    "FPS: " + (int)(1/deltaT.getSeconds())
                );
            } else {
                xyBox.setText("");
            }
        }));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();   
    }
}
