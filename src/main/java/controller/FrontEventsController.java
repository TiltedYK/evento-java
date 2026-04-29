package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import model.Event;
import model.Reservation;
import model.User;
import service.EventService;
import service.OpenMeteoService;
import service.ReservationService;
import util.OverlayService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EVENTS module — vertical concert-listing view.
 *
 * APIs:
 *   • OpenMeteoService → Open-Meteo (live weather forecast for event day, prominently displayed)
 *
 * Advanced features:
 *   1. Live weather badge per event (geocodes venue, fetches daily forecast)
 *   2. Smart genre-based recommendations at the bottom of filtered results
 */
public class FrontEventsController implements Initializable {

    @FXML private VBox              eventsList;
    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  genreFilter;
    @FXML private ComboBox<String>  statusFilter;
    @FXML private ComboBox<String>  sortCombo;
    @FXML private Label             countLabel;

    private final EventService       eventService       = new EventService();
    private final ReservationService reservationService = new ReservationService();

    private List<Event> allEvents;
    private User        currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        genreFilter.getItems().addAll("All Genres", "Rock", "Metal", "Punk",
                "Electronic", "Hip-Hop", "Jazz", "Classical", "Pop", "Indie");
        genreFilter.setValue("All Genres");

        statusFilter.getItems().addAll("All", "published", "draft", "cancelled", "completed");
        statusFilter.setValue("All");

        sortCombo.getItems().addAll("Date ↑", "Date ↓", "Title A–Z", "Capacity ↑");
        sortCombo.setValue("Date ↑");

        loadEvents();
    }

    public void setCurrentUser(User user) { this.currentUser = user; }

    private void loadEvents() {
        try {
            allEvents = eventService.recuperer();
            renderEvents(allEvents);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML void filterEvents() {
        if (allEvents == null) return;
        String q    = searchField.getText().toLowerCase().trim();
        String genre  = genreFilter.getValue();
        String status = statusFilter.getValue();
        String sort   = sortCombo.getValue();

        List<Event> filtered = allEvents.stream()
                .filter(e -> q.isEmpty()
                        || (e.getTitre()       != null && e.getTitre().toLowerCase().contains(q))
                        || (e.getVenue()       != null && e.getVenue().toLowerCase().contains(q))
                        || (e.getDescription() != null && e.getDescription().toLowerCase().contains(q)))
                .filter(e -> "All Genres".equals(genre) || genre == null
                        || (e.getGenre() != null && e.getGenre().equalsIgnoreCase(genre)))
                .filter(e -> "All".equals(status) || status == null
                        || (e.getStatut() != null && e.getStatut().equalsIgnoreCase(status)))
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Date ↑"    -> filtered.sort(Comparator.comparing(Event::getDateHeure, Comparator.nullsLast(Comparator.naturalOrder())));
            case "Date ↓"    -> filtered.sort(Comparator.comparing(Event::getDateHeure, Comparator.nullsLast(Comparator.reverseOrder())));
            case "Title A–Z" -> filtered.sort(Comparator.comparing(e -> e.getTitre() != null ? e.getTitre().toLowerCase() : ""));
            case "Capacity ↑"-> filtered.sort(Comparator.comparingInt(Event::getCapacite));
        }
        renderEvents(filtered);
    }

    // ── Render ────────────────────────────────────────────────────────

    private void renderEvents(List<Event> events) {
        eventsList.getChildren().clear();
        countLabel.setText(events.size() + " show" + (events.size() == 1 ? "" : "s"));

        if (events.isEmpty()) {
            Label empty = new Label("🎸  No shows found. Try a different search.");
            empty.getStyleClass().add("front-empty-label");
            eventsList.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            VBox row = buildEventRow(events.get(i));
            eventsList.getChildren().add(row);
            row.setOpacity(0);
            row.setTranslateX(-20);
            final int delay = i * 60;
            PauseTransition p = new PauseTransition(Duration.millis(delay));
            p.setOnFinished(ev -> {
                FadeTransition ft = new FadeTransition(Duration.millis(300), row); ft.setToValue(1);
                TranslateTransition tt = new TranslateTransition(Duration.millis(300), row);
                tt.setToX(0); tt.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(ft, tt).play();
            });
            p.play();
        }

        // Genre recommendations at bottom
        addRecommendations(events);
    }

    private VBox buildEventRow(Event event) {
        VBox row = new VBox(0);
        row.getStyleClass().add("event-list-row");

        HBox inner = new HBox(20);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.getStyleClass().add("event-list-inner");

        // Left: date block
        VBox dateBlock = new VBox(2);
        dateBlock.setAlignment(Pos.CENTER);
        dateBlock.getStyleClass().add("event-date-block");
        if (event.getDateHeure() != null) {
            Label mon = new Label(event.getDateHeure().format(DateTimeFormatter.ofPattern("MMM")).toUpperCase());
            mon.getStyleClass().add("event-date-month");
            Label day = new Label(event.getDateHeure().format(DateTimeFormatter.ofPattern("dd")));
            day.getStyleClass().add("event-date-day");
            Label time = new Label(event.getDateHeure().format(DateTimeFormatter.ofPattern("HH:mm")));
            time.getStyleClass().add("event-date-time");
            dateBlock.getChildren().addAll(mon, day, time);
        } else {
            Label tba = new Label("TBA"); tba.getStyleClass().add("event-date-day");
            dateBlock.getChildren().add(tba);
        }

        // Centre: event info
        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox badges = new HBox(8);
        if (event.getGenre() != null) {
            Label g = new Label(event.getGenre().toUpperCase()); g.getStyleClass().add("card-genre-badge"); badges.getChildren().add(g);
        }
        String statut = event.getStatut() != null ? event.getStatut() : "draft";
        Label sl = new Label(statut.toUpperCase());
        sl.getStyleClass().add("published".equalsIgnoreCase(statut) ? "badge-upcoming" : "badge-status");
        badges.getChildren().add(sl);

        Label title = new Label(event.getTitre() != null ? event.getTitre() : "Untitled");
        title.getStyleClass().add("event-list-title");
        title.setWrapText(true);

        HBox meta = new HBox(20);
        if (event.getVenue() != null) {
            Label v = new Label("📍 " + event.getVenue()); v.getStyleClass().add("event-list-meta"); meta.getChildren().add(v);
        }
        Label cap = new Label("🎟 " + event.getCapacite() + " seats");
        cap.getStyleClass().add("event-list-meta");
        meta.getChildren().add(cap);

        // Capacity bar
        int reserved = 0;
        try { reserved = reservationService.countByEventId(event.getId()); } catch (Exception ignored) {}
        HBox capBar = buildCapBar(reserved, event.getCapacite());

        // Weather badge (loaded async — VISIBLE API)
        HBox weatherBox = new HBox(8);
        weatherBox.setAlignment(Pos.CENTER_LEFT);
        Label weatherLbl = new Label("🌤  Loading weather forecast…");
        weatherLbl.getStyleClass().add("event-weather-loading");
        weatherBox.getChildren().add(weatherLbl);
        loadWeatherAsync(event, weatherLbl);

        info.getChildren().addAll(badges, title, meta, capBar, weatherBox);

        // Right: reserve button
        VBox right = new VBox(8);
        right.setAlignment(Pos.CENTER);
        right.setMinWidth(150);

        boolean past = event.getDateHeure() != null && event.getDateHeure().isBefore(LocalDateTime.now());
        boolean cancelled = "cancelled".equalsIgnoreCase(statut);
        Button btn = new Button(past || cancelled ? "UNAVAILABLE" : "⚡  RESERVE");
        btn.getStyleClass().add(past || cancelled ? "event-btn-unavailable" : "event-btn-reserve");
        btn.setDisable(past || cancelled);
        if (!past && !cancelled) btn.setOnAction(e -> handleReserve(event, btn));

        right.getChildren().add(btn);
        inner.getChildren().addAll(dateBlock, info, right);
        row.getChildren().add(inner);
        return row;
    }

    // ── Weather API (prominently shown) ───────────────────────────────

    private void loadWeatherAsync(Event event, Label weatherLbl) {
        if (event.getDateHeure() == null) {
            weatherLbl.setText("📅  Date TBA — no forecast available");
            weatherLbl.getStyleClass().setAll("event-weather-na");
            return;
        }
        String city = event.getVenue() != null ? event.getVenue()
                : (event.getLocation() != null ? event.getLocation() : "Tunis");
        java.time.LocalDate date = event.getDateHeure().toLocalDate();

        new Thread(() -> {
            OpenMeteoService.Forecast f = OpenMeteoService.getForecast(city, date);
            Platform.runLater(() -> {
                weatherLbl.setText(f.emoji() + "  " + f.condition()
                        + "  " + String.format("%.0f°C / %.0f°C", f.maxTemp(), f.minTemp())
                        + "   [" + city + " · " + date + "]");
                weatherLbl.getStyleClass().setAll("event-weather-badge");
            });
        }, "weather-" + event.getId()).start();
    }

    // ── Capacity bar ──────────────────────────────────────────────────

    private HBox buildCapBar(int reserved, int capacity) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        int cap = Math.max(capacity, 1);
        double pct = Math.min((double) reserved / cap, 1.0);
        int left = Math.max(cap - reserved, 0);
        Label lbl = new Label(reserved + "/" + cap + " · " + left + " left");
        lbl.getStyleClass().add("event-cap-text");

        Region track = new Region(); track.setPrefHeight(5); track.setStyle("-fx-background-color:#E5E7EB;-fx-background-radius:3;"); track.setPrefWidth(140);
        Region fill  = new Region(); fill.setPrefHeight(5);
        String col = pct < 0.6 ? "#22C55E" : pct < 0.85 ? "#F59E0B" : "#EF4444";
        fill.setStyle("-fx-background-color:" + col + ";-fx-background-radius:3;");
        fill.setPrefWidth(140 * pct);
        StackPane bar = new StackPane(track, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        box.getChildren().addAll(bar, lbl);
        return box;
    }

    // ── Recommendations ───────────────────────────────────────────────

    private void addRecommendations(List<Event> shown) {
        if (allEvents == null || allEvents.size() <= shown.size()) return;
        Optional<String> topGenre = shown.stream()
                .filter(e -> e.getGenre() != null)
                .collect(Collectors.groupingBy(Event::getGenre, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
        if (topGenre.isEmpty()) return;

        List<Event> recs = allEvents.stream()
                .filter(e -> topGenre.get().equalsIgnoreCase(e.getGenre()))
                .filter(e -> shown.stream().noneMatch(s -> s.getId() == e.getId()))
                .limit(3).collect(Collectors.toList());
        if (recs.isEmpty()) return;

        Label h = new Label("◈  MORE " + topGenre.get().toUpperCase() + " SHOWS YOU MAY LIKE");
        h.getStyleClass().add("recommendation-header");
        h.setMaxWidth(Double.MAX_VALUE);
        eventsList.getChildren().add(h);
        recs.forEach(e -> eventsList.getChildren().add(buildEventRow(e)));
    }

    // ── Reservation ───────────────────────────────────────────────────

    private void handleReserve(Event event, Button btn) {
        if (currentUser == null) {
            OverlayService.show("Login Required", buildLoginPrompt());
            return;
        }
        try {
            Reservation res = new Reservation();
            res.setEventId(event.getId());
            res.setUserId(currentUser.getId());
            res.setNombrePlaces(1);
            res.setStatut("pending");
            res.setCreatedAt(LocalDateTime.now());
            reservationService.ajouter(res);
            btn.setText("✅  RESERVED!");
            btn.setDisable(true);
            btn.getStyleClass().setAll("event-btn-done");
            ScaleTransition pulse = new ScaleTransition(Duration.millis(120), btn);
            pulse.setToX(1.08); pulse.setToY(1.08); pulse.setCycleCount(2); pulse.setAutoReverse(true);
            pulse.play();
        } catch (SQLException e) {
            OverlayService.show("Error", buildErrorLabel(e.getMessage()));
        }
    }

    private Label buildLoginPrompt() {
        Label l = new Label("Please log in to reserve a ticket.");
        l.getStyleClass().add("overlay-post-body");
        l.setStyle("-fx-padding:20 24 20 24;");
        return l;
    }

    private Label buildErrorLabel(String msg) {
        Label l = new Label("Could not complete reservation:\n" + msg);
        l.getStyleClass().add("overlay-post-body");
        l.setStyle("-fx-padding:20 24 20 24;");
        return l;
    }
}
