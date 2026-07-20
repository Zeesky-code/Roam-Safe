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

    public PublicApiController(SafetyScoreService safetyScoreService,
            CitySummaryService citySummaryService,
            ScamReportRepository scamReportRepository) {
        this.safetyScoreService = safetyScoreService;
        this.citySummaryService = citySummaryService;
        this.scamReportRepository = scamReportRepository;
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

        // 3. Get Recent High-Severity Alerts (Top 5)
        List<ScamReport> alerts = scamReportRepository.findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(
                city, ScamReportStatus.APPROVED).stream().limit(5).toList();

        response.put("city", score.getCity().getName());
        response.put("country", score.getCity().getCountry());
        response.put("overallScore", score.getOverallScore()); // 0-100
        response.put("riskLevel", getRiskLevel(score.getOverallScore()));
        response.put("riskBreakdown", Map.of(
                "financial", score.getFinancialRiskScore(),
                "physical", score.getPhysicalRiskScore(),
                "digital", score.getDigitalRiskScore()));
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
