package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.City;
import com.zainab.roamSafe.model.SafetyScore;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.CityRepository;
import com.zainab.roamSafe.repository.SafetyScoreRepository;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SafetyScoreService {

    private final SafetyScoreRepository safetyScoreRepository;
    private final ScamReportRepository scamReportRepository;
    private final CityRepository cityRepository;

    public SafetyScoreService(SafetyScoreRepository safetyScoreRepository,
            ScamReportRepository scamReportRepository,
            CityRepository cityRepository) {
        this.safetyScoreRepository = safetyScoreRepository;
        this.scamReportRepository = scamReportRepository;
        this.cityRepository = cityRepository;
    }

    /** More weight to recent reports so the score tracks current conditions. */
    private static double recencyWeight(LocalDateTime when) {
        if (when == null) {
            return 0.5;
        }
        long months = java.time.temporal.ChronoUnit.MONTHS.between(when, LocalDateTime.now());
        if (months <= 3)
            return 1.0;
        if (months <= 6)
            return 0.7;
        if (months <= 12)
            return 0.5;
        return 0.3;
    }

    /** acc[0] += risk*weight; acc[1] += weight. */
    private static void accumulate(double[] acc, double risk, double weight) {
        acc[0] += risk * weight;
        acc[1] += weight;
    }

    /** Weighted mean risk in 0..1 (0 when the bucket is empty). */
    private static double weightedAverage(double[] acc) {
        return acc[1] > 0 ? acc[0] / acc[1] : 0.0;
    }

    /**
     * Map average risk (0..1) to a 1..100 safety score. The 0.85 slope keeps an
     * all-severe city near 15 rather than 0, so the scale stays informative, and
     * SafetyScore's @Min(1)/@Max(100) constraints are always satisfied.
     */
    private static int riskToScore(double avgRisk) {
        int raw = (int) Math.round(100 - avgRisk * 85);
        return Math.max(1, Math.min(100, raw));
    }

    public SafetyScore getScoreForCity(String cityName) {
        City city = cityRepository.findFirstByName(cityName);
        if (city == null) {
            return null;
        }

        // Check if we have a recent score
        Optional<SafetyScore> existingScore = safetyScoreRepository.findByCity(city);
        if (existingScore.isPresent()) {
            SafetyScore score = existingScore.get();
            // If calculated within last 24 hours, return it
            if (score.getLastCalculated().isAfter(LocalDateTime.now().minusHours(24))) {
                return score;
            }
        }

        // Otherwise calculate fresh score
        return calculateAndSaveScore(city);
    }

    private SafetyScore calculateAndSaveScore(City city) {
        List<ScamReport> reports = scamReportRepository.findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(
                city.getName(), ScamReportStatus.APPROVED);

        int totalReports = reports.size();
        int recentReports = 0;
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        // Buckets of (weightedRisk, weight) accumulators per dimension.
        double[] allRisk = new double[2]; // [sumRisk*weight, sumWeight]
        double[] finRisk = new double[2];
        double[] physRisk = new double[2];
        double[] digRisk = new double[2];

        for (ScamReport report : reports) {
            if (report.getCreatedAt() != null && report.getCreatedAt().isAfter(sixMonthsAgo)) {
                recentReports++;
            }
            double risk = report.getSeverityScore() / 10.0; // 0.1 .. 1.0
            double weight = recencyWeight(report.getCreatedAt());
            accumulate(allRisk, risk, weight);

            String cat = report.getCategory() != null ? report.getCategory().toLowerCase() : "";
            if (cat.contains("financial") || cat.contains("theft")) {
                accumulate(finRisk, risk, weight);
            } else if (cat.contains("harassment") || cat.contains("social") || cat.contains("tourism")) {
                accumulate(physRisk, risk, weight);
            } else if (cat.contains("digital")) {
                accumulate(digRisk, risk, weight);
            }
        }

        // Recency-weighted AVERAGE severity (0..1), independent of report volume:
        // a well-documented city no longer scores as dangerous just for having
        // more reports. Volume is expressed through confidence instead. Mapping
        // avg-risk → score with a 0.85 slope floors an all-severe city near 15
        // rather than 0, keeping the scale meaningful.
        int overallScore = riskToScore(weightedAverage(allRisk));
        // Category sub-scores fall back to the overall figure when a category has
        // no reports, so they're never a misleading "0 risk / perfectly safe".
        int financeScore = finRisk[1] > 0 ? riskToScore(weightedAverage(finRisk)) : overallScore;
        int physicalScore = physRisk[1] > 0 ? riskToScore(weightedAverage(physRisk)) : overallScore;
        int digitalScore = digRisk[1] > 0 ? riskToScore(weightedAverage(digRisk)) : overallScore;

        SafetyScore score = safetyScoreRepository.findByCity(city).orElse(new SafetyScore());
        score.setCity(city);
        score.setOverallScore(overallScore);
        score.setFinancialRiskScore(financeScore);
        score.setPhysicalRiskScore(physicalScore);
        score.setDigitalRiskScore(digitalScore);
        score.setTotalScamCount(totalReports);
        score.setRecentScamCount(recentReports);
        score.setConfidenceLevel(totalReports > 10 ? 0.9 : (totalReports > 5 ? 0.7 : 0.4));
        score.setLastCalculated(LocalDateTime.now());
        score.setLastUpdated(LocalDateTime.now());

        // Placeholder for AI insights
        score.setAiInsights("Safety score calculated based on " + totalReports + " community reports.");

        return safetyScoreRepository.save(score);
    }
}
