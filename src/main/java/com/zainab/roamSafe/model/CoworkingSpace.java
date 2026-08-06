package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A coworking space in a city, from OpenStreetMap.
 *
 * Real, named places tagged office=coworking / amenity=coworking_space in OSM,
 * not invented listings: each row is something a mapper recorded, and it carries
 * a link back to the OSM element so it can be checked. This is the honest way to
 * answer the digital-nomad "where can I work from" question - surface what a
 * public dataset actually holds, rather than guessing.
 *
 * One row per (city, OSM element), so a re-run updates rather than duplicates.
 */
@Entity
@Table(name = "coworking_spaces", uniqueConstraints = @UniqueConstraint(name = "uq_coworking_osm", columnNames = "osm_id"))
public class CoworkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false)
    private String name;

    /** OSM element id, e.g. "node/123" - stable identity for upserts + a link. */
    @Column(name = "osm_id", nullable = false)
    private String osmId;

    private Double latitude;
    private Double longitude;
    private String website;

    private LocalDateTime retrievedAt = LocalDateTime.now();

    public CoworkingSpace() {
    }

    public CoworkingSpace(String cityName, String name, String osmId, Double latitude, Double longitude,
            String website) {
        this.cityName = cityName;
        this.name = name;
        this.osmId = osmId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.website = website;
        this.retrievedAt = LocalDateTime.now();
    }

    /** Link back to the source element on OpenStreetMap. */
    public String getSourceUrl() {
        return osmId == null ? null : "https://www.openstreetmap.org/" + osmId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOsmId() {
        return osmId;
    }

    public void setOsmId(String osmId) {
        this.osmId = osmId;
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

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }
}
