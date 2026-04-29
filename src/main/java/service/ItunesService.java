package service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Queries the iTunes Search API (free, no key required).
 * Used on the Artists page to show top tracks for a given artist.
 */
public class ItunesService {

    private static final String BASE = "https://itunes.apple.com/search?";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public record Track(String trackName, String albumName, String previewUrl,
                        String artworkUrl, String releaseDate, String genre) {}

    public static List<Track> searchArtist(String artistName, int limit) {
        try {
            String query = "term=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8)
                    + "&entity=musicTrack&limit=" + limit;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + query))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return parseTracks(resp.body());
        } catch (Exception ignored) {}
        return List.of();
    }

    private static List<Track> parseTracks(String json) {
        List<Track> tracks = new ArrayList<>();
        int pos = json.indexOf("{\"wrapperType\":");
        while (pos >= 0) {
            int end = findObjectEnd(json, pos);
            if (end < 0) break;
            String obj = json.substring(pos, end + 1);

            String trackName  = field(obj, "trackName");
            String albumName  = field(obj, "collectionName");
            String preview    = field(obj, "previewUrl");
            String artwork    = field(obj, "artworkUrl100");
            String release    = field(obj, "releaseDate");
            String genre      = field(obj, "primaryGenreName");

            if (trackName != null)
                tracks.add(new Track(unescape(trackName),
                        albumName  != null ? unescape(albumName)  : "",
                        preview    != null ? preview : "",
                        artwork    != null ? artwork : "",
                        release    != null ? release.substring(0, Math.min(10, release.length())) : "",
                        genre      != null ? genre : ""));

            pos = json.indexOf("{\"wrapperType\":", end);
        }
        return tracks;
    }

    private static String field(String json, String key) {
        String s = "\"" + key + "\":\"";
        int idx = json.indexOf(s);
        if (idx < 0) return null;
        int start = idx + s.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    private static int findObjectEnd(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') { if (--depth == 0) return i; }
        }
        return -1;
    }

    private static String unescape(String s) {
        return s.replace("\\u2019", "'").replace("\\u2018", "'")
                .replace("\\u2014", "—").replace("\\\"", "\"");
    }
}
