package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scam_reports")
public class ScamReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private String scamType;

    @Column(nullable = false)
    private Integer severityScore; // 1-10

    @Column(columnDefinition = "TEXT")
    private String preventionTips;

    private String location; // Specific spot like "Central Station"

    @Enumerated(EnumType.STRING)
    private ScamReportStatus status = ScamReportStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ScamReport() {
        this.createdAt = LocalDateTime.now();
    }

    public ScamReport(String city, String description, String scamType, Integer severityScore, String preventionTips,
            String location) {
        this();
        this.city = city;
        this.description = description;
        this.scamType = scamType;
        this.severityScore = severityScore;
        this.preventionTips = preventionTips;
        this.location = location;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScamType() {
        return scamType;
    }

    public void setScamType(String scamType) {
        this.scamType = scamType;
    }

    public Integer getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(Integer severityScore) {
        this.severityScore = severityScore;
    }

    public String getPreventionTips() {
        return preventionTips;
    }

    public void setPreventionTips(String preventionTips) {
        this.preventionTips = preventionTips;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ScamReportStatus getStatus() {
        return status;
    }

    public void setStatus(ScamReportStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
