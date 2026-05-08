package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.User;
import service.CaptchaService;
import service.GoogleAuthService;
import service.MailService;
import service.UserService;
import util.ThemeManager;

import java.net.URL;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Login screen controller.
 *
 * EVERY user-facing flow happens inline inside the login card — no JavaFX
 * {@link Dialog}s or {@link Alert}s are spawned from here, per the product
 * requirement of "never open a single extra window".
 *
 *   • Sign-in    → main loginPanel
 *   • Sign-up    → signupPanel (replaces loginPanel while open)
 *   • Forgot pw  → forgotPanel (replaces loginPanel while open). Two-step
 *                  state machine: enter email → choose delivery (email or
 *                  in-app code) → enter code + new password.
 *
 * SMTP and Google-OAuth credentials are configured in the admin
 * "Settings" tab — they are not exposed here, since they're admin
 * concerns and don't change between sessions.
 */
public class LoginController implements Initializable {

    /** Bumped on every material edit. Grep this string in the source AND
     *  watch for it in the Run console at startup to confirm you are NOT
     *  running a stale compiled build. */
    public static final String BUILD_TAG = "EVENTO_LOGIN_BUILD_2026_04_28_INLINE_RESET_V4";

    // ── Login ──────────────────────────────────────────────────────────
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginBtn;
    @FXML private Button        googleBtn;
    @FXML private Label         forgotLink;
    @FXML private Label         forgotBigLink;

    // ── Forgot password (inline panel) ─────────────────────────────────
    @FXML private VBox          forgotPanel;
    @FXML private VBox          forgotStep1;
    @FXML private VBox          forgotStep2;
    @FXML private TextField     forgotEmail;
    @FXML private TextField     forgotCode;
    @FXML private PasswordField forgotNewPwd;
    @FXML private PasswordField forgotConfirmPwd;
    @FXML private Label         forgotMailReady;
    @FXML private Label         forgotCodeBanner;
    @FXML private Label         forgotStatus;
    @FXML private Button        forgotSendByMail;
    @FXML private Button        forgotShowInApp;
    @FXML private Button        forgotResetBtn;
    private boolean forgotOpen = false;
    private String  forgotIssuedCode;     // null until step 1 succeeds
    private User    forgotMatchedUser;

    // ── Sign up (inline panel) ─────────────────────────────────────────
    @FXML private VBox      signupPanel;
    @FXML private TextField signupPrenom;
    @FXML private TextField signupNom;
    @FXML private TextField signupEmail;
    @FXML private PasswordField signupPassword;
    @FXML private TextField captchaAnswer;
    @FXML private ImageView captchaImage;
    @FXML private Label     signupError;
    @FXML private Label     signupLink;
    private boolean signupOpen = false;

    // ── Shared ────────────────────────────────────────────────────────
    @FXML private VBox    loginCard;
    @FXML private VBox    loginPanel;
    @FXML private Button  loginThemeToggle;
    @FXML private Label   clockLabel;
    @FXML private MediaView bgVideo;

    private MediaPlayer loginBgPlayer;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("[EVENTO] LoginController loaded — build=" + BUILD_TAG
                + "  gauthBuild=" + GoogleAuthService.BUILD_TAG
                + "  mailBuild=" + MailService.BUILD_TAG
                + "  smtp=" + MailService.USER_CREDENTIALS_FILE);
        animateCardIn();
        startClock();
        updateThemeIcon();
        passwordField.setOnAction(e -> handleLogin());
        forgotEmail.setOnAction(e -> showForgotCodeInApp());
        forgotConfirmPwd.setOnAction(e -> confirmForgotReset());
    }

    // ── LOGIN ─────────────────────────────────────────────────────────

    @FXML void handleLogin() {
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();
        if (email.isEmpty() || pass.isEmpty()) { error(errorLabel, "⚠  Fill in both fields."); return; }
        try {
            User matched = userService.recuperer().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(pass)
                            && u.getBanned() == 0 && u.getDeleted() == 0)
                    .findFirst().orElse(null);
            if (matched == null) { error(errorLabel, "⛔  Invalid credentials."); return; }
            routeAfterLogin(matched);
        } catch (SQLException ex) { error(errorLabel, "🔴  " + ex.getMessage()); }
    }

    // ── GOOGLE SIGN-IN ────────────────────────────────────────────────

    @FXML
    private void handleGoogleLogin() {
        errorLabel.setText("");

        if (!new GoogleAuthService().isConfigured()) {
            error(errorLabel,
                    "⚙  Google sign-in isn't configured. An admin can set it up under Settings.");
            return;
        }
        runGoogleSignIn();
    }

    private void runGoogleSignIn() {
        final String idleText = googleBtn.getText();
        googleBtn.setDisable(true);
        loginBtn.setDisable(true);
        googleBtn.setText("⏳  WAITING FOR GOOGLE…");

        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                GoogleAuthService gauth = new GoogleAuthService();
                GoogleAuthService.GoogleUser gu = gauth.signIn();
                if (gu.email == null || gu.email.isBlank()) {
                    throw new IllegalStateException("Google did not return an email address.");
                }

                List<User> users = userService.recuperer();
                User existing = users.stream()
                        .filter(u -> u.getEmail() != null
                                  && u.getEmail().equalsIgnoreCase(gu.email))
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    if (existing.getBanned() == 1)
                        throw new IllegalStateException("This account is banned.");
                    if (existing.getDeleted() == 1)
                        throw new IllegalStateException("This account has been removed.");
                    return existing;
                }

                User u = new User();
                u.setEmail(gu.email);
                u.setNom(gu.lastName != null && !gu.lastName.isBlank() ? gu.lastName : "");
                u.setPrenom(gu.firstName != null && !gu.firstName.isBlank()
                        ? gu.firstName : gu.email.split("@")[0]);
                u.setNumTelephone("");
                u.setLocalisation("");
                u.setImage(gu.picture);
                u.setRoles("[\"ROLE_USER\"]");
                u.setPassword("__GOOGLE_OAUTH__");
                u.setBanned(0);
                u.setDeleted(0);
                u.setNblogin(0);
                userService.ajouter(u);

                return userService.recuperer().stream()
                        .filter(usr -> gu.email.equalsIgnoreCase(usr.getEmail()))
                        .findFirst()
                        .orElse(u);
            }
        };

        task.setOnSucceeded(e -> {
            googleBtn.setDisable(false);
            loginBtn.setDisable(false);
            googleBtn.setText(idleText);
            routeAfterLogin(task.getValue());
        });

        task.setOnFailed(e -> {
            googleBtn.setDisable(false);
            loginBtn.setDisable(false);
            googleBtn.setText(idleText);
            Throwable ex = task.getException();
            String msg = ex != null && ex.getMessage() != null
                    ? ex.getMessage() : "Google sign-in failed.";
            error(errorLabel, "⛔  Google: " + msg);
            if (ex != null) ex.printStackTrace();
        });

        Thread t = new Thread(task, "GoogleAuthThread");
        t.setDaemon(true);
        t.start();
    }

    // ── FORGOT PASSWORD (fully inline state machine) ───────────────────

    /** Toggle the forgot-password panel; mutually exclusive with the signup panel. */
    @FXML
    void toggleForgot() {
        forgotOpen = !forgotOpen;

        // Mutually exclusive with signup
        if (forgotOpen && signupOpen) {
            signupOpen = false;
            signupPanel.setVisible(false); signupPanel.setManaged(false);
            signupLink.setText("Create an account →");
        }

        loginPanel.setVisible(!forgotOpen); loginPanel.setManaged(!forgotOpen);
        forgotPanel.setVisible(forgotOpen); forgotPanel.setManaged(forgotOpen);

        if (forgotOpen) {
            // Reset state
            forgotIssuedCode = null;
            forgotMatchedUser = null;
            forgotEmail.setDisable(false);
            forgotEmail.clear();
            forgotCode.clear();
            forgotNewPwd.clear();
            forgotConfirmPwd.clear();
            forgotCodeBanner.setText("");
            forgotStatus.setText("");
            forgotStatus.setStyle("");
            forgotStep1.setVisible(true);  forgotStep1.setManaged(true);
            forgotStep2.setVisible(false); forgotStep2.setManaged(false);
            forgotSendByMail.setDisable(false);
            forgotShowInApp.setDisable(false);

            // Tell the user what email options are available right now.
            MailService mail = new MailService();
            if (mail.isConfigured()) {
                forgotSendByMail.setDisable(false);
                String u = mail.getSettings().username;
                forgotMailReady.setText("📨  Email delivery: ready (sending from " + u + ")");
                forgotMailReady.setStyle("-fx-text-fill:#4ADE80;-fx-font-size:10px;-fx-font-family:'Courier New',monospace;");
            } else {
                forgotSendByMail.setDisable(true);
                forgotMailReady.setText("📭  Email delivery isn't configured yet (admin can set it up "
                        + "under Settings). You can still reset using \"Show code in app\".");
                forgotMailReady.setStyle("-fx-text-fill:#F59E0B;-fx-font-size:10px;-fx-font-family:'Courier New',monospace;");
            }

            forgotEmail.requestFocus();
        } else {
            errorLabel.setText("");
            emailField.requestFocus();
        }
    }

    @FXML
    void sendForgotCodeByMail() {
        if (!validateAndIssueCode()) return;
        forgotSendByMail.setDisable(true);
        forgotShowInApp.setDisable(true);
        forgotStatus.setStyle("-fx-text-fill:#9AA0B0;");
        forgotStatus.setText("📨  Sending reset code to " + forgotMatchedUser.getEmail() + "…");

        final User target = forgotMatchedUser;
        final String code = forgotIssuedCode;
        Thread t = new Thread(() -> {
            try {
                new MailService().sendPasswordResetCode(
                        target.getEmail(), target.getPrenom(), code);
                Platform.runLater(() -> revealStep2(
                        "✅  Email sent to " + target.getEmail() + ".\n"
                      + "    Open your inbox and copy the 6-digit code below.\n"
                      + "    (Tip: it might be in Spam — please check there too.)"));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    forgotSendByMail.setDisable(false);
                    forgotShowInApp.setDisable(false);
                    forgotStatus.setStyle("-fx-text-fill:#FF4422;");
                    forgotStatus.setText("📭  Could not send email: " + ex.getMessage());
                });
            }
        }, "ResetMailer");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    void showForgotCodeInApp() {
        if (!validateAndIssueCode()) return;
        revealStep2(
                "✅  Confirmation code generated.\n"
              + "    Your code is:  " + forgotIssuedCode + "\n"
              + "    Type it into the field below to reset your password.");
    }

    /**
     * Validates the email, finds the user, and stores
     * {@link #forgotMatchedUser} + {@link #forgotIssuedCode} for the
     * upcoming step-2 submission. Sets {@link #forgotStatus} on failure.
     */
    private boolean validateAndIssueCode() {
        forgotStatus.setStyle("");
        forgotStatus.setText("");
        String em = forgotEmail.getText() == null ? "" : forgotEmail.getText().trim();
        if (em.isEmpty() || !em.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            forgotStatus.setStyle("-fx-text-fill:#FF4422;");
            forgotStatus.setText("⚠  Enter a valid email address.");
            return false;
        }
        try {
            User found = userService.recuperer().stream()
                    .filter(u -> em.equalsIgnoreCase(u.getEmail())
                              && u.getDeleted() == 0 && u.getBanned() == 0)
                    .findFirst().orElse(null);
            if (found == null) {
                forgotStatus.setStyle("-fx-text-fill:#FF4422;");
                forgotStatus.setText("⚠  No active account found with that email.");
                return false;
            }
            forgotMatchedUser = found;
            forgotIssuedCode  = String.format("%06d", new SecureRandom().nextInt(1_000_000));
            return true;
        } catch (SQLException ex) {
            forgotStatus.setStyle("-fx-text-fill:#FF4422;");
            forgotStatus.setText("DB error: " + ex.getMessage());
            return false;
        }
    }

    /** Hide step-1 controls and show step-2 with the given banner text. */
    private void revealStep2(String banner) {
        forgotCodeBanner.setText(banner);
        forgotStep1.setVisible(false); forgotStep1.setManaged(false);
        forgotStep2.setVisible(true);  forgotStep2.setManaged(true);
        forgotEmail.setDisable(true);
        forgotStatus.setText("");
        forgotStatus.setStyle("");
        forgotCode.requestFocus();
    }

    /** Validate the code + new password and persist the change. */
    @FXML
    void confirmForgotReset() {
        forgotStatus.setStyle("");
        String code = forgotCode.getText() == null ? "" : forgotCode.getText().trim();
        String np   = forgotNewPwd.getText()    == null ? "" : forgotNewPwd.getText();
        String cp   = forgotConfirmPwd.getText()== null ? "" : forgotConfirmPwd.getText();

        if (forgotIssuedCode == null || !forgotIssuedCode.equals(code)) {
            forgotStatus.setStyle("-fx-text-fill:#FF4422;");
            forgotStatus.setText("⛔  Invalid or missing reset code.");
            return;
        }
        if (np.length() < 6) {
            forgotStatus.setStyle("-fx-text-fill:#FF4422;");
            forgotStatus.setText("⚠  Password must be at least 6 characters.");
            return;
        }
        if (!np.equals(cp)) {
            forgotStatus.setStyle("-fx-text-fill:#FF4422;");
            forgotStatus.setText("⚠  Passwords don't match.");
            return;
        }

        forgotResetBtn.setDisable(true);
        try {
            forgotMatchedUser.setPassword(np);
            userService.modifier(forgotMatchedUser);

            // Verify the change actually landed in the DB — this catches the
            // very nasty class of bug where modifier() silently leaves the
            // password column untouched.
            User refreshed = userService.recuperer().stream()
                    .filter(u -> u.getId() == forgotMatchedUser.getId())
                    .findFirst().orElse(null);
            if (refreshed == null || !np.equals(refreshed.getPassword())) {
                forgotResetBtn.setDisable(false);
                forgotStatus.setStyle("-fx-text-fill:#FF4422;");
                forgotStatus.setText("⛔  The DB confirmed the row exists but the password didn't update. "
                        + "(Check UserService.modifier — it must include the password column.)");
                return;
            }

            forgotStatus.setStyle("-fx-text-fill:#4ADE80;-fx-font-size:12px;");
            forgotStatus.setText("✅  Password reset! Returning to sign-in…");

            final String resetEmail = forgotMatchedUser.getEmail();
            PauseTransition wait = new PauseTransition(Duration.seconds(1.4));
            wait.setOnFinished(e -> {
                toggleForgot();
                emailField.setText(resetEmail);
                passwordField.requestFocus();
            });
            wait.play();
        } catch (SQLException ex) {
            forgotResetBtn.setDisable(false);
            forgotStatus.setStyle("-fx-text-fill:#FF4422;");
            forgotStatus.setText("DB error: " + ex.getMessage());
        }
    }

    // ── SIGN UP (expandable) ──────────────────────────────────────────

    @FXML void toggleSignup() {
        signupOpen = !signupOpen;

        // Mutually exclusive with forgot
        if (signupOpen && forgotOpen) {
            forgotOpen = false;
            forgotPanel.setVisible(false); forgotPanel.setManaged(false);
        }

        loginPanel.setVisible(!signupOpen); loginPanel.setManaged(!signupOpen);
        signupPanel.setVisible(signupOpen); signupPanel.setManaged(signupOpen);

        signupLink.setText(signupOpen ? "Already have an account? Sign in ↑" : "Create an account →");
        if (signupOpen) {
            refreshCaptcha();
            signupError.setText("");
            signupPrenom.requestFocus();
        } else {
            errorLabel.setText("");
            emailField.requestFocus();
        }
    }

    @FXML void handleSignup() {
        String prenom = signupPrenom.getText().trim();
        String nom    = signupNom.getText().trim();
        String email  = signupEmail.getText().trim();
        String pass   = signupPassword.getText();
        String answer = captchaAnswer.getText().trim();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            error(signupError, "⚠  All fields are required."); return;
        }
        if (pass.length() < 6) { error(signupError, "⚠  Password must be at least 6 characters."); return; }
        if (!CaptchaService.validate(answer)) {
            error(signupError, "🤖  Wrong CAPTCHA — please try again."); refreshCaptcha(); return;
        }
        try {
            if (userService.recuperer().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email))) {
                error(signupError, "⚠  That email is already registered."); return;
            }
            userService.ajouter(new User(nom, prenom, email, "", pass));
            signupError.setStyle("-fx-text-fill:#4ADE80;");
            signupError.setText("✅  Account created! Signing you in…");
            PauseTransition wait = new PauseTransition(Duration.millis(900));
            wait.setOnFinished(e -> {
                try {
                    User created = userService.recuperer().stream()
                            .filter(u -> u.getEmail().equalsIgnoreCase(email))
                            .findFirst().orElse(null);
                    if (created != null) routeAfterLogin(created);
                } catch (Exception ex) { error(signupError, ex.getMessage()); }
            });
            wait.play();
        } catch (SQLException ex) { error(signupError, "🔴  " + ex.getMessage()); }
    }

    @FXML void refreshCaptcha() {
        javafx.scene.image.Image img = CaptchaService.generateCaptcha();
        if (captchaImage != null && img != null) captchaImage.setImage(img);
        if (captchaAnswer != null) captchaAnswer.clear();
    }

    // ── Theme / background (music + video, same sources as main app) ───

    /** Called from {@link MainApp} after {@link ThemeManager#apply} picks the opening theme. */
    public void attachLoginAmbience(Scene scene) {
        ThemeManager.setScene(scene);
        stopLoginAmbience();
        startLoginBackgroundForCurrentTheme();
    }

    private void startLoginBackgroundForCurrentTheme() {
        stopLoginAmbience();
        try {
            if (bgVideo == null) return;
            ThemeManager.Theme t = ThemeManager.getCurrent();
            URL u = getClass().getResource(t.trackPath);
            if (u == null) return;
            Media media = new Media(u.toExternalForm());
            loginBgPlayer = new MediaPlayer(media);
            loginBgPlayer.setAutoPlay(true);
            loginBgPlayer.setMute(false);
            loginBgPlayer.setVolume(0.34);
            loginBgPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgVideo.setMediaPlayer(loginBgPlayer);
        } catch (Exception ignored) { }
    }

    private void stopLoginAmbience() {
        try {
            MediaPlayer p = loginBgPlayer;
            loginBgPlayer = null;
            if (bgVideo != null) {
                bgVideo.setMediaPlayer(null);
            }
            if (p != null) {
                p.stop();
                p.dispose();
            }
        } catch (Exception ignored) { }
    }

    @FXML void toggleLoginTheme() {
        stopLoginAmbience();
        ThemeManager.Theme t = ThemeManager.randomThemeDifferentFrom(ThemeManager.getCurrent());
        ThemeManager.apply(t);
        if (loginCard != null && loginCard.getScene() != null && loginCard.getScene().getRoot() != null) {
            loginCard.getScene().getRoot().setOpacity(1.0);
        }
        startLoginBackgroundForCurrentTheme();
    }

    private void updateThemeIcon() {
        if (loginThemeToggle != null)
            loginThemeToggle.setTooltip(new Tooltip("Random theme + track (fixes stacked audio)"));
    }

    // ── Navigation ────────────────────────────────────────────────────

    private void routeAfterLogin(User user) {
        stopLoginAmbience();
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
        FadeTransition out = new FadeTransition(Duration.millis(300), loginCard.getScene().getRoot());
        out.setToValue(0);
        out.setOnFinished(ev -> {
            try { navigateTo(isAdmin ? "/fxml/Dashboard.fxml" : "/fxml/FrontDashboard.fxml",
                    user, isAdmin ? "EVENTO — Admin Panel" : "EVENTO — Live Experience"); }
            catch (Exception e) { e.printStackTrace(); }
        });
        out.play();
    }

    private void navigateTo(String path, User user, String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
        Parent root = loader.load();
        Object ctrl = loader.getController();
        Scene scene = new Scene(root, 1240, 760);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        if (ctrl instanceof FrontDashboardController front) {
            front.setCurrentUser(user);
            // Always include the metal base — the ThemeManager will overlay kpop/chill
            // CSS on top depending on which song was randomly picked in setupAudio().
            scene.getStylesheets().add(getClass().getResource("/css/front-styles.css").toExternalForm());
            front.initTheme(scene);
        } else if (ctrl instanceof DashboardController dash) {
            dash.setCurrentUser(user);
            scene.getStylesheets().add(getClass().getResource("/css/front-styles.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/css/front-light.css").toExternalForm());
        }

        Stage stage = (Stage) loginCard.getScene().getWindow();
        stage.setTitle(title); stage.setScene(scene);
        root.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(450), root);
        in.setToValue(1); in.play();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void animateCardIn() {
        loginCard.setOpacity(0); loginCard.setTranslateY(20);
        PauseTransition d = new PauseTransition(Duration.millis(200));
        d.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(550), loginCard); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(550), loginCard);
            tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
        });
        d.play();
    }

    private void startClock() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        t.setCycleCount(Animation.INDEFINITE); t.play();
    }

    private void error(Label lbl, String msg) {
        lbl.setStyle("");
        lbl.setText(msg);
        loginCard.getStyleClass().add("login-card-error");
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), loginCard);
        shake.setCycleCount(6); shake.setAutoReverse(true); shake.setFromX(-6); shake.setToX(6);
        shake.setOnFinished(e -> loginCard.getStyleClass().remove("login-card-error"));
        shake.play();
    }
}
