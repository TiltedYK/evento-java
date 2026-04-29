package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public-holidays lookup using the free Nager.Date API
 * (<a href="https://date.nager.at/api/v3/PublicHolidays/{year}/{country}">docs</a>).
 *
 * No API key required. Cached per (country, year) for the JVM lifetime.
 *
 * Build tag: {@code EVENTO_HOLIDAY_BUILD_2026_04_28_NAGER_V1}
 */
public final class HolidayService {

    public static final String BUILD_TAG = "EVENTO_HOLIDAY_BUILD_2026_04_28_NAGER_V1";

    private HolidayService() {}

    public record Holiday(LocalDate date, String localName, String name) {
        public long daysFromNow() {
            return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
        }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    private static final ConcurrentHashMap<String, List<Holiday>> CACHE = new ConcurrentHashMap<>();

    public static List<Holiday> forCountry(String iso2, int year) {
        if (iso2 == null || iso2.isBlank()) iso2 = "TN";
        String code = iso2.toUpperCase();
        String key = code + ":" + year;
        List<Holiday> cached = CACHE.get(key);
        if (cached != null) return cached;

        try {
            String url = "https://date.nager.at/api/v3/PublicHolidays/" + year + "/" + code;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(7))
                    .header("User-Agent", "EventoApp/1.0")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                List<Holiday> parsed = parse(resp.body());
                CACHE.put(key, parsed);
                return parsed;
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    /** Returns the next upcoming holiday for the country, or null. */
    public static Holiday nextHoliday(String iso2) {
        LocalDate today = LocalDate.now();
        List<Holiday> all = forCountry(iso2, today.getYear());
        // Also peek into next year so December queries don't return null.
        if (all.isEmpty() || all.stream().allMatch(h -> h.date().isBefore(today))) {
            all = new ArrayList<>(all);
            all.addAll(forCountry(iso2, today.getYear() + 1));
        }
        return all.stream()
                .filter(h -> !h.date().isBefore(today))
                .min(java.util.Comparator.comparing(Holiday::date))
                .orElse(null);
    }

    private static List<Holiday> parse(String json) {
        List<Holiday> out = new ArrayList<>();
        // Each entry: {"date":"2026-12-25","localName":"...","name":"...", ...}
        int idx = 0;
        while ((idx = json.indexOf("\"date\":\"", idx)) >= 0) {
            int end = json.indexOf('"', idx + 8);
            if (end < 0) break;
            String dateStr = json.substring(idx + 8, end);
            String local  = readField(json, end, "localName");
            String name   = readField(json, end, "name");
            try {
                LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                out.add(new Holiday(d, local, name));
            } catch (Exception ignored) {}
            idx = end + 1;
        }
        return out;
    }

    private static String readField(String json, int from, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s, from);
        if (i < 0 || i > from + 250) return "";
        int start = i + s.length();
        int end = start;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\' && end + 1 < json.length()) end += 2;
            else end++;
        }
        return json.substring(start, end);
    }

    /**
     * One-line marketing-friendly tagline:
     *   "🎉 In 5 days: Independence Day — gear up!"
     */
    public static String marketingTagline(String iso2) {
        Holiday h = nextHoliday(iso2);
        if (h == null) return "";
        long days = h.daysFromNow();
        String name = (h.localName() == null || h.localName().isBlank()) ? h.name() : h.localName();
        if (days == 0) return "🎉  Today: " + name + "  ·  celebrate with merch!";
        if (days == 1) return "🎉  Tomorrow: " + name + "  ·  last call for gifts!";
        if (days <= 14) return "🎉  In " + days + " days: " + name + "  ·  shop the holiday drop!";
        return "📅  Next holiday: " + name + "  (in " + days + " days)";
    }
}
