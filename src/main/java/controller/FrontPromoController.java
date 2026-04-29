package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import model.Collaboration;
import model.User;
import service.*;
import util.OverlayService;
import util.Router;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class FrontPromoController implements Initializable {

    @FXML private FlowPane         adGrid;
    @FXML private Label            countLabel;
    @FXML private Label            geoLabel;
    @FXML private StackPane        heroBanner;
    @FXML private Label            heroTitleLabel;
    @FXML private Label            heroDescLabel;
    @FXML private Button           btnAll, btnVideo, btnImage, btnAffiche;
    @FXML private VBox             partnerHelpBox;

    private final CollaborationService colService  = new CollaborationService();
    private final ReferralHitService   hitService  = new ReferralHitService();

    /**
     * Set of {@code "collabId:userId"} pairs already counted as an impression
     * during this JVM session. Prevents double-counting when the user clicks
     * a filter button, navigates away and back, or whenever {@link #renderAds}
     * runs more than once for the same (campaign, viewer) pair.
     *
     * Static so it survives controller re-instantiation on navigation.
     * Cleared on app restart, which is the desired behaviour: a fresh launch
     * of the app DOES legitimately constitute a new impression.
     */
    private static final java.util.Set<String> SESSION_IMPRESSIONS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private List<Collaboration> allAds = new ArrayList<>();
    private User                currentUser;
    private GeoLocationService.Location userLocation;
    private final List<Timeline> activeTimers = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Safe check for FXML injection
        if (adGrid == null) {
            System.err.println("Critical Error: adGrid is NOT injected!");
            return;
        }

        loadGeoAsync();
        loadAds();
        
        if (btnAll != null) setActiveCategory(btnAll);
    }

    public void setCurrentUser(User user) { 
        this.currentUser = user; 
        if (currentUser != null && partnerHelpBox != null) {
            boolean isPartner = currentUser.getRoles() != null && currentUser.getRoles().contains("ROLE_PARTNER");
            partnerHelpBox.setVisible(!isPartner);
            partnerHelpBox.setManaged(!isPartner);
        }
    }

    private void loadGeoAsync() {
        if (geoLabel != null) geoLabel.setText("📍 Detecting location...");
        new Thread(() -> {
            try {
                userLocation = GeoLocationService.getLocation();
                if (userLocation != null \u0026\u0026 geoLabel != null) {
                    Platform.runLater(() -> geoLabel.setText("📍 " + userLocation.display()));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadAds() {
        try {
            allAds = colService.recuperer().stream()
                    .filter(c -> "approved".equalsIgnoreCase(c.getStatus()) || "validated".equalsIgnoreCase(c.getStatus()))
                    .collect(Collectors.toList());
            updateHeroBanner();
            renderAds(allAds);
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                if (countLabel != null) countLabel.setText("Error loading ads");
            });
        }
    }

    private void updateHeroBanner() {
        if (heroBanner == null) return;
        if (allAds == null || allAds.isEmpty()) {
            heroBanner.setVisible(false);
            heroBanner.setManaged(false);
            return;
        }

        new Thread(() -> {
            try {
                Collaboration topCollab = allAds.stream()
                    .max(Comparator.comparingInt(c -> hitService.getClicks(c.getId())))
                    .orElse(allAds.get(0));

                Platform.runLater(() -> {
                    if (heroTitleLabel != null) heroTitleLabel.setText("Trending: " + topCollab.getTitle());
                    if (heroDescLabel != null) heroDescLabel.setText("Check out our most popular campaign!");
                    heroBanner.setOnMouseClicked(e -> handleVisit(topCollab, null));
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void filterAll()     { setActiveCategory(btnAll); renderAds(allAds); }
    @FXML void filterVideo()   { setActiveCategory(btnVideo); filterByType("video"); }
    @FXML void filterImage()   { setActiveCategory(btnImage); filterByType("image"); }
    @FXML void filterAffiche() { setActiveCategory(btnAffiche); filterByType("affiche"); }

    private void filterByType(String type) {
        List<Collaboration> filtered = allAds.stream()
                .filter(c -> type.equalsIgnoreCase(c.getType()))
                .collect(Collectors.toList());
        renderAds(filtered);
    }

    private void setActiveCategory(Button active) {
        Button[] btns = {btnAll, btnVideo, btnImage, btnAffiche};
        for (Button b : btns) if (b != null) b.getStyleClass().remove("promo-side-btn-active");
        if (active != null) active.getStyleClass().add("promo-side-btn-active");
    }

    @FXML void handleApply() {
        Router.navigate("/fxml/FrontPartnershipApply.fxml");
    }

    private void renderAds(List<Collaboration> ads) {
        if (adGrid == null) return;
        
        activeTimers.forEach(Animation::stop);
        activeTimers.clear();
        adGrid.getChildren().clear();

        if (countLabel != null) countLabel.setText(ads.size() + " campaign" + (ads.size() == 1 ? "" : "s"));

        if (ads.isEmpty()) {
            Label empty = new Label("📭 No active campaigns right now.");
            empty.getStyleClass().add("front-empty-label");
            adGrid.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < ads.size(); i++) {
            try {
                VBox card = buildAdCard(ads.get(i));
                adGrid.getChildren().add(card);
                animateIn(card, i * 50L);

                // Record an impression for this card — but only once per
                // (campaign, viewer) pair per app session, otherwise filter
                // clicks and re-navigation would inflate the count.
                final Collaboration col = ads.get(i);
                final Integer viewerId = currentUser != null ? currentUser.getId() : null;
                final String key = col.getId() + ":" + (viewerId != null ? viewerId : 0);
                if (SESSION_IMPRESSIONS.add(key)) {
                    new Thread(() -> hitService.recordImpression(col.getId(), viewerId),
                            "ImpressionWriter").start();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private VBox buildAdCard(Collaboration col) {
        VBox card = new VBox(0);
        card.getStyleClass().add("promo-card");
        card.setPrefWidth(340);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getStyleClass().add("promo-card-badges");

        Label sponsoredBadge = new Label("SPONSORED");
        sponsoredBadge.getStyleClass().add("promo-badge-sponsored");
        badges.getChildren().add(sponsoredBadge);

        if ("video".equalsIgnoreCase(col.getType())) {
            Label vidBadge = new Label("▶ VIDEO");
            vidBadge.getStyleClass().add("promo-badge-video");
            badges.getChildren().add(vidBadge);
        }

        Region badgeSpacer = new Region(); HBox.setHgrow(badgeSpacer, Priority.ALWAYS);
        badges.getChildren().add(badgeSpacer);

        if (col.getEndDate() != null \u0026\u0026 !col.getEndDate().isBefore(LocalDate.now())) {
            Label countdown = new Label();
            countdown.getStyleClass().add("promo-countdown");
            updateCountdown(countdown, col.getEndDate());
            badges.getChildren().add(countdown);
        }

        // -- Media Display (Image/Affiche) --
        if (col.getFileName() != null && !col.getFileName().isBlank()) {
            try {
                // Assuming images are stored in project root /uploads/
                File file = new File("uploads/" + col.getFileName());
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(340); iv.setPreserveRatio(true);
                    card.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }

        String color = "video".equalsIgnoreCase(col.getType()) ? "#E8320A" : "#6366F1";
        Region topBand = new Region();
        topBand.setPrefHeight(6); topBand.setMaxWidth(Double.MAX_VALUE);
        topBand.setStyle("-fx-background-color:" + color + ";");

        VBox body = new VBox(10);
        body.getStyleClass().add("promo-card-body");

        Label partnerName = new Label(col.getTitle() != null ? col.getTitle() : "Partner");
        partnerName.getStyleClass().add("promo-partner-name");

        Label previewTitle = new Label("Loading campaign details...");
        previewTitle.getStyleClass().add("promo-preview-title");
        previewTitle.setWrapText(true);
        Label previewDesc = new Label("");
        previewDesc.getStyleClass().add("promo-preview-desc");
        previewDesc.setWrapText(true);

        if (col.getLinkUrl() != null \u0026\u0026 !col.getLinkUrl().isBlank()) {
            loadPreviewAsync(col.getLinkUrl(), previewTitle, previewDesc);
        } else {
            previewTitle.setText(col.getTitle());
            previewDesc.setText("Exclusive collaboration.");
        }

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getStyleClass().add("promo-meta-row");

        Label views = new Label("👁 " + col.getViewCount() + " views");
        views.getStyleClass().add("promo-meta-item");
        
        Label gradeLabel = new Label("⭐ —");
        gradeLabel.getStyleClass().add("promo-grade-label");
        loadGradeAsync(col.getId(), gradeLabel);

        meta.getChildren().addAll(views, new Region(), gradeLabel);
        HBox.setHgrow(meta.getChildren().get(1), Priority.ALWAYS);

        HBox actions = new HBox(10);
        Button visitBtn = new Button("🔗 VISIT");
        visitBtn.getStyleClass().add("promo-visit-btn");
        visitBtn.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(visitBtn, Priority.ALWAYS);
        visitBtn.setOnAction(e -> handleVisit(col, visitBtn));

        Button qrBtn = new Button("📱 QR");
        qrBtn.getStyleClass().add("card-action-btn-outline");
        qrBtn.setOnAction(e -> showQR(col));

        actions.getChildren().addAll(visitBtn, qrBtn);

        body.getChildren().addAll(partnerName, previewTitle, previewDesc, meta, actions);
        card.getChildren().addAll(topBand, badges, body);

        hoverPop(card);
        return card;
    }

    private void loadPreviewAsync(String url, Label titleLbl, Label descLbl) {
        new Thread(() -> {
            try {
                LinkPreviewService.Preview p = LinkPreviewService.fetch(url);
                Platform.runLater(() -> {
                    titleLbl.setText(p.title());
                    descLbl.setText(p.description().length() > 100 ? p.description().substring(0, 100) + "..." : p.description());
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadGradeAsync(int collabId, Label gradeLabel) {
        new Thread(() -> {
            try {
                String grade = hitService.effectivenessGrade(collabId);
                Platform.runLater(() -> gradeLabel.setText("⭐ " + grade));
            } catch (Exception ignored) {}
        }).start();
    }

    private void handleVisit(Collaboration col, Button visitBtn) {
        new Thread(() -> {
            hitService.recordClick(col.getId(), currentUser != null ? currentUser.getId() : null);
        }).start();

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("overlay-body");
        content.setMinWidth(450);

        Label t = new Label(col.getTitle()); 
        t.getStyleClass().add("overlay-post-title");
        t.setStyle("-fx-font-size: 24px;");

        VBox mediaBox = new VBox();
        mediaBox.setAlignment(Pos.CENTER);
        if (col.getFileName() != null && !col.getFileName().isBlank()) {
            try {
                File file = new File("uploads/" + col.getFileName());
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(400); iv.setPreserveRatio(true);
                    mediaBox.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }

        Button openBtn = new Button("🌐  OPEN OFFICIAL CAMPAIGN PAGE");
        openBtn.getStyleClass().add("hero-cta-btn");
        openBtn.setMaxWidth(Double.MAX_VALUE);
        openBtn.setOnAction(e -> {
            if (col.getLinkUrl() != null && !col.getLinkUrl().isBlank()) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(col.getLinkUrl()));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        Label stats = new Label("Campaign Analytics: " + hitService.getImpressions(col.getId()) + " views · " + hitService.effectivenessGrade(col.getId()) + " Grade");
        stats.getStyleClass().add("promo-meta-item");

        content.getChildren().addAll(t, mediaBox, stats, openBtn);
        OverlayService.show("Campaign Details", content);
    }

    private void showQR(Collaboration col) {
        try {
            QRCodeService qr = new QRCodeService();
            javafx.scene.image.Image img = qr.generateQRCode(col.getLinkUrl(), 200);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            OverlayService.show("QR Code", new VBox(iv));
        } catch (Exception ignored) {}
    }

    private void updateCountdown(Label label, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        if (days < 0) label.setText("Expired");
        else label.setText("⏰ " + days + "d left");
    }

    private void animateIn(VBox card, long delayMs) {
        card.setOpacity(0);
        PauseTransition p = new PauseTransition(Duration.millis(delayMs));
        p.setOnFinished(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(300), card);
            ft.setToValue(1); ft.play();
        });
        p.play();
    }

    private void hoverPop(VBox card) {
        ScaleTransition up = new ScaleTransition(Duration.millis(150), card); up.setToX(1.03); up.setToY(1.03);
        ScaleTransition dn = new ScaleTransition(Duration.millis(150), card); dn.setToX(1.0); dn.setToY(1.0);
        card.setOnMouseEntered(e -> up.play()); card.setOnMouseExited(e -> dn.play());
    }
}
