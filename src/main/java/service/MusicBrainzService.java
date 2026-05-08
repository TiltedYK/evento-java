package service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches artist metadata from MusicBrainz API (free, no API key required).
 * Used in the shop to show genre tags and artist type on product cards.
 */
public class MusicBrainzService {

    private static final String BASE = "https://musicbrainz.org/ws/2/";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    private static final Map<String, List<String>> CACHE = new HashMap<>();

    /**
     * Returns a list of genre/tag names for the given artist (e.g. ["rock","alternative","indie"]).
     * Returns an empty list if the artist is not found.
     */
    public static List<String> getArtistTags(String artistName) {
        if (artistName == null || artistName.isBlank()) return List.of();
        String key = artistName.toLowerCase().trim();
        if (CACHE.containsKey(key)) return CACHE.get(key);

        List<String> tags = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode("artist:" + key, StandardCharsets.UTF_8);
            String url = BASE + "artist/?query=" + encoded + "&fmt=json&limit=1";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "EventoApp/1.0 (contact@evento.tn)")
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                tags = parseTags(resp.body());
            }
        } catch (Exception ignored) {}

        CACHE.put(key, tags);
        return tags;
    }

    /**
     * Returns the artist type (e.g. "Person", "Group", "Orchestra") or null.
     */
    public static String getArtistType(String artistName) {
        if (artistName == null || artistName.isBlank()) return null;
        try {
            String encoded = URLEncoder.encode("artist:" + artistName.trim(), StandardCharsets.UTF_8);
            String url = BASE + "artist/?query=" + encoded + "&fmt=json&limit=1";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "EventoApp/1.0 (contact@evento.tn)")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return extractType(resp.body());
        } catch (Exception ignored) {}
        return null;
    }

    private static List<String> parseTags(String json) {
        List<String> tags = new ArrayList<>();
        // Find "tags":[{"count":N,"name":"..."},...]
        int tagsIdx = json.indexOf("\"tags\":");
        if (tagsIdx < 0) return tags;
        int arrStart = json.indexOf('[', tagsIdx);
        int arrEnd   = json.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return tags;
        String arr = json.substring(arrStart, arrEnd);
        // Extract all "name":"..." values
        int pos = 0;
        while (true) {
            int ni = arr.indexOf("\"name\":\"", pos);
            if (ni < 0) break;
            int start = ni + 8;
            int end   = arr.indexOf('"', start);
            if (end > start) {
                String tag = arr.substring(start, end);
                if (!tag.isBlank()) tags.add(tag);
            }
            pos = end + 1;
            if (tags.size() >= 4) break; // max 4 tags
        }
        return tags;
    }

    private static String extractType(String json) {
        String s = "\"type\":\"";
        int idx = json.indexOf(s);
        if (idx < 0) return null;
        int start = idx + s.length();
        int end   = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : null;
    }
}
