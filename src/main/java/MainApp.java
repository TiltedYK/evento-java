import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // ── Start at LOGIN, which routes to Admin or Front ──
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));

        Scene scene = new Scene(root, 1240, 760);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/front-light.css").toExternalForm()); // light mode by default

        stage.setTitle("EVENTO — Login");
        stage.setScene(scene);
        stage.setMinWidth(1050);
        stage.setMinHeight(640);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
