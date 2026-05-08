package controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import util.OverlayService;
import model.Event;
import model.Reservation;
import model.User;
import service.EventService;
import service.PdfService;
import service.QRCodeService;
import service.ReservationService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FrontMyReservationsController implements Initializable {

    @FXML private FlowPane          ticketsGrid;
    @FXML private ComboBox<String>  statusFilter;
    @FXML private Label             ticketCount;

    private final ReservationService reservationService = new ReservationService();
    private final EventService       eventService       = new EventService();
    private final PdfService         pdfService         = new PdfService();
    private final QRCodeService      qrService          = new QRCodeService();

    private User             currentUser;
    private List<Reservation> allMyReservations;
    private List<Event>       allEvents;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusFilter.getItems().addAll("All Statuses", "pending", "confirmed", "cancelled");
        statusFilter.setValue("All Statuses");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void loadData() {
        ticketsGrid.getChildren().clear();
        if (currentUser == null) {
            showEmpty("🔒  Please log in to see your tickets.");
            return;
        }
        try {
            allEvents         = eventService.recuperer();
            allMyReservations = reservationService.recuperer().stream()
                    .filter(r -> r.getUserId() == currentUser.getId())
                    .collect(Collectors.toList());
            renderTickets(allMyReservations);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML void filterTickets() {
        if (allMyReservations == null) return;
        String status = statusFilter.getValue();
        List<Reservation> filtered = "All Statuses".equals(status)
                ? allMyReservations
                : allMyReservations.stream()
                        .filter(r -> status.equalsIgnoreCase(r.getStatut()))
                        .collect(Collectors.toList());
        renderTickets(filtered);
    }

    private void renderTickets(List<Reservation> list) {
        ticketsGrid.getChildren().clear();
        ticketCount.setText(list.size() + " ticket" + (list.size() == 1 ? "" : "s"));
        if (list.isEmpty()) { showEmpty("🎸  No tickets here.\nGo reserve a show!"); return; }

        for (int i = 0; i < list.size(); i++) {
            Reservation res = list.get(i);
            Event event = allEvents.stream()
                    .filter(e -> e.getId() == res.getEventId())
                    .findFirst().orElse(null);
            VBox card = buildTicketCard(res, event);
            ticketsGrid.getChildren().add(card);
            card.setOpacity(0);
            final int delay = i * 70;
            PauseTransition p = new PauseTransition(Duration.millis(delay));
            p.setOnFinished(ev -> { FadeTransition ft = new FadeTransition(Duration.millis(400), card); ft.setToValue(1); ft.play(); });
            p.play();
        }
    }

    private VBox buildTicketCard(Reservation res, Event event) {
        VBox card = new VBox(12);
        card.getStyleClass().add("ticket-card");
        card.setPrefWidth(340);
        card.setMinWidth(340);

        Label stub = new Label("🎟  TICKET  #EVT-" + res.getId());
        stub.getStyleClass().add("ticket-stub-label");

        Label title = new Label(event != null ? event.getTitre() : "Unknown Event");
        title.getStyleClass().add("card-event-title-large");
        title.setWrapText(true);

        VBox meta = new VBox(5);
        if (event != null) {
            row(meta, "📍", event.getVenue() != null ? event.getVenue() : "TBA");
            if (event.getDateHeure() != null)
                row(meta, "📅", event.getDateHeure().format(DateTimeFormatter.ofPattern("EEE, MMM dd · HH:mm")));
            if (event.getGenre() != null) row(meta, "🎸", event.getGenre());
        }
        row(meta, "🪑", res.getNombrePlaces() + " seat(s)");

        String statut = res.getStatut() != null ? res.getStatut() : "pending";
        Label badge = new Label(statut.toUpperCase());
        badge.getStyleClass().add("confirmed".equalsIgnoreCase(statut) ? "badge-upcoming" : "badge-status");

        // Action buttons — stacked vertically so labels never get clipped
        // (3 buttons in a 300-px row was causing the dreaded "..." ellipsis).
        VBox actions = new VBox(6);
        Button btnPDF = new Button("📄  Download Ticket PDF");
        btnPDF.getStyleClass().add("card-action-btn");
        btnPDF.setMaxWidth(Double.MAX_VALUE);
        btnPDF.setOnAction(e -> downloadTicketPDF(res, event));

        Button btnQR = new Button("📱  Show QR Code");
        btnQR.getStyleClass().add("card-action-btn-outline");
        btnQR.setMaxWidth(Double.MAX_VALUE);
        btnQR.setOnAction(e -> showQR(res, event));

        Button btnCancel = new Button("✕  Cancel reservation");
        btnCancel.getStyleClass().add("card-cancel-btn");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setDisable("cancelled".equalsIgnoreCase(statut));
        btnCancel.setOnAction(e -> onCancel(res, card));

        actions.getChildren().addAll(btnPDF, btnQR, btnCancel);

        card.getChildren().addAll(stub, title, meta, badge, actions);

        ScaleTransition up = new ScaleTransition(Duration.millis(150), card); up.setToX(1.02); up.setToY(1.02);
        ScaleTransition dn = new ScaleTransition(Duration.millis(150), card); dn.setToX(1.0);  dn.setToY(1.0);
        card.setOnMouseEntered(ev -> up.play());
        card.setOnMouseExited(ev  -> dn.play());
        return card;
    }

    // ── Actions ───────────────────────────────────────────────────────

    private void downloadTicketPDF(Reservation res, Event event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Ticket PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        fc.setInitialFileName("ticket-EVT-" + res.getId() + ".pdf");
        File file = fc.showSaveDialog(ticketsGrid.getScene().getWindow());
        if (file == null) return;
        try {
            pdfService.generateTicket(res, event, file);
            info("Ticket saved: " + file.getName());
        } catch (Exception e) {
            error("PDF failed: " + e.getMessage());
        }
    }

    private void showQR(Reservation res, Event event) {
        String content = "EVENTO|TICKET:" + res.getId()
                + "|EVENT:" + (event != null ? event.getTitre() : "?")
                + "|USER:" + res.getUserId()
                + "|SEATS:" + res.getNombrePlaces();
        try {
            javafx.scene.image.Image qr = qrService.generateQRCode(content, 250);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(qr);
            iv.setFitWidth(250); iv.setFitHeight(250);

            Label sub = new Label("Scan at the door to validate entry.");
            sub.setStyle("-fx-font-size:11px;-fx-text-fill:#5A5F78;-fx-font-family:'Courier New',monospace;");

            VBox box = new VBox(14, iv, sub);
            box.setAlignment(javafx.geometry.Pos.CENTER);
            box.setStyle("-fx-padding:16 24 20 24;");

            OverlayService.show("Ticket QR — #EVT-" + res.getId(), box);
        } catch (Exception e) { error("QR failed: " + e.getMessage()); }
    }

    private void onCancel(Reservation res, VBox card) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Cancel Ticket");
        dlg.setHeaderText("Cancel ticket #EVT-" + res.getId() + "?");
        dlg.setContentText("This will mark your reservation as cancelled. This cannot be undone.");
        styleAlert(dlg);
        dlg.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    res.setStatut("cancelled");
                    reservationService.modifier(res);
                    FadeTransition fade = new FadeTransition(Duration.millis(400), card);
                    fade.setToValue(0.4); fade.play();
                    loadData();
                } catch (SQLException e) { error("Cancel failed: " + e.getMessage()); }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void row(VBox parent, String icon, String text) {
        HBox row = new HBox(8);
        Label ic = new Label(icon); ic.setMinWidth(20);
        Label tx = new Label(text); tx.getStyleClass().add("card-event-venue");
        row.getChildren().addAll(ic, tx);
        parent.getChildren().add(row);
    }

    private void showEmpty(String msg) {
        Label l = new Label(msg); l.getStyleClass().add("front-empty-label"); l.setWrapText(true);
        ticketsGrid.getChildren().add(l);
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("EVENTO"); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        try {
            a.getDialogPane().getStylesheets().add(getClass().getResource("/css/front-styles.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}
