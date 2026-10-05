package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.service.TripBriefingService;
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

    public TripController(TripBriefingService tripBriefingService) {
        this.tripBriefingService = tripBriefingService;
    }

    @GetMapping("/trip")
    public String trip(@RequestParam(required = false) String cities,
            Model model) {

        List<String> names = parse(cities);
        model.addAttribute("query", cities);

        if (names.isEmpty()) {
            model.addAttribute("needsInput", true);
            return "trip";
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
