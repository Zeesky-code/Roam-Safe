package com.zainab.roamSafe.service;

import com.zainab.roamSafe.dto.ScamReportRequest;
import com.zainab.roamSafe.dto.ScamReportResponse;
import com.zainab.roamSafe.dto.CitySummaryResponse;
import com.zainab.roamSafe.model.CitySummary;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.CitySummaryRepository;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ScamReportService {

    private final ScamReportRepository scamReportRepository;
    private final CitySummaryRepository citySummaryRepository;

    public ScamReportService(ScamReportRepository scamReportRepository, CitySummaryRepository citySummaryRepository) {
        this.scamReportRepository = scamReportRepository;
        this.citySummaryRepository = citySummaryRepository;
    }

    // US-01: Search reports by city
    public Page<ScamReportResponse> getReportsByCity(String city, Pageable pageable) {
        return scamReportRepository.findByCityIgnoreCaseAndStatus(city, ScamReportStatus.APPROVED, pageable)
                .map(ScamReportResponse::fromEntity);
    }

    // US-04: Submit a new report
    @Transactional
    public ScamReportResponse submitReport(ScamReportRequest request) {
        ScamReport report = new ScamReport(
                request.city(),
                request.description(),
                request.scamType(),
                request.severityScore(),
                request.preventionTips(),
                request.location());
        ScamReport savedReport = scamReportRepository.save(report);
        return ScamReportResponse.fromEntity(savedReport);
    }

    // US-02: Get or generate city summary
    public CitySummaryResponse getCitySummary(String city) {
        // Try to fetch from cache
        Optional<CitySummary> cachedSummary = citySummaryRepository.findByCityIgnoreCase(city);

        if (cachedSummary.isPresent()) {
            return CitySummaryResponse.fromEntity(cachedSummary.get());
        }

        // Fallback: Generate a simple summary (In real world, this would call LLM)
        String generatedText = "AI-generated summary for " + city
                + ": Be cautious of tourist traps and pickpockets in crowded areas. Avoid unlicensed taxis.";

        CitySummary newSummary = new CitySummary(city, generatedText);
        citySummaryRepository.save(newSummary);

        return new CitySummaryResponse(city, generatedText, LocalDateTime.now(), false);
    }

    // US-07: List pending reports
    public List<ScamReportResponse> getPendingReports() {
        return scamReportRepository.findByStatusOrderByCreatedAtDesc(ScamReportStatus.PENDING)
                .stream()
                .map(ScamReportResponse::fromEntity)
                .toList();
    }

    // US-07: Moderate report
    @Transactional
    public ScamReportResponse updateReportStatus(Long id, ScamReportStatus status) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        ScamReport report = scamReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(status);
        return ScamReportResponse.fromEntity(scamReportRepository.save(report));
    }
}
