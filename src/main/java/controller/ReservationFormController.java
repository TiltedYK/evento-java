package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Reservation;
import service.ReservationService;
import util.Router;

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

        this.editing = ReservationListController.pendingEdit;
        ReservationListController.pendingEdit = null;

        if (editing == null) {
            formTitle.setText("New Reservation");
            saveButton.setText("Create reservation");
        } else {
            formTitle.setText("Edit Reservation #" + editing.getId());
            saveButton.setText("Save changes");

            eventIdField.setText(String.valueOf(editing.getEventId()));
            userIdField.setText(String.valueOf(editing.getUserId()));
            placesSpinner.getValueFactory().setValue(editing.getNombrePlaces());
            if (editing.getStatut() != null) statutCombo.setValue(editing.getStatut());
        }
    }

    @FXML
    public void onSave() {
        int eventId, userId;
        try {
            eventId = Integer.parseInt(eventIdField.getText().trim());
            userId = Integer.parseInt(userIdField.getText().trim());
        } catch (Exception e) {
            error("Event ID and User ID must be valid numbers.");
            return;
        }
        if (eventId <= 0 || userId <= 0) { error("Event ID and User ID must be positive."); return; }

        Reservation target = editing != null ? editing : new Reservation();
        target.setEventId(eventId);
        target.setUserId(userId);
        target.setNombrePlaces(placesSpinner.getValue());
        target.setStatut(statutCombo.getValue());
        if (editing == null) target.setCreatedAt(LocalDateTime.now());

        try {
            if (editing == null) service.ajouter(target);
            else service.modifier(target);
            onCancel();
        } catch (Exception e) { error("Save failed: " + e.getMessage()); }
    }

    @FXML public void onCancel() { Router.navigate("/fxml/ReservationList.fxml"); }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        a.showAndWait();
    }
}
