package com.zainab.roamSafe.service;

import com.zainab.roamSafe.dto.LandingView.*;
import com.zainab.roamSafe.model.Advisory;
import com.zainab.roamSafe.model.SafetyScore;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.AdvisoryRepository;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles the landing page from real data only.
 *
 * The design handoff ships illustrative figures (2.4M reports, sample
 * testimonials, invented live scores). Per the product's "summarizes, never
 * invents" principle, none of those are reproduced — every number here is
 * counted or computed from the database.
 */
@Service
public class LandingService {

    private final ScamReportRepository scamReportRepository;
    private final ScamService scamService;
    private final SafetyScoreService safetyScoreService;
    private final AdvisoryRepository advisoryRepository;

    public LandingService(ScamReportRepository scamReportRepository,
            ScamService scamService,
            SafetyScoreService safetyScoreService,
            AdvisoryRepository advisoryRepository) {
        this.scamReportRepository = scamReportRepository;
        this.scamService = scamService;
        this.safetyScoreService = safetyScoreService;
        this.advisoryRepository = advisoryRepository;
    }

    /**
     * Countries under an active (high/critical) government advisory, one row per
     * country (most severe kept), for the landing "active advisories" strip.
     * Real, sourced data straight from the advisories table.
     */
    public List<Advisory> activeAdvisories(int limit) {
        List<Advisory> raw = advisoryRepository.findBySeverityIn(List.of("high", "critical"));
        Map<String, Advisory> byCountry = new LinkedHashMap<>();
        for (Advisory a : raw) {
            Advisory kept = byCountry.get(a.getCountryName());
            if (kept == null || rank(a.getSeverity()) > rank(kept.getSeverity())) {
                byCountry.put(a.getCountryName(), a);
            }
        }
        return byCountry.values().stream()
                .sorted(Comparator.comparingInt((Advisory a) -> rank(a.getSeverity())).reversed()
                        .thenComparing(Advisory::getCountryName))
                .limit(limit)
                .toList();
    }

    private static int rank(String severity) {
        return "critical".equals(severity) ? 2 : "high".equals(severity) ? 1 : 0;
    }

    /** Trust-strip figures, all counted in SQL. */
    public List<Stat> stats() {
        long approved = scamReportRepository.countByStatus(ScamReportStatus.APPROVED);
        long cities = scamReportRepository.countDistinctCities();
        long neighborhoods = scamReportRepository.countDistinctNeighborhoods();
        long safe = scamReportRepository.countByStatusAndSafetyZone(
                ScamReportStatus.APPROVED, com.zainab.roamSafe.model.SafetyZone.GREEN);

        List<Stat> out = new ArrayList<>();
        out.add(new Stat(String.valueOf(approved), "Reports analyzed"));
        out.add(new Stat(String.valueOf(cities), "Cities covered"));
        out.add(new Stat(String.valueOf(neighborhoods), "Neighborhoods mapped"));
        out.add(new Stat(String.valueOf(safe), "Safe zones flagged"));
        return out;
    }

    /** Hero briefing card for the most-reported city with a computable score. */
    public Briefing heroBriefing() {
        List<Object[]> top = scamService.getTopCities(3);
        for (Object[] row : top) {
            String city = (String) row[0];
            SafetyScore score = safetyScoreService.getScoreForCity(city);
            if (score == null) {
                continue;
            }
            return new Briefing(
                    city,
                    score.getOverallScore(),
                    (int) Math.round(score.getConfidenceLevel() * 100),
                    score.getTotalScamCount(),
                    relativeTime(score.getLastCalculated()),
                    meters(city));
        }
        return null;
    }

    /**
     * Three real, distinct signal meters computed from the city's reports.
     * We deliberately avoid the financial/physical/digital entity fields here:
     * most reports carry the generic "General" category, so those would collapse
     * to identical values. Severity, recency and night-incident share are always
     * present on every report, so these are honest and non-redundant.
     */
    private List<Meter> meters(String city) {
        List<ScamReport> reports = scamService.getReportsByCity(city);
        if (reports.isEmpty()) {
            return List.of();
        }
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        int total = reports.size();
        double sumSeverity = 0;
        int recent = 0;
        int night = 0;
        for (ScamReport r : reports) {
            sumSeverity += r.getSeverityScore();
            if (r.getCreatedAt() != null && r.getCreatedAt().isAfter(sixMonthsAgo)) {
                recent++;
            }
            if (Boolean.TRUE.equals(r.getIsNightTimeIncident())) {
                night++;
            }
        }
        int severity = (int) Math.round(sumSeverity / total * 10); // avg severity 1-10 → 0-100
        int recentShare = (int) Math.round(100.0 * recent / total);
        int nightShare = (int) Math.round(100.0 * night / total);
        return List.of(
                new Meter("Scam severity", severity, severityOf(severity)),
                new Meter("Recent activity", recentShare, severityOf(recentShare)),
                new Meter("Night incidents", nightShare, severityOf(nightShare)));
    }

    /** Up to four live-intel cards from the top-reported cities. */
    public List<Intel> intel() {
        List<Object[]> top = scamService.getTopCities(4);
        List<Intel> out = new ArrayList<>();
        for (Object[] row : top) {
            String city = (String) row[0];
            SafetyScore score = safetyScoreService.getScoreForCity(city);
            if (score == null) {
                continue;
            }
            int overall = score.getOverallScore();
            List<String> tags = new ArrayList<>();
            tags.add(overall >= 75 ? "Lower risk" : overall >= 55 ? "Mixed signals" : "Elevated risk");
            if (score.getRecentScamCount() > 0) {
                tags.add(score.getRecentScamCount() + " recent");
            }
            out.add(new Intel(
                    city,
                    overall,
                    scoreColor(overall),
                    (int) Math.round(score.getConfidenceLevel() * 100),
                    score.getTotalScamCount(),
                    noteFor(city, score),
                    intelTint(city),
                    tags));
        }
        return out;
    }

    /** Scam-library preview: the highest-severity recent tactics. */
    public List<ScamReport> scamPreview(int limit) {
        List<ScamReport> recent = scamService.getRecentReports(limit);
        return recent.size() > limit ? recent.subList(0, limit) : recent;
    }

    /** Command-palette seed: real cities + top-level actions. */
    public List<PaletteItem> paletteItems() {
        List<PaletteItem> items = new ArrayList<>();
        for (Object[] row : scamService.getTopCities(6)) {
            String city = (String) row[0];
            long count = ((Number) row[1]).longValue();
            items.add(new PaletteItem("Cities", city, count + " reports", "/scams?city=" + city));
        }
        items.add(new PaletteItem("Actions", "Browse the scam library", "All documented tactics", "/scams"));
        items.add(new PaletteItem("Actions", "Open the live map", "Global safety at a glance", "/map"));
        items.add(new PaletteItem("Actions", "Submit a report", "Share what you saw", "/submit"));
        items.add(new PaletteItem("Actions", "See pricing", "Free, Trip Pass or Pro", "/pricing"));
        return items;
    }

    // --- helpers ---

    private static String severityOf(int risk) {
        if (risk >= 70)
            return "critical";
        if (risk >= 45)
            return "high";
        if (risk >= 25)
            return "medium";
        return "low";
    }

    private static String scoreColor(int score) {
        if (score >= 85)
            return "#4F7F4C";
        if (score >= 75)
            return "#6E9469";
        if (score >= 65)
            return "#B98A2E";
        return "#B4553B";
    }

    private static String noteFor(String city, SafetyScore score) {
        int n = score.getTotalScamCount();
        if (n == 0) {
            return "No reports yet. Coverage is expanding.";
        }
        return n + " community report" + (n == 1 ? "" : "s")
                + " · " + score.getRecentScamCount() + " in the last 6 months.";
    }

    /**
     * Deterministic gradient stand-in for a city photo (same approach as the
     * handoff's intelTint). Hue derived from the city name so it's stable.
     */
    private static String intelTint(String city) {
        int h = Math.floorMod(city.hashCode(), 360);
        return "linear-gradient(150deg, hsl(" + h + ",26%,32%), hsl(" + ((h + 28) % 360) + ",30%,20%))";
    }

    private static String relativeTime(LocalDateTime when) {
        if (when == null) {
            return "just now";
        }
        long mins = Duration.between(when, LocalDateTime.now()).toMinutes();
        if (mins < 1)
            return "just now";
        if (mins < 60)
            return "updated " + mins + " min ago";
        long hours = mins / 60;
        if (hours < 24)
            return "updated " + hours + "h ago";
        long days = hours / 24;
        return "updated " + days + "d ago";
    }
}
