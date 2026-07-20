package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Recorded crime for a UK city in one calendar month, from data.police.uk.
 *
 * This is official police-recorded data, not a traveler account: it carries a
 * real month, an official category, and a count taken from the source rather
 * than inferred. It is kept in its own table instead of being folded into
 * scam_reports precisely so the two never blur — a report is somebody's
 * experience, this is a statistic, and they warrant different confidence.
 *
 * One row per (city, month, category). Uniqueness is enforced at the DB level so
 * a re-run can't duplicate rows; the ingestion service upserts.
 *
 * Counts cover roughly a one-mile radius around the city-centre coordinate the
 * ingestion queried, which is what the source's street-level endpoint returns.
 * They are therefore a central-area sample, not a whole-city total, and must be
 * described that way wherever they surface.
 */
@Entity
@Table(name = "police_crime_stats", uniqueConstraints = @UniqueConstraint(name = "uq_police_city_month_category", columnNames = {
        "city_name", "month", "category" }))
public class PoliceCrimeStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cityName;

    /** Calendar month of the crimes, "YYYY-MM", straight from the source. */
    @Column(nullable = false)
    private String month;

    /** Official category slug, e.g. "violent-crime", "theft-from-the-person". */
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer count;

    /** Centre of the queried area, so the sample is reproducible. */
    private Double latitude;
    private Double longitude;

    private LocalDateTime lastFetched;

    public PoliceCrimeStat() {
    }

    public PoliceCrimeStat(String cityName, String month, String category, int count,
            double latitude, double longitude) {
        this.cityName = cityName;
        this.month = month;
        this.category = category;
        this.count = count;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastFetched = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getLastFetched() {
        return lastFetched;
    }

    public void setLastFetched(LocalDateTime lastFetched) {
        this.lastFetched = lastFetched;
    }
}
