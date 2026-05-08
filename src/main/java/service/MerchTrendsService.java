package service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shop sidebar: live iTunes chart signal — trending album artists + dominant singles genres.
 * No API keys; uses {@link ItunesService} RSS endpoints.
 */
public final class MerchTrendsService {

    private MerchTrendsService() {}

    public record TrendingArtist(String name, String blurb) {}

    public record PanelData(String genresLine, List<TrendingArtist> artists) {}

    /** ISO-2 storefront (e.g. tn, us). Invalid → {@code us}. */
    public static PanelData fetch(String iso2) {
        String c = normalizeIso(iso2);
        String genres = ItunesService.summarizeTrendingGenres(c);
        List<TrendingArtist> artists = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ItunesService.TopAlbum a : ItunesService.topAlbums(c, 28)) {
            if (a.artist() == null || a.artist().isBlank()) continue;
            if (!seen.add(a.artist().toLowerCase(Locale.ROOT))) continue;
            String blurb = "Chart heat with «" + shorten(a.name(), 44) + "» right now.";
            artists.add(new TrendingArtist(a.artist(), blurb));
            if (artists.size() >= 6) break;
        }
        return new PanelData(genres, artists);
    }

    public static String defaultCountryCode() {
        String cc = Locale.getDefault().getCountry();
        return (cc != null && cc.length() == 2) ? cc.toLowerCase(Locale.ROOT) : "us";
    }

    private static String normalizeIso(String iso2) {
        if (iso2 == null || iso2.length() != 2) return "us";
        return iso2.toLowerCase(Locale.ROOT).trim();
    }

    private static String shorten(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
