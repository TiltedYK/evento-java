package service;

import model.User;

import java.io.File;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Resolves the right avatar URL for a user, with a fallback chain:
 *
 *   1. {@code user.image} is an absolute URL (starts with http/https) → use directly
 *      (covers Google profile pictures returned by GoogleAuthService)
 *   2. {@code user.image} is a filename matching a file in the project's
 *      {@code uploads/} directory → return its file:// URL
 *   3. Otherwise → fall back to a Gravatar URL derived from the email's MD5
 *      (Gravatar serves an "identicon" if the user never registered there,
 *      so we always get a deterministic visual identity)
 *
 * Used everywhere the app needs to show a profile picture (admin user
 * form, front profile tab, Google-signed-up users, etc.) so the rules stay
 * consistent.
 */
public class GravatarService {

    private static final String BASE = "https://www.gravatar.com/avatar/";

    /** Direct Gravatar URL — useful when you know you only want Gravatar. */
    public static String getAvatarUrl(String email, int sizePx) {
        String hash = md5(email != null ? email.trim().toLowerCase() : "");
        return BASE + hash + "?s=" + sizePx + "&d=identicon&r=g";
    }

    /**
     * Resolves the best avatar URL for a user, applying the fallback chain
     * documented at the class level.
     *
     * @param user      the user (may be {@code null}, in which case Gravatar
     *                  is returned with an empty email hash)
     * @param sizePx    requested avatar size in pixels (only used for Gravatar)
     * @return a string usable as the {@code url} arg of
     *         {@link javafx.scene.image.Image#Image(String)}
     */
    public static String resolveAvatarUrl(User user, int sizePx) {
        if (user == null) return getAvatarUrl("", sizePx);

        String img = user.getImage();
        if (img != null && !img.isBlank()) {
            // (1) absolute URL — Google profile picture, etc.
            String trimmed = img.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed;
            }
            // (2) local upload
            File f = new File("uploads", trimmed);
            if (f.isFile()) {
                return f.toURI().toString();
            }
        }
        // (3) Gravatar fallback (auto-generates an identicon if no avatar set)
        return getAvatarUrl(user.getEmail(), sizePx);
    }

    /**
     * Returns a {@code file://} URL only when {@code user.image} is a filename that
     * exists under {@code uploads/}. No HTTP URLs and no Gravatar — used for
     * comment avatars so only real uploaded files appear as photos.
     */
    public static String resolveLocalUploadAvatarUrl(User user) {
        if (user == null) return null;
        String img = user.getImage();
        if (img == null || img.isBlank()) return null;
        String t = img.trim();
        if (t.startsWith("http://") || t.startsWith("https://")) return null;
        File f = new File("uploads", t);
        return f.isFile() ? f.toURI().toString() : null;
    }

    /**
     * Returns a short human-readable description of which fallback the
     * resolver picked, useful for the small caption under the avatar.
     */
    public static String describeAvatarSource(User user) {
        if (user == null) return "Default avatar";
        String img = user.getImage();
        if (img != null && !img.isBlank()) {
            String t = img.trim();
            if (t.startsWith("http://") || t.startsWith("https://")) return "Linked from Google";
            if (new File("uploads", t).isFile()) return "Custom picture (" + t + ")";
        }
        return "Avatar by Gravatar — gravatar.com";
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) { return ""; }
    }
}
