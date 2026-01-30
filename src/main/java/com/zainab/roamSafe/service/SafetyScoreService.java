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

    public SafetyScore getScoreForCity(String cityName) {
        City city = cityRepository.findByName(cityName);
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
        int sumSeverity = 0;
        int financialSeverity = 0;
        int physicalSeverity = 0;
        int digitalSeverity = 0;
        int financialCount = 0;
        int physicalCount = 0;
        int digitalCount = 0;

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        for (ScamReport report : reports) {
            if (report.getCreatedAt().isAfter(sixMonthsAgo)) {
                recentReports++;
            }
            sumSeverity += report.getSeverityScore();

            String cat = report.getCategory() != null ? report.getCategory().toLowerCase() : "";
            if (cat.contains("financial") || cat.contains("theft")) {
                financialSeverity += report.getSeverityScore();
                financialCount++;
            } else if (cat.contains("harassment") || cat.contains("social") || cat.contains("tourism")) {
                physicalSeverity += report.getSeverityScore();
                physicalCount++;
            } else if (cat.contains("digital")) {
                digitalSeverity += report.getSeverityScore();
                digitalCount++;
            }
        }

        // Simplified Scoring Algorithm
        // 100 = Perfect Safety
        // Deduct points based on volume and severity
        // Base deduction per report: 2 points
        // Extra deduction for severity > 5: severity points

        int penalty = 0;
        for (ScamReport report : reports) {
            penalty += 2;
            if (report.getSeverityScore() > 5) {
                penalty += (report.getSeverityScore() - 5);
            }
        }

        // Normalize scores
        int overallScore = Math.max(0, 100 - penalty);
        int financeScore = Math.max(0, 100 - (financialCount * 5 + financialSeverity));
        int physicalScore = Math.max(0, 100 - (physicalCount * 5 + physicalSeverity));
        int digitalScore = Math.max(0, 100 - (digitalCount * 5 + digitalSeverity));

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
