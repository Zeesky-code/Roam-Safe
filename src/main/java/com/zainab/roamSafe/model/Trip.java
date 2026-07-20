package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A planned trip to one city between two dates.
 *
 * Deliberately minimal: a destination and a date range is everything the
 * dashboard actually needs, and it keeps the model to things a user genuinely
 * told us rather than inferred travel details.
 */
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Trip() {
    }

    public Trip(Long userId, String cityName, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.cityName = cityName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = LocalDateTime.now();
    }

    /** Nights away, derived rather than stored so it can't drift from the dates. */
    public long getNights() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(startDate, endDate));
    }

    /** Days until departure; negative once the trip has started. */
    public long getDaysUntil() {
        return startDate == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), startDate);
    }

    public boolean isUpcoming() {
        return endDate != null && !endDate.isBefore(LocalDate.now());
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
