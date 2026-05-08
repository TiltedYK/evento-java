package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Fetches current weather for a city using the free wttr.in API (no key required).
 * Returns a short human-readable string like "Sunny +22°C".
 */
public class WeatherService {

    private static final String BASE_URL = "https://wttr.in/%s?format=%%C+%%t&m";
    private static final int TIMEOUT_MS = 5000;

    public String getWeather(String city) {
        if (city == null || city.isBlank()) return "No location provided";
        try {
            String encoded = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
            URI uri = URI.create(String.format(BASE_URL, encoded));
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "curl/7.68.0");
            if (conn.getResponseCode() != 200) return "Weather unavailable";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line = br.readLine();
                return (line != null && !line.isBlank()) ? line.trim() : "N/A";
            }
        } catch (Exception e) {
            return "Weather unavailable";
        }
    }
}
