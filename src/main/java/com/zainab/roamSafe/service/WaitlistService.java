package com.zainab.roamSafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Service
public class WaitlistService {

    @Value("${google.sheets.webhook.url}")
    private String googleSheetsWebhookUrl;

    private final RestTemplate restTemplate;
    private final Set<String> emailCache = ConcurrentHashMap.newKeySet();

    public WaitlistService() {
        this.restTemplate = new RestTemplate();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        // Load existing emails from Google Sheets on startup
        loadExistingEmails();
    }

    public boolean addToWaitlist(String email) {
        email = email.toLowerCase().trim();

        // Check if email is already in cache (in-memory check)
        if (emailCache.contains(email)) {
            System.out.println("Email already in cache: " + email);
            return false;
        }

        try {
            boolean sheetsSuccess = sendToGoogleSheets(email);

            if (sheetsSuccess) {
                emailCache.add(email);
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("Error adding to waitlist: " + e.getMessage());
            return false;
        }
    }

    private boolean sendToGoogleSheets(String email) {
        try {
            // Create the payload for Google Sheets webhook
            Map<String, Object> payload = new HashMap<>();
            payload.put("email", email);
            payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            payload.put("source", "website");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            // Create request entity
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Log the request for debugging
            System.out.println("Sending to Google Sheets: " + payload);
            System.out.println("URL: " + googleSheetsWebhookUrl);

            // Configure RestTemplate to follow redirects
            RestTemplate restTemplate = new RestTemplate();

            // Use exchange instead of postForEntity for more control
            ResponseEntity<String> response = restTemplate.exchange(
                    googleSheetsWebhookUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());

            return response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection();

        } catch (RestClientException e) {
            System.err.println("Failed to send to Google Sheets: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public int getWaitlistCount() {
        return emailCache.size();
    }

    // Method to manually clear cache (useful for testing)
    public void clearCache() {
        emailCache.clear();
        System.out.println("Email cache cleared");
    }

    private void loadExistingEmails() {
        try {
            // Create a simple GET request to load emails
            String loadEmailsUrl = googleSheetsWebhookUrl + "?action=loadEmails";

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<String> request = new HttpEntity<>(headers);

            System.out.println("Loading existing emails from Google Sheets...");

            ResponseEntity<String> response = restTemplate.exchange(
                    loadEmailsUrl,
                    HttpMethod.GET,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection()) {
                // Parse the response and add emails to cache
                String responseBody = response.getBody();
                if (responseBody != null && responseBody.contains("emails")) {
                    // Simple JSON parsing - extract emails array
                    try {
                        // Extract emails from JSON response
                        int emailsStart = responseBody.indexOf("\"emails\":[") + 10;
                        int emailsEnd = responseBody.indexOf("]", emailsStart);
                        if (emailsStart > 9 && emailsEnd > emailsStart) {
                            String emailsJson = responseBody.substring(emailsStart, emailsEnd + 1);
                            // Remove quotes and split by comma
                            String[] emails = emailsJson.replaceAll("\"", "").split(",");
                            for (String email : emails) {
                                email = email.trim().toLowerCase();
                                if (email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                                    emailCache.add(email);
                                }
                            }
                        }
                    } catch (Exception parseError) {
                        System.err.println("Error parsing emails JSON: " + parseError.getMessage());
                    }
                    System.out.println("Loaded " + emailCache.size() + " existing emails into cache");
                } else {
                    System.out.println("No existing emails found or invalid response format");
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to load existing emails: " + e.getMessage());
            System.out.println("Continuing without loading existing emails...");
        }
    }
}