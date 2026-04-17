package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class DashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnEvents;
    @FXML private Button btnReservations;
    @FXML private Button btnUsers;
    @FXML private Button btnProducts;
    @FXML private Button btnCategories;
    @FXML private Button btnPosts;
    @FXML private Button btnComments;

    @FXML
    public void initialize() {
        openEvents();
    }

    @FXML public void openEvents()       { loadView("/fxml/EventList.fxml");       setActive(btnEvents); }
    @FXML public void openReservations() { loadView("/fxml/ReservationList.fxml"); setActive(btnReservations); }
    @FXML public void openUsers()        { loadView("/fxml/UserList.fxml");        setActive(btnUsers); }
    @FXML public void openProducts()     { loadView("/fxml/ProductList.fxml");     setActive(btnProducts); }
    @FXML public void openCategories()   { loadView("/fxml/CategoryList.fxml");    setActive(btnCategories); }
    @FXML public void openPosts()        { loadView("/fxml/PostList.fxml");        setActive(btnPosts); }
    @FXML public void openComments()     { loadView("/fxml/CommentList.fxml");     setActive(btnComments); }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnEvents, btnReservations, btnUsers, btnProducts, btnCategories, btnPosts, btnComments}) {
            b.getStyleClass().remove("active");
        }
        if (!active.getStyleClass().contains("active")) {
            active.getStyleClass().add("active");
        }
    }
}
