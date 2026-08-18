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

    /** Most recent approved reports, honouring {@code limit} (feed stream). */
    public List<ScamReport> getRecentApproved(int limit) {
        return scamReportRepository
                .findByStatusOrderByCreatedAtDesc(ScamReportStatus.APPROVED)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * An even spread across every covered city, for the map.
     *
     * The map cannot use the recency feed. Almost every report was bulk
     * imported, so createdAt is ingestion order rather than anything about the
     * world, and taking the newest 200 rendered the tail of the last upload:
     * 22 cities out of 75, with Paris, Rome, London and Tokyo absent entirely
     * while the page presented itself as global coverage.
     *
     * Capping per city instead gives every covered city a pin. The ordering
     * stays the neutral ingestion order on purpose: taking each city's most
     * severe reports sounds better but is selection bias, and it rendered a map
     * on which no city anywhere was green - the legend's "Reassuring / safe"
     * case became unreachable and the page overstated risk everywhere. The
     * markers should show what the data looks like, not its worst tail.
     */
    public List<ScamReport> getMapSpread(int perCity) {
        java.util.Map<String, Integer> taken = new java.util.HashMap<>();
        List<ScamReport> out = new java.util.ArrayList<>();
        for (ScamReport r : scamReportRepository.findByStatusOrderByCreatedAtDesc(ScamReportStatus.APPROVED)) {
            String city = r.getCity();
            if (city == null || city.isBlank()) {
                continue;
            }
            int n = taken.getOrDefault(city, 0);
            if (n < perCity) {
                taken.put(city, n + 1);
                out.add(r);
            }
        }
        return out;
    }

    public List<ScamReport> getReportsByCity(String city) {
        return scamReportRepository.findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(city,
                ScamReportStatus.APPROVED);
    }
}