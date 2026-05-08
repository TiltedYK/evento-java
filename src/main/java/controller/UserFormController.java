package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.User;
import service.GravatarService;
import service.UserService;
import util.Router;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Admin form to create or edit a user.
 *
 * Picture upload model:
 *   - On create: pickedImageFilename = null and imageCleared = false → user.image stays null
 *     and Gravatar/identicon is auto-generated from the email.
 *   - On create + admin picks a file: file is copied to {@code uploads/<ts>_<orig>}
 *     and that filename is stored in user.image.
 *   - On edit: user.image starts at editing.getImage(); admin can pick a new one or clear it.
 */
public class UserFormController {

    @FXML private Label formTitle;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private DatePicker birthPicker;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private ImageView avatarPreview;
    @FXML private Label imageHint;
    @FXML private Button saveButton;

    private final UserService service = new UserService();
    private User editing;

    private String pickedImageFilename;
    private boolean imageCleared = false;

    private static final Pattern EMAIL_RX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("ROLE_USER", "ROLE_ADMIN", "ROLE_ARTIST");
        roleCombo.setValue("ROLE_USER");

        this.editing = UserListController.pendingEdit;
        UserListController.pendingEdit = null;

        if (editing == null) {
            formTitle.setText("New User");
            saveButton.setText("Create user");
            passwordLabel.setText("PASSWORD");
            refreshPreviewFromEmail();
        } else {
            formTitle.setText("Edit User #" + editing.getId());
            saveButton.setText("Save changes");
            passwordLabel.setText("PASSWORD (leave empty to keep current)");

            nomField.setText(editing.getNom());
            prenomField.setText(editing.getPrenom());
            emailField.setText(editing.getEmail());
            telField.setText(editing.getNumTelephone());
            birthPicker.setValue(editing.getDateNaissance());
            locationField.setText(editing.getLocalisation());

            if (editing.getRoles() != null) {
                if (editing.getRoles().contains("ADMIN")) roleCombo.setValue("ROLE_ADMIN");
                else if (editing.getRoles().contains("ARTIST")) roleCombo.setValue("ROLE_ARTIST");
                else roleCombo.setValue("ROLE_USER");
            }

            // Preview the existing avatar (uploaded file, Google URL, or Gravatar).
            try {
                avatarPreview.setImage(new Image(GravatarService.resolveAvatarUrl(editing, 96), true));
                imageHint.setText(GravatarService.describeAvatarSource(editing));
            } catch (Exception ignored) { refreshPreviewFromEmail(); }
        }

        // Live-update the Gravatar preview as the email is typed (only when no file is picked).
        emailField.textProperty().addListener((obs, oldV, newV) -> {
            if (pickedImageFilename == null && !imageCleared && (editing == null || editing.getImage() == null || editing.getImage().isBlank())) {
                refreshPreviewFromEmail();
            }
        });
    }

    private void refreshPreviewFromEmail() {
        String em = safe(emailField.getText());
        if (em.isEmpty()) {
            avatarPreview.setImage(null);
            imageHint.setText("No file selected — a Gravatar/identicon will be generated from the email.");
            return;
        }
        try {
            avatarPreview.setImage(new Image(GravatarService.getAvatarUrl(em, 96), true));
            imageHint.setText("Auto avatar via Gravatar (you can override by uploading a file).");
        } catch (Exception ignored) { /* preview is best-effort */ }
    }

    @FXML
    public void onPickImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select profile picture");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        Stage owner = (Stage) saveButton.getScene().getWindow();
        File chosen = fc.showOpenDialog(owner);
        if (chosen == null) return;

        try {
            File uploads = new File("uploads");
            if (!uploads.exists()) uploads.mkdirs();
            String safeName = chosen.getName().replaceAll("[^A-Za-z0-9._-]", "_");
            String fname = System.currentTimeMillis() + "_" + safeName;
            Files.copy(chosen.toPath(), Paths.get("uploads", fname), StandardCopyOption.REPLACE_EXISTING);

            pickedImageFilename = fname;
            imageCleared = false;
            avatarPreview.setImage(new Image(new File("uploads", fname).toURI().toString()));
            imageHint.setText("Will save as: " + fname);
        } catch (IOException ex) {
            error("Failed to copy file: " + ex.getMessage());
        }
    }

    @FXML
    public void onClearImage() {
        pickedImageFilename = null;
        imageCleared = true;
        refreshPreviewFromEmail();
    }

    @FXML
    public void onSave() {
        String nom = safe(nomField.getText());
        String prenom = safe(prenomField.getText());
        String email = safe(emailField.getText());
        String tel = safe(telField.getText());
        String pwd = passwordField.getText() == null ? "" : passwordField.getText();

        if (nom.isEmpty())    { error("Last name is required."); return; }
        if (prenom.isEmpty()) { error("First name is required."); return; }
        if (email.isEmpty())  { error("Email is required."); return; }
        if (!EMAIL_RX.matcher(email).matches()) { error("Invalid email format."); return; }
        if (!tel.isEmpty() && !tel.matches("\\d{6,15}")) { error("Phone must be 6-15 digits."); return; }
        if (editing == null && pwd.length() < 6) { error("Password must be at least 6 characters."); return; }
        if (editing != null && !pwd.isEmpty() && pwd.length() < 6) { error("Password must be at least 6 characters."); return; }

        User target = editing != null ? editing : new User();
        target.setNom(nom);
        target.setPrenom(prenom);
        target.setEmail(email);
        target.setNumTelephone(tel);
        target.setDateNaissance(birthPicker.getValue());
        target.setLocalisation(safe(locationField.getText()));
        target.setRoles("[\"" + roleCombo.getValue() + "\"]");

        // Image: prefer newly-picked file → keep existing on edit → null (use Gravatar fallback)
        if (pickedImageFilename != null) {
            target.setImage(pickedImageFilename);
        } else if (imageCleared) {
            target.setImage(null);
        } else if (editing != null) {
            target.setImage(editing.getImage());
        } else {
            target.setImage(null);
        }

        // banned/deleted are no longer settable from the admin form — defaults to 0 for new users,
        // and existing values are preserved untouched on edit.
        if (editing == null) {
            target.setBanned(0);
            target.setDeleted(0);
        }

        if (!pwd.isEmpty()) target.setPassword(pwd);

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            onCancel();
        } catch (Exception e) { error("Save failed: " + e.getMessage()); }
    }

    @FXML public void onCancel() { Router.navigate("/fxml/UserList.fxml"); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
