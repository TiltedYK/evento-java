package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.User;
import service.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private VBox loginCard;
    @FXML private MediaView bgVideo;
    @FXML private Label clockLabel;

    private UserService userService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userService = new UserService();

        // Start background video
        setupVideo();

        // Animate card entrance
        loginCard.setOpacity(0);
        loginCard.setTranslateY(30);
        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(700), loginCard);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(700), loginCard);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, slide).play();
        });
        delay.play();

        // Live clock
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        // Enter key submits login
        passwordField.setOnAction(e -> handleLogin());
    }

    private void setupVideo() {
        try {
            URL videoUrl = getClass().getResource("/video/music.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                player.setMute(true);
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setAutoPlay(true);
                bgVideo.setMediaPlayer(player);
            }
        } catch (Exception e) {
            // Video not found — background CSS fallback kicks in
            System.err.println("Login bg video not found: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            shakeAndError("⚠  Fill in both fields.");
            return;
        }

        try {
            List<User> users = userService.recuperer();
            User matched = users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email)
                          && u.getPassword().equals(password)
                          && u.getBanned() == 0
                          && u.getDeleted() == 0)
                .findFirst()
                .orElse(null);

            if (matched == null) {
                shakeAndError("⛔  Invalid credentials. Try again.");
                return;
            }

            // Route by role
            boolean isAdmin = matched.getRoles() != null
                && matched.getRoles().contains("ROLE_ADMIN");

            // Fade out then switch scene
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400),
                loginBtn.getScene().getRoot());
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(evt -> {
                try {
                    if (isAdmin) {
                        navigateTo("/fxml/Dashboard.fxml", matched, "EVENTO — Admin Panel");
                    } else {
                        navigateTo("/fxml/FrontDashboard.fxml", matched, "EVENTO — Live Experience");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            fadeOut.play();

        } catch (SQLException ex) {
            shakeAndError("🔴  Database error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath, User user, String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        // Pass user to next controller if it supports it
        Object controller = loader.getController();

        Scene scene = new Scene(root, 1240, 760);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        if (controller instanceof FrontDashboardController front) {
            front.setCurrentUser(user);
            // Light mode by default; ThemeManager manages toggling
            scene.getStylesheets().add(getClass().getResource("/css/front-light.css").toExternalForm());
            front.initTheme(scene);
        } else if (controller instanceof DashboardController dash) {
            dash.setCurrentUser(user);
            scene.getStylesheets().add(getClass().getResource("/css/front-styles.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/css/front-light.css").toExternalForm());
        }

        Stage stage = (Stage) loginBtn.getScene().getWindow();
        stage.setTitle(title);
        stage.setScene(scene);

        // Fade in new scene
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void shakeAndError(String msg) {
        errorLabel.setText(msg);

        // Shake animation on card
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), loginCard);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setFromX(-8);
        shake.setToX(8);
        shake.play();

        // Flash error red briefly
        loginCard.getStyleClass().add("login-card-error");
        PauseTransition clearError = new PauseTransition(Duration.millis(400));
        clearError.setOnFinished(e -> loginCard.getStyleClass().remove("login-card-error"));
        clearError.play();
    }
}
