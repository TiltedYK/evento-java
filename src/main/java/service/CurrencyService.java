package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches live USD-base exchange rates from open.er-api.com (free, no API key).
 * Converts TND prices to EUR, USD, and GBP for display in the shop.
 */
public class CurrencyService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";
    private static Map<String, Double> cachedRates = null;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public Map<String, Double> getRates() {
        if (cachedRates != null) return cachedRates;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(6))
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                cachedRates = parseRates(resp.body());
                return cachedRates;
            }
        } catch (Exception ignored) {}
        cachedRates = fallback();
        return cachedRates;
    }

    private Map<String, Double> parseRates(String json) {
        // JSON: {"rates":{"EUR":0.92,"TND":3.07,"USD":1.0,...}}
        double tndPerUsd = extractDouble(json, "TND");
        if (tndPerUsd <= 0) tndPerUsd = 3.07;

        Map<String, Double> result = new HashMap<>();
        for (String code : new String[]{"EUR", "USD", "GBP"}) {
            double rateVsUsd = extractDouble(json, code);
            if (rateVsUsd > 0)
                result.put(code, rateVsUsd / tndPerUsd); // 1 TND → target currency
        }
        result.put("TND", 1.0);
        return result;
    }

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return -1;
        int start = idx + search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (Exception e) { return -1; }
    }

    private Map<String, Double> fallback() {
        Map<String, Double> m = new HashMap<>();
        m.put("TND", 1.0);
        m.put("USD", 0.326);
        m.put("EUR", 0.299);
        m.put("GBP", 0.257);
        return m;
    }

    public String convertLabel(double tndAmount, String currency) {
        Double rate = getRates().get(currency);
        if (rate == null || rate <= 0) return "";
        double converted = tndAmount * rate;
        return switch (currency) {
            case "EUR" -> String.format("≈ %.2f €",  converted);
            case "USD" -> String.format("≈ $%.2f",   converted);
            case "GBP" -> String.format("≈ £%.2f",   converted);
            default    -> String.format("%.2f %s", converted, currency);
        };
    }
}
