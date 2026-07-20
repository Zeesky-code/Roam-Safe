package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.SafetyScore;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reviews a pasted itinerary against the evidence RoamSafe holds.
 *
 * Deliberately not a language model. The product's position is that it is not
 * ChatGPT for travel, and a model asked to review an itinerary will produce
 * confident, plausible, unsourced advice - which is the failure this whole
 * codebase has been pulling out. So this matches the itinerary against real
 * reports and returns findings that each carry the evidence behind them. If
 * nothing in the data speaks to a line, the review says nothing about it rather
 * than filling the space.
 *
 * What it can read: covered cities, named places, arrival and activity times,
 * and transport choices. What it deliberately cannot: whether a street is noisy,
 * how long a transfer takes, or which route is prettier - the design's example
 * mentions those and no source here carries them.
 */
@Service
public class TripReviewService {

    /** Above this share of night-time reports, a late arrival is worth flagging. */
    private static final int NIGHT_RISK_THRESHOLD = 40;

    private static final Pattern LATE_TIME = Pattern.compile(
            "\\b(1[0-9]|2[0-3])\\s*(:|\\.)?\\d{0,2}\\s*(pm|p\\.m\\.)\\b"
                    + "|\\b(2[0-3]|1[89]):[0-5][0-9]\\b"
                    + "|\\b(late|midnight|overnight|red[- ]?eye|after dark|at night)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> TRANSPORT = Map.of(
            "taxi", "Transport", "cab", "Transport", "uber", "Transport",
            "tuk tuk", "Transport", "rickshaw", "Transport", "minibus", "Transport");

    private final ScamReportRepository reportRepository;
    private final SafetyScoreService safetyScoreService;
    private final DestinationService destinationService;
    private final StreetIntelligenceService streetService;
    private final CityCountryResolver countryResolver;
    private final EmergencyNumberService emergencyNumberService;

    public TripReviewService(ScamReportRepository reportRepository,
            SafetyScoreService safetyScoreService,
            DestinationService destinationService,
            StreetIntelligenceService streetService,
            CityCountryResolver countryResolver,
            EmergencyNumberService emergencyNumberService) {
        this.reportRepository = reportRepository;
        this.safetyScoreService = safetyScoreService;
        this.destinationService = destinationService;
        this.streetService = streetService;
        this.countryResolver = countryResolver;
        this.emergencyNumberService = emergencyNumberService;
    }

    /** One finding, with the evidence that produced it. */
    public record Finding(String severity, String headline, String detail,
            String quotedLine, String linkLabel, String linkUrl,
            List<ScamReport> evidence) {
    }

    public record Review(String itinerary, List<String> citiesFound, List<String> placesFound,
            List<Finding> findings, List<String> notAssessed, String emergencyNote) {
    }

    public Review review(String itinerary) {
        String text = itinerary == null ? "" : itinerary.trim();
        if (text.isEmpty()) {
            return new Review("", List.of(), List.of(), List.of(), notAssessed(), null);
        }

        List<String> lines = Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim).filter(l -> !l.isEmpty()).toList();

        List<String> cities = detectCities(text);
        List<Finding> findings = new ArrayList<>();
        Set<String> places = new LinkedHashSet<>();

        for (String city : cities) {
            findings.addAll(cityFindings(city, lines));
        }
        for (String city : cities) {
            for (String line : lines) {
                for (StreetIntelligenceService.Match m : placesIn(line, city)) {
                    if (places.add(m.place())) {
                        placeFinding(city, m, line).ifPresent(findings::add);
                    }
                }
            }
        }

        // Most severe first, so the thing worth acting on is at the top.
        findings.sort(Comparator.comparingInt(f -> severityRank(f.severity())));

        String emergency = cities.stream().findFirst()
                .flatMap(countryResolver::countryFor)
                .flatMap(emergencyNumberService::forCountry)
                .map(e -> "In " + e.getCountry() + ", the emergency number is " + e.getPrimary() + ".")
                .orElse(null);

        return new Review(text, cities, new ArrayList<>(places), findings, notAssessed(), emergency);
    }

    /** Findings that come from the city as a whole. */
    private List<Finding> cityFindings(String city, List<String> lines) {
        List<Finding> out = new ArrayList<>();
        List<ScamReport> reports = reportRepository
                .findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(city, ScamReportStatus.APPROVED);
        if (reports.isEmpty()) {
            return out;
        }

        SafetyScore score = safetyScoreService.getScoreForCity(city);
        var night = destinationService.nightRisk(reports);

        // Late arrival, where the city's own reports skew night-time.
        for (String line : lines) {
            if (!LATE_TIME.matcher(line).find() || !mentions(line, city)) {
                continue;
            }
            if (night.nightIncidentShare() >= NIGHT_RISK_THRESHOLD) {
                List<ScamReport> nightReports = reports.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getIsNightTimeIncident()))
                        .limit(3).toList();
                out.add(new Finding("high",
                        "Late arrival in " + city,
                        night.nightIncidentShare() + "% of " + city + "'s reports are night-time incidents"
                                + (night.topNightAreas().isEmpty() ? "."
                                        : ", clustering around " + String.join(", ", night.topNightAreas()) + "."),
                        line, "See " + city + " report", "/scams?city=" + city, nightReports));
            }
            break;
        }

        // Transport choices the reports have something to say about.
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> t : TRANSPORT.entrySet()) {
                if (!lower.contains(t.getKey())) {
                    continue;
                }
                List<ScamReport> matching = reports.stream()
                        .filter(r -> mentionsWord(r.getName(), t.getKey())
                                || mentionsWord(r.getDescription(), t.getKey()))
                        .limit(3).toList();
                if (matching.isEmpty()) {
                    continue; // no evidence, so no finding
                }
                out.add(new Finding("high",
                        capitalise(t.getKey()) + " in " + city,
                        matching.size() == 1
                                ? "One report in " + city + " describes a problem with this."
                                : matching.size() + "+ reports in " + city + " describe problems with this.",
                        line, "See " + city + " report", "/scams?city=" + city, matching));
                break;
            }
        }

        // Overall standing of the destination, always worth stating once.
        if (score != null && score.getOverallScore() != null) {
            out.add(new Finding(score.getOverallScore() < 55 ? "medium" : "info",
                    city + " scores " + score.getOverallScore() + "/100",
                    "Computed from " + reports.size() + " reports, "
                            + (int) Math.round(score.getConfidenceLevel() * 100) + "% confidence.",
                    null, "Full " + city + " report", "/scams?city=" + city, List.of()));
        }
        return out;
    }

    /** A finding for a specific place named in the itinerary. */
    private Optional<Finding> placeFinding(String city, StreetIntelligenceService.Match match, String line) {
        return streetService.profile(city, match.place(), countryResolver.countryFor(city).orElse(null))
                .map(profile -> {
                    String severity = profile.score() == null ? "info"
                            : profile.score() < 50 ? "high" : profile.score() < 65 ? "medium" : "info";
                    String detail = profile.score() == null
                            ? "Only " + profile.reports() + " report names this area, too little to score it."
                            : profile.place() + " scores " + profile.score() + "/100 from "
                                    + profile.reports() + " reports"
                                    + (profile.nightSharePct() == null ? "."
                                            : ", " + profile.nightSharePct() + "% night-time.")
                                    + (profile.saferAlternatives().isEmpty() ? ""
                                            : " Better-scoring nearby: "
                                                    + profile.saferAlternatives().stream()
                                                            .map(a -> a.place() + " (" + a.score() + ")")
                                                            .reduce((a, b) -> a + ", " + b).orElse(""));
                    return new Finding(severity,
                            "Staying near " + profile.place(),
                            detail, line,
                            "Street report", "/street?q=" + profile.place() + "&city=" + city,
                            profile.evidence().stream().limit(2).toList());
                });
    }

    /** Covered cities named anywhere in the itinerary, longest match first. */
    private List<String> detectCities(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (Object[] row : reportRepository.findTopCities(PageRequest.of(0, 500))) {
            String city = (String) row[0];
            if (city != null && lower.contains(city.toLowerCase(Locale.ROOT))) {
                found.add(city);
            }
        }
        found.sort(Comparator.comparingInt(String::length).reversed());
        // Drop a city fully contained in a longer one already matched
        // ("New York" inside "New York City").
        List<String> out = new ArrayList<>();
        for (String c : found) {
            if (out.stream().noneMatch(kept -> kept.toLowerCase(Locale.ROOT)
                    .contains(c.toLowerCase(Locale.ROOT)))) {
                out.add(c);
            }
        }
        return out;
    }

    /** Places named in a line that RoamSafe actually has reports for. */
    private List<StreetIntelligenceService.Match> placesIn(String line, String city) {
        List<StreetIntelligenceService.Match> out = new ArrayList<>();
        for (String word : line.split("[,.;:]|\\bnear\\b|\\bin\\b|\\bat\\b")) {
            String candidate = word.trim().replaceAll("^(the|a|an)\\s+", "");
            if (candidate.length() < 4 || candidate.equalsIgnoreCase(city)) {
                continue;
            }
            streetService.search(candidate).stream()
                    .filter(m -> m.city().equalsIgnoreCase(city))
                    .filter(m -> m.place().equalsIgnoreCase(candidate)
                            || candidate.toLowerCase(Locale.ROOT)
                                    .contains(m.place().toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .ifPresent(out::add);
        }
        return out;
    }

    /** Named on the page so absence isn't mistaken for approval. */
    private List<String> notAssessed() {
        return List.of(
                "Street noise, hotel quality or comfort — no source carries these",
                "Journey times and transfers — no transit data is ingested",
                "Which specific route to walk — RoamSafe has no routing data",
                "Visa, vaccination or insurance requirements — not collected");
    }

    private static boolean mentions(String line, String city) {
        return line.toLowerCase(Locale.ROOT).contains(city.toLowerCase(Locale.ROOT))
                || !line.matches(".*\\b(in|to|at)\\b.*"); // an undated line still belongs to the trip
    }

    private static boolean mentionsWord(String haystack, String needle) {
        if (haystack == null) {
            return false;
        }
        Matcher m = Pattern.compile("\\b" + Pattern.quote(needle) + "\\w*\\b", Pattern.CASE_INSENSITIVE)
                .matcher(haystack);
        return m.find();
    }

    private static int severityRank(String severity) {
        return switch (severity) {
            case "high" -> 0;
            case "medium" -> 1;
            default -> 2;
        };
    }

    private static String capitalise(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
