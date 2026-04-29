package service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Fetches a plain-text summary from the Wikipedia REST API (free, no key).
 * Used in the Artists/Partners module to show bio snippets.
 */
public class WikipediaService {

    private static final String BASE = "https://en.wikipedia.org/api/rest_v1/page/summary/";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** Returns the article extract, or a "not found" message. */
    public static String getSummary(String query) {
        if (query == null || query.isBlank()) return "No search term provided.";
        try {
            String slug = URLEncoder.encode(
                    query.trim().replace(" ", "_"), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + slug))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "EventoApp/1.0")
                    .header("Accept",     "application/json")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String extract = extractField(resp.body(), "extract");
                if (extract != null && !extract.isBlank())
                    return extract.length() > 500 ? extract.substring(0, 500) + "…" : extract;
            }
            if (resp.statusCode() == 404) return "No Wikipedia article found for \"" + query + "\".";
        } catch (Exception ignored) {}
        return "Wikipedia lookup unavailable.";
    }

    private static String extractField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && json.charAt(i - 1) != '\\') break;
            if (c == '\\' && i + 1 < json.length()) {
                char nx = json.charAt(i + 1);
                if      (nx == 'n') { sb.append(' '); i++; }
                else if (nx == '"') { sb.append('"'); i++; }
                else if (nx == '\\') { sb.append('\\'); i++; }
                else sb.append(c);
            } else sb.append(c);
        }
        return sb.toString();
    }
}
