package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.*;
import com.zainab.roamSafe.repository.AdvisoryRepository;
import com.zainab.roamSafe.repository.PracticalInfoRepository;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * A single guide covering several destinations, for a multi-stop trip.
 *
 * Assembles what RoamSafe already holds per city - score, top scams, areas,
 * emergency numbers, official advisory and arrival information - into one
 * document, and is styled to print cleanly so it can be saved as a PDF from the
 * browser. That is deliberately not a generated PDF: a print stylesheet gives
 * the same artefact without adding a rendering dependency, and it stays readable
 * offline, which is the actual requirement behind "offline guide".
 *
 * Embassy contacts and weather appear in the story and are absent here: no
 * source RoamSafe ingests carries either, and a wrong embassy address is the
 * kind of error that matters when someone needs it.
 */
@Service
public class TripGuideService {

    private final ScamReportRepository reportRepository;
    private final SafetyScoreService safetyScoreService;
    private final DestinationService destinationService;
    private final CityCountryResolver countryResolver;
    private final EmergencyNumberService emergencyNumberService;
    private final AdvisoryRepository advisoryRepository;
    private final PracticalInfoRepository practicalInfoRepository;

    public TripGuideService(ScamReportRepository reportRepository,
            SafetyScoreService safetyScoreService,
            DestinationService destinationService,
            CityCountryResolver countryResolver,
            EmergencyNumberService emergencyNumberService,
            AdvisoryRepository advisoryRepository,
            PracticalInfoRepository practicalInfoRepository) {
        this.reportRepository = reportRepository;
        this.safetyScoreService = safetyScoreService;
        this.destinationService = destinationService;
        this.countryResolver = countryResolver;
        this.emergencyNumberService = emergencyNumberService;
        this.advisoryRepository = advisoryRepository;
        this.practicalInfoRepository = practicalInfoRepository;
    }

    public record Stop(String city, String country, boolean covered,
            Integer score, String scoreColor, String riskLevel, int reports,
            String emergencyPrimary, String emergencyDetail,
            String advisoryLevel, String advisorySummary, String advisoryUrl,
            List<ScamReport> topScams,
            List<DestinationService.NeighborhoodTile> areas,
            List<PracticalInfo> practical) {
    }

    public record Guide(List<Stop> stops, List<String> uncovered, List<String> notIncluded) {
    }

    public Guide build(List<String> cities) {
        List<Stop> stops = new ArrayList<>();
        List<String> uncovered = new ArrayList<>();

        for (String raw : cities) {
            String city = raw.trim();
            if (city.isEmpty()) {
                continue;
            }
            List<ScamReport> reports = reportRepository
                    .findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(city, ScamReportStatus.APPROVED);
            String country = countryResolver.countryFor(city).orElse(null);

            if (reports.isEmpty()) {
                // Still worth a stop if we can at least give the emergency
                // number, which is the single most useful thing in a guide.
                var numbers = country == null ? java.util.Optional.<EmergencyNumber>empty()
                        : emergencyNumberService.forCountry(country);
                if (numbers.isEmpty()) {
                    uncovered.add(city);
                    continue;
                }
                stops.add(new Stop(city, country, false, null, "#8A877D", null, 0,
                        numbers.get().getPrimary(), detail(numbers.get()),
                        null, null, null, List.of(), List.of(), List.of()));
                continue;
            }

            SafetyScore score = safetyScoreService.getScoreForCity(city);
            Integer overall = score != null ? score.getOverallScore() : null;

            String advLevel = null, advSummary = null, advUrl = null;
            if (country != null) {
                var advisories = advisoryRepository.findByCountryName(country);
                if (!advisories.isEmpty()) {
                    var a = advisories.get(0);
                    advLevel = a.getLevel();
                    advSummary = a.getSummary();
                    advUrl = a.getUrl();
                }
            }

            var numbers = country == null ? java.util.Optional.<EmergencyNumber>empty()
                    : emergencyNumberService.forCountry(country);

            stops.add(new Stop(city, country, true,
                    overall,
                    overall != null ? scoreColor(overall) : "#8A877D",
                    riskLevel(overall),
                    reports.size(),
                    numbers.map(EmergencyNumber::getPrimary).orElse(null),
                    numbers.map(TripGuideService::detail).orElse(null),
                    advLevel, advSummary, advUrl,
                    reports.stream().limit(5).toList(),
                    destinationService.neighborhoodBreakdown(reports).stream().limit(6).toList(),
                    practicalInfoRepository.findByCityNameIgnoreCase(city)));
        }

        return new Guide(stops, uncovered, List.of(
                "Embassy and consulate contacts — not collected; check your government's site",
                "Weather and seasonal conditions — no meteorological source is ingested",
                "Visa requirements — not collected; check the official source for your passport",
                "Accommodation and booking — RoamSafe does not hold these"));
    }

    /** The service-by-service list, or null when only one number is known. */
    private static String detail(EmergencyNumber e) {
        List<String> parts = new ArrayList<>();
        if (e.getPolice() != null) {
            parts.add("police " + e.getPolice());
        }
        if (e.getAmbulance() != null) {
            parts.add("ambulance " + e.getAmbulance());
        }
        if (e.getFire() != null) {
            parts.add("fire " + e.getFire());
        }
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    private static String riskLevel(Integer score) {
        if (score == null) {
            return null;
        }
        if (score >= 70) {
            return "LOW";
        }
        if (score >= 55) {
            return "MODERATE";
        }
        return score >= 40 ? "ELEVATED" : "HIGH";
    }

    private static String scoreColor(int score) {
        if (score >= 85) {
            return "#4F7F4C";
        }
        if (score >= 75) {
            return "#6E9469";
        }
        return score >= 65 ? "#B98A2E" : "#B4553B";
    }
}
