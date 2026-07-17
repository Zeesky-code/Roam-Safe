package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.SafetyScore;
import com.zainab.roamSafe.model.ScamReport;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Assembles a per-city destination report from real reports + the computed
 * safety score. No invented figures — meters and neighborhood tiles are derived
 * from the city's actual approved reports.
 */
@Service
public class DestinationService {

    private final SafetyScoreService safetyScoreService;

    public DestinationService(SafetyScoreService safetyScoreService) {
        this.safetyScoreService = safetyScoreService;
    }

    public record Meter(String label, int value, String severity) {
    }

    public record NeighborhoodTile(String name, int score, String color) {
    }

    public record View(
            String city,
            Integer score,
            String scoreColor,
            int confidencePct,
            int reports,
            int recentReports,
            String updated,
            List<Meter> meters,
            List<NeighborhoodTile> neighborhoods) {
    }

    /**
     * Builds the report view for a city from its full approved report list
     * (pass the complete list, not the paywalled subset, so stats are accurate).
     */
    public View build(String city, List<ScamReport> allReports) {
        SafetyScore score = safetyScoreService.getScoreForCity(city);
        Integer overall = score != null ? score.getOverallScore() : null;
        int confidence = score != null ? (int) Math.round(score.getConfidenceLevel() * 100) : 0;
        String updated = score != null ? relativeTime(score.getLastCalculated()) : "not yet scored";

        return new View(
                city,
                overall,
                overall != null ? scoreColor(overall) : "#8A877D",
                confidence,
                allReports.size(),
                countRecent(allReports),
                updated,
                meters(allReports),
                neighborhoods(allReports));
    }

    private List<Meter> meters(List<ScamReport> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        int total = reports.size();
        double sumSeverity = 0;
        int recent = 0, night = 0;
        for (ScamReport r : reports) {
            sumSeverity += r.getSeverityScore();
            if (r.getCreatedAt() != null && r.getCreatedAt().isAfter(sixMonthsAgo))
                recent++;
            if (Boolean.TRUE.equals(r.getIsNightTimeIncident()))
                night++;
        }
        int severity = (int) Math.round(sumSeverity / total * 10);
        int recentShare = (int) Math.round(100.0 * recent / total);
        int nightShare = (int) Math.round(100.0 * night / total);
        return List.of(
                new Meter("Scam severity", severity, severityOf(severity)),
                new Meter("Recent activity", recentShare, severityOf(recentShare)),
                new Meter("Night incidents", nightShare, severityOf(nightShare)));
    }

    /** Aggregate reports by neighborhood → a safety score per district. */
    private List<NeighborhoodTile> neighborhoods(List<ScamReport> reports) {
        Map<String, double[]> byHood = new LinkedHashMap<>(); // name -> [sumSeverity, count]
        for (ScamReport r : reports) {
            String hood = r.getNeighborhood();
            if (hood == null || hood.isBlank())
                continue;
            double[] acc = byHood.computeIfAbsent(hood, k -> new double[2]);
            acc[0] += r.getSeverityScore();
            acc[1] += 1;
        }
        List<NeighborhoodTile> tiles = new ArrayList<>();
        for (Map.Entry<String, double[]> e : byHood.entrySet()) {
            double avgSeverity = e.getValue()[0] / e.getValue()[1];
            int hoodScore = Math.max(1, Math.min(100, (int) Math.round(100 - avgSeverity / 10.0 * 85)));
            tiles.add(new NeighborhoodTile(e.getKey(), hoodScore, scoreColor(hoodScore)));
        }
        tiles.sort(Comparator.comparingInt(NeighborhoodTile::score).reversed());
        return tiles;
    }

    private static int countRecent(List<ScamReport> reports) {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        int n = 0;
        for (ScamReport r : reports) {
            if (r.getCreatedAt() != null && r.getCreatedAt().isAfter(sixMonthsAgo))
                n++;
        }
        return n;
    }

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

    private static String relativeTime(LocalDateTime when) {
        if (when == null)
            return "just now";
        long mins = java.time.Duration.between(when, LocalDateTime.now()).toMinutes();
        if (mins < 1)
            return "updated just now";
        if (mins < 60)
            return "updated " + mins + " min ago";
        long hours = mins / 60;
        if (hours < 24)
            return "updated " + hours + "h ago";
        return "updated " + (hours / 24) + "d ago";
    }
}
