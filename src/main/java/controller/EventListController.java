package controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.converter.DefaultStringConverter;
import util.OverlayService;
import model.Event;
import service.EventService;
import service.EventStatsService;
import service.ICalService;
import service.QRCodeService;
import util.Router;

import java.io.File;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

public class EventListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> colTitre;
    @FXML private TableColumn<Event, String> colDate;
    @FXML private TableColumn<Event, String> colVenue;
    @FXML private TableColumn<Event, String> colGenre;
    @FXML private TableColumn<Event, String> colStatut;
    @FXML private TableColumn<Event, Void>   colActions;

    @FXML private Label statTotal;
    @FXML private Label statUpcoming;
    @FXML private Label statPublished;
    @FXML private Label statAvgCapacity;

    private final EventService      service      = new EventService();
    private final EventStatsService statsService = new EventStatsService();
    private final ICalService       icalService  = new ICalService();
    private final QRCodeService     qrService    = new QRCodeService();

    private final ObservableList<Event> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Event pendingEdit;

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("All", "draft", "published", "cancelled", "completed");
        statusFilter.setValue("All");

        sortCombo.getItems().addAll("Date ↑", "Date ↓", "Title A–Z", "Capacity ↑", "Capacity ↓");
        sortCombo.setValue("Date ↑");

        eventsTable.setEditable(true);

        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateHeure() != null ? c.getValue().getDateHeure().format(DATE_FMT) : "—"));
        colVenue.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getVenue() != null ? c.getValue().getVenue() : ""));
        colGenre.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getGenre() != null ? c.getValue().getGenre() : ""));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        colTitre.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colTitre.setOnEditCommit(e -> {
            Event ev = e.getRowValue(); ev.setTitre(e.getNewValue());
            try { service.modifier(ev); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });
        colVenue.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colVenue.setOnEditCommit(e -> {
            Event ev = e.getRowValue(); ev.setVenue(e.getNewValue());
            try { service.modifier(ev); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });
        colGenre.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colGenre.setOnEditCommit(e -> {
            Event ev = e.getRowValue(); ev.setGenre(e.getNewValue());
            try { service.modifier(ev); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
        });

        setupStatusPillColumn();
        setupActionsColumn();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        sortCombo.valueProperty().addListener((o, a, b) -> applyFilters());

        eventsTable.setItems(data);
        refresh();
        loadStatsAsync();
    }

    // ── Stats ─────────────────────────────────────────────────────────────

    private void loadStatsAsync() {
        new Thread(() -> {
            try {
                long total     = statsService.totalCount();
                long upcoming  = statsService.upcomingCount();
                long published = statsService.countByStatut("published");
                double avgCap  = statsService.averageCapacity();
                Platform.runLater(() -> {
                    statTotal.setText(String.valueOf(total));
                    statUpcoming.setText(String.valueOf(upcoming));
                    statPublished.setText(String.valueOf(published));
                    statAvgCapacity.setText(String.format("%.0f", avgCap));
                });
            } catch (Exception ignored) {}
        }, "stats-loader").start();
    }

    // ── Status pill ───────────────────────────────────────────────────────

    private void setupStatusPillColumn() {
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label pill = new Label(item);
                pill.getStyleClass().add("status-pill");
                switch (item.toLowerCase()) {
                    case "published", "confirmed" -> pill.getStyleClass().add("status-confirmed");
                    case "cancelled"              -> pill.getStyleClass().add("status-cancelled");
                    case "draft"                  -> pill.getStyleClass().add("status-draft");
                    default                       -> pill.getStyleClass().add("status-pending");
                }
                setGraphic(pill); setText(null);
            }
        });
    }

    // ── Actions column: Clone | QR | Delete ───────────────────────────────

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnClone  = new Button("Clone");
            private final Button btnQR     = new Button("QR");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(4, btnClone, btnQR, btnDelete);

            {
                btnClone .getStyleClass().addAll("button", "btn-warning",   "btn-action");
                btnQR    .getStyleClass().addAll("button", "btn-info",      "btn-action");
                btnDelete.getStyleClass().addAll("button", "btn-danger",    "btn-action");

                btnClone.setOnAction(e -> onClone(getTableView().getItems().get(getIndex())));
                btnQR.setOnAction(e -> onQR(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ── Data ─────────────────────────────────────────────────────────────

    private void refresh() {
        try {
            data.setAll(service.recuperer());
            applyFilters();
            loadStatsAsync();
        } catch (Exception e) { error("Failed to load events: " + e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<Event> all = service.recuperer();
            String q  = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String st = statusFilter.getValue();

            List<Event> filtered = all.stream()
                    .filter(ev -> q.isEmpty()
                            || (ev.getTitre()  != null && ev.getTitre().toLowerCase().contains(q))
                            || (ev.getVenue()  != null && ev.getVenue().toLowerCase().contains(q))
                            || (ev.getGenre()  != null && ev.getGenre().toLowerCase().contains(q)))
                    .filter(ev -> st == null || st.equals("All")
                            || (ev.getStatut() != null && ev.getStatut().equalsIgnoreCase(st)))
                    .collect(Collectors.toList());

            // Sorting
            String sort = sortCombo.getValue();
            if (sort != null) switch (sort) {
                case "Date ↑"     -> filtered.sort(Comparator.comparing(Event::getDateHeure, Comparator.nullsLast(Comparator.naturalOrder())));
                case "Date ↓"     -> filtered.sort(Comparator.comparing(Event::getDateHeure, Comparator.nullsLast(Comparator.reverseOrder())));
                case "Title A–Z"  -> filtered.sort(Comparator.comparing(e -> e.getTitre() != null ? e.getTitre().toLowerCase() : ""));
                case "Capacity ↑" -> filtered.sort(Comparator.comparingInt(Event::getCapacite));
                case "Capacity ↓" -> filtered.sort(Comparator.comparingInt(Event::getCapacite).reversed());
            }

            data.setAll(filtered);
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    // ── Action handlers ───────────────────────────────────────────────────

    @FXML public void onReset() {
        searchField.clear();
        statusFilter.setValue("All");
        sortCombo.setValue("Date ↑");
    }

    @FXML public void onAdd() {
        pendingEdit = null;
        Router.navigate("/fxml/EventForm.fxml");
    }

    private void onClone(Event ev) {
        try {
            service.dupliquer(ev.getId());
            refresh();
        } catch (Exception e) { error("Clone failed: " + e.getMessage()); }
    }

    private void onQR(Event ev) {
        String qrContent = "EVENT: " + ev.getTitre()
                + "\nDATE: " + (ev.getDateHeure() != null ? ev.getDateHeure().format(DATE_FMT) : "TBA")
                + "\nVENUE: " + (ev.getVenue() != null ? ev.getVenue() : "TBA")
                + "\nCAPACITY: " + ev.getCapacite()
                + "\nSTATUS: " + ev.getStatut();
        try {
            OverlayService.init(Router.getContentArea());
            javafx.scene.image.Image qr = qrService.generateQRCode(qrContent, 260);
            ImageView iv = new ImageView(qr);
            iv.setFitWidth(260); iv.setFitHeight(260);

            Label hint = new Label("Scan to view event details");
            hint.setStyle("-fx-font-size:11px;-fx-text-fill:#7880A0;");

            VBox box = new VBox(14, iv, hint);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-padding:16 24 20 24;");
            OverlayService.show("QR Code — " + ev.getTitre(), box);
        } catch (Exception e) { error("QR generation failed: " + e.getMessage()); }
    }

    private void onDelete(Event ev) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete event \"" + ev.getTitre() + "\"?");
        a.setContentText("This action cannot be undone.");
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(ev.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    // ── Export ────────────────────────────────────────────────────────────

    @FXML public void onShowStats() {
        try {
            long   total    = statsService.totalCount();
            long   upcoming = statsService.upcomingCount();
            double avgCap   = statsService.averageCapacity();
            int    totalCap = statsService.totalCapacitySum();
            Map<String, Long> byStatus = statsService.countByStatus();
            Map<Month, Long>  byMonth  = statsService.eventsPerMonth();
            Map<String, Long> genres   = statsService.topGenres(6);

            VBox root = new VBox(20);
            root.setStyle("-fx-background-color:#080A0F;-fx-padding:28;");
            root.setPrefWidth(560);

            // ── KPI row ──────────────────────────────────────────
            Label title = new Label("EVENT STATISTICS");
            title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#ECEEF8;-fx-letter-spacing:3px;");

            HBox kpis = new HBox(12);
            kpis.getChildren().addAll(
                    kpiCard("TOTAL",        String.valueOf(total),               "#6366F1"),
                    kpiCard("UPCOMING",     String.valueOf(upcoming),            "#22C55E"),
                    kpiCard("AVG CAPACITY", String.format("%.0f", avgCap),       "#F59E0B"),
                    kpiCard("TOTAL SEATS",  String.valueOf(totalCap),            "#F43F5E")
            );

            // ── By Status ─────────────────────────────────────────
            VBox statusSection = chartSection("Events by Status", byStatus, "#6366F1");

            // ── By Month ──────────────────────────────────────────
            Map<String, Long> monthNamed = new LinkedHashMap<>();
            byMonth.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> monthNamed.put(
                            e.getKey().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), e.getValue()));
            VBox monthSection = chartSection("Events per Month", monthNamed, "#E8320A");

            // ── Top Genres ────────────────────────────────────────
            VBox genreSection = chartSection("Top Genres", genres, "#C8FF00");

            ScrollPane scroll = new ScrollPane(new VBox(16, kpis, statusSection, monthSection, genreSection));
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
            scroll.setPrefHeight(520);

            root.getChildren().addAll(title, scroll);
            root.setStyle("-fx-padding:20 24 24 24;");

            OverlayService.init(Router.getContentArea());
            OverlayService.show("Event Statistics", root);
        } catch (Exception e) { error("Stats error: " + e.getMessage()); }
    }

    private VBox kpiCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0D0F16;-fx-border-color:" + color
                + " #1C2133 #1C2133 " + color + ";-fx-border-width:1 1 1 4;"
                + "-fx-padding:12 16 12 16;-fx-background-radius:8;-fx-border-radius:8;");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#7880A0;-fx-letter-spacing:2px;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#ECEEF8;");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    private VBox chartSection(String heading, Map<String, Long> data, String barColor) {
        VBox section = new VBox(8);
        section.setStyle("-fx-background-color:#0D0F16;-fx-padding:16;-fx-background-radius:10;"
                + "-fx-border-color:#1C2133;-fx-border-radius:10;-fx-border-width:1;");

        Label h = new Label(heading.toUpperCase());
        h.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#7880A0;-fx-letter-spacing:2px;");
        section.getChildren().add(h);

        if (data.isEmpty()) {
            Label empty = new Label("No data");
            empty.setStyle("-fx-text-fill:#3D4560;-fx-font-size:11px;");
            section.getChildren().add(empty);
            return section;
        }

        long max = data.values().stream().mapToLong(Long::longValue).max().orElse(1);

        data.forEach((key, count) -> {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label keyLbl = new Label(key);
            keyLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#C8CDD8;-fx-font-family:'Courier New',monospace;");
            keyLbl.setMinWidth(90);

            double pct = (double) count / max;
            Region track = new Region();
            track.setPrefHeight(10);
            track.setStyle("-fx-background-color:#141826;-fx-background-radius:3;");
            HBox.setHgrow(track, Priority.ALWAYS);

            Region fill = new Region();
            fill.setPrefHeight(10);
            fill.setPrefWidth(pct * 340);
            fill.setMaxWidth(pct * 340);
            fill.setStyle("-fx-background-color:" + barColor + ";-fx-background-radius:3;");

            StackPane bar = new StackPane();
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            bar.getChildren().addAll(track, fill);
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label countLbl = new Label(String.valueOf(count));
            countLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + barColor
                    + ";-fx-font-family:'Courier New',monospace;");
            countLbl.setMinWidth(30);

            row.getChildren().addAll(keyLbl, bar, countLbl);
            section.getChildren().add(row);
        });

        return section;
    }

    @FXML public void onExportCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Events as CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        fc.setInitialFileName("events.csv");
        File file = fc.showSaveDialog(eventsTable.getScene().getWindow());
        if (file == null) return;
        try {
            service.exportToCSV(data, file);
            info("Exported " + data.size() + " events to " + file.getName());
        } catch (Exception e) { error("CSV export failed: " + e.getMessage()); }
    }

    @FXML public void onExportICal() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Events as iCalendar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCal files", "*.ics"));
        fc.setInitialFileName("events.ics");
        File file = fc.showSaveDialog(eventsTable.getScene().getWindow());
        if (file == null) return;
        try {
            icalService.exportEvents(data, file);
            info("Exported " + data.size() + " events to " + file.getName());
        } catch (Exception e) { error("iCal export failed: " + e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Done"); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
}
