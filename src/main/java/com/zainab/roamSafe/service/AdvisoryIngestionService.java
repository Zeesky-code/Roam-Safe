package com.zainab.roamSafe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zainab.roamSafe.model.Advisory;
import com.zainab.roamSafe.repository.AdvisoryRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ingests official government travel advisories from two sources:
 * the UK FCDO (GOV.UK content API) and the US State Department (data API).
 * Both are public and unauthenticated, and both link back to the original
 * advisory. This is real, sourced data — the opposite of the fabricated
 * NewsIngestionService it replaces. We store only what the sources say.
 */
@Service
public class AdvisoryIngestionService {

    private static final String UK_API = "https://www.gov.uk/api/content/foreign-travel-advice/";
    private static final String UK_SOURCE = "UK FCDO";
    private static final String US_API = "https://cadataapi.state.gov/api/TravelAdvisories";
    private static final String US_SOURCE = "US State Dept";

    // "Country Name - Level 4: Do Not Travel" (dash may be hyphen/en/em dash).
    private static final Pattern US_TITLE = Pattern.compile("^(.*?)\\s*[-\\u2013\\u2014]\\s*Level\\s*([1-4])",
            Pattern.CASE_INSENSITIVE);

    private final AdvisoryRepository advisoryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public AdvisoryIngestionService(AdvisoryRepository advisoryRepository) {
        this.advisoryRepository = advisoryRepository;
    }

    /**
     * Refresh daily, with the first run held back 5 minutes.
     *
     * A plain fixedRate fires as soon as the scheduler starts, which put ~44
     * outbound HTTP calls and their DB writes in direct competition with
     * application startup. On a small container that pushed time-to-listening
     * past the host's health-check window and the deploy was marked failed even
     * though the app was fine. The delay keeps boot clear.
     *
     * It also fired twice under DevTools' restart classloader and raced, hence
     * the synchronized guard. Initial population is done once via the admin
     * trigger (GET /api/admin/seed/advisories).
     */
    @Scheduled(fixedRate = 86_400_000, initialDelay = 300_000) // daily, first run 5 min after boot
    public synchronized void refreshAll() {
        System.out.println("[advisory] Refreshing government travel advisories...");
        int uk = refreshUk();
        // US State Dept's data endpoint is fronted by Cloudflare and intermittently
        // serves a bot challenge instead of JSON. Treat it as strictly best-effort
        // so a US failure never blocks the reliable UK FCDO source.
        int us = 0;
        try {
            us = refreshUs();
        } catch (Exception e) {
            System.out.println("[advisory] US source unavailable (" + e.getClass().getSimpleName()
                    + "); UK data unaffected.");
        }
        System.out.println("[advisory] Updated " + uk + " UK + " + us + " US advisories.");
    }

    // --- UK FCDO ---

    private int refreshUk() {
        int ok = 0;
        for (CountryLookup.Country c : CountryLookup.advisoryCountries()) {
            try {
                if (fetchUk(c)) {
                    ok++;
                }
                Thread.sleep(120);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ok;
            } catch (Exception e) {
                System.out.println("[advisory] UK " + c.name() + " failed: " + e.getMessage());
            }
        }
        return ok;
    }

    private boolean fetchUk(CountryLookup.Country country) {
        JsonNode root = get(UK_API + country.govUkSlug());
        if (root == null) {
            return false;
        }
        JsonNode alerts = root.path("details").path("alert_status");
        String basePath = root.path("base_path").asText("");
        upsert(country.name(), UK_SOURCE,
                ukLevel(alerts), ukSeverity(alerts),
                root.path("description").asText(""),
                basePath.isEmpty() ? "https://www.gov.uk/foreign-travel-advice" : "https://www.gov.uk" + basePath,
                root.path("public_updated_at").asText(""));
        return true;
    }

    private static String ukSeverity(JsonNode alerts) {
        String j = alerts.toString();
        if (j.contains("avoid_all_travel_to_whole_country"))
            return "critical";
        if (j.contains("avoid_all_travel"))
            return "high";
        if (j.contains("avoid_all_but_essential"))
            return "medium";
        return "low";
    }

    private static String ukLevel(JsonNode alerts) {
        if (!alerts.isArray() || alerts.isEmpty())
            return "No blanket travel warning";
        String j = alerts.toString();
        boolean whole = j.contains("whole_country");
        if (j.contains("avoid_all_travel"))
            return whole ? "Advises against all travel" : "Advises against all travel to parts";
        if (j.contains("avoid_all_but_essential"))
            return whole ? "Advises against all but essential travel"
                    : "Advises against all but essential travel to parts";
        return "Active advisory in effect";
    }

    // --- US State Department ---

    private int refreshUs() {
        JsonNode root = get(US_API);
        if (root == null || !root.isArray()) {
            return 0;
        }
        // country name -> parsed advisory fields
        Map<String, String[]> byCountry = new HashMap<>(); // name -> [level, severity, summary, url, updated]
        for (JsonNode item : root) {
            String title = item.path("Title").asText("");
            Matcher m = US_TITLE.matcher(title);
            if (!m.find()) {
                continue;
            }
            // Some titles read "Mexico Travel Advisory - Level 2"; drop the suffix.
            String country = m.group(1).trim().replaceAll("(?i)\\s+travel advisory$", "").trim();
            int level = Integer.parseInt(m.group(2));
            byCountry.put(country.toLowerCase(), new String[] {
                    usLevelText(level),
                    usSeverity(level),
                    stripHtml(item.path("Summary").asText("")),
                    item.path("Link").asText("https://travel.state.gov/content/travel/en/traveladvisories.html"),
                    item.path("Updated").asText(""),
            });
        }

        int ok = 0;
        for (CountryLookup.Country c : CountryLookup.advisoryCountries()) {
            String[] a = byCountry.get(c.name().toLowerCase());
            if (a == null) {
                continue; // US doesn't cover it (e.g. USA itself), skip
            }
            try {
                upsert(c.name(), US_SOURCE, a[0], a[1], a[2], a[3], a[4]);
                ok++;
            } catch (Exception e) {
                System.out.println("[advisory] US " + c.name() + " skipped: " + e.getMessage());
            }
        }
        return ok;
    }

    private static String usLevelText(int level) {
        return switch (level) {
            case 1 -> "Level 1: Exercise normal precautions";
            case 2 -> "Level 2: Exercise increased caution";
            case 3 -> "Level 3: Reconsider travel";
            case 4 -> "Level 4: Do not travel";
            default -> "Advisory in effect";
        };
    }

    private static String usSeverity(int level) {
        return switch (level) {
            case 1 -> "low";
            case 2 -> "medium";
            case 3 -> "high";
            case 4 -> "critical";
            default -> "low";
        };
    }

    // --- shared ---

    private JsonNode get(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "RoamSafe/1.0 (+https://roamsafe.app)");
        ResponseEntity<JsonNode> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                JsonNode.class);
        return resp.getBody();
    }

    private void upsert(String country, String source, String level, String severity,
            String summary, String url, String updated) {
        Advisory a = advisoryRepository.findByCountryNameAndSource(country, source).orElseGet(Advisory::new);
        a.setCountryName(country);
        a.setSource(source);
        a.setLevel(level);
        a.setSeverity(severity);
        a.setSummary(summary);
        a.setUrl(url);
        a.setSourceUpdated(updated);
        a.setLastFetched(LocalDateTime.now());
        advisoryRepository.save(a);
    }

    private static String stripHtml(String s) {
        if (s == null)
            return "";
        String text = s.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
        return text.length() > 600 ? text.substring(0, 597).trim() + "..." : text;
    }
}
