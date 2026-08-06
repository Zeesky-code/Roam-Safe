package com.zainab.roamSafe.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Official entry-requirements / visa link for a destination country.
 *
 * These are links to authoritative government pages, never advice written here:
 * a wrong statement about whether someone needs a visa is the kind of error that
 * strands a traveler at a border, so RoamSafe points at the official source
 * rather than summarising it. The UK FCDO "entry requirements" pages are used
 * for foreign destinations; the UK itself points at the official UK visa
 * checker, since FCDO has no foreign advice for the UK.
 *
 * Every URL in this map was checked to resolve (HTTP 200) when it was added.
 * A country not in the map returns empty, and the UI shows nothing rather than a
 * guessed or dead link.
 */
@Component
public class VisaLinkResolver {

    private static final Map<String, String> LINKS = new LinkedHashMap<>();
    static {
        LINKS.put("Argentina", "https://www.gov.uk/foreign-travel-advice/argentina/entry-requirements");
        LINKS.put("Australia", "https://www.gov.uk/foreign-travel-advice/australia/entry-requirements");
        LINKS.put("Austria", "https://www.gov.uk/foreign-travel-advice/austria/entry-requirements");
        LINKS.put("Belgium", "https://www.gov.uk/foreign-travel-advice/belgium/entry-requirements");
        LINKS.put("Bolivia", "https://www.gov.uk/foreign-travel-advice/bolivia/entry-requirements");
        LINKS.put("Brazil", "https://www.gov.uk/foreign-travel-advice/brazil/entry-requirements");
        LINKS.put("Cambodia", "https://www.gov.uk/foreign-travel-advice/cambodia/entry-requirements");
        LINKS.put("Chile", "https://www.gov.uk/foreign-travel-advice/chile/entry-requirements");
        LINKS.put("China", "https://www.gov.uk/foreign-travel-advice/china/entry-requirements");
        LINKS.put("Colombia", "https://www.gov.uk/foreign-travel-advice/colombia/entry-requirements");
        LINKS.put("Cuba", "https://www.gov.uk/foreign-travel-advice/cuba/entry-requirements");
        LINKS.put("Czechia", "https://www.gov.uk/foreign-travel-advice/czech-republic/entry-requirements");
        LINKS.put("Denmark", "https://www.gov.uk/foreign-travel-advice/denmark/entry-requirements");
        LINKS.put("Ecuador", "https://www.gov.uk/foreign-travel-advice/ecuador/entry-requirements");
        LINKS.put("Egypt", "https://www.gov.uk/foreign-travel-advice/egypt/entry-requirements");
        LINKS.put("Ethiopia", "https://www.gov.uk/foreign-travel-advice/ethiopia/entry-requirements");
        LINKS.put("Fiji", "https://www.gov.uk/foreign-travel-advice/fiji/entry-requirements");
        LINKS.put("Finland", "https://www.gov.uk/foreign-travel-advice/finland/entry-requirements");
        LINKS.put("France", "https://www.gov.uk/foreign-travel-advice/france/entry-requirements");
        LINKS.put("Germany", "https://www.gov.uk/foreign-travel-advice/germany/entry-requirements");
        LINKS.put("Ghana", "https://www.gov.uk/foreign-travel-advice/ghana/entry-requirements");
        LINKS.put("Greece", "https://www.gov.uk/foreign-travel-advice/greece/entry-requirements");
        LINKS.put("Hong Kong", "https://www.gov.uk/foreign-travel-advice/hong-kong/entry-requirements");
        LINKS.put("Hungary", "https://www.gov.uk/foreign-travel-advice/hungary/entry-requirements");
        LINKS.put("India", "https://www.gov.uk/foreign-travel-advice/india/entry-requirements");
        LINKS.put("Indonesia", "https://www.gov.uk/foreign-travel-advice/indonesia/entry-requirements");
        LINKS.put("Ireland", "https://www.gov.uk/foreign-travel-advice/ireland/entry-requirements");
        LINKS.put("Israel", "https://www.gov.uk/foreign-travel-advice/israel/entry-requirements");
        LINKS.put("Italy", "https://www.gov.uk/foreign-travel-advice/italy/entry-requirements");
        LINKS.put("Japan", "https://www.gov.uk/foreign-travel-advice/japan/entry-requirements");
        LINKS.put("Kenya", "https://www.gov.uk/foreign-travel-advice/kenya/entry-requirements");
        LINKS.put("Malaysia", "https://www.gov.uk/foreign-travel-advice/malaysia/entry-requirements");
        LINKS.put("Mexico", "https://www.gov.uk/foreign-travel-advice/mexico/entry-requirements");
        LINKS.put("Morocco", "https://www.gov.uk/foreign-travel-advice/morocco/entry-requirements");
        LINKS.put("Nepal", "https://www.gov.uk/foreign-travel-advice/nepal/entry-requirements");
        LINKS.put("Netherlands", "https://www.gov.uk/foreign-travel-advice/netherlands/entry-requirements");
        LINKS.put("New Zealand", "https://www.gov.uk/foreign-travel-advice/new-zealand/entry-requirements");
        LINKS.put("Peru", "https://www.gov.uk/foreign-travel-advice/peru/entry-requirements");
        LINKS.put("Philippines", "https://www.gov.uk/foreign-travel-advice/philippines/entry-requirements");
        LINKS.put("Poland", "https://www.gov.uk/foreign-travel-advice/poland/entry-requirements");
        LINKS.put("Portugal", "https://www.gov.uk/foreign-travel-advice/portugal/entry-requirements");
        LINKS.put("Singapore", "https://www.gov.uk/foreign-travel-advice/singapore/entry-requirements");
        LINKS.put("South Africa", "https://www.gov.uk/foreign-travel-advice/south-africa/entry-requirements");
        LINKS.put("South Korea", "https://www.gov.uk/foreign-travel-advice/south-korea/entry-requirements");
        LINKS.put("Spain", "https://www.gov.uk/foreign-travel-advice/spain/entry-requirements");
        LINKS.put("Sweden", "https://www.gov.uk/foreign-travel-advice/sweden/entry-requirements");
        LINKS.put("Taiwan", "https://www.gov.uk/foreign-travel-advice/taiwan/entry-requirements");
        LINKS.put("Tanzania", "https://www.gov.uk/foreign-travel-advice/tanzania/entry-requirements");
        LINKS.put("Thailand", "https://www.gov.uk/foreign-travel-advice/thailand/entry-requirements");
        LINKS.put("Türkiye", "https://www.gov.uk/foreign-travel-advice/turkey/entry-requirements");
        LINKS.put("United Arab Emirates", "https://www.gov.uk/foreign-travel-advice/united-arab-emirates/entry-requirements");
        LINKS.put("United Kingdom", "https://www.gov.uk/check-uk-visa");
        LINKS.put("United States", "https://www.gov.uk/foreign-travel-advice/usa/entry-requirements");
        LINKS.put("Vietnam", "https://www.gov.uk/foreign-travel-advice/vietnam/entry-requirements");
    }

    /** Official entry-requirements URL for a country, or empty if none is held. */
    public Optional<String> forCountry(String country) {
        if (country == null || country.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LINKS.get(country.trim()));
    }

    public String sourceName() {
        return "UK FCDO entry requirements";
    }
}
