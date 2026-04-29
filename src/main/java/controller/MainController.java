package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController implements Initializable {

    @FXML
    private Button btnCollaborations;

    @FXML
    private Button btnReferrals;

    @FXML
    private Button btnRequests;

    @FXML
    private StackPane contentArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showCollaborations(null);
    }

    @FXML
    void showCollaborations(ActionEvent event) {
        setActive(btnCollaborations);
        loadView("/fxml/CollaborationView.fxml");
    }

    @FXML
    void showReferrals(ActionEvent event) {
        setActive(btnReferrals);
        loadView("/fxml/ReferralHitView.fxml");
    }

    @FXML
    void showRequests(ActionEvent event) {
        setActive(btnRequests);
        loadView("/fxml/PartnershipRequestView.fxml");
    }

    private void setActive(Button button) {
        btnCollaborations.getStyleClass().remove("nav-active");
        btnRequests.getStyleClass().remove("nav-active");
        btnReferrals.getStyleClass().remove("nav-active");
        button.getStyleClass().add("nav-active");
    }

    private void loadView(String path) {
        try {
            URL resource = getClass().getResource(path);
            if (resource == null) {
                showAlert("Erreur FXML", "Fichier FXML introuvable : " + path);
                return;
            }
            Node node = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(node);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur de chargement", "Impossible de charger la vue : " + ex.getMessage() + "\nCause : " + ex.getCause());
        }
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
