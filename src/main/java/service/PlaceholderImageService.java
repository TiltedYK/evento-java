package service;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generates contextually relevant placeholder image URLs without requiring
 * any API key.
 *
 * Strategy
 * --------
 *   1. <b>LoremFlickr</b> (https://loremflickr.com/W/H/tag1,tag2/all?lock=N)
 *      returns a free Flickr photo matching the comma-separated tags.
 *      The {@code lock} parameter pins a specific image so the same product
 *      / event / artist always shows the same picture across renders.
 *   2. <b>ui-avatars</b> for user avatars / artist initials with branded colors.
 *   3. <b>Picsum</b> as a final-fallback (random but deterministic by seed).
 *
 * Each call is non-blocking: the URL is constructed synchronously, the
 * actual HTTP fetch happens on a JavaFX background loader.
 *
 * Build tag for verification:
 *   {@code EVENTO_PLACEHOLDER_BUILD_2026_04_28_LOREMFLICKR_V1}
 */
public final class PlaceholderImageService {

    public static final String BUILD_TAG = "EVENTO_PLACEHOLDER_BUILD_2026_04_28_LOREMFLICKR_V1";

    private PlaceholderImageService() {}

    private static final ExecutorService LOADER =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "placeholder-loader");
                t.setDaemon(true);
                return t;
            });

    /** Tiny in-memory cache so the same image isn't fetched twice in one session. */
    private static final ConcurrentHashMap<String, Image> CACHE = new ConcurrentHashMap<>();

    // ── Public URL builders ────────────────────────────────────────────────

    public static String forArtist(String name, String genre) {
        String tag = baseGenreTag(genre) + ",musician,portrait";
        return loremFlickr(tag, 400, 400, "artist|" + safe(name) + "|" + safe(genre));
    }

    public static String forEvent(String title, String genre, String venue) {
        String tag = baseGenreTag(genre) + ",concert,stage,crowd";
        return loremFlickr(tag, 600, 400, "event|" + safe(title) + "|" + safe(venue));
    }

    /** For posts use the title as a heuristic (e.g. words "concert", "review"). */
    public static String forPost(String title, String tags) {
        String t = (tags != null && !tags.isBlank()) ? tags : guessPostTags(title);
        return loremFlickr(t, 600, 400, "post|" + safe(title));
    }

    /** For products try a merch-flavoured query, optionally biased by artist genre. */
    public static String forProduct(String productName, String artistName, String genreHint) {
        String tag = baseGenreTag(genreHint) + ",merch,band";
        return loremFlickr(tag, 400, 400, "product|" + safe(productName) + "|" + safe(artistName));
    }

    /** ui-avatars for round/square initial circles (kept for parity with existing code). */
    public static String userAvatar(String fullName, String hexBg) {
        String n = encode(fullName == null || fullName.isBlank() ? "User" : fullName);
        String bg = (hexBg == null) ? "FF1493" : hexBg.replace("#", "");
        return "https://ui-avatars.com/api/?name=" + n
                + "&background=" + bg
                + "&color=FFFFFF&size=256&rounded=true&bold=true";
    }

    // ── Async helper to load into an ImageView ─────────────────────────────

    /**
     * Loads {@code primaryUrl} into the view asynchronously; if the image fails
     * to load, falls back to {@code fallbackUrl}. Any uncaught error is silently
     * ignored (the view simply stays empty).
     */
    public static void loadInto(ImageView view, String primaryUrl, String fallbackUrl) {
        if (view == null || primaryUrl == null) return;

        Image cached = CACHE.get(primaryUrl);
        if (cached != null && !cached.isError()) {
            view.setImage(cached);
            return;
        }

        LOADER.submit(() -> {
            try {
                Image img = new Image(primaryUrl, 0, 0, true, true, true);
                img.errorProperty().addListener((obs, was, isErr) -> {
                    if (isErr && fallbackUrl != null) {
                        try {
                            Image fb = new Image(fallbackUrl, 0, 0, true, true, true);
                            CACHE.put(primaryUrl, fb);
                            Platform.runLater(() -> view.setImage(fb));
                        } catch (Exception ignored) {}
                    }
                });
                CACHE.put(primaryUrl, img);
                Platform.runLater(() -> view.setImage(img));
            } catch (Exception ignored) {
                if (fallbackUrl != null) {
                    try {
                        Image fb = new Image(fallbackUrl, 0, 0, true, true, true);
                        Platform.runLater(() -> view.setImage(fb));
                    } catch (Exception ignored2) {}
                }
            }
        });
    }

    // ── Internals ──────────────────────────────────────────────────────────

    private static String loremFlickr(String tag, int w, int h, String seed) {
        int lock = Math.abs(seed.hashCode()) % 100000;
        return "https://loremflickr.com/" + w + "/" + h + "/" + encode(tag) + "/all?lock=" + lock;
    }

    /**
     * Maps a raw genre string ("Heavy Metal", "K-Pop", "Hip-Hop / Rap") to a
     * comma-separated tag list LoremFlickr understands.
     */
    private static String baseGenreTag(String genre) {
        if (genre == null) return "music";
        String g = genre.toLowerCase(Locale.ROOT);
        if (g.contains("metal")) return "metal,heavy,guitar";
        if (g.contains("rock"))  return "rock,guitar,band";
        if (g.contains("pop") && g.contains("k"))   return "kpop,korean,pop";
        if (g.contains("pop"))   return "pop,music,bright";
        if (g.contains("hip") || g.contains("rap")) return "hiphop,rap,urban";
        if (g.contains("jazz"))  return "jazz,saxophone,blue";
        if (g.contains("blues")) return "blues,guitar,vintage";
        if (g.contains("electro") || g.contains("edm") || g.contains("techno") || g.contains("house"))
            return "electronic,neon,club";
        if (g.contains("chill") || g.contains("lofi") || g.contains("ambient"))
            return "chill,sunset,beach";
        if (g.contains("classic"))  return "orchestra,classical,piano";
        if (g.contains("country"))  return "country,guitar,road";
        if (g.contains("folk"))     return "folk,acoustic,nature";
        if (g.contains("reggae"))   return "reggae,beach,sunset";
        if (g.contains("punk"))     return "punk,leather,graffiti";
        if (g.contains("indie"))    return "indie,vintage,band";
        if (g.contains("arab") || g.contains("oriental") || g.contains("tunis"))
            return "tunisia,oud,desert";
        return "music,band";
    }

    private static String guessPostTags(String title) {
        if (title == null) return "music,blog";
        String t = title.toLowerCase(Locale.ROOT);
        if (t.contains("concert") || t.contains("live")) return "concert,stage";
        if (t.contains("review"))                         return "music,review,vinyl";
        if (t.contains("interview"))                      return "interview,microphone";
        if (t.contains("album"))                          return "vinyl,album,music";
        if (t.contains("festival"))                       return "festival,crowd,sunset";
        if (t.contains("tour"))                           return "tour,backstage,music";
        if (t.contains("song") || t.contains("track"))    return "music,headphones";
        return "music,blog";
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "_");
    }
}
