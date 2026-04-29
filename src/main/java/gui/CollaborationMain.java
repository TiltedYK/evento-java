package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class CollaborationMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));
        primaryStage.setTitle("Collaboration Management Module");
        primaryStage.setScene(new Scene(root, 1024, 768));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
