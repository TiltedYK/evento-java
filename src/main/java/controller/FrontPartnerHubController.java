package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Collaboration;
import model.User;
import service.CollaborationService;
import service.ReferralHitService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FrontPartnerHubController implements Initializable {

    @FXML private TableView<Collaboration> tableCollaborations;
    @FXML private TableColumn<Collaboration, String> colTitle;
    @FXML private TableColumn<Collaboration, String> colType;
    @FXML private TableColumn<Collaboration, String> colStatus;
    @FXML private TableColumn<Collaboration, LocalDate> colStartDate;
    @FXML private TableColumn<Collaboration, Double> colPrice;

    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField txtLinkUrl;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Label lblEstimatedPrice;
    @FXML private Button btnChooseFile;
    @FXML private Label lblFileName;

    // Insights UI
    @FXML private VBox paneInsights;
    @FXML private javafx.scene.chart.BarChart<String, Number> chartDailyHits;
    @FXML private javafx.scene.chart.PieChart chartDistribution;
    @FXML private Label lblImpressions;
    @FXML private Label lblClicks;
    @FXML private Label lblCTR;
    @FXML private Label lblGrade;

    private User currentUser;
    private final CollaborationService collaborationService = new CollaborationService();
    private final ReferralHitService referralHitService = new ReferralHitService();
    private final service.PdfService pdfService = new service.PdfService();
    private ObservableList<Collaboration> collaborationsList = FXCollections.observableArrayList();
    private Double currentEstimatedPrice = null;
    private File selectedFile = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        cmbType.setItems(FXCollections.observableArrayList("image", "video", "affiche"));
        cmbType.getSelectionModel().selectFirst();

        // Real-time price listeners
        cmbType.valueProperty().addListener((obs, oldVal, newVal) -> updateAutoEstimate());
        dpStartDate.valueProperty().addListener((obs, oldVal, newVal) -> updateAutoEstimate());
        dpEndDate.valueProperty().addListener((obs, oldVal, newVal) -> updateAutoEstimate());
        
        tableCollaborations.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal == null) {
                paneInsights.setVisible(false);
                paneInsights.setManaged(false);
            }
        });
    }

    private void updateAutoEstimate() {
        if (dpStartDate.getValue() != null && dpEndDate.getValue() != null) {
            handleEstimatePrice(null);
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void loadData() {
        if (currentUser == null) return;
        try {
            List<Collaboration> all = collaborationService.recuperer();
            List<Collaboration> myCollabs = all.stream()
                .filter(c -> c.getPartnerId() == currentUser.getId())
                .collect(Collectors.toList());
            collaborationsList.setAll(myCollabs);
            tableCollaborations.setItems(collaborationsList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load your collaborations.");
        }
    }

    @FXML
    void handleEstimatePrice(ActionEvent event) {
        String type = cmbType.getValue();
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        if (start == null || end == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select start and end dates to estimate cost.");
            return;
        }
        if (end.isBefore(start)) {
            showAlert(Alert.AlertType.WARNING, "Warning", "End date cannot be before start date.");
            return;
        }

        currentEstimatedPrice = collaborationService.calculateEstimatedPrice(type, "top", start, end);
        lblEstimatedPrice.setText(String.format("Estimate: %.2f TND", currentEstimatedPrice));
        lblEstimatedPrice.setStyle("-fx-text-fill: #C8FF00; -fx-font-weight: bold;");
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in as a Partner.");
            return;
        }

        if (!validateInput()) return;
        
        if (currentEstimatedPrice == null) {
            // Auto estimate if they didn't click the button
            handleEstimatePrice(null);
            if (currentEstimatedPrice == null) return; 
        }

        try {
            String title = txtTitle.getText().trim();
            String type = cmbType.getValue();
            String linkUrl = txtLinkUrl.getText().trim();
            LocalDate start = dpStartDate.getValue();
            LocalDate end = dpEndDate.getValue();
            String fileName = "";

            if (selectedFile != null) {
                fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                File uploadsDir = new File("uploads");
                if (!uploadsDir.exists()) uploadsDir.mkdir();
                
                java.nio.file.Files.copy(selectedFile.toPath(), 
                    java.nio.file.Paths.get("uploads/" + fileName), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            Collaboration c = new Collaboration(currentUser.getId(), title, type, fileName, linkUrl, "top", start, end);
            c.setStatus("PENDING");
            c.setPrice(currentEstimatedPrice);
            
            collaborationService.ajouter(c);
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Campaign proposal submitted successfully!");
            loadData();
            handleClear(null);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Submission Error", "Error: " + e.getMessage());
        }
    }

    @FXML
    void handleClear(ActionEvent event) {
        txtTitle.clear();
        txtLinkUrl.clear();
        dpStartDate.setValue(null);
        dpEndDate.setValue(null);
        cmbType.getSelectionModel().selectFirst();
        lblEstimatedPrice.setText("");
        currentEstimatedPrice = null;
        selectedFile = null;
        lblFileName.setText("No file selected");
    }

    @FXML
    void handleChooseFile(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Media");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Media Files", "*.png", "*.jpg", "*.mp4", "*.pdf", "*.jpeg")
        );
        selectedFile = fileChooser.showOpenDialog(btnChooseFile.getScene().getWindow());
        if (selectedFile != null) {
            lblFileName.setText(selectedFile.getName());
            lblFileName.setStyle("-fx-text-fill: #22C55E;");
        }
    }

    @FXML
    void handleViewStats(ActionEvent event) {
        Collaboration selected = tableCollaborations.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a campaign from the table to view its performance.");
            return;
        }
        
        if (!"VALIDATED".equalsIgnoreCase(selected.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Campaign Pending", "Detailed analytics are available once the campaign is VALIDATED and active.");
            return;
        }

        showAnalytics(selected);
    }

    private void showAnalytics(Collaboration selected) {
        int colId = selected.getId();
        
        // 1. Data Retrieval
        int impressions = referralHitService.getImpressions(colId);
        int clicks = referralHitService.getClicks(colId);
        double ctrPercent = referralHitService.getCTR(colId);
        String grade = referralHitService.effectivenessGrade(colId);
        
        // 2. Update Labels
        lblImpressions.setText(String.valueOf(impressions));
        lblClicks.setText(String.valueOf(clicks));
        lblCTR.setText(String.format("%.2f%%", ctrPercent));
        lblGrade.setText(grade);
        
        // 3. Update Bar Chart (Daily Hits)
        chartDailyHits.getData().clear();
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Hits");
        
        java.util.Map<java.time.LocalDate, Long> dailyData = referralHitService.getDailyHitsForCollaboration(colId);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
        
        dailyData.forEach((date, count) -> series.getData().add(new javafx.scene.chart.XYChart.Data<>(date.format(fmt), count)));
        chartDailyHits.getData().add(series);
        
        // 4. Update Pie Chart (Distribution)
        chartDistribution.getData().clear();
        java.util.Map<String, Long> dist = referralHitService.getHitDistribution(colId);
        dist.forEach((type, count) -> {
            chartDistribution.getData().add(new javafx.scene.chart.PieChart.Data(type.toUpperCase() + " ("+count+")", count));
        });

        // 5. Animation and Visibility
        paneInsights.setManaged(true);
        paneInsights.setVisible(true);
        
        // Simple fade-in effect
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), paneInsights);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    @FXML
    void handleDownloadReport(ActionEvent event) {
        Collaboration selected = tableCollaborations.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Performance Report");
        fileChooser.setInitialFileName("Report_" + selected.getTitle().replaceAll("\\s+", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        java.io.File file = fileChooser.showSaveDialog(paneInsights.getScene().getWindow());
        if (file != null) {
            try {
                int impressions = referralHitService.getImpressions(selected.getId());
                int clicks = referralHitService.getClicks(selected.getId());
                String grade = referralHitService.effectivenessGrade(selected.getId());
                
                pdfService.generateCollaborationReport(selected, impressions, clicks, grade, file);
                
                showAlert(Alert.AlertType.INFORMATION, "Report Generated", "The campaign report has been saved successfully to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to generate PDF: " + e.getMessage());
            }
        }
    }
    
    @FXML
    void handleGetLink(ActionEvent event) {
        Collaboration selected = tableCollaborations.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a campaign from the table.");
            return;
        }
        
        if (selected.getReferralCode() == null || selected.getReferralCode().isEmpty()) {
            // Auto-generate if missing for existing campaigns
            String newCode = "COL-" + (int)(Math.random() * 9000 + 1000);
            selected.setReferralCode(newCode);
            try {
                collaborationService.modifier(selected);
            } catch (SQLException e) { e.printStackTrace(); }
        }

        showAlert(Alert.AlertType.INFORMATION, "Tracking Link", 
            "Use this link to track your campaign performance:\n\n" + 
            "https://evento.tn/promo?ref=" + selected.getReferralCode());
    }

    private boolean validateInput() {
        if (txtTitle.getText() == null || txtTitle.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Campaign title is required.");
            return false;
        }
        if (dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Start and end dates are required.");
            return false;
        }
        if (txtLinkUrl.getText() != null && !txtLinkUrl.getText().isEmpty()) {
            if (!txtLinkUrl.getText().matches("^(http|https)://.*$")) {
                showAlert(Alert.AlertType.WARNING, "Validation", "URL must start with http:// or https://");
                return false;
            }
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
