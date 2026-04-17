package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Category;
import service.CategoryService;
import util.Router;

public class CategoryFormController {

    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;

    private final CategoryService service = new CategoryService();
    private Category editing;

    @FXML
    public void initialize() {
        this.editing = CategoryListController.pendingEdit;
        CategoryListController.pendingEdit = null;

        if (editing == null) {
            formTitle.setText("New Category");
            saveButton.setText("Create category");
        } else {
            formTitle.setText("Edit Category #" + editing.getId());
            saveButton.setText("Save changes");
            nameField.setText(editing.getName());
            descriptionArea.setText(editing.getDescription());
        }
    }

    @FXML
    public void onSave() {
        String name = safe(nameField.getText());
        if (name.isEmpty())    { error("Name is required."); return; }
        if (name.length() < 2) { error("Name must be at least 2 characters."); return; }

        Category target = editing != null ? editing : new Category();
        target.setName(name);
        target.setDescription(safe(descriptionArea.getText()));

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            onCancel();
        } catch (Exception e) { error("Save failed: " + e.getMessage()); }
    }

    @FXML public void onCancel() { Router.navigate("/fxml/CategoryList.fxml"); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
