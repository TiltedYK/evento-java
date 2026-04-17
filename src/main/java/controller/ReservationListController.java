package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Reservation;
import service.ReservationService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Reservation> table;
    @FXML private TableColumn<Reservation, Number> colId;
    @FXML private TableColumn<Reservation, Number> colEvent;
    @FXML private TableColumn<Reservation, Number> colUser;
    @FXML private TableColumn<Reservation, Number> colPlaces;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, String> colCreated;
    @FXML private TableColumn<Reservation, Void> colActions;

    private final ReservationService service = new ReservationService();
    private final ObservableList<Reservation> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("all", "pending", "confirmed", "cancelled");
        statusFilter.setValue("all");

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colEvent.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getEventId()));
        colUser.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getUserId()));
        colPlaces.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getNombrePlaces()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));

        setupStatusPillColumn();
        setupActionsColumn();

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        table.setItems(data);
        refresh();
    }

    private void setupStatusPillColumn() {
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    Label pill = new Label(item);
                    pill.getStyleClass().add("status-pill");
                    switch (item.toLowerCase()) {
                        case "confirmed" -> pill.getStyleClass().add("status-confirmed");
                        case "cancelled" -> pill.getStyleClass().add("status-cancelled");
                        case "pending"   -> pill.getStyleClass().add("status-pending");
                        default          -> pill.getStyleClass().add("status-draft");
                    }
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().addAll("button", "btn-primary");
                btnDelete.getStyleClass().addAll("button", "btn-danger");
                btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
                btnDelete.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
                btnEdit.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void refresh() {
        try {
            data.setAll(service.recuperer());
            applyFilters();
        } catch (Exception e) { showError("Failed to load reservations", e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<Reservation> all = service.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim();
            String st = statusFilter.getValue();

            List<Reservation> filtered = all.stream()
                    .filter(r -> q.isEmpty()
                            || String.valueOf(r.getEventId()).equals(q)
                            || String.valueOf(r.getUserId()).equals(q)
                            || String.valueOf(r.getId()).equals(q))
                    .filter(r -> st == null || st.equals("all")
                            || (r.getStatut() != null && r.getStatut().equalsIgnoreCase(st)))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { showError("Filter failed", e.getMessage()); }
    }

    @FXML public void onReset() {
        searchField.clear();
        statusFilter.setValue("all");
    }

    @FXML public void onAdd() { openForm(null); }

    private void onDelete(Reservation r) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete reservation #" + r.getId() + "?");
        a.setContentText("This action cannot be undone.");
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try { service.supprimer(r.getId()); refresh(); }
                catch (Exception e) { showError("Delete failed", e.getMessage()); }
            }
        });
    }

    private void openForm(Reservation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReservationForm.fxml"));
            Parent root = loader.load();
            ReservationFormController ctrl = loader.getController();
            ctrl.setReservation(r);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle(r == null ? "New Reservation" : "Edit Reservation");
            Scene sc = new Scene(root);
            sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            st.setScene(sc);
            st.showAndWait();
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open form", e.getMessage());
        }
    }

    private void showError(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
