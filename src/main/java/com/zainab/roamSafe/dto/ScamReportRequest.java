package com.zainab.roamSafe.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScamReportRequest(
        @NotBlank(message = "City is required") String city,

        @NotBlank(message = "Description is required") String description,

        @NotBlank(message = "Scam type is required") String scamType,

        @NotNull(message = "Severity score is required") @Min(value = 1, message = "Severity score must be at least 1") @Max(value = 10, message = "Severity score must be at most 10") Integer severityScore,

        String preventionTips,

        String location) {
}
