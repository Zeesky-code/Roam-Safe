package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.service.ComparisonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Side-by-side destination comparison.
 *
 * Accepts /compare?cities=Barcelona,Lisbon and also the phrasing from the user
 * story, /compare?q=Barcelona vs Lisbon, so the same URL works whether it comes
 * from a form or from someone typing what they mean.
 */
@Controller
public class CompareController {

    /** Comparing more than this stops being a comparison and becomes a list. */
    private static final int MAX_CITIES = 4;

    private final ComparisonService comparisonService;
    private final com.zainab.roamSafe.service.SearchQuotaService searchQuota;

    public CompareController(ComparisonService comparisonService,
            com.zainab.roamSafe.service.SearchQuotaService searchQuota) {
        this.comparisonService = comparisonService;
        this.searchQuota = searchQuota;
    }

    @GetMapping("/compare")
    public String compare(@RequestParam(required = false) String cities,
            @RequestParam(required = false) String q,
            HttpSession session,
            Model model) {

        List<String> names = parse(cities != null && !cities.isBlank() ? cities : q);
        model.addAttribute("query", cities != null ? cities : q);

        if (names.size() < 2) {
            model.addAttribute("needsInput", true);
            return "compare";
        }
        var user = (com.zainab.roamSafe.model.User) session.getAttribute("user");
        if (!searchQuota.allow(session, user, "compare:" + String.join(",", names))) {
            return "redirect:/pricing?limit=search";
        }
        model.addAttribute("searchesLeft", searchQuota.remaining(session, user));
        if (names.size() > MAX_CITIES) {
            names = names.subList(0, MAX_CITIES);
            model.addAttribute("trimmed", true);
        }
        model.addAttribute("comparison", comparisonService.compare(names));
        return "compare";
    }

    /** Splits on commas or the word "vs", so both input styles work. */
    private static List<String> parse(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split("(?i),|\\bvs\\.?\\b|\\bversus\\b")) {
            String name = part.trim();
            if (!name.isEmpty()) {
                out.add(name);
            }
        }
        return out;
    }
}
