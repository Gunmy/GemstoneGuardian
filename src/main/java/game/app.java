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
        Parent root = FXMLLoader.load(getClass().getResource("Game.fxml"));
        Scene scene = new Scene(root);
        
        scene.getStylesheets().add(getClass().getResource("media/style.css").toExternalForm());
        primaryStage.setTitle("Gemstone Guardian");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

}

