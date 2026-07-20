package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.SafetyScore;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.AdvisoryRepository;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Side-by-side comparison of destinations.
 *
 * The user story lists cost, nightlife safety, tourist density and public
 * transport quality among the dimensions. None of those exist in any source
 * RoamSafe ingests, and inventing them would make the comparison confidently
 * wrong on exactly the axes a decision turns on. So this compares what the data
 * supports - overall score, concern-by-concern risk, night-time share,
 * neighborhood spread, evidence depth and live government advisories - and the
 * page states which dimensions are missing rather than quietly omitting them.
 */
@Service
public class ComparisonService {

    private final SafetyScoreService safetyScoreService;
    private final ScamReportRepository reportRepository;
    private final DestinationService destinationService;
    private final CityCountryResolver countryResolver;
    private final AdvisoryRepository advisoryRepository;
    private final EmergencyNumberService emergencyNumberService;

    public ComparisonService(SafetyScoreService safetyScoreService,
            ScamReportRepository reportRepository,
            DestinationService destinationService,
            CityCountryResolver countryResolver,
            AdvisoryRepository advisoryRepository,
            EmergencyNumberService emergencyNumberService) {
        this.safetyScoreService = safetyScoreService;
        this.reportRepository = reportRepository;
        this.destinationService = destinationService;
        this.countryResolver = countryResolver;
        this.advisoryRepository = advisoryRepository;
        this.emergencyNumberService = emergencyNumberService;
    }

    public record Column(
            String city, String country, boolean covered,
            Integer score, String scoreColor, String riskLevel,
            int reports, int confidencePct,
            Integer nightSharePct,
            int neighborhoods,
            List<DestinationService.CategoryStat> concerns,
            String advisoryLevel, String advisorySummary, String advisoryUrl,
            String emergencyNumber) {
    }

    /** A dimension where the columns genuinely differ, worth calling out. */
    public record Difference(String label, String detail) {
    }

    public record Comparison(List<Column> columns, List<Difference> differences,
            String verdict, List<String> notCompared) {
    }

    public Comparison compare(List<String> cities) {
        List<Column> columns = new ArrayList<>();
        for (String city : cities) {
            columns.add(column(city.trim()));
        }
        List<Column> covered = columns.stream().filter(Column::covered).toList();
        return new Comparison(columns, differences(covered), verdict(covered), notCompared());
    }

    private Column column(String city) {
        SafetyScore score = safetyScoreService.getScoreForCity(city);
        List<ScamReport> reports = reportRepository
                .findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(city, ScamReportStatus.APPROVED);

        if (score == null || reports.isEmpty()) {
            // Not covered: say so in the column instead of rendering zeros, which
            // would read as "measured and found safe".
            return new Column(city, countryResolver.countryFor(city).orElse(null), false,
                    null, "#8A877D", null, 0, 0, null, 0, List.of(),
                    null, null, null, null);
        }

        String country = countryResolver.countryFor(city).orElse(null);
        var night = destinationService.nightRisk(reports);
        int confidence = score.getConfidenceLevel() != null
                ? (int) Math.round(score.getConfidenceLevel() * 100)
                : 0;

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

        return new Column(city, country, true,
                score.getOverallScore(), scoreColor(score.getOverallScore()),
                riskLevel(score.getOverallScore()),
                reports.size(), confidence,
                night.nightIncidentShare(),
                destinationService.neighborhoodBreakdown(reports).size(),
                destinationService.categoryBreakdown(reports),
                advLevel, advSummary, advUrl,
                country == null ? null
                        : emergencyNumberService.forCountry(country)
                                .map(com.zainab.roamSafe.model.EmergencyNumber::getPrimary)
                                .orElse(null));
    }

    /**
     * Where the columns actually diverge.
     *
     * Only differences large enough to matter are listed. Two cities three
     * points apart are not meaningfully different given how coarse the
     * underlying severity signal is, and presenting that as a distinction would
     * be false precision.
     */
    private List<Difference> differences(List<Column> columns) {
        List<Difference> out = new ArrayList<>();
        if (columns.size() < 2) {
            return out;
        }

        Column best = columns.stream().max(Comparator.comparingInt(c -> c.score() == null ? 0 : c.score())).get();
        Column worst = columns.stream().min(Comparator.comparingInt(c -> c.score() == null ? 0 : c.score())).get();
        if (best.score() != null && worst.score() != null && best.score() - worst.score() >= 5) {
            out.add(new Difference("Overall safety",
                    best.city() + " scores " + best.score() + " against " + worst.city()
                            + "'s " + worst.score() + ", from " + best.reports() + " and "
                            + worst.reports() + " reports respectively."));
        }

        Column night = columns.stream()
                .filter(c -> c.nightSharePct() != null)
                .max(Comparator.comparingInt(Column::nightSharePct)).orElse(null);
        Column day = columns.stream()
                .filter(c -> c.nightSharePct() != null)
                .min(Comparator.comparingInt(Column::nightSharePct)).orElse(null);
        if (night != null && day != null && night != day && night.nightSharePct() - day.nightSharePct() >= 10) {
            out.add(new Difference("After dark",
                    night.nightSharePct() + "% of " + night.city() + "'s reports are night-time incidents, "
                            + "against " + day.nightSharePct() + "% in " + day.city() + "."));
        }

        // Concern-level: where does each city's worst risk sit?
        for (Column c : columns) {
            if (!c.concerns().isEmpty()) {
                var worstConcern = c.concerns().get(0);
                out.add(new Difference(c.city() + ": main concern",
                        worstConcern.category() + ", scoring " + worstConcern.riskScore()
                                + " across " + worstConcern.reports() + " reports."));
            }
        }

        Column evidence = columns.stream().max(Comparator.comparingInt(Column::reports)).get();
        Column thin = columns.stream().min(Comparator.comparingInt(Column::reports)).get();
        if (evidence != thin && evidence.reports() >= thin.reports() * 2) {
            out.add(new Difference("Evidence depth",
                    "We hold " + evidence.reports() + " reports for " + evidence.city() + " but only "
                            + thin.reports() + " for " + thin.city()
                            + ", so the two scores aren't equally well-supported."));
        }
        return out;
    }

    /**
     * A recommendation, hedged to the strength of the evidence.
     *
     * Returns null when the scores are close enough that picking a winner would
     * be noise rather than a finding.
     */
    private String verdict(List<Column> columns) {
        if (columns.size() < 2) {
            return null;
        }
        Column best = columns.stream().max(Comparator.comparingInt(c -> c.score() == null ? 0 : c.score())).get();
        Column worst = columns.stream().min(Comparator.comparingInt(c -> c.score() == null ? 0 : c.score())).get();
        if (best.score() == null || worst.score() == null) {
            return null;
        }
        int gap = best.score() - worst.score();
        if (gap < 5) {
            return "On the evidence RoamSafe holds, these are too close to separate. "
                    + "Both sit within a few points, which is inside the noise of how these scores are computed. "
                    + "Pick on other grounds and read each city's concerns below.";
        }
        String hedge = best.confidencePct() < 50 || best.reports() < 20
                ? " Evidence for " + best.city() + " is still thin, so treat this as a steer rather than a verdict."
                : "";
        return best.city() + " scores better (" + best.score() + " against " + worst.score()
                + "), driven by lower average severity across its reports." + hedge;
    }

    /** Stated openly on the page so its absence isn't mistaken for a low score. */
    private List<String> notCompared() {
        return List.of(
                "Cost of living — no pricing source is ingested",
                "Nightlife safety — not separable from general reports",
                "Public transport quality — only incidents are recorded, not service quality",
                "Tourist density — no visitor-volume source is ingested",
                "Solo-traveler and identity-specific risk — on the roadmap, not yet collected");
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

    private static String scoreColor(Integer score) {
        if (score == null) {
            return "#8A877D";
        }
        if (score >= 85) {
            return "#4F7F4C";
        }
        if (score >= 75) {
            return "#6E9469";
        }
        return score >= 65 ? "#B98A2E" : "#B4553B";
    }
}
