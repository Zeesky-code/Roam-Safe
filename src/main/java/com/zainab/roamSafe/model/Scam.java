package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
    private Category category;
    
    @ManyToMany
    @JoinTable(
        name = "scam_cities",
        joinColumns = @JoinColumn(name = "scam_id"),
        inverseJoinColumns = @JoinColumn(name = "city_id")
    )
    private List<City> cities;
    
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
}