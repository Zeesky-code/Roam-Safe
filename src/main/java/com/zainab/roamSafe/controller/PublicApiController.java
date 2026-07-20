package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.CitySummary;
import com.zainab.roamSafe.model.SafetyScore;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;
import com.zainab.roamSafe.service.CitySummaryService;
import com.zainab.roamSafe.service.SafetyScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PublicApiController {

    private final SafetyScoreService safetyScoreService;
    private final CitySummaryService citySummaryService;
    private final ScamReportRepository scamReportRepository;
    private final com.zainab.roamSafe.service.DestinationService destinationService;
    private final com.zainab.roamSafe.service.ScamService scamService;
    private final com.zainab.roamSafe.service.StreetIntelligenceService streetService;
    private final com.zainab.roamSafe.service.CityCountryResolver countryResolver;
    private final com.zainab.roamSafe.service.EmergencyNumberService emergencyNumberService;
    private final com.zainab.roamSafe.repository.PracticalInfoRepository practicalInfoRepository;
    private final com.zainab.roamSafe.repository.LiveIncidentRepository liveIncidentRepository;

    public PublicApiController(SafetyScoreService safetyScoreService,
            CitySummaryService citySummaryService,
            ScamReportRepository scamReportRepository,
            com.zainab.roamSafe.service.DestinationService destinationService,
            com.zainab.roamSafe.service.ScamService scamService,
            com.zainab.roamSafe.service.StreetIntelligenceService streetService,
            com.zainab.roamSafe.service.CityCountryResolver countryResolver,
            com.zainab.roamSafe.service.EmergencyNumberService emergencyNumberService,
            com.zainab.roamSafe.repository.PracticalInfoRepository practicalInfoRepository,
            com.zainab.roamSafe.repository.LiveIncidentRepository liveIncidentRepository) {
        this.practicalInfoRepository = practicalInfoRepository;
        this.liveIncidentRepository = liveIncidentRepository;
        this.streetService = streetService;
        this.countryResolver = countryResolver;
        this.emergencyNumberService = emergencyNumberService;
        this.safetyScoreService = safetyScoreService;
        this.citySummaryService = citySummaryService;
        this.scamReportRepository = scamReportRepository;
        this.destinationService = destinationService;
        this.scamService = scamService;
    }

    /**
     * Cities RoamSafe actually covers, safest first. Lets a client (or an AI
     * agent) discover what's available instead of guessing city names, and
     * suggest real alternatives when a requested city isn't covered.
     */
    @GetMapping("/cities")
    public ResponseEntity<List<Map<String, Object>>> listCities() {
        List<Object[]> rows = scamService.getTopCities(500);

        // Read every cached score in one query rather than per-city (which would
        // recalculate and turn this into ~1 remote round-trip per city).
        Map<String, Integer> cached = new HashMap<>();
        for (Object[] r : safetyScoreService.getCachedCityScores()) {
            if (r[0] != null && r[1] != null) {
                cached.put(((String) r[0]).toLowerCase(), ((Number) r[1]).intValue());
            }
        }

        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String city = (String) row[0];
            long reports = ((Number) row[1]).longValue();
            Integer score = cached.get(city.toLowerCase());
            Map<String, Object> m = new HashMap<>();
            m.put("city", city);
            m.put("reports", reports);
            com.zainab.roamSafe.service.CountryLookup.forCity(city)
                    .ifPresent(c -> m.put("country", c.name()));
            if (score != null) {
                m.put("score", score);
                m.put("riskLevel", getRiskLevel(score));
            }
            out.add(m);
        }
        // Safest first; cities without a cached score yet sort last.
        out.sort((a, b) -> Integer.compare(
                (int) b.getOrDefault("score", -1), (int) a.getOrDefault("score", -1)));
        return ResponseEntity.ok(out);
    }

    /**
     * Get comprehensive risk data for a specific city.
     * This is the main endpoint partners like TripDesk will call.
     */
    @GetMapping("/risk/city/{city}")
    public ResponseEntity<Map<String, Object>> getCityRisk(@PathVariable String city) {
        Map<String, Object> response = new HashMap<>();

        // 1. Get Safety Score
        SafetyScore score = safetyScoreService.getScoreForCity(city);
        if (score == null) {
            // An empty 404 is ambiguous to a caller: wrong path, wrong key and
            // uncovered city all look identical. Since refusing to guess is the
            // point of this API, say so explicitly and point at the coverage
            // list, so an agent can distinguish "no data" from "broken request"
            // and doesn't fall back to inventing a score.
            response.put("city", city);
            response.put("covered", false);
            response.put("message", "RoamSafe has no coverage for this city. "
                    + "No safety score exists for it and none has been inferred.");
            response.put("coveredCitiesEndpoint", "/api/v1/cities");
            return ResponseEntity.status(404).body(response);
        }
        response.put("covered", true);

        // 2. Get AI Summary
        CitySummary summary = citySummaryService.getSummaryForCity(city);

        // 3. All approved reports for this city (full set drives the breakdowns;
        // the alert list stays capped).
        List<ScamReport> all = scamReportRepository.findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(
                city, ScamReportStatus.APPROVED);
        List<ScamReport> alerts = all.stream().limit(5).toList();

        int confidencePct = (int) Math.round(score.getConfidenceLevel() * 100);

        response.put("city", score.getCity().getName());
        response.put("country", score.getCity().getCountry());
        com.zainab.roamSafe.service.CountryLookup.forCity(city)
                .ifPresent(c -> response.put("country", c.name()));
        response.put("overallScore", score.getOverallScore()); // 0-100
        response.put("riskLevel", getRiskLevel(score.getOverallScore()));
        response.put("riskBreakdown", Map.of(
                "financial", score.getFinancialRiskScore(),
                "physical", score.getPhysicalRiskScore(),
                "digital", score.getDigitalRiskScore()));

        // How much evidence sits behind the numbers (counts + date range).
        response.put("coverage", destinationService.coverage(all, confidencePct));
        // Risk per reported concern, so callers can rank by theft/scams/etc.
        response.put("categoryBreakdown", destinationService.categoryBreakdown(all));
        // Neighborhood-level scores, not just city-wide.
        response.put("neighborhoods", destinationService.neighborhoodBreakdown(all));
        // Night-time signal + the areas those incidents cluster in.
        response.put("nightRisk", destinationService.nightRisk(all));

        response.put("summary", summary.getSummaryText());
        response.put("latestAlerts", alerts);
        response.put("lastUpdated", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Real-time feed of all new approved reports globally.
     * Partners can poll this or subscribe to it.
     */
    @GetMapping("/alerts/feed")
    public ResponseEntity<List<ScamReport>> getAlertsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Fetch paginated approved reports, newest first
        List<ScamReport> reports = scamReportRepository.findByStatusOrderByCreatedAtDesc(ScamReportStatus.APPROVED);
        // Implementing simple sublist pagination for now as repository method returns
        // List
        int start = Math.min(page * size, reports.size());
        int end = Math.min((page + 1) * size, reports.size());

        return ResponseEntity.ok(reports.subList(start, end));
    }

    /**
     * Map a safety score to a risk label.
     *
     * Calibrated to the current scoring model, where a score is
     * {@code 100 - recencyWeightedAvgSeverity * 85} and well-reported cities
     * land in the 45-70 band. The previous thresholds (LOW>=80, HIGH>=40) were
     * written for the old volume-based score and labelled almost every city
     * "HIGH", which badly overstates risk to API/MCP consumers.
     */
    private String getRiskLevel(int score) {
        if (score >= 70)
            return "LOW";
        if (score >= 55)
            return "MODERATE";
        if (score >= 40)
            return "ELEVATED";
        return "HIGH";
    }

    /**
     * Street-level intelligence for a named place.
     *
     * Searching returns every place that matches so a caller can disambiguate;
     * passing ?city= pins it to one profile. An unmatched place returns an
     * explicit no-coverage body, never an empty 404, so an agent can tell "no
     * data" from "bad request" and doesn't fall back on inventing an answer.
     */
    @GetMapping("/street")
    public ResponseEntity<Map<String, Object>> street(@RequestParam String place,
            @RequestParam(required = false) String city) {
        Map<String, Object> response = new HashMap<>();
        List<com.zainab.roamSafe.service.StreetIntelligenceService.Match> matches =
                streetService.search(place);

        if (matches.isEmpty()) {
            response.put("place", place);
            response.put("covered", false);
            response.put("message", "No report in RoamSafe names this place. "
                    + "That is an absence of evidence, not a finding that it is safe.");
            return ResponseEntity.status(404).body(response);
        }

        var target = matches.stream()
                .filter(m -> city == null || m.city().equalsIgnoreCase(city))
                .findFirst().orElse(null);
        if (target == null) {
            response.put("place", place);
            response.put("covered", false);
            response.put("message", "That place isn't reported in " + city + ".");
            response.put("matches", matches);
            return ResponseEntity.status(404).body(response);
        }

        String country = countryResolver.countryFor(target.city()).orElse(null);
        var profile = streetService.profile(target.city(), target.place(), country).orElse(null);
        if (profile == null) {
            response.put("place", place);
            response.put("covered", false);
            return ResponseEntity.status(404).body(response);
        }

        response.put("covered", true);
        response.put("place", profile.place());
        response.put("city", profile.city());
        response.put("country", profile.country());
        response.put("safetyScore", profile.score());
        response.put("riskLevel", profile.riskLevel());
        response.put("reports", profile.reports());
        // Null rather than 0 when there is too little evidence to state a share.
        response.put("nightIncidentSharePct", profile.nightSharePct());
        response.put("thinEvidence", profile.thinEvidence());
        response.put("concerns", profile.categories());
        response.put("preventionTips", profile.preventionTips());
        response.put("saferAlternatives", profile.saferAlternatives());
        response.put("evidence", profile.evidence().stream().map(r -> Map.of(
                "title", r.getName() == null ? "" : r.getName(),
                "description", r.getDescription() == null ? "" : r.getDescription(),
                "category", r.getCategory() == null ? "General" : r.getCategory(),
                "severityScore", r.getSeverityScore())).toList());
        if (country != null) {
            emergencyNumberService.forCountry(country).ifPresent(e -> {
                Map<String, Object> numbers = new HashMap<>();
                numbers.put("primary", e.getPrimary());
                numbers.put("general", e.getGeneral());
                numbers.put("police", e.getPolice());
                numbers.put("ambulance", e.getAmbulance());
                numbers.put("fire", e.getFire());
                numbers.put("source", e.getSource());
                response.put("emergencyNumbers", numbers);
            });
        }
        // Other cities with a place of the same name, so an agent can offer them.
        if (matches.size() > 1) {
            response.put("alsoFoundIn", matches.stream()
                    .filter(m -> !m.city().equalsIgnoreCase(target.city()))
                    .map(m -> Map.of("place", m.place(), "city", m.city(), "reports", m.reports()))
                    .toList());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Emergency numbers for a country. Split out so an agent can answer "what do
     * I dial" without pulling a whole city profile.
     */
    @GetMapping("/emergency/{country}")
    public ResponseEntity<Map<String, Object>> emergency(@PathVariable String country) {
        return emergencyNumberService.forCountry(country)
                .<ResponseEntity<Map<String, Object>>>map(e -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("country", e.getCountry());
                    body.put("covered", true);
                    body.put("primary", e.getPrimary());
                    body.put("general", e.getGeneral());
                    body.put("police", e.getPolice());
                    body.put("ambulance", e.getAmbulance());
                    body.put("fire", e.getFire());
                    body.put("source", e.getSource());
                    body.put("sourceUrl", e.getSourceUrl());
                    body.put("note", "Verify locally on arrival where you can.");
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("country", country);
                    body.put("covered", false);
                    body.put("message", "RoamSafe has no emergency numbers for this country. "
                            + "None have been inferred - check an official source before travelling.");
                    return ResponseEntity.status(404).body(body);
                });
    }

    /**
     * Arrival and practical information for a city: airport transfers, local
     * transport, money and connectivity.
     *
     * Excerpts are returned as stored - verbatim, with source and licence - so a
     * caller repeating them is repeating the source rather than a paraphrase,
     * and can cite it. An agent must not restate these as its own knowledge.
     */
    @GetMapping("/practical/{city}")
    public ResponseEntity<Map<String, Object>> practical(@PathVariable String city) {
        var rows = practicalInfoRepository.findByCityNameIgnoreCase(city);
        Map<String, Object> body = new HashMap<>();
        body.put("city", city);
        if (rows.isEmpty()) {
            body.put("covered", false);
            body.put("message", "RoamSafe has no arrival or practical information for this city. "
                    + "None has been inferred - do not substitute general knowledge.");
            return ResponseEntity.status(404).body(body);
        }
        body.put("covered", true);
        body.put("sections", rows.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("topic", p.getTopic());
            m.put("label", p.getLabel());
            m.put("content", p.getContent());
            m.put("source", p.getSourceName());
            m.put("sourceUrl", p.getSourceUrl());
            m.put("licence", p.getLicence());
            m.put("retrievedAt", p.getRetrievedAt());
            m.put("verbatim", true);
            return m;
        }).toList());
        body.put("note", "Excerpts are quoted, not summarised. Check times and prices at the source.");
        return ResponseEntity.ok(body);
    }

    /**
     * Current news mentions of a disruption in a city.
     *
     * These are unverified third-party reports, and the response says so on every
     * item: an agent repeating one must attribute it, not assert it.
     */
    @GetMapping("/incidents")
    public ResponseEntity<Map<String, Object>> incidents(
            @RequestParam(required = false) String city) {
        var rows = city == null || city.isBlank()
                ? liveIncidentRepository.findTop20ByOrderByPublishedAtDesc()
                : liveIncidentRepository.findByCityNameIgnoreCaseOrderByPublishedAtDesc(city);
        Map<String, Object> body = new HashMap<>();
        body.put("city", city);
        body.put("count", rows.size());
        body.put("verified", false);
        body.put("note", "Third-party news headlines, stored verbatim and not verified by RoamSafe. "
                + "They do not affect safety scores. Attribute them to the outlet, don't assert them.");
        body.put("incidents", rows.stream().map(i -> Map.of(
                "city", i.getCityName(),
                "headline", i.getTitle(),
                "outlet", i.getSourceDomain() == null ? "" : i.getSourceDomain(),
                "url", i.getSourceUrl(),
                "publishedAt", i.getPublishedAt() == null ? "" : i.getPublishedAt().toString())).toList());
        return ResponseEntity.ok(body);
    }
}
