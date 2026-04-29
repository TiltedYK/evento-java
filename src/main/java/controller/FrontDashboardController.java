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
import service.AudioDBService;
import service.CartService;
import service.EventService;
import service.ProductService;
import util.OverlayService;
import util.ThemeManager;

import javafx.scene.control.Tooltip;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;

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

    // Music player
    @FXML private Button                          btnPlayPause;
    @FXML private Button                          btnPrev;
    @FXML private Button                          btnNext;
    @FXML private javafx.scene.control.Slider playerProgress;
    @FXML private Label                           playerElapsed;
    @FXML private Label                           playerDuration;
    @FXML private Label                           npTrackLabel;

    @FXML private Button btnNavHome;
    @FXML private Button btnNavArtists;
    @FXML private Button btnNavEvents;
    @FXML private Button btnNavShop;
    @FXML private Button btnNavMyRes;
    @FXML private Button btnNavBlog;
    @FXML private Button btnNavPromo;
    @FXML private Button btnNavProfile;
    @FXML private Button btnNavApplyPartner;
    @FXML private Button btnNavArtistHub;
    @FXML private Button btnNavPartnerHub;
    @FXML private Button btnNavInfluencerHub;

    public static final String BUILD_TAG = "EVENTO_FRONT_BUILD_2026_04_28_MUSIC_THEMES_V1";

    private MediaPlayer  heroPlayer;
    private User         currentUser;
    private boolean      isPlaying = true;
    private int          currentTrack = 0;

    /**
     * The playlist is now driven by {@link util.ThemeManager.Theme}: each track
     * has its own theme (palette + font + transition).
     *   index 0  → METAL  (Linkin Park — In The End)
     *   index 1  → KPOP   ((G)I-DLE — Uh-Oh)
     *   index 2  → CHILL  (Noor & Selim Arjoun — Streams)
     */
    private static final ThemeManager.Theme[] PLAYLIST = {
            ThemeManager.Theme.METAL,
            ThemeManager.Theme.KPOP,
            ThemeManager.Theme.CHILL
    };

    private final EventService   eventService   = new EventService();
    private final ProductService productService = new ProductService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        OverlayService.init(frontContentArea);
        // Clip the center content pane so it never overflows into the navbar and blocks clicks
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(frontContentArea.widthProperty());
        clip.heightProperty().bind(frontContentArea.heightProperty());
        frontContentArea.setClip(clip);
        setupAudio();
        setupSliderSeek();
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

    /** Re-fetches the user's roles from DB and toggles the partner/influencer
     *  nav buttons accordingly. Safe to call at any time. */
    public void refreshUserData() {
        if (currentUser == null) return;

        try {
            User latest = new service.UserService().recupererParId(currentUser.getId());
            if (latest != null) currentUser.setRoles(latest.getRoles()); // sync roles
        } catch (Exception e) {
            System.err.println("Failed to refresh user roles: " + e.getMessage());
        }

        userLabel.setText("🎸 " + currentUser.getPrenom() + " " + currentUser.getNom());

        String roles = currentUser.getRoles();
        if (roles != null && btnNavApplyPartner != null) {
            boolean isPartner    = roles.contains("ROLE_PARTNER");
            boolean isInfluencer = roles.contains("ROLE_INFLUENCER");
            boolean isArtist     = roles.contains("ROLE_ARTIST");

            btnNavApplyPartner.setVisible(!isPartner && !isArtist);
            btnNavApplyPartner.setManaged(!isPartner && !isArtist);

            btnNavArtistHub.setVisible(isArtist);
            btnNavArtistHub.setManaged(isArtist);

            btnNavPartnerHub.setVisible(isPartner);
            btnNavPartnerHub.setManaged(isPartner);

            btnNavInfluencerHub.setVisible(isInfluencer);
            btnNavInfluencerHub.setManaged(isInfluencer);
        }
    }

    /** Bind the scene + apply the current theme (random was picked in setupAudio). */
    public void initTheme(Scene scene) {
        ThemeManager.setScene(scene);
        ThemeManager.apply(ThemeManager.getCurrent()); // picked already by setupAudio
        if (themeToggle != null) {
            // The old dark/light toggle is gone — repurpose the button to "shuffle song & theme".
            themeToggle.setText("🎲");
            themeToggle.setTooltip(new Tooltip("Shuffle song & theme"));
        }
    }

    // ── Audio & Player ─────────────────────────────────────────────────

    private void setupAudio() {
        int start = playlistIndexFor(ThemeManager.getCurrent());
        if (start < 0) start = ThreadLocalRandom.current().nextInt(PLAYLIST.length);
        ThemeManager.apply(PLAYLIST[start]);
        loadTrack(start, /*animate*/ false);
    }

    private int playlistIndexFor(ThemeManager.Theme t) {
        for (int i = 0; i < PLAYLIST.length; i++) {
            if (PLAYLIST[i] == t) return i;
        }
        return -1;
    }

    private void loadTrack(int index, boolean animateThemeSwap) {
        try {
            if (heroPlayer != null) heroPlayer.stop();
            currentTrack = ((index % PLAYLIST.length) + PLAYLIST.length) % PLAYLIST.length;
            ThemeManager.Theme theme = PLAYLIST[currentTrack];
            URL audio = getClass().getResource(theme.trackPath);
            if (audio == null) {
                System.err.println("Audio missing: " + theme.trackPath);
                if (npTrackLabel != null)
                    Platform.runLater(() -> npTrackLabel.setText(theme.trackName + "  (file missing)"));
                return;
            }

            heroPlayer = new MediaPlayer(new Media(audio.toExternalForm()));
            heroPlayer.setAutoPlay(true);
            heroPlayer.setVolume(0.3);
            isPlaying = true;
            if (btnPlayPause != null) Platform.runLater(() -> btnPlayPause.setText("⏸"));
            if (npTrackLabel  != null) Platform.runLater(() -> npTrackLabel.setText(theme.trackName));

            // Theme swap with cross-fade
            Runnable swap = () -> {
                if (npTrackLabel != null) npTrackLabel.setText(theme.trackName);
            };
            if (animateThemeSwap) {
                ThemeManager.applyWithFade(theme, swap);
            } else {
                ThemeManager.apply(theme);
                swap.run();
            }

            // On track end → next (rotates METAL → KPOP → CHILL → METAL …)
            heroPlayer.setOnEndOfMedia(() -> Platform.runLater(this::playerNext));
            heroPlayer.currentTimeProperty().addListener((obs, o, n) ->
                    Platform.runLater(this::updatePlayerProgress));
            heroPlayer.totalDurationProperty().addListener((obs, o, n) ->
                    Platform.runLater(this::updatePlayerProgress));
        } catch (Exception e) {
            System.err.println("Audio: " + e.getMessage());
        }
    }

    // ── Player controls ───────────────────────────────────────────────

    @FXML void playerPlayPause() {
        if (heroPlayer == null) return;
        if (isPlaying) {
            heroPlayer.pause();
            btnPlayPause.setText("▶");
        } else {
            heroPlayer.play();
            btnPlayPause.setText("⏸");
        }
        isPlaying = !isPlaying;
    }

    @FXML void playerNext() {
        loadTrack(currentTrack + 1, /*animate*/ true);
    }

    @FXML void playerPrev() {
        if (heroPlayer != null && heroPlayer.getCurrentTime().toSeconds() > 3) {
            heroPlayer.seek(Duration.ZERO);
        } else {
            loadTrack(currentTrack - 1, /*animate*/ true);
        }
    }

    private boolean sliderSeekInProgress = false;

    private void updatePlayerProgress() {
        if (heroPlayer == null) return;
        javafx.util.Duration cur   = heroPlayer.getCurrentTime();
        javafx.util.Duration total = heroPlayer.getTotalDuration();
        if (cur == null || total == null || total.isUnknown() || total.isIndefinite()) return;
        double pct = cur.toSeconds() / total.toSeconds();
        if (playerProgress != null && !sliderSeekInProgress)
            playerProgress.setValue(Math.min(pct, 1.0) * 100.0);
        if (playerElapsed  != null) playerElapsed.setText(fmt(cur));
        if (playerDuration != null) playerDuration.setText(fmt(total));
    }

    private void setupSliderSeek() {
        if (playerProgress == null) return;
        playerProgress.setOnMousePressed(e  -> sliderSeekInProgress = true);
        playerProgress.setOnMouseReleased(e -> {
            sliderSeekInProgress = false;
            if (heroPlayer != null) {
                javafx.util.Duration total = heroPlayer.getTotalDuration();
                if (total != null && !total.isUnknown())
                    heroPlayer.seek(total.multiply(playerProgress.getValue() / 100.0));
            }
        });
    }

    private String fmt(javafx.util.Duration d) {
        int secs = (int) d.toSeconds();
        return String.format("%d:%02d", secs / 60, secs % 60);
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
        VBox card = new VBox(8);
        card.getStyleClass().add("home-event-card");
        card.setPrefWidth(220);
        card.setStyle("-fx-padding:0;");

        // Cover image (LoremFlickr query keyed on genre + title)
        javafx.scene.image.ImageView cover = new javafx.scene.image.ImageView();
        cover.setFitWidth(220);
        cover.setFitHeight(110);
        cover.setPreserveRatio(false);
        cover.setSmooth(true);
        javafx.scene.layout.StackPane coverWrap = new javafx.scene.layout.StackPane(cover);
        coverWrap.setStyle("-fx-background-radius:14 14 0 0; -fx-background-color:#222;");
        coverWrap.setMinHeight(110);
        coverWrap.setMaxHeight(110);
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(220, 110);
        clip.setArcWidth(28); clip.setArcHeight(28);
        coverWrap.setClip(clip);
        String url = service.PlaceholderImageService.forEvent(ev.getTitre(), ev.getGenre(), ev.getVenue());
        service.PlaceholderImageService.loadInto(cover, url, null);

        VBox body = new VBox(8);
        body.setStyle("-fx-padding:10 14 12 14;");

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
        body.getChildren().addAll(genre, title, venue, date, sp, btn);

        card.getChildren().addAll(coverWrap, body);
        hoverPop(card); return card;
    }

    private VBox buildMerchCard(Product p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("home-merch-card");
        card.setPrefWidth(200);
        card.setStyle("-fx-padding:0;");

        String[] colors = {"#6366F1", "#E8320A", "#10B981", "#F59E0B", "#8B5CF6", "#0EA5E9"};
        String col = colors[Math.abs((p.getArtistName() != null ? p.getArtistName() : p.getName()).hashCode()) % colors.length];

        StackPane coverWrap = new StackPane();
        coverWrap.setStyle("-fx-background-radius:14 14 0 0; -fx-background-color:linear-gradient(to bottom right,"
                + col + "33," + col + "11);");
        coverWrap.setMinHeight(140);
        coverWrap.setMaxHeight(140);
        Label initial = new Label(p.getArtistName() != null && !p.getArtistName().isBlank()
                ? p.getArtistName().substring(0, 1).toUpperCase() : "?");
        initial.setStyle("-fx-font-size:32px;-fx-font-weight:700;-fx-text-fill:" + col + ";-fx-opacity:0.65;");
        coverWrap.getChildren().add(initial);

        java.io.File local = null;
        if (p.getImage() != null && !p.getImage().isBlank()) {
            java.io.File f = new java.io.File("uploads", p.getImage());
            if (f.exists()) local = f;
        }
        if (local != null) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(local.toURI().toString(), 200, 140, false, true, true);
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(200);
                iv.setFitHeight(140);
                iv.setPreserveRatio(false);
                coverWrap.getChildren().add(0, iv);
                initial.setVisible(false);
            } catch (Exception ignored) { }
        } else if (p.getArtistName() != null && !p.getArtistName().isBlank()) {
            new Thread(() -> {
                String url = AudioDBService.getArtistThumb(p.getArtistName());
                if (url == null) url = AudioDBService.getAlbumThumb(p.getArtistName());
                final String resolved = url;
                Platform.runLater(() -> {
                    try {
                        if (resolved != null) {
                            javafx.scene.image.Image img = new javafx.scene.image.Image(resolved, 200, 140, false, true, true);
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                            iv.setFitWidth(200);
                            iv.setFitHeight(140);
                            iv.setPreserveRatio(false);
                            iv.setStyle("-fx-opacity:0.65;");
                            coverWrap.getChildren().add(0, iv);
                        }
                    } catch (Exception ignored) { }
                });
            }, "home-merch-audiodb-" + p.getId()).start();
        }

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(200, 140);
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        coverWrap.setClip(clip);

        VBox body = new VBox(8);
        body.setStyle("-fx-padding:10 14 12 14;");

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

        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);
        body.getChildren().addAll(artist, name, price, stock, sp, btn);

        card.getChildren().addAll(coverWrap, body);
        hoverPop(card);
        return card;
    }

    // ── Navigation ─────────────────────────────────────────────────────

    @FXML void showHome()           { setActive(null);          switchContent(heroPane); }
    @FXML void showArtists()        { setActive(btnNavArtists); navigateTo("/fxml/FrontPartners.fxml"); }
    @FXML void showEvents()         { setActive(btnNavEvents);  navigateTo("/fxml/FrontEvents.fxml"); }
    @FXML void showShop()           { setActive(btnNavShop);    navigateTo("/fxml/FrontShop.fxml"); }
    @FXML void showMyReservations() { setActive(btnNavMyRes);   navigateTo("/fxml/FrontMyReservations.fxml"); }
    @FXML void showBlog()           { setActive(btnNavBlog);    navigateTo("/fxml/FrontBlog.fxml"); }
    @FXML void showPromos()         { setActive(btnNavPromo);   navigateTo("/fxml/FrontPromo.fxml"); }
    @FXML void showProfile()        { refreshUserData(); setActive(null); navigateTo("/fxml/FrontProfile.fxml"); }
    @FXML void showApplyPartner()   { refreshUserData(); setActive(btnNavApplyPartner);  navigateTo("/fxml/FrontPartnershipApply.fxml"); }
    @FXML void showArtistHub()      { refreshUserData(); setActive(btnNavArtistHub);     navigateTo("/fxml/FrontArtistHub.fxml"); }
    @FXML void showPartnerHub()     { refreshUserData(); setActive(btnNavPartnerHub);    navigateTo("/fxml/FrontPartnerHub.fxml"); }
    @FXML void showInfluencerHub()  { refreshUserData(); setActive(btnNavInfluencerHub); navigateTo("/fxml/FrontInfluencerHub.fxml"); }

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
            else if (ctrl instanceof FrontArtistHubController c)      c.setCurrentUser(currentUser);
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
        Button[] all = {btnNavHome, btnNavArtists, btnNavEvents, btnNavShop,
                        btnNavMyRes, btnNavBlog, btnNavPromo, btnNavProfile,
                        btnNavApplyPartner, btnNavArtistHub, btnNavPartnerHub, btnNavInfluencerHub};
        for (Button b : all) if (b != null) b.getStyleClass().remove("active");
        if (btn != null) btn.getStyleClass().add("active");
    }

    // ── Theme shuffle (replaces old dark/light toggle) ─────────────────

    /** Shuffles to a different random song (and therefore theme + font). */
    @FXML void toggleTheme() {
        // Pick a *different* track so the user actually sees a change.
        int next = currentTrack;
        if (PLAYLIST.length > 1) {
            while (next == currentTrack) {
                next = ThreadLocalRandom.current().nextInt(PLAYLIST.length);
            }
        }
        loadTrack(next, /*animate*/ true);
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
            LoginController loginCtl = loader.getController();
            Scene scene = new Scene(root, 1240, 760);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            ThemeManager.setScene(scene);
            ThemeManager.apply(ThemeManager.randomTheme());
            loginCtl.attachLoginAmbience(scene);
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
