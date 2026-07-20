package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.dto.BulkScamReportRequest;
import com.zainab.roamSafe.model.City;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.model.SafetyZone;
import com.zainab.roamSafe.repository.CityRepository;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.zainab.roamSafe.service.NewsIngestionService;
import com.zainab.roamSafe.service.AdvisoryIngestionService;

@RestController
@RequestMapping("/api/admin/seed")
public class SeedController {

        @Autowired
        private ScamReportRepository scamReportRepository;

        @Autowired
        private CityRepository cityRepository;

        @Autowired
        private NewsIngestionService newsIngestionService;

        @Autowired
        private AdvisoryIngestionService advisoryIngestionService;

        @Autowired
        private com.zainab.roamSafe.service.PoliceDataService policeDataService;

        @Autowired
        private com.zainab.roamSafe.service.CityCountryResolver cityCountryResolver;

        /**
         * Country for a destination, or "Unknown" when it genuinely isn't known.
         * City.country is @NotBlank, so unresolved has to be a marker rather than
         * null - but it stays an honest marker instead of a guess.
         */
        private String countryFor(String city) {
                return cityCountryResolver.countryFor(city).orElse("Unknown");
        }

        /**
         * Backfill countries on existing cities. Every row was created as
         * "Unknown" because the importers never carried the value, which disabled
         * country filtering across the API. Safe to re-run.
         */
        @org.springframework.web.bind.annotation.GetMapping("/countries")
        public ResponseEntity<String> backfillCountries() {
                List<City> all = cityRepository.findAll();
                int updated = 0;
                List<String> unresolved = new ArrayList<>();
                for (City c : all) {
                        var country = cityCountryResolver.countryFor(c.getName());
                        if (country.isEmpty()) {
                                unresolved.add(c.getName());
                                continue;
                        }
                        if (!country.get().equals(c.getCountry())) {
                                c.setCountry(country.get());
                                updated++;
                        }
                }
                cityRepository.saveAll(all);
                return ResponseEntity.ok("Countries backfilled: " + updated + " of " + all.size()
                                + " updated" + (unresolved.isEmpty() ? "."
                                                : ", " + unresolved.size() + " unresolved: " + unresolved));
        }

        /**
         * One-shot trigger to pull recorded crime from data.police.uk.
         *
         * Runs inline and takes a couple of minutes (16 cities x 3 months, paced
         * so the free source isn't hammered), so call it deliberately rather than
         * from anything user-facing.
         */
        @org.springframework.web.bind.annotation.GetMapping("/police")
        public ResponseEntity<String> refreshPoliceData() {
                var result = policeDataService.refreshAll();
                return ResponseEntity.ok("Police data refresh complete: "
                                + result.cityMonths() + " city-months, "
                                + result.crimes() + " crimes recorded"
                                + (result.unpublished().isEmpty() ? ""
                                                : ", no force data for: " + result.unpublished())
                                + (result.failed().isEmpty() ? "." : ", failed: " + result.failed()));
        }

        @Autowired
        private com.zainab.roamSafe.service.ScoreSnapshotService scoreSnapshotService;

        @Autowired
        private com.zainab.roamSafe.service.EmergencyNumberService emergencyNumberService;

        @Autowired
        private com.zainab.roamSafe.service.GdeltIngestionService gdeltIngestionService;

        /**
         * Pull current disruption reporting. Paced at one request every six
         * seconds as GDELT asks, so a full run takes several minutes.
         */
        @org.springframework.web.bind.annotation.GetMapping("/incidents")
        public ResponseEntity<String> refreshIncidents(
                        @org.springframework.web.bind.annotation.RequestParam(required = false) String city) {
                if (city != null && !city.isBlank()) {
                        int n = gdeltIngestionService.ingestCity(city);
                        return ResponseEntity.ok("Stored " + n + " new incidents for " + city + ".");
                }
                var r = gdeltIngestionService.refreshAll();
                return ResponseEntity.ok("Polled " + r.citiesPolled() + " cities, stored "
                                + r.stored() + " incidents, pruned " + r.pruned() + ".");
        }

        /** Load emergency numbers from the bundled dataset. Safe to re-run. */
        @org.springframework.web.bind.annotation.GetMapping("/emergency")
        public ResponseEntity<String> loadEmergencyNumbers() {
                return ResponseEntity.ok("Loaded emergency numbers for "
                                + emergencyNumberService.load() + " countries.");
        }

        /**
         * Record today's score snapshot for every city. Safe to re-run: only one
         * snapshot per city per day is kept, so this won't stack duplicate points
         * onto a trend.
         */
        @org.springframework.web.bind.annotation.GetMapping("/snapshot")
        public ResponseEntity<String> snapshotScores() {
                int written = scoreSnapshotService.snapshotAll();
                return ResponseEntity.ok("Recorded " + written + " score snapshots"
                                + (written == 0 ? " (already snapshotted today)." : "."));
        }

        /** One-shot trigger to (re)populate government advisories (UK + US). */
        @org.springframework.web.bind.annotation.GetMapping("/advisories")
        public ResponseEntity<String> refreshAdvisories() {
                advisoryIngestionService.refreshAll();
                return ResponseEntity.ok("Advisory refresh triggered.");
        }

        @org.springframework.web.bind.annotation.GetMapping
        public ResponseEntity<String> seedDatabase() {
                if (scamReportRepository.count() > 0) {
                        return ResponseEntity.badRequest().body("Database is not empty. Skipping seed.");
                }

                List<ScamReport> seedData = Arrays.asList(
                                // --- PARIS ---
                                createReport("Paris", "The 'Gold Ring' Scam",
                                                "A person picks up a gold ring from the ground and asks if you dropped it. They then demand money for 'finding' it.",
                                                "RED",
                                                "Avoid engaging with anyone picking up objects in front of you. Walk away immediately.",
                                                "Seine River Banks", 2, "Financial"),

                                createReport("Paris", "Friendship Bracelet Scam",
                                                "Men will try to tie a bracelet on your wrist and demand payment. They can be aggressive if you refuse.",
                                                "RED", "Keep hands in pockets near Sacré-Cœur steps. Do not stop.",
                                                "Sacré-Cœur Steps", 1, "Harassment"),

                                createReport("Paris", "Metro Pickpockets",
                                                "Crowded Line 1 trains are prime targets. Thieves create bottlenecks at doors.",
                                                "YELLOW", "Wear your backpack on your front. Keep zippers locked.",
                                                "Metro Line 1", 2, "Theft"),

                                createReport("Paris", "Safe Night Walk",
                                                "The Marais district is generally lively and safe at night with many pedestrians.",
                                                "GREEN", "Great area for solo dining and walking.", "Le Marais", 5,
                                                "Safe Area"),

                                // --- ISTANBUL ---
                                createReport("Istanbul", "Shoe Shine Scam",
                                                "A cleaner drops their brush. If you pick it up, they offer a 'free' shine effectively and then demand exorbitant payment.",
                                                "YELLOW", "Do not pick up dropped brushes. Just keep walking.",
                                                "Galata Bridge", 3, "Financial"),

                                createReport("Istanbul", "Friendly 'Lira Exchange'",
                                                "A 'tourist' asks for help exchanging coins, but gives you worthless currency from another country.",
                                                "RED",
                                                "Never exchange money on the street. Use official exchange offices.",
                                                "Sultanahmet Square", 2, "Financial"),

                                createReport("Istanbul", "Safe Cafe Area",
                                                "Moda seaside is full of families and students. Very safe and relaxed vibe.",
                                                "GREEN", "Perfect for sunset walks.", "Kadikoy / Moda", 5, "Safe Area"),

                                // --- ROME ---
                                createReport("Rome", "Fake Gladiator Photos",
                                                "Men dressed as gladiators invite you for a photo and then aggressively demand 50 Euro.",
                                                "RED", "Do not make eye contact. A firm 'No' works best.", "Colosseum",
                                                2, "Tourism"),

                                // --- BALI ---
                                createReport("Bali", "Money Changer Trick",
                                                "Calculators are rigged or bills are palmed (dropped behind the counter) during counting.",
                                                "RED",
                                                "Only use authorized changers with glass booths (e.g. BMC). Count your money last.",
                                                "Kuta / Seminyak", 2, "Financial"),

                                createReport("Bali", "Scooter Bag Snatch",
                                                "Thieves on bikes snatch purses from pedestrians or other riders.",
                                                "YELLOW",
                                                "Do not wear cross-body bags on the street side. Put bags under the scooter seat.",
                                                "Canggu Shortcuts", 2, "Theft"),

                                createReport("Bali", "Safe Coworking Hub",
                                                "Area is full of digital nomads and security guards. Very safe day and night.",
                                                "GREEN", "Community is very helpful.", "Pererenan", 5, "Safe Area"));

                scamReportRepository.saveAll(seedData);
                return ResponseEntity.ok("Database seeded successfully with " + seedData.size() + " reports.");
        }

        private ScamReport createReport(String city, String name, String description, String zoneStr, String prevention,
                        String neighborhood, int rating, String category) {
                // Ensure city exists
                if (cityRepository.findFirstByName(city) == null) {
                        cityRepository.save(new City(city, countryFor(city)));
                }

                ScamReport report = new ScamReport();
                report.setCity(city);
                report.setName(name);
                report.setDescription(description);
                report.setNeighborhood(neighborhood);
                report.setPreventionTips(prevention);
                report.setSafetyZone(SafetyZone.valueOf(zoneStr)); // GREEN, YELLOW, RED
                report.setSafetyRating(rating);
                report.setScamType("SCAM"); // Default
                report.setCategory(category);
                report.setSeverityScore(zoneStr.equals("RED") ? 8 : (zoneStr.equals("YELLOW") ? 5 : 2));
                report.setStatus(ScamReportStatus.APPROVED); // Auto-approve seed data
                report.setIsNightTimeIncident(false);
                // reportedAt is deliberately left null: seed sources carry no incident
                // date, and inventing one (this previously assigned a random day in the
                // last 30) made imported guidance look like fresh reporting and fed
                // random noise into the recency-weighted safety scores.
                return report;
        }

        /**
         * Bulk import endpoint for LLM-generated scam data.
         * Accepts JSON array from scripts/generate_scam_data.py
         */
        @PostMapping("/bulk")
        public ResponseEntity<String> bulkSeed(@RequestBody List<BulkScamReportRequest> reports) {
                List<ScamReport> entities = new ArrayList<>();

                // Create any missing cities in one pass, instead of a query per report
                // (1400+ sequential lookups against a remote DB would time the request out).
                java.util.Set<String> existingCities = new java.util.HashSet<>();
                cityRepository.findAll().forEach(c -> existingCities.add(c.getName()));
                java.util.Set<String> newCityNames = new java.util.LinkedHashSet<>();
                for (BulkScamReportRequest req : reports) {
                        if (req.city() != null && !existingCities.contains(req.city())) {
                                newCityNames.add(req.city());
                        }
                }
                List<City> newCities = new ArrayList<>();
                for (String name : newCityNames) {
                        newCities.add(new City(name, "Unknown"));
                }
                cityRepository.saveAll(newCities);

                for (BulkScamReportRequest req : reports) {
                        ScamReport report = new ScamReport();
                        report.setCity(req.city());

                        // Handle scamType - default to "Safe Area" for GREEN zones
                        String scamType = req.scamType();
                        if (scamType == null || scamType.isBlank()) {
                                scamType = "Safe Area";
                        }
                        report.setScamType(scamType);

                        // Preserve the source category (Theft, Financial, ...); default only if absent.
                        report.setCategory(
                                        req.category() != null && !req.category().isBlank() ? req.category() : "General");

                        report.setName(req.name() != null ? req.name() : scamType + " in " + req.city());
                        report.setDescription(req.description());
                        report.setSeverityScore(req.severityScore() != null ? req.severityScore() : 5);
                        report.setPreventionTips(req.preventionTips());
                        report.setNeighborhood(req.neighborhood());

                        // Parse safetyZone string to enum, default to YELLOW
                        try {
                                report.setSafetyZone(SafetyZone.valueOf(req.safetyZone()));
                        } catch (Exception e) {
                                report.setSafetyZone(SafetyZone.YELLOW);
                        }

                        report.setSafetyRating(req.safetyRating() != null ? req.safetyRating() : 3);
                        report.setIsNightTimeIncident(
                                        req.isNightTimeIncident() != null ? req.isNightTimeIncident() : false);
                        report.setStatus(ScamReportStatus.APPROVED); // Always approve bulk imports
                        // No invented reportedAt — see the note in the seed path above.

                        entities.add(report);
                }

                scamReportRepository.saveAll(entities);
                return ResponseEntity.ok("Bulk import successful: " + entities.size() + " reports imported.");
        }

        @PostMapping("/clear")
        public ResponseEntity<String> clearDatabase() {
                long count = scamReportRepository.count();
                scamReportRepository.deleteAll();
                cityRepository.deleteAll(); // Also clear cities to keep it clean
                return ResponseEntity.ok("Database cleared. Removed " + count + " entries.");
        }

        @PostMapping("/trigger-pipeline")
        public ResponseEntity<String> triggerPipeline() {
                newsIngestionService.ingestFreshScamData();
                return ResponseEntity.ok("Intelligence pipeline triggered. Check logs for new alerts.");
        }
}
