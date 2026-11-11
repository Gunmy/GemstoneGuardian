package game;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class app extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        guiText.loadFont();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Game.fxml"));
        Parent root = loader.load();
        controller game = loader.getController();
        Scene scene = new Scene(root);
        
        scene.getStylesheets().add(getClass().getResource("media/style.css").toExternalForm());
        primaryStage.setTitle("Gemstone Guardian");
        primaryStage.setScene(scene);
        //Closing the window saves the campaign being played
        primaryStage.setOnCloseRequest(e -> game.shutdown());
        primaryStage.show();

    }

}

