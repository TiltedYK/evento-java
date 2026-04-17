package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.Category;
import service.CategoryService;
import util.Router;

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

    public static Category pendingEdit;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        setupActions();
        searchField.textProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private void setupActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().addAll("button", "btn-primary", "btn-action");
                btnDelete.getStyleClass().addAll("button", "btn-danger", "btn-action");
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
        try { data.setAll(service.recuperer()); applyFilters(); }
        catch (Exception e) { error("Failed to load categories: " + e.getMessage()); }
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
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    @FXML public void onReset() { searchField.clear(); }
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
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void openForm(Category c) {
        pendingEdit = c;
        Router.navigate("/fxml/CategoryForm.fxml");
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
}
