package com.zainab.roamSafe.dto;

/**
 * DTO for bulk importing scam reports.
 *
 * Matches the output of scripts/scrape_wikivoyage.py, which extracts reports
 * from Wikivoyage "Stay safe" sections and carries the article URL through as
 * sourceUrl/sourceName. Imports are auto-approved, so the source fields are the
 * only record of where an entry came from — a report that arrives without them
 * cannot be traced by anyone later.
 */
public record BulkScamReportRequest(
        String city,
        String name,
        String description,
        String scamType,
        String category, // High-level category (Theft, Financial, Transport, ...)
        Integer severityScore,
        String preventionTips,
        String neighborhood,
        String safetyZone, // String to handle JSON parsing, converted to enum in service
        Integer safetyRating,
        Boolean isNightTimeIncident,
        String status, // Will be ignored - always set to APPROVED for bulk imports
        String sourceUrl, // Where this report can be verified
        String sourceName // Human-readable origin, e.g. "Wikivoyage"
) {
}
