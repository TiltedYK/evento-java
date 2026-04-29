package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Detects the user's approximate location using ipapi.co (free, no API key).
 * Used on the Promotions page to tag locally relevant campaigns with "Near you".
 */
public class GeoLocationService {

    private static final String URL = "https://ipapi.co/json/";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public record Location(String city, String country, String countryCode, String region) {
        public String display() {
            if (city != null && !city.isBlank()) return city + ", " + country;
            return country != null ? country : "Unknown";
        }
    }

    private static Location cached = null;

    public static Location getLocation() {
        if (cached != null) return cached;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                cached = parse(resp.body());
                return cached;
            }
        } catch (Exception ignored) {}
        cached = new Location("Tunis", "Tunisia", "TN", "Tunis");
        return cached;
    }

    private static Location parse(String json) {
        String city        = field(json, "city");
        String country     = field(json, "country_name");
        String countryCode = field(json, "country_code");
        String region      = field(json, "region");
        return new Location(
                city        != null ? city        : "Tunis",
                country     != null ? country     : "Tunisia",
                countryCode != null ? countryCode : "TN",
                region      != null ? region      : "");
    }

    private static String field(String json, String key) {
        String s = "\"" + key + "\": \"";
        int idx = json.indexOf(s);
        if (idx < 0) { s = "\"" + key + "\":\""; idx = json.indexOf(s); }
        if (idx < 0) return null;
        int start = idx + s.length();
        int end   = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : null;
    }
}
