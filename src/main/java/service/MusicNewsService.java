package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches music articles from Dev.to public API (free, no API key).
 * Used in the Blog/Feed module to show live music news.
 */
public class MusicNewsService {

    private static final int DEFAULT_PAGE_SIZE = 8;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** {@code coverImage} is Dev.to {@code cover_image} when present (https URL), else null. */
    public record Article(String title, String description, String url,
                          String author, String publishedAt,
                          int reactions, int comments,
                          String coverImage) {}

    public static List<Article> getLatestNews() {
        return getLatestNews(DEFAULT_PAGE_SIZE);
    }

    /**
     * @param perPage API page size (clamped 1–30). Never returns placeholder/fake articles —
     *                an empty list means the network failed or Dev.to returned nothing.
     */
    public static List<Article> getLatestNews(int perPage) {
        int n = Math.max(1, Math.min(30, perPage));
        String url = "https://dev.to/api/articles?tag=music&per_page=" + n + "&top=30";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return parse(resp.body());
        } catch (Exception ignored) {}
        return List.of();
    }

    private static List<Article> parse(String json) {
        List<Article> list = new ArrayList<>();
        int pos = 0;
        while (true) {
            int start = json.indexOf("{\"type_of\":", pos);
            if (start < 0) break;
            int end = findObjectEnd(json, start);
            if (end < 0) break;
            String obj = json.substring(start, end + 1);

            String title       = field(obj, "title");
            String desc        = field(obj, "description");
            String url         = field(obj, "url");
            String author      = nested(obj, "user", "name");
            String published   = field(obj, "published_at");
            int    reactions   = intField(obj, "public_reactions_count");
            int    comments    = intField(obj, "comments_count");
            String cover       = nullableStringField(obj, "cover_image");

            if (title != null && !title.isBlank()) {
                list.add(new Article(
                        unescape(title),
                        desc != null ? unescape(desc) : "",
                        url  != null ? url : "",
                        author != null ? author : "Dev.to Author",
                        published != null ? published.substring(0, 10) : "",
                        reactions, comments,
                        cover != null && !cover.isBlank() ? cover : null));
            }
            pos = end + 1;
        }
        return list;
    }

    private static String field(String json, String key) {
        String s = "\"" + key + "\":\"";
        int idx = json.indexOf(s);
        if (idx < 0) return null;
        int start = idx + s.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    /** Returns null when the key is missing or JSON {@code null} (not a string). */
    private static String nullableStringField(String json, String key) {
        String nullTok = "\"" + key + "\":null";
        if (json.contains(nullTok)) return null;
        return field(json, key);
    }

    private static String nested(String json, String outer, String inner) {
        String s = "\"" + outer + "\":";
        int idx = json.indexOf(s);
        if (idx < 0) return null;
        int braceStart = json.indexOf("{", idx);
        if (braceStart < 0) return null;
        int braceEnd = findObjectEnd(json, braceStart);
        if (braceEnd < 0) return null;
        return field(json.substring(braceStart, braceEnd + 1), inner);
    }

    private static int intField(String json, String key) {
        String s = "\"" + key + "\":";
        int idx = json.indexOf(s);
        if (idx < 0) return 0;
        int start = idx + s.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return 0; }
    }

    private static int findObjectEnd(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static String unescape(String s) {
        return s.replace("\\u2019", "'").replace("\\u2018", "'")
                .replace("\\u2014", "—").replace("\\u2013", "–")
                .replace("\\n", " ").replace("\\\"", "\"").replace("\\\\", "\\");
    }

}
