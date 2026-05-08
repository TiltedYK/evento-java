package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

/**
 * Global router so list controllers can replace the content area
 * (open forms "in place" instead of popping new windows).
 */
public class Router {
    private static StackPane contentArea;

    public static void setContentArea(StackPane pane) {
        contentArea = pane;
    }

    public static StackPane getContentArea() { return contentArea; }

    /** Load fxml into the content area with a fade animation. */
    public static <T> T navigate(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Router.class.getResource(fxmlPath));
            Parent view = loader.load();
            UIUtil.fadeIn(contentArea, view);
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
