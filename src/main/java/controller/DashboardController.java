package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.User;
import util.Router;

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

    private void setActive(Button active) {
        for (Button b : new Button[]{
            btnEvents, btnReservations, btnUsers, btnProducts,
            btnCategories, btnPosts, btnComments,
            btnCollaborations, btnPartnerships, btnReferrals
        }) {
            if (b != null) b.getStyleClass().remove("active");
        }
        if (active != null) active.getStyleClass().add("active");
    }
}
