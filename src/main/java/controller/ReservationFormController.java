package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Reservation;
import service.ReservationService;

import java.time.LocalDateTime;

public class ReservationFormController {

    @FXML private Label formTitle;
    @FXML private TextField eventIdField;
    @FXML private TextField userIdField;
    @FXML private Spinner<Integer> placesSpinner;
    @FXML private ComboBox<String> statutCombo;
    @FXML private Button saveButton;

    private final ReservationService service = new ReservationService();
    private Reservation editing;

    @FXML
    public void initialize() {
        placesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1, 1));
        statutCombo.getItems().addAll("pending", "confirmed", "cancelled");
        statutCombo.setValue("pending");
    }

    public void setReservation(Reservation r) {
        this.editing = r;
        if (r == null) {
            formTitle.setText("New Reservation");
            saveButton.setText("Create reservation");
            return;
        }
        formTitle.setText("Edit Reservation #" + r.getId());
        saveButton.setText("Save changes");

        eventIdField.setText(String.valueOf(r.getEventId()));
        userIdField.setText(String.valueOf(r.getUserId()));
        placesSpinner.getValueFactory().setValue(r.getNombrePlaces());
        if (r.getStatut() != null) statutCombo.setValue(r.getStatut());
    }

    @FXML
    public void onSave() {
        int eventId, userId;
        try {
            eventId = Integer.parseInt(eventIdField.getText().trim());
            userId = Integer.parseInt(userIdField.getText().trim());
        } catch (Exception e) {
            showError("Event ID and User ID must be valid numbers.");
            return;
        }
        if (eventId <= 0 || userId <= 0) {
            showError("Event ID and User ID must be positive.");
            return;
        }

        Reservation target = editing != null ? editing : new Reservation();
        target.setEventId(eventId);
        target.setUserId(userId);
        target.setNombrePlaces(placesSpinner.getValue());
        target.setStatut(statutCombo.getValue());
        if (editing == null) target.setCreatedAt(LocalDateTime.now());

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            close();
        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    @FXML public void onCancel() { close(); }

    private void close() {
        ((Stage) eventIdField.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
