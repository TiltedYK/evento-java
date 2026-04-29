package service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Generates Gravatar avatar URLs from an email address (free, no API key).
 * Used on the Profile page to show a personalized avatar.
 */
public class GravatarService {

    private static final String BASE = "https://www.gravatar.com/avatar/";

    /** Returns a Gravatar image URL. Falls back to a robot identicon if no Gravatar is set. */
    public static String getAvatarUrl(String email, int sizePx) {
        String hash = md5(email != null ? email.trim().toLowerCase() : "");
        return BASE + hash + "?s=" + sizePx + "&d=identicon&r=g";
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
