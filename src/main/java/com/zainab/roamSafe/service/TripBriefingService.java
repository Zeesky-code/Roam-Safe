package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.Advisory;
import com.zainab.roamSafe.model.EmergencyNumber;
import com.zainab.roamSafe.model.PracticalInfo;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.repository.AdvisoryRepository;
import com.zainab.roamSafe.repository.PracticalInfoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * US6 (backpacker): a multi-city trip - "Paris, Brussels, Amsterdam, Berlin" -
 * turned into one stacked, printable briefing. It does not invent anything: each
 * leg is assembled from the same sourced data the single-city destination page
 * already uses (score, top scams, emergency numbers, official advisory, arrival
 * and transport notes). Where a real source is missing for a leg, that section
 * is simply absent rather than filled with a guess.
 */
@Service
public class TripBriefingService {

    /** How many cities one briefing will build, to keep the page and queries sane. */
    public static final int MAX_LEGS = 8;

    private final ScamService scamService;
    private final DestinationService destinationService;
    private final CityCountryResolver cityCountryResolver;
    private final EmergencyNumberService emergencyNumberService;
    private final AdvisoryRepository advisoryRepository;
    private final PracticalInfoRepository practicalInfoRepository;

    public TripBriefingService(ScamService scamService,
            DestinationService destinationService,
            CityCountryResolver cityCountryResolver,
            EmergencyNumberService emergencyNumberService,
            AdvisoryRepository advisoryRepository,
            PracticalInfoRepository practicalInfoRepository) {
        this.scamService = scamService;
        this.destinationService = destinationService;
        this.cityCountryResolver = cityCountryResolver;
        this.emergencyNumberService = emergencyNumberService;
        this.advisoryRepository = advisoryRepository;
        this.practicalInfoRepository = practicalInfoRepository;
    }

    /** A single scam line, kept small for the briefing. */
    public record ScamLine(String name, String category, int severity) {
    }

    /** One city's leg of the trip. Any field may be null/empty when unsourced. */
    public record Leg(
            String city,
            String country,
            Integer score,
            String scoreColor,
            int reports,
            List<ScamLine> topScams,
            EmergencyNumber emergency,
            Advisory advisory,
            List<PracticalInfo> transport) {

        public boolean hasScore() {
            return score != null;
        }
    }

    /** The whole trip: the legs we could build, and the names we couldn't. */
    public record Briefing(List<Leg> legs, List<String> notCovered) {

        public boolean isEmpty() {
            return legs.isEmpty();
        }
    }

    public Briefing build(List<String> cities) {
        List<Leg> legs = new ArrayList<>();
        List<String> notCovered = new ArrayList<>();

        for (String raw : cities) {
            String city = raw == null ? "" : raw.trim();
            if (city.isEmpty()) {
                continue;
            }

            List<ScamReport> reports = scamService.getReportsByCity(city);
            String country = cityCountryResolver.countryFor(city)
                    .orElseGet(() -> CountryLookup.forCity(city)
                            .map(CountryLookup.Country::name)
                            .orElse(null));

            // A leg we genuinely have nothing sourced for: name it honestly in the
            // "not covered" list instead of rendering an empty card.
            if (reports.isEmpty() && country == null) {
                notCovered.add(city);
                continue;
            }

            DestinationService.View view = destinationService.build(city, reports);

            List<ScamLine> topScams = reports.stream()
                    .sorted(Comparator.comparingInt(ScamReport::getSeverityScore).reversed())
                    .limit(3)
                    .map(r -> new ScamLine(r.getName(), r.getCategory(), r.getSeverityScore()))
                    .toList();

            EmergencyNumber emergency = country == null ? null
                    : emergencyNumberService.forCountry(country).orElse(null);

            Advisory advisory = null;
            if (country != null) {
                List<Advisory> advisories = advisoryRepository.findByCountryName(country);
                if (!advisories.isEmpty()) {
                    advisory = advisories.get(0);
                }
            }

            List<PracticalInfo> transport = practicalInfoRepository
                    .findByCityNameIgnoreCase(city).stream()
                    .limit(3)
                    .toList();

            legs.add(new Leg(city, country, view.score(), view.scoreColor(),
                    view.reports(), topScams, emergency, advisory, transport));
        }

        return new Briefing(legs, notCovered);
    }
}
