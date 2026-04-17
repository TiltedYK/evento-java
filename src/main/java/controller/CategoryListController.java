package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Category;
import service.CategoryService;

import java.util.List;

public class CategoryListController {

    @FXML private TextField searchField;
    @FXML private TableView<Category> table;
    @FXML private TableColumn<Category, Number> colId;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TableColumn<Category, String> colDescription;
    @FXML private TableColumn<Category, Void> colActions;

    private final CategoryService service = new CategoryService();
    private final ObservableList<Category> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        setupActionsColumn();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().addAll("button", "btn-primary");
                btnDelete.getStyleClass().addAll("button", "btn-danger");
                btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
                btnDelete.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
                btnEdit.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void refresh() {
        try {
            data.setAll(service.recuperer());
            applyFilters();
        } catch (Exception e) { showError("Failed to load categories", e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<Category> all = service.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

            List<Category> filtered = all.stream()
                    .filter(c -> q.isEmpty()
                            || (c.getName() != null && c.getName().toLowerCase().contains(q))
                            || (c.getDescription() != null && c.getDescription().toLowerCase().contains(q)))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { showError("Filter failed", e.getMessage()); }
    }

    @FXML public void onReset() {
        searchField.clear();
    }

    @FXML public void onAdd() { openForm(null); }

    private void onDelete(Category c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete category #" + c.getId() + "?");
        a.setContentText("Category: " + c.getName() + "\nProducts linked to this category may become unassigned.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(c.getId()); refresh(); }
                catch (Exception e) { showError("Delete failed", e.getMessage()); }
            }
        });
    }

    private void openForm(Category c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CategoryForm.fxml"));
            Parent root = loader.load();
            CategoryFormController ctrl = loader.getController();
            ctrl.setCategory(c);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle(c == null ? "New Category" : "Edit Category");
            Scene sc = new Scene(root);
            sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            st.setScene(sc);
            st.showAndWait();
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open form", e.getMessage());
        }
    }

    private void showError(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    private void styleAlert(Alert a) {
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
}
