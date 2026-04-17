package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Event;
import service.EventService;

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
    }

    public void setEvent(Event ev) {
        this.editing = ev;
        if (ev == null) {
            formTitle.setText("New Event");
            saveButton.setText("Create event");
            return;
        }
        formTitle.setText("Edit Event #" + ev.getId());
        saveButton.setText("Save changes");

        titreField.setText(ev.getTitre());
        if (ev.getDateHeure() != null) {
            datePicker.setValue(ev.getDateHeure().toLocalDate());
            timeField.setText(ev.getDateHeure().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        capaciteSpinner.getValueFactory().setValue(ev.getCapacite());
        if (ev.getStatut() != null) statutCombo.setValue(ev.getStatut());
        venueField.setText(ev.getVenue());
        genreField.setText(ev.getGenre());
        locationField.setText(ev.getLocation());
        descriptionArea.setText(ev.getDescription());
    }

    @FXML
    public void onSave() {
        String titre = safe(titreField.getText());
        String venue = safe(venueField.getText());

        if (titre.isEmpty()) { showError("Title is required."); return; }
        if (venue.isEmpty()) { showError("Venue is required."); return; }
        if (datePicker.getValue() == null) { showError("Please pick a date."); return; }

        LocalTime time;
        try {
            time = LocalTime.parse(safe(timeField.getText()), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            showError("Invalid time. Use HH:mm (e.g. 20:30).");
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
            close();
        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    @FXML
    public void onCancel() {
        close();
    }

    private void close() {
        ((Stage) titreField.getScene().getWindow()).close();
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
