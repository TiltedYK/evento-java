package util;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class OverlayService {

    private static StackPane root;
    private static final String ID = "evento-overlay";

    public static void init(StackPane r) { root = r; }

    public static void show(String title, Node content) {
        if (root == null) return;
        hide();

        Label titleLbl = new Label(title.toUpperCase());
        titleLbl.getStyleClass().add("overlay-title");

        Button closeX = new Button("✕");
        closeX.getStyleClass().add("overlay-x-btn");
        closeX.setOnAction(e -> hide());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, titleLbl, spacer, closeX);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("overlay-header");

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(520);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;-fx-border-color:transparent;");
        scroll.getStyleClass().add("front-scroll");

        VBox panel = new VBox(0, header, scroll);
        panel.getStyleClass().add("overlay-panel");
        panel.setMaxWidth(560);
        panel.setPrefWidth(560);

        StackPane dim = new StackPane(panel);
        dim.setId(ID);
        dim.setStyle("-fx-background-color: rgba(0,0,0,0.72);");
        dim.setOnMouseClicked(e -> { if (e.getTarget() == dim) hide(); });

        root.getChildren().add(dim);
        dim.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), dim);
        ft.setToValue(1);
        ft.play();
    }

    /**
     * Larger centered panel (e.g. music-news browser). Clicking the dimmed backdrop
     * (not the panel) dismisses the overlay, same as {@link #show}.
     */
    public static void showPanel(String title, Node content, double maxWidth, double maxHeight) {
        if (root == null) return;
        hide();

        Label titleLbl = new Label(title.toUpperCase());
        titleLbl.getStyleClass().add("overlay-title");

        Button closeX = new Button("✕");
        closeX.getStyleClass().add("overlay-x-btn");
        closeX.setOnAction(e -> hide());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, titleLbl, spacer, closeX);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("overlay-header");

        VBox panel = new VBox(0, header, content);
        panel.getStyleClass().addAll("overlay-panel", "overlay-panel-large");
        panel.setMaxWidth(maxWidth);
        panel.setMaxHeight(maxHeight);

        StackPane dim = new StackPane(panel);
        dim.setId(ID);
        // Softer “frosted glass” dim — reads closer to a blur without native backdrop-filter
        dim.setStyle("-fx-background-color: rgba(8,10,18,0.62);");
        dim.setOnMouseClicked(e -> { if (e.getTarget() == dim) hide(); });

        root.getChildren().add(dim);
        dim.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), dim);
        ft.setToValue(1);
        ft.play();
    }

    public static void hide() {
        if (root != null)
            root.getChildren().removeIf(n -> ID.equals(n.getId()));
    }
}
