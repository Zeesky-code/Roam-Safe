package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.*;
import com.zainab.roamSafe.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the dashboard from what the user actually told us and what the data
 * actually supports.
 *
 * Several figures in the original design have no source and are deliberately
 * absent rather than approximated: a "trip readiness" percentage with no
 * definition, "new alerts today" when no report carries a real date, and
 * relative timestamps on undated imports. Where a real equivalent exists it is
 * used instead - coverage confidence is a genuine measure and replaces trip
 * readiness.
 */
@Service
public class DashboardService {

    private final WatchedCityRepository watchedCityRepository;
    private final TripRepository tripRepository;
    private final SafetyScoreService safetyScoreService;
    private final ScoreSnapshotService snapshotService;
    private final ScamReportRepository scamReportRepository;
    private final CitySummaryService citySummaryService;
    private final CityRepository cityRepository;

    public DashboardService(WatchedCityRepository watchedCityRepository,
            TripRepository tripRepository,
            SafetyScoreService safetyScoreService,
            ScoreSnapshotService snapshotService,
            ScamReportRepository scamReportRepository,
            CitySummaryService citySummaryService,
            CityRepository cityRepository) {
        this.watchedCityRepository = watchedCityRepository;
        this.tripRepository = tripRepository;
        this.safetyScoreService = safetyScoreService;
        this.snapshotService = snapshotService;
        this.scamReportRepository = scamReportRepository;
        this.citySummaryService = citySummaryService;
        this.cityRepository = cityRepository;
    }

    /**
     * A watched destination. {@code change} and {@code trend} are null/empty
     * until enough daily snapshots exist, and the template must show that state
     * rather than drawing a flat line implying no movement.
     */
    public record WatchedTile(String city, String country, Integer score, String color,
            Integer change, List<Integer> trend, int reports) {

        public boolean hasTrend() {
            return trend != null && trend.size() >= 2;
        }

        public String changeLabel() {
            if (change == null) {
                return null;
            }
            return change > 0 ? "+" + change : String.valueOf(change);
        }
    }

    /** The next trip, with the real numbers behind its destination. */
    public record TripCard(Long id, String city, String country, String dateRange, long nights,
            long daysUntil, Integer score, String color, Integer confidencePct, String summary) {
    }

    /** A recent report in a watched destination. No invented timestamps. */
    public record Signal(String city, String title, String category, String severity, String when) {
    }

    public record View(String greeting, String today, String subtitle,
            TripCard trip, List<WatchedTile> watched, List<Signal> signals,
            int watchedCount, int reportsInWatched, Integer avgConfidence,
            int libraryCount) {
    }

    public View build(User user) {
        List<WatchedCity> watchedRows = watchedCityRepository.findByUserIdOrderByAddedAtDesc(user.getId());
        List<WatchedTile> watched = new ArrayList<>();
        int reportsInWatched = 0;
        int confidenceSum = 0, confidenceCount = 0;

        for (WatchedCity row : watchedRows) {
            SafetyScore score = safetyScoreService.getScoreForCity(row.getCityName());
            List<ScamReport> reports = scamReportRepository
                    .findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(row.getCityName(),
                            ScamReportStatus.APPROVED);
            reportsInWatched += reports.size();

            City city = score != null ? score.getCity() : cityRepository.findFirstByName(row.getCityName());
            Integer overall = score != null ? score.getOverallScore() : null;
            if (score != null && score.getConfidenceLevel() != null) {
                confidenceSum += (int) Math.round(score.getConfidenceLevel() * 100);
                confidenceCount++;
            }

            watched.add(new WatchedTile(
                    row.getCityName(),
                    city != null ? city.getCountry() : null,
                    overall,
                    overall != null ? scoreColor(overall) : "#8A877D",
                    city != null ? snapshotService.changeFor(city) : null,
                    city != null ? snapshotService.trendFor(city) : List.of(),
                    reports.size()));
        }

        return new View(
                greeting(),
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMMM"))
                        .toUpperCase(),
                subtitle(watched.size()),
                nextTrip(user),
                watched,
                signals(watchedRows),
                watched.size(),
                reportsInWatched,
                confidenceCount > 0 ? confidenceSum / confidenceCount : null,
                (int) scamReportRepository.count());
    }

    private TripCard nextTrip(User user) {
        List<Trip> upcoming = tripRepository
                .findByUserIdAndEndDateGreaterThanEqualOrderByStartDateAsc(user.getId(), java.time.LocalDate.now());
        if (upcoming.isEmpty()) {
            return null;
        }
        Trip trip = upcoming.get(0);
        SafetyScore score = safetyScoreService.getScoreForCity(trip.getCityName());
        City city = score != null ? score.getCity() : cityRepository.findFirstByName(trip.getCityName());
        CitySummary summary = citySummaryService.getSummaryForCity(trip.getCityName());
        Integer overall = score != null ? score.getOverallScore() : null;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM");
        String range = trip.getStartDate().format(fmt) + " – " + trip.getEndDate().format(fmt)
                + ", " + trip.getEndDate().getYear();

        return new TripCard(
                trip.getId(),
                trip.getCityName(),
                city != null ? city.getCountry() : null,
                range,
                trip.getNights(),
                trip.getDaysUntil(),
                overall,
                overall != null ? scoreColor(overall) : "#8A877D",
                score != null && score.getConfidenceLevel() != null
                        ? (int) Math.round(score.getConfidenceLevel() * 100)
                        : null,
                realSummary(summary));
    }

    /**
     * Newest reports across watched destinations.
     *
     * The design showed relative times ("26m ago"), but no imported report
     * carries a real incident date, so the time column says so instead of
     * dressing an import up as breaking news.
     */
    private List<Signal> signals(List<WatchedCity> watchedRows) {
        List<Signal> signals = new ArrayList<>();
        for (WatchedCity row : watchedRows) {
            List<ScamReport> reports = scamReportRepository
                    .findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(row.getCityName(),
                            ScamReportStatus.APPROVED);
            reports.stream().limit(2).forEach(r -> signals.add(new Signal(
                    r.getCity(),
                    r.getName(),
                    r.getCategory() != null && !r.getCategory().isBlank() ? r.getCategory() : "General",
                    severityOf(r.getSeverityScore()),
                    r.getReportedAt() != null
                            ? r.getReportedAt().toLocalDate().toString()
                            : "date unknown")));
        }
        return signals.stream().limit(6).toList();
    }

    /**
     * The summary text only when a real one was written.
     *
     * CitySummaryService hands back a "No summary available for this city yet"
     * placeholder rather than null, which the dashboard was rendering in the
     * position where analysis belongs - so an absence of content looked like
     * content. Treat the placeholder as the nothing it is and let the template
     * show its own empty state.
     */
    private static String realSummary(CitySummary summary) {
        if (summary == null || summary.getSummaryText() == null) {
            return null;
        }
        String text = summary.getSummaryText().trim();
        if (text.isEmpty() || text.startsWith("No summary available")) {
            return null;
        }
        return text;
    }

    private static String subtitle(int watchedCount) {
        if (watchedCount == 0) {
            return "Add a destination to start tracking its safety score.";
        }
        return "Tracking " + watchedCount + (watchedCount == 1 ? " destination." : " destinations.");
    }

    private static String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return "Good morning";
        }
        return hour < 18 ? "Good afternoon" : "Good evening";
    }

    private static String severityOf(Integer sev) {
        int s = sev == null ? 5 : sev;
        if (s >= 8) {
            return "critical";
        }
        if (s >= 6) {
            return "high";
        }
        return s >= 4 ? "medium" : "low";
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
