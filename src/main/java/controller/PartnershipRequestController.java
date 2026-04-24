package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Collaboration;
import model.PartnershipRequest;
import service.CollaborationService;
import service.PartnershipRequestService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class PartnershipRequestController implements Initializable {

    @FXML private TableView<PartnershipRequest> tableRequests;
    @FXML private TableColumn<PartnershipRequest, Integer> colId;
    @FXML private TableColumn<PartnershipRequest, String> colCompany;
    @FXML private TableColumn<PartnershipRequest, String> colContact;
    @FXML private TableColumn<PartnershipRequest, String> colEmail;
    @FXML private TableColumn<PartnershipRequest, String> colStatus;
    @FXML private TableColumn<PartnershipRequest, LocalDateTime> colDate;

    // Form fields
    @FXML private TextField txtCompany;
    @FXML private TextField txtContact;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextArea txtMessage;

    private PartnershipRequestService requestService = new PartnershipRequestService();
    private CollaborationService collaborationService = new CollaborationService();
    private ObservableList<PartnershipRequest> requestsList = FXCollections.observableArrayList();
    private PartnershipRequest selectedRequest = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCompany.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        loadData();

        tableRequests.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                fillForm(newSel);
            }
        });
    }

    private void loadData() {
        try {
            requestsList.setAll(requestService.recuperer());
            tableRequests.setItems(requestsList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de charger les requêtes.");
        }
    }

    private void fillForm(PartnershipRequest request) {
        selectedRequest = request;
        txtCompany.setText(request.getCompanyName());
        txtContact.setText(request.getContactName());
        txtEmail.setText(request.getEmail());
        txtPhone.setText(request.getPhone());
        txtMessage.setText(request.getMessage());
    }

    @FXML
    void handleClear(ActionEvent event) {
        selectedRequest = null;
        txtCompany.clear();
        txtContact.clear();
        txtEmail.clear();
        txtPhone.clear();
        txtMessage.clear();
        tableRequests.getSelectionModel().clearSelection();
    }

    @FXML
    void handleSave(ActionEvent event) {
        if (!validateInput()) return;

        try {
            String company = txtCompany.getText().trim();
            String contact = txtContact.getText().trim();
            String email = txtEmail.getText().trim();
            String phone = txtPhone.getText().trim();
            String message = txtMessage.getText().trim();

            // Check for duplicates (same email and company for pending request)
            boolean exists = requestsList.stream()
                .anyMatch(r -> r.getEmail().equalsIgnoreCase(email) 
                            && r.getCompanyName().equalsIgnoreCase(company)
                            && "PENDING".equalsIgnoreCase(r.getStatus())
                            && (selectedRequest == null || r.getId() != selectedRequest.getId()));

            if (exists) {
                showAlert(Alert.AlertType.ERROR, "Doublon", "Une requête en attente existe déjà pour cet email et entreprise.");
                return;
            }

            if (selectedRequest == null) {
                // Add
                PartnershipRequest p = new PartnershipRequest(contact, email, company, phone, message);
                requestService.ajouter(p);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Demande ajoutée.");
            } else {
                // Update
                selectedRequest.setCompanyName(company);
                selectedRequest.setContactName(contact);
                selectedRequest.setEmail(email);
                selectedRequest.setPhone(phone);
                selectedRequest.setMessage(message);
                requestService.modifier(selectedRequest);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Demande mise à jour.");
            }
            loadData();
            handleClear(null);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la sauvegarde.");
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (selectedRequest == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une requête.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement cette demande ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        
        if (confirm.getResult() == ButtonType.YES) {
            try {
                requestService.supprimer(selectedRequest.getId());
                loadData();
                handleClear(null);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Demande supprimée.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de suppression.");
            }
        }
    }

    @FXML
    void handleAccept(ActionEvent event) {
        if (selectedRequest == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une requête en attente.");
            return;
        }
        
        if (!"PENDING".equalsIgnoreCase(selectedRequest.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Cette requête a déjà été traitée.");
            return;
        }

        try {
            selectedRequest.setStatus("ACCEPTED");
            requestService.modifier(selectedRequest);

            int partnerId = selectedRequest.getUserId() != null ? selectedRequest.getUserId() : 1; 

            Collaboration newCollab = new Collaboration(
                    partnerId, 
                    "Collab : " + selectedRequest.getCompanyName(), 
                    "image", 
                    "", 
                    "", 
                    "top", 
                    LocalDate.now(), 
                    LocalDate.now().plusMonths(1)
            );
            newCollab.setStatus("PENDING");
            
            collaborationService.ajouter(newCollab);
            
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Collaboration Créée", "La demande a été acceptée et une collaboration a été ajoutée automatiquement !");
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Base de données", "Erreur lors de l'acceptation.");
        }
    }

    @FXML
    void handleReject(ActionEvent event) {
        if (selectedRequest == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une requête.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selectedRequest.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Cette requête a déjà été traitée.");
            return;
        }

        try {
            selectedRequest.setStatus("REJECTED");
            requestService.modifier(selectedRequest);
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Demande refusée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du refus.");
        }
    }


    private boolean validateInput() {
        StringBuilder error = new StringBuilder();

        if (txtCompany.getText() == null || txtCompany.getText().trim().isEmpty()) {
            error.append("- Nom de l'entreprise requis.\n");
        }
        
        if (txtContact.getText() == null || txtContact.getText().trim().isEmpty()) {
            error.append("- Nom du contact requis.\n");
        } else if (txtContact.getText().length() < 3) {
            error.append("- Nom du contact trop court.\n");
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (txtEmail.getText() == null || txtEmail.getText().trim().isEmpty()) {
            error.append("- Email requis.\n");
        } else if (!Pattern.compile(emailRegex).matcher(txtEmail.getText()).matches()) {
            error.append("- Format d'email invalide (ex: test@mail.com).\n");
        }

        if (txtPhone.getText() != null && !txtPhone.getText().trim().isEmpty()) {
            if (!txtPhone.getText().matches("^[0-9+\\s\\-]*$")) {
                error.append("- Téléphone invalide (chiffres, +, espaces, -).\n");
            }
        }

        if (error.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Erreurs de validation", error.toString());
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
