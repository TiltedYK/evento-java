package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import service.GoogleAuthService;
import service.MailService;

/**
 * Admin "Settings" page — central place to configure email (SMTP) and
 * Google OAuth credentials. The login screen no longer prompts the user
 * for these because they are administrator-level concerns and don't
 * change between sessions.
 *
 * All success/error feedback is rendered inline (no JavaFX
 * {@link javafx.scene.control.Alert}s spawn additional windows).
 */
public class AdminSettingsController {

    // ── SMTP fields ────────────────────────────────────────────────────
    @FXML private TextField     smtpHost;
    @FXML private TextField     smtpPort;
    @FXML private TextField     smtpUser;
    @FXML private PasswordField smtpPass;
    @FXML private TextField     smtpFrom;
    @FXML private TextField     smtpFromName;
    @FXML private Label         smtpStatusLine;
    @FXML private Label         smtpStatusMsg;

    // ── Google OAuth fields ───────────────────────────────────────────
    @FXML private TextField googleClientId;
    @FXML private TextField googleClientSecret;
    @FXML private Label     googleStatusLine;
    @FXML private Label     googleStatusMsg;

    @FXML
    public void initialize() {
        loadSmtp();
        loadGoogle();
    }

    // ── SMTP ──────────────────────────────────────────────────────────

    private void loadSmtp() {
        MailService mail = new MailService();
        MailService.Settings s = mail.getSettings();
        smtpHost.setText(s.host == null ? "smtp.gmail.com" : s.host);
        smtpPort.setText(s.port == 0 ? "465" : String.valueOf(s.port));
        smtpUser.setText(s.username == null ? "" : s.username);
        if (s.password != null) smtpPass.setText(s.password);
        smtpFrom.setText(s.fromAddress == null ? "" : s.fromAddress);
        smtpFromName.setText(s.fromName == null ? "" : s.fromName);
        updateSmtpStatusLine(mail);
    }

    private void updateSmtpStatusLine(MailService mail) {
        if (mail.isConfigured()) {
            smtpStatusLine.setText("✅  Configured — sending from " + mail.getSettings().username
                    + " (" + mail.getSettings().host + ":" + mail.getSettings().port + ")");
            smtpStatusLine.setStyle("-fx-text-fill:#16A34A;-fx-font-size:11px;");
        } else {
            smtpStatusLine.setText("⚠  Not configured yet — password-reset emails are disabled.");
            smtpStatusLine.setStyle("-fx-text-fill:#F59E0B;-fx-font-size:11px;");
        }
    }

    @FXML
    public void onSaveSmtp() {
        smtpStatusMsg.setStyle("");
        int port;
        try { port = Integer.parseInt(smtpPort.getText().trim()); }
        catch (NumberFormatException ex) {
            smtpStatusMsg.setStyle("-fx-text-fill:#EF4444;");
            smtpStatusMsg.setText("Port must be a number (commonly 465).");
            return;
        }
        if (smtpHost.getText().isBlank() || smtpUser.getText().isBlank()
                || smtpPass.getText().isBlank()) {
            smtpStatusMsg.setStyle("-fx-text-fill:#EF4444;");
            smtpStatusMsg.setText("Host, username and password are required.");
            return;
        }
        try {
            MailService.saveSettings(
                    smtpHost.getText().trim(),
                    port,
                    smtpUser.getText().trim(),
                    smtpPass.getText(),
                    smtpFrom.getText().trim(),
                    smtpFromName.getText().trim());
            smtpStatusMsg.setStyle("-fx-text-fill:#16A34A;");
            smtpStatusMsg.setText("✅  Saved.");
            updateSmtpStatusLine(new MailService());
        } catch (Exception ex) {
            smtpStatusMsg.setStyle("-fx-text-fill:#EF4444;");
            smtpStatusMsg.setText("Save failed: " + ex.getMessage());
        }
    }

    @FXML
    public void onSendTestEmail() {
        smtpStatusMsg.setStyle("");
        smtpStatusMsg.setText("📨  Sending test email…");
        // Run send on a background thread so the UI doesn't freeze.
        new Thread(() -> {
            try {
                MailService mail = new MailService();
                if (!mail.isConfigured()) {
                    javafx.application.Platform.runLater(() -> {
                        smtpStatusMsg.setStyle("-fx-text-fill:#EF4444;");
                        smtpStatusMsg.setText("Save SMTP first, then test.");
                    });
                    return;
                }
                String to = mail.getSettings().fromAddress != null && !mail.getSettings().fromAddress.isBlank()
                        ? mail.getSettings().fromAddress : mail.getSettings().username;
                mail.sendPasswordResetCode(to, "Admin", "TEST-" + System.currentTimeMillis());
                javafx.application.Platform.runLater(() -> {
                    smtpStatusMsg.setStyle("-fx-text-fill:#16A34A;");
                    smtpStatusMsg.setText("✅  Test email sent to " + to + " (check inbox/spam).");
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    smtpStatusMsg.setStyle("-fx-text-fill:#EF4444;");
                    smtpStatusMsg.setText("Test failed: " + ex.getMessage());
                });
            }
        }, "SmtpTest").start();
    }

    // ── Google OAuth ──────────────────────────────────────────────────

    private void loadGoogle() {
        GoogleAuthService gauth = new GoogleAuthService();
        if (gauth.isConfigured()) {
            googleStatusLine.setText("✅  Configured — \"Continue with Google\" is enabled on the login screen.");
            googleStatusLine.setStyle("-fx-text-fill:#16A34A;-fx-font-size:11px;");
        } else {
            googleStatusLine.setText("⚠  Not configured — \"Continue with Google\" is disabled.");
            googleStatusLine.setStyle("-fx-text-fill:#F59E0B;-fx-font-size:11px;");
        }
    }

    @FXML
    public void onSaveGoogle() {
        googleStatusMsg.setStyle("");
        String id = googleClientId.getText() == null ? "" : googleClientId.getText().trim();
        String secret = googleClientSecret.getText() == null ? "" : googleClientSecret.getText().trim();
        if (id.isEmpty() || secret.isEmpty()) {
            googleStatusMsg.setStyle("-fx-text-fill:#EF4444;");
            googleStatusMsg.setText("Both Client ID and Secret are required.");
            return;
        }
        if (!id.contains(".apps.googleusercontent.com")) {
            googleStatusMsg.setStyle("-fx-text-fill:#EF4444;");
            googleStatusMsg.setText("Client ID should end with .apps.googleusercontent.com");
            return;
        }
        try {
            GoogleAuthService.saveCredentials(id, secret);
            googleStatusMsg.setStyle("-fx-text-fill:#16A34A;");
            googleStatusMsg.setText("✅  Saved.");
            loadGoogle();
        } catch (Exception ex) {
            googleStatusMsg.setStyle("-fx-text-fill:#EF4444;");
            googleStatusMsg.setText("Save failed: " + ex.getMessage());
        }
    }
}
