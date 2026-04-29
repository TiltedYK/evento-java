package service;

import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * Generates a visual image CAPTCHA locally (no external API key required).
 * Uses Java2D to draw distorted alphanumeric text with noise, color shifts,
 * and grid lines — similar to classic web CAPTCHAs.
 */
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RNG   = new Random();
    private static String currentCode = "";

    /** Generates a new CAPTCHA and returns the JavaFX Image. */
    public static Image generateCaptcha() {
        currentCode = randomCode(5);
        return render(currentCode);
    }

    /** Returns true if the user's answer matches (case-insensitive). */
    public static boolean validate(String answer) {
        return answer != null && answer.trim().equalsIgnoreCase(currentCode);
    }

    public static String getCurrentCode() { return currentCode; }

    // ── Rendering ─────────────────────────────────────────────────────

    private static Image render(String code) {
        int w = 200, h = 60;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background gradient
        GradientPaint bg = new GradientPaint(0, 0,
                new Color(16, 16, 26), w, h, new Color(22, 22, 38));
        g.setPaint(bg); g.fillRect(0, 0, w, h);

        // Noise dots
        for (int i = 0; i < 280; i++) {
            g.setColor(new Color(RNG.nextInt(256), RNG.nextInt(256), RNG.nextInt(256), 60 + RNG.nextInt(80)));
            int x = RNG.nextInt(w), y = RNG.nextInt(h);
            g.fillOval(x, y, 2, 2);
        }

        // Distortion lines
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(RNG.nextInt(200) + 55, RNG.nextInt(100), RNG.nextInt(100), 120));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(RNG.nextInt(w / 2), RNG.nextInt(h),
                       w / 2 + RNG.nextInt(w / 2), RNG.nextInt(h));
        }

        // Draw each character with random tilt, color, and size
        Font baseFont = new Font("Courier New", Font.BOLD, 26);
        int cellW = w / code.length();
        for (int i = 0; i < code.length(); i++) {
            g.setFont(baseFont.deriveFont((float)(22 + RNG.nextInt(8))));
            g.setColor(new Color(180 + RNG.nextInt(76), 100 + RNG.nextInt(80), 100 + RNG.nextInt(80)));

            double angle = Math.toRadians(-18 + RNG.nextInt(36));
            int cx = cellW * i + cellW / 2;
            int cy = h / 2 + 8;
            g.rotate(angle, cx, cy);
            g.drawString(String.valueOf(code.charAt(i)), cx - 8, cy);
            g.rotate(-angle, cx, cy);
        }

        // Border
        g.setColor(new Color(232, 50, 10, 100));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(1, 1, w - 2, h - 2, 6, 6);

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "PNG", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) { return null; }
    }

    private static String randomCode(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        return sb.toString();
    }
}
