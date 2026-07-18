package com.zainab.roamSafe.service;

import java.util.Map;
import java.util.Optional;

/**
 * Maps the cities we actually hold data for to their country and the GOV.UK
 * foreign-travel-advice slug for that country.
 *
 * The {@code cities} table was polluted by an earlier Reddit import (rows like
 * "Africa|and my first time..."), so country lookup is driven by this curated
 * map keyed on the clean city names that appear in {@code scam_reports}, not by
 * that table. Every slug here was verified against the live GOV.UK content API.
 */
public final class CountryLookup {

    private CountryLookup() {
    }

    /** country display name + GOV.UK travel-advice slug. */
    public record Country(String name, String govUkSlug) {
    }

    private static final Map<String, Country> BY_CITY = Map.ofEntries(
            Map.entry("amsterdam", new Country("Netherlands", "netherlands")),
            Map.entry("athens", new Country("Greece", "greece")),
            Map.entry("auckland", new Country("New Zealand", "new-zealand")),
            Map.entry("bali", new Country("Indonesia", "indonesia")),
            Map.entry("bangkok", new Country("Thailand", "thailand")),
            Map.entry("barcelona", new Country("Spain", "spain")),
            Map.entry("berlin", new Country("Germany", "germany")),
            Map.entry("bogota", new Country("Colombia", "colombia")),
            Map.entry("bruges", new Country("Belgium", "belgium")),
            Map.entry("budapest", new Country("Hungary", "hungary")),
            Map.entry("buenos aires", new Country("Argentina", "argentina")),
            Map.entry("cairo", new Country("Egypt", "egypt")),
            Map.entry("cancun", new Country("Mexico", "mexico")),
            Map.entry("cape town", new Country("South Africa", "south-africa")),
            Map.entry("cartagena", new Country("Colombia", "colombia")),
            Map.entry("casablanca", new Country("Morocco", "morocco")),
            Map.entry("cusco", new Country("Peru", "peru")),
            Map.entry("delhi", new Country("India", "india")),
            Map.entry("dubai", new Country("United Arab Emirates", "united-arab-emirates")),
            Map.entry("dublin", new Country("Ireland", "ireland")),
            Map.entry("fiji", new Country("Fiji", "fiji")),
            Map.entry("florence", new Country("Italy", "italy")),
            Map.entry("hanoi", new Country("Vietnam", "vietnam")),
            Map.entry("havana", new Country("Cuba", "cuba")),
            Map.entry("ho chi minh city", new Country("Vietnam", "vietnam")),
            Map.entry("hong kong", new Country("Hong Kong", "hong-kong")),
            Map.entry("istanbul", new Country("Turkey", "turkey")),
            Map.entry("johannesburg", new Country("South Africa", "south-africa")),
            Map.entry("kathmandu", new Country("Nepal", "nepal")),
            Map.entry("krakow", new Country("Poland", "poland")),
            Map.entry("kuala lumpur", new Country("Malaysia", "malaysia")),
            Map.entry("las vegas", new Country("United States", "usa")),
            Map.entry("lima", new Country("Peru", "peru")),
            Map.entry("lisbon", new Country("Portugal", "portugal")),
            Map.entry("london", new Country("United Kingdom", null)),
            Map.entry("los angeles", new Country("United States", "usa")),
            Map.entry("manila", new Country("Philippines", "philippines")),
            Map.entry("marrakech", new Country("Morocco", "morocco")),
            Map.entry("melbourne", new Country("Australia", "australia")),
            Map.entry("mexico city", new Country("Mexico", "mexico")),
            Map.entry("miami", new Country("United States", "usa")),
            Map.entry("mumbai", new Country("India", "india")),
            Map.entry("nairobi", new Country("Kenya", "kenya")),
            Map.entry("new york", new Country("United States", "usa")),
            Map.entry("nice", new Country("France", "france")),
            Map.entry("paris", new Country("France", "france")),
            Map.entry("phuket", new Country("Thailand", "thailand")),
            Map.entry("prague", new Country("Czechia", "czechia")),
            Map.entry("queenstown", new Country("New Zealand", "new-zealand")),
            Map.entry("rio de janeiro", new Country("Brazil", "brazil")),
            Map.entry("rome", new Country("Italy", "italy")),
            Map.entry("san francisco", new Country("United States", "usa")),
            Map.entry("seoul", new Country("South Korea", "south-korea")),
            Map.entry("shanghai", new Country("China", "china")),
            Map.entry("siem reap", new Country("Cambodia", "cambodia")),
            Map.entry("singapore", new Country("Singapore", "singapore")),
            Map.entry("sydney", new Country("Australia", "australia")),
            Map.entry("tel aviv", new Country("Israel", "israel")),
            Map.entry("tokyo", new Country("Japan", "japan")),
            Map.entry("venice", new Country("Italy", "italy")),
            Map.entry("vienna", new Country("Austria", "austria")),
            Map.entry("zanzibar", new Country("Tanzania", "tanzania")));

    public static Optional<Country> forCity(String city) {
        if (city == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CITY.get(city.trim().toLowerCase()));
    }

    /** Distinct countries we can pull advisories for (have a GOV.UK slug). */
    public static java.util.Set<Country> advisoryCountries() {
        java.util.LinkedHashSet<Country> out = new java.util.LinkedHashSet<>();
        for (Country c : BY_CITY.values()) {
            if (c.govUkSlug() != null) {
                out.add(c);
            }
        }
        return out;
    }
}
