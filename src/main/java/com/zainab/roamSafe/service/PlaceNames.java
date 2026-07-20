package com.zainab.roamSafe.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns the free-text neighborhood field into usable place names.
 *
 * The importer wrote whatever the source paragraph mentioned, which produced two
 * problems. Section headings landed in the field as if they were districts, so
 * "avoid Stay safe after dark" was a sentence the product could generate. And
 * several places were crammed into one value - "La Rambla, Raval, public
 * transport, train and bus stations" - which hid real coverage, because a search
 * for La Rambla matched none of it.
 *
 * Splitting those apart is not inventing data: the source really did report the
 * incident against each of those places. What is invented is a heading like
 * "Crime" being treated as somewhere you can stand, and that is filtered out.
 */
public final class PlaceNames {

    private PlaceNames() {
    }

    /** Headings and topics the importer stored as if they were districts. */
    private static final Set<String> NOT_A_PLACE = Set.of(
            "stay safe", "safety", "crime", "crimes", "scam", "scams", "theft", "pickpocketing",
            "police", "emergency", "emergencies", "health", "natural disasters", "general",
            "drugs", "terrorism", "racism", "anti-semitism", "homophobia", "women", "lgbt",
            "solo travel", "transport", "getting around", "respect", "cope", "connect",
            "traffic", "road safety", "corruption", "begging", "harassment",
            "violent crimes", "reporting crimes", "football", "cars", "atm", "atms",
            "public transport", "public transportation", "subway", "metro", "buses", "trains",
            "taxis", "nightlife", "beaches", "tourist areas", "certain tourist hotspots",
            "areas of caution", "tourist hotspots", "restaurants", "bars", "hotels", "markets",
            "train and bus stations", "bus stations", "train stations", "airports", "airport",
            "shopping malls", "malls", "parks", "streets", "city centre", "city center",
            "downtown", "old town", "tourist attractions", "attractions", "landmarks");

    /** Phrases like "Theft in public transit" describe a risk, not a district. */
    private static final List<String> TOPIC_PREFIXES = List.of(
            "theft ", "scam", "crime ", "pickpocket", "robbery ", "mugging ",
            "harassment ", "violence ", "fraud ", "assault ", "avoid ", "beware ");

    /**
     * The distinct places named in one neighborhood value, in order.
     *
     * A value listing several places yields several names; a heading yields none.
     */
    public static List<String> extract(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String part : raw.split("[,;/]| and (?=[A-Z])")) {
            String name = tidy(part);
            if (name != null && seen.add(name.toLowerCase(Locale.ROOT))) {
                out.add(name);
            }
        }
        return out;
    }

    /** A cleaned place name, or null when the fragment isn't a place. */
    private static String tidy(String fragment) {
        String name = fragment.trim().replaceAll("^(the|in|near|around|at)\\s+", "").trim();
        // Trailing punctuation and stray articles left by the split.
        name = name.replaceAll("[.!?]+$", "").trim();
        if (name.length() < 3 || name.length() > 45) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (NOT_A_PLACE.contains(lower)) {
            return null;
        }
        for (String prefix : TOPIC_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return null;
            }
        }
        // A fragment with no letters, or one that reads as a sentence, isn't a name.
        if (!name.matches(".*[A-Za-z].*") || name.split("\\s+").length > 5) {
            return null;
        }
        return name;
    }

    /** True when the whole value names at least one real place. */
    public static boolean isRealPlace(String raw) {
        return !extract(raw).isEmpty();
    }
}
