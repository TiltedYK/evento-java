package service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Live weather forecast using Open-Meteo API (free, no API key required).
 * Step 1: geocode city → lat/lon via geocoding-api.open-meteo.com
 * Step 2: fetch daily forecast for the event date via api.open-meteo.com
 */
public class OpenMeteoService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public record Forecast(String city, LocalDate date,
                           double maxTemp, double minTemp,
                           String condition, String emoji) {
        @Override public String toString() {
            return emoji + "  " + condition + "  " + String.format("%.0f°C / %.0f°C", maxTemp, minTemp);
        }
    }

    public static Forecast getForecast(String city, LocalDate date) {
        try {
            double[] coords = geocode(city);
            LocalDate today = LocalDate.now();
            long daysAway = java.time.temporal.ChronoUnit.DAYS.between(today, date);

            if (daysAway < -1) {
                // Past event — show current weather for the city instead of 0/0
                return fetchForecast(city, coords[0], coords[1], today);
            }
            if (daysAway > 15) {
                // Beyond Open-Meteo 16-day forecast window — show current weather
                return fetchForecast(city, coords[0], coords[1], today);
            }
            return fetchForecast(city, coords[0], coords[1], date);
        } catch (Exception e) {
            return fetchFallbackTunis(city, date);
        }
    }

    // Default coordinates: Tunis, Tunisia — used when geocoding fails
    private static final double DEFAULT_LAT = 36.8065;
    private static final double DEFAULT_LON = 10.1815;

    private static double[] geocode(String city) throws Exception {
        if (city == null || city.isBlank()) return new double[]{DEFAULT_LAT, DEFAULT_LON};
        // Strip common venue suffixes to improve geocoding hit rate
        String simplified = city.replaceAll("(?i)amphitheater|amphitheatre|amphithéâtre|theater|theatre|arena|hall|center|centre|stadium", "").trim();
        if (simplified.isBlank()) simplified = city.trim();

        String encoded = URLEncoder.encode(simplified, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=en&format=json";
        String body = get(url);
        if (body != null && body.contains("\"latitude\"")) {
            double lat = extractDouble(body, "latitude");
            double lon = extractDouble(body, "longitude");
            if (lat != 0 || lon != 0) return new double[]{lat, lon};
        }
        // Fallback: Tunis coordinates
        return new double[]{DEFAULT_LAT, DEFAULT_LON};
    }

    private static Forecast fetchForecast(String city, double lat, double lon, LocalDate date) throws Exception {
        String d = date.toString();
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&daily=weathercode,temperature_2m_max,temperature_2m_min"
                + "&timezone=auto&start_date=" + d + "&end_date=" + d;
        String body = get(url);
        if (body == null) return fallback(city, date);

        // The response has the requested keys in BOTH "daily_units" (string °C)
        // AND "daily" (number array). We MUST scope the lookup to the daily
        // block, otherwise extractDouble returns 0 for everything.
        String dailySection = sliceObject(body, "\"daily\":");
        if (dailySection == null || dailySection.isEmpty()) dailySection = body;

        int code   = (int) extractDouble(dailySection, "weathercode");
        double max = extractDouble(dailySection, "temperature_2m_max");
        double min = extractDouble(dailySection, "temperature_2m_min");
        String cond  = codeToCondition(code);
        String emoji = codeToEmoji(code);
        return new Forecast(city, date, max, min, cond, emoji);
    }

    private static String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url)).timeout(Duration.ofSeconds(6))
                .header("User-Agent", "EventoApp/1.0").GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200 ? resp.body() : null;
    }

    /**
     * Returns the substring covering the JSON object whose key matches
     * {@code keyHeader} (e.g. {@code "\"daily\":"}). Null if not found.
     * The slice covers the matching pair of braces so {@link #extractDouble}
     * only sees the values from that scope.
     */
    private static String sliceObject(String json, String keyHeader) {
        int i = json.indexOf(keyHeader);
        if (i < 0) return null;
        int start = json.indexOf('{', i + keyHeader.length());
        if (start < 0) return null;
        int depth = 0;
        for (int p = start; p < json.length(); p++) {
            char c = json.charAt(p);
            if (c == '{') depth++;
            else if (c == '}') {
                if (--depth == 0) return json.substring(start, p + 1);
            }
        }
        return null;
    }

    /** Reads the first numeric value associated with {@code key} (handles array
     *  literals, leading whitespace, and {@code null}/string values returning 0). */
    private static double extractDouble(String json, String key) {
        String s = "\"" + key + "\":";
        int idx = json.indexOf(s);
        if (idx < 0) return 0;
        int start = idx + s.length();
        // Skip past array brackets, whitespace, newlines.
        while (start < json.length()) {
            char c = json.charAt(start);
            if (c == '[' || c == ' ' || c == '\t' || c == '\n' || c == '\r') start++;
            else break;
        }
        if (start >= json.length()) return 0;
        // If we hit a string or null, bail.
        char first = json.charAt(start);
        if (first == '"' || first == 'n') return 0;

        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == 'e' || c == 'E' || c == '+') end++;
            else break;
        }
        if (end == start) return 0;
        try { return Double.parseDouble(json.substring(start, end)); } catch (Exception e) { return 0; }
    }

    private static String codeToCondition(int code) {
        if (code == 0)            return "Clear Sky";
        if (code <= 3)            return "Partly Cloudy";
        if (code <= 48)           return "Foggy";
        if (code <= 57)           return "Drizzle";
        if (code <= 67)           return "Rain";
        if (code <= 77)           return "Snow";
        if (code <= 82)           return "Rain Showers";
        if (code == 95)           return "Thunderstorm";
        return "Mixed";
    }

    private static String codeToEmoji(int code) {
        if (code == 0)  return "☀";
        if (code <= 3)  return "⛅";
        if (code <= 48) return "🌫";
        if (code <= 57) return "🌦";
        if (code <= 67) return "🌧";
        if (code <= 77) return "❄";
        if (code <= 82) return "🌦";
        if (code == 95) return "⛈";
        return "🌤";
    }

    private static Forecast fetchFallbackTunis(String city, LocalDate date) {
        try {
            return fetchForecast(city, DEFAULT_LAT, DEFAULT_LON, date);
        } catch (Exception e) {
            return new Forecast(city, date, 0, 0, "Unavailable", "—");
        }
    }

    @SuppressWarnings("unused")
    private static Forecast fallback(String city, LocalDate date) {
        return fetchFallbackTunis(city, date);
    }
}
