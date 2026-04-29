package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import model.Collaboration;
import model.PartnershipRequest;
import model.User;
import service.*;
import util.OverlayService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * ARTISTS & PARTNERS.
 *
 * Artists section  = Users with ROLE_ARTIST (primary) or PartnershipRequests (fallback)
 * Partners section = Collaboration media records
 *
 * APIs:
 *   • WikipediaService → Wikipedia REST (bio lookup)
 *   • ItunesService    → iTunes Search API (free, no key) — shows discography in bio overlay
 */
public class FrontPartnersController implements Initializable {

    @FXML private FlowPane         artistsGrid;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label            artistCount;

    @FXML private FlowPane         partnersGrid;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label            partnerCount;

    @FXML private VBox      featuredBox;
    @FXML private TextField searchField;

    private final UserService               userService    = new UserService();
    private final PartnershipRequestService partnerService = new PartnershipRequestService();
    private final CollaborationService      mediaService   = new CollaborationService();

    private List<User>               roleArtists  = new ArrayList<>();
    private List<PartnershipRequest> partnerReqs  = new ArrayList<>();
    private List<Collaboration>      mediaList    = new ArrayList<>();

    private static final String[] PALETTE = {
        "#E8320A","#6366F1","#10B981","#F59E0B","#8B5CF6","#0EA5E9","#F43F5E","#14B8A6"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusFilter.getItems().addAll("All Statuses","pending","approved","rejected");
        statusFilter.setValue("approved");
        typeFilter.getItems().addAll("All Types","image","video");
        typeFilter.setValue("All Types");
        loadData();
    }

    public void setCurrentUser(User user) {}

    // ── Data loading ──────────────────────────────────────────────────

    private void loadData() {
        try { roleArtists = userService.getArtists(); }   catch (Exception e) { roleArtists  = List.of(); }
        try { partnerReqs = partnerService.recuperer(); } catch (Exception e) { partnerReqs  = List.of(); }
        try { mediaList   = mediaService.recuperer();  }  catch (Exception e) { mediaList    = List.of(); }
        filterPartners();
    }

    @FXML void filterPartners() {
        String q      = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String status = statusFilter.getValue();
        String type   = typeFilter.getValue();

        // Artists = ROLE_ARTIST users first; fallback to PartnershipRequest if empty
        boolean useRoleArtists = !roleArtists.isEmpty();

        List<Collaboration> media = mediaList.stream()
                .filter(c -> q.isEmpty() || (c.getTitle() != null && c.getTitle().toLowerCase().contains(q)))
                .filter(c -> "All Types".equals(type) || type == null
                        || (c.getType() != null && c.getType().equalsIgnoreCase(type)))
                .filter(c -> "approved".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());

        if (useRoleArtists) {
            List<User> filtered = roleArtists.stream()
                    .filter(u -> q.isEmpty()
                            || (u.getPrenom() + " " + u.getNom()).toLowerCase().contains(q)
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
            renderUserArtists(filtered);
            renderFeaturedUser(filtered.isEmpty() ? null : filtered.get(0));
        } else {
            List<PartnershipRequest> filtered = partnerReqs.stream()
                    .filter(a -> q.isEmpty()
                            || (a.getContactName() != null && a.getContactName().toLowerCase().contains(q))
                            || (a.getCompanyName() != null && a.getCompanyName().toLowerCase().contains(q)))
                    .filter(a -> "All Statuses".equals(status) || status == null
                            || (a.getStatus() != null && a.getStatus().equalsIgnoreCase(status)))
                    .collect(Collectors.toList());
            renderRequestArtists(filtered);
            renderFeaturedRequest(filtered.isEmpty() ? null : filtered.get(0));
        }

        renderMedia(media);
    }

    // ── Featured spotlight ────────────────────────────────────────────

    private void renderFeaturedUser(User star) {
        featuredBox.getChildren().clear();
        if (star == null) return;
        String name = star.getPrenom() + " " + star.getNom();
        HBox banner = featuredBanner("🎵  FEATURED ARTIST", name,
                star.getEmail() != null ? star.getEmail() : "Artist", name,
                e -> showUserBio(star));
        featuredBox.getChildren().add(banner);
    }

    private void renderFeaturedRequest(PartnershipRequest req) {
        featuredBox.getChildren().clear();
        if (req == null) return;
        String name = req.getCompanyName() != null && !req.getCompanyName().isBlank()
                ? req.getCompanyName() : req.getContactName();
        HBox banner = featuredBanner("★  FEATURED PARTNER", name,
                req.getEmail() != null ? req.getEmail() : "Partner application", name,
                e -> showRequestBio(req));
        featuredBox.getChildren().add(banner);
    }

    private HBox featuredBanner(String tag, String name, String sub, String colorKey,
                                javafx.event.EventHandler<javafx.event.ActionEvent> onAbout) {
        HBox banner = new HBox(24);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("partner-featured-banner");

        String color = PALETTE[Math.abs(colorKey.hashCode()) % PALETTE.length];
        Region block = new Region(); block.setPrefWidth(72); block.setPrefHeight(72);
        block.setStyle("-fx-background-color:" + color + ";-fx-background-radius:14;");
        Label icon = new Label("🎵"); icon.setStyle("-fx-font-size:28px;");
        StackPane av = new StackPane(block, icon); av.setMaxWidth(72);

        VBox info = new VBox(6); HBox.setHgrow(info, Priority.ALWAYS);
        Label tagLbl  = new Label(tag);  tagLbl.getStyleClass().add("partner-featured-tag");
        Label nameLbl = new Label(name); nameLbl.getStyleClass().add("partner-featured-name");
        Label subLbl  = new Label(sub);  subLbl.getStyleClass().add("partner-featured-sub");
        info.getChildren().addAll(tagLbl, nameLbl, subLbl);

        Button btn = new Button("🔍  ABOUT");
        btn.getStyleClass().add("card-action-btn");
        btn.setStyle("-fx-border-color:#F0F2F8;-fx-text-fill:#F0F2F8;-fx-background-color:transparent;");
        btn.setOnAction(onAbout);
        banner.getChildren().addAll(av, info, btn);
        return banner;
    }

    // ── Artist cards — ROLE_ARTIST users ──────────────────────────────

    private void renderUserArtists(List<User> list) {
        artistsGrid.getChildren().clear();
        artistCount.setText(list.size() + " artist" + (list.size() == 1 ? "" : "s"));
        for (int i = 0; i < list.size(); i++) {
            VBox card = buildUserCard(list.get(i), i);
            artistsGrid.getChildren().add(card);
            animateIn(card, i * 55L);
        }
        if (list.isEmpty()) {
            Label e = new Label("No artists found.");
            e.getStyleClass().add("front-empty-label");
            artistsGrid.getChildren().add(e);
        }
    }

    private VBox buildUserCard(User u, int idx) {
        String name  = u.getPrenom() + " " + u.getNom();
        String color = PALETTE[Math.abs(name.hashCode()) % PALETTE.length];

        VBox card = new VBox(0); card.getStyleClass().add("partner-card-grid");

        StackPane header = new StackPane();
        header.setStyle("-fx-background-color:" + color + "22;-fx-background-radius:16 16 0 0;-fx-pref-height:80;");
        Label ic = new Label("🎵"); ic.setStyle("-fx-font-size:30px;");
        Region bar = new Region(); bar.setPrefHeight(4); bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color:" + color + ";");
        header.getChildren().add(ic);
        card.getChildren().add(new VBox(0, header, bar));

        VBox body = new VBox(8); body.getStyleClass().add("partner-card-body");
        Label badge = new Label("ARTIST"); badge.getStyleClass().add("badge-upcoming");
        Label nameLbl = new Label(name); nameLbl.getStyleClass().add("partner-card-name"); nameLbl.setWrapText(true);
        if (u.getLocalisation() != null && !u.getLocalisation().isBlank()) {
            Label loc = new Label("📍 " + u.getLocalisation()); loc.getStyleClass().add("partner-card-meta");
            body.getChildren().add(loc);
        }
        Button aboutBtn = new Button("🔍 ABOUT + MUSIC"); aboutBtn.getStyleClass().add("card-action-btn");
        aboutBtn.setOnAction(e -> showUserBio(u));
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        body.getChildren().addAll(badge, nameLbl, sp, aboutBtn);
        card.getChildren().add(body);
        hoverPop(card); return card;
    }

    private void showUserBio(User u) {
        String name = u.getPrenom() + " " + u.getNom();

        VBox content = new VBox(14); content.getStyleClass().add("overlay-body");
        Label searching = new Label("🔍  Loading bio + top tracks for " + name + "…");
        searching.getStyleClass().add("overlay-post-body"); searching.setWrapText(true);
        content.getChildren().add(searching);
        OverlayService.show("Artist — " + name, content);

        new Thread(() -> {
            String bio    = WikipediaService.getSummary(name);
            List<ItunesService.Track> tracks = ItunesService.searchArtist(name, 5);
            Platform.runLater(() -> {
                content.getChildren().clear();
                Label titleLbl = new Label(name); titleLbl.getStyleClass().add("overlay-post-title");
                Label bioLbl   = new Label(bio);  bioLbl.getStyleClass().add("overlay-post-body"); bioLbl.setWrapText(true);
                content.getChildren().addAll(titleLbl, bioLbl);

                if (!tracks.isEmpty()) {
                    Label th = new Label("🎵  TOP TRACKS  (iTunes)");
                    th.getStyleClass().add("overlay-section-label");
                    th.setStyle("-fx-padding:12 0 4 0;");
                    content.getChildren().add(th);
                    for (ItunesService.Track t : tracks) {
                        HBox row = new HBox(10);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setStyle("-fx-padding:6 0 6 0;-fx-border-color:transparent transparent #E5E7EB transparent;-fx-border-width:0 0 1 0;");
                        Label trackName = new Label("♪  " + t.trackName());
                        trackName.getStyleClass().add("feed-post-content"); HBox.setHgrow(trackName, Priority.ALWAYS);
                        Label album = new Label(t.albumName().length() > 30 ? t.albumName().substring(0,30)+"…" : t.albumName());
                        album.getStyleClass().add("comment-date");
                        row.getChildren().addAll(trackName, album);
                        content.getChildren().add(row);
                    }
                }
            });
        }, "itunes-" + u.getId()).start();
    }

    // ── Artist cards — PartnershipRequest fallback ────────────────────

    private void renderRequestArtists(List<PartnershipRequest> list) {
        artistsGrid.getChildren().clear();
        artistCount.setText(list.size() + " artist" + (list.size() == 1 ? "" : "s"));
        for (int i = 0; i < list.size(); i++) {
            VBox card = buildRequestCard(list.get(i), i);
            artistsGrid.getChildren().add(card);
            animateIn(card, i * 55L);
        }
        if (list.isEmpty()) {
            Label e = new Label("No artists yet. Apply below to be featured!");
            e.getStyleClass().add("front-empty-label");
            artistsGrid.getChildren().add(e);
        }
    }

    private VBox buildRequestCard(PartnershipRequest ar, int idx) {
        String display = ar.getCompanyName() != null && !ar.getCompanyName().isBlank()
                ? ar.getCompanyName() : ar.getContactName();
        String color = PALETTE[Math.abs((display != null ? display : "").hashCode()) % PALETTE.length];

        VBox card = new VBox(0); card.getStyleClass().add("partner-card-grid");
        StackPane header = new StackPane();
        header.setStyle("-fx-background-color:" + color + "22;-fx-background-radius:16 16 0 0;-fx-pref-height:80;");
        Label ic = new Label("🎵"); ic.setStyle("-fx-font-size:30px;");
        Region bar = new Region(); bar.setPrefHeight(4); bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color:" + color + ";");
        header.getChildren().add(ic);
        card.getChildren().add(new VBox(0, header, bar));

        VBox body = new VBox(8); body.getStyleClass().add("partner-card-body");
        String st = ar.getStatus() != null ? ar.getStatus() : "pending";
        Label status = new Label(st.toUpperCase());
        status.getStyleClass().add("approved".equalsIgnoreCase(st) ? "badge-upcoming" : "badge-status");
        Label nameLbl = new Label(display != null ? display : "Artist"); nameLbl.getStyleClass().add("partner-card-name"); nameLbl.setWrapText(true);
        if (ar.getMessage() != null && !ar.getMessage().isBlank()) {
            Label msg = new Label(ar.getMessage().length() > 80 ? ar.getMessage().substring(0,80)+"…" : ar.getMessage());
            msg.getStyleClass().add("partner-card-meta"); msg.setWrapText(true);
            body.getChildren().add(msg);
        }
        Button aboutBtn = new Button("🔍 ABOUT"); aboutBtn.getStyleClass().add("card-action-btn");
        aboutBtn.setOnAction(e -> showRequestBio(ar));
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        body.getChildren().addAll(status, nameLbl, sp, aboutBtn);
        card.getChildren().add(body);
        hoverPop(card); return card;
    }

    private void showRequestBio(PartnershipRequest ar) {
        String name = ar.getCompanyName() != null && !ar.getCompanyName().isBlank()
                ? ar.getCompanyName() : ar.getContactName();
        VBox content = new VBox(12); content.getStyleClass().add("overlay-body");
        Label loading = new Label("🔍  Looking up \"" + name + "\"…");
        loading.getStyleClass().add("overlay-post-body"); loading.setWrapText(true);
        content.getChildren().add(loading);
        OverlayService.show("About — " + name, content);
        new Thread(() -> {
            String bio = WikipediaService.getSummary(name);
            List<ItunesService.Track> tracks = ItunesService.searchArtist(name, 4);
            Platform.runLater(() -> {
                content.getChildren().clear();
                Label t = new Label(name); t.getStyleClass().add("overlay-post-title"); t.setWrapText(true);
                Label b = new Label(bio);  b.getStyleClass().add("overlay-post-body"); b.setWrapText(true);
                content.getChildren().addAll(t, b);
                if (ar.getEmail() != null) {
                    Label e = new Label("📧 " + ar.getEmail()); e.getStyleClass().add("comment-date");
                    content.getChildren().add(e);
                }
                if (!tracks.isEmpty()) {
                    Label th = new Label("🎵  TOP TRACKS"); th.getStyleClass().add("overlay-section-label");
                    th.setStyle("-fx-padding:10 0 4 0;");
                    content.getChildren().add(th);
                    tracks.forEach(tr -> {
                        Label row = new Label("♪  " + tr.trackName() + " — " + tr.albumName());
                        row.getStyleClass().add("feed-post-content");
                        content.getChildren().add(row);
                    });
                }
            });
        }, "wiki-req").start();
    }

    // ── Media partner cards (Collaboration) ───────────────────────────

    private void renderMedia(List<Collaboration> list) {
        partnersGrid.getChildren().clear();
        partnerCount.setText(list.size() + " partner" + (list.size() == 1 ? "" : "s"));
        for (int i = 0; i < list.size(); i++) {
            VBox card = buildMediaCard(list.get(i));
            partnersGrid.getChildren().add(card);
            animateIn(card, i * 55L);
        }
        if (list.isEmpty()) {
            Label e = new Label("No media partners yet.");
            e.getStyleClass().add("front-empty-label");
            partnersGrid.getChildren().add(e);
        }
    }

    private VBox buildMediaCard(Collaboration col) {
        String color = PALETTE[Math.abs((col.getTitle() != null ? col.getTitle() : "").hashCode()) % PALETTE.length];
        VBox card = new VBox(0); card.getStyleClass().add("partner-card-grid");
        StackPane header = new StackPane();
        header.setStyle("-fx-background-color:" + color + "22;-fx-background-radius:16 16 0 0;-fx-pref-height:80;");
        Label ic = new Label("video".equalsIgnoreCase(col.getType()) ? "▶" : "🖼"); ic.setStyle("-fx-font-size:28px;");
        Region bar = new Region(); bar.setPrefHeight(4); bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color:" + color + ";");
        header.getChildren().add(ic);
        card.getChildren().add(new VBox(0, header, bar));

        VBox body = new VBox(8); body.getStyleClass().add("partner-card-body");
        Label typeBadge = new Label(col.getType() != null ? col.getType().toUpperCase() : "MEDIA");
        typeBadge.getStyleClass().add("video".equalsIgnoreCase(col.getType()) ? "card-genre-badge" : "card-genre-badge-acid");
        Label name = new Label(col.getTitle() != null ? col.getTitle() : "Partner");
        name.getStyleClass().add("partner-card-name"); name.setWrapText(true);
        String st = col.getStatus() != null ? col.getStatus() : "pending";
        Label status = new Label(st.toUpperCase());
        status.getStyleClass().add("approved".equalsIgnoreCase(st) ? "badge-upcoming" : "badge-status");
        Label views = new Label("👁  " + col.getViewCount() + " views"); views.getStyleClass().add("partner-card-views");

        HBox actions = new HBox(8);
        Button aboutBtn = new Button("🔍 ABOUT"); aboutBtn.getStyleClass().add("card-action-btn");
        aboutBtn.setOnAction(e -> showMediaBio(col));
        actions.getChildren().add(aboutBtn);
        if (col.getLinkUrl() != null && !col.getLinkUrl().isBlank()) {
            Button visit = new Button("🔗"); visit.getStyleClass().add("card-action-btn-outline");
            visit.setOnAction(e -> showVisit(col));
            actions.getChildren().add(visit);
        }
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        body.getChildren().addAll(typeBadge, name, status, views, sp, actions);
        card.getChildren().add(body);
        hoverPop(card); return card;
    }

    private void showMediaBio(Collaboration col) {
        col.setViewCount(col.getViewCount() + 1);
        try { mediaService.modifier(col); } catch (Exception ignored) {}
        VBox content = new VBox(12); content.getStyleClass().add("overlay-body");
        Label loading = new Label("🔍  Loading…"); loading.getStyleClass().add("overlay-post-body");
        content.getChildren().add(loading);
        OverlayService.show("About — " + col.getTitle(), content);
        new Thread(() -> {
            String s = WikipediaService.getSummary(col.getTitle());
            Platform.runLater(() -> {
                content.getChildren().clear();
                Label t = new Label(col.getTitle()); t.getStyleClass().add("overlay-post-title"); t.setWrapText(true);
                Label b = new Label(s); b.getStyleClass().add("overlay-post-body"); b.setWrapText(true);
                Label src = new Label("Wikipedia · " + col.getViewCount() + " views"); src.getStyleClass().add("comment-date");
                content.getChildren().addAll(t, b, src);
            });
        }, "wiki-media").start();
    }

    private void showVisit(Collaboration col) {
        VBox c = new VBox(12); c.getStyleClass().add("overlay-body");
        Label l = new Label(col.getLinkUrl()); l.getStyleClass().add("overlay-post-body"); l.setWrapText(true);
        Label h = new Label("Copy and open in your browser."); h.getStyleClass().add("comment-date");
        c.getChildren().addAll(l, h);
        OverlayService.show("Visit — " + col.getTitle(), c);
    }

    // ── Partnership application form ──────────────────────────────────

    @FXML void onApplyNow() {
        VBox form = new VBox(10); form.getStyleClass().add("overlay-body"); form.setPrefWidth(480);
        Label intro = new Label("Apply as an artist or media partner. We review within 48 hours.");
        intro.getStyleClass().add("card-event-desc"); intro.setWrapText(true);

        TextField nameField    = mkField("Your Name / Artist Name *");
        TextField companyField = mkField("Band / Label / Company");
        TextField emailField   = mkField("Contact Email *");
        TextField phoneField   = mkField("Phone Number");
        TextArea  msgField     = new TextArea();
        msgField.setPromptText("Describe your music, goals and how you'd like to collaborate…");
        msgField.setPrefRowCount(4); msgField.setWrapText(true); msgField.getStyleClass().add("overlay-textarea");

        Button submit = new Button("SUBMIT APPLICATION →");
        submit.getStyleClass().add("partner-cta-btn"); submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            String n = nameField.getText().trim(), em = emailField.getText().trim();
            if (n.isEmpty() || em.isEmpty()) { nameField.setStyle("-fx-border-color:#E8320A;"); return; }
            try {
                PartnershipRequest req = new PartnershipRequest(n, em,
                        companyField.getText().trim(), phoneField.getText().trim(), msgField.getText().trim());
                partnerService.ajouter(req);
                OverlayService.show("Application Sent!", successLabel("✅  Thank you, " + n + "!\n\nWe'll contact you at " + em + " within 48 hours."));
                loadData();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        ScrollPane scroll = new ScrollPane(new VBox(10, intro,
                lbl("NAME *"), nameField, lbl("BAND / COMPANY"), companyField,
                lbl("EMAIL *"), emailField, lbl("PHONE"), phoneField,
                lbl("MESSAGE"), msgField, submit));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
        scroll.setPrefHeight(480);
        OverlayService.show("Apply to the EVENTO Network", scroll);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private TextField mkField(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt);
        tf.getStyleClass().add("post-create-input"); return tf;
    }
    private Label lbl(String t) { Label l = new Label(t); l.getStyleClass().add("overlay-section-label"); return l; }
    private Label successLabel(String t) {
        Label l = new Label(t); l.getStyleClass().add("overlay-post-body");
        l.setWrapText(true); l.setStyle("-fx-padding:20 24 20 24;"); return l;
    }
    private void animateIn(VBox card, long delay) {
        card.setOpacity(0); card.setTranslateY(14);
        PauseTransition p = new PauseTransition(Duration.millis(delay));
        p.setOnFinished(ev -> {
            FadeTransition ft = new FadeTransition(Duration.millis(300), card); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
            tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
        });
        p.play();
    }
    private void hoverPop(VBox card) {
        ScaleTransition up = new ScaleTransition(Duration.millis(140), card); up.setToX(1.03); up.setToY(1.03);
        ScaleTransition dn = new ScaleTransition(Duration.millis(140), card); dn.setToX(1.0);  dn.setToY(1.0);
        card.setOnMouseEntered(e -> up.play()); card.setOnMouseExited(e -> dn.play());
    }
}
