package service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public record TopAlbum(String name, String artist, String artworkUrl, String iTunesUrl, int rank) {}

    /**
     * Fetches the iTunes top albums RSS for the given country (ISO-2).
     * Returns up to {@code limit} albums, parsed from the public JSON feed.
     * Cached per (country, limit) for the lifetime of the JVM.
     */
    private static final java.util.Map<String, List<TopAlbum>> TOP_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, List<String>> TOPSONG_GENRE_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static List<TopAlbum> topAlbums(String iso2, int limit) {
        String c = (iso2 == null || iso2.isBlank()) ? "us" : iso2.toLowerCase().trim();
        if (c.length() != 2) c = "us";
        int n = Math.max(1, Math.min(50, limit));
        String key = c + ":" + n;
        List<TopAlbum> cached = TOP_CACHE.get(key);
        if (cached != null) return cached;

        try {
            String url = "https://itunes.apple.com/" + c + "/rss/topalbums/limit=" + n + "/json";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(7))
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                List<TopAlbum> parsed = parseTopAlbums(resp.body());
                TOP_CACHE.put(key, parsed);
                return parsed;
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    /**
     * Reads iTunes top-songs RSS for {@code iso2} and returns primary genre labels
     * (one per chart entry), newest first. Cached per country for the JVM lifetime.
     */
    public static List<String> topSongGenres(String iso2, int limit) {
        String c = (iso2 == null || iso2.isBlank()) ? "us" : iso2.toLowerCase().trim();
        if (c.length() != 2) c = "us";
        int n = Math.max(8, Math.min(50, limit));
        String key = c + ":" + n;
        List<String> cached = TOPSONG_GENRE_CACHE.get(key);
        if (cached != null) return cached;

        try {
            String url = "https://itunes.apple.com/" + c + "/rss/topsongs/limit=" + n + "/json";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(7))
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                List<String> genres = parseTopSongGenres(resp.body());
                TOPSONG_GENRE_CACHE.put(key, genres);
                return genres;
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    /** Human-readable line for the merch sidebar: which genres dominate the singles chart. */
    public static String summarizeTrendingGenres(String iso2) {
        List<String> raw = topSongGenres(iso2, 30);
        if (raw.isEmpty())
            return "Genre pulse unavailable right now — still browse the chart artists opposite.";
        Map<String, Long> freq = raw.stream()
                .filter(g -> g != null && !g.isBlank())
                .collect(Collectors.groupingBy(g -> g, LinkedHashMap::new, Collectors.counting()));
        List<Map.Entry<String, Long>> top = freq.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(4)
                .toList();
        if (top.isEmpty())
            return "Chart genres loading…";
        StringBuilder sb = new StringBuilder("This week’s most-played flavours on iTunes: ");
        for (int i = 0; i < top.size(); i++) {
            if (i > 0) sb.append(i == top.size() - 1 ? " & " : ", ");
            sb.append(top.get(i).getKey());
        }
        sb.append(". Matching merch in those vibes tends to move fast.");
        return sb.toString();
    }

    private static List<String> parseTopSongGenres(String json) {
        List<String> out = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"im:name\"", idx + 1)) > 0) {
            String g = genreTermNear(json, idx);
            if (g != null && !g.isBlank() && !"Music".equalsIgnoreCase(g))
                out.add(unescape(g));
        }
        return out;
    }

    /** Genre {@code term} from the {@code category} block nearest after {@code from}. */
    private static String genreTermNear(String json, int from) {
        int cat = json.indexOf("\"category\":", from);
        if (cat < 0 || cat - from > 5000) return "";
        int term = json.indexOf("\"term\":\"", cat);
        if (term < 0 || term - cat > 900) return "";
        int start = term + "\"term\":\"".length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : "";
    }

    private static List<TopAlbum> parseTopAlbums(String json) {
        // Feed format: feed.entry[]  -> { "im:name": {"label":"X"}, "im:artist": {"label":"Y"},
        //                                  "im:image":[..., {"label":"<png url>"}], "id":{"label":"<itunes link>"} }
        List<TopAlbum> result = new ArrayList<>();
        int entryStart = json.indexOf("\"entry\":[");
        if (entryStart < 0) return result;
        int idx = entryStart;
        int rank = 1;
        while ((idx = json.indexOf("\"im:name\"", idx)) >= 0 && rank <= 50) {
            String name   = readNestedLabel(json, idx);
            int artistIdx = json.indexOf("\"im:artist\"", idx);
            String artist = artistIdx > 0 ? readNestedLabel(json, artistIdx) : "";
            int imgIdx    = json.indexOf("\"im:image\":[", idx);
            String art    = imgIdx > 0 ? readLastImageLabel(json, imgIdx) : "";
            int linkIdx   = json.indexOf("\"id\":{", idx);
            String link   = linkIdx > 0 ? readNestedLabel(json, linkIdx) : "";
            if (name != null && !name.isEmpty())
                result.add(new TopAlbum(name, artist, art, link, rank));
            rank++;
            idx = (artistIdx > 0 ? artistIdx : idx) + 1;
        }
        return result;
    }

    /** Reads {@code "label":"…"} after the cursor. */
    private static String readNestedLabel(String json, int from) {
        String key = "\"label\":\"";
        int i = json.indexOf(key, from);
        if (i < 0) return "";
        int start = i + key.length();
        int end = start;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\' && end + 1 < json.length()) end += 2;
            else end++;
        }
        return unescape(json.substring(start, end));
    }

    /** Reads the LAST {@code "label":"…"} before the closing array bracket. */
    private static String readLastImageLabel(String json, int from) {
        int arrEnd = json.indexOf(']', from);
        if (arrEnd < 0) return "";
        String slice = json.substring(from, arrEnd);
        int last = slice.lastIndexOf("\"label\":\"");
        if (last < 0) return "";
        int start = from + last + "\"label\":\"".length();
        int end = start;
        while (end < json.length() && json.charAt(end) != '"') end++;
        // upgrade artwork URL from /60x60bb to /256x256bb for crisper rendering.
        String url = json.substring(start, end);
        return url.replace("/60x60bb", "/256x256bb")
                  .replace("/170x170bb", "/256x256bb");
    }

    /**
     * Looks up official artwork for a performing artist via the iTunes Search API
     * ({@code entity=musicArtist}). Returns {@code null} if the catalog has no art.
     */
    public static String artistArtworkUrl(String artistName) {
        if (artistName == null || artistName.isBlank()) return null;
        try {
            String query = "term=" + URLEncoder.encode(artistName.trim(), StandardCharsets.UTF_8)
                    + "&entity=musicArtist&limit=8";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + query))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            return firstArtworkUrl(resp.body());
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Parses the first {@code artworkUrl100} (or 30/60) in a search JSON blob. */
    private static String firstArtworkUrl(String body) {
        String key = "\"artworkUrl100\":\"";
        int idx = body.indexOf(key);
        if (idx < 0) {
            key = "\"artworkUrl60\":\"";
            idx = body.indexOf(key);
        }
        if (idx < 0) {
            key = "\"artworkUrl30\":\"";
            idx = body.indexOf(key);
        }
        if (idx < 0) return null;
        int start = idx + key.length();
        int end = body.indexOf('"', start);
        if (end <= start) return null;
        String url = unescape(body.substring(start, end));
        return upscaleArtwork(url);
    }

    private static String upscaleArtwork(String url) {
        if (url == null || url.isBlank()) return null;
        return url.replace("/30x30bb", "/600x600bb")
                .replace("/60x60bb", "/600x600bb")
                .replace("/100x100bb", "/600x600bb")
                .replace("/170x170bb", "/600x600bb")
                .replace("/200x200bb", "/600x600bb");
    }

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
