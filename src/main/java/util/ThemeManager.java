package util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Music-driven theme manager.
 *
 * Each playlist track has its OWN theme (palette + font). When a track changes
 * the theme cross-fades in via an overlay stylesheet that comes AFTER
 * {@code front-styles.css} (which still contains the structural rules).
 *
 * Three themes:
 *   • METAL  — edgy dark (front-styles.css alone, no overlay)
 *   • KPOP   — white & pink   (front-styles.css + front-kpop.css)
 *   • CHILL  — orange chill   (front-styles.css + front-chill.css)
 */
public class ThemeManager {

    public static final String BUILD_TAG = "EVENTO_THEME_BUILD_2026_04_28_MUSIC_DRIVEN_V1";

    public enum Theme {
        METAL ("Metal",  null,                     "/video/metal.mp4", "Linkin Park — In The End"),
        KPOP  ("K-Pop",  "/css/front-kpop.css",    "/video/kpop.mp4",  "(G)I-DLE — Uh-Oh"),
        CHILL ("Chill",  "/css/front-chill.css",   "/video/chill.mp4", "Noor & Selim Arjoun — Streams");

        public final String displayName;
        public final String overlayCss;   // null for METAL (base stylesheet is enough)
        public final String trackPath;
        public final String trackName;

        Theme(String displayName, String overlayCss, String trackPath, String trackName) {
            this.displayName = displayName;
            this.overlayCss  = overlayCss;
            this.trackPath   = trackPath;
            this.trackName   = trackName;
        }
    }

    private static Theme  current = Theme.METAL;
    private static Scene  scene;

    public static Theme  getCurrent() { return current; }
    public static Scene  getScene()   { return scene; }

    /** Bind the current scene so future {@link #apply(Theme)} calls work. */
    public static void setScene(Scene s) {
        scene = s;
    }

    /** Apply the theme to the bound scene immediately (no animation). */
    public static void apply(Theme theme) {
        if (theme == null) theme = Theme.METAL;
        current = theme;
        if (scene == null) return;
        swapStylesheets();
    }

    /**
     * Cross-fade the scene to a new theme. The runnable runs at the moment
     * the stylesheets are swapped (e.g. to flip a label or icon).
     */
    public static void applyWithFade(Theme theme, Runnable atSwap) {
        if (theme == null) theme = Theme.METAL;
        if (scene == null || scene.getRoot() == null) {
            current = theme;
            swapStylesheets();
            if (atSwap != null) atSwap.run();
            return;
        }
        Theme target = theme;
        FadeTransition out = new FadeTransition(Duration.millis(280), scene.getRoot());
        out.setFromValue(scene.getRoot().getOpacity());
        out.setToValue(0.45);
        out.setOnFinished(e -> {
            current = target;
            swapStylesheets();
            if (atSwap != null) atSwap.run();
            // Tiny pause so JavaFX picks up the new CSS before fading back in.
            PauseTransition p = new PauseTransition(Duration.millis(40));
            p.setOnFinished(ev -> {
                FadeTransition in = new FadeTransition(Duration.millis(380), scene.getRoot());
                in.setToValue(1.0);
                in.play();
            });
            p.play();
        });
        out.play();
    }

    /** Random theme — used when the front dashboard first opens. */
    public static Theme randomTheme() {
        Theme[] all = Theme.values();
        return all[ThreadLocalRandom.current().nextInt(all.length)];
    }

    public static Theme randomThemeDifferentFrom(Theme avoid) {
        Theme[] all = Theme.values();
        if (all.length <= 1 || avoid == null) return randomTheme();
        Theme pick;
        int guard = 0;
        do {
            pick = all[ThreadLocalRandom.current().nextInt(all.length)];
            guard++;
        } while (pick == avoid && guard < 16);
        return pick;
    }

    private static void swapStylesheets() {
        if (scene == null) return;
        // Drop any of OUR overlays (and the legacy front-light.css from the login screen).
        scene.getStylesheets().removeIf(s ->
                s.contains("front-kpop.css")
             || s.contains("front-chill.css")
             || s.contains("front-light.css"));

        // Always make sure base front-styles.css is present.
        String base = res("/css/front-styles.css");
        if (!scene.getStylesheets().contains(base)) {
            scene.getStylesheets().add(base);
        }

        // Add the matching overlay (METAL has no overlay — base stylesheet is the metal look).
        if (current.overlayCss != null) {
            String overlay = res(current.overlayCss);
            if (overlay != null) scene.getStylesheets().add(overlay);
        }
    }

    private static String res(String path) {
        java.net.URL u = ThemeManager.class.getResource(path);
        return u == null ? null : u.toExternalForm();
    }

    // ── Legacy shims (so old call-sites still compile) ─────────────────────

    /** @deprecated Themes are now music-driven. Always returns METAL == "dark". */
    @Deprecated public static boolean isDark() { return current == Theme.METAL; }

    /** @deprecated The dark/light toggle was replaced by music-driven themes. */
    @Deprecated public static void toggle() {
        // Cycle to the next theme — used by old toggle buttons that survive in FXML.
        Theme[] all = Theme.values();
        int next = (current.ordinal() + 1) % all.length;
        applyWithFade(all[next], null);
    }

    /** @deprecated Kept for old code paths. */
    @Deprecated public static void apply() { swapStylesheets(); }
}
