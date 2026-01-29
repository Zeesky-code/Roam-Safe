package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScamReportRepository extends JpaRepository<ScamReport, Long> {

    // Find approved reports for a city (paginated for API)
    Page<ScamReport> findByCityIgnoreCaseAndStatus(String city, ScamReportStatus status, Pageable pageable);

    // Find approved reports for a city (list for web UI)
    List<ScamReport> findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(String city, ScamReportStatus status);

    // Find all pending reports for moderation
    // Find all reports by status
    List<ScamReport> findByStatus(ScamReportStatus status);

    // Find all pending reports for moderation
    List<ScamReport> findByStatusOrderByCreatedAtDesc(ScamReportStatus status);
}
