package com.zainab.roamSafe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zainab.roamSafe.model.EmergencyNumber;
import com.zainab.roamSafe.repository.EmergencyNumberRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Loads emergency numbers from the bundled dataset and answers lookups.
 *
 * The dataset was extracted from Wikidata's typed P2852 statements rather than
 * transcribed by hand, so no number in it passed through anyone's memory. It
 * ships as a resource instead of being fetched at runtime: these numbers change
 * very rarely, and a page that shows an emergency number must not depend on an
 * external service being reachable at the moment someone needs it.
 */
@Service
public class EmergencyNumberService {

    private static final String DATA = "data/emergency_numbers.json";

    private final EmergencyNumberRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmergencyNumberService(EmergencyNumberRepository repository) {
        this.repository = repository;
    }

    /** Numbers for a country, or empty when the country isn't in the dataset. */
    public Optional<EmergencyNumber> forCountry(String country) {
        if (country == null || country.isBlank() || "Unknown".equalsIgnoreCase(country)) {
            return Optional.empty();
        }
        return repository.findByCountryIgnoreCase(country.trim());
    }

    /** Loads the dataset into the database. Safe to re-run; upserts by country. */
    public int load() {
        try {
            JsonNode root = mapper.readTree(new ClassPathResource(DATA).getInputStream());
            int saved = 0;
            var fields = root.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String country = entry.getKey();
                JsonNode v = entry.getValue();

                EmergencyNumber row = repository.findByCountryIgnoreCase(country)
                        .orElseGet(EmergencyNumber::new);
                row.setCountry(country);
                row.setGeneral(text(v, "general"));
                row.setPolice(text(v, "police"));
                row.setFire(text(v, "fire"));
                row.setAmbulance(text(v, "ambulance"));
                row.setSource("Wikidata (P2852)");
                row.setSourceUrl("https://www.wikidata.org/wiki/Property:P2852");
                row.setRetrievedAt(LocalDateTime.now());
                repository.save(row);
                saved++;
            }
            System.out.println("[emergency] Loaded " + saved + " countries.");
            return saved;
        } catch (Exception e) {
            System.out.println("[emergency] Failed to load dataset: " + e.getMessage());
            return 0;
        }
    }

    /** Null stays null: an absent number must never become an empty string. */
    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }
}
