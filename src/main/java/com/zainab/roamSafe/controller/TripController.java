package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.service.SearchQuotaService;
import com.zainab.roamSafe.service.TripBriefingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * US6 (backpacker): a multi-city trip briefing. "Paris, Brussels, Amsterdam,
 * Berlin" becomes one stacked, printable page - each leg carrying its own score,
 * scams, emergency numbers, advisory and transport notes, all from real sources.
 * The page prints to PDF straight from the browser (the "offline guide" beat).
 */
@Controller
public class TripController {

    private final TripBriefingService tripBriefingService;
    private final SearchQuotaService searchQuota;

    public TripController(TripBriefingService tripBriefingService, SearchQuotaService searchQuota) {
        this.tripBriefingService = tripBriefingService;
        this.searchQuota = searchQuota;
    }

    @GetMapping("/trip")
    public String trip(@RequestParam(required = false) String cities,
            HttpSession session,
            Model model) {

        List<String> names = parse(cities);
        model.addAttribute("query", cities);

        if (names.isEmpty()) {
            model.addAttribute("needsInput", true);
            return "trip";
        }

        User user = (User) session.getAttribute("user");
        // The multi-city briefing and its offline PDF are Trip Pass features.
        if (!searchQuota.hasProAccess(user)) {
            return "redirect:/pricing?locked=trip";
        }

        if (names.size() > TripBriefingService.MAX_LEGS) {
            names = names.subList(0, TripBriefingService.MAX_LEGS);
            model.addAttribute("trimmed", true);
        }

        model.addAttribute("briefing", tripBriefingService.build(names));
        return "trip";
    }

    /** Splits the route on commas or arrows, so "Paris, Brussels" and "Paris > Brussels" both work. */
    private static List<String> parse(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",|->|→|>|\\bthen\\b")) {
            String name = part.trim();
            if (!name.isEmpty()) {
                out.add(name);
            }
        }
        return out;
    }
}
