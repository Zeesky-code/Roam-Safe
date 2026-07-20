package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Practical arrival and living information for a city, taken from Wikivoyage.
 *
 * Stored as a verbatim excerpt with a link back, never as a summary. Summarising
 * "take the Havabus from the airport, 25 TL" risks dropping the qualifier that
 * made it true, and the whole point of this product is that a reader can check
 * the claim against its source. Wikivoyage is CC BY-SA, so attribution is also a
 * licence obligation, not just good manners.
 *
 * One row per (city, topic).
 */
@Entity
@Table(name = "practical_info", uniqueConstraints = @UniqueConstraint(name = "uq_practical_city_topic", columnNames = {
        "city_name", "topic" }))
public class PracticalInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    /** AIRPORT, TRANSPORT, MONEY, CONNECT. */
    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String sourceUrl;
    private String sourceName = "Wikivoyage";
    private String licence = "CC BY-SA 4.0";
    private LocalDateTime retrievedAt = LocalDateTime.now();

    public PracticalInfo() {
    }

    public PracticalInfo(String cityName, String topic, String content, String sourceUrl) {
        this.cityName = cityName;
        this.topic = topic;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.retrievedAt = LocalDateTime.now();
    }

    /** Human label for the topic, for headings. */
    public String getLabel() {
        return switch (topic) {
            case "AIRPORT" -> "Arriving by air";
            case "TRANSPORT" -> "Getting around";
            case "MONEY" -> "Money and payments";
            case "CONNECT" -> "Internet and SIM cards";
            default -> topic;
        };
    }

    public Long getId() { return id; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getLicence() { return licence; }
    public void setLicence(String licence) { this.licence = licence; }
    public LocalDateTime getRetrievedAt() { return retrievedAt; }
    public void setRetrievedAt(LocalDateTime retrievedAt) { this.retrievedAt = retrievedAt; }
}
