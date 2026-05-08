package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches OpenGraph meta-tags from any URL (free, no API key).
 * Used to generate rich ad previews for Collaboration campaigns.
 */
public class LinkPreviewService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public record Preview(String title, String description, String imageUrl, String siteName, String url) {}

    public static Preview fetch(String url) {
        if (url == null || url.isBlank()) return empty(url);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Mozilla/5.0 (compatible; EventoBot/1.0)")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return parse(resp.body(), url);
            }
        } catch (Exception ignored) {}
        return empty(url);
    }

    private static Preview parse(String html, String url) {
        String title       = og(html, "og:title");
        String description = og(html, "og:description");
        String imageUrl    = og(html, "og:image");
        String siteName    = og(html, "og:site_name");

        if (title       == null) title       = tag(html, "<title>", "</title>");
        if (description == null) description = meta(html, "description");
        if (siteName    == null) siteName    = domain(url);

        return new Preview(
                clean(title,       "Sponsored"),
                clean(description, "Visit the partner page for more information."),
                imageUrl,
                clean(siteName, ""),
                url);
    }

    private static String og(String html, String property) {
        String s = "property=\"" + property + "\"";
        int idx = html.indexOf(s);
        if (idx < 0) { s = "property='" + property + "'"; idx = html.indexOf(s); }
        if (idx < 0) return null;
        return contentAttr(html, idx + s.length());
    }

    private static String meta(String html, String name) {
        String s = "name=\"" + name + "\"";
        int idx = html.indexOf(s);
        if (idx < 0) return null;
        return contentAttr(html, idx + s.length());
    }

    private static String contentAttr(String html, int from) {
        int tagEnd = html.indexOf('>', from);
        if (tagEnd < 0) return null;
        String tag = html.substring(from, Math.min(tagEnd + 1, html.length()));
        int ci = tag.indexOf("content=\"");
        if (ci >= 0) {
            int start = ci + 9, end = tag.indexOf('"', start);
            return end > start ? unescape(tag.substring(start, end)) : null;
        }
        ci = tag.indexOf("content='");
        if (ci >= 0) {
            int start = ci + 9, end = tag.indexOf('\'', start);
            return end > start ? unescape(tag.substring(start, end)) : null;
        }
        return null;
    }

    private static String tag(String html, String open, String close) {
        int start = html.indexOf(open);
        if (start < 0) return null;
        start += open.length();
        int end = html.indexOf(close, start);
        return end > start ? unescape(html.substring(start, end).replaceAll("<[^>]+>", "")) : null;
    }

    private static String domain(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : "";
        } catch (Exception e) { return ""; }
    }

    private static String unescape(String s) {
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ");
    }

    private static String clean(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s.trim();
    }

    private static Preview empty(String url) {
        return new Preview("Sponsored Campaign", "Click to learn more.", null, domain(url != null ? url : ""), url != null ? url : "");
    }
}
