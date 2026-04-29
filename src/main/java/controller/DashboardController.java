package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.User;
import util.Router;
import util.ThemeManager;

public class DashboardController {
    @FXML private Button btnCollaborations;
    @FXML private Button btnPartnerships;
    @FXML private Button btnReferrals;
    @FXML private StackPane contentArea;
    @FXML private Button btnEvents;
    @FXML private Button btnReservations;
    @FXML private Button btnUsers;
    @FXML private Button btnProducts;
    @FXML private Button btnCategories;
    @FXML private Button btnPosts;
    @FXML private Button btnComments;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    private User currentUser;

    @FXML
    public void initialize() {
        Router.setContentArea(contentArea);
        openEvents();
    }

    /** Called by LoginController after successful admin login. */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML public void openEvents()       { Router.navigate("/fxml/EventList.fxml");       setActive(btnEvents); }
    @FXML public void openReservations() { Router.navigate("/fxml/ReservationList.fxml"); setActive(btnReservations); }
    @FXML public void openUsers()        { Router.navigate("/fxml/UserList.fxml");        setActive(btnUsers); }
    @FXML public void openProducts()     { Router.navigate("/fxml/ProductList.fxml");     setActive(btnProducts); }
    @FXML public void openCategories()   { Router.navigate("/fxml/CategoryList.fxml");    setActive(btnCategories); }
    @FXML public void openPosts()        { Router.navigate("/fxml/PostList.fxml");        setActive(btnPosts); }
    @FXML public void openComments()     { Router.navigate("/fxml/CommentList.fxml");     setActive(btnComments); }
    @FXML public void openCollaborations(){ Router.navigate("/fxml/CollaborationView.fxml"); setActive(btnCollaborations); }
    @FXML public void openPartnerships() { Router.navigate("/fxml/PartnershipRequestView.fxml"); setActive(btnPartnerships); }
    @FXML public void openReferrals()    { Router.navigate("/fxml/ReferralHitView.fxml"); setActive(btnReferrals); }
    @FXML public void openSettings()     { Router.navigate("/fxml/AdminSettings.fxml");   setActive(btnSettings); }

    private void setActive(Button active) {
        for (Button b : new Button[]{
            btnEvents, btnReservations, btnUsers, btnProducts,
            btnCategories, btnPosts, btnComments,
            btnCollaborations, btnPartnerships, btnReferrals, btnSettings
        }) {
            if (b != null) b.getStyleClass().remove("active");
        }
        if (active != null) active.getStyleClass().add("active");
    }

    /**
     * Logs the current admin out and returns to the login screen.
     * No popup confirmation — the user clicked the dedicated logout button so
     * intent is clear, and per product spec we don't open extra windows.
     */
    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            LoginController loginCtl = loader.getController();
            Scene scene = new Scene(root, 1240, 760);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            ThemeManager.setScene(scene);
            ThemeManager.apply(ThemeManager.randomTheme());
            loginCtl.attachLoginAmbience(scene);

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("EVENTO — Sign In");
            stage.setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
