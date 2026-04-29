package service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches album artwork from TheAudioDB API (free, no key — uses public key "2").
 * Used to display artist/album images on shop product cards.
 */
public class AudioDBService {

    private static final String BASE = "https://www.theaudiodb.com/api/v1/json/2/";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    // Cache so we don't hammer the API for repeated lookups of the same artist
    private static final Map<String, String> CACHE = new HashMap<>();

    /** Returns a thumbnail URL for the artist, or null if not found. */
    public static String getArtistThumb(String artistName) {
        if (artistName == null || artistName.isBlank()) return null;
        String key = artistName.toLowerCase().trim();
        if (CACHE.containsKey(key)) return CACHE.get(key);

        try {
            String encoded = URLEncoder.encode(key, StandardCharsets.UTF_8);
            String url = BASE + "search.php?s=" + encoded;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "EventoApp/1.0")
                    .header("Accept",     "application/json")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String thumb = field(resp.body(), "strArtistThumb");
                if (thumb == null) thumb = field(resp.body(), "strArtistFanart");
                CACHE.put(key, thumb);
                return thumb;
            }
        } catch (Exception ignored) {}
        CACHE.put(key, null);
        return null;
    }

    /** Returns the first album artwork URL for the given artist, or null. */
    public static String getAlbumThumb(String artistName) {
        if (artistName == null || artistName.isBlank()) return null;
        try {
            String encoded = URLEncoder.encode(artistName.toLowerCase().trim(), StandardCharsets.UTF_8);
            String url = BASE + "searchalbum.php?s=" + encoded;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return field(resp.body(), "strAlbumThumb");
        } catch (Exception ignored) {}
        return null;
    }

    private static String field(String json, String key) {
        String s = "\"" + key + "\":\"";
        int idx = json.indexOf(s);
        if (idx < 0) return null;
        int start = idx + s.length();
        int end   = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : null;
    }
}
