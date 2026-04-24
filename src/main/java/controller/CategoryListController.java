package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.DefaultStringConverter;
import model.Category;
import service.CategoryService;
import util.Router;

import java.util.List;

public class CategoryListController {

    @FXML private TextField searchField;
    @FXML private TableView<Category> table;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TableColumn<Category, String> colDescription;
    @FXML private TableColumn<Category, Void> colActions;

    private final CategoryService service = new CategoryService();
    private final ObservableList<Category> data = FXCollections.observableArrayList();

    public static Category pendingEdit;

    @FXML
    public void initialize() {
        table.setEditable(true);

        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        colName.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colName.setOnEditCommit(e -> {
            Category c = e.getRowValue();
            c.setName(e.getNewValue());
            try { service.modifier(c); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        colDescription.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colDescription.setOnEditCommit(e -> {
            Category c = e.getRowValue();
            c.setDescription(e.getNewValue());
            try { service.modifier(c); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        setupActions();
        searchField.textProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private void setupActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            {
                btnDelete.getStyleClass().addAll("button", "btn-danger", "btn-action");
                btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
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
    @FXML public void onAdd() {
        pendingEdit = null;
        Router.navigate("/fxml/CategoryForm.fxml");
    }

    private void onDelete(Category c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete category \"" + c.getName() + "\"?");
        a.setContentText("Products linked to this category may become unassigned.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(c.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
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
