package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Category;
import model.Product;
import service.CategoryService;
import service.ProductService;
import util.Router;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin product form. Now supports uploading a product photo from disk:
 * the file is copied to {@code uploads/<timestamp>_<original-name>} and that
 * filename is persisted in {@link Product#getImage()}. The front shop
 * displays it via {@code GravatarService.resolveAvatarUrl}-style logic.
 */
public class ProductFormController {

    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextField artistField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private TextField priceField;
    @FXML private Spinner<Integer> stockSpinner;
    @FXML private TextField barcodeField;
    @FXML private TextField sizeField;
    @FXML private ImageView productPreview;
    @FXML private Label productImageHint;
    @FXML private TextArea descriptionArea;
    @FXML private CheckBox availableCheck;
    @FXML private ComboBox<String> currencyCombo;
    @FXML private Button saveButton;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private Product editing;

    private String pickedImageFilename;
    private boolean imageCleared = false;

    @FXML
    public void initialize() {
        stockSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 10, 1));

        if (currencyCombo != null) {
            currencyCombo.getItems().addAll("TND","EUR","USD","GBP","JPY","CAD","AUD","MAD","DZD","SAR");
            currencyCombo.setValue("TND");
        }

        try {
            List<Category> cats = categoryService.recuperer();
            categoryCombo.setItems(FXCollections.observableArrayList(cats));
            categoryCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Category c) { return c == null ? "" : c.getName(); }
                @Override public Category fromString(String s) { return null; }
            });
            if (!cats.isEmpty()) categoryCombo.setValue(cats.get(0));
        } catch (Exception e) { /* empty combo */ }

        this.editing = ProductListController.pendingEdit;
        ProductListController.pendingEdit = null;

        if (editing == null) {
            formTitle.setText("New Product");
            saveButton.setText("Create product");
            updateImageHint("Optional. If left empty a generated cover will be used.");
        } else {
            formTitle.setText("Edit Product #" + editing.getId());
            saveButton.setText("Save changes");
            nameField.setText(editing.getName());
            artistField.setText(editing.getArtistName());
            priceField.setText(String.valueOf(editing.getPrice()));
            stockSpinner.getValueFactory().setValue(editing.getStock());
            barcodeField.setText(editing.getBarcode());
            if (sizeField != null) sizeField.setText(editing.getSize() != null ? editing.getSize() : "");
            if (currencyCombo != null && editing.getCurrency() != null) currencyCombo.setValue(editing.getCurrency());
            descriptionArea.setText(editing.getDescription());
            availableCheck.setSelected(editing.isAvailable());

            for (Category c : categoryCombo.getItems()) {
                if (c.getId() == editing.getCategoryId()) {
                    categoryCombo.setValue(c); break;
                }
            }

            // Existing image preview
            String existing = editing.getImage();
            if (existing != null && !existing.isBlank()) {
                File f = new File("uploads", existing);
                if (f.exists()) {
                    productPreview.setImage(new Image(f.toURI().toString()));
                    updateImageHint("Current file: " + existing);
                } else {
                    updateImageHint("Stored as " + existing + " (file not found locally)");
                }
            } else {
                updateImageHint("No image uploaded — auto-generated cover will be used.");
            }
        }
    }

    @FXML
    public void onPickProductImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pick a product image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp"));
        Stage owner = saveButton == null || saveButton.getScene() == null ? null
                : (Stage) saveButton.getScene().getWindow();
        File chosen = fc.showOpenDialog(owner);
        if (chosen == null) return;
        try {
            File uploads = new File("uploads");
            if (!uploads.exists()) uploads.mkdirs();
            String orig = chosen.getName().replaceAll("\\s+", "_");
            String fname = System.currentTimeMillis() + "_" + orig;
            Files.copy(chosen.toPath(), Paths.get("uploads", fname), StandardCopyOption.REPLACE_EXISTING);

            pickedImageFilename = fname;
            imageCleared = false;
            productPreview.setImage(new Image(new File("uploads", fname).toURI().toString()));
            updateImageHint("Will save as " + fname);
        } catch (Exception e) {
            updateImageHint("⚠  Failed to copy file: " + e.getMessage());
        }
    }

    @FXML
    public void onClearProductImage() {
        pickedImageFilename = null;
        imageCleared = true;
        productPreview.setImage(null);
        updateImageHint("Image cleared — auto-generated cover will be used.");
    }

    private void updateImageHint(String s) {
        if (productImageHint != null) productImageHint.setText(s);
    }

    @FXML
    public void onSave() {
        String name = safe(nameField.getText());
        String artist = safe(artistField.getText());

        if (name.isEmpty())   { error("Name is required."); return; }
        if (artist.isEmpty()) { error("Artist name is required."); return; }
        if (categoryCombo.getValue() == null) {
            error("Please select a category (create one first if none exists)."); return;
        }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().replace(",", ".").trim());
            if (price < 0) throw new NumberFormatException();
        } catch (Exception e) {
            error("Price must be a positive number."); return;
        }

        Product target = editing != null ? editing : new Product();
        target.setName(name);
        target.setArtistName(artist);
        target.setCategoryId(categoryCombo.getValue().getId());
        target.setPrice(price);
        target.setStock(stockSpinner.getValue());
        target.setBarcode(safe(barcodeField.getText()));
        if (sizeField != null) target.setSize(safe(sizeField.getText()));
        if (currencyCombo != null && currencyCombo.getValue() != null) target.setCurrency(currencyCombo.getValue());
        target.setDescription(safe(descriptionArea.getText()));
        target.setAvailable(availableCheck.isSelected());

        // Image: pick > clear > keep
        if (pickedImageFilename != null) {
            target.setImage(pickedImageFilename);
        } else if (imageCleared) {
            target.setImage(null);
        }

        if (editing == null) target.setCreatedAt(LocalDateTime.now());

        try {
            if (editing == null) productService.ajouter(target);
            else productService.modifier(target);
            onCancel();
        } catch (Exception e) { error("Save failed: " + e.getMessage()); }
    }

    @FXML public void onCancel() { Router.navigate("/fxml/ProductList.fxml"); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
