package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches a random inspirational quote from quotable.io (free, no API key).
 * Used in the Blog module header.
 */
public class QuoteService {

    private static final String URL =
            "https://api.quotable.io/quotes/random?tags=music|art|creativity&maxLength=180";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** Returns [content, author] or a built-in fallback. */
    public static String[] getRandomQuote() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String body = resp.body();
                String content = field(body, "content");
                String author  = field(body, "author");
                if (content != null && author != null && !content.isBlank())
                    return new String[]{unescape(content), author};
            }
        } catch (Exception ignored) {}
        return FALLBACKS[(int)(Math.random() * FALLBACKS.length)];
    }

    private static String field(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    private static String unescape(String s) {
        return s.replace("\\u2019", "'").replace("\\u2014", "—")
                .replace("\\n", " ").replace("\\\"", "\"");
    }

    private static final String[][] FALLBACKS = {
        {"Music gives a soul to the universe, wings to the mind.", "Plato"},
        {"Without music, life would be a mistake.", "Friedrich Nietzsche"},
        {"One good thing about music: when it hits you, you feel no pain.", "Bob Marley"},
        {"Music is the shorthand of emotion.", "Leo Tolstoy"}
    };
}
