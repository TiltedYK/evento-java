package util;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class UIUtil {

    public static void fadeIn(StackPane container, Node newView) {
        newView.setOpacity(0);
        newView.setTranslateY(12);
        container.getChildren().setAll(newView);

        FadeTransition fade = new FadeTransition(Duration.millis(280), newView);
        fade.setFromValue(0); fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(280), newView);
        slide.setFromY(12); slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    public static void toast(StackPane root, String message, ToastType type) {
        Label l = new Label(message);
        l.getStyleClass().add("toast-label");

        HBox box = new HBox(l);
        box.getStyleClass().addAll("toast", type.styleClass);
        box.setMaxWidth(Region.USE_PREF_SIZE);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.setAlignment(Pos.CENTER);

        StackPane.setAlignment(box, Pos.TOP_RIGHT);
        StackPane.setMargin(box, new Insets(22, 26, 0, 0));

        box.setOpacity(0); box.setTranslateY(-14);
        root.getChildren().add(box);

        FadeTransition in = new FadeTransition(Duration.millis(220), box);
        in.setFromValue(0); in.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), box);
        slideIn.setFromY(-14); slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        PauseTransition hold = new PauseTransition(Duration.millis(2200));

        FadeTransition out = new FadeTransition(Duration.millis(260), box);
        out.setFromValue(1); out.setToValue(0);
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(260), box);
        slideOut.setFromY(0); slideOut.setToY(-14);

        SequentialTransition seq = new SequentialTransition(
                new ParallelTransition(in, slideIn),
                hold,
                new ParallelTransition(out, slideOut)
        );
        seq.setOnFinished(e -> root.getChildren().remove(box));
        seq.play();
    }

    public enum ToastType {
        SUCCESS("toast-success"),
        ERROR("toast-error"),
        INFO("toast");
        final String styleClass;
        ToastType(String s) { this.styleClass = s; }
    }
}
