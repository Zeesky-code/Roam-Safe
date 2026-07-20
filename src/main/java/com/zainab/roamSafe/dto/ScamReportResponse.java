package com.zainab.roamSafe.dto;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import java.time.LocalDateTime;

public record ScamReportResponse(
        Long id,
        String city,
        String description,
        String scamType,
        Integer severityScore,
        String preventionTips,
        String location,
        ScamReportStatus status,
        /** When RoamSafe ingested this record. */
        LocalDateTime createdAt,
        /** When the incident happened, or null if the source gave no date. */
        LocalDateTime reportedAt) {
    public static ScamReportResponse fromEntity(ScamReport report) {
        return new ScamReportResponse(
                report.getId(),
                report.getCity(),
                report.getDescription(),
                report.getScamType(),
                report.getSeverityScore(),
                report.getPreventionTips(),
                report.getLocation(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getReportedAt());
    }
}
