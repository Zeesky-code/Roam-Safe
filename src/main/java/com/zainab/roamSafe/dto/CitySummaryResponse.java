package com.zainab.roamSafe.dto;

import com.zainab.roamSafe.model.CitySummary;
import java.time.LocalDateTime;

public record CitySummaryResponse(
        String city,
        String summary,
        LocalDateTime lastUpdated,
        boolean isCached) {
    public static CitySummaryResponse fromEntity(CitySummary summary) {
        return new CitySummaryResponse(
                summary.getCity(),
                summary.getSummaryText(),
                summary.getLastUpdated(),
                true);
    }
}
