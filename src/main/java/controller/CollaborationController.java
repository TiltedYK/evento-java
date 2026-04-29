package controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.converter.DefaultStringConverter;
import model.Collaboration;
import model.User;
import service.CollaborationService;
import service.PdfService;
import service.QRCodeService;
import service.ReferralHitService;
import service.UserService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class CollaborationController implements Initializable {

    @FXML private Label lblPartnerEmail;
    @FXML private TextField fldTitle;
    @FXML private TextField fldType;
    @FXML private TextField fldLinkUrl;
    @FXML private TextField fldStartDate;
    @FXML private TextField fldEndDate;
    @FXML private TextField fldPrice;
    @FXML private Label lblGrade;
    @FXML private Label lblImpressions;

    @FXML private Label lblStatActive;
    @FXML private Label lblStatPending;
    @FXML private Label lblStatTotalBudget;

    @FXML private TableView<Collaboration> tableCollaborations;
    @FXML private TableColumn<Collaboration, String> colTitle;
    @FXML private TableColumn<Collaboration, String> colType;
    @FXML private TableColumn<Collaboration, String> colStatus;
    @FXML private TableColumn<Collaboration, String> colStartDate;
    @FXML private TableColumn<Collaboration, Double> colPrice;

    private final CollaborationService collaborationService = new CollaborationService();
    private final ReferralHitService referralHitService = new ReferralHitService();
    private final UserService userService = new UserService();
    private final PdfService pdfService = new PdfService();
    private final QRCodeService qrCodeService = new QRCodeService();

    private final ObservableList<Collaboration> collaborationsList = FXCollections.observableArrayList();
    private Collaboration selectedCollaboration = null;
    /** Avoid persisting sidebar while programmatically filling fields. */
    private boolean syncingSidebar = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tableCollaborations.setEditable(true);

        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colTitle.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colTitle.setOnEditCommit(e -> {
            Collaboration c = e.getRowValue();
            c.setTitle(e.getNewValue());
            persist(c);
        });

        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colType.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colType.setOnEditCommit(e -> {
            Collaboration c = e.getRowValue();
            c.setType(e.getNewValue());
            persist(c);
        });

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colStatus.setOnEditCommit(e -> {
            Collaboration c = e.getRowValue();
            c.setStatus(e.getNewValue());
            persist(c);
        });

        colStartDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStartDate() == null ? "" : c.getValue().getStartDate().toString()));
        colStartDate.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        colStartDate.setOnEditCommit(e -> {
            Collaboration c = e.getRowValue();
            try {
                String nv = e.getNewValue();
                c.setStartDate(nv == null || nv.isBlank() ? null : LocalDate.parse(nv.trim()));
                persist(c);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Date de début invalide (YYYY-MM-DD).");
            }
        });

        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(
                c.getValue().getPrice() != null ? c.getValue().getPrice() : 0.0));
        colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colPrice.setOnEditCommit(e -> {
            Collaboration c = e.getRowValue();
            Double v = e.getNewValue();
            if (v != null) {
                c.setPrice(v);
                persist(c);
            }
        });

        for (TextField tf : List.of(fldTitle, fldType, fldLinkUrl, fldStartDate, fldEndDate, fldPrice)) {
            tf.focusedProperty().addListener((o, was, now) -> {
                if (Boolean.TRUE.equals(was) && Boolean.FALSE.equals(now)) {
                    persistSidebarIfNeeded();
                }
            });
        }

        loadData();

        tableCollaborations.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) fillForm(newSelection);
        });
    }

    private void persist(Collaboration c) {
        try {
            collaborationService.modifier(c);
            tableCollaborations.refresh();
            recomputeHeaderStats();
            if (selectedCollaboration != null && selectedCollaboration.getId() == c.getId()) {
                fillForm(c);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'enregistrer.");
        }
    }

    private void recomputeHeaderStats() {
        List<Collaboration> list = collaborationsList;
        long active = list.stream().filter(x -> "VALIDATED".equalsIgnoreCase(x.getStatus())).count();
        long pending = list.stream().filter(x -> "PENDING".equalsIgnoreCase(x.getStatus())).count();
        double budget = list.stream().filter(x -> x.getPrice() != null).mapToDouble(Collaboration::getPrice).sum();
        if (lblStatActive != null) lblStatActive.setText(String.valueOf(active));
        if (lblStatPending != null) lblStatPending.setText(String.valueOf(pending));
        if (lblStatTotalBudget != null) lblStatTotalBudget.setText(String.format("%.2f DT", budget));
    }

    private void persistSidebarIfNeeded() {
        if (syncingSidebar || selectedCollaboration == null) return;
        try {
            Collaboration c = selectedCollaboration;
            c.setTitle(nullToEmpty(fldTitle.getText()));
            c.setType(nullToEmpty(fldType.getText()));
            c.setLinkUrl(nullToEmpty(fldLinkUrl.getText()));
            String sd = fldStartDate.getText() == null ? "" : fldStartDate.getText().trim();
            String ed = fldEndDate.getText() == null ? "" : fldEndDate.getText().trim();
            c.setStartDate(sd.isEmpty() ? null : LocalDate.parse(sd));
            c.setEndDate(ed.isEmpty() ? null : LocalDate.parse(ed));
            String p = fldPrice.getText() == null ? "" : fldPrice.getText().trim().replace(",", ".");
            c.setPrice(p.isEmpty() ? null : Double.parseDouble(p));
            collaborationService.modifier(c);
            tableCollaborations.refresh();
            recomputeHeaderStats();
            refreshMetrics(c);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Vérifiez dates (YYYY-MM-DD) et prix numérique.");
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private void loadData() {
        try {
            List<Collaboration> list = collaborationService.recuperer();
            collaborationsList.setAll(list);
            tableCollaborations.setItems(collaborationsList);

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
        syncingSidebar = true;
        try {
            selectedCollaboration = c;
            try {
                User pu = userService.recupererParId(c.getPartnerId());
                lblPartnerEmail.setText(pu != null && pu.getEmail() != null && !pu.getEmail().isBlank()
                        ? pu.getEmail() : "—");
            } catch (Exception e) {
                lblPartnerEmail.setText("—");
            }
            fldTitle.setText(c.getTitle() == null ? "" : c.getTitle());
            fldType.setText(c.getType() == null ? "" : c.getType());
            fldLinkUrl.setText(c.getLinkUrl() == null ? "" : c.getLinkUrl());
            fldStartDate.setText(c.getStartDate() != null ? c.getStartDate().toString() : "");
            fldEndDate.setText(c.getEndDate() != null ? c.getEndDate().toString() : "");
            fldPrice.setText(c.getPrice() != null ? String.format("%.2f", c.getPrice()) : "");
            refreshMetrics(c);
        } finally {
            syncingSidebar = false;
        }
    }

    private void refreshMetrics(Collaboration c) {
        int collabId = c.getId();
        int impressions = referralHitService.getImpressions(collabId);
        String grade = referralHitService.effectivenessGrade(collabId);
        lblImpressions.setText(String.valueOf(impressions));
        lblGrade.setText(grade);
        if (grade.startsWith("A")) lblGrade.setStyle("-fx-font-weight: bold; -fx-text-fill: #22C55E;");
        else if (grade.startsWith("B")) lblGrade.setStyle("-fx-font-weight: bold; -fx-text-fill: #F59E0B;");
        else lblGrade.setStyle("-fx-font-weight: bold; -fx-text-fill: #EF4444;");
    }

    private void clearSelection() {
        selectedCollaboration = null;
        lblPartnerEmail.setText("-");
        fldTitle.clear();
        fldType.clear();
        fldLinkUrl.clear();
        fldStartDate.clear();
        fldEndDate.clear();
        fldPrice.clear();
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
        fileChooser.setInitialFileName("Collab_Report_" +
                (selectedCollaboration.getTitle() != null
                        ? selectedCollaboration.getTitle().replaceAll("[^a-zA-Z0-9_-]", "_")
                        : "export") + ".pdf");
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                int cid = selectedCollaboration.getId();
                int impressions = referralHitService.getImpressions(cid);
                int clicks = referralHitService.getClicks(cid);
                String ctr = referralHitService.effectivenessGrade(cid);

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

        int cid = selectedCollaboration.getId();
        int impressions = referralHitService.getImpressions(cid);
        int clicks = referralHitService.getClicks(cid);
        String ctrGrade = referralHitService.effectivenessGrade(cid);
        double ctrPercent = referralHitService.getCTR(cid);

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
