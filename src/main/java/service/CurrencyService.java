package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fetches live USD-base exchange rates from open.er-api.com (free, no API key).
 * Converts TND prices to multiple currencies for display in the shop.
 *
 * Build tag: {@code EVENTO_CURRENCY_BUILD_2026_04_28_MULTI_CCY_V2}
 */
public class CurrencyService {

    public static final String BUILD_TAG = "EVENTO_CURRENCY_BUILD_2026_04_28_MULTI_CCY_V2";

    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";
    private static Map<String, Double> cachedRates = null;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Currencies supported in the shop UI. The list is ordered for the dropdown.
     * Each entry: code → display data (flag, symbol, prefix-or-suffix).
     */
    public static final LinkedHashMap<String, CurrencyInfo> SUPPORTED;
    static {
        SUPPORTED = new LinkedHashMap<>();
        SUPPORTED.put("TND", new CurrencyInfo("TND", "🇹🇳", "TND", "%.2f TND", false));
        SUPPORTED.put("EUR", new CurrencyInfo("EUR", "🇪🇺", "€",   "≈ %.2f €",  true));
        SUPPORTED.put("USD", new CurrencyInfo("USD", "🇺🇸", "$",   "≈ $%.2f",   true));
        SUPPORTED.put("GBP", new CurrencyInfo("GBP", "🇬🇧", "£",   "≈ £%.2f",   true));
        SUPPORTED.put("JPY", new CurrencyInfo("JPY", "🇯🇵", "¥",   "≈ ¥%.0f",   true));
        SUPPORTED.put("KRW", new CurrencyInfo("KRW", "🇰🇷", "₩",   "≈ ₩%.0f",   true));
        SUPPORTED.put("CAD", new CurrencyInfo("CAD", "🇨🇦", "C$",  "≈ C$%.2f",  true));
        SUPPORTED.put("AUD", new CurrencyInfo("AUD", "🇦🇺", "A$",  "≈ A$%.2f",  true));
        SUPPORTED.put("CHF", new CurrencyInfo("CHF", "🇨🇭", "CHF", "≈ %.2f CHF",false));
    }

    public record CurrencyInfo(String code, String flag, String symbol,
                               String fmt, boolean approx) {
        public String label() { return flag + "  " + code; }
    }

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
        // pivot: "1 TND = X foreign currency"
        for (String code : SUPPORTED.keySet()) {
            if ("TND".equals(code)) { result.put("TND", 1.0); continue; }
            double rateVsUsd = extractDouble(json, code);
            if (rateVsUsd > 0) result.put(code, rateVsUsd / tndPerUsd);
        }
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
        // hand-tuned recent approximate rates relative to TND (1 TND = X)
        m.put("TND", 1.0);
        m.put("USD", 0.326);
        m.put("EUR", 0.299);
        m.put("GBP", 0.257);
        m.put("JPY", 49.5);
        m.put("KRW", 433.0);
        m.put("CAD", 0.443);
        m.put("AUD", 0.498);
        m.put("CHF", 0.286);
        return m;
    }

    /**
     * Returns a "≈ X €" / "≈ ¥123" / etc. string for the given TND amount.
     * Returns {@code ""} when the rate is unknown.
     */
    public String convertLabel(double tndAmount, String currency) {
        Double rate = getRates().get(currency);
        if (rate == null || rate <= 0) return "";
        double converted = tndAmount * rate;
        CurrencyInfo info = SUPPORTED.get(currency);
        if (info == null) return String.format("%.2f %s", converted, currency);
        return String.format(java.util.Locale.US, info.fmt, converted);
    }

    /** Same as {@link #convertLabel} but always with the leading "≈" stripped. */
    public String convertPlain(double tndAmount, String currency) {
        String s = convertLabel(tndAmount, currency);
        return s.startsWith("≈ ") ? s.substring(2) : s;
    }
}
