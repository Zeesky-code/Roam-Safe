package com.zainab.roamSafe.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zainab.roamSafe.model.Scam;
import com.zainab.roamSafe.model.Category;
import com.zainab.roamSafe.model.City;
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

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ScamRepository scamRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private CityRepository cityRepository;

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
                
                Category category = new Category(name, description);
                category = categoryRepository.save(category);
                categoryMap.put(name, category);
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
                
                City city = new City(name, country);
                city = cityRepository.save(city);
                cityMap.put(name, city);
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
                            City city = cityMap.get(cityName);
                            if (city != null) {
                                cities.add(city);
                            }
                        }
                    }
                }
                scam.setCities(cities);
                
                scamRepository.save(scam);
            }
        }
        
        System.out.println("Loaded scams with relationships");
    }
} 