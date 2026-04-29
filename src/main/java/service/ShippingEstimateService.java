package service;

import java.util.Locale;
import java.util.Set;

/**
 * Zone-based shipping estimator. Uses {@link GeoLocationService} to figure out
 * the buyer's country, then maps it to a shipping zone with a flat fee in TND
 * and a delivery ETA window. Pure data, no extra network calls — combines
 * neatly with the live currency rates already in {@link CurrencyService}.
 *
 * Zones (TND):
 *   <ul>
 *     <li>HOME       — Tunisia: free over 80 TND, otherwise 4 TND</li>
 *     <li>MAGHREB    — DZ/MA/LY/EG/MR: 12 TND, 5–9 days</li>
 *     <li>EUROPE     — EU + UK + CH: 22 TND, 6–12 days</li>
 *     <li>NORTH_AMER — US/CA: 38 TND, 8–14 days</li>
 *     <li>WORLD      — everything else: 45 TND, 10–18 days</li>
 *   </ul>
 *
 * Build tag: {@code EVENTO_SHIPPING_BUILD_2026_04_28_ZONES_V1}
 */
public final class ShippingEstimateService {

    public static final String BUILD_TAG = "EVENTO_SHIPPING_BUILD_2026_04_28_ZONES_V1";

    private ShippingEstimateService() {}

    private static final Set<String> MAGHREB =
            Set.of("DZ", "MA", "LY", "EG", "MR", "SD");

    private static final Set<String> EUROPE = Set.of(
            "FR","DE","IT","ES","PT","BE","NL","AT","GR","IE","FI",
            "SE","DK","NO","CH","UK","GB","PL","CZ","SK","HU","RO",
            "BG","HR","SI","LU","EE","LV","LT","CY","MT","IS"
    );

    private static final Set<String> NORTH_AMERICA = Set.of("US", "CA");

    public enum Zone {
        HOME("HOME — Tunisia",        0,  4,  "1–3 days",   "🏠"),
        MAGHREB("MAGHREB",            12, 12, "5–9 days",   "🌍"),
        EUROPE("EUROPE",              22, 22, "6–12 days",  "✈"),
        NORTH_AMERICA("NORTH AMERICA",38, 38, "8–14 days",  "✈"),
        WORLD("REST OF WORLD",        45, 45, "10–18 days", "🌐");

        public final String label;
        public final double minTnd;
        public final double maxTnd;
        public final String eta;
        public final String icon;

        Zone(String label, double minTnd, double maxTnd, String eta, String icon) {
            this.label = label;
            this.minTnd = minTnd;
            this.maxTnd = maxTnd;
            this.eta = eta;
            this.icon = icon;
        }
    }

    public record Estimate(Zone zone, double feeTnd, boolean freeShippingApplied,
                           String eta, String countryCode, String country) {}

    /**
     * Computes an estimate using the auto-detected location + current cart total.
     * Tunisian customers get free shipping above 80 TND.
     */
    public static Estimate forCart(double cartTotalTnd) {
        GeoLocationService.Location loc = GeoLocationService.detect();
        String code = loc.countryCode() == null ? "TN" : loc.countryCode().toUpperCase(Locale.ROOT);
        Zone z = zoneFor(code);
        double fee = z.minTnd;
        boolean free = false;
        if (z == Zone.HOME && cartTotalTnd >= 80.0) {
            fee = 0;
            free = true;
        }
        return new Estimate(z, fee, free, z.eta, code, loc.country());
    }

    public static Zone zoneFor(String iso2) {
        if (iso2 == null) return Zone.WORLD;
        String c = iso2.toUpperCase(Locale.ROOT);
        if (c.equals("TN")) return Zone.HOME;
        if (MAGHREB.contains(c)) return Zone.MAGHREB;
        if (EUROPE.contains(c)) return Zone.EUROPE;
        if (NORTH_AMERICA.contains(c)) return Zone.NORTH_AMERICA;
        return Zone.WORLD;
    }
}
