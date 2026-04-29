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
import service.AudioDBService;
import service.CartService;
import service.CurrencyService;
import service.MerchTrendsService;
import service.PdfService;
import service.ProductService;
import service.ShippingEstimateService;
import util.OverlayService;

import javafx.geometry.Insets;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FrontShopController implements Initializable {

    public static final String BUILD_TAG = "EVENTO_SHOP_BUILD_2026_04_29_ITUNES_TRENDS_V1";

    @FXML private FlowPane        shopGrid;
    @FXML private TextField       searchField;
    @FXML private ComboBox<String> sizeFilter;
    @FXML private ComboBox<String> sortFilter;
    @FXML private ComboBox<String> currencyCombo;
    @FXML private Slider          priceSlider;
    @FXML private Label           priceLabel;
    @FXML private Label           countLabel;
    @FXML private Label           rateLabel;

    @FXML private VBox   localPriceList;
    @FXML private VBox   cartItemsBox;
    @FXML private Label  cartBadge;
    @FXML private Label  cartSubtotalLabel;
    @FXML private Label  cartShippingLabel;
    @FXML private Label  cartTotalLabel;
    @FXML private Label  cartTotalConverted;
    @FXML private Button checkoutBtn;

    @FXML private Label trendGenreLabel;
    @FXML private VBox  trendArtistsBox;
    @FXML private Label trendStatusLabel;

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

        // Currency dropdown: now flag-decorated and supports 9 currencies.
        currencyCombo.getItems().setAll(CurrencyService.SUPPORTED.values()
                .stream().map(CurrencyService.CurrencyInfo::label).toList());
        currencyCombo.setValue(infoLabel("TND"));
        currencyCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
            }
        });

        priceSlider.valueProperty().addListener((obs, o, n) ->
                priceLabel.setText((int) n.doubleValue() + " TND"));

        cart.setOnChange(this::refreshCart);
        checkoutBtn.setDisable(cart.isEmpty());

        loadProducts();
        loadRatesAsync();
        refreshCart();
        loadTrendsPanelAsync();
    }

    public void setCurrentUser(User user)                { this.currentUser = user; }
    public void setDashboard(FrontDashboardController d) { this.dashboard = d; }

    // ── Load ──────────────────────────────────────────────────────────

    private void loadProducts() {
        try {
            allProducts = productService.recuperer();
            rebuildSizeFilterItems();
            filterProducts();
            rebuildLocalPriceList();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void rebuildSizeFilterItems() {
        if (sizeFilter == null || allProducts == null) return;
        String prev = sizeFilter.getValue();
        Set<String> sizes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Product p : allProducts) {
            if (p.getSize() != null && !p.getSize().isBlank())
                sizes.add(p.getSize().trim());
        }
        sizeFilter.getItems().clear();
        sizeFilter.getItems().add("All sizes");
        sizeFilter.getItems().addAll(sizes);
        if (prev != null && sizeFilter.getItems().contains(prev))
            sizeFilter.setValue(prev);
        else
            sizeFilter.setValue("All sizes");
    }

    private void loadTrendsPanelAsync() {
        if (trendGenreLabel != null) trendGenreLabel.setText("Loading chart pulse…");
        if (trendArtistsBox != null) trendArtistsBox.getChildren().clear();
        if (trendStatusLabel != null) trendStatusLabel.setText("");
        String cc = MerchTrendsService.defaultCountryCode();
        new Thread(() -> {
            try {
                MerchTrendsService.PanelData data = MerchTrendsService.fetch(cc);
                Platform.runLater(() -> applyTrendsUi(data, cc));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (trendGenreLabel != null) trendGenreLabel.setText("Could not load charts.");
                    if (trendStatusLabel != null) trendStatusLabel.setText(e.getMessage());
                });
            }
        }, "merch-trends").start();
    }

    @FXML void onRefreshTrends() {
        loadTrendsPanelAsync();
    }

    private void applyTrendsUi(MerchTrendsService.PanelData data, String cc) {
        if (trendGenreLabel != null) trendGenreLabel.setText(data.genresLine());
        if (trendArtistsBox != null) {
            trendArtistsBox.getChildren().clear();
            if (data.artists().isEmpty()) {
                trendArtistsBox.getChildren().add(new Label("No artist rows yet — check your connection."));
            } else {
                for (MerchTrendsService.TrendingArtist a : data.artists()) {
                    VBox row = new VBox(4);
                    row.setStyle("-fx-background-color:rgba(128,128,128,0.06);-fx-background-radius:8;-fx-padding:8 10 8 10;");
                    Label name = new Label(a.name());
                    name.getStyleClass().add("shop-filter-label");
                    name.setStyle("-fx-font-size:12px;");
                    Label blurb = new Label(a.blurb());
                    blurb.setWrapText(true);
                    blurb.getStyleClass().add("shop-rate-label");
                    row.getChildren().addAll(name, blurb);
                    trendArtistsBox.getChildren().add(row);
                }
            }
        }
        if (trendStatusLabel != null)
            trendStatusLabel.setText("Storefront " + cc.toUpperCase() + " · data from public iTunes RSS.");
    }

    private void loadRatesAsync() {
        rateLabel.setText("Fetching live exchange rates…");
        new Thread(() -> {
            rates = currencyService.getRates();
            Platform.runLater(() -> {
                if (rates != null) {
                    String info = rates.entrySet().stream()
                            .filter(e -> !e.getKey().equals("TND"))
                            .sorted(Map.Entry.comparingByKey())
                            .limit(4)
                            .map(e -> "1 TND ≈ " + String.format("%.3f", e.getValue()) + " " + e.getKey())
                            .collect(Collectors.joining("\n"));
                    rateLabel.setText(info.isEmpty() ? "Rates loaded" : info);
                } else {
                    rateLabel.setText("Exchange rates unavailable");
                }
                filterProducts();
                rebuildLocalPriceList();
            });
        }, "currency-loader").start();
    }

    private void rebuildLocalPriceList() {
        if (localPriceList == null || allProducts == null) return;
        localPriceList.getChildren().clear();
        List<Product> sorted = new ArrayList<>(allProducts);
        sorted.sort(Comparator.comparing(p -> p.getName() != null ? p.getName() : "", String.CASE_INSENSITIVE_ORDER));
        for (Product p : sorted) {
            StringBuilder sb = new StringBuilder();
            sb.append("• ").append(p.getName() != null ? p.getName() : "?");
            sb.append(" — ").append(String.format("%.2f TND", p.getPrice()));
            if (rates != null && activeCurrency != null && !"TND".equals(activeCurrency)) {
                try {
                    sb.append("  (").append(currencyService.convertLabel(p.getPrice(), activeCurrency).trim()).append(")");
                } catch (Exception ignored) { }
            }
            Label line = new Label(sb.toString());
            line.setWrapText(true);
            line.getStyleClass().add("shop-rate-label");
            localPriceList.getChildren().add(line);
        }
    }

    private static String infoLabel(String code) {
        CurrencyService.CurrencyInfo info = CurrencyService.SUPPORTED.get(code);
        return info == null ? code : info.label();
    }

    /** Reverse "🇹🇳  TND" → "TND". */
    private static String codeFromLabel(String label) {
        if (label == null) return "TND";
        for (Map.Entry<String, CurrencyService.CurrencyInfo> e : CurrencyService.SUPPORTED.entrySet()) {
            if (label.equals(e.getValue().label())) return e.getKey();
        }
        // tolerate raw codes
        if (CurrencyService.SUPPORTED.containsKey(label)) return label;
        return "TND";
    }

    // ── Filter / Sort ─────────────────────────────────────────────────

    @FXML void filterProducts() {
        if (allProducts == null) return;
        String q    = searchField.getText().toLowerCase().trim();
        String sort = sortFilter.getValue();
        String sizeChoice = sizeFilter != null ? sizeFilter.getValue() : null;
        double maxP = priceSlider.getValue();

        List<Product> filtered = allProducts.stream()
                .filter(p -> q.isEmpty()
                        || p.getName().toLowerCase().contains(q)
                        || (p.getArtistName()  != null && p.getArtistName().toLowerCase().contains(q))
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q)))
                .filter(p -> p.getPrice() <= maxP)
                .filter(p -> matchesSizeFilter(p, sizeChoice))
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Price ↑"    -> filtered.sort(Comparator.comparingDouble(Product::getPrice));
            case "Price ↓"    -> filtered.sort(Comparator.comparingDouble(Product::getPrice).reversed());
            case "Artist A–Z" -> filtered.sort(Comparator.comparing(p -> p.getArtistName() != null ? p.getArtistName() : ""));
            case "Name A–Z"   -> filtered.sort(Comparator.comparing(p -> p.getName() != null ? p.getName() : ""));
        }
        renderProducts(filtered);
    }

    /** Products with no size count as “fits any filter”. */
    private static boolean matchesSizeFilter(Product p, String sizeChoice) {
        if (sizeChoice == null || sizeChoice.isBlank() || "All sizes".equalsIgnoreCase(sizeChoice))
            return true;
        String ps = p.getSize();
        if (ps == null || ps.isBlank()) return true;
        return ps.trim().equalsIgnoreCase(sizeChoice.trim());
    }

    @FXML void onCurrencyChanged() {
        activeCurrency = codeFromLabel(currencyCombo.getValue());
        if (allProducts != null) filterProducts();
        rebuildLocalPriceList();
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
        VBox card = new VBox(0);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(220);

        // ── Album-art header (AudioDB API) ──────────────────────────────
        StackPane imgHeader = new StackPane();
        imgHeader.getStyleClass().add("product-card-img-header");
        imgHeader.setPrefHeight(110);

        // Gradient fallback colour based on artist name
        String[] colors = {"#6366F1","#E8320A","#10B981","#F59E0B","#8B5CF6","#0EA5E9"};
        String col = colors[Math.abs((p.getArtistName() != null ? p.getArtistName() : p.getName()).hashCode()) % colors.length];
        imgHeader.setStyle("-fx-background-color:linear-gradient(to bottom right," + col + "33," + col + "11);"
                + "-fx-background-radius:16 16 0 0;");

        // Artist initial label (always visible until image loads)
        Label initial = new Label(p.getArtistName() != null && !p.getArtistName().isBlank()
                ? p.getArtistName().substring(0, 1).toUpperCase() : "?");
        initial.setStyle("-fx-font-size:36px;-fx-font-weight:700;-fx-text-fill:" + col + ";-fx-opacity:0.7;");
        imgHeader.getChildren().add(initial);

        // Low-stock badge (Advanced Feature — ≤ 5 left)
        if (p.getStock() > 0 && p.getStock() <= 5) {
            Label lowStock = new Label("🔥 ONLY " + p.getStock() + " LEFT");
            lowStock.getStyleClass().add("product-low-stock-badge");
            StackPane.setAlignment(lowStock, javafx.geometry.Pos.TOP_RIGHT);
            imgHeader.getChildren().add(lowStock);
        }

        // ── Image priority: uploaded file (admin) → AudioDB → genre placeholder
        java.io.File local = null;
        if (p.getImage() != null && !p.getImage().isBlank()) {
            java.io.File f = new java.io.File("uploads", p.getImage());
            if (f.exists()) local = f;
        }
        if (local != null) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(local.toURI().toString(), 220, 110, false, true, true);
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(220); iv.setFitHeight(110);
                iv.setPreserveRatio(false);
                iv.setStyle("-fx-opacity:1;");
                imgHeader.getChildren().add(0, iv);
                // Hide initial letter when we have a real photo.
                initial.setVisible(false);
            } catch (Exception ignored) {}
        } else if (p.getArtistName() != null && !p.getArtistName().isBlank()) {
            new Thread(() -> {
                String url = AudioDBService.getArtistThumb(p.getArtistName());
                if (url == null) url = AudioDBService.getAlbumThumb(p.getArtistName());
                final String resolved = url;
                Platform.runLater(() -> {
                    try {
                        if (resolved != null) {
                            javafx.scene.image.Image img = new javafx.scene.image.Image(resolved, 220, 110, false, true, true);
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                            iv.setFitWidth(220); iv.setFitHeight(110);
                            iv.setPreserveRatio(false);
                            iv.setStyle("-fx-opacity:0.6;");
                            imgHeader.getChildren().add(0, iv);
                        }
                    } catch (Exception ignored) {}
                });
            }, "audiodb-" + p.getId()).start();
        }

        // ── Card body ────────────────────────────────────────────────────
        VBox body = new VBox(8);
        body.setStyle("-fx-padding:14 16 16 16;");

        // Badges row
        HBox badges = new HBox(6);
        Label artist = new Label(p.getArtistName() != null ? p.getArtistName().toUpperCase() : "ARTIST");
        artist.getStyleClass().add("card-genre-badge-acid");
        badges.getChildren().add(artist);
        if (p.getSize() != null && !p.getSize().isBlank()) {
            Label sz = new Label(p.getSize().trim());
            sz.getStyleClass().add("card-genre-badge");
            sz.setStyle("-fx-font-size:9px;-fx-padding:2 6 2 6;");
            badges.getChildren().add(sz);
        }

        Label name = new Label(p.getName());
        name.getStyleClass().add("card-event-title");
        name.setWrapText(true);

        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            Label desc = new Label(p.getDescription());
            desc.getStyleClass().add("card-event-desc");
            desc.setWrapText(true); desc.setMaxHeight(36);
            body.getChildren().add(desc);
        }

        // Price — TND only (no secondary currency clutter)
        Label tndPrice = new Label(String.format("%.2f TND", p.getPrice()));
        tndPrice.getStyleClass().add("card-price-main");

        boolean inStock = p.getStock() > 0 && p.isAvailable();
        Label stock = new Label(inStock ? "✅  " + p.getStock() + " left" : "❌  Sold out");
        stock.getStyleClass().add(inStock ? "badge-upcoming" : "badge-status");

        Button addBtn = new Button(inStock ? "🛒  ADD TO CART" : "SOLD OUT");
        addBtn.getStyleClass().add(inStock ? "card-reserve-btn-large" : "card-action-btn-outline");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setDisable(!inStock);
        addBtn.setOnAction(e -> onAddToCart(p, addBtn));

        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        body.getChildren().addAll(badges, name, tndPrice, stock, sp, addBtn);
        card.getChildren().addAll(imgHeader, body);

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

            double subtotal = cart.getTotal();
            ShippingEstimateService.Estimate est = ShippingEstimateService.forCart(subtotal);
            double total = subtotal + est.feeTnd();

            // Sidebar shipping fee + cart shipping line
            if (cartSubtotalLabel != null) cartSubtotalLabel.setText(String.format("%.2f TND", subtotal));
            if (cartShippingLabel != null) cartShippingLabel.setText(est.freeShippingApplied()
                    ? "FREE"
                    : String.format("%.2f TND", est.feeTnd()));

            cartTotalLabel.setText(String.format("%.2f TND", total));
            cartTotalConverted.setText(!"TND".equals(activeCurrency)
                    ? currencyService.convertLabel(total, activeCurrency) : "");
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
