package com.zainab.roamSafe.dto;

/**
 * DTO for bulk importing scam reports from LLM-generated JSON.
 * Matches the output format of generate_scam_data.py
 */
public record BulkScamReportRequest(
        String city,
        String name,
        String description,
        String scamType,
        Integer severityScore,
        String preventionTips,
        String neighborhood,
        String safetyZone, // String to handle JSON parsing, converted to enum in service
        Integer safetyRating,
        Boolean isNightTimeIncident,
        String status // Will be ignored - always set to APPROVED for bulk imports
) {
}
