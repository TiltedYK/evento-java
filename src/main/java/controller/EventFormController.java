package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import model.Event;
import service.EventService;
import service.WeatherService;
import util.Router;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EventFormController {

    @FXML private Label     formTitle;
    @FXML private TextField titreField;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private Spinner<Integer> capaciteSpinner;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField venueField;
    @FXML private TextField genreField;
    @FXML private TextField locationField;
    @FXML private TextArea  descriptionArea;
    @FXML private Button    saveButton;
    @FXML private TextField imageField;
    @FXML private Label     weatherLabel;

    private final EventService   service = new EventService();
    private final WeatherService weather = new WeatherService();
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
            saveButton.setText("Create Event");
        } else {
            formTitle.setText("Edit Event #" + editing.getId());
            saveButton.setText("Save Changes");
            titreField.setText(editing.getTitre());
            if (editing.getDateHeure() != null) {
                datePicker.setValue(editing.getDateHeure().toLocalDate());
                timeField.setText(editing.getDateHeure().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            capaciteSpinner.getValueFactory().setValue(editing.getCapacite());
            if (editing.getStatut() != null) statutCombo.setValue(editing.getStatut());
            venueField.setText(editing.getVenue() != null ? editing.getVenue() : "");
            genreField.setText(editing.getGenre() != null ? editing.getGenre() : "");
            locationField.setText(editing.getLocation() != null ? editing.getLocation() : "");
            descriptionArea.setText(editing.getDescription() != null ? editing.getDescription() : "");
            if (editing.getImageFilename() != null) imageField.setText(editing.getImageFilename());
        }

        // Weather fetch when venue/location focus is lost
        venueField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !venueField.getText().isBlank()) fetchWeather(venueField.getText());
        });
        locationField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !locationField.getText().isBlank()) fetchWeather(locationField.getText());
        });
    }

    private void fetchWeather(String city) {
        weatherLabel.setText("🌐 Fetching weather for " + city + "…");
        new Thread(() -> {
            String result = weather.getWeather(city);
            Platform.runLater(() -> weatherLabel.setText("🌤 " + city + ": " + result));
        }, "weather-fetch").start();
    }

    @FXML
    public void onBrowseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Event Cover Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File file = fc.showOpenDialog(imageField.getScene().getWindow());
        if (file != null) imageField.setText(file.getName());
    }

    @FXML
    public void onSave() {
        String titre = safe(titreField.getText());
        String venue = safe(venueField.getText());

        if (titre.isEmpty()) { error("Title is required."); return; }
        if (venue.isEmpty()) { error("Venue is required.");  return; }
        if (datePicker.getValue() == null) { error("Please pick a date."); return; }

        LocalTime time;
        try {
            time = LocalTime.parse(safe(timeField.getText()), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            error("Invalid time format — use HH:mm (e.g. 20:30).");
            return;
        }

        Event target = editing != null ? editing : new Event();
        target.setTitre(titre);
        target.setDateHeure(LocalDateTime.of(datePicker.getValue(), time));
        target.setCapacite(capaciteSpinner.getValue());
        target.setStatut(statutCombo.getValue());
        target.setVenue(venue);
        target.setGenre(safe(genreField.getText()));
        target.setLocation(safe(locationField.getText()));
        target.setDescription(safe(descriptionArea.getText()));
        target.setImageFilename(safe(imageField.getText()));

        try {
            if (editing == null) service.ajouter(target);
            else                 service.modifier(target);
            onCancel();
        } catch (Exception e) {
            error("Save failed: " + e.getMessage());
        }
    }

    @FXML public void onCancel() { Router.navigate("/fxml/EventList.fxml"); }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
