package com.zainab.roamSafe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zainab.roamSafe.model.LiveIncident;
import com.zainab.roamSafe.repository.LiveIncidentRepository;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Pulls current disruption reporting for covered cities from GDELT.
 *
 * This is the only source here that is genuinely live, which is what makes
 * "there is a transport strike in Paris right now" answerable at all. It is also
 * the source most capable of embarrassing us, because a keyword search over
 * world news returns a great deal that has nothing to do with travelling
 * somewhere. A raw query for Paris strikes returns "Climate strike named Collin
 * word of the year 2019".
 *
 * So the filter is deliberately strict, and errs towards showing nothing:
 *
 * - the city must appear in the headline itself, not merely in the article body,
 *   which removes national and unrelated coverage
 * - the headline must also carry a disruption term
 * - headlines are stored verbatim and never summarised. Paraphrasing a headline
 *   would turn an unverified news report into a RoamSafe assertion
 *
 * On a live sample this took 30 Paris articles down to 3, all of them genuinely
 * travel-affecting. Missing a real story is recoverable; publishing a scare that
 * isn't happening is not.
 */
@Service
public class GdeltIngestionService {

    private static final String ENDPOINT = "https://api.gdeltproject.org/api/v2/doc/doc";

    /**
     * GDELT asks for one request every five seconds, and throttles harder than
     * that under bursts, so this sits well clear of the stated limit.
     */
    private static final long PAUSE_MS = 10_000;

    /** Wait before a single retry when throttled anyway. */
    private static final long RETRY_BACKOFF_MS = 20_000;

    /** How far back a story can be and still count as current. */
    private static final int WINDOW_DAYS = 14;

    /** Cities to poll per run, most-reported first, to bound the runtime. */
    private static final int CITIES_PER_RUN = 40;

    private static final Pattern DISRUPTION = Pattern.compile(
            "\\b(strike|protest|demonstration|riot|unrest|closed|closure|shutdown|disrupt\\w*"
                    + "|cancel\\w*|evacuat\\w*|curfew|blockade|walkout|lockdown|wildfire|flood\\w*"
                    + "|earthquake|storm|outage|terror\\w*|explosion)\\b",
            Pattern.CASE_INSENSITIVE);

    private final LiveIncidentRepository incidentRepository;
    private final ScamReportRepository reportRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public GdeltIngestionService(LiveIncidentRepository incidentRepository,
            ScamReportRepository reportRepository) {
        this.incidentRepository = incidentRepository;
        this.reportRepository = reportRepository;
    }

    public record RefreshResult(int citiesPolled, int stored, int pruned, List<String> failed) {
    }

    /**
     * Every six hours, first run 20 minutes after boot so it never competes with
     * startup. Paced polling means a full run takes several minutes.
     */
    @Scheduled(fixedRate = 6L * 60 * 60 * 1000, initialDelay = 1_200_000)
    public synchronized RefreshResult refreshAll() {
        List<String> cities = new ArrayList<>();
        for (Object[] row : reportRepository.findTopCities(PageRequest.of(0, CITIES_PER_RUN))) {
            if (row[0] != null) {
                cities.add((String) row[0]);
            }
        }

        int stored = 0;
        List<String> failed = new ArrayList<>();
        for (String city : cities) {
            try {
                stored += ingestCity(city);
            } catch (Exception e) {
                failed.add(city);
            }
            pause();
        }

        int pruned = prune();
        System.out.println("[gdelt] Polled " + cities.size() + " cities, stored " + stored
                + " new incidents, pruned " + pruned
                + (failed.isEmpty() ? "." : ", failed: " + failed.size()));
        return new RefreshResult(cities.size(), stored, pruned, failed);
    }

    /** Fetch, filter and store one city's current disruption reporting. */
    public int ingestCity(String city) {
        String query = "\"" + city + "\" (strike OR protest OR demonstration OR closure OR disruption "
                + "OR evacuation OR curfew) sourcelang:english";
        URI uri = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(ENDPOINT)
                .queryParam("query", query)
                .queryParam("mode", "artlist")
                .queryParam("maxrecords", 30)
                .queryParam("format", "json")
                .queryParam("timespan", WINDOW_DAYS + "d")
                .build().encode().toUri();

        // GDELT signals throttling with a 429, which RestTemplate raises rather
        // than returns, so it has to be caught - an uncaught one failed the whole
        // request. Back off once and give up quietly after that: missing a poll
        // is harmless, the next scheduled run picks it up.
        String body;
        try {
            body = get(uri);
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            sleep(RETRY_BACKOFF_MS);
            try {
                body = get(uri);
            } catch (Exception retry) {
                System.out.println("[gdelt] Still throttled for " + city + "; skipping this run.");
                return 0;
            }
        }
        if (body == null || body.isBlank() || body.trim().startsWith("Please limit")) {
            return 0;
        }

        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            return 0; // GDELT returns plain text on some errors
        }

        int stored = 0;
        for (JsonNode article : root.path("articles")) {
            String title = article.path("title").asText("").trim();
            String url = article.path("url").asText("").trim();
            if (title.isEmpty() || url.isEmpty()) {
                continue;
            }
            if (!isRelevant(city, title)) {
                continue;
            }
            if (incidentRepository.findBySourceUrl(url).isPresent()) {
                continue;
            }
            incidentRepository.save(new LiveIncident(city, truncate(title, 500), truncate(url, 900),
                    article.path("domain").asText(""), parseDate(article.path("seendate").asText(""))));
            stored++;
        }
        return stored;
    }

    /**
     * GET with an identifying User-Agent.
     *
     * Without one, RestTemplate sends the default Java agent and GDELT answers
     * 429 to every request regardless of pacing - the identical query from a
     * plain HTTP client succeeded at the same moment, which is how this was
     * found. Identifying the caller is also the courteous thing to do on a free
     * public service.
     */
    private String get(URI uri) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", "RoamSafe/1.0 (travel safety intelligence; +https://roamsafe.app)");
        headers.set("Accept", "application/json");
        return restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), String.class).getBody();
    }

    /**
     * Whether a headline is about a disruption in this city.
     *
     * Both tests must pass: the city named in the headline itself, and a
     * disruption term in the headline. Matching the article body instead is far
     * too loose - that is what let "Climate strike named Collin word of the year
     * 2019" through as a Paris travel incident.
     *
     * Public so it can be tested without a network call.
     */
    public static boolean isRelevant(String city, String title) {
        if (city == null || title == null) {
            return false;
        }
        return title.toLowerCase(Locale.ROOT).contains(city.toLowerCase(Locale.ROOT))
                && DISRUPTION.matcher(title).find();
    }

    /** Drop stories that have aged out: last month's strike isn't current. */
    private int prune() {
        List<LiveIncident> old = incidentRepository
                .findByPublishedAtBefore(LocalDateTime.now().minusDays(WINDOW_DAYS * 2L));
        incidentRepository.deleteAll(old);
        return old.size();
    }

    /** GDELT stamps are "20260713T101500Z". */
    private static LocalDateTime parseDate(String stamp) {
        try {
            return LocalDateTime.parse(stamp.replace("Z", ""),
                    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static void pause() {
        sleep(PAUSE_MS);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
