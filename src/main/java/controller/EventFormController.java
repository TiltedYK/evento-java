package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Event;
import service.EventService;
import util.Router;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EventFormController {

    @FXML private Label formTitle;
    @FXML private TextField titreField;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private Spinner<Integer> capaciteSpinner;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField venueField;
    @FXML private TextField genreField;
    @FXML private TextField locationField;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;

    private final EventService service = new EventService();
    private Event editing;

    @FXML
    public void initialize() {
        capaciteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 100, 10));
        statutCombo.getItems().addAll("draft", "published", "cancelled", "completed");
        statutCombo.setValue("draft");
        datePicker.setValue(LocalDate.now());
        timeField.setText("20:00");

        this.editing = EventListController.pendingEdit;
        EventListController.pendingEdit = null;

        if (editing == null) {
            formTitle.setText("New Event");
            saveButton.setText("Create event");
        } else {
            formTitle.setText("Edit Event #" + editing.getId());
            saveButton.setText("Save changes");

            titreField.setText(editing.getTitre());
            if (editing.getDateHeure() != null) {
                datePicker.setValue(editing.getDateHeure().toLocalDate());
                timeField.setText(editing.getDateHeure().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            capaciteSpinner.getValueFactory().setValue(editing.getCapacite());
            if (editing.getStatut() != null) statutCombo.setValue(editing.getStatut());
            venueField.setText(editing.getVenue());
            genreField.setText(editing.getGenre());
            locationField.setText(editing.getLocation());
            descriptionArea.setText(editing.getDescription());
        }
    }

    @FXML
    public void onSave() {
        String titre = safe(titreField.getText());
        String venue = safe(venueField.getText());

        if (titre.isEmpty()) { error("Title is required."); return; }
        if (venue.isEmpty()) { error("Venue is required."); return; }
        if (datePicker.getValue() == null) { error("Please pick a date."); return; }

        LocalTime time;
        try {
            time = LocalTime.parse(safe(timeField.getText()), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            error("Invalid time. Use HH:mm (e.g. 20:30).");
            return;
        }
        LocalDateTime dt = LocalDateTime.of(datePicker.getValue(), time);

        Event target = editing != null ? editing : new Event();
        target.setTitre(titre);
        target.setDateHeure(dt);
        target.setCapacite(capaciteSpinner.getValue());
        target.setStatut(statutCombo.getValue());
        target.setVenue(venue);
        target.setGenre(safe(genreField.getText()));
        target.setLocation(safe(locationField.getText()));
        target.setDescription(safe(descriptionArea.getText()));

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            onCancel(); // back to list
        } catch (Exception e) {
            error("Save failed: " + e.getMessage());
        }
    }

    @FXML public void onCancel() {
        Router.navigate("/fxml/EventList.fxml");
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
