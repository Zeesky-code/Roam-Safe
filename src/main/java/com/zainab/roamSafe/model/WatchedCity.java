package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A city a user is keeping an eye on.
 *
 * User.preferredCity only ever held one place, which couldn't back a dashboard
 * that watches several. City is stored by name rather than as a foreign key to
 * match how reports already reference cities, and is compared case-insensitively
 * on the way in.
 */
@Entity
@Table(name = "watched_cities", uniqueConstraints = @UniqueConstraint(name = "uq_watched_user_city", columnNames = {
        "user_id", "city_name" }))
public class WatchedCity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    private LocalDateTime addedAt = LocalDateTime.now();

    public WatchedCity() {
    }

    public WatchedCity(Long userId, String cityName) {
        this.userId = userId;
        this.cityName = cityName;
        this.addedAt = LocalDateTime.now();
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

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
