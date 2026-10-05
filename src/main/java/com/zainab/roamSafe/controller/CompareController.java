package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.service.ComparisonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    public CompareController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping("/compare")
    public String compare(@RequestParam(required = false) String cities,
            @RequestParam(required = false) String q,
            Model model) {

        List<String> names = parse(cities != null && !cities.isBlank() ? cities : q);
        model.addAttribute("query", cities != null ? cities : q);

        if (names.size() < 2) {
            model.addAttribute("needsInput", true);
            return "compare";
        }
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
