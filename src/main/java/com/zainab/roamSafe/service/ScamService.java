package com.zainab.roamSafe.service;

import java.util.List;
import java.util.ArrayList;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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
        // Mock implementation or use a custom query in repository if needed
        // For now returning empty list to fix compilation
        return new ArrayList<>();
    }
}