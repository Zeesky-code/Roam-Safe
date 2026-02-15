package com.zainab.roamSafe.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;

@Service
public class ScamService {
    private final ScamReportRepository scamReportRepository;

    public ScamService(ScamReportRepository scamReportRepository) {
        this.scamReportRepository = scamReportRepository;
    }

    public long getTotalScams() {
        return scamReportRepository.count();
    }

    public long getApprovedScams() {
        return scamReportRepository.findByStatus(ScamReportStatus.APPROVED).size();
    }

    public long getPendingScams() {
        return scamReportRepository.findByStatus(ScamReportStatus.PENDING).size();
    }

    public List<Object[]> getTopCities(int limit) {
        return scamReportRepository.findTopCities(PageRequest.of(0, limit));
    }

    public List<ScamReport> getRecentReports(int limit) {
        return scamReportRepository.findTop5ByStatusOrderByCreatedAtDesc(ScamReportStatus.APPROVED);
    }

    public List<ScamReport> getReportsByCity(String city) {
        return scamReportRepository.findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(city,
                ScamReportStatus.APPROVED);
    }
}