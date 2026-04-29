package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import model.CartItem;
import model.Product;
import model.User;
import service.CartService;
import service.CurrencyService;
import service.PdfService;
import service.ProductService;
import util.OverlayService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FrontShopController implements Initializable {

    @FXML private FlowPane        shopGrid;
    @FXML private TextField       searchField;
    @FXML private ComboBox<String> sortFilter;
    @FXML private ComboBox<String> currencyCombo;
    @FXML private Slider          priceSlider;
    @FXML private Label           priceLabel;
    @FXML private Label           countLabel;
    @FXML private Label           rateLabel;

    @FXML private VBox   cartItemsBox;
    @FXML private Label  cartBadge;
    @FXML private Label  cartTotalLabel;
    @FXML private Label  cartTotalConverted;
    @FXML private Button checkoutBtn;

    private final ProductService  productService  = new ProductService();
    private final CartService     cart            = CartService.getInstance();
    private final CurrencyService currencyService = new CurrencyService();
    private final PdfService      pdfService      = new PdfService();

    private List<Product> allProducts;
    private User          currentUser;
    private FrontDashboardController dashboard;
    private String        activeCurrency = "TND";
    private Map<String, Double> rates;

    private static final AtomicInteger ORDER_SEQ = new AtomicInteger(1000);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortFilter.getItems().addAll("Default", "Price ↑", "Price ↓", "Artist A–Z", "Name A–Z");
        sortFilter.setValue("Default");

        currencyCombo.getItems().addAll("TND", "USD", "EUR", "GBP");
        currencyCombo.setValue("TND");

        priceSlider.valueProperty().addListener((obs, o, n) ->
                priceLabel.setText((int) n.doubleValue() + " TND"));

        cart.setOnChange(this::refreshCart);
        checkoutBtn.setDisable(cart.isEmpty());

        loadProducts();
        loadRatesAsync();
        refreshCart();
    }

    public void setCurrentUser(User user)                { this.currentUser = user; }
    public void setDashboard(FrontDashboardController d) { this.dashboard = d; }

    // ── Load ──────────────────────────────────────────────────────────

    private void loadProducts() {
        try {
            allProducts = productService.recuperer();
            renderProducts(allProducts);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadRatesAsync() {
        rateLabel.setText("Fetching live exchange rates…");
        new Thread(() -> {
            rates = currencyService.getRates();
            Platform.runLater(() -> {
                if (rates != null) {
                    String info = rates.entrySet().stream()
                            .filter(e -> !e.getKey().equals("TND"))
                            .map(e -> "1 TND = " + String.format("%.4f", e.getValue()) + " " + e.getKey())
                            .collect(Collectors.joining("   ·   "));
                    rateLabel.setText(info.isEmpty() ? "Rates loaded" : "  " + info);
                } else {
                    rateLabel.setText("Exchange rates unavailable");
                }
                renderProducts(allProducts);
            });
        }, "currency-loader").start();
    }

    // ── Filter / Sort ─────────────────────────────────────────────────

    @FXML void filterProducts() {
        if (allProducts == null) return;
        String q    = searchField.getText().toLowerCase().trim();
        String sort = sortFilter.getValue();
        double maxP = priceSlider.getValue();

        List<Product> filtered = allProducts.stream()
                .filter(p -> q.isEmpty()
                        || p.getName().toLowerCase().contains(q)
                        || (p.getArtistName()  != null && p.getArtistName().toLowerCase().contains(q))
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q)))
                .filter(p -> p.getPrice() <= maxP)
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Price ↑"    -> filtered.sort(Comparator.comparingDouble(Product::getPrice));
            case "Price ↓"    -> filtered.sort(Comparator.comparingDouble(Product::getPrice).reversed());
            case "Artist A–Z" -> filtered.sort(Comparator.comparing(p -> p.getArtistName() != null ? p.getArtistName() : ""));
            case "Name A–Z"   -> filtered.sort(Comparator.comparing(p -> p.getName() != null ? p.getName() : ""));
        }
        renderProducts(filtered);
    }

    @FXML void onCurrencyChanged() {
        activeCurrency = currencyCombo.getValue();
        if (allProducts != null) filterProducts();
        refreshCart();
    }

    // ── Product cards ─────────────────────────────────────────────────

    private void renderProducts(List<Product> products) {
        if (products == null) return;
        shopGrid.getChildren().clear();
        countLabel.setText(products.size() + " item" + (products.size() == 1 ? "" : "s"));

        for (int i = 0; i < products.size(); i++) {
            VBox card = buildProductCard(products.get(i));
            shopGrid.getChildren().add(card);
            animateIn(card, i * 45L);
        }
        if (products.isEmpty()) {
            Label empty = new Label("🛒  Nothing found.\nCheck back later.");
            empty.getStyleClass().add("front-empty-label");
            empty.setWrapText(true);
            shopGrid.getChildren().add(empty);
        }
    }

    private VBox buildProductCard(Product p) {
        VBox card = new VBox(9);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(210);

        Label artist = new Label(p.getArtistName() != null ? p.getArtistName().toUpperCase() : "ARTIST");
        artist.getStyleClass().add("card-genre-badge-acid");

        Label name = new Label(p.getName());
        name.getStyleClass().add("card-event-title");
        name.setWrapText(true);

        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            Label desc = new Label(p.getDescription());
            desc.getStyleClass().add("card-event-desc");
            desc.setWrapText(true); desc.setMaxHeight(40);
            card.getChildren().add(desc);
        }

        VBox priceBlock = new VBox(2);
        Label tndPrice = new Label(String.format("%.2f TND", p.getPrice()));
        tndPrice.getStyleClass().add("card-price-main");
        priceBlock.getChildren().add(tndPrice);
        if (!"TND".equals(activeCurrency) && rates != null) {
            Label conv = new Label(currencyService.convertLabel(p.getPrice(), activeCurrency));
            conv.getStyleClass().add("card-price-converted");
            priceBlock.getChildren().add(conv);
        }

        boolean inStock = p.getStock() > 0 && p.isAvailable();
        Label stock = new Label(inStock ? "✅  " + p.getStock() + " left" : "❌  Sold out");
        stock.getStyleClass().add(inStock ? "badge-upcoming" : "badge-status");

        Button addBtn = new Button(inStock ? "🛒  ADD TO CART" : "SOLD OUT");
        addBtn.getStyleClass().add(inStock ? "card-reserve-btn-large" : "card-action-btn-outline");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setDisable(!inStock);
        addBtn.setOnAction(e -> onAddToCart(p, addBtn));

        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        card.getChildren().addAll(artist, name, priceBlock, stock, sp, addBtn);

        ScaleTransition up = new ScaleTransition(Duration.millis(140), card); up.setToX(1.03); up.setToY(1.03);
        ScaleTransition dn = new ScaleTransition(Duration.millis(140), card); dn.setToX(1.0);  dn.setToY(1.0);
        card.setOnMouseEntered(ev -> up.play());
        card.setOnMouseExited(ev  -> dn.play());
        return card;
    }

    private void onAddToCart(Product p, Button btn) {
        cart.addItem(p);
        String orig = btn.getText();
        btn.setText("✅  ADDED!"); btn.setDisable(true);
        ScaleTransition pulse = new ScaleTransition(Duration.millis(120), btn);
        pulse.setToX(1.08); pulse.setToY(1.08); pulse.setCycleCount(2); pulse.setAutoReverse(true);
        pulse.setOnFinished(e -> { btn.setText(orig); btn.setDisable(false); });
        pulse.play();
        if (dashboard != null) dashboard.updateShopBadge();
    }

    // ── Cart panel ────────────────────────────────────────────────────

    private void refreshCart() {
        Platform.runLater(() -> {
            cartItemsBox.getChildren().clear();
            List<CartItem> items = cart.getItems();

            if (items.isEmpty()) {
                Label empty = new Label("Your cart is empty.\nAdd some merch!");
                empty.getStyleClass().add("cart-empty-label");
                empty.setWrapText(true);
                cartItemsBox.getChildren().add(empty);
            } else {
                items.forEach(ci -> cartItemsBox.getChildren().add(buildCartRow(ci)));
            }

            int n = cart.getTotalItems();
            cartBadge.setText(n > 0 ? n + " item" + (n == 1 ? "" : "s") : "empty");
            cartTotalLabel.setText(String.format("%.2f TND", cart.getTotal()));
            cartTotalConverted.setText(!"TND".equals(activeCurrency)
                    ? currencyService.convertLabel(cart.getTotal(), activeCurrency) : "");
            checkoutBtn.setDisable(cart.isEmpty());
        });
    }

    private HBox buildCartRow(CartItem ci) {
        HBox row = new HBox(6);
        row.getStyleClass().add("cart-item-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        String nm = ci.getProduct().getName();
        Label name = new Label(nm.length() > 18 ? nm.substring(0, 18) + "…" : nm);
        name.getStyleClass().add("cart-item-name");
        Label sub  = new Label(String.format("%.2f", ci.getSubtotal()) + " TND");
        sub.getStyleClass().add("cart-item-sub");
        info.getChildren().addAll(name, sub);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button minus = mkQtyBtn("−"); Button plus = mkQtyBtn("+"); Button del = mkDelBtn();
        Label  qty   = new Label(String.valueOf(ci.getQuantity())); qty.getStyleClass().add("cart-qty-label");

        int id = ci.getProduct().getId();
        minus.setOnAction(e -> { cart.decreaseQty(id); if (dashboard != null) dashboard.updateShopBadge(); });
        plus.setOnAction(e  -> { cart.increaseQty(id); if (dashboard != null) dashboard.updateShopBadge(); });
        del.setOnAction(e   -> { cart.removeItem(id);  if (dashboard != null) dashboard.updateShopBadge(); });

        row.getChildren().addAll(info, minus, qty, plus, del);
        return row;
    }

    private Button mkQtyBtn(String t) { Button b = new Button(t); b.getStyleClass().add("cart-qty-btn"); return b; }
    private Button mkDelBtn()         { Button b = new Button("✕"); b.getStyleClass().add("cart-del-btn"); return b; }

    // ── Checkout ──────────────────────────────────────────────────────

    @FXML void onCheckout() {
        if (cart.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        cart.getItems().forEach(ci ->
                sb.append("• ").append(ci.getProduct().getName())
                  .append("  ×").append(ci.getQuantity())
                  .append("  →  ").append(String.format("%.2f TND\n", ci.getSubtotal())));
        sb.append("\nTOTAL: ").append(String.format("%.2f TND", cart.getTotal()));

        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Confirm Order"); dlg.setHeaderText("Review your order:"); dlg.setContentText(sb.toString());
        styleAlert(dlg);
        dlg.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) placeOrder(); });
    }

    private void placeOrder() {
        int ref = ORDER_SEQ.incrementAndGet();

        // ── 1. Reduce stock for every item ──────────────────────────────
        List<CartItem> snapshot = new java.util.ArrayList<>(cart.getItems());
        StringBuilder stockErrors = new StringBuilder();
        for (CartItem ci : snapshot) {
            Product p = ci.getProduct();
            int newStock = Math.max(0, p.getStock() - ci.getQuantity());
            p.setStock(newStock);
            try { productService.modifier(p); }
            catch (Exception ex) { stockErrors.append(p.getName()).append(": ").append(ex.getMessage()).append("\n"); }
        }

        // ── 2. Clear cart & update badge ────────────────────────────────
        cart.clear();
        if (dashboard != null) dashboard.updateShopBadge();

        // ── 3. Reload products so stock labels refresh ───────────────────
        loadProducts();

        // ── 4. Show success overlay (+ offer PDF) ──────────────────────
        VBox content = new VBox(18);
        content.setStyle("-fx-padding:24 28 24 28;");

        Label check = new Label("✅"); check.setStyle("-fx-font-size:48px;-fx-alignment:CENTER;");
        check.setMaxWidth(Double.MAX_VALUE);
        Label heading = new Label("Order Confirmed!");
        heading.setStyle("-fx-font-size:20px;-fx-font-weight:700;-fx-alignment:CENTER;");
        heading.setMaxWidth(Double.MAX_VALUE);
        Label ref_lbl = new Label("Order #ORD-" + ref);
        ref_lbl.setStyle("-fx-font-size:13px;-fx-text-fill:#9CA3AF;-fx-alignment:CENTER;");
        ref_lbl.setMaxWidth(Double.MAX_VALUE);

        // Item summary
        VBox summary = new VBox(6);
        summary.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14 16 14 16;");
        for (CartItem ci : snapshot) {
            HBox row = new HBox(8);
            Label name = new Label(ci.getProduct().getName()); name.getStyleClass().add("card-event-title"); HBox.setHgrow(name, Priority.ALWAYS);
            Label sub  = new Label(String.format("×%d  %.2f TND", ci.getQuantity(), ci.getSubtotal()));
            sub.getStyleClass().add("card-price-main");
            row.getChildren().addAll(name, sub);
            summary.getChildren().add(row);
        }
        double total = snapshot.stream().mapToDouble(CartItem::getSubtotal).sum();
        Label totalLbl = new Label("TOTAL:  " + String.format("%.2f TND", total));
        totalLbl.setStyle("-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:#E8320A;-fx-padding:8 0 0 0;");
        summary.getChildren().add(totalLbl);

        // PDF button
        Button pdfBtn = new Button("📄  Download Receipt PDF");
        pdfBtn.getStyleClass().add("card-action-btn");
        pdfBtn.setMaxWidth(Double.MAX_VALUE);
        pdfBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Receipt");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fc.setInitialFileName("receipt-ORD-" + ref + ".pdf");
            File f = fc.showSaveDialog(shopGrid.getScene().getWindow());
            if (f != null) {
                try { pdfService.generateReceipt(snapshot, total, ref, f); }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        if (!stockErrors.isEmpty()) {
            Label warn = new Label("⚠ Some stock updates failed:\n" + stockErrors);
            warn.setStyle("-fx-font-size:11px;-fx-text-fill:#F59E0B;"); warn.setWrapText(true);
            content.getChildren().add(warn);
        }

        content.getChildren().addAll(check, heading, ref_lbl, summary, pdfBtn);
        OverlayService.show("Order Placed!", content);
    }

    @FXML void onClearCart() {
        cart.clear();
        if (dashboard != null) dashboard.updateShopBadge();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void animateIn(VBox card, long delayMs) {
        card.setOpacity(0); card.setTranslateY(12);
        PauseTransition p = new PauseTransition(Duration.millis(delayMs));
        p.setOnFinished(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(320), card); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(320), card);
            tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
        });
        p.play();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("EVENTO"); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        try {
            a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            a.getDialogPane().getStylesheets().add(getClass().getResource("/css/front-styles.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}
