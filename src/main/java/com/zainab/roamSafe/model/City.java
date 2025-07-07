package com.zainab.roamSafe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Entity
@Table(name = "cities")
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String name;
    
    @NotBlank
    private String country;
    
    @ManyToMany(mappedBy = "cities")
    private List<Scam> scams;
    
    public City() {}
    
    public City(String name, String country) {
        this.name = name;
        this.country = country;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public List<Scam> getScams() { return scams; }
    public void setScams(List<Scam> scams) { this.scams = scams; }
}
