package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.dto.CitySummaryResponse;
import com.zainab.roamSafe.dto.ScamReportRequest;
import com.zainab.roamSafe.dto.ScamReportResponse;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.service.ScamReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend access
public class ScamReportController {

    private final ScamReportService service;

    public ScamReportController(ScamReportService service) {
        this.service = service;
    }

    // US-01: Get reports by city
    @GetMapping("/reports")
    public ResponseEntity<Page<ScamReportResponse>> getReports(
            @RequestParam String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "severityScore") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(service.getReportsByCity(city, PageRequest.of(page, size, sort)));
    }

    // US-02: Get city summary
    @GetMapping("/cities/{city}/summary")
    public ResponseEntity<CitySummaryResponse> getCitySummary(@PathVariable String city) {
        return ResponseEntity.ok(service.getCitySummary(city));
    }

    // US-04: Submit report
    @PostMapping("/reports")
    public ResponseEntity<ScamReportResponse> submitReport(@Valid @RequestBody ScamReportRequest request) {
        return ResponseEntity.ok(service.submitReport(request));
    }

    // US-07: Admin - List pending
    @GetMapping("/reports/pending")
    public ResponseEntity<List<ScamReportResponse>> getPendingReports() {
        return ResponseEntity.ok(service.getPendingReports());
    }

    // US-07: Admin - Approve/Reject
    @PutMapping("/reports/{id}/status")
    public ResponseEntity<ScamReportResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ScamReportStatus status) {
        return ResponseEntity.ok(service.updateReportStatus(id, status));
    }
}
