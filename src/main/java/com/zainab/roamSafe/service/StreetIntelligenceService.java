package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Street-level intelligence: what the reports say about one named place.
 *
 * Everything here is derived from reports that actually name the place. Two
 * things the design asked for are deliberately absent because no data supports
 * them: nearby police stations (no geospatial source is ingested yet) and
 * time-of-day guidance beyond the share of incidents flagged as night-time.
 * Safer alternatives are real - they are other places in the same city that
 * score better on their own reports, not suggestions invented to fill the slot.
 */
@Service
public class StreetIntelligenceService {

    /** Below this, a place's score is too thin to state as fact. */
    private static final int MIN_REPORTS_FOR_SCORE = 2;

    private final ScamReportRepository reportRepository;

    public StreetIntelligenceService(ScamReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /** One place that matched a search. */
    public record Match(String place, String city, int reports, Integer score) {
    }

    /** A safer place in the same city, with the evidence behind it. */
    public record Alternative(String place, int score, int reports) {
    }

    public record Profile(
            String place, String city, String country,
            Integer score, String scoreColor, String riskLevel,
            int reports, int nightIncidents, Integer nightSharePct,
            boolean thinEvidence,
            List<String> categories,
            List<ScamReport> evidence,
            List<String> preventionTips,
            List<Alternative> saferAlternatives) {
    }

    /**
     * Places matching a free-text query, best-evidenced first.
     *
     * Matching is done on extracted names rather than the raw field, so
     * "Rambla" finds the place inside "La Rambla, Raval, public transport".
     */
    public List<Match> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<ScamReport> candidates = reportRepository
                .findByStatusAndNeighborhoodIsNotNull(ScamReportStatus.APPROVED);

        // (city|place) -> [count, severitySum]
        Map<String, double[]> agg = new LinkedHashMap<>();
        Map<String, String[]> label = new LinkedHashMap<>();
        for (ScamReport r : candidates) {
            for (String place : PlaceNames.extract(r.getNeighborhood())) {
                if (!place.toLowerCase(Locale.ROOT).contains(needle)) {
                    continue;
                }
                String key = (r.getCity() + "|" + place).toLowerCase(Locale.ROOT);
                label.putIfAbsent(key, new String[] { place, r.getCity() });
                double[] acc = agg.computeIfAbsent(key, k -> new double[2]);
                acc[0] += 1;
                acc[1] += r.getSeverityScore();
            }
        }

        List<Match> matches = new ArrayList<>();
        for (Map.Entry<String, double[]> e : agg.entrySet()) {
            String[] l = label.get(e.getKey());
            int count = (int) e.getValue()[0];
            matches.add(new Match(l[0], l[1], count,
                    count >= MIN_REPORTS_FOR_SCORE ? score(e.getValue()[1] / count) : null));
        }
        matches.sort(Comparator.comparingInt(Match::reports).reversed());
        return matches;
    }

    /** Full profile for one place in one city, or empty if nothing names it. */
    public Optional<Profile> profile(String city, String place, String country) {
        if (city == null || place == null) {
            return Optional.empty();
        }
        List<ScamReport> cityReports = reportRepository
                .findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(city, ScamReportStatus.APPROVED);

        List<ScamReport> here = new ArrayList<>();
        for (ScamReport r : cityReports) {
            if (PlaceNames.extract(r.getNeighborhood()).stream()
                    .anyMatch(p -> p.equalsIgnoreCase(place))) {
                here.add(r);
            }
        }
        if (here.isEmpty()) {
            return Optional.empty();
        }

        double severitySum = 0;
        int night = 0;
        Set<String> categories = new LinkedHashSet<>();
        List<String> tips = new ArrayList<>();
        for (ScamReport r : here) {
            severitySum += r.getSeverityScore();
            if (Boolean.TRUE.equals(r.getIsNightTimeIncident())) {
                night++;
            }
            if (r.getCategory() != null && !r.getCategory().isBlank()) {
                categories.add(r.getCategory());
            }
            String tip = r.getPreventionTips();
            if (tip != null && !tip.isBlank() && !tips.contains(tip.trim())) {
                tips.add(tip.trim());
            }
        }

        boolean thin = here.size() < MIN_REPORTS_FOR_SCORE;
        Integer score = thin ? null : score(severitySum / here.size());

        return Optional.of(new Profile(
                place, city, country,
                score,
                score != null ? scoreColor(score) : "#8A877D",
                score != null ? riskLevel(score) : null,
                here.size(),
                night,
                // A night share off one report is noise, not a pattern.
                thin ? null : (int) Math.round(100.0 * night / here.size()),
                thin,
                new ArrayList<>(categories),
                here.stream().limit(6).toList(),
                tips.stream().limit(5).toList(),
                saferAlternatives(cityReports, place, score)));
    }

    /**
     * Better-scoring places in the same city.
     *
     * Only places with enough reports to score are offered, and only when they
     * genuinely beat this one - an "alternative" that is no safer would be
     * advice we can't stand behind.
     */
    private List<Alternative> saferAlternatives(List<ScamReport> cityReports, String place, Integer score) {
        if (score == null) {
            return List.of();
        }
        Map<String, double[]> agg = new LinkedHashMap<>();
        Map<String, String> label = new LinkedHashMap<>();
        for (ScamReport r : cityReports) {
            for (String p : PlaceNames.extract(r.getNeighborhood())) {
                if (p.equalsIgnoreCase(place)) {
                    continue;
                }
                String key = p.toLowerCase(Locale.ROOT);
                label.putIfAbsent(key, p);
                double[] acc = agg.computeIfAbsent(key, k -> new double[2]);
                acc[0] += 1;
                acc[1] += r.getSeverityScore();
            }
        }
        List<Alternative> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : agg.entrySet()) {
            int count = (int) e.getValue()[0];
            if (count < MIN_REPORTS_FOR_SCORE) {
                continue;
            }
            int alt = score(e.getValue()[1] / count);
            if (alt > score) {
                out.add(new Alternative(label.get(e.getKey()), alt, count));
            }
        }
        out.sort(Comparator.comparingInt(Alternative::score).reversed());
        return out.stream().limit(4).toList();
    }

    /** Same mapping the city scores use, so a place is comparable to its city. */
    private static int score(double avgSeverity) {
        return Math.max(1, Math.min(100, (int) Math.round(100 - avgSeverity / 10.0 * 85)));
    }

    private static String riskLevel(int score) {
        if (score >= 70) {
            return "LOW";
        }
        if (score >= 55) {
            return "MODERATE";
        }
        return score >= 40 ? "ELEVATED" : "HIGH";
    }

    private static String scoreColor(int score) {
        if (score >= 85) {
            return "#4F7F4C";
        }
        if (score >= 75) {
            return "#6E9469";
        }
        return score >= 65 ? "#B98A2E" : "#B4553B";
    }
}
