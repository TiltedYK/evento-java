package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Event;
import model.Product;
import model.User;
import service.CartService;
import service.EventService;
import service.ProductService;
import util.OverlayService;
import util.ThemeManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class FrontDashboardController implements Initializable {

    @FXML private StackPane frontContentArea;
    @FXML private VBox      heroPane;
    @FXML private HBox      featuredEventsBox;
    @FXML private HBox      featuredMerchBox;
    @FXML private Label     userLabel;
    @FXML private Label     heroLogo;
    @FXML private Label     heroTitle;
    @FXML private Button    themeToggle;
    @FXML private Label     npDot;
    @FXML private Button    muteBtn;

    @FXML private Button btnNavHome;
    @FXML private Button btnNavArtists;
    @FXML private Button btnNavEvents;
    @FXML private Button btnNavShop;
    @FXML private Button btnNavMyRes;
    @FXML private Button btnNavBlog;
    @FXML private Button btnNavPromo;
    @FXML private Button btnNavProfile;
    @FXML private Button btnNavApplyPartner;
    @FXML private Button btnNavPartnerHub;
    @FXML private Button btnNavInfluencerHub;

    private MediaPlayer  heroPlayer;
    private User         currentUser;

    private final EventService   eventService   = new EventService();
    private final ProductService productService = new ProductService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        OverlayService.init(frontContentArea);
        setupAudio();
        loadFeaturedEvents();
        loadFeaturedMerch();
        animateHero();
        startNowPlayingPulse();
        CartService.getInstance().setOnChange(() ->
                Platform.runLater(this::updateShopBadge));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        refreshUserData();
    }

    public void refreshUserData() {
        if (currentUser == null) return;
        
        try {
            service.UserService userService = new service.UserService();
            User latest = userService.recupererParId(currentUser.getId());
            if (latest != null) {
                currentUser.setRoles(latest.getRoles()); // Sync roles
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh user roles: " + e.getMessage());
        }

        userLabel.setText("🎸 " + currentUser.getPrenom() + " " + currentUser.getNom());
        
        String roles = currentUser.getRoles();
        if (roles != null) {
            boolean isPartner = roles.contains("ROLE_PARTNER");
            boolean isInfluencer = roles.contains("ROLE_INFLUENCER");

            btnNavApplyPartner.setVisible(!isPartner);
            btnNavApplyPartner.setManaged(!isPartner);
            
            btnNavPartnerHub.setVisible(isPartner);
            btnNavPartnerHub.setManaged(isPartner);
            
            btnNavInfluencerHub.setVisible(isInfluencer);
            btnNavInfluencerHub.setManaged(isInfluencer);
        }
    }

    public void initTheme(Scene scene) {
        ThemeManager.setScene(scene);
        ThemeManager.apply();
        updateThemeIcon();
    }

    // ── Audio ──────────────────────────────────────────────────────────

    private void setupAudio() {
        try {
            URL audio = getClass().getResource("/video/music.mp4");
            if (audio == null) return;
            heroPlayer = new MediaPlayer(new Media(audio.toExternalForm()));
            heroPlayer.setAutoPlay(true);
            heroPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            heroPlayer.setVolume(0.3);
        } catch (Exception e) {
            System.err.println("Audio: " + e.getMessage());
        }
    }

    // ── Hero animations ────────────────────────────────────────────────

    private void animateHero() {
        // Glitch on subtitle
        final String text = "THE SHOW NEVER STOPS";
        final String[] gc  = {"█", "▓", "░", "▒"};
        final int[] frame  = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(80), e -> {
            if (frame[0] < 10) {
                StringBuilder sb = new StringBuilder();
                for (char ch : text.toCharArray())
                    sb.append(Math.random() < 0.13 ? gc[(int)(Math.random()*4)] : ch);
                heroTitle.setText(sb.toString());
            } else { heroTitle.setText(text); }
            frame[0]++;
        }));
        tl.setCycleCount(12);
        PauseTransition d = new PauseTransition(Duration.millis(600));
        d.setOnFinished(e -> tl.play());
        d.play();

        // Logo pulse
        ScaleTransition pulse = new ScaleTransition(Duration.millis(2400), heroLogo);
        pulse.setFromX(1.0); pulse.setToX(1.04);
        pulse.setFromY(1.0); pulse.setToY(1.04);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
    }

    // ── Mute toggle ───────────────────────────────────────────────────

    @FXML void toggleMute() {
        if (heroPlayer == null) return;
        boolean nowMuted = !heroPlayer.isMute();
        heroPlayer.setMute(nowMuted);
        if (muteBtn != null) muteBtn.setText(nowMuted ? "🔇" : "🔊");
    }

    // ── Now Playing pulse ─────────────────────────────────────────────

    private void startNowPlayingPulse() {
        if (npDot == null) return;
        FadeTransition blink = new FadeTransition(Duration.millis(900), npDot);
        blink.setFromValue(1.0);
        blink.setToValue(0.2);
        blink.setCycleCount(Animation.INDEFINITE);
        blink.setAutoReverse(true);
        blink.play();
    }

    // ── Featured cards ─────────────────────────────────────────────────

    private void loadFeaturedEvents() {
        try {
            List<Event> events = eventService.recuperer();
            featuredEventsBox.getChildren().clear();
            events.stream().limit(6).forEach(ev -> {
                VBox card = buildEventCard(ev);
                featuredEventsBox.getChildren().add(card);
                animateIn(card, featuredEventsBox.getChildren().size() * 80L);
            });
            if (events.isEmpty())
                featuredEventsBox.getChildren().add(emptyLabel("No events yet. Stay tuned."));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadFeaturedMerch() {
        try {
            List<Product> products = productService.recuperer();
            featuredMerchBox.getChildren().clear();
            products.stream().limit(6).forEach(p -> {
                VBox card = buildMerchCard(p);
                featuredMerchBox.getChildren().add(card);
                animateIn(card, featuredMerchBox.getChildren().size() * 80L);
            });
            if (products.isEmpty())
                featuredMerchBox.getChildren().add(emptyLabel("Merch dropping soon…"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox buildEventCard(Event ev) {
        VBox card = new VBox(10);
        card.getStyleClass().add("home-event-card");
        card.setPrefWidth(220);

        Label genre = new Label(ev.getGenre() != null ? ev.getGenre().toUpperCase() : "MUSIC");
        genre.getStyleClass().add("card-genre-badge");

        Label title = new Label(ev.getTitre());
        title.getStyleClass().add("card-event-title");
        title.setWrapText(true);

        Label venue = new Label("📍 " + (ev.getVenue() != null ? ev.getVenue() : "TBA"));
        venue.getStyleClass().add("card-meta-text");

        String dateStr = ev.getDateHeure() != null
                ? ev.getDateHeure().format(DateTimeFormatter.ofPattern("MMM dd · HH:mm")) : "TBA";
        Label date = new Label("📅 " + dateStr);
        date.getStyleClass().add("card-meta-text");

        Button btn = new Button("GET TICKET");
        btn.getStyleClass().add("card-action-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> showEvents());

        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        card.getChildren().addAll(genre, title, venue, date, sp, btn);
        hoverPop(card); return card;
    }

    private VBox buildMerchCard(Product p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("home-merch-card");
        card.setPrefWidth(200);

        Label artist = new Label(p.getArtistName() != null ? p.getArtistName().toUpperCase() : "ARTIST");
        artist.getStyleClass().add("card-genre-badge-acid");

        Label name = new Label(p.getName());
        name.getStyleClass().add("card-event-title");
        name.setWrapText(true);

        Label price = new Label(String.format("%.2f TND", p.getPrice()));
        price.getStyleClass().add("card-price-label");

        Label stock = new Label(p.getStock() > 0 ? "✅  In stock" : "❌  Sold out");
        stock.getStyleClass().add("card-meta-text");

        Button btn = new Button("VIEW");
        btn.getStyleClass().add("card-action-btn-outline");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> showShop());

        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        card.getChildren().addAll(artist, name, price, stock, sp, btn);
        hoverPop(card); return card;
    }

    // ── Navigation ─────────────────────────────────────────────────────

    @FXML void showHome()           { refreshUserData(); setActive(btnNavHome);    switchContent(heroPane); }
    @FXML void showArtists()        { setActive(btnNavArtists); navigateTo("/fxml/FrontPartners.fxml"); }
    @FXML void showEvents()         { setActive(btnNavEvents);  navigateTo("/fxml/FrontEvents.fxml"); }
    @FXML void showShop()           { setActive(btnNavShop);    navigateTo("/fxml/FrontShop.fxml"); }
    @FXML void showMyReservations() { setActive(btnNavMyRes);   navigateTo("/fxml/FrontMyReservations.fxml"); }
    @FXML void showBlog()           { setActive(btnNavBlog);    navigateTo("/fxml/FrontBlog.fxml"); }
    @FXML void showPromos()         { setActive(btnNavPromo);   navigateTo("/fxml/FrontPromo.fxml"); }
    @FXML void showProfile()        { refreshUserData(); setActive(btnNavProfile); navigateTo("/fxml/FrontProfile.fxml"); }
    @FXML void showApplyPartner()   { setActive(btnNavApplyPartner); navigateTo("/fxml/FrontPartnershipApply.fxml"); }
    @FXML void showPartnerHub()     { setActive(btnNavPartnerHub); navigateTo("/fxml/FrontPartnerHub.fxml"); }
    @FXML void showInfluencerHub()  { setActive(btnNavInfluencerHub); navigateTo("/fxml/FrontInfluencerHub.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof FrontEventsController c)              c.setCurrentUser(currentUser);
            else if (ctrl instanceof FrontShopController c)           { c.setCurrentUser(currentUser); c.setDashboard(this); }
            else if (ctrl instanceof FrontMyReservationsController c) c.setCurrentUser(currentUser);
            else if (ctrl instanceof FrontPartnersController c)       c.setCurrentUser(currentUser);
            else if (ctrl instanceof FrontBlogController c)           c.setCurrentUser(currentUser);
            else if (ctrl instanceof FrontPromoController c)          c.setCurrentUser(currentUser);
            else if (ctrl instanceof FrontProfileController c)        c.setCurrentUser(currentUser);
            else if (ctrl instanceof FrontPartnershipApplyController c) c.setCurrentUser(currentUser);
            else if (ctrl instanceof FrontPartnerHubController c)     c.setCurrentUser(currentUser);
            else if (ctrl instanceof FrontInfluencerHubController c)  c.setCurrentUser(currentUser);
            switchContent(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    void switchContent(Parent view) {
        OverlayService.hide();
        frontContentArea.getChildren().setAll(view);
        view.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(260), view);
        ft.setToValue(1); ft.play();
    }

    private void setActive(Button btn) {
        Button[] all = {btnNavHome, btnNavArtists, btnNavEvents, btnNavShop, btnNavMyRes, btnNavBlog, btnNavPromo, btnNavProfile, btnNavApplyPartner, btnNavPartnerHub, btnNavInfluencerHub};
        for (Button b : all) {
            if (b != null) b.getStyleClass().remove("active");
        }
        if (btn != null) btn.getStyleClass().add("active");
    }

    // ── Theme toggle ──────────────────────────────────────────────────

    @FXML void toggleTheme() {
        ThemeManager.toggle();
        updateThemeIcon();
    }

    private void updateThemeIcon() {
        themeToggle.setText(ThemeManager.isDark() ? "☀" : "🌙");
    }

    // ── Cart badge ────────────────────────────────────────────────────

    public void updateShopBadge() {
        int n = CartService.getInstance().getTotalItems();
        btnNavShop.setText(n > 0 ? "MERCH [" + n + "]" : "MERCH");
    }

    // ── Logout ────────────────────────────────────────────────────────

    @FXML void handleLogout() {
        if (heroPlayer != null) heroPlayer.stop();
        CartService.getInstance().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1240, 760);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/front-light.css").toExternalForm());
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setTitle("EVENTO — Login");
            root.setOpacity(0);
            stage.setScene(scene);
            FadeTransition ft = new FadeTransition(Duration.millis(450), root);
            ft.setToValue(1); ft.play();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void animateIn(VBox card, long delayMs) {
        card.setOpacity(0); card.setTranslateY(14);
        PauseTransition p = new PauseTransition(Duration.millis(delayMs));
        p.setOnFinished(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(350), card); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(350), card);
            tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
        });
        p.play();
    }

    private void hoverPop(VBox card) {
        ScaleTransition up   = new ScaleTransition(Duration.millis(130), card); up.setToX(1.04); up.setToY(1.04);
        ScaleTransition down = new ScaleTransition(Duration.millis(130), card); down.setToX(1.0); down.setToY(1.0);
        card.setOnMouseEntered(e -> up.play());
        card.setOnMouseExited(e  -> down.play());
    }

    private Label emptyLabel(String t) {
        Label l = new Label(t); l.getStyleClass().add("front-empty-label"); return l;
    }
}
