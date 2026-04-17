package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.User;
import service.UserService;

import java.util.regex.Pattern;

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
    @FXML private TextField imageField;
    @FXML private CheckBox bannedCheck;
    @FXML private CheckBox deletedCheck;
    @FXML private Button saveButton;

    private final UserService service = new UserService();
    private User editing;

    private static final Pattern EMAIL_RX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("ROLE_USER", "ROLE_ADMIN", "ROLE_ARTIST");
        roleCombo.setValue("ROLE_USER");
    }

    public void setUser(User u) {
        this.editing = u;
        if (u == null) {
            formTitle.setText("New User");
            saveButton.setText("Create user");
            passwordLabel.setText("PASSWORD");
            return;
        }
        formTitle.setText("Edit User #" + u.getId());
        saveButton.setText("Save changes");
        passwordLabel.setText("PASSWORD (leave empty to keep current)");

        nomField.setText(u.getNom());
        prenomField.setText(u.getPrenom());
        emailField.setText(u.getEmail());
        telField.setText(u.getNumTelephone());
        birthPicker.setValue(u.getDateNaissance());
        locationField.setText(u.getLocalisation());
        imageField.setText(u.getImage());
        bannedCheck.setSelected(u.getBanned() == 1);
        deletedCheck.setSelected(u.getDeleted() == 1);

        if (u.getRoles() != null) {
            if (u.getRoles().contains("ADMIN")) roleCombo.setValue("ROLE_ADMIN");
            else if (u.getRoles().contains("ARTIST")) roleCombo.setValue("ROLE_ARTIST");
            else roleCombo.setValue("ROLE_USER");
        }
    }

    @FXML
    public void onSave() {
        String nom = safe(nomField.getText());
        String prenom = safe(prenomField.getText());
        String email = safe(emailField.getText());
        String tel = safe(telField.getText());
        String pwd = passwordField.getText() == null ? "" : passwordField.getText();

        if (nom.isEmpty())    { showError("Last name is required."); return; }
        if (prenom.isEmpty()) { showError("First name is required."); return; }
        if (email.isEmpty())  { showError("Email is required."); return; }
        if (!EMAIL_RX.matcher(email).matches()) { showError("Invalid email format."); return; }
        if (!tel.isEmpty() && !tel.matches("\\d{6,15}")) { showError("Phone must be 6-15 digits."); return; }
        if (editing == null && pwd.length() < 6) { showError("Password must be at least 6 characters."); return; }
        if (editing != null && !pwd.isEmpty() && pwd.length() < 6) { showError("Password must be at least 6 characters."); return; }

        User target = editing != null ? editing : new User();
        target.setNom(nom);
        target.setPrenom(prenom);
        target.setEmail(email);
        target.setNumTelephone(tel);
        target.setDateNaissance(birthPicker.getValue());
        target.setLocalisation(safe(locationField.getText()));
        target.setImage(safe(imageField.getText()));
        target.setRoles("[\"" + roleCombo.getValue() + "\"]");
        target.setBanned(bannedCheck.isSelected() ? 1 : 0);
        target.setDeleted(deletedCheck.isSelected() ? 1 : 0);

        if (!pwd.isEmpty()) target.setPassword(pwd);

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            close();
        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    @FXML public void onCancel() { close(); }

    private void close() { ((Stage) nomField.getScene().getWindow()).close(); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
