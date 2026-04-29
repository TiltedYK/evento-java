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
import service.ReferralHitService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @FXML private Label lblStatTotal;
    @FXML private Label lblStatPending;
    @FXML private Label lblStatToday;

    // Form fields (now read-only Labels)
    @FXML private Label lblCompany;
    @FXML private Label lblContact;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;
    @FXML private Label lblMessage;

    private PartnershipRequestService requestService = new PartnershipRequestService();
    private CollaborationService collaborationService = new CollaborationService();
    private ReferralHitService referralHitService = new ReferralHitService();
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
            List<PartnershipRequest> list = requestService.recuperer();
            requestsList.setAll(list);
            tableRequests.setItems(requestsList);

            // Update stats
            long total = list.size();
            long pending = list.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
            long today = list.stream().filter(r -> r.getCreatedAt() != null \u0026\u0026 r.getCreatedAt().toLocalDate().equals(LocalDate.now())).count();

            if (lblStatTotal != null) lblStatTotal.setText(String.valueOf(total));
            if (lblStatPending != null) lblStatPending.setText(String.valueOf(pending));
            if (lblStatToday != null) lblStatToday.setText(String.valueOf(today));

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de charger les requêtes.");
        }
    }

    private void fillForm(PartnershipRequest request) {
        selectedRequest = request;
        lblCompany.setText(request.getCompanyName());
        lblContact.setText(request.getContactName());
        lblEmail.setText(request.getEmail());
        lblPhone.setText(request.getPhone());
        lblMessage.setText(request.getMessage());
    }

    private void clearSelection() {
        selectedRequest = null;
        lblCompany.setText("-");
        lblContact.setText("-");
        lblEmail.setText("-");
        lblPhone.setText("-");
        lblMessage.setText("-");
        tableRequests.getSelectionModel().clearSelection();
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
                clearSelection();
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

            // Upgrade user role to ROLE_PARTNER
            try {
                service.UserService userService = new service.UserService();
                model.User u = userService.recupererParId(partnerId);
                if (u != null) {
                    String roles = u.getRoles();
                    if (roles == null) roles = "[\"ROLE_USER\"]";
                    if (!roles.contains("ROLE_PARTNER")) {
                        if (roles.equals("[]")) {
                            roles = "[\"ROLE_PARTNER\"]";
                        } else {
                            roles = roles.replace("]", ",\"ROLE_PARTNER\"]");
                        }
                        u.setRoles(roles);
                        userService.modifier(u);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not update user role: " + ex.getMessage());
            }

            LocalDate start = LocalDate.now();
            LocalDate end = LocalDate.now().plusMonths(1);
            
            // Generate a unique referral code for the partner
            String referralCode = referralHitService.generateReferralCode(partnerId);

            // Calculate estimated price automatically
            double estimatedPrice = collaborationService.calculateEstimatedPrice("image", "top", start, end);

            Collaboration newCollab = new Collaboration(
                    partnerId, 
                    "Collab : " + selectedRequest.getCompanyName(), 
                    "image", 
                    "", 
                    referralCode, // Use referral code as link URL or append to base URL
                    "top", 
                    start, 
                    end
            );
            newCollab.setStatus("PENDING");
            newCollab.setPrice(estimatedPrice);
            newCollab.setReferralCode(referralCode);
            
            collaborationService.ajouter(newCollab);
            
            loadData();
            
            String successMsg = String.format("La demande a été acceptée !\n\n- Collaboration ajoutée.\n- Code Partenaire : %s\n- Prix Estimé : %.2f TND", referralCode, estimatedPrice);
            showAlert(Alert.AlertType.INFORMATION, "Collaboration Créée", successMsg);
            
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

    // validateInput method removed since admin only validates

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
