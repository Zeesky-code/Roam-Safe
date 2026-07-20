package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.CitySummary;
import com.zainab.roamSafe.model.SafetyScore;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;
import com.zainab.roamSafe.service.CitySummaryService;
import com.zainab.roamSafe.service.SafetyScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PublicApiController {

    private final SafetyScoreService safetyScoreService;
    private final CitySummaryService citySummaryService;
    private final ScamReportRepository scamReportRepository;
    private final com.zainab.roamSafe.service.DestinationService destinationService;
    private final com.zainab.roamSafe.service.ScamService scamService;

    public PublicApiController(SafetyScoreService safetyScoreService,
            CitySummaryService citySummaryService,
            ScamReportRepository scamReportRepository,
            com.zainab.roamSafe.service.DestinationService destinationService,
            com.zainab.roamSafe.service.ScamService scamService) {
        this.safetyScoreService = safetyScoreService;
        this.citySummaryService = citySummaryService;
        this.scamReportRepository = scamReportRepository;
        this.destinationService = destinationService;
        this.scamService = scamService;
    }

    /**
     * Cities RoamSafe actually covers, safest first. Lets a client (or an AI
     * agent) discover what's available instead of guessing city names, and
     * suggest real alternatives when a requested city isn't covered.
     */
    @GetMapping("/cities")
    public ResponseEntity<List<Map<String, Object>>> listCities() {
        List<Object[]> rows = scamService.getTopCities(500);

        // Read every cached score in one query rather than per-city (which would
        // recalculate and turn this into ~1 remote round-trip per city).
        Map<String, Integer> cached = new HashMap<>();
        for (Object[] r : safetyScoreService.getCachedCityScores()) {
            if (r[0] != null && r[1] != null) {
                cached.put(((String) r[0]).toLowerCase(), ((Number) r[1]).intValue());
            }
        }

        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String city = (String) row[0];
            long reports = ((Number) row[1]).longValue();
            Integer score = cached.get(city.toLowerCase());
            Map<String, Object> m = new HashMap<>();
            m.put("city", city);
            m.put("reports", reports);
            com.zainab.roamSafe.service.CountryLookup.forCity(city)
                    .ifPresent(c -> m.put("country", c.name()));
            if (score != null) {
                m.put("score", score);
                m.put("riskLevel", getRiskLevel(score));
            }
            out.add(m);
        }
        // Safest first; cities without a cached score yet sort last.
        out.sort((a, b) -> Integer.compare(
                (int) b.getOrDefault("score", -1), (int) a.getOrDefault("score", -1)));
        return ResponseEntity.ok(out);
    }

    /**
     * Get comprehensive risk data for a specific city.
     * This is the main endpoint partners like TripDesk will call.
     */
    @GetMapping("/risk/city/{city}")
    public ResponseEntity<Map<String, Object>> getCityRisk(@PathVariable String city) {
        Map<String, Object> response = new HashMap<>();

        // 1. Get Safety Score
        SafetyScore score = safetyScoreService.getScoreForCity(city);
        if (score == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Get AI Summary
        CitySummary summary = citySummaryService.getSummaryForCity(city);

        // 3. All approved reports for this city (full set drives the breakdowns;
        // the alert list stays capped).
        List<ScamReport> all = scamReportRepository.findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(
                city, ScamReportStatus.APPROVED);
        List<ScamReport> alerts = all.stream().limit(5).toList();

        int confidencePct = (int) Math.round(score.getConfidenceLevel() * 100);

        response.put("city", score.getCity().getName());
        response.put("country", score.getCity().getCountry());
        com.zainab.roamSafe.service.CountryLookup.forCity(city)
                .ifPresent(c -> response.put("country", c.name()));
        response.put("overallScore", score.getOverallScore()); // 0-100
        response.put("riskLevel", getRiskLevel(score.getOverallScore()));
        response.put("riskBreakdown", Map.of(
                "financial", score.getFinancialRiskScore(),
                "physical", score.getPhysicalRiskScore(),
                "digital", score.getDigitalRiskScore()));

        // How much evidence sits behind the numbers (counts + date range).
        response.put("coverage", destinationService.coverage(all, confidencePct));
        // Risk per reported concern, so callers can rank by theft/scams/etc.
        response.put("categoryBreakdown", destinationService.categoryBreakdown(all));
        // Neighborhood-level scores, not just city-wide.
        response.put("neighborhoods", destinationService.neighborhoodBreakdown(all));
        // Night-time signal + the areas those incidents cluster in.
        response.put("nightRisk", destinationService.nightRisk(all));

        response.put("summary", summary.getSummaryText());
        response.put("latestAlerts", alerts);
        response.put("lastUpdated", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Real-time feed of all new approved reports globally.
     * Partners can poll this or subscribe to it.
     */
    @GetMapping("/alerts/feed")
    public ResponseEntity<List<ScamReport>> getAlertsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Fetch paginated approved reports, newest first
        List<ScamReport> reports = scamReportRepository.findByStatusOrderByCreatedAtDesc(ScamReportStatus.APPROVED);
        // Implementing simple sublist pagination for now as repository method returns
        // List
        int start = Math.min(page * size, reports.size());
        int end = Math.min((page + 1) * size, reports.size());

        return ResponseEntity.ok(reports.subList(start, end));
    }

    /**
     * Map a safety score to a risk label.
     *
     * Calibrated to the current scoring model, where a score is
     * {@code 100 - recencyWeightedAvgSeverity * 85} and well-reported cities
     * land in the 45-70 band. The previous thresholds (LOW>=80, HIGH>=40) were
     * written for the old volume-based score and labelled almost every city
     * "HIGH", which badly overstates risk to API/MCP consumers.
     */
    private String getRiskLevel(int score) {
        if (score >= 70)
            return "LOW";
        if (score >= 55)
            return "MODERATE";
        if (score >= 40)
            return "ELEVATED";
        return "HIGH";
    }
}
