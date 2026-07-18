package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A government travel advisory for a country, ingested from an official source
 * (UK FCDO via the GOV.UK content API, and the US State Department). One row per
 * (country, source), so a country can carry advisories from both governments.
 */
@Entity
@Table(name = "advisories", uniqueConstraints = @UniqueConstraint(name = "uq_advisory_country_source", columnNames = {
        "country_name", "source" }))
public class Advisory {
    // Uniqueness of (country, source) is enforced at the DB level (physical
    // column names) and via findByCountryNameAndSource upsert in the ingestion
    // service, so a double-fired startup can't create duplicate rows.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String countryName;

    private String source; // e.g. "UK FCDO"

    private String level; // human-readable, e.g. "Advise against travel to parts"

    private String severity; // low | medium | high | critical (for UI coloring)

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String url; // link back to the source (evidence)

    private String sourceUpdated; // ISO timestamp string from the source

    private LocalDateTime lastFetched;

    public Advisory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSourceUpdated() {
        return sourceUpdated;
    }

    public void setSourceUpdated(String sourceUpdated) {
        this.sourceUpdated = sourceUpdated;
    }

    public LocalDateTime getLastFetched() {
        return lastFetched;
    }

    public void setLastFetched(LocalDateTime lastFetched) {
        this.lastFetched = lastFetched;
    }
}
