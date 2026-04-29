package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Detects the visitor's country / city using the free public IP geolocation
 * service at <a href="http://ip-api.com/json/">ip-api.com</a> (no key required).
 *
 * The result is cached for the lifetime of the JVM — the IP/country never
 * changes during a session, so we only ever do one network call.
 *
 * Build tag: {@code EVENTO_GEO_BUILD_2026_04_28_IPAPI_V2}
 */
public final class GeoLocationService {

    public static final String BUILD_TAG = "EVENTO_GEO_BUILD_2026_04_28_IPAPI_V2";

    public record Location(String country,
                           String countryCode, // ISO-2 like "TN", "FR"
                           String city,
                           String regionName,
                           String isp,
                           double lat,
                           double lon) {

        public boolean isValid() {
            return countryCode != null && !countryCode.isBlank();
        }

        /** Pretty short form for UI labels — used by the front promo page. */
        public String display() {
            String c = country == null ? "" : country;
            String t = city == null ? "" : city;
            if (c.isBlank() && t.isBlank()) return "Unknown location";
            if (c.isBlank()) return t;
            if (t.isBlank()) return c;
            return t + ", " + c;
        }
    }

    private static volatile Location cached;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    private GeoLocationService() {}

    /** Primary entry point. Cached after first successful call. */
    public static Location detect() {
        if (cached != null) return cached;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/?fields=status,country,countryCode,region,regionName,city,lat,lon,isp"))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Location l = parse(resp.body());
                if (l != null && l.isValid()) {
                    cached = l;
                    return l;
                }
            }
        } catch (Exception ignored) {}
        cached = new Location("Tunisia", "TN", "Tunis", "Tunis", "—", 36.8, 10.18);
        return cached;
    }

    /** Backwards-compat alias used by older controllers (e.g. FrontPromo). */
    public static Location getLocation() {
        return detect();
    }

    private static Location parse(String json) {
        if (!json.contains("\"status\":\"success\"")) return null;
        return new Location(
                stringField(json, "country"),
                stringField(json, "countryCode"),
                stringField(json, "city"),
                stringField(json, "regionName"),
                stringField(json, "isp"),
                doubleField(json, "lat"),
                doubleField(json, "lon"));
    }

    private static String stringField(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s);
        if (i < 0) return "";
        int start = i + s.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }

    private static double doubleField(String json, String key) {
        String s = "\"" + key + "\":";
        int i = json.indexOf(s);
        if (i < 0) return 0;
        int start = i + s.length();
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end))
                    || json.charAt(end) == '.'
                    || json.charAt(end) == '-')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (Exception e) { return 0; }
    }

    /** Maps an ISO-2 country code to the "best matching" merch currency. */
    public static String suggestCurrency(String iso2) {
        if (iso2 == null) return "TND";
        return switch (iso2.toUpperCase()) {
            case "TN", "DZ", "MA", "LY"             -> "TND";
            case "US", "CA"                          -> "USD";
            case "GB", "UK"                          -> "GBP";
            case "JP"                                -> "JPY";
            case "KR"                                -> "KRW";
            case "AU"                                -> "AUD";
            case "FR", "DE", "IT", "ES", "PT", "BE",
                 "NL", "AT", "GR", "IE", "FI"       -> "EUR";
            default                                  -> "EUR";
        };
    }

    /** Best-guess flag emoji for an ISO-2 country code (e.g. "TN" → 🇹🇳). */
    public static String flagEmoji(String iso2) {
        if (iso2 == null || iso2.length() != 2) return "🌍";
        int base = 0x1F1E6 - 'A';
        int c1 = base + Character.toUpperCase(iso2.charAt(0));
        int c2 = base + Character.toUpperCase(iso2.charAt(1));
        return new String(Character.toChars(c1)) + new String(Character.toChars(c2));
    }
}
