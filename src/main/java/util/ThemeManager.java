package util;

import javafx.scene.Scene;

public class ThemeManager {

    private static boolean darkMode = false;
    private static Scene scene;

    public static void setScene(Scene s) { scene = s; }

    public static void toggle() {
        darkMode = !darkMode;
        apply();
    }

    public static boolean isDark() { return darkMode; }

    public static void apply() {
        if (scene == null) return;
        String dark  = res("/css/front-styles.css");
        String light = res("/css/front-light.css");
        scene.getStylesheets().removeIf(s -> s.equals(dark) || s.equals(light));
        scene.getStylesheets().add(darkMode ? dark : light);
    }

    private static String res(String path) {
        return ThemeManager.class.getResource(path).toExternalForm();
    }
}
