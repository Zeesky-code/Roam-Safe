package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Emergency telephone numbers for a country.
 *
 * This is the most safety-critical data in the product: a wrong number here is
 * worse than no number, because someone may dial it in an emergency and lose
 * time. Two rules follow from that, and both are enforced by how this is
 * populated rather than by convention:
 *
 * 1. Values come from a structured source (Wikidata property P2852, where each
 *    number is a typed statement with a service qualifier), never from a model's
 *    recollection and never from parsing prose. An earlier attempt to scrape the
 *    equivalent Wikipedia tables produced misaligned columns - fire numbers
 *    containing paragraphs of text - which is exactly the failure mode that must
 *    not reach a database.
 * 2. Every row carries its source and the date it was retrieved, so a reader can
 *    check it and a stale entry is visible rather than silently trusted.
 *
 * A null service-specific number means the source had no separate number for it,
 * which usually means the universal number covers it. Null is never rendered as
 * a guess.
 */
@Entity
@Table(name = "emergency_numbers", uniqueConstraints = @UniqueConstraint(name = "uq_emergency_country", columnNames = "country"))
public class EmergencyNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String country;

    /** Universal number, e.g. 112 or 911. Usually what a traveler wants. */
    private String general;

    private String police;

    private String fire;

    private String ambulance;

    /** Where these came from, kept with the data so it can be checked. */
    private String source;

    private String sourceUrl;

    private LocalDateTime retrievedAt;

    public EmergencyNumber() {
    }

    public EmergencyNumber(String country, String general, String police, String fire, String ambulance) {
        this.country = country;
        this.general = general;
        this.police = police;
        this.fire = fire;
        this.ambulance = ambulance;
        this.source = "Wikidata (P2852)";
        this.sourceUrl = "https://www.wikidata.org/wiki/Property:P2852";
        this.retrievedAt = LocalDateTime.now();
    }

    /**
     * Numbers that reach a general emergency operator in the countries that use
     * them. Used only to decide what to lead with, never to add or alter data.
     */
    private static final java.util.Set<String> UNIVERSAL = java.util.Set.of(
            "112", "911", "999", "000", "111", "123");

    /**
     * The single number to lead with.
     *
     * Not simply the first "general" value: Japan's unqualified number is 118,
     * the Coast Guard, and leading with it would send someone in Tokyo to a
     * maritime line instead of 110 or 119. So a recognised universal number wins
     * first, then police as the reliable fallback, and only then whatever else
     * the source listed.
     */
    public String getPrimary() {
        if (general != null && !general.isBlank()) {
            for (String candidate : general.split("/")) {
                if (UNIVERSAL.contains(candidate.trim())) {
                    return candidate.trim();
                }
            }
        }
        if (police != null && !police.isBlank()) {
            return police;
        }
        return general;
    }

    public Long getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGeneral() {
        return general;
    }

    public void setGeneral(String general) {
        this.general = general;
    }

    public String getPolice() {
        return police;
    }

    public void setPolice(String police) {
        this.police = police;
    }

    public String getFire() {
        return fire;
    }

    public void setFire(String fire) {
        this.fire = fire;
    }

    public String getAmbulance() {
        return ambulance;
    }

    public void setAmbulance(String ambulance) {
        this.ambulance = ambulance;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }
}
