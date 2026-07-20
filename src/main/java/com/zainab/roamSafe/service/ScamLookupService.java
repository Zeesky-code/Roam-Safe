package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Looks up a named scam - "the bracelet scam in Paris" - across every city that
 * reports it.
 *
 * Grouping by city is the point: hearing that a scam exists is less useful than
 * seeing where it is actually reported and how often. Likelihood is expressed as
 * the number of reports behind it, not as a percentage, because report counts
 * reflect how well a city is documented as much as how common a scam is, and a
 * percentage would imply a precision the data doesn't have.
 */
@Service
public class ScamLookupService {

    private final ScamReportRepository reportRepository;
    private final CityCountryResolver countryResolver;

    public ScamLookupService(ScamReportRepository reportRepository, CityCountryResolver countryResolver) {
        this.reportRepository = reportRepository;
        this.countryResolver = countryResolver;
    }

    /** Where a scam is reported, and how much evidence there is. */
    public record CityHit(String city, String country, int reports, int worstSeverity,
            List<String> places, List<ScamReport> examples) {
    }

    public record Lookup(String query, String cityFilter, int totalReports,
            List<CityHit> cities, List<String> preventionTips, Set<String> categories) {
    }

    /**
     * Search terms, minus any city name mentioned in the query.
     *
     * "bracelet scam Paris" should search for the bracelet scam and filter to
     * Paris, not search for the literal string including the city.
     */
    public Lookup lookup(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            return new Lookup(query, null, 0, List.of(), List.of(), Set.of());
        }

        String cityFilter = detectCity(query);
        String terms = query;
        if (cityFilter != null) {
            terms = query.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(cityFilter) + "\\b", "").trim();
        }
        // Drop filler that would never appear in a report title.
        terms = terms.replaceAll("(?i)\\b(scam|scams|in|at|the|a|an)\\b", " ")
                .replaceAll("\\s+", " ").trim();
        if (terms.isEmpty()) {
            terms = query.replaceAll("(?i)\\b(in|at|the|a|an)\\b", " ").replaceAll("\\s+", " ").trim();
        }

        List<ScamReport> hits = reportRepository.searchApproved(ScamReportStatus.APPROVED, terms);
        if (cityFilter != null) {
            String c = cityFilter;
            hits = hits.stream().filter(r -> c.equalsIgnoreCase(r.getCity())).toList();
        }

        Map<String, List<ScamReport>> byCity = new LinkedHashMap<>();
        Set<String> categories = new LinkedHashSet<>();
        List<String> tips = new ArrayList<>();
        for (ScamReport r : hits) {
            byCity.computeIfAbsent(r.getCity(), k -> new ArrayList<>()).add(r);
            if (r.getCategory() != null && !r.getCategory().isBlank()) {
                categories.add(r.getCategory());
            }
            String tip = r.getPreventionTips();
            if (tip != null && !tip.isBlank() && !tips.contains(tip.trim())) {
                tips.add(tip.trim());
            }
        }

        List<CityHit> cities = new ArrayList<>();
        for (Map.Entry<String, List<ScamReport>> e : byCity.entrySet()) {
            List<ScamReport> reports = e.getValue();
            Set<String> places = new LinkedHashSet<>();
            int worst = 0;
            for (ScamReport r : reports) {
                places.addAll(PlaceNames.extract(r.getNeighborhood()));
                worst = Math.max(worst, r.getSeverityScore());
            }
            cities.add(new CityHit(e.getKey(),
                    countryResolver.countryFor(e.getKey()).orElse(null),
                    reports.size(), worst,
                    places.stream().limit(5).toList(),
                    reports.stream().limit(3).toList()));
        }
        cities.sort(Comparator.comparingInt(CityHit::reports).reversed());

        return new Lookup(query, cityFilter, hits.size(), cities,
                tips.stream().limit(6).toList(), categories);
    }

    /** The covered city named in the query, if any. */
    private String detectCity(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        String best = null;
        for (Object[] row : reportRepository.findTopCities(
                org.springframework.data.domain.PageRequest.of(0, 500))) {
            String city = (String) row[0];
            if (city == null) {
                continue;
            }
            if (lower.contains(city.toLowerCase(Locale.ROOT))
                    && (best == null || city.length() > best.length())) {
                best = city; // longest match wins: "New York City" over "New York"
            }
        }
        return best;
    }
}
