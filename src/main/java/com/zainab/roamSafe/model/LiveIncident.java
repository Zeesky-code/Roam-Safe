package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A news article reporting a disruption in a covered city.
 *
 * These are third-party news mentions, not RoamSafe findings, and the
 * distinction is load-bearing. We store the headline exactly as published, the
 * outlet, the date and the link, and we never summarise or re-word it - the
 * moment we paraphrase a headline we are asserting something we haven't
 * verified. Everything shown to a user is attributed and clickable so they can
 * judge the source themselves.
 *
 * One row per source URL, so a re-run can't duplicate a story.
 */
@Entity
@Table(name = "live_incidents", uniqueConstraints = @UniqueConstraint(name = "uq_incident_url", columnNames = "source_url"))
public class LiveIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cityName;

    /** The headline as published. Never rewritten. */
    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "source_url", nullable = false, length = 900)
    private String sourceUrl;

    private String sourceDomain;

    /** When the article was published, from the source. */
    private LocalDateTime publishedAt;

    private LocalDateTime ingestedAt = LocalDateTime.now();

    public LiveIncident() {
    }

    public LiveIncident(String cityName, String title, String sourceUrl, String sourceDomain,
            LocalDateTime publishedAt) {
        this.cityName = cityName;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.sourceDomain = sourceDomain;
        this.publishedAt = publishedAt;
        this.ingestedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceDomain() { return sourceDomain; }
    public void setSourceDomain(String sourceDomain) { this.sourceDomain = sourceDomain; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
}
