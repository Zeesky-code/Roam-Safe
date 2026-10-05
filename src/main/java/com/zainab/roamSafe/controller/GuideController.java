package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.service.TripGuideService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-stop trip guide, styled to print or save as a PDF.
 *
 * /guide?cities=Paris,Brussels,Amsterdam,Berlin
 *
 * This used to exist twice, as /guide and a near-identical /trip briefing. They
 * were merged here; /trip now redirects so old links keep working.
 */
@Controller
public class GuideController {

    /** Beyond this a guide stops being usable and the page becomes enormous. */
    private static final int MAX_STOPS = 8;

    private final TripGuideService tripGuideService;

    public GuideController(TripGuideService tripGuideService) {
        this.tripGuideService = tripGuideService;
    }

    @GetMapping("/guide")
    public String guide(@RequestParam(required = false) String cities, Model model) {

        model.addAttribute("query", cities);
        List<String> names = parse(cities);
        if (names.isEmpty()) {
            model.addAttribute("needsInput", true);
            return "guide";
        }
        if (names.size() > MAX_STOPS) {
            names = names.subList(0, MAX_STOPS);
            model.addAttribute("trimmed", MAX_STOPS);
        }

        model.addAttribute("guide", tripGuideService.build(names));
        return "guide";
    }

    @GetMapping("/trip")
    public String trip(@RequestParam(required = false) String cities) {
        if (cities == null || cities.isBlank()) {
            return "redirect:/guide";
        }
        return "redirect:" + UriComponentsBuilder.fromPath("/guide")
                .queryParam("cities", cities).encode().toUriString();
    }

    /**
     * Splits a route on commas, newlines or arrows, so "Paris, Brussels",
     * "Paris -> Brussels" and "Paris then Brussels" all work.
     */
    static List<String> parse(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",|\\n|->|→|>|\\bthen\\b")) {
            String name = part.trim();
            if (!name.isEmpty()) {
                out.add(name);
            }
        }
        return out;
    }
}
