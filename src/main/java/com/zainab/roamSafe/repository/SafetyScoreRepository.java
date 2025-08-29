package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.City;
import com.zainab.roamSafe.model.SafetyScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SafetyScoreRepository extends JpaRepository<SafetyScore, Long> {
    
    /**
     * Find safety score by city
     */
    Optional<SafetyScore> findByCity(City city);
    
    /**
     * Find safety score by city ID
     */
    Optional<SafetyScore> findByCityId(Long cityId);
    
    /**
     * Find safety score by city name (case-insensitive)
     */
    @Query("SELECT ss FROM SafetyScore ss JOIN ss.city c WHERE LOWER(c.name) = LOWER(:cityName)")
    Optional<SafetyScore> findByCityName(@Param("cityName") String cityName);
    
    /**
     * Find safety score by city name and country (case-insensitive)
     */
    @Query("SELECT ss FROM SafetyScore ss JOIN ss.city c WHERE LOWER(c.name) = LOWER(:cityName) AND LOWER(c.country) = LOWER(:country)")
    Optional<SafetyScore> findByCityNameAndCountry(@Param("cityName") String cityName, @Param("country") String country);
    
    /**
     * Find safety scores for multiple cities by name
     */
    @Query("SELECT ss FROM SafetyScore ss JOIN ss.city c WHERE LOWER(c.name) IN :cityNames")
    List<SafetyScore> findByCityNames(@Param("cityNames") List<String> cityNames);
    
    /**
     * Find all safety scores that are stale (need recalculation)
     */
    @Query("SELECT ss FROM SafetyScore ss WHERE ss.lastCalculated < :threshold")
    List<SafetyScore> findStaleScores(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find safety scores with low confidence levels
     */
    @Query("SELECT ss FROM SafetyScore ss WHERE ss.confidenceLevel < :threshold")
    List<SafetyScore> findLowConfidenceScores(@Param("threshold") Double threshold);
    
    /**
     * Find top N safest cities by overall score
     */
    @Query("SELECT ss FROM SafetyScore ss ORDER BY ss.overallScore DESC")
    List<SafetyScore> findTopSafestCities();
    
    /**
     * Find top N riskiest cities by overall score
     */
    @Query("SELECT ss FROM SafetyScore ss ORDER BY ss.overallScore ASC")
    List<SafetyScore> findTopRiskiestCities();
    
    /**
     * Find cities with scores in a specific range
     */
    @Query("SELECT ss FROM SafetyScore ss WHERE ss.overallScore BETWEEN :minScore AND :maxScore ORDER BY ss.overallScore DESC")
    List<SafetyScore> findByScoreRange(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);
    
    /**
     * Find cities with high financial risk
     */
    @Query("SELECT ss FROM SafetyScore ss WHERE ss.financialRiskScore < :threshold ORDER BY ss.financialRiskScore ASC")
    List<SafetyScore> findHighFinancialRiskCities(@Param("threshold") Integer threshold);
    
    /**
     * Find cities with high physical risk
     */
    @Query("SELECT ss FROM SafetyScore ss WHERE ss.physicalRiskScore < :threshold ORDER BY ss.physicalRiskScore ASC")
    List<SafetyScore> findHighPhysicalRiskCities(@Param("threshold") Integer threshold);
    
    /**
     * Find cities with high digital risk
     */
    @Query("SELECT ss FROM SafetyScore ss WHERE ss.digitalRiskScore < :threshold ORDER BY ss.digitalRiskScore ASC")
    List<SafetyScore> findHighDigitalRiskCities(@Param("threshold") Integer threshold);
    
    /**
     * Find cities with recent scam activity
     */
    @Query("SELECT ss FROM SafetyScore ss WHERE ss.recentScamCount > :threshold ORDER BY ss.recentScamCount DESC")
    List<SafetyScore> findCitiesWithRecentScamActivity(@Param("threshold") Integer threshold);
    
    /**
     * Get average safety score across all cities
     */
    @Query("SELECT AVG(ss.overallScore) FROM SafetyScore ss")
    Double getAverageOverallScore();
    
    /**
     * Get count of cities by score range
     */
    @Query("SELECT COUNT(ss) FROM SafetyScore ss WHERE ss.overallScore BETWEEN :minScore AND :maxScore")
    Long countByScoreRange(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);
    
    /**
     * Find cities that need AI insights update (premium feature)
     */
    @Query("SELECT ss FROM SafetyScore ss WHERE ss.aiInsights IS NULL OR ss.lastUpdated < :threshold")
    List<SafetyScore> findCitiesNeedingAIInsights(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Delete safety scores for a specific city
     */
    void deleteByCity(City city);
    
    /**
     * Delete safety scores for a specific city ID
     */
    void deleteByCityId(Long cityId);
}