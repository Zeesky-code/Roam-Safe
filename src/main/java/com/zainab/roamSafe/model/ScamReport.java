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
    private String scamType; // Specific type e.g "Gold Ring Scam"

    @Column(name = "category")
    private String category; // High-level category e.g. "Social", "Transportation"

    @Column(nullable = false)
    private Integer severityScore; // 1-10

    @Column(columnDefinition = "TEXT")
    private String preventionTips;

    private String location; // Specific spot like "Central Station"

    @Enumerated(EnumType.STRING)
    private ScamReportStatus status = ScamReportStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String name; // Title of the report

    private String neighborhood;

    @Enumerated(EnumType.STRING)
    private SafetyZone safetyZone;

    private Integer safetyRating; // 1-5

    private Boolean isNightTimeIncident;

    public ScamReport() {
        this.createdAt = LocalDateTime.now();
    }

    // Simplified constructor for basic report submission (backward compatible)
    public ScamReport(String city, String description, String scamType, Integer severityScore,
            String preventionTips, String location) {
        this();
        this.city = city;
        this.name = scamType + " in " + city; // Auto-generate a name
        this.description = description;
        this.scamType = scamType;
        this.severityScore = severityScore;
        this.preventionTips = preventionTips;
        this.neighborhood = location;
        this.safetyZone = SafetyZone.YELLOW; // Default to medium caution
        this.safetyRating = 3; // Default middle rating
        this.isNightTimeIncident = false;
    }

    public ScamReport(String city, String name, String description, String scamType, Integer severityScore,
            String preventionTips,
            String neighborhood, SafetyZone safetyZone, Integer safetyRating, Boolean isNightTimeIncident) {
        this();
        this.city = city;
        this.name = name;
        this.description = description;
        this.scamType = scamType;
        this.severityScore = severityScore;
        this.preventionTips = preventionTips;
        this.neighborhood = neighborhood;
        this.safetyZone = safetyZone;
        this.safetyRating = safetyRating;
        this.isNightTimeIncident = isNightTimeIncident;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
        return neighborhood; // Map location to neighborhood for backward compatibility if needed, or keep
                             // separate
    }

    public void setLocation(String location) {
        this.neighborhood = location;
    }

    public SafetyZone getSafetyZone() {
        return safetyZone;
    }

    public void setSafetyZone(SafetyZone safetyZone) {
        this.safetyZone = safetyZone;
    }

    public Integer getSafetyRating() {
        return safetyRating;
    }

    public void setSafetyRating(Integer safetyRating) {
        this.safetyRating = safetyRating;
    }

    public Boolean getIsNightTimeIncident() {
        return isNightTimeIncident;
    }

    public void setIsNightTimeIncident(Boolean nightTimeIncident) {
        isNightTimeIncident = nightTimeIncident;
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
