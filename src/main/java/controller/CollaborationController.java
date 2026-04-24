package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Collaboration;
import service.CollaborationService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class CollaborationController implements Initializable {

    @FXML
    private ComboBox<String> cmbType;

    @FXML
    private TableColumn<Collaboration, Integer> colId;

    @FXML
    private TableColumn<Collaboration, Double> colPrice;

    @FXML
    private TableColumn<Collaboration, LocalDate> colStartDate;

    @FXML
    private TableColumn<Collaboration, String> colStatus;

    @FXML
    private TableColumn<Collaboration, String> colTitle;

    @FXML
    private TableColumn<Collaboration, String> colType;

    @FXML
    private DatePicker dpEndDate;

    @FXML
    private DatePicker dpStartDate;

    @FXML
    private TableView<Collaboration> tableCollaborations;

    @FXML
    private TextField txtLinkUrl;

    @FXML
    private TextField txtPartnerId;

    @FXML
    private TextField txtPrice;

    @FXML
    private TextField txtTitle;

    private CollaborationService collaborationService = new CollaborationService();
    private ObservableList<Collaboration> collaborationsList = FXCollections.observableArrayList();
    private Collaboration selectedCollaboration = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        cmbType.setItems(FXCollections.observableArrayList("image", "video"));
        cmbType.getSelectionModel().selectFirst();

        loadData();

        tableCollaborations.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillForm(newSelection);
            }
        });
    }

    private void loadData() {
        try {
            collaborationsList.setAll(collaborationService.recuperer());
            tableCollaborations.setItems(collaborationsList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Base de données", "Impossible de charger les données.");
        }
    }

    private void fillForm(Collaboration c) {
        selectedCollaboration = c;
        txtPartnerId.setText(String.valueOf(c.getPartnerId()));
        txtTitle.setText(c.getTitle());
        cmbType.setValue(c.getType());
        txtLinkUrl.setText(c.getLinkUrl());
        dpStartDate.setValue(c.getStartDate());
        dpEndDate.setValue(c.getEndDate());
        txtPrice.setText(c.getPrice() != null ? String.valueOf(c.getPrice()) : "");
    }

    @FXML
    void handleClear(ActionEvent event) {
        selectedCollaboration = null;
        txtPartnerId.clear();
        txtTitle.clear();
        txtLinkUrl.clear();
        txtPrice.clear();
        dpStartDate.setValue(null);
        dpEndDate.setValue(null);
        cmbType.getSelectionModel().selectFirst();
        tableCollaborations.getSelectionModel().clearSelection();
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (selectedCollaboration != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cette collaboration ?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait();
            
            if (confirm.getResult() == ButtonType.YES) {
                try {
                    collaborationService.supprimer(selectedCollaboration.getId());
                    loadData();
                    handleClear(null);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Collaboration supprimée.");
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression.");
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une collaboration à supprimer.");
        }
    }

    @FXML
    void handleSave(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        try {
            int partnerId = Integer.parseInt(txtPartnerId.getText());
            String title = txtTitle.getText().trim();
            String type = cmbType.getValue();
            String linkUrl = txtLinkUrl.getText().trim();
            LocalDate startDate = dpStartDate.getValue();
            LocalDate endDate = dpEndDate.getValue();
            Double price = txtPrice.getText().isEmpty() ? null : Double.parseDouble(txtPrice.getText());

            // Check duplicate title preventing duplicate logic
            boolean exists = collaborationsList.stream()
                .anyMatch(c -> c.getTitle().equalsIgnoreCase(title) && (selectedCollaboration == null || c.getId() != selectedCollaboration.getId()));

            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Erreur de Doublon", "Une collaboration avec ce titre existe déjà !");
                return;
            }

            if (selectedCollaboration == null) {
                // Add new
                Collaboration c = new Collaboration(partnerId, title, type, "", linkUrl, "top", startDate, endDate);
                c.setStatus("PENDING");
                c.setPrice(price);
                collaborationService.ajouter(c);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Collaboration ajoutée avec succès.");
            } else {
                // Update
                selectedCollaboration.setPartnerId(partnerId);
                selectedCollaboration.setTitle(title);
                selectedCollaboration.setType(type);
                selectedCollaboration.setLinkUrl(linkUrl);
                selectedCollaboration.setStartDate(startDate);
                selectedCollaboration.setEndDate(endDate);
                selectedCollaboration.setPrice(price);
                collaborationService.modifier(selectedCollaboration);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Collaboration modifiée avec succès.");
            }
            loadData();
            handleClear(null);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Base de données", "Erreur lors de l'enregistrement.");
        }
    }
    

    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();

        // 1. Check Empty Fields
        if (txtPartnerId.getText() == null || txtPartnerId.getText().trim().isEmpty()) {
            errorMessage.append("- L'ID du partenaire est requis.\n");
        } else {
            try {
                int pid = Integer.parseInt(txtPartnerId.getText());
                if (pid <= 0) errorMessage.append("- L'ID du partenaire doit être un entier positif.\n");
            } catch (NumberFormatException e) {
                errorMessage.append("- L'ID du partenaire doit être un nombre valide.\n");
            }
        }

        if (txtTitle.getText() == null || txtTitle.getText().trim().isEmpty()) {
            errorMessage.append("- Le titre est obligatoire.\n");
        } else if (txtTitle.getText().length() < 3) {
            errorMessage.append("- Le titre doit contenir au moins 3 caractères.\n");
        }

        // 2. Format URL
        if (txtLinkUrl.getText() != null && !txtLinkUrl.getText().isEmpty()) {
            if (!txtLinkUrl.getText().matches("^(http|https)://.*$")) {
                errorMessage.append("- L'URL doit commencer par http:// ou https://\n");
            }
        }

        // 3. Price Validation
        if (txtPrice.getText() != null && !txtPrice.getText().trim().isEmpty()) {
            try {
                double price = Double.parseDouble(txtPrice.getText());
                if (price < 0) {
                    errorMessage.append("- Le prix ne peut pas être négatif.\n");
                }
            } catch (NumberFormatException e) {
                errorMessage.append("- Le prix doit être un nombre décimal valide (ex: 50.5)\n");
            }
        }

        // 4. Date Validation
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                errorMessage.append("- La date de fin ne peut pas être antérieure à la date de début.\n");
            }
        }

        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Erreurs de validation", errorMessage.toString());
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
