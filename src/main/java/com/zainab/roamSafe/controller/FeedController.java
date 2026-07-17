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
 * Live intelligence feed — a filterable stream of the most recent approved
 * reports. Real data only: each signal is a genuine report. Filtering is done
 * client-side over the rendered list.
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
                    relativeTime(r.getCreatedAt()),
                    "/scams?city=" + r.getCity()));
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
        if (when == null)
            return "recently";
        long mins = Duration.between(when, LocalDateTime.now()).toMinutes();
        if (mins < 60)
            return Math.max(1, mins) + "m ago";
        long hours = mins / 60;
        if (hours < 24)
            return hours + "h ago";
        return (hours / 24) + "d ago";
    }
}
