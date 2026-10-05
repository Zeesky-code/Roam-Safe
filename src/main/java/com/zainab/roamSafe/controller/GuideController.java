package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.service.TripGuideService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

/**
 * Multi-stop trip guide, styled to print or save as a PDF.
 *
 * /guide?cities=Paris,Brussels,Amsterdam,Berlin
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
        if (cities == null || cities.isBlank()) {
            model.addAttribute("needsInput", true);
            return "guide";
        }

        List<String> names = Arrays.stream(cities.split("[,\\n]"))
                .map(String::trim).filter(s -> !s.isEmpty()).limit(MAX_STOPS).toList();
        if (names.isEmpty()) {
            model.addAttribute("needsInput", true);
            return "guide";
        }


        model.addAttribute("guide", tripGuideService.build(names));
        return "guide";
    }
}
