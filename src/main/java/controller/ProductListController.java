package controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
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
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colArtist;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
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

        table.setEditable(true);

        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colArtist.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getArtistName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
                categoryNames.getOrDefault(c.getValue().getCategoryId(), "—")));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrice()));
        colStock.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStock()));
        colAvailable.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isAvailable() ? "available" : "unavailable"));

        colName.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colName.setOnEditCommit(e -> {
            Product p = e.getRowValue(); p.setName(e.getNewValue());
            try { productService.modifier(p); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        colArtist.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colArtist.setOnEditCommit(e -> {
            Product p = e.getRowValue(); p.setArtistName(e.getNewValue());
            try { productService.modifier(p); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colPrice.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            Double v = e.getNewValue();
            if (v != null) {
                p.setPrice(v);
                try { productService.modifier(p); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
            }
        });

        colStock.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colStock.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            Integer v = e.getNewValue();
            if (v != null) {
                p.setStock(v);
                try { productService.modifier(p); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
            }
        });

        ObservableList<String> catChoices = FXCollections.observableArrayList();
        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(catChoices));
        colCategory.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            String name = e.getNewValue();
            if (name == null) return;
            for (var en : categoryNames.entrySet()) {
                if (en.getValue().equals(name)) {
                    p.setCategoryId(en.getKey());
                    try { productService.modifier(p); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
                    return;
                }
            }
        });

        ObservableList<String> availChoices = FXCollections.observableArrayList("available", "unavailable");
        colAvailable.setCellFactory(ComboBoxTableCell.forTableColumn(availChoices));
        colAvailable.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            String v = e.getNewValue();
            if (v == null) return;
            p.setAvailable("available".equalsIgnoreCase(v));
            try { productService.modifier(p); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        refreshCategoryChoices(catChoices);

        setupActions();

        searchField.textProperty().addListener((o, a, b) -> {
            refreshCategoryChoices(catChoices);
            applyFilters();
        });
        categoryFilter.valueProperty().addListener((o, a, b) -> {
            refreshCategoryChoices(catChoices);
            applyFilters();
        });
        availabilityFilter.valueProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private void loadCategoryNames() {
        categoryNames.clear();
        try {
            for (Category c : categoryService.recuperer()) categoryNames.put(c.getId(), c.getName());
        } catch (Exception e) { /* silent */ }
    }

    private void refreshCategoryChoices(ObservableList<String> catChoices) {
        try {
            loadCategoryNames();
            catChoices.setAll(categoryNames.values().stream().sorted().toList());
        } catch (Exception ignored) { }
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
        try { loadCategoryNames(); data.setAll(productService.recuperer()); applyFilters(); }
        catch (Exception e) { error("Failed to load products: " + e.getMessage()); }
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
        searchField.clear(); categoryFilter.setValue("all"); availabilityFilter.setValue("all");
    }

    @FXML public void onAdd() {
        pendingEdit = null;
        Router.navigate("/fxml/ProductForm.fxml");
    }

    private void onDelete(Product p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete product \"" + p.getName() + "\"?");
        a.setContentText("This action cannot be undone.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { productService.supprimer(p.getId()); refresh(); }
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
