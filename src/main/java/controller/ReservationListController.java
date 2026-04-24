package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.DefaultStringConverter;
import model.Reservation;
import service.ReservationService;
import util.Router;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Reservation> table;
    @FXML private TableColumn<Reservation, Number> colEvent;
    @FXML private TableColumn<Reservation, Number> colUser;
    @FXML private TableColumn<Reservation, Number> colPlaces;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, String> colCreated;
    @FXML private TableColumn<Reservation, Void> colActions;

    private final ReservationService service = new ReservationService();
    private final ObservableList<Reservation> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Reservation pendingEdit;

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("all", "pending", "confirmed", "cancelled");
        statusFilter.setValue("all");

        table.setEditable(true);

        colEvent.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getEventId()));
        colUser.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getUserId()));
        colPlaces.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getNombrePlaces()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));

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
        try { data.setAll(service.recuperer()); applyFilters(); }
        catch (Exception e) { error("Failed to load reservations: " + e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<Reservation> all = service.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim();
            String st = statusFilter.getValue();
            List<Reservation> filtered = all.stream()
                    .filter(r -> q.isEmpty()
                            || String.valueOf(r.getEventId()).equals(q)
                            || String.valueOf(r.getUserId()).equals(q))
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
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete this reservation?");
        a.setContentText("Event ID: " + r.getEventId() + " — User ID: " + r.getUserId() + "\nThis action cannot be undone.");
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
