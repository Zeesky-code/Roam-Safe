package com.zainab.roamSafe.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScamTest {

    private Scam scam;
    private Category category;
    private City istanbul;
    private City paris;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Fraud");

        istanbul = new City("Istanbul", "Turkey");
        paris = new City("Paris", "France");

        scam = new Scam("ATM Scam", "Fake ATM machines", "Use bank ATMs only");
        scam.setCategory(category);
        scam.setCities(Arrays.asList(istanbul, paris));
    }

    @Test
    void testScamConstructor() {
        Scam newScam = new Scam("Test Scam", "Test Description", "Test Prevention");
        
        assertThat(newScam.getName()).isEqualTo("Test Scam");
        assertThat(newScam.getDescription()).isEqualTo("Test Description");
        assertThat(newScam.getPrevention()).isEqualTo("Test Prevention");
        assertThat(newScam.getSafetyZone()).isEqualTo(SafetyZone.UNKNOWN); // Default value
        assertThat(newScam.getIsNightTimeIncident()).isFalse(); // Default value
    }

    @Test
    void testGetCategoryName() {
        assertThat(scam.getCategoryName()).isEqualTo("Fraud");
        
        // Test with null category
        scam.setCategory(null);
        assertThat(scam.getCategoryName()).isNull();
    }

    @Test
    void testGetCityNames() {
        List<String> cityNames = scam.getCityNames();
        
        assertThat(cityNames).hasSize(2);
        assertThat(cityNames).containsExactlyInAnyOrder("Istanbul", "Paris");
        
        // Test with null cities
        scam.setCities(null);
        assertThat(scam.getCityNames()).isNull();
    }

    @Test
    void testSafetyZoneGettersAndSetters() {
        scam.setSafetyZone(SafetyZone.RED);
        assertThat(scam.getSafetyZone()).isEqualTo(SafetyZone.RED);
        
        scam.setSafetyZone(SafetyZone.GREEN);
        assertThat(scam.getSafetyZone()).isEqualTo(SafetyZone.GREEN);
    }

    @Test
    void testNeighborhoodGettersAndSetters() {
        scam.setNeighborhood("Sultanahmet");
        assertThat(scam.getNeighborhood()).isEqualTo("Sultanahmet");
        
        scam.setNeighborhood(null);
        assertThat(scam.getNeighborhood()).isNull();
    }

    @Test
    void testIncidentTypeGettersAndSetters() {
        scam.setIncidentType("SCAM");
        assertThat(scam.getIncidentType()).isEqualTo("SCAM");
        
        scam.setIncidentType("THEFT");
        assertThat(scam.getIncidentType()).isEqualTo("THEFT");
    }

    @Test
    void testSafetyRatingGettersAndSetters() {
        scam.setSafetyRating(3);
        assertThat(scam.getSafetyRating()).isEqualTo(3);
        
        scam.setSafetyRating(5);
        assertThat(scam.getSafetyRating()).isEqualTo(5);
        
        scam.setSafetyRating(null);
        assertThat(scam.getSafetyRating()).isNull();
    }

    @Test
    void testNightTimeIncidentGettersAndSetters() {
        scam.setIsNightTimeIncident(true);
        assertThat(scam.getIsNightTimeIncident()).isTrue();
        
        scam.setIsNightTimeIncident(false);
        assertThat(scam.getIsNightTimeIncident()).isFalse();
    }

    @Test
    void testAdditionalDetailsGettersAndSetters() {
        String details = "This happened near the main tourist area";
        scam.setAdditionalDetails(details);
        assertThat(scam.getAdditionalDetails()).isEqualTo(details);
        
        scam.setAdditionalDetails(null);
        assertThat(scam.getAdditionalDetails()).isNull();
    }

    @Test
    void testBasicGettersAndSetters() {
        scam.setId(1L);
        assertThat(scam.getId()).isEqualTo(1L);
        
        scam.setName("Updated Name");
        assertThat(scam.getName()).isEqualTo("Updated Name");
        
        scam.setDescription("Updated Description");
        assertThat(scam.getDescription()).isEqualTo("Updated Description");
        
        scam.setPrevention("Updated Prevention");
        assertThat(scam.getPrevention()).isEqualTo("Updated Prevention");
    }

    @Test
    void testCategoryRelationship() {
        Category newCategory = new Category();
        newCategory.setName("Theft");
        
        scam.setCategory(newCategory);
        assertThat(scam.getCategory()).isEqualTo(newCategory);
        assertThat(scam.getCategoryName()).isEqualTo("Theft");
    }

    @Test
    void testCitiesRelationship() {
        City london = new City("London", "UK");
        List<City> newCities = Arrays.asList(london);
        
        scam.setCities(newCities);
        assertThat(scam.getCities()).hasSize(1);
        assertThat(scam.getCities().get(0)).isEqualTo(london);
        assertThat(scam.getCityNames()).containsExactly("London");
    }
}