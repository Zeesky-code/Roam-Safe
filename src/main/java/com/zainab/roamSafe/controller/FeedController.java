package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.service.ScamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Latest reports - a filterable stream of the most recently added approved
 * reports. Real data only: each signal is a genuine report. Filtering is done
 * client-side over the rendered list.
 *
 * Not billed as "live": these are evergreen library entries ordered by when they
 * were ingested, and most carry no incident date. Current events come from GDELT
 * and appear on each city's "Happening now" tab.
 */
@Controller
public class FeedController {

    private final ScamService scamService;

    public FeedController(ScamService scamService) {
        this.scamService = scamService;
    }

    public record Signal(
            String category,
            String severity,
            String city,
            String title,
            String description,
            String time,
            String source,
            String href) {
    }

    @GetMapping("/feed")
    public String feed(Model model) {
        List<ScamReport> reports = scamService.getRecentApproved(40);
        List<Signal> signals = new ArrayList<>();
        for (ScamReport r : reports) {
            signals.add(new Signal(
                    r.getCategory() != null && !r.getCategory().isBlank() ? r.getCategory() : "General",
                    severityOf(r.getSeverityScore()),
                    r.getCity(),
                    r.getName(),
                    r.getDescription(),
                    relativeTime(r.getReportedAt()),
                    r.getSourceName() == null || r.getSourceName().isBlank() ? null : r.getSourceName(),
                    "/scams?city=" + org.springframework.web.util.UriUtils.encodeQueryParam(r.getCity(), java.nio.charset.StandardCharsets.UTF_8)));
        }
        model.addAttribute("signals", signals);
        model.addAttribute("signalCount", signals.size());
        // Distinct categories present, for the filter chips.
        List<String> categories = signals.stream().map(Signal::category).distinct().sorted().toList();
        model.addAttribute("categories", categories);
        return "feed";
    }

    private static String severityOf(Integer sev) {
        int s = sev == null ? 5 : sev;
        if (s >= 8)
            return "critical";
        if (s >= 6)
            return "high";
        if (s >= 4)
            return "medium";
        return "low";
    }

    private static String relativeTime(LocalDateTime when) {
        // "recently" was a guess. Undated sources say so.
        if (when == null)
            return "date unknown";
        long mins = Duration.between(when, LocalDateTime.now()).toMinutes();
        if (mins < 60)
            return Math.max(1, mins) + "m ago";
        long hours = mins / 60;
        if (hours < 24)
            return hours + "h ago";
        return (hours / 24) + "d ago";
    }
}
