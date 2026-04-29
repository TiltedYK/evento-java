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
import service.LinkPreviewService;
import service.PdfService;
import service.QRCodeService;
import service.ReferralHitService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class CollaborationController implements Initializable {

    // Read-only labels
    @FXML private Label lblPartnerId;
    @FXML private Label lblTitle;
    @FXML private Label lblType;
    @FXML private Label lblLinkUrl;
    @FXML private Label lblStartDate;
    @FXML private Label lblEndDate;
    @FXML private Label lblPrice;
    @FXML private Label lblGrade;
    @FXML private Label lblImpressions;

    @FXML private Label lblStatActive;
    @FXML private Label lblStatPending;
    @FXML private Label lblStatTotalBudget;

    @FXML private TableView<Collaboration> tableCollaborations;
    @FXML private TableColumn<Collaboration, Integer> colId;
    @FXML private TableColumn<Collaboration, String> colTitle;
    @FXML private TableColumn<Collaboration, String> colType;
    @FXML private TableColumn<Collaboration, String> colStatus;
    @FXML private TableColumn<Collaboration, LocalDate> colStartDate;
    @FXML private TableColumn<Collaboration, Double> colPrice;

    private CollaborationService collaborationService = new CollaborationService();
    private ReferralHitService referralHitService = new ReferralHitService();
    private PdfService pdfService = new PdfService();
    private QRCodeService qrCodeService = new QRCodeService();

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

        loadData();

        tableCollaborations.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillForm(newSelection);
            }
        });
    }

    private void loadData() {
        try {
            List<Collaboration> list = collaborationService.recuperer();
            collaborationsList.setAll(list);
            tableCollaborations.setItems(collaborationsList);

            // Update stats
            long active = list.stream().filter(c -> "VALIDATED".equalsIgnoreCase(c.getStatus())).count();
            long pending = list.stream().filter(c -> "PENDING".equalsIgnoreCase(c.getStatus())).count();
            double budget = list.stream().filter(c -> c.getPrice() != null).mapToDouble(Collaboration::getPrice).sum();

            if (lblStatActive != null) lblStatActive.setText(String.valueOf(active));
            if (lblStatPending != null) lblStatPending.setText(String.valueOf(pending));
            if (lblStatTotalBudget != null) lblStatTotalBudget.setText(String.format("%.2f DT", budget));

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Base de données", "Impossible de charger les données.");
        }
    }

    private void fillForm(Collaboration c) {
        selectedCollaboration = c;
        lblPartnerId.setText(String.valueOf(c.getPartnerId()));
        lblTitle.setText(c.getTitle());
        lblType.setText(c.getType());
        lblLinkUrl.setText(c.getLinkUrl());
        lblStartDate.setText(c.getStartDate() != null ? c.getStartDate().toString() : "");
        lblEndDate.setText(c.getEndDate() != null ? c.getEndDate().toString() : "");
        lblPrice.setText(c.getPrice() != null ? String.valueOf(c.getPrice()) : "");

        // Advanced metrics
        int collabId = c.getId();
        int impressions = referralHitService.getImpressions(collabId);
        String grade = referralHitService.effectivenessGrade(collabId);
        lblImpressions.setText(String.valueOf(impressions));
        lblGrade.setText(grade);
        
        // Color coding grade
        if (grade.startsWith("A")) lblGrade.setStyle("-fx-font-weight: bold; -fx-text-fill: #22C55E;");
        else if (grade.startsWith("B")) lblGrade.setStyle("-fx-font-weight: bold; -fx-text-fill: #F59E0B;");
        else lblGrade.setStyle("-fx-font-weight: bold; -fx-text-fill: #EF4444;");
    }

    private void clearSelection() {
        selectedCollaboration = null;
        lblPartnerId.setText("-");
        lblTitle.setText("-");
        lblLinkUrl.setText("-");
        lblPrice.setText("-");
        lblStartDate.setText("-");
        lblEndDate.setText("-");
        lblType.setText("-");
        lblGrade.setText("-");
        lblImpressions.setText("-");
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
                    clearSelection();
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
    void handleValidate(ActionEvent event) {
        if (selectedCollaboration == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une collaboration.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selectedCollaboration.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Cette collaboration a déjà été traitée.");
            return;
        }

        try {
            selectedCollaboration.setStatus("VALIDATED");
            collaborationService.modifier(selectedCollaboration);
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Collaboration validée et prête à être affichée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la validation.");
        }
    }

    @FXML
    void handleReject(ActionEvent event) {
        if (selectedCollaboration == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une collaboration.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selectedCollaboration.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Cette collaboration a déjà été traitée.");
            return;
        }

        try {
            selectedCollaboration.setStatus("REJECTED");
            collaborationService.modifier(selectedCollaboration);
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Collaboration refusée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du refus.");
        }
    }

    // ========== ADVANCED BUSINESS FUNCTIONALITIES ==========



    @FXML
    void handleGenerateQR(ActionEvent event) {
        if (selectedCollaboration == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une collaboration.");
            return;
        }
        String content = selectedCollaboration.getLinkUrl() != null && !selectedCollaboration.getLinkUrl().isEmpty() 
                            ? selectedCollaboration.getLinkUrl() : "https://evento.tn/collab/" + selectedCollaboration.getId();
        try {
            Image qrImage = qrCodeService.generateQRCode(content, 200);
            ImageView imageView = new ImageView(qrImage);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("QR Code");
            alert.setHeaderText("QR Code pour : " + selectedCollaboration.getTitle());
            alert.setGraphic(imageView);
            alert.setContentText("Vous pouvez utiliser ce QR code pour vos campagnes.");
            alert.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de générer le QR Code.");
        }
    }

    @FXML
    void handleExportPDF(ActionEvent event) {
        if (selectedCollaboration == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une collaboration.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Collab_Report_" + selectedCollaboration.getId() + ".pdf");
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                int colId = selectedCollaboration.getId();
                int impressions = referralHitService.getImpressions(colId);
                int clicks = referralHitService.getClicks(colId);
                String ctr = referralHitService.effectivenessGrade(colId);

                pdfService.generateCollaborationReport(selectedCollaboration, impressions, clicks, ctr, file);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Rapport PDF généré avec succès !");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la création du PDF.");
            }
        }
    }

    @FXML
    void handleViewStats(ActionEvent event) {
        if (selectedCollaboration == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Sélectionnez une collaboration.");
            return;
        }
        
        int colId = selectedCollaboration.getId();
        int impressions = referralHitService.getImpressions(colId);
        int clicks = referralHitService.getClicks(colId);
        String ctrGrade = referralHitService.effectivenessGrade(colId);
        double ctrPercent = referralHitService.getCTR(colId);

        String stats = String.format(
            "Statistiques pour: %s\n\n- Impressions : %d\n- Clics : %d\n- Taux de Clic (CTR) : %.2f%%\n- Note de Performance : %s",
            selectedCollaboration.getTitle(), impressions, clicks, ctrPercent, ctrGrade
        );

        showAlert(Alert.AlertType.INFORMATION, "Tableau de Bord Analytique", stats);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
