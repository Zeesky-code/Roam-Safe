package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.City;
import com.zainab.roamSafe.model.SafetyScore;
import com.zainab.roamSafe.model.SafetyScoreHistory;
import com.zainab.roamSafe.repository.SafetyScoreHistoryRepository;
import com.zainab.roamSafe.repository.SafetyScoreRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Records one score snapshot per city per day.
 *
 * The score history table and its repository already existed but nothing ever
 * wrote to them, so every trend view had zero rows behind it. Trends are the one
 * thing that cannot be backfilled - a score not recorded on a given day is gone
 * for good - so this runs from now on and the history fills in as days pass.
 * Expect an empty trend for roughly the first week; that is the honest state,
 * not a bug.
 */
@Service
public class ScoreSnapshotService {

    private final SafetyScoreRepository safetyScoreRepository;
    private final SafetyScoreHistoryRepository historyRepository;

    public ScoreSnapshotService(SafetyScoreRepository safetyScoreRepository,
            SafetyScoreHistoryRepository historyRepository) {
        this.safetyScoreRepository = safetyScoreRepository;
        this.historyRepository = historyRepository;
    }

    /** How many snapshots a sparkline shows. */
    public static final int TREND_POINTS = 14;

    /**
     * Daily, first run 15 minutes after boot so it never competes with startup.
     * Snapshotting more often than daily would only add points that say the same
     * thing, since the underlying reports change far more slowly than that.
     */
    @Scheduled(fixedRate = 24L * 60 * 60 * 1000, initialDelay = 900_000)
    public synchronized int snapshotAll() {
        List<SafetyScore> scores = safetyScoreRepository.findAll();
        int written = 0;
        for (SafetyScore score : scores) {
            if (snapshot(score)) {
                written++;
            }
        }
        System.out.println("[snapshot] Recorded " + written + " score snapshots of " + scores.size() + " cities.");
        return written;
    }

    /**
     * Writes today's snapshot for one city, carrying the previous values across
     * so a row is self-contained. Returns false when today is already recorded -
     * re-running is safe and won't stack duplicate points onto a sparkline.
     */
    private boolean snapshot(SafetyScore score) {
        City city = score.getCity();
        if (city == null || score.getOverallScore() == null) {
            return false;
        }

        SafetyScoreHistory latest = historyRepository.findMostRecentByCity(city);
        if (latest != null && latest.getCalculatedAt() != null
                && latest.getCalculatedAt().toLocalDate().equals(LocalDate.now())) {
            return false;
        }

        SafetyScoreHistory row = new SafetyScoreHistory();
        row.setCity(city);
        row.setNewScore(score.getOverallScore());
        row.setNewFinancialScore(orZero(score.getFinancialRiskScore()));
        row.setNewPhysicalScore(orZero(score.getPhysicalRiskScore()));
        row.setNewDigitalScore(orZero(score.getDigitalRiskScore()));
        if (latest != null) {
            row.setPreviousScore(latest.getNewScore());
            row.setPreviousFinancialScore(latest.getNewFinancialScore());
            row.setPreviousPhysicalScore(latest.getNewPhysicalScore());
            row.setPreviousDigitalScore(latest.getNewDigitalScore());
        }
        row.setCalculatedAt(LocalDateTime.now());
        row.setChangeReason("Daily snapshot");
        historyRepository.save(row);
        return true;
    }

    /**
     * Recent snapshots for a city, oldest first, ready to plot. Returns fewer
     * points than requested - possibly none - when the history is still short,
     * and callers must render that rather than inventing a flat line.
     */
    public List<Integer> trendFor(City city) {
        List<SafetyScoreHistory> rows = historyRepository.findByCityOrderByCalculatedAtDesc(city);
        return rows.stream()
                .limit(TREND_POINTS)
                .map(SafetyScoreHistory::getNewScore)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(), list -> {
                            java.util.Collections.reverse(list); // oldest first
                            return list;
                        }));
    }

    /**
     * Change since the previous snapshot, or null when there isn't one yet. Null
     * means "no basis for comparison" and must not be shown as a zero change.
     */
    public Integer changeFor(City city) {
        SafetyScoreHistory latest = historyRepository.findMostRecentByCity(city);
        if (latest == null || latest.getPreviousScore() == null || latest.getNewScore() == null) {
            return null;
        }
        return latest.getNewScore() - latest.getPreviousScore();
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
