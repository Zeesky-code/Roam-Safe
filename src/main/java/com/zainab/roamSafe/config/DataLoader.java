package com.zainab.roamSafe.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zainab.roamSafe.model.Scam;
import com.zainab.roamSafe.model.Category;
import com.zainab.roamSafe.model.City;
import com.zainab.roamSafe.model.SafetyZone;
import com.zainab.roamSafe.repository.ScamRepository;
import com.zainab.roamSafe.repository.CategoryRepository;
import com.zainab.roamSafe.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.text.Normalizer;
import java.util.regex.Pattern;


@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ScamRepository scamRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private CityRepository cityRepository;
    
    // Pattern to remove accents and special characters
    private static final Pattern ACCENT_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    
    /**
     * Normalize text by removing accents and converting to lowercase
     */
    private String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        // Remove accents and convert to lowercase
        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        normalized = ACCENT_PATTERN.matcher(normalized).replaceAll("");
        return normalized.toLowerCase();
    }

    @Override
    public void run(String... args) throws Exception {
        // Only load if database is empty
        if (scamRepository.count() == 0) {
            loadDataFromJson();
        }
    }

    private void loadDataFromJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("data/scams.json");
            JsonNode rootNode = mapper.readTree(resource.getInputStream());
            
            // Load categories first
            Map<String, Category> categoryMap = loadCategories(rootNode);
            
            // Load cities
            Map<String, City> cityMap = loadCities(rootNode);
            
            // Load scams with relationships
            loadScams(rootNode, categoryMap, cityMap);
            
            System.out.println("Data loading completed successfully!");
            
        } catch (IOException e) {
            System.err.println("Error loading data from JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Map<String, Category> loadCategories(JsonNode rootNode) {
        Map<String, Category> categoryMap = new HashMap<>();
        JsonNode categoriesNode = rootNode.get("categories");
        
        if (categoriesNode != null && categoriesNode.isArray()) {
            for (JsonNode categoryNode : categoriesNode) {
                String name = categoryNode.get("name").asText();
                String description = categoryNode.get("description").asText();
                
                // Check if category already exists
                Category existingCategory = categoryRepository.findByName(name);
                if (existingCategory != null) {
                    categoryMap.put(name, existingCategory);
                } else {
                    Category category = new Category(name, description);
                    category = categoryRepository.save(category);
                    categoryMap.put(name, category);
                }
            }
        }
        
        System.out.println("Loaded " + categoryMap.size() + " categories");
        return categoryMap;
    }
    
    private Map<String, City> loadCities(JsonNode rootNode) {
        Map<String, City> cityMap = new HashMap<>();
        JsonNode citiesNode = rootNode.get("cities");
        
        if (citiesNode != null && citiesNode.isArray()) {
            for (JsonNode cityNode : citiesNode) {
                String name = cityNode.get("city").asText();
                String country = cityNode.get("country").asText();
                // Normalize city and country names
                String normName = normalizeText(name);
                String normCountry = normalizeText(country);
                
                // Check if city already exists (using normalized name)
                City existingCity = cityRepository.findByNameAndCountry(name.trim(), country.trim());
                if (existingCity != null) {
                    cityMap.put(normName, existingCity);
                } else {
                    City city = new City(name.trim(), country.trim()); // Store original names
                    city = cityRepository.save(city);
                    cityMap.put(normName, city);
                }
            }
        }
        
        System.out.println("Loaded " + cityMap.size() + " cities");
        return cityMap;
    }
    
    private void loadScams(JsonNode rootNode, Map<String, Category> categoryMap, Map<String, City> cityMap) {
        JsonNode scamsNode = rootNode.get("scams");
        
        if (scamsNode != null && scamsNode.isArray()) {
            for (JsonNode scamNode : scamsNode) {
                String name = scamNode.get("name").asText();
                String description = scamNode.get("description").asText();
                String prevention = scamNode.get("prevention").asText();
                String categoryName = scamNode.get("category").asText();
                
                // Create scam
                Scam scam = new Scam(name, description, prevention);
                
                // Set category
                Category category = categoryMap.get(categoryName);
                if (category != null) {
                    scam.setCategory(category);
                }
                
                // Set cities
                List<City> cities = new ArrayList<>();
                JsonNode citiesNode = scamNode.get("cities");
                if (citiesNode != null && citiesNode.isArray()) {
                    for (JsonNode cityNode : citiesNode) {
                        String cityName = cityNode.asText();
                        if (cityName.equals("All destinations") || cityName.equals("All major cities")) {
                            // Add all cities for generic scams
                            cities.addAll(cityMap.values());
                        } else {
                            // Normalize city name for lookup
                            String normCityName = normalizeText(cityName);
                            City city = cityMap.get(normCityName);
                            if (city != null) {
                                cities.add(city);
                            }
                        }
                    }
                }
                scam.setCities(cities);
                
                // Set safety zone fields
                if (scamNode.has("safetyZone")) {
                    String safetyZoneStr = scamNode.get("safetyZone").asText();
                    try {
                        SafetyZone safetyZone = SafetyZone.valueOf(safetyZoneStr);
                        scam.setSafetyZone(safetyZone);
                    } catch (IllegalArgumentException e) {
                        scam.setSafetyZone(SafetyZone.UNKNOWN);
                    }
                }
                
                if (scamNode.has("incidentType")) {
                    scam.setIncidentType(scamNode.get("incidentType").asText());
                }
                
                if (scamNode.has("safetyRating")) {
                    scam.setSafetyRating(scamNode.get("safetyRating").asInt());
                }
                
                if (scamNode.has("isNightTimeIncident")) {
                    scam.setIsNightTimeIncident(scamNode.get("isNightTimeIncident").asBoolean());
                }
                
                if (scamNode.has("additionalDetails")) {
                    scam.setAdditionalDetails(scamNode.get("additionalDetails").asText());
                }
                
                scamRepository.save(scam);
            }
        }
        
        System.out.println("Loaded scams with relationships and safety zone data");
    }
} 