package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import model.PartnershipRequest;
import model.User;
import service.PartnershipRequestService;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class FrontPartnershipApplyController {

    @FXML private TextField txtCompany;
    @FXML private TextField txtContact;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextArea txtMessage;
    @FXML private Label lblStatus;

    private User currentUser;
    private final PartnershipRequestService requestService = new PartnershipRequestService();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            // Auto-fill available user information
            txtContact.setText(user.getPrenom() + " " + user.getNom());
            txtEmail.setText(user.getEmail());
            // If the user already has a pending or accepted request, maybe notify them
        }
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        lblStatus.setText("");
        if (!validateInput()) {
            return;
        }

        try {
            String company = txtCompany.getText().trim();
            String contact = txtContact.getText().trim();
            String email = txtEmail.getText().trim();
            String phone = txtPhone.getText().trim();
            String message = txtMessage.getText().trim();

            PartnershipRequest pr = new PartnershipRequest(contact, email, company, phone, message);
            if (currentUser != null) {
                pr.setUserId(currentUser.getId());
            }

            requestService.ajouter(pr);

            lblStatus.setText("Demande envoyée avec succès ! Notre équipe l'étudiera prochainement.");
            lblStatus.setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Votre demande de partenariat a été envoyée avec succès !");
            alert.showAndWait();
            
            // Clear fields
            txtCompany.clear();
            txtMessage.clear();
            if (currentUser == null) {
                txtContact.clear();
                txtEmail.clear();
            }
            txtPhone.clear();

        } catch (SQLException e) {
            e.printStackTrace();
            lblStatus.setText("Une erreur est survenue lors de l'envoi.");
            lblStatus.setStyle("-fx-text-fill: #E8320A; -fx-font-weight: bold;");
        }
    }

    private boolean validateInput() {
        StringBuilder error = new StringBuilder();

        if (txtCompany.getText() == null || txtCompany.getText().trim().isEmpty()) {
            error.append("- Company name is required.\n");
        }
        
        if (txtContact.getText() == null || txtContact.getText().trim().isEmpty()) {
            error.append("- Contact name is required.\n");
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (txtEmail.getText() == null || txtEmail.getText().trim().isEmpty()) {
            error.append("- Email is required.\n");
        } else if (!Pattern.compile(emailRegex).matcher(txtEmail.getText()).matches()) {
            error.append("- Invalid email format.\n");
        }

        if (error.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Please correct the following errors:");
            alert.setContentText(error.toString());
            alert.showAndWait();
            return false;
        }
        return true;
    }
}
