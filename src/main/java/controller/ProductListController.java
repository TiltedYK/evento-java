package controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.Category;
import model.Product;
import service.CategoryService;
import service.ProductService;
import util.Router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> availabilityFilter;
    @FXML private TableView<Product> table;
    @FXML private TableColumn<Product, Number> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colArtist;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Number> colPrice;
    @FXML private TableColumn<Product, Number> colStock;
    @FXML private TableColumn<Product, String> colAvailable;
    @FXML private TableColumn<Product, Void> colActions;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<Product> data = FXCollections.observableArrayList();
    private final Map<Integer, String> categoryNames = new HashMap<>();

    public static Product pendingEdit;

    @FXML
    public void initialize() {
        loadCategoryNames();

        categoryFilter.getItems().add("all");
        categoryFilter.getItems().addAll(categoryNames.values().stream().sorted().toList());
        categoryFilter.setValue("all");

        availabilityFilter.getItems().addAll("all", "available", "unavailable");
        availabilityFilter.setValue("all");

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colArtist.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getArtistName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
                categoryNames.getOrDefault(c.getValue().getCategoryId(), "—")));
        colPrice.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getPrice()));
        colStock.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getStock()));
        colAvailable.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isAvailable() ? "available" : "unavailable"));

        formatPriceColumn();
        setupStockColoring();
        setupAvailabilityPill();
        setupActions();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        categoryFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        availabilityFilter.valueProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private void loadCategoryNames() {
        categoryNames.clear();
        try {
            for (Category c : categoryService.recuperer()) {
                categoryNames.put(c.getId(), c.getName());
            }
        } catch (Exception e) { /* silent */ }
    }

    private void formatPriceColumn() {
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f DT", item.doubleValue()));
            }
        });
    }

    private void setupStockColoring() {
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                int stock = item.intValue();
                setText(String.valueOf(stock));
                if (stock == 0)       setStyle("-fx-text-fill: #D28994; -fx-font-weight: bold;");
                else if (stock < 5)   setStyle("-fx-text-fill: #DEBC87; -fx-font-weight: bold;");
                else                  setStyle("-fx-text-fill: #E8ECF2;");
            }
        });
    }

    private void setupAvailabilityPill() {
        colAvailable.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else {
                    Label pill = new Label(item);
                    pill.getStyleClass().add("status-pill");
                    pill.getStyleClass().add(item.equals("available") ? "status-confirmed" : "status-cancelled");
                    setGraphic(pill); setText(null);
                }
            }
        });
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
        try {
            loadCategoryNames();
            data.setAll(productService.recuperer());
            applyFilters();
        } catch (Exception e) { error("Failed to load products: " + e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<Product> all = productService.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String cat = categoryFilter.getValue();
            String av = availabilityFilter.getValue();

            List<Product> filtered = all.stream()
                    .filter(p -> q.isEmpty()
                            || (p.getName() != null && p.getName().toLowerCase().contains(q))
                            || (p.getArtistName() != null && p.getArtistName().toLowerCase().contains(q)))
                    .filter(p -> cat == null || cat.equals("all")
                            || cat.equals(categoryNames.getOrDefault(p.getCategoryId(), "—")))
                    .filter(p -> av == null || av.equals("all")
                            || (av.equals("available") && p.isAvailable())
                            || (av.equals("unavailable") && !p.isAvailable()))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    @FXML public void onReset() {
        searchField.clear();
        categoryFilter.setValue("all");
        availabilityFilter.setValue("all");
    }

    @FXML public void onAdd() { openForm(null); }

    private void onDelete(Product p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete product #" + p.getId() + "?");
        a.setContentText(p.getName() + "\nThis action cannot be undone.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { productService.supprimer(p.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void openForm(Product p) {
        pendingEdit = p;
        Router.navigate("/fxml/ProductForm.fxml");
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
