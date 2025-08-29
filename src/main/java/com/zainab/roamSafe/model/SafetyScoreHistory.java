package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_score_history")
public class SafetyScoreHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    @NotNull
    private City city;
    
    @Column(name = "previous_score")
    private Integer previousScore;
    
    @Column(name = "new_score", nullable = false)
    @NotNull
    private Integer newScore;
    
    @Column(name = "previous_financial_score")
    private Integer previousFinancialScore;
    
    @Column(name = "new_financial_score", nullable = false)
    @NotNull
    private Integer newFinancialScore;
    
    @Column(name = "previous_physical_score")
    private Integer previousPhysicalScore;
    
    @Column(name = "new_physical_score", nullable = false)
    @NotNull
    private Integer newPhysicalScore;
    
    @Column(name = "previous_digital_score")
    private Integer previousDigitalScore;
    
    @Column(name = "new_digital_score", nullable = false)
    @NotNull
    private Integer newDigitalScore;
    
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;
    
    @Column(name = "calculated_at", nullable = false)
    @NotNull
    private LocalDateTime calculatedAt;
    
    @Column(name = "scam_count_change")
    private Integer scamCountChange; // Difference in scam count since last calculation
    
    @Column(name = "confidence_level_change")
    private Double confidenceLevelChange; // Difference in confidence level
    
    // Constructors
    public SafetyScoreHistory() {}
    
    public SafetyScoreHistory(City city, Integer previousScore, Integer newScore,
                             Integer previousFinancialScore, Integer newFinancialScore,
                             Integer previousPhysicalScore, Integer newPhysicalScore,
                             Integer previousDigitalScore, Integer newDigitalScore,
                             String changeReason) {
        this.city = city;
        this.previousScore = previousScore;
        this.newScore = newScore;
        this.previousFinancialScore = previousFinancialScore;
        this.newFinancialScore = newFinancialScore;
        this.previousPhysicalScore = previousPhysicalScore;
        this.newPhysicalScore = newPhysicalScore;
        this.previousDigitalScore = previousDigitalScore;
        this.newDigitalScore = newDigitalScore;
        this.changeReason = changeReason;
        this.calculatedAt = LocalDateTime.now();
    }
    
    // Convenience constructor for creating from SafetyScore objects
    public SafetyScoreHistory(SafetyScore previousScore, SafetyScore newScore, String changeReason) {
        this.city = newScore.getCity();
        this.previousScore = previousScore != null ? previousScore.getOverallScore() : null;
        this.newScore = newScore.getOverallScore();
        this.previousFinancialScore = previousScore != null ? previousScore.getFinancialRiskScore() : null;
        this.newFinancialScore = newScore.getFinancialRiskScore();
        this.previousPhysicalScore = previousScore != null ? previousScore.getPhysicalRiskScore() : null;
        this.newPhysicalScore = newScore.getPhysicalRiskScore();
        this.previousDigitalScore = previousScore != null ? previousScore.getDigitalRiskScore() : null;
        this.newDigitalScore = newScore.getDigitalRiskScore();
        this.changeReason = changeReason;
        this.calculatedAt = LocalDateTime.now();
        
        // Calculate changes
        if (previousScore != null) {
            this.scamCountChange = newScore.getTotalScamCount() - previousScore.getTotalScamCount();
            this.confidenceLevelChange = newScore.getConfidenceLevel() - previousScore.getConfidenceLevel();
        }
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
    
    public Integer getPreviousScore() {
        return previousScore;
    }
    
    public void setPreviousScore(Integer previousScore) {
        this.previousScore = previousScore;
    }
    
    public Integer getNewScore() {
        return newScore;
    }
    
    public void setNewScore(Integer newScore) {
        this.newScore = newScore;
    }
    
    public Integer getPreviousFinancialScore() {
        return previousFinancialScore;
    }
    
    public void setPreviousFinancialScore(Integer previousFinancialScore) {
        this.previousFinancialScore = previousFinancialScore;
    }
    
    public Integer getNewFinancialScore() {
        return newFinancialScore;
    }
    
    public void setNewFinancialScore(Integer newFinancialScore) {
        this.newFinancialScore = newFinancialScore;
    }
    
    public Integer getPreviousPhysicalScore() {
        return previousPhysicalScore;
    }
    
    public void setPreviousPhysicalScore(Integer previousPhysicalScore) {
        this.previousPhysicalScore = previousPhysicalScore;
    }
    
    public Integer getNewPhysicalScore() {
        return newPhysicalScore;
    }
    
    public void setNewPhysicalScore(Integer newPhysicalScore) {
        this.newPhysicalScore = newPhysicalScore;
    }
    
    public Integer getPreviousDigitalScore() {
        return previousDigitalScore;
    }
    
    public void setPreviousDigitalScore(Integer previousDigitalScore) {
        this.previousDigitalScore = previousDigitalScore;
    }
    
    public Integer getNewDigitalScore() {
        return newDigitalScore;
    }
    
    public void setNewDigitalScore(Integer newDigitalScore) {
        this.newDigitalScore = newDigitalScore;
    }
    
    public String getChangeReason() {
        return changeReason;
    }
    
    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
    
    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
    
    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
    
    public Integer getScamCountChange() {
        return scamCountChange;
    }
    
    public void setScamCountChange(Integer scamCountChange) {
        this.scamCountChange = scamCountChange;
    }
    
    public Double getConfidenceLevelChange() {
        return confidenceLevelChange;
    }
    
    public void setConfidenceLevelChange(Double confidenceLevelChange) {
        this.confidenceLevelChange = confidenceLevelChange;
    }
    
    // Utility methods
    @PrePersist
    public void prePersist() {
        if (this.calculatedAt == null) {
            this.calculatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Get the overall score change
     * @return Positive value if score improved, negative if it worsened
     */
    public Integer getOverallScoreChange() {
        if (previousScore == null) {
            return null;
        }
        return newScore - previousScore;
    }
    
    /**
     * Check if this represents a significant score change
     * @param threshold Minimum change to be considered significant
     * @return true if the change is significant
     */
    public boolean isSignificantChange(int threshold) {
        Integer change = getOverallScoreChange();
        return change != null && Math.abs(change) >= threshold;
    }
    
    /**
     * Get a human-readable description of the change
     * @return Description of the change
     */
    public String getChangeDescription() {
        Integer change = getOverallScoreChange();
        if (change == null) {
            return "Initial score calculation";
        } else if (change > 0) {
            return String.format("Score improved by %d points", change);
        } else if (change < 0) {
            return String.format("Score decreased by %d points", Math.abs(change));
        } else {
            return "Score remained unchanged";
        }
    }
}