package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.model.SafetyZone;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class NewsIngestionService {

    @Autowired
    private ScamReportRepository scamReportRepository;

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // In a real production environment, we would call an actual News API
    // For this demo, we will simulate "fresh" findings for target cities
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void ingestFreshScamData() {
        System.out.println("Beginning hourly threat intelligence scan...");

        // List of cities to monitor
        String[] cities = { "Paris", "London", "Rome", "Barcelona" };

        for (String city : cities) {
            System.out.println("Scanning sources for: " + city);
            fetchAndProcessCity(city);
        }
    }

    private void fetchAndProcessCity(String city) {
        // Mocking the ingestion of "breaking news"
        // In the real implementation, this connects to GNews or Reddit API

        if (city.equals("Paris")) {
            // Simulate a finding from 20 mins ago
            createAlert(city, "Fake Petition Scam",
                    "Groups of teenagers pretending to represent a deaf-mute charity are swarming tourists near the Louvre today.",
                    "Louvre & Tuileries Garden", SafetyZone.RED, 9, "Aggressive. Do not sign anything. Keep walking.");
        } else if (city.equals("Rome")) {
            createAlert(city, "Bracelet Scam Update",
                    "New group of scammers spotted at Spanish Steps attempting the bracelet forced-sale trick.",
                    "Spanish Steps", SafetyZone.YELLOW, 6, "Keep hands in pockets. Do not engage.");
        }
    }

    private void createAlert(String city, String title, String description, String location, SafetyZone zone,
            int severity, String prevention) {
        // Check if similar report exists recently to avoid duplicates
        boolean exists = scamReportRepository.findAll().stream()
                .anyMatch(r -> r.getCity().equalsIgnoreCase(city) && r.getName().equalsIgnoreCase(title)
                        && r.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)));

        if (!exists) {
            ScamReport report = new ScamReport();
            report.setCity(city);
            report.setName(title);
            report.setDescription(description);
            report.setNeighborhood(location);
            report.setScamType("News Alert");
            report.setCategory("News");
            report.setSafetyZone(zone);
            report.setSeverityScore(severity);
            report.setPreventionTips(prevention);
            report.setSafetyRating(2); // Low safety due to active scam
            report.setIsNightTimeIncident(false);
            report.setStatus(ScamReportStatus.APPROVED);
            report.setCreatedAt(LocalDateTime.now());

            scamReportRepository.save(report);
            System.out.println("[NEW ALERT] Saved new threat intel for " + city + ": " + title);
        }
    }
}
