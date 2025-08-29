package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.City;
import com.zainab.roamSafe.model.SafetyScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SafetyScoreHistoryRepository extends JpaRepository<SafetyScoreHistory, Long> {
    
    /**
     * Find all history records for a specific city
     */
    List<SafetyScoreHistory> findByCity(City city);
    
    /**
     * Find all history records for a specific city ID
     */
    List<SafetyScoreHistory> findByCityId(Long cityId);
    
    /**
     * Find history records for a city by name (case-insensitive)
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh JOIN ssh.city c WHERE LOWER(c.name) = LOWER(:cityName) ORDER BY ssh.calculatedAt DESC")
    List<SafetyScoreHistory> findByCityName(@Param("cityName") String cityName);
    
    /**
     * Find history records for a city ordered by calculation date (most recent first)
     */
    List<SafetyScoreHistory> findByCityOrderByCalculatedAtDesc(City city);
    
    /**
     * Find the most recent history record for a city
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ssh.city = :city ORDER BY ssh.calculatedAt DESC LIMIT 1")
    SafetyScoreHistory findMostRecentByCity(@Param("city") City city);
    
    /**
     * Find history records within a date range
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ssh.calculatedAt BETWEEN :startDate AND :endDate ORDER BY ssh.calculatedAt DESC")
    List<SafetyScoreHistory> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find history records for a city within a date range
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ssh.city = :city AND ssh.calculatedAt BETWEEN :startDate AND :endDate ORDER BY ssh.calculatedAt DESC")
    List<SafetyScoreHistory> findByCityAndDateRange(@Param("city") City city, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find significant score changes (above threshold)
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ABS(ssh.newScore - COALESCE(ssh.previousScore, 0)) >= :threshold ORDER BY ssh.calculatedAt DESC")
    List<SafetyScoreHistory> findSignificantChanges(@Param("threshold") Integer threshold);
    
    /**
     * Find score improvements (positive changes)
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ssh.newScore > COALESCE(ssh.previousScore, 0) ORDER BY (ssh.newScore - COALESCE(ssh.previousScore, 0)) DESC")
    List<SafetyScoreHistory> findScoreImprovements();
    
    /**
     * Find score deteriorations (negative changes)
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ssh.newScore < COALESCE(ssh.previousScore, 100) ORDER BY (COALESCE(ssh.previousScore, 100) - ssh.newScore) DESC")
    List<SafetyScoreHistory> findScoreDeteriorations();
    
    /**
     * Find history records with specific change reasons
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ssh.changeReason LIKE %:reason% ORDER BY ssh.calculatedAt DESC")
    List<SafetyScoreHistory> findByChangeReason(@Param("reason") String reason);
    
    /**
     * Find recent history records (within last N days)
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ssh.calculatedAt >= :threshold ORDER BY ssh.calculatedAt DESC")
    List<SafetyScoreHistory> findRecentHistory(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Get count of calculations for a city
     */
    @Query("SELECT COUNT(ssh) FROM SafetyScoreHistory ssh WHERE ssh.city = :city")
    Long countByCity(@Param("city") City city);
    
    /**
     * Get count of calculations within date range
     */
    @Query("SELECT COUNT(ssh) FROM SafetyScoreHistory ssh WHERE ssh.calculatedAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find cities with most frequent score updates
     */
    @Query("SELECT ssh.city, COUNT(ssh) as updateCount FROM SafetyScoreHistory ssh GROUP BY ssh.city ORDER BY updateCount DESC")
    List<Object[]> findCitiesWithMostUpdates();
    
    /**
     * Get average score change over time
     */
    @Query("SELECT AVG(ssh.newScore - COALESCE(ssh.previousScore, 0)) FROM SafetyScoreHistory ssh WHERE ssh.previousScore IS NOT NULL")
    Double getAverageScoreChange();
    
    /**
     * Find history records for cities with large scam count changes
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ABS(COALESCE(ssh.scamCountChange, 0)) >= :threshold ORDER BY ABS(ssh.scamCountChange) DESC")
    List<SafetyScoreHistory> findLargeScamCountChanges(@Param("threshold") Integer threshold);
    
    /**
     * Find history records for cities with significant confidence level changes
     */
    @Query("SELECT ssh FROM SafetyScoreHistory ssh WHERE ABS(COALESCE(ssh.confidenceLevelChange, 0)) >= :threshold ORDER BY ABS(ssh.confidenceLevelChange) DESC")
    List<SafetyScoreHistory> findSignificantConfidenceChanges(@Param("threshold") Double threshold);
    
    /**
     * Delete old history records (older than specified date)
     */
    @Query("DELETE FROM SafetyScoreHistory ssh WHERE ssh.calculatedAt < :threshold")
    void deleteOldRecords(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Delete all history records for a specific city
     */
    void deleteByCity(City city);
    
    /**
     * Delete all history records for a specific city ID
     */
    void deleteByCityId(Long cityId);
}