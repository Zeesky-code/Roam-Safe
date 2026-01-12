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

    // Find approved reports for a city
    Page<ScamReport> findByCityIgnoreCaseAndStatus(String city, ScamReportStatus status, Pageable pageable);

    // Find all pending reports for moderation
    List<ScamReport> findByStatusOrderByCreatedAtDesc(ScamReportStatus status);
}
