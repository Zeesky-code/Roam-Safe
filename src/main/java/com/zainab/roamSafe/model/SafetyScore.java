package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_scores")
public class SafetyScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    @NotNull
    private City city;
    
    @Column(name = "overall_score", nullable = false)
    @Min(1)
    @Max(100)
    @NotNull
    private Integer overallScore;
    
    @Column(name = "financial_risk_score", nullable = false)
    @Min(1)
    @Max(100)
    @NotNull
    private Integer financialRiskScore;
    
    @Column(name = "physical_risk_score", nullable = false)
    @Min(1)
    @Max(100)
    @NotNull
    private Integer physicalRiskScore;
    
    @Column(name = "digital_risk_score", nullable = false)
    @Min(1)
    @Max(100)
    @NotNull
    private Integer digitalRiskScore;
    
    @Column(name = "total_scam_count", nullable = false)
    @Min(0)
    @NotNull
    private Integer totalScamCount;
    
    @Column(name = "recent_scam_count", nullable = false)
    @Min(0)
    @NotNull
    private Integer recentScamCount; // Last 6 months
    
    @Column(name = "confidence_level", nullable = false)
    @Min(0)
    @Max(1)
    @NotNull
    private Double confidenceLevel; // 0.0-1.0
    
    @Column(name = "top_scam_types", columnDefinition = "TEXT")
    private String topScamTypes; // JSON array of top 3 scam types
    
    @Column(name = "ai_insights", columnDefinition = "TEXT")
    private String aiInsights; // Premium AI-generated insights
    
    @Column(name = "last_calculated", nullable = false)
    @NotNull
    private LocalDateTime lastCalculated;
    
    @Column(name = "last_updated", nullable = false)
    @NotNull
    private LocalDateTime lastUpdated;
    
    // Constructors
    public SafetyScore() {}
    
    public SafetyScore(City city, Integer overallScore, Integer financialRiskScore, 
                      Integer physicalRiskScore, Integer digitalRiskScore, 
                      Integer totalScamCount, Integer recentScamCount, 
                      Double confidenceLevel) {
        this.city = city;
        this.overallScore = overallScore;
        this.financialRiskScore = financialRiskScore;
        this.physicalRiskScore = physicalRiskScore;
        this.digitalRiskScore = digitalRiskScore;
        this.totalScamCount = totalScamCount;
        this.recentScamCount = recentScamCount;
        this.confidenceLevel = confidenceLevel;
        this.lastCalculated = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public City getCity() {
        return city;
    }
    
    public void setCity(City city) {
        this.city = city;
    }
    
    public Integer getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }
    
    public Integer getFinancialRiskScore() {
        return financialRiskScore;
    }
    
    public void setFinancialRiskScore(Integer financialRiskScore) {
        this.financialRiskScore = financialRiskScore;
    }
    
    public Integer getPhysicalRiskScore() {
        return physicalRiskScore;
    }
    
    public void setPhysicalRiskScore(Integer physicalRiskScore) {
        this.physicalRiskScore = physicalRiskScore;
    }
    
    public Integer getDigitalRiskScore() {
        return digitalRiskScore;
    }
    
    public void setDigitalRiskScore(Integer digitalRiskScore) {
        this.digitalRiskScore = digitalRiskScore;
    }
    
    public Integer getTotalScamCount() {
        return totalScamCount;
    }
    
    public void setTotalScamCount(Integer totalScamCount) {
        this.totalScamCount = totalScamCount;
    }
    
    public Integer getRecentScamCount() {
        return recentScamCount;
    }
    
    public void setRecentScamCount(Integer recentScamCount) {
        this.recentScamCount = recentScamCount;
    }
    
    public Double getConfidenceLevel() {
        return confidenceLevel;
    }
    
    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }
    
    public String getTopScamTypes() {
        return topScamTypes;
    }
    
    public void setTopScamTypes(String topScamTypes) {
        this.topScamTypes = topScamTypes;
    }
    
    public String getAiInsights() {
        return aiInsights;
    }
    
    public void setAiInsights(String aiInsights) {
        this.aiInsights = aiInsights;
    }
    
    public LocalDateTime getLastCalculated() {
        return lastCalculated;
    }
    
    public void setLastCalculated(LocalDateTime lastCalculated) {
        this.lastCalculated = lastCalculated;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    // Utility methods
    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.lastCalculated == null) {
            this.lastCalculated = now;
        }
        if (this.lastUpdated == null) {
            this.lastUpdated = now;
        }
    }
    
    /**
     * Check if the score is stale and needs recalculation
     * @param hoursThreshold Number of hours after which score is considered stale
     * @return true if score is stale
     */
    public boolean isStale(int hoursThreshold) {
        return this.lastCalculated.isBefore(LocalDateTime.now().minusHours(hoursThreshold));
    }
    
    /**
     * Get color-coded indicator based on overall score
     * @return Color indicator (GREEN, YELLOW, RED)
     */
    public String getColorIndicator() {
        if (overallScore >= 70) {
            return "GREEN";
        } else if (overallScore >= 40) {
            return "YELLOW";
        } else {
            return "RED";
        }
    }
}