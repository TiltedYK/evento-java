package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.Event;
import service.EventService;
import util.Router;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, Number> colId;
    @FXML private TableColumn<Event, String> colTitre;
    @FXML private TableColumn<Event, String> colDate;
    @FXML private TableColumn<Event, String> colVenue;
    @FXML private TableColumn<Event, String> colGenre;
    @FXML private TableColumn<Event, Number> colCapacite;
    @FXML private TableColumn<Event, String> colStatut;
    @FXML private TableColumn<Event, Void> colActions;

    private final EventService service = new EventService();
    private final ObservableList<Event> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Event pendingEdit; // passed to form

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("all", "draft", "published", "cancelled", "completed");
        statusFilter.setValue("all");

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateHeure() != null ? c.getValue().getDateHeure().format(DATE_FMT) : ""));
        colVenue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVenue()));
        colGenre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGenre()));
        colCapacite.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCapacite()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        setupStatusPillColumn();
        setupActionsColumn();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());

        eventsTable.setItems(data);
        refresh();
    }

    private void setupStatusPillColumn() {
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else {
                    Label pill = new Label(item);
                    pill.getStyleClass().add("status-pill");
                    switch (item.toLowerCase()) {
                        case "published", "confirmed" -> pill.getStyleClass().add("status-confirmed");
                        case "cancelled"              -> pill.getStyleClass().add("status-cancelled");
                        case "draft"                  -> pill.getStyleClass().add("status-draft");
                        default                       -> pill.getStyleClass().add("status-pending");
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
                btnEdit.getStyleClass().addAll("button", "btn-primary", "btn-action");
                btnDelete.getStyleClass().addAll("button", "btn-danger", "btn-action");
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
        } catch (Exception e) { error("Failed to load events: " + e.getMessage()); }
    }

    private void applyFilters() {
        try {
            List<Event> all = service.recuperer();
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String st = statusFilter.getValue();

            List<Event> filtered = all.stream()
                    .filter(ev -> q.isEmpty()
                            || (ev.getTitre() != null && ev.getTitre().toLowerCase().contains(q))
                            || (ev.getVenue() != null && ev.getVenue().toLowerCase().contains(q)))
                    .filter(ev -> st == null || st.equals("all")
                            || (ev.getStatut() != null && ev.getStatut().equalsIgnoreCase(st)))
                    .toList();
            data.setAll(filtered);
        } catch (Exception e) { error("Filter failed: " + e.getMessage()); }
    }

    @FXML public void onReset() {
        searchField.clear();
        statusFilter.setValue("all");
    }

    @FXML public void onAdd() { openForm(null); }

    private void onDelete(Event ev) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm deletion");
        a.setHeaderText("Delete event #" + ev.getId() + "?");
        a.setContentText("This action cannot be undone.\nEvent: " + ev.getTitre());
        styleAlert(a);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimer(ev.getId()); refresh(); }
                catch (Exception e) { error("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void openForm(Event ev) {
        pendingEdit = ev;
        Router.navigate("/fxml/EventForm.fxml");
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
