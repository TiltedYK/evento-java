package controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Event;
import model.User;
import service.*;
import util.OverlayService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class FrontArtistHubController implements Initializable {

    @FXML private Label  statEvents;
    @FXML private Label  statAttendees;
    @FXML private Label  statUpcoming;
    @FXML private VBox   eventsBox;
    @FXML private VBox   profileBanner;
    @FXML private Label  artistNameLabel;
    @FXML private Label  artistRoleLabel;
    @FXML private StackPane avatarHolder;

    private final EventService       eventService       = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  •  HH:mm");
    private static final String[] PALETTE = {
        "#E8320A","#6366F1","#10B981","#F59E0B","#8B5CF6","#0EA5E9","#F43F5E","#14B8A6"
    };

    private User currentUser;

    @Override public void initialize(URL url, ResourceBundle rb) {}

    public void setCurrentUser(User user) {
        this.currentUser = user;
        buildProfileBanner();
        loadData();
    }

    // ── Profile banner ────────────────────────────────────────────────

    private void buildProfileBanner() {
        if (currentUser == null || profileBanner == null) return;
        String name = currentUser.getPrenom() + " " + currentUser.getNom();
        if (artistNameLabel != null) artistNameLabel.setText(name);
        if (artistRoleLabel != null) artistRoleLabel.setText("🎤  VERIFIED ARTIST");

        if (avatarHolder != null) {
            String color = PALETTE[Math.abs(name.hashCode()) % PALETTE.length].replace("#", "");

            // Circular avatar background
            Region bg = new Region();
            bg.setPrefSize(96, 96); bg.setMaxSize(96, 96);
            bg.setStyle("-fx-background-color:#" + color + "44;-fx-background-radius:48;-fx-border-color:#" + color + ";-fx-border-radius:48;-fx-border-width:2;");

            Label fallback = new Label("🎵");
            fallback.setStyle("-fx-font-size:36px;");

            ImageView av = new ImageView();
            av.setFitWidth(96); av.setFitHeight(96);
            Circle clip = new Circle(48, 48, 48);
            av.setClip(clip);

            avatarHolder.getChildren().setAll(bg, fallback, av);

            // Load artist photo async
            new Thread(() -> {
                String photoUrl = ArtistPhotoService.resolveArtistImageUrl(name);
                if (photoUrl == null || photoUrl.isBlank())
                    photoUrl = PlaceholderImageService.userAvatar(name, color);
                final String url = photoUrl;
                Platform.runLater(() -> {
                    PlaceholderImageService.loadInto(av, url, null);
                    av.imageProperty().addListener((o, old, n) -> {
                        if (n != null && !n.isError()) fallback.setVisible(false);
                    });
                });
            }, "artist-hub-photo").start();
        }
    }

    // ── Data loading ──────────────────────────────────────────────────

    private void loadData() {
        new Thread(() -> {
            try {
                List<Event> myEvents = currentUser != null
                        ? eventService.getByCreatedBy(currentUser.getId())
                        : List.of();

                int totalAttendees = 0;
                long upcoming = 0;
                for (Event e : myEvents) {
                    try { totalAttendees += reservationService.countByEventId(e.getId()); } catch (Exception ignored) {}
                    if (e.getDateHeure() != null && e.getDateHeure().isAfter(LocalDateTime.now())) upcoming++;
                }

                final int att = totalAttendees;
                final long up = upcoming;
                Platform.runLater(() -> {
                    if (statEvents    != null) statEvents.setText(String.valueOf(myEvents.size()));
                    if (statAttendees != null) statAttendees.setText(String.valueOf(att));
                    if (statUpcoming  != null) statUpcoming.setText(String.valueOf(up));
                    buildList(myEvents);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }, "artist-hub-load").start();
    }

    // ── Event list ────────────────────────────────────────────────────

    private void buildList(List<Event> events) {
        if (eventsBox == null) return;
        eventsBox.getChildren().clear();
        if (events.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding:40;");
            Label icon = new Label("🎤");
            icon.setStyle("-fx-font-size:48px;-fx-opacity:0.4;");
            Label msg = new Label("No events yet — create your first show!");
            msg.getStyleClass().add("front-empty-label");
            msg.setStyle("-fx-padding:0;");
            empty.getChildren().addAll(icon, msg);
            eventsBox.getChildren().add(empty);
            return;
        }

        // Split into upcoming vs past
        LocalDateTime now = LocalDateTime.now();
        List<Event> upcoming = events.stream()
                .filter(e -> e.getDateHeure() == null || e.getDateHeure().isAfter(now))
                .toList();
        List<Event> past = events.stream()
                .filter(e -> e.getDateHeure() != null && !e.getDateHeure().isAfter(now))
                .toList();

        if (!upcoming.isEmpty()) {
            Label section = sectionLabel("📅  UPCOMING SHOWS");
            eventsBox.getChildren().add(section);
            for (Event ev : upcoming) {
                VBox card = buildCard(ev, true);
                eventsBox.getChildren().add(card);
                animateIn(card);
            }
        }
        if (!past.isEmpty()) {
            Label section = sectionLabel("🕰  PAST SHOWS");
            eventsBox.getChildren().add(section);
            for (Event ev : past) {
                VBox card = buildCard(ev, false);
                eventsBox.getChildren().add(card);
            }
        }
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("sidebar-section-title");
        l.setStyle("-fx-padding:16 0 8 0;-fx-font-size:11px;");
        return l;
    }

    private void animateIn(VBox card) {
        card.setOpacity(0);
        card.setTranslateY(12);
        FadeTransition ft = new FadeTransition(Duration.millis(320), card);
        ft.setToValue(1);
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(320), card);
        tt.setToY(0);
        tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(ft, tt).play();
    }

    private VBox buildCard(Event ev, boolean isUpcoming) {
        String color = PALETTE[Math.abs((ev.getTitre() != null ? ev.getTitre() : "").hashCode()) % PALETTE.length];

        VBox card = new VBox(0);
        card.setStyle("-fx-background-radius:14;-fx-border-radius:14;-fx-border-width:1;"
                + "-fx-border-color:" + color + "44;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),16,0,0,4);");
        card.setOpacity(isUpcoming ? 1.0 : 0.75);

        // ── Card header (colored gradient + event image) ──
        StackPane header = new StackPane();
        header.setMinHeight(120); header.setPrefHeight(120); header.setMaxHeight(120);
        header.setStyle("-fx-background-color:linear-gradient(to right," + color + "55," + color + "11);"
                + "-fx-background-radius:14 14 0 0;");

        // Event image (if uploaded)
        if (ev.getImageFilename() != null && !ev.getImageFilename().isBlank()) {
            java.io.File imgFile = new java.io.File("uploads", ev.getImageFilename());
            if (imgFile.exists()) {
                ImageView iv = new ImageView(new Image(imgFile.toURI().toString(), 0, 120, true, true, true));
                iv.setFitHeight(120);
                iv.setPreserveRatio(false);
                Rectangle imgClip = new Rectangle();
                imgClip.widthProperty().bind(header.widthProperty());
                imgClip.setHeight(120);
                imgClip.setArcWidth(28); imgClip.setArcHeight(28);
                iv.setClip(imgClip);
                iv.fitWidthProperty().bind(header.widthProperty());
                header.getChildren().add(0, iv);
            }
        }

        // Genre badge + status pill overlay
        HBox overlayRow = new HBox(8);
        overlayRow.setAlignment(Pos.BOTTOM_LEFT);
        overlayRow.setStyle("-fx-padding:10 14 10 14;");
        StackPane.setAlignment(overlayRow, Pos.BOTTOM_LEFT);
        if (ev.getGenre() != null && !ev.getGenre().isBlank()) {
            Label genre = new Label(ev.getGenre().toUpperCase());
            genre.getStyleClass().add("card-genre-badge");
            genre.setStyle("-fx-background-color:" + color + ";-fx-text-fill:#FFFFFF;-fx-border-color:transparent;");
            overlayRow.getChildren().add(genre);
        }
        String st = ev.getStatut() != null ? ev.getStatut() : "draft";
        Label statusBadge = new Label(st.toUpperCase());
        statusBadge.getStyleClass().add("published".equalsIgnoreCase(st) ? "badge-upcoming" : "badge-status");
        overlayRow.getChildren().add(statusBadge);
        header.getChildren().add(overlayRow);

        // Dim overlay for past events
        if (!isUpcoming) {
            Region dim = new Region();
            dim.setStyle("-fx-background-color:rgba(0,0,0,0.42);-fx-background-radius:14 14 0 0;");
            header.getChildren().add(dim);
            Label pastLabel = new Label("PAST EVENT");
            pastLabel.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.6);-fx-letter-spacing:2px;");
            header.getChildren().add(pastLabel);
        }

        // ── Card body ──
        VBox body = new VBox(8);
        body.setStyle("-fx-padding:16 18 16 18;-fx-background-color:rgba(13,15,22,0.9);-fx-background-radius:0 0 14 14;");

        Label titleLbl = new Label(ev.getTitre() != null ? ev.getTitre() : "Untitled Event");
        titleLbl.getStyleClass().add("event-list-title");
        titleLbl.setStyle("-fx-font-size:16px;");
        titleLbl.setWrapText(true);

        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        if (ev.getDateHeure() != null) {
            Label dateLbl = new Label("📅 " + ev.getDateHeure().format(FMT));
            dateLbl.getStyleClass().add("event-list-meta");
            metaRow.getChildren().add(dateLbl);
        }
        if (ev.getVenue() != null && !ev.getVenue().isBlank()) {
            Label venueLbl = new Label("📍 " + ev.getVenue());
            venueLbl.getStyleClass().add("event-list-meta");
            metaRow.getChildren().add(venueLbl);
        }

        // Attendance bar
        Label attLbl = new Label("🎟 loading…");
        attLbl.getStyleClass().add("event-list-meta");
        attLbl.setStyle("-fx-text-fill:#A5B4FC;");

        HBox capBarOuter = new HBox();
        capBarOuter.setStyle("-fx-background-color:rgba(255,255,255,0.08);-fx-background-radius:4;-fx-pref-height:6;-fx-max-height:6;");
        Region capFill = new Region();
        capFill.setStyle("-fx-background-color:" + color + ";-fx-background-radius:4;");
        HBox.setHgrow(capFill, Priority.NEVER);
        capBarOuter.getChildren().add(capFill);

        new Thread(() -> {
            try {
                int count = reservationService.countByEventId(ev.getId());
                double pct = ev.getCapacite() > 0 ? Math.min(1.0, (double) count / ev.getCapacite()) : 0;
                Platform.runLater(() -> {
                    attLbl.setText("🎟 " + count + " / " + ev.getCapacite() + " attending");
                    capFill.setPrefWidth(capBarOuter.getWidth() * pct);
                    capBarOuter.widthProperty().addListener((o, old, w) ->
                            capFill.setPrefWidth(w.doubleValue() * pct));
                });
            } catch (Exception ignored) {}
        }, "att-" + ev.getId()).start();

        // Weather badge (if location/venue available)
        Label weatherLbl = new Label("🌤 —");
        weatherLbl.getStyleClass().add("event-weather-loading");
        if (ev.getVenue() != null && !ev.getVenue().isBlank() && ev.getDateHeure() != null
                && ev.getDateHeure().isAfter(LocalDateTime.now())) {
            loadWeatherForCard(ev, weatherLbl);
        } else {
            weatherLbl.setText("");
        }

        // Action row
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = new Button("✏ Edit");
        editBtn.getStyleClass().addAll("btn-secondary-small");
        editBtn.setOnAction(e -> onEditEvent(ev));
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().addAll("btn-secondary-small");
        deleteBtn.setOnAction(e -> onDeleteEvent(ev));
        actions.getChildren().addAll(editBtn, deleteBtn);

        body.getChildren().addAll(titleLbl, metaRow, attLbl, capBarOuter, weatherLbl, actions);
        card.getChildren().addAll(header, body);
        return card;
    }

    private void loadWeatherForCard(Event ev, Label lbl) {
        new Thread(() -> {
            try {
                String venue = ev.getVenue() != null ? ev.getVenue() : "Tunis";
                OpenMeteoService.Forecast f = OpenMeteoService.getForecast(venue, ev.getDateHeure().toLocalDate());
                Platform.runLater(() -> {
                    if (f != null) lbl.setText(f.emoji() + "  " + f.condition()
                            + "  " + String.format("%.0f–%.0f°C", f.minTemp(), f.maxTemp()));
                    else lbl.setText("");
                });
            } catch (Exception ignored) { Platform.runLater(() -> lbl.setText("")); }
        }, "weather-artist-" + ev.getId()).start();
    }

    private void onEditEvent(Event ev) {
        VBox form = new VBox(12);
        form.getStyleClass().add("overlay-body");
        form.setPrefWidth(460);

        TextField fTitle  = prefilled(ev.getTitre());
        TextField fVenue  = prefilled(ev.getVenue());
        TextField fDate   = prefilled(ev.getDateHeure() != null ? ev.getDateHeure().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        TextField fCap    = prefilled(String.valueOf(ev.getCapacite()));
        TextField fGenre  = prefilled(ev.getGenre());
        TextArea  fDesc   = new TextArea(ev.getDescription() != null ? ev.getDescription() : "");
        fDesc.setPromptText("Description…"); fDesc.setPrefRowCount(3); fDesc.setWrapText(true);
        fDesc.getStyleClass().add("detail-value-text");
        Label errLbl = new Label();
        errLbl.getStyleClass().add("login-error");

        Button save = new Button("SAVE CHANGES →");
        save.getStyleClass().add("btn-primary"); save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> {
            try {
                ev.setTitre(fTitle.getText().trim());
                ev.setVenue(fVenue.getText().trim());
                ev.setGenre(fGenre.getText().trim());
                ev.setDescription(fDesc.getText().trim());
                if (!fCap.getText().trim().isEmpty()) ev.setCapacite(Integer.parseInt(fCap.getText().trim()));
                if (!fDate.getText().trim().isEmpty()) ev.setDateHeure(LocalDateTime.parse(fDate.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                eventService.modifier(ev);
                OverlayService.hide();
                loadData();
            } catch (Exception ex) { errLbl.setText("Error: " + ex.getMessage()); }
        });

        form.getChildren().addAll(
            label("Title"), fTitle, label("Venue"), fVenue,
            label("Date (YYYY-MM-DD HH:mm)"), fDate, label("Capacity"), fCap,
            label("Genre"), fGenre, label("Description"), fDesc, errLbl, save
        );
        OverlayService.show("Edit Event", form);
    }

    private void onDeleteEvent(Event ev) {
        try {
            eventService.supprimer(ev.getId());
            loadData();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Create event form ─────────────────────────────────────────────

    @FXML void onCreateEvent() {
        VBox form = new VBox(12);
        form.getStyleClass().add("overlay-body");
        form.setPrefWidth(460);

        TextField fTitle = field("Event Title *");
        TextField fVenue = field("Venue");
        TextField fDate  = field("Date (YYYY-MM-DD HH:mm) *");
        TextField fCap   = field("Capacity *");
        TextField fGenre = field("Genre");
        TextArea  fDesc  = new TextArea();
        fDesc.setPromptText("Description…"); fDesc.setPrefRowCount(3); fDesc.setWrapText(true);
        fDesc.getStyleClass().add("detail-value-text");
        Label errLbl = new Label();
        errLbl.getStyleClass().add("login-error");

        Button submit = new Button("CREATE EVENT →");
        submit.getStyleClass().add("btn-primary"); submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            String title = fTitle.getText().trim();
            String dateStr = fDate.getText().trim();
            String capStr  = fCap.getText().trim();
            if (title.isEmpty() || dateStr.isEmpty() || capStr.isEmpty()) {
                errLbl.setText("Title, date and capacity are required."); return;
            }
            LocalDateTime dt;
            int cap;
            try { dt  = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); }
            catch (Exception ex) { errLbl.setText("Date format: YYYY-MM-DD HH:mm"); return; }
            try { cap = Integer.parseInt(capStr); }
            catch (Exception ex) { errLbl.setText("Capacity must be a number."); return; }

            Event ev = new Event();
            ev.setTitre(title); ev.setDateHeure(dt); ev.setCapacite(cap);
            ev.setVenue(fVenue.getText().trim()); ev.setGenre(fGenre.getText().trim());
            ev.setDescription(fDesc.getText().trim()); ev.setStatut("draft");
            if (currentUser != null) ev.setCreatedById(currentUser.getId());
            try { eventService.ajouter(ev); OverlayService.hide(); loadData(); }
            catch (SQLException ex) { errLbl.setText("Save failed: " + ex.getMessage()); }
        });

        form.getChildren().addAll(
            label("Event Title"), fTitle, label("Venue"), fVenue,
            label("Date (YYYY-MM-DD HH:mm)"), fDate, label("Capacity"), fCap,
            label("Genre"), fGenre, label("Description"), fDesc, errLbl, submit
        );
        OverlayService.show("Create New Event", form);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("detail-value-text");
        return tf;
    }

    private TextField prefilled(String value) {
        TextField tf = field("");
        tf.setText(value != null ? value : "");
        return tf;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }
}
