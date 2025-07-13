package com.zainab.roamSafe.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class SubmittedScam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 3000)
    private String description;

    @Column(length = 2000)
    private String prevention;

    private String city;
    
    // Safety Zone Fields
    private String neighborhood;
    
    private String incidentType; // SCAM, HARASSMENT, THEFT, POSITIVE
    
    private Integer safetyRating; // 1-5 scale
    
    private Boolean isNightTimeIncident = false;
    
    private String safetyZone; // GREEN, YELLOW, RED

    private boolean reviewed = false;

    private LocalDateTime submittedAt = LocalDateTime.now();

    // Default constructor
    public SubmittedScam() {}

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrevention() {
        return prevention;
    }

    public void setPrevention(String prevention) {
        this.prevention = prevention;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    // Safety Zone Getters and Setters
    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
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

    public void setIsNightTimeIncident(Boolean isNightTimeIncident) {
        this.isNightTimeIncident = isNightTimeIncident;
    }

    public String getSafetyZone() {
        return safetyZone;
    }

    public void setSafetyZone(String safetyZone) {
        this.safetyZone = safetyZone;
    }
}
