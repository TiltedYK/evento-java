import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import controller.LoginController;
import util.ThemeManager;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        LoginController loginCtl = loader.getController();

        Scene scene = new Scene(root, 1240, 760);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        ThemeManager.setScene(scene);
        ThemeManager.apply(ThemeManager.randomTheme());
        loginCtl.attachLoginAmbience(scene);

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
