package service;

import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Google OAuth 2.0 sign-in for desktop apps using the Loopback IP redirect flow.
 *
 *   1. Spawns a tiny HTTP server bound to a free port on 127.0.0.1
 *   2. Opens the system browser to Google's account-chooser screen
 *   3. Receives the authorization code at the loopback URL
 *   4. Exchanges it for an access token, then fetches the user profile
 *
 * Credentials are loaded with the following precedence (first wins):
 *   1. JVM system properties {@code -Dgoogle.client.id=...} / {@code -Dgoogle.client.secret=...}
 *   2. {@code ~/.evento/google_oauth.properties} (user-writable, persisted at runtime)
 *   3. {@code /google_oauth.properties} on the classpath (project default / template)
 *
 * Identifying tag for build verification:
 *   EVENTO_GAUTH_BUILD_2026_04_28_OAUTH_LOOPBACK
 */
public class GoogleAuthService {

    /** Bumped on every material edit. Grep this string in the source AND
     *  watch for it in the Run console at startup to confirm you are NOT
     *  running a stale compiled build. */
    public static final String BUILD_TAG = "EVENTO_GAUTH_BUILD_2026_04_28_OAUTH_LOOPBACK";

    /** Lightweight DTO of the data Google gives back from /userinfo. */
    public static class GoogleUser {
        public final String email;
        public final String firstName;
        public final String lastName;
        public final String picture;
        public final String googleId;

        public GoogleUser(String email, String firstName, String lastName,
                          String picture, String googleId) {
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.picture = picture;
            this.googleId = googleId;
        }
    }

    private static final String AUTH_ENDPOINT     = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT    = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPE             = "openid email profile";

    /** Where credentials saved by {@link #saveCredentials} are persisted. */
    public static final Path USER_CREDENTIALS_FILE =
            Path.of(System.getProperty("user.home"), ".evento", "google_oauth.properties");

    private final String clientId;
    private final String clientSecret;

    public GoogleAuthService() {
        Properties p = loadProps();
        this.clientId     = p.getProperty("google.client.id", "").trim();
        this.clientSecret = p.getProperty("google.client.secret", "").trim();
    }

    /** True only when real (non-placeholder) credentials are present. */
    public boolean isConfigured() {
        return looksValid(clientId) && looksValid(clientSecret);
    }

    private static boolean looksValid(String v) {
        return v != null && !v.isBlank() && !v.startsWith("YOUR_");
    }

    private Properties loadProps() {
        Properties p = new Properties();

        // 3) classpath template (lowest priority)
        try (InputStream in = getClass().getResourceAsStream("/google_oauth.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) { /* ignore */ }

        // 2) user-home overrides classpath
        if (Files.isReadable(USER_CREDENTIALS_FILE)) {
            try (InputStream in = Files.newInputStream(USER_CREDENTIALS_FILE)) {
                Properties userProps = new Properties();
                userProps.load(in);
                userProps.forEach((k, v) -> p.setProperty(k.toString(), v.toString()));
            } catch (IOException ignored) { /* ignore */ }
        }

        // 1) system properties trump everything
        String idOverride  = System.getProperty("google.client.id");
        String secOverride = System.getProperty("google.client.secret");
        if (idOverride  != null) p.setProperty("google.client.id",     idOverride);
        if (secOverride != null) p.setProperty("google.client.secret", secOverride);
        return p;
    }

    /**
     * Persists the given Google OAuth credentials to
     * {@link #USER_CREDENTIALS_FILE} so subsequent runs pick them up
     * automatically. Trims whitespace, throws on invalid values.
     */
    public static void saveCredentials(String clientId, String clientSecret) throws IOException {
        if (clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("Client ID and Client Secret are required.");
        }
        Properties p = new Properties();
        p.setProperty("google.client.id",     clientId.trim());
        p.setProperty("google.client.secret", clientSecret.trim());

        Files.createDirectories(USER_CREDENTIALS_FILE.getParent());
        try (OutputStream out = new FileOutputStream(USER_CREDENTIALS_FILE.toFile())) {
            p.store(out, "EVENTO — Google OAuth credentials (do not commit this file)");
        }
    }

    /**
     * Runs the full OAuth flow synchronously. MUST be called off the JavaFX thread.
     *
     * @return populated {@link GoogleUser}
     * @throws Exception if Google rejects the request, the user closes the tab,
     *                   or no callback is received within 3 minutes
     */
    public GoogleUser signIn() throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException(
                "Google OAuth is not configured. Save your Client ID and Secret via the in-app "
              + "setup dialog, or edit " + USER_CREDENTIALS_FILE + " (see GOOGLE_OAUTH_SETUP.md).");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String redirectUri = "http://127.0.0.1:" + port;

        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        String state = randomState();

        server.createContext("/", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            String body;
            int status = 200;

            String returnedCode  = params.get("code");
            String returnedState = params.get("state");
            String errorParam    = params.get("error");

            if (errorParam != null) {
                body = htmlPage("AUTHENTICATION DENIED",
                        "Google returned: " + errorParam, "#FF4422");
                status = 400;
                codeFuture.completeExceptionally(
                        new RuntimeException("Google error: " + errorParam));
            } else if (returnedCode == null
                    || returnedState == null
                    || !state.equals(returnedState)) {
                body = htmlPage("INVALID REQUEST",
                        "Missing or mismatched state parameter.", "#FF4422");
                status = 400;
                codeFuture.completeExceptionally(
                        new RuntimeException("Invalid state or missing code"));
            } else {
                body = htmlPage("ACCESS GRANTED",
                        "You can close this tab and return to EVENTO.", "#E8320A");
                codeFuture.complete(returnedCode);
            }

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GoogleAuth-HttpServer");
            t.setDaemon(true);
            return t;
        }));
        server.start();

        try {
            String authUrl = AUTH_ENDPOINT
                    + "?response_type=code"
                    + "&client_id="     + urlEnc(clientId)
                    + "&redirect_uri="  + urlEnc(redirectUri)
                    + "&scope="         + urlEnc(SCOPE)
                    + "&prompt=select_account"
                    + "&access_type=online"
                    + "&state="         + urlEnc(state);

            openBrowser(authUrl);

            String code = codeFuture.get(3, TimeUnit.MINUTES);

            String tokenJson = httpPost(TOKEN_ENDPOINT,
                      "code="          + urlEnc(code)
                    + "&client_id="    + urlEnc(clientId)
                    + "&client_secret="+ urlEnc(clientSecret)
                    + "&redirect_uri=" + urlEnc(redirectUri)
                    + "&grant_type=authorization_code");
            JSONObject tokens = new JSONObject(tokenJson);
            String accessToken = tokens.getString("access_token");

            String userJson = httpGet(USERINFO_ENDPOINT, accessToken);
            JSONObject u = new JSONObject(userJson);

            return new GoogleUser(
                    u.optString("email", null),
                    u.optString("given_name", ""),
                    u.optString("family_name", ""),
                    u.optString("picture", null),
                    u.optString("sub", null));
        } finally {
            server.stop(0);
        }
    }

    // ─────────── helpers ───────────

    private static String randomState() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static Map<String, String> parseQuery(String q) {
        Map<String, String> map = new HashMap<>();
        if (q == null || q.isEmpty()) return map;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            map.put(k, v);
        }
        return map;
    }

    private static String urlEnc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String httpPost(String url, String body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        c.setRequestProperty("Accept", "application/json");
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (code >= 400) throw new IOException("Google token endpoint HTTP " + code + ": " + resp);
        return resp;
    }

    private static String httpGet(String url, String bearer) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + bearer);
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (code >= 400) throw new IOException("Google userinfo HTTP " + code + ": " + resp);
        return resp;
    }

    private static void openBrowser(String url) throws IOException {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) { /* fall through */ }

        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (IOException e) {
            System.out.println("Open this URL in your browser:\n" + url);
            throw e;
        }
    }

    private static String htmlPage(String title, String message, String accent) {
        return "<!doctype html><html><head><meta charset='utf-8'><title>" + title + "</title>"
             + "<style>"
             + "body{font-family:-apple-system,Segoe UI,Helvetica,sans-serif;background:#050507;"
             +     "color:#F0F2F8;display:flex;align-items:center;justify-content:center;"
             +     "height:100vh;margin:0}"
             + ".card{background:#0A0A0F;border:1.5px solid " + accent + ";"
             +     "padding:48px 64px;text-align:center;border-radius:2px;"
             +     "box-shadow:0 0 60px " + accent + "55}"
             + "h1{color:" + accent + ";letter-spacing:6px;font-family:\"Courier New\",monospace;"
             +    "margin:0 0 12px;font-size:22px}"
             + "p{color:#8890A8;font-family:\"Courier New\",monospace;margin:0;font-size:13px}"
             + "</style></head><body><div class='card'>"
             + "<h1>&#9889; " + title + "</h1><p>" + message + "</p></div></body></html>";
    }
}
