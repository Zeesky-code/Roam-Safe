package com.zainab.roamSafe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zainab.roamSafe.model.PracticalInfo;
import com.zainab.roamSafe.repository.PracticalInfoRepository;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ingests arrival and practical information from Wikivoyage.
 *
 * This is what makes "how do I get out of the airport" and "where do I buy a SIM"
 * answerable without making anything up. The alternative was to write plausible
 * airport advice from general knowledge, which is precisely the kind of
 * confident, unsourced claim this product exists not to make - and getting it
 * wrong strands someone at an airport at midnight.
 *
 * Excerpts are stored close to verbatim, with markup stripped and a link back.
 * They are not summarised: compressing "the Havabus runs to Taksim until 01:00"
 * can quietly drop the qualifier that made it true. Wikivoyage is CC BY-SA, so
 * attribution is a licence term as well as good practice, and both the source
 * name and licence travel with every row.
 */
@Service
public class PracticalInfoService {

    private static final String API = "https://en.wikivoyage.org/w/api.php";
    private static final String PAGE_URL = "https://en.wikivoyage.org/wiki/";

    /** Be gentle with a free wiki. */
    private static final long PAUSE_MS = 1_500;

    /** Excerpts longer than this are trimmed at a sentence boundary. */
    private static final int MAX_CHARS = 2_600;

    /**
     * Wikivoyage headings mapped to our topics. "By plane" sits under "Get in"
     * and is the airport-arrival section travellers actually need.
     */
    private static final Map<String, String> SECTIONS = new LinkedHashMap<>(Map.of(
            "By plane", "AIRPORT",
            "Get around", "TRANSPORT",
            "Buy", "MONEY",
            "Connect", "CONNECT"));

    private final PracticalInfoRepository repository;
    private final ScamReportRepository reportRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public PracticalInfoService(PracticalInfoRepository repository, ScamReportRepository reportRepository) {
        this.repository = repository;
        this.reportRepository = reportRepository;
    }

    public record IngestResult(int cities, int sections, List<String> missing) {
    }

    /** Ingest the covered cities, most-reported first. */
    public IngestResult ingestTopCities(int limit) {
        int cities = 0, sections = 0;
        List<String> missing = new ArrayList<>();
        for (Object[] row : reportRepository.findTopCities(PageRequest.of(0, limit))) {
            String city = (String) row[0];
            if (city == null) {
                continue;
            }
            int n = ingestCity(city);
            if (n == 0) {
                missing.add(city);
            } else {
                sections += n;
            }
            cities++;
            pause();
        }
        System.out.println("[practical] " + cities + " cities, " + sections + " sections stored"
                + (missing.isEmpty() ? "." : ", no page/sections for " + missing.size()));
        return new IngestResult(cities, sections, missing);
    }

    /** Fetch one city's page and store the sections we understand. */
    public int ingestCity(String city) {
        String wikitext = fetchWikitext(city);
        if (wikitext == null || wikitext.isBlank()) {
            return 0;
        }
        int stored = 0;
        for (Map.Entry<String, String> entry : SECTIONS.entrySet()) {
            String body = extractSection(wikitext, entry.getKey());
            if (body == null) {
                continue;
            }
            String clean = stripMarkup(body);
            if (clean.length() < 120) {
                continue; // a stub heading isn't worth publishing
            }
            String url = PAGE_URL + city.replace(' ', '_');
            PracticalInfo row = repository.findByCityNameIgnoreCaseAndTopic(city, entry.getValue())
                    .orElseGet(PracticalInfo::new);
            row.setCityName(city);
            row.setTopic(entry.getValue());
            row.setContent(trim(clean));
            row.setSourceUrl(url);
            row.setSourceName("Wikivoyage");
            row.setLicence("CC BY-SA 4.0");
            row.setRetrievedAt(LocalDateTime.now());
            repository.save(row);
            stored++;
        }
        return stored;
    }

    private String fetchWikitext(String city) {
        try {
            URI uri = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(API)
                    .queryParam("action", "parse")
                    .queryParam("page", city)
                    .queryParam("prop", "wikitext")
                    .queryParam("format", "json")
                    .queryParam("formatversion", 2)
                    .queryParam("redirects", 1)
                    .build().encode().toUri();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "RoamSafe/1.0 (travel safety intelligence; contact zainablaw1012@gmail.com)");
            String body = restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(headers), String.class).getBody();
            if (body == null) {
                return null;
            }
            var node = mapper.readTree(body).path("parse").path("wikitext");
            return node.isMissingNode() ? null : node.asText();
        } catch (Exception e) {
            return null; // no page, redirect miss, or transient failure
        }
    }

    /**
     * The body of a section, up to the next heading of the same or higher level.
     * Sub-sections are kept: "By plane" often has the useful detail nested under
     * the individual airports.
     */
    static String extractSection(String wikitext, String heading) {
        Matcher m = Pattern.compile("^(={2,4})\\s*" + Pattern.quote(heading) + "\\s*\\1\\s*$",
                Pattern.MULTILINE).matcher(wikitext);
        if (!m.find()) {
            return null;
        }
        int level = m.group(1).length();
        int start = m.end();
        Matcher next = Pattern.compile("^={2," + level + "}[^=].*?={2," + level + "}\\s*$",
                Pattern.MULTILINE).matcher(wikitext);
        int end = wikitext.length();
        if (next.find(start)) {
            end = next.start();
        }
        return wikitext.substring(start, end);
    }

    /** Wiki markup to readable prose, without rewording anything. */
    static String stripMarkup(String wiki) {
        String s = wiki;
        s = s.replaceAll("(?s)\\{\\{[^{}]*\\}\\}", " ");   // templates
        s = s.replaceAll("(?s)\\{\\{.*?\\}\\}", " ");      // nested leftovers
        s = s.replaceAll("(?s)<ref[^>]*>.*?</ref>", " ");
        s = s.replaceAll("(?s)<!--.*?-->", " ");
        s = s.replaceAll("(?s)<[^>]+>", " ");
        // Drop file and image embeds outright. Rendering [[File:x.jpg|thumb|A
        // fortress]] as its caption dropped "thumb|Rumeli Fortress" into the
        // middle of airport directions, which reads as instructions.
        s = s.replaceAll("(?s)\\[\\[(File|Image|Media)\\s*:.*?\\]\\]", " ");
        s = s.replaceAll("(?m)^\\s*(thumb|left|right|center|frame)\\|.*$", "");
        // A caption can also survive when its brackets sat on another line, so
        // "thumb|A songthaew serves as a bus" arrives mid-paragraph and reads as
        // transport guidance. Remove the fragment wherever it appears.
        s = s.replaceAll("(?i)\\b(thumb|thumbnail|upright)\\s*\\|[^\\n]*", " ");
        s = s.replaceAll("\\[\\[[^\\]|]*\\|([^\\]]*)\\]\\]", "$1"); // [[a|b]] -> b
        s = s.replaceAll("\\[\\[([^\\]]*)\\]\\]", "$1");
        s = s.replaceAll("\\[https?://\\S+\\s+([^\\]]*)\\]", "$1");
        s = s.replaceAll("\\[https?://\\S+\\]", " ");
        s = s.replaceAll("'''''|'''|''", "");
        // Sub-headings first, and kept as plain labels. They are useful
        // structure ("Internet", "Markets"); deleting them lost that, and doing
        // this after list conversion left "• === Internet ===".
        s = s.replaceAll("(?m)^\\s*={2,6}\\s*(.+?)\\s*={2,6}\\s*$", "\n$1\n");

        s = s.replaceAll("(?m)^[*#:;]+\\s*", "• ");
        s = s.replaceAll("(?m)^(•\\s*)+", "• ");          // collapse "• • • •"
        s = s.replaceAll("(?m)^\\s*•\\s*$", "");          // empty bullets
        s = s.replaceAll("[ \\t]+", " ");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }

    /** Trim long excerpts at a sentence end so nothing is cut mid-claim. */
    private static String trim(String text) {
        if (text.length() <= MAX_CHARS) {
            return text;
        }
        String cut = text.substring(0, MAX_CHARS);
        int lastStop = Math.max(cut.lastIndexOf(". "), cut.lastIndexOf("\n"));
        return (lastStop > MAX_CHARS / 2 ? cut.substring(0, lastStop + 1) : cut).trim() + " […]";
    }

    private static void pause() {
        try {
            Thread.sleep(PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
