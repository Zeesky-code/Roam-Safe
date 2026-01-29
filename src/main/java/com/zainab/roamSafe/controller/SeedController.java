package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.dto.BulkScamReportRequest;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.model.SafetyZone;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.zainab.roamSafe.service.NewsIngestionService;

@RestController
@RequestMapping("/api/admin/seed")
public class SeedController {

        @Autowired
        private ScamReportRepository scamReportRepository;

        @Autowired
        private NewsIngestionService newsIngestionService;

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
                                                "Seine River Banks", 2),

                                createReport("Paris", "Friendship Bracelet Scam",
                                                "Men will try to tie a bracelet on your wrist and demand payment. They can be aggressive if you refuse.",
                                                "RED", "Keep hands in pockets near Sacré-Cœur steps. Do not stop.",
                                                "Sacré-Cœur Steps", 1),

                                createReport("Paris", "Metro Pickpockets",
                                                "Crowded Line 1 trains are prime targets. Thieves create bottlenecks at doors.",
                                                "YELLOW", "Wear your backpack on your front. Keep zippers locked.",
                                                "Metro Line 1", 2),

                                createReport("Paris", "Safe Night Walk",
                                                "The Marais district is generally lively and safe at night with many pedestrians.",
                                                "GREEN", "Great area for solo dining and walking.", "Le Marais", 5),

                                // --- ISTANBUL ---
                                createReport("Istanbul", "Shoe Shine Scam",
                                                "A cleaner drops their brush. If you pick it up, they offer a 'free' shine effectively and then demand exorbitant payment.",
                                                "YELLOW", "Do not pick up dropped brushes. Just keep walking.",
                                                "Galata Bridge", 3),

                                createReport("Istanbul", "Friendly 'Lira Exchange'",
                                                "A 'tourist' asks for help exchanging coins, but gives you worthless currency from another country.",
                                                "RED",
                                                "Never exchange money on the street. Use official exchange offices.",
                                                "Sultanahmet Square", 2),

                                createReport("Istanbul", "Safe Cafe Area",
                                                "Moda seaside is full of families and students. Very safe and relaxed vibe.",
                                                "GREEN", "Perfect for sunset walks.", "Kadikoy / Moda", 5),

                                // --- ROME ---
                                createReport("Rome", "Fake Gladiator Photos",
                                                "Men dressed as gladiators invite you for a photo and then aggressively demand 50 Euro.",
                                                "RED", "Do not make eye contact. A firm 'No' works best.", "Colosseum",
                                                2),

                                // --- BALI ---
                                createReport("Bali", "Money Changer Trick",
                                                "Calculators are rigged or bills are palmed (dropped behind the counter) during counting.",
                                                "RED",
                                                "Only use authorized changers with glass booths (e.g. BMC). Count your money last.",
                                                "Kuta / Seminyak", 2),

                                createReport("Bali", "Scooter Bag Snatch",
                                                "Thieves on bikes snatch purses from pedestrians or other riders.",
                                                "YELLOW",
                                                "Do not wear cross-body bags on the street side. Put bags under the scooter seat.",
                                                "Canggu Shortcuts", 2),

                                createReport("Bali", "Safe Coworking Hub",
                                                "Area is full of digital nomads and security guards. Very safe day and night.",
                                                "GREEN", "Community is very helpful.", "Pererenan", 5));

                scamReportRepository.saveAll(seedData);
                return ResponseEntity.ok("Database seeded successfully with " + seedData.size() + " reports.");
        }

        private ScamReport createReport(String city, String name, String description, String zoneStr, String prevention,
                        String neighborhood, int rating) {
                ScamReport report = new ScamReport();
                report.setCity(city);
                report.setName(name);
                report.setDescription(description);
                report.setNeighborhood(neighborhood);
                report.setPreventionTips(prevention);
                report.setSafetyZone(SafetyZone.valueOf(zoneStr)); // GREEN, YELLOW, RED
                report.setSafetyRating(rating);
                report.setScamType("SCAM"); // Default
                report.setSeverityScore(zoneStr.equals("RED") ? 8 : (zoneStr.equals("YELLOW") ? 5 : 2));
                report.setStatus(ScamReportStatus.APPROVED); // Auto-approve seed data
                report.setIsNightTimeIncident(false);
                report.setCreatedAt(LocalDateTime.now().minusDays((long) (Math.random() * 30))); // Random date in last
                                                                                                 // 30 days
                return report;
        }

        /**
         * Bulk import endpoint for LLM-generated scam data.
         * Accepts JSON array from scripts/generate_scam_data.py
         */
        @PostMapping("/bulk")
        public ResponseEntity<String> bulkSeed(@RequestBody List<BulkScamReportRequest> reports) {
                if (scamReportRepository.count() > 0) {
                        return ResponseEntity.badRequest().body(
                                        "Database is not empty. Skipping bulk seed. Please clear the database first.");
                }

                List<ScamReport> entities = new ArrayList<>();

                for (BulkScamReportRequest req : reports) {
                        ScamReport report = new ScamReport();
                        report.setCity(req.city());

                        // Handle scamType - default to "Safe Area" for GREEN zones
                        String scamType = req.scamType();
                        if (scamType == null || scamType.isBlank()) {
                                scamType = "Safe Area";
                        }
                        report.setScamType(scamType);

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
                        report.setCreatedAt(LocalDateTime.now().minusDays((long) (Math.random() * 60))); // Random date
                                                                                                         // in last 60
                                                                                                         // days

                        entities.add(report);
                }

                scamReportRepository.saveAll(entities);
                return ResponseEntity.ok("Bulk import successful: " + entities.size() + " reports imported.");
        }

        public ResponseEntity<String> clearDatabase() {
                long count = scamReportRepository.count();
                scamReportRepository.deleteAll();
                return ResponseEntity.ok("Database cleared. Removed " + count + " entries.");
        }

        @PostMapping("/trigger-pipeline")
        public ResponseEntity<String> triggerPipeline() {
                newsIngestionService.ingestFreshScamData();
                return ResponseEntity.ok("Intelligence pipeline triggered. Check logs for new alerts.");
        }
}
