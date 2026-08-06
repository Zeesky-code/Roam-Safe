package com.zainab.roamSafe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zainab.roamSafe.model.CoworkingSpace;
import com.zainab.roamSafe.repository.CoworkingSpaceRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Ingests coworking spaces from OpenStreetMap via the Overpass API.
 *
 * OSM tags real, named coworking spaces (office=coworking,
 * amenity=coworking_space). Surfacing them answers the digital-nomad "where can
 * I actually work from" question with public data rather than an invented list:
 * every space here is one a mapper recorded, stored with a link back to the OSM
 * element so it can be checked. Unnamed elements are skipped - a pin with no
 * name is not useful to a traveler.
 *
 * Overpass is a shared free service, so requests are paced and identify
 * themselves. Ingestion is a manual admin trigger, not a hot path.
 */
@Service
public class OsmCoworkingService {

    private static final String OVERPASS = "https://overpass-api.de/api/interpreter";
    private static final int RADIUS_M = 15000;

    private static final Map<String, double[]> COORDS = new LinkedHashMap<>();
    static {
        COORDS.put("Paris", new double[] { 48.8566, 2.3522 });
        COORDS.put("London", new double[] { 51.5074, -0.1278 });
        COORDS.put("Rome", new double[] { 41.9028, 12.4964 });
        COORDS.put("Barcelona", new double[] { 41.3851, 2.1734 });
        COORDS.put("Istanbul", new double[] { 41.0082, 28.9784 });
        COORDS.put("New York", new double[] { 40.7128, -74.006 });
        COORDS.put("New York City", new double[] { 40.7128, -74.006 });
        COORDS.put("Tokyo", new double[] { 35.6762, 139.6503 });
        COORDS.put("Bangkok", new double[] { 13.7563, 100.5018 });
        COORDS.put("Bali", new double[] { -8.4095, 115.1889 });
        COORDS.put("Cairo", new double[] { 30.0444, 31.2357 });
        COORDS.put("Lisbon", new double[] { 38.7223, -9.1393 });
        COORDS.put("Amsterdam", new double[] { 52.3676, 4.9041 });
        COORDS.put("Prague", new double[] { 50.0755, 14.4378 });
        COORDS.put("Dubai", new double[] { 25.2048, 55.2708 });
        COORDS.put("Mexico City", new double[] { 19.4326, -99.1332 });
        COORDS.put("Rio de Janeiro", new double[] { -22.9068, -43.1729 });
        COORDS.put("Cape Town", new double[] { -33.9249, 18.4241 });
        COORDS.put("Sydney", new double[] { -33.8688, 151.2093 });
        COORDS.put("Nairobi", new double[] { -1.2864, 36.8172 });
        COORDS.put("Marrakech", new double[] { 31.6295, -7.9811 });
        COORDS.put("Athens", new double[] { 37.9838, 23.7275 });
        COORDS.put("Vienna", new double[] { 48.2082, 16.3738 });
        COORDS.put("Berlin", new double[] { 52.52, 13.405 });
        COORDS.put("Singapore", new double[] { 1.3521, 103.8198 });
        COORDS.put("Seoul", new double[] { 37.5665, 126.978 });
        COORDS.put("Delhi", new double[] { 28.6139, 77.209 });
        COORDS.put("Mumbai", new double[] { 19.076, 72.8777 });
        COORDS.put("Accra", new double[] { 5.6037, -0.187 });
        COORDS.put("Addis Ababa", new double[] { 9.03, 38.74 });
        COORDS.put("Auckland", new double[] { -36.8485, 174.7633 });
        COORDS.put("Bogota", new double[] { 4.711, -74.0721 });
        COORDS.put("Bogotá", new double[] { 4.711, -74.0721 });
        COORDS.put("Bruges", new double[] { 51.2093, 3.2247 });
        COORDS.put("Budapest", new double[] { 47.4979, 19.0402 });
        COORDS.put("Buenos Aires", new double[] { -34.6037, -58.3816 });
        COORDS.put("Cancun", new double[] { 21.1619, -86.8515 });
        COORDS.put("Cancún", new double[] { 21.1619, -86.8515 });
        COORDS.put("Cartagena", new double[] { 10.391, -75.4794 });
        COORDS.put("Casablanca", new double[] { 33.5731, -7.5898 });
        COORDS.put("Chiang Mai", new double[] { 18.7883, 98.9853 });
        COORDS.put("Copenhagen", new double[] { 55.6761, 12.5683 });
        COORDS.put("Cusco", new double[] { -13.532, -71.9675 });
        COORDS.put("Dar es Salaam", new double[] { -6.7924, 39.2083 });
        COORDS.put("Dublin", new double[] { 53.3498, -6.2603 });
        COORDS.put("Fiji", new double[] { -17.7134, 178.065 });
        COORDS.put("Florence", new double[] { 43.7696, 11.2558 });
        COORDS.put("Hanoi", new double[] { 21.0278, 105.8342 });
        COORDS.put("Havana", new double[] { 23.1136, -82.3666 });
        COORDS.put("Helsinki", new double[] { 60.1699, 24.9384 });
        COORDS.put("Ho Chi Minh City", new double[] { 10.8231, 106.6297 });
        COORDS.put("Hong Kong", new double[] { 22.3193, 114.1694 });
        COORDS.put("Johannesburg", new double[] { -26.2041, 28.0473 });
        COORDS.put("Kathmandu", new double[] { 27.7172, 85.324 });
        COORDS.put("Krakow", new double[] { 50.0647, 19.945 });
        COORDS.put("Kraków", new double[] { 50.0647, 19.945 });
        COORDS.put("Kuala Lumpur", new double[] { 3.139, 101.6869 });
        COORDS.put("La Paz", new double[] { -16.4897, -68.1193 });
        COORDS.put("Las Vegas", new double[] { 36.1699, -115.1398 });
        COORDS.put("Lima", new double[] { -12.0464, -77.0428 });
        COORDS.put("Los Angeles", new double[] { 34.0522, -118.2437 });
        COORDS.put("Manila", new double[] { 14.5995, 120.9842 });
        COORDS.put("Medellín", new double[] { 6.2442, -75.5812 });
        COORDS.put("Melbourne", new double[] { -37.8136, 144.9631 });
        COORDS.put("Miami", new double[] { 25.7617, -80.1918 });
        COORDS.put("Nice", new double[] { 43.7102, 7.262 });
        COORDS.put("Osaka", new double[] { 34.6937, 135.5023 });
        COORDS.put("Phuket", new double[] { 7.8804, 98.3923 });
        COORDS.put("Queenstown", new double[] { -45.0312, 168.6626 });
        COORDS.put("Quito", new double[] { -0.1807, -78.4678 });
        COORDS.put("San Francisco", new double[] { 37.7749, -122.4194 });
        COORDS.put("Santiago", new double[] { -33.4489, -70.6693 });
        COORDS.put("Shanghai", new double[] { 31.2304, 121.4737 });
        COORDS.put("Siem Reap", new double[] { 13.3671, 103.8448 });
        COORDS.put("Stockholm", new double[] { 59.3293, 18.0686 });
        COORDS.put("Taipei", new double[] { 25.033, 121.5654 });
        COORDS.put("Tel Aviv", new double[] { 32.0853, 34.7818 });
        COORDS.put("Venice", new double[] { 45.4408, 12.3155 });
        COORDS.put("Zanzibar", new double[] { -6.1659, 39.2026 });
    }

    private final CoworkingSpaceRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public OsmCoworkingService(CoworkingSpaceRepository repository) {
        this.repository = repository;
    }

    public record RefreshResult(int cities, int stored, List<String> failed) {
    }

    /** Pull coworking spaces for every city with coordinates. Paced for Overpass. */
    public synchronized RefreshResult refreshAll() {
        int stored = 0, cities = 0;
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, double[]> e : COORDS.entrySet()) {
            try {
                stored += ingest(e.getKey(), e.getValue()[0], e.getValue()[1]);
                cities++;
            } catch (Exception ex) {
                failed.add(e.getKey());
            }
            pause();
        }
        System.out.println("[coworking] Stored " + stored + " spaces across " + cities + " cities"
                + (failed.isEmpty() ? "." : ", failed: " + failed));
        return new RefreshResult(cities, stored, failed);
    }

    /** One city: query Overpass, upsert named coworking spaces. Returns count. */
    public int ingest(String city, double lat, double lng) {
        String query = "[out:json][timeout:25];("
                + "nwr[\"office\"=\"coworking\"](around:" + RADIUS_M + "," + lat + "," + lng + ");"
                + "nwr[\"amenity\"=\"coworking_space\"](around:" + RADIUS_M + "," + lat + "," + lng + ");"
                + ");out center tags 60;";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "RoamSafe/1.0 (travel safety intelligence; +https://roamsafe.app)");
        String body = restTemplate.exchange(OVERPASS, HttpMethod.POST,
                new HttpEntity<>("data=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8),
                        headers),
                String.class).getBody();
        if (body == null || body.isBlank()) {
            return 0;
        }

        int stored = 0;
        try {
            for (JsonNode el : mapper.readTree(body).path("elements")) {
                JsonNode tags = el.path("tags");
                String name = tags.path("name").asText("").trim();
                // Skip unnamed pins and junk names (a bare number like "401" is a
                // real OSM element but useless to a traveler). Must have a letter.
                if (name.length() < 3 || !name.matches(".*\\p{L}.*")) {
                    continue;
                }
                String osmId = el.path("type").asText("node") + "/" + el.path("id").asLong();
                double elLat = el.has("lat") ? el.path("lat").asDouble()
                        : el.path("center").path("lat").asDouble(Double.NaN);
                double elLng = el.has("lon") ? el.path("lon").asDouble()
                        : el.path("center").path("lon").asDouble(Double.NaN);
                String website = firstNonBlank(tags.path("website").asText(""),
                        tags.path("contact:website").asText(""), tags.path("url").asText(""));

                CoworkingSpace space = repository.findByOsmId(osmId).orElseGet(CoworkingSpace::new);
                space.setCityName(city);
                space.setName(truncate(name, 200));
                space.setOsmId(osmId);
                space.setLatitude(Double.isNaN(elLat) ? null : elLat);
                space.setLongitude(Double.isNaN(elLng) ? null : elLng);
                space.setWebsite(website.isBlank() ? null : truncate(website, 300));
                space.setRetrievedAt(LocalDateTime.now());
                repository.save(space);
                stored++;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unparseable Overpass response for " + city, e);
        }
        return stored;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String truncate(String v, int max) {
        return v.length() <= max ? v : v.substring(0, max);
    }

    private static void pause() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
