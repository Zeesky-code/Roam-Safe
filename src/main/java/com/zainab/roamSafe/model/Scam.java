package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "scams")
public class Scam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String prevention;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private Category category;
    
    @ManyToMany
    @JoinTable(
        name = "scam_cities",
        joinColumns = @JoinColumn(name = "scam_id"),
        inverseJoinColumns = @JoinColumn(name = "city_id")
    )
    @JsonIgnore
    private List<City> cities;
    
    // Safety Zone Fields
    @Enumerated(EnumType.STRING)
    private SafetyZone safetyZone = SafetyZone.UNKNOWN;
    
    private String neighborhood;
    
    @Column(columnDefinition = "TEXT")
    private String incidentType; // SCAM, HARASSMENT, THEFT, POSITIVE
    
    private Integer safetyRating; // 1-5 scale
    
    private Boolean isNightTimeIncident = false;
    
    @Column(columnDefinition = "TEXT")
    private String additionalDetails;
    
    public Scam() {}
    
    public Scam(String name, String description, String prevention) {
        this.name = name;
        this.description = description;
        this.prevention = prevention;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPrevention() { return prevention; }
    public void setPrevention(String prevention) { this.prevention = prevention; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public List<City> getCities() { return cities; }
    public void setCities(List<City> cities) { this.cities = cities; }
    
    // Custom getters for JSON serialization
    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }
    
    public List<String> getCityNames() {
        if (cities == null) return null;
        return cities.stream()
                    .map(City::getName)
                    .toList();
    }
    
    // Safety Zone Getters and Setters
    public SafetyZone getSafetyZone() { return safetyZone; }
    public void setSafetyZone(SafetyZone safetyZone) { this.safetyZone = safetyZone; }
    
    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }
    
    public Integer getSafetyRating() { return safetyRating; }
    public void setSafetyRating(Integer safetyRating) { this.safetyRating = safetyRating; }
    
    public Boolean getIsNightTimeIncident() { return isNightTimeIncident; }
    public void setIsNightTimeIncident(Boolean isNightTimeIncident) { this.isNightTimeIncident = isNightTimeIncident; }
    
    public String getAdditionalDetails() { return additionalDetails; }
    public void setAdditionalDetails(String additionalDetails) { this.additionalDetails = additionalDetails; }
}