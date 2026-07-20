package com.zainab.roamSafe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zainab.roamSafe.model.PoliceCrimeStat;
import com.zainab.roamSafe.repository.PoliceCrimeStatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Ingests police-recorded crime from data.police.uk.
 *
 * Why this source matters: it is official, free, requires no key, carries a real
 * calendar month, and is geocoded to street level. Where the imported travel
 * guidance is undated prose, this is dated evidence — so it is the one dataset
 * that can honestly back a "what changed this month" claim.
 *
 * Scope is England, Wales and Northern Ireland. Police Scotland does not publish
 * to this API, so Edinburgh and Glasgow are deliberately absent rather than
 * filled in from a weaker source.
 */
@Service
public class PoliceDataService {

    private static final String LAST_UPDATED = "https://data.police.uk/api/crime-last-updated";
    private static final String STREET_CRIME = "https://data.police.uk/api/crimes-street/all-crime?lat=%s&lng=%s&date=%s";

    /** How many months back to pull on a refresh. */
    private static final int MONTHS = 3;

    /**
     * Below this, a city-month is treated as missing rather than peaceful.
     *
     * Real city centres in this dataset run from ~500 (Oxford) to ~4,400
     * (London) crimes a month, so the floor sits an order of magnitude under any
     * genuine reading and only catches forces that have stopped supplying. It is
     * a coverage check, not a smoothing rule: qualifying counts are stored
     * exactly as the source reports them.
     */
    private static final int MIN_PLAUSIBLE_MONTHLY_CRIMES = 50;

    /**
     * City-centre coordinates for the covered cities.
     *
     * These are approximate centre points, not boundaries: the source's
     * street-level endpoint returns crimes within about a one-mile radius of the
     * point, so every figure here is a central-area sample. That limitation is
     * carried through to the API response rather than being smoothed over.
     */
    private static final Map<String, double[]> UK_CITIES = new LinkedHashMap<>();
    static {
        UK_CITIES.put("London", new double[] { 51.5074, -0.1278 });
        UK_CITIES.put("Manchester", new double[] { 53.4808, -2.2426 });
        UK_CITIES.put("Birmingham", new double[] { 52.4862, -1.8904 });
        UK_CITIES.put("Liverpool", new double[] { 53.4084, -2.9916 });
        UK_CITIES.put("Leeds", new double[] { 53.8008, -1.5491 });
        UK_CITIES.put("Bristol", new double[] { 51.4545, -2.5879 });
        UK_CITIES.put("Newcastle", new double[] { 54.9783, -1.6178 });
        UK_CITIES.put("Sheffield", new double[] { 53.3811, -1.4701 });
        UK_CITIES.put("Nottingham", new double[] { 52.9548, -1.1581 });
        UK_CITIES.put("Cardiff", new double[] { 51.4816, -3.1791 });
        UK_CITIES.put("Belfast", new double[] { 54.5973, -5.9301 });
        UK_CITIES.put("Brighton", new double[] { 50.8225, -0.1372 });
        UK_CITIES.put("Oxford", new double[] { 51.7520, -1.2577 });
        UK_CITIES.put("Cambridge", new double[] { 52.2053, 0.1218 });
        UK_CITIES.put("Bath", new double[] { 51.3811, -2.3590 });
        UK_CITIES.put("York", new double[] { 53.9600, -1.0873 });
    }

    private final PoliceCrimeStatRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public PoliceDataService(PoliceCrimeStatRepository repository) {
        this.repository = repository;
    }

    /** Cities this service can ingest, for callers that want to show coverage. */
    public Set<String> supportedCities() {
        return Collections.unmodifiableSet(UK_CITIES.keySet());
    }

    /**
     * Monthly refresh, first run held back 10 minutes so it never competes with
     * startup (a slow boot gets the deploy killed by the host's health check).
     * The source publishes roughly monthly, so polling harder buys nothing.
     */
    @Scheduled(fixedRate = 30L * 24 * 60 * 60 * 1000, initialDelay = 600_000)
    public synchronized RefreshResult refreshAll() {
        String latest = latestAvailableMonth();
        if (latest == null) {
            System.out.println("[police] Could not determine latest month; skipping refresh.");
            return new RefreshResult(0, 0, List.of(), List.of());
        }

        List<String> months = recentMonths(latest, MONTHS);
        System.out.println("[police] Refreshing " + UK_CITIES.size() + " cities for months " + months);

        int rows = 0, crimes = 0;
        List<String> failed = new ArrayList<>();
        Set<String> unpublished = new LinkedHashSet<>();
        for (Map.Entry<String, double[]> city : UK_CITIES.entrySet()) {
            for (String month : months) {
                try {
                    int n = ingest(city.getKey(), city.getValue(), month);
                    // A force that stopped supplying still returns a handful of
                    // stray records from other forces operating in the area, and
                    // that reads as "almost no crime here" rather than "no data".
                    // Greater Manchester withdrew from this dataset years ago and
                    // returns ~4 crimes a month, which would rank Manchester the
                    // safest city in Britain. Anything this far below a real
                    // city-centre month is absence of coverage, not safety.
                    if (n < MIN_PLAUSIBLE_MONTHLY_CRIMES) {
                        unpublished.add(city.getKey());
                        politePause();
                        continue;
                    }
                    crimes += n;
                    rows++;
                } catch (Exception e) {
                    // One city-month failing must not abandon the rest: the source
                    // occasionally 502s under load and a partial refresh is still
                    // worth keeping.
                    failed.add(city.getKey() + "/" + month);
                }
                politePause();
            }
        }
        // Drop anything previously stored for a city we now know isn't published,
        // so an earlier run's misleading counts don't linger.
        for (String city : unpublished) {
            List<PoliceCrimeStat> stale = repository.findByCityNameIgnoreCase(city);
            if (!stale.isEmpty()) {
                repository.deleteAll(stale);
            }
        }
        System.out.println("[police] Stored " + rows + " city-months (" + crimes + " crimes)"
                + (unpublished.isEmpty() ? "" : ", no force data for: " + unpublished)
                + (failed.isEmpty() ? "" : ", " + failed.size() + " failed: " + failed));
        return new RefreshResult(rows, crimes, failed, new ArrayList<>(unpublished));
    }

    /** Result of a refresh, so an admin trigger can report what happened. */
    public record RefreshResult(int cityMonths, int crimes, List<String> failed, List<String> unpublished) {
    }



    /** Pull one city-month, aggregate by category, upsert. Returns crime count. */
    private int ingest(String city, double[] latLng, String month) {
        String url = String.format(STREET_CRIME, latLng[0], latLng[1], month);
        String body = restTemplate.getForObject(url, String.class);
        if (body == null || body.isBlank()) {
            return 0;
        }

        Map<String, Integer> byCategory = new LinkedHashMap<>();
        int total = 0;
        try {
            JsonNode arr = mapper.readTree(body);
            for (JsonNode crime : arr) {
                String category = crime.path("category").asText("");
                if (category.isBlank()) {
                    continue;
                }
                byCategory.merge(category, 1, Integer::sum);
                total++;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unparseable response for " + city + "/" + month, e);
        }

        for (Map.Entry<String, Integer> e : byCategory.entrySet()) {
            PoliceCrimeStat stat = repository
                    .findByCityNameAndMonthAndCategory(city, month, e.getKey())
                    .orElseGet(() -> new PoliceCrimeStat(city, month, e.getKey(), 0, latLng[0], latLng[1]));
            stat.setCount(e.getValue());
            stat.setLastFetched(LocalDateTime.now());
            repository.save(stat);
        }
        return total;
    }

    /** The most recent month the source has published, or null if unreachable. */
    private String latestAvailableMonth() {
        try {
            String body = restTemplate.getForObject(LAST_UPDATED, String.class);
            // {"date":"2026-05-01"} - trim to the YYYY-MM the crime endpoint wants.
            String date = mapper.readTree(body).path("date").asText("");
            return date.length() >= 7 ? date.substring(0, 7) : null;
        } catch (Exception e) {
            System.out.println("[police] last-updated lookup failed: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /** The n months ending at (and including) the given "YYYY-MM". */
    private static List<String> recentMonths(String latest, int n) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth end = YearMonth.parse(latest, fmt);
        List<String> months = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            months.add(end.minusMonths(i).format(fmt));
        }
        return months;
    }

    /** data.police.uk is free and unauthenticated; don't hammer it. */
    private static void politePause() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
