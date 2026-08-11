package com.zainab.roamSafe.service;

/**
 * US4 (airport arrival): a traveler searches "Istanbul Airport" or
 * "Barcelona airport" expecting arrival guidance. The airport-to-city
 * transport, SIM and currency intelligence already lives on the city's
 * "Arriving &amp; getting around" tab, so we normalize an airport-style
 * query down to its base city and let the destination page open there.
 *
 * <p>Deliberately conservative: it only strips an explicit trailing
 * "airport" (optionally "international airport"). It does not guess from
 * three-letter IATA codes, which collide with real place names and would
 * risk sending someone to the wrong city.
 */
public final class AirportQuery {

    private AirportQuery() {
    }

    /** True when the query reads as an airport rather than a city. */
    public static boolean isAirport(String query) {
        if (query == null) {
            return false;
        }
        String q = query.trim().toLowerCase();
        return q.endsWith(" airport") || q.endsWith(" international airport");
    }

    /**
     * The base city for an airport query ("Istanbul Airport" -&gt; "Istanbul").
     * Returns the input unchanged when it is not an airport query.
     */
    public static String toCity(String query) {
        if (!isAirport(query)) {
            return query;
        }
        String q = query.trim();
        // Strip the trailing "airport" (and an optional preceding "international").
        String stripped = q.replaceAll("(?i)\\s+(international\\s+)?airport$", "").trim();
        return stripped.isEmpty() ? query : stripped;
    }
}
