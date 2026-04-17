package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.ReferralHit;
import service.ReferralHitService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;

public class ReferralHitController implements Initializable {

    @FXML
    private TableColumn<ReferralHit, LocalDateTime> colDate;

    @FXML
    private TableColumn<ReferralHit, Integer> colId;

    @FXML
    private TableColumn<ReferralHit, Integer> colInfluencer;

    @FXML
    private TableColumn<ReferralHit, Integer> colReferred;

    @FXML
    private TableColumn<ReferralHit, String> colSession;

    @FXML
    private Label lblConversions;

    @FXML
    private Label lblTotalHits;

    @FXML
    private TableView<ReferralHit> tableHits;

    @FXML
    private TextField txtInfluencerId;

    @FXML
    private TextField txtReferredUserId;

    @FXML
    private TextField txtSessionId;

    private ReferralHitService referralHitService = new ReferralHitService();
    private ObservableList<ReferralHit> hitsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colInfluencer.setCellValueFactory(new PropertyValueFactory<>("influencerId"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("visitedAt"));
        colReferred.setCellValueFactory(new PropertyValueFactory<>("referredUserId"));

        handleGenerateSession(null);
        loadData();
    }

    private void loadData() {
        try {
            hitsList.setAll(referralHitService.recuperer());
            tableHits.setItems(hitsList);
            updateStats();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load referral hits.");
        }
    }

    private void updateStats() {
        lblTotalHits.setText(String.valueOf(hitsList.size()));
        long conversions = hitsList.stream().filter(hit -> hit.getReferredUserId() != null && hit.getReferredUserId() > 0).count();
        lblConversions.setText(String.valueOf(conversions));
    }

    @FXML
    void handleGenerateSession(ActionEvent event) {
        txtSessionId.setText(UUID.randomUUID().toString().substring(0, 12));
    }

    @FXML
    void handleSimulateConversion(ActionEvent event) {
        if (txtReferredUserId.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please provide a valid Referred User ID for a conversion.");
            return;
        }
        submitHit(true);
    }

    @FXML
    void handleSimulateView(ActionEvent event) {
        submitHit(false);
    }

    private void submitHit(boolean isConversion) {
        try {
            if (txtInfluencerId.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Influencer ID is required.");
                return;
            }

            int influencerId = Integer.parseInt(txtInfluencerId.getText());
            String sessionId = txtSessionId.getText(); // Using the generated session
            Integer referredId = null;

            if (isConversion) {
                referredId = Integer.parseInt(txtReferredUserId.getText());
            }

            ReferralHit hit = new ReferralHit(influencerId, sessionId, referredId);
            referralHitService.ajouter(hit);

            loadData();
            handleGenerateSession(null); // Generate new session for next simulation
            
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numeric IDs.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to record the hit.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
