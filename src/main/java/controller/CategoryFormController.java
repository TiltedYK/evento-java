package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Category;
import service.CategoryService;

public class CategoryFormController {

    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;

    private final CategoryService service = new CategoryService();
    private Category editing;

    public void setCategory(Category c) {
        this.editing = c;
        if (c == null) {
            formTitle.setText("New Category");
            saveButton.setText("Create category");
            return;
        }
        formTitle.setText("Edit Category #" + c.getId());
        saveButton.setText("Save changes");

        nameField.setText(c.getName());
        descriptionArea.setText(c.getDescription());
    }

    @FXML
    public void onSave() {
        String name = safe(nameField.getText());
        if (name.isEmpty()) { showError("Name is required."); return; }
        if (name.length() < 2) { showError("Name must be at least 2 characters."); return; }

        Category target = editing != null ? editing : new Category();
        target.setName(name);
        target.setDescription(safe(descriptionArea.getText()));

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            close();
        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    @FXML public void onCancel() { close(); }

    private void close() { ((Stage) nameField.getScene().getWindow()).close(); }

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
