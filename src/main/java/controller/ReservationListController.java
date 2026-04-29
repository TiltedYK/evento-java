package controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import model.Event;
import model.Reservation;
import model.User;
import service.EventService;
import service.ReservationService;
import service.UserService;
import util.Router;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Reservation> table;
    @FXML private TableColumn<Reservation, String> colEvent;
    @FXML private TableColumn<Reservation, String> colUser;
    @FXML private TableColumn<Reservation, Integer> colPlaces;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, String> colCreated;
    @FXML private TableColumn<Reservation, Void> colActions;

    private final ReservationService service = new ReservationService();
    private final EventService eventService = new EventService();
    private final UserService userService = new UserService();
    private final ObservableList<Reservation> data = FXCollections.observableArrayList();
    private final Map<Integer, String> eventTitles = new HashMap<>();
    private final Map<Integer, String> userEmails = new HashMap<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Reservation pendingEdit;

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("all", "pending", "confirmed", "cancelled");
        statusFilter.setValue("all");

        table.setEditable(true);

        colEvent.setCellValueFactory(c -> new SimpleStringProperty(
                eventTitles.getOrDefault(c.getValue().getEventId(), "—")));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(
                userEmails.getOrDefault(c.getValue().getUserId(), "—")));
        colPlaces.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNombrePlaces()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));

        colPlaces.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colPlaces.setOnEditCommit(e -> {
            Reservation r = e.getRowValue();
            Integer v = e.getNewValue();
            if (v != null && v > 0) {
                r.setNombrePlaces(v);
                try { service.modifier(r); refresh(); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
            }
        });

        // Inline edit on status field
        colStatut.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            private final TextField editor = new TextField();

            {
                pill.getStyleClass().add("status-pill");
                pill.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) startEdit();
                });
                editor.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());
                    r.setStatut(editor.getText());
                    try { service.modifier(r); refresh(); } catch (Exception ex) { error("Save failed: " + ex.getMessage()); }
                    cancelEdit();
                });
                editor.setOnKeyPressed(e -> {
                    if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) cancelEdit();
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                pill.setText(item);
                pill.getStyleClass().removeAll("status-confirmed", "status-cancelled", "status-pending", "status-draft");
                switch (item.toLowerCase()) {
                    case "confirmed" -> pill.getStyleClass().add("status-confirmed");
                    case "cancelled" -> pill.getStyleClass().add("status-cancelled");
                    case "pending"   -> pill.getStyleClass().add("status-pending");
                    default          -> pill.getStyleClass().add("status-draft");
                }
                setGraphic(isEditing() ? editor : pill);
                setText(null);
            }

            @Override
            public void startEdit() {
                super.startEdit();
                Reservation r = getTableView().getItems().get(getIndex());
                editor.setText(r.getStatut());
                setGraphic(editor);
                editor.requestFocus();
                editor.selectAll();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                updateItem(getItem(), isEmpty());
            }
        });

        setupActionsColumn();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private void loadLookups() {
        eventTitles.clear();
        userEmails.clear();
        try {
            for (Event ev : eventService.recuperer()) {
                String t = ev.getTitre() != null ? ev.getTitre() : "(untitled event)";
                eventTitles.put(ev.getId(), t);
            }
            for (User u : userService.recuperer()) {
                String em = u.getEmail() != null ? u.getEmail() : (u.getPrenom() + " " + u.getNom()).trim();
                userEmails.put(u.getId(), em.isBlank() ? "—" : em);
            }
        } catch (Exception ignored) { }
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            {
                btnDelete.getStyleClass().addAll("button", "btn-danger", "btn-action");
                btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void refresh() {
        try {
            loadLookups();
            data.setAll(service.recuperer());
            applyFilters();
        }
        catch (Exception e) { error("Failed to load reservations: " + e.getMessage()); }
    }

    private void applyFilters() {
        try {
            loadLookups();
            List<Reservation> all = service.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String st = statusFilter.getValue();
            List<Reservation> filtered = all.stream()
                    .filter(r -> q.isEmpty()
                            || eventTitles.getOrDefault(r.getEventId(), "").toLowerCase().contains(q)
                            || userEmails.getOrDefault(r.getUserId(), "").toLowerCase().contains(q)
                            || String.valueOf(r.getNombrePlaces()).contains(q))
                    .filter(r -> st == null || st.equals("all")
                            || (r.getStatut() != null && r.getStatut().equalsIgnoreCase(st)))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    @FXML public void onReset() { searchField.clear(); statusFilter.setValue("all"); }

    @FXML public void onAdd() {
        pendingEdit = null;
        Router.navigate("/fxml/ReservationForm.fxml");
    }

    private void onDelete(Reservation r) {
        String ev = eventTitles.getOrDefault(r.getEventId(), "this event");
        String usr = userEmails.getOrDefault(r.getUserId(), "this user");
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete this reservation?");
        a.setContentText(ev + " — " + usr + "\nThis action cannot be undone.");
        styleAlert(a);
        a.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try { service.supprimer(r.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
}
