package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Category;
import model.Product;
import service.CategoryService;
import service.ProductService;

import java.time.LocalDateTime;
import java.util.List;

public class ProductFormController {

    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextField artistField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private TextField priceField;
    @FXML private Spinner<Integer> stockSpinner;
    @FXML private TextField barcodeField;
    @FXML private TextField imageField;
    @FXML private TextArea descriptionArea;
    @FXML private CheckBox availableCheck;
    @FXML private Button saveButton;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private Product editing;

    @FXML
    public void initialize() {
        stockSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 10, 1));

        try {
            List<Category> cats = categoryService.recuperer();
            categoryCombo.setItems(FXCollections.observableArrayList(cats));
            categoryCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Category c) { return c == null ? "" : c.getName(); }
                @Override public Category fromString(String s) { return null; }
            });
            if (!cats.isEmpty()) categoryCombo.setValue(cats.get(0));
        } catch (Exception e) {
            // no categories yet — combo stays empty; save will complain
        }
    }

    public void setProduct(Product p) {
        this.editing = p;
        if (p == null) {
            formTitle.setText("New Product");
            saveButton.setText("Create product");
            return;
        }
        formTitle.setText("Edit Product #" + p.getId());
        saveButton.setText("Save changes");

        nameField.setText(p.getName());
        artistField.setText(p.getArtistName());
        priceField.setText(String.valueOf(p.getPrice()));
        stockSpinner.getValueFactory().setValue(p.getStock());
        barcodeField.setText(p.getBarcode());
        imageField.setText(p.getImage());
        descriptionArea.setText(p.getDescription());
        availableCheck.setSelected(p.isAvailable());

        for (Category c : categoryCombo.getItems()) {
            if (c.getId() == p.getCategoryId()) {
                categoryCombo.setValue(c);
                break;
            }
        }
    }

    @FXML
    public void onSave() {
        String name = safe(nameField.getText());
        String artist = safe(artistField.getText());

        if (name.isEmpty())   { showError("Name is required."); return; }
        if (artist.isEmpty()) { showError("Artist name is required."); return; }
        if (categoryCombo.getValue() == null) { showError("Please select a category (create one first if none exists)."); return; }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().replace(",", ".").trim());
            if (price < 0) throw new NumberFormatException();
        } catch (Exception e) {
            showError("Price must be a positive number.");
            return;
        }

        Product target = editing != null ? editing : new Product();
        target.setName(name);
        target.setArtistName(artist);
        target.setCategoryId(categoryCombo.getValue().getId());
        target.setPrice(price);
        target.setStock(stockSpinner.getValue());
        target.setBarcode(safe(barcodeField.getText()));
        target.setImage(safe(imageField.getText()));
        target.setDescription(safe(descriptionArea.getText()));
        target.setAvailable(availableCheck.isSelected());
        if (editing == null) target.setCreatedAt(LocalDateTime.now());

        try {
            if (editing == null) productService.ajouter(target);
            else productService.modifier(target);
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
