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

    public record NeighborhoodTile(String name, int score, String color, int reports, int nightIncidents) {
    }

    /** Per-category risk, derived from the real category field on reports. */
    public record CategoryStat(String category, int reports, double avgSeverity, int riskScore) {
    }

    /** How much evidence sits behind a city's numbers. */
    public record Coverage(int totalReports, int recentReports, int confidencePct,
            String oldestReport, String newestReport) {
    }

    /** Night-time risk signal: share of reports flagged as night incidents. */
    public record NightRisk(int nightIncidentShare, List<String> topNightAreas) {
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
        int recent = 0, night = 0, dated = 0;
        for (ScamReport r : reports) {
            sumSeverity += r.getSeverityScore();
            if (r.getReportedAt() != null) {
                dated++;
                if (r.getReportedAt().isAfter(sixMonthsAgo))
                    recent++;
            }
            if (Boolean.TRUE.equals(r.getIsNightTimeIncident()))
                night++;
        }
        int severity = (int) Math.round(sumSeverity / total * 10);
        int nightShare = (int) Math.round(100.0 * night / total);

        List<Meter> meters = new ArrayList<>();
        meters.add(new Meter("Scam severity", severity, severityOf(severity)));
        // Only claim a recency figure when something is actually dated, and take
        // the share of dated reports rather than of all of them — otherwise a city
        // of undated imports reads as "0% recent activity", which looks like a
        // measured all-clear instead of an absence of data.
        if (dated > 0) {
            int recentShare = (int) Math.round(100.0 * recent / dated);
            meters.add(new Meter("Recent activity", recentShare, severityOf(recentShare)));
        }
        meters.add(new Meter("Night incidents", nightShare, severityOf(nightShare)));
        return meters;
    }

    /**
     * Section headings the Wikivoyage import stored in the neighborhood field
     * when no specific area was named. They are topics, not places, and would
     * otherwise surface as advice like "avoid Stay safe after dark".
     */
    private static final java.util.Set<String> NOT_A_PLACE = java.util.Set.of(
            "stay safe", "safety", "crime", "crimes", "scam", "scams", "theft", "pickpocketing",
            "police", "emergency", "emergencies", "health", "natural disasters", "general",
            "drugs", "terrorism", "racism", "anti-semitism", "homophobia", "women", "lgbt",
            "solo travel", "transport", "getting around", "respect", "cope", "connect",
            "traffic", "road safety", "corruption", "begging", "harassment",
            // Objects/topics the extractor also mislabelled as neighborhoods
            "violent crimes", "reporting crimes", "football", "cars", "atm", "atms",
            "public transport", "public transportation", "subway", "metro", "buses", "trains",
            "taxis", "nightlife", "beaches", "tourist areas", "certain tourist hotspots",
            "areas of caution", "tourist hotspots", "restaurants", "bars", "hotels", "markets");

    /** Phrases like "Theft in public transit" describe a risk, not a district. */
    private static final List<String> TOPIC_PREFIXES = List.of(
            "theft ", "scam", "crime ", "pickpocket", "robbery ", "mugging ",
            "harassment ", "violence ", "fraud ", "assault ");

    /** True when a neighborhood value is really a topic/heading, not a location. */
    private static boolean isRealPlace(String name) {
        String n = name.trim().toLowerCase();
        if (n.isEmpty() || NOT_A_PLACE.contains(n)) {
            return false;
        }
        for (String p : TOPIC_PREFIXES) {
            if (n.startsWith(p)) {
                return false;
            }
        }
        // Long values are sentences/descriptions from the extractor, not place names.
        return name.trim().length() <= 45;
    }

    /** Aggregate reports by neighborhood → a safety score per district. */
    private List<NeighborhoodTile> neighborhoods(List<ScamReport> reports) {
        // lowercased name -> [sumSeverity, count, nightIncidents]; grouping is
        // case-insensitive so "Subway" and "subway" don't become two districts.
        Map<String, double[]> byHood = new LinkedHashMap<>();
        Map<String, String> displayName = new LinkedHashMap<>();
        for (ScamReport r : reports) {
            String hood = r.getNeighborhood();
            if (hood == null || hood.isBlank() || !isRealPlace(hood))
                continue;
            String name = hood.trim();
            String key = name.toLowerCase();
            displayName.putIfAbsent(key, name);
            double[] acc = byHood.computeIfAbsent(key, k -> new double[3]);
            acc[0] += r.getSeverityScore();
            acc[1] += 1;
            if (Boolean.TRUE.equals(r.getIsNightTimeIncident()))
                acc[2] += 1;
        }
        List<NeighborhoodTile> tiles = new ArrayList<>();
        for (Map.Entry<String, double[]> e : byHood.entrySet()) {
            double avgSeverity = e.getValue()[0] / e.getValue()[1];
            int hoodScore = Math.max(1, Math.min(100, (int) Math.round(100 - avgSeverity / 10.0 * 85)));
            tiles.add(new NeighborhoodTile(displayName.get(e.getKey()), hoodScore, scoreColor(hoodScore),
                    (int) e.getValue()[1], (int) e.getValue()[2]));
        }
        tiles.sort(Comparator.comparingInt(NeighborhoodTile::score).reversed());
        return tiles;
    }

    /** Public: neighborhood breakdown for a city's reports. */
    public List<NeighborhoodTile> neighborhoodBreakdown(List<ScamReport> reports) {
        return neighborhoods(reports);
    }

    /**
     * Risk per reported category (Theft, Financial, Harassment, ...). Lets a
     * caller rank by a specific concern instead of only the overall score.
     */
    public List<CategoryStat> categoryBreakdown(List<ScamReport> reports) {
        Map<String, double[]> byCat = new LinkedHashMap<>(); // cat -> [sumSeverity, count]
        for (ScamReport r : reports) {
            String cat = (r.getCategory() == null || r.getCategory().isBlank()) ? "General" : r.getCategory();
            double[] acc = byCat.computeIfAbsent(cat, k -> new double[2]);
            acc[0] += r.getSeverityScore();
            acc[1] += 1;
        }
        List<CategoryStat> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : byCat.entrySet()) {
            double avg = e.getValue()[0] / e.getValue()[1];
            int score = Math.max(1, Math.min(100, (int) Math.round(100 - avg / 10.0 * 85)));
            out.add(new CategoryStat(e.getKey(), (int) e.getValue()[1],
                    Math.round(avg * 10) / 10.0, score));
        }
        // Riskiest concern first
        out.sort(Comparator.comparingInt(CategoryStat::riskScore));
        return out;
    }

    /** How much evidence backs these numbers, including the report date range. */
    public Coverage coverage(List<ScamReport> reports, int confidencePct) {
        LocalDateTime oldest = null, newest = null;
        for (ScamReport r : reports) {
            // Real incident dates only. Ingestion time would make every source
            // look like it was reported the day we imported it.
            LocalDateTime c = r.getReportedAt();
            if (c == null)
                continue;
            if (oldest == null || c.isBefore(oldest))
                oldest = c;
            if (newest == null || c.isAfter(newest))
                newest = c;
        }
        return new Coverage(reports.size(), countRecent(reports), confidencePct,
                oldest == null ? null : oldest.toLocalDate().toString(),
                newest == null ? null : newest.toLocalDate().toString());
    }

    /** Share of reports flagged as night-time, plus the areas they cluster in. */
    public NightRisk nightRisk(List<ScamReport> reports) {
        if (reports.isEmpty()) {
            return new NightRisk(0, List.of());
        }
        int night = 0;
        Map<String, Integer> areas = new LinkedHashMap<>();
        for (ScamReport r : reports) {
            if (Boolean.TRUE.equals(r.getIsNightTimeIncident())) {
                night++;
                String hood = r.getNeighborhood();
                if (hood != null && !hood.isBlank() && isRealPlace(hood)) {
                    areas.merge(hood.trim(), 1, Integer::sum);
                }
            }
        }
        List<String> top = areas.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
        return new NightRisk((int) Math.round(100.0 * night / reports.size()), top);
    }

    private static int countRecent(List<ScamReport> reports) {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        int n = 0;
        for (ScamReport r : reports) {
            if (r.getReportedAt() != null && r.getReportedAt().isAfter(sixMonthsAgo))
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
