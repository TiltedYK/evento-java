package service;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;

/**
 * Tiny SMTP-over-SSL client used to email the password-reset code.
 *
 * Why not JavaMail? — to stay zero-dependency (the rest of this app runs
 * on JDK + JavaFX + a couple of self-contained libs). SMTP-over-SSL on
 * port 465 is supported by Gmail, Outlook, Yahoo, Zoho, and most
 * providers, so a pure-JDK SSL socket is enough.
 *
 * Credentials are stored at ~/.evento/smtp.properties (same folder as the
 * Google OAuth credentials) so they survive between runs.
 *
 * For Gmail the {@code password} must be an "App Password" (account
 * settings → Security → 2-Step Verification → App passwords), NOT the
 * regular Gmail password.
 */
public class MailService {

    public static final String BUILD_TAG = "EVENTO_MAIL_BUILD_2026_04_28_RAW_SMTPS_V1";

    /** Where SMTP credentials are persisted between runs. */
    public static final Path USER_CREDENTIALS_FILE =
            Path.of(System.getProperty("user.home"), ".evento", "smtp.properties");

    public static class Settings {
        public String host;
        public int    port;
        public String username;
        public String password;
        public String fromAddress;
        public String fromName;
    }

    private final Settings settings = new Settings();

    public MailService() { loadSettings(); }

    /** Reloads settings from disk. */
    public void loadSettings() {
        Properties p = new Properties();
        try {
            if (Files.exists(USER_CREDENTIALS_FILE)) {
                try (var in = Files.newBufferedReader(USER_CREDENTIALS_FILE, StandardCharsets.UTF_8)) {
                    p.load(in);
                }
            }
        } catch (IOException ignored) { /* fall through with empty props */ }

        settings.host        = p.getProperty("smtp.host", "smtp.gmail.com").trim();
        settings.port        = parseInt(p.getProperty("smtp.port", "465"), 465);
        settings.username    = p.getProperty("smtp.username", "").trim();
        settings.password    = p.getProperty("smtp.password", "").trim();
        settings.fromAddress = p.getProperty("smtp.from", settings.username).trim();
        settings.fromName    = p.getProperty("smtp.fromName", "EVENTO").trim();
    }

    public Settings getSettings() { return settings; }

    public boolean isConfigured() {
        return settings.host != null && !settings.host.isEmpty()
            && settings.port > 0
            && settings.username != null && !settings.username.isEmpty()
            && settings.password != null && !settings.password.isEmpty()
            && settings.fromAddress != null && !settings.fromAddress.isEmpty();
    }

    /** Persists new settings to disk and reloads them. */
    public static void saveSettings(String host, int port,
                                    String username, String password,
                                    String fromAddress, String fromName) throws IOException {
        Path dir = USER_CREDENTIALS_FILE.getParent();
        if (dir != null && !Files.exists(dir)) Files.createDirectories(dir);

        Properties p = new Properties();
        p.setProperty("smtp.host", host == null ? "" : host.trim());
        p.setProperty("smtp.port", String.valueOf(port));
        p.setProperty("smtp.username", username == null ? "" : username.trim());
        p.setProperty("smtp.password", password == null ? "" : password);
        p.setProperty("smtp.from",     fromAddress == null || fromAddress.isBlank()
                ? (username == null ? "" : username.trim())
                : fromAddress.trim());
        p.setProperty("smtp.fromName", fromName == null || fromName.isBlank()
                ? "EVENTO"
                : fromName.trim());

        try (var out = Files.newBufferedWriter(USER_CREDENTIALS_FILE, StandardCharsets.UTF_8)) {
            p.store(out,
                    "EVENTO SMTP credentials.\n"
                  + "For Gmail use an App Password (Google Account → Security → 2-Step Verification → App passwords).");
        }
    }

    // ── Public API ───────────────────────────────────────────────────

    /**
     * Sends the password-reset email. Blocks until SMTP confirms delivery.
     * Throws {@link IOException} on any network/auth error so the caller
     * can show a meaningful message.
     */
    public void sendPasswordResetCode(String toEmail, String firstName, String code)
            throws IOException {
        if (!isConfigured())
            throw new IOException("SMTP is not configured.");

        String safeName = firstName == null || firstName.isBlank() ? "there" : firstName;
        String subject = "Your EVENTO password reset code";
        String body =
                "Hi " + safeName + ",\r\n\r\n"
              + "Someone — hopefully you — asked to reset the password for your EVENTO account (" + toEmail + ").\r\n\r\n"
              + "Your one-time confirmation code is:\r\n\r\n"
              + "    " + code + "\r\n\r\n"
              + "Type this code into the EVENTO app to choose a new password. The code is valid only for the\r\n"
              + "current reset session and dies the moment you close the dialog.\r\n\r\n"
              + "If you didn't request a reset, you can safely ignore this email — your password hasn't changed.\r\n\r\n"
              + "— The EVENTO team\r\n";

        sendRaw(settings.fromAddress, settings.fromName,
                toEmail, subject, body);
    }

    // ── Raw SMTPS implementation ─────────────────────────────────────

    private void sendRaw(String fromAddr, String fromName,
                         String toAddr, String subject, String body) throws IOException {
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket sock = (SSLSocket) ssf.createSocket(settings.host, settings.port)) {
            sock.setSoTimeout(30_000);
            sock.startHandshake();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    sock.getInputStream(), StandardCharsets.UTF_8));
            OutputStream rawOut = sock.getOutputStream();
            Writer w = new OutputStreamWriter(rawOut, StandardCharsets.UTF_8);
            PrintWriter out = new PrintWriter(w, false);

            expect(in, "220");                                   // greeting
            send(out, "EHLO evento-client");
            consumeMultiline(in, "250");

            send(out, "AUTH LOGIN");
            expect(in, "334");
            send(out, b64(settings.username));
            expect(in, "334");
            send(out, b64(settings.password));
            expect(in, "235");                                   // auth ok

            send(out, "MAIL FROM:<" + fromAddr + ">");
            expect(in, "250");
            send(out, "RCPT TO:<" + toAddr + ">");
            expect(in, "250");

            send(out, "DATA");
            expect(in, "354");

            // Headers + body. Lines starting with "." must be dot-stuffed.
            StringBuilder msg = new StringBuilder()
                    .append("From: ").append(encodedFrom(fromName, fromAddr)).append("\r\n")
                    .append("To: <").append(toAddr).append(">\r\n")
                    .append("Subject: ").append(encodedHeader(subject)).append("\r\n")
                    .append("MIME-Version: 1.0\r\n")
                    .append("Content-Type: text/plain; charset=UTF-8\r\n")
                    .append("Content-Transfer-Encoding: 8bit\r\n")
                    .append("\r\n")
                    .append(dotStuff(body))
                    .append("\r\n.\r\n");
            out.write(msg.toString());
            out.flush();
            expect(in, "250");                                   // accepted

            send(out, "QUIT");
            // ignore final "221 bye"
        }
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static void send(PrintWriter out, String cmd) {
        out.write(cmd);
        out.write("\r\n");
        out.flush();
    }

    /** Reads a single-line response, throws if the SMTP code isn't the expected prefix. */
    private static String expect(BufferedReader in, String expected) throws IOException {
        String line = in.readLine();
        if (line == null) throw new IOException("SMTP server closed the connection unexpectedly.");
        // Multi-line responses start with "<code>-": consume all and check the last line code.
        while (line.length() >= 4 && line.charAt(3) == '-') {
            line = in.readLine();
            if (line == null) throw new IOException("SMTP server closed the connection mid-response.");
        }
        if (!line.startsWith(expected))
            throw new IOException("SMTP error (expected " + expected + "): " + line);
        return line;
    }

    /** Like expect, but the caller doesn't care which exact code, only the prefix. */
    private static void consumeMultiline(BufferedReader in, String expected) throws IOException {
        String line = in.readLine();
        if (line == null) throw new IOException("SMTP server closed the connection unexpectedly.");
        while (line.length() >= 4 && line.charAt(3) == '-') {
            line = in.readLine();
            if (line == null) throw new IOException("SMTP server closed the connection mid-response.");
        }
        if (!line.startsWith(expected))
            throw new IOException("SMTP error (expected " + expected + "): " + line);
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    /** Dot-stuffing: per RFC 5321, any line starting with "." inside DATA must be doubled. */
    private static String dotStuff(String body) {
        // Normalize line endings to CRLF and dot-stuff
        StringBuilder b = new StringBuilder(body.length() + 16);
        for (String line : body.replace("\r\n", "\n").split("\n", -1)) {
            if (line.startsWith(".")) b.append('.');
            b.append(line).append("\r\n");
        }
        // Trim trailing CRLF the caller will add back
        if (b.length() >= 2 && b.charAt(b.length() - 1) == '\n' && b.charAt(b.length() - 2) == '\r')
            b.setLength(b.length() - 2);
        return b.toString();
    }

    /** Encode a name+address using RFC 2047 if the name has non-ASCII. */
    private static String encodedFrom(String name, String addr) {
        if (name == null || name.isBlank()) return "<" + addr + ">";
        return encodedHeader(name) + " <" + addr + ">";
    }

    private static String encodedHeader(String value) {
        boolean ascii = true;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) > 127) { ascii = false; break; }
        if (ascii) return value;
        return "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8))
                + "?=";
    }

    private static int parseInt(String v, int def) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }
}
