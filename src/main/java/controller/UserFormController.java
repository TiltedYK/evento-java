package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.User;
import service.UserService;
import util.Router;

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

        this.editing = UserListController.pendingEdit;
        UserListController.pendingEdit = null;

        if (editing == null) {
            formTitle.setText("New User");
            saveButton.setText("Create user");
            passwordLabel.setText("PASSWORD");
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
            imageField.setText(editing.getImage());
            bannedCheck.setSelected(editing.getBanned() == 1);
            deletedCheck.setSelected(editing.getDeleted() == 1);

            if (editing.getRoles() != null) {
                if (editing.getRoles().contains("ADMIN")) roleCombo.setValue("ROLE_ADMIN");
                else if (editing.getRoles().contains("ARTIST")) roleCombo.setValue("ROLE_ARTIST");
                else roleCombo.setValue("ROLE_USER");
            }
        }
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
        target.setImage(safe(imageField.getText()));
        target.setRoles("[\"" + roleCombo.getValue() + "\"]");
        target.setBanned(bannedCheck.isSelected() ? 1 : 0);
        target.setDeleted(deletedCheck.isSelected() ? 1 : 0);
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
