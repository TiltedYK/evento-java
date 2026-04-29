package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import model.Collaboration;
import model.ReferralHit;
import model.User;
import service.CollaborationService;
import service.ReferralHitService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FrontInfluencerHubController implements Initializable {

    @FXML private TableView<Collaboration> tableCampaigns;
    @FXML private TableColumn<Collaboration, String> colTitle;
    @FXML private TableColumn<Collaboration, String> colType;
    @FXML private TableColumn<Collaboration, Double> colPrice;

    @FXML private Label lblTotalClicks;
    @FXML private Label lblConversions;

    @FXML private TableView<ReferralHit> tableMyHits;
    @FXML private TableColumn<ReferralHit, LocalDateTime> colHitDate;
    @FXML private TableColumn<ReferralHit, String> colHitSession;

    private User currentUser;
    private final CollaborationService collaborationService = new CollaborationService();
    private final ReferralHitService referralHitService = new ReferralHitService();

    private ObservableList<Collaboration> activeCampaigns = FXCollections.observableArrayList();
    private ObservableList<ReferralHit> myHits = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        colHitDate.setCellValueFactory(new PropertyValueFactory<>("visitedAt"));
        colHitSession.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void loadData() {
        if (currentUser == null) return;
        try {
            // Load Active Campaigns
            List<Collaboration> allCollabs = collaborationService.recuperer();
            List<Collaboration> activeCollabs = allCollabs.stream()
                .filter(c -> "VALIDATED".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());
            activeCampaigns.setAll(activeCollabs);
            tableCampaigns.setItems(activeCampaigns);

            // Load Influencer's Hits
            List<ReferralHit> allHits = referralHitService.recuperer();
            List<ReferralHit> myHitList = allHits.stream()
                .filter(h -> h.getInfluencerId() == currentUser.getId())
                .collect(Collectors.toList());
            myHits.setAll(myHitList);
            tableMyHits.setItems(myHits);

            // Update stats
            lblTotalClicks.setText(String.valueOf(myHitList.size()));
            long conversions = myHitList.stream().filter(h -> h.getReferredUserId() != null && h.getReferredUserId() > 0).count();
            lblConversions.setText(String.valueOf(conversions));

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load data.");
        }
    }

    @FXML
    void handleGetLink(ActionEvent event) {
        Collaboration selected = tableCampaigns.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a campaign to promote.");
            return;
        }

        if (currentUser == null) return;

        // Ensure the campaign has a referral code
        String collabCode = selected.getReferralCode();
        if (collabCode == null || collabCode.isEmpty()) {
            collabCode = "C" + selected.getId(); // Fallback code
        }

        // The URL pattern allows the system to track both the campaign AND the influencer
        String trackingLink = String.format("https://evento.tn/promo?ref=%s&inf=%d", collabCode, currentUser.getId());

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(trackingLink);
        clipboard.setContent(content);

        showAlert(Alert.AlertType.INFORMATION, "Link Copied!", "Your unique tracking link has been copied to your clipboard:\n\n" + trackingLink);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
