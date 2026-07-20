package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.service.SearchQuotaService;
import com.zainab.roamSafe.service.TripGuideService;
import jakarta.servlet.http.HttpSession;
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
    private final SearchQuotaService searchQuota;

    public GuideController(TripGuideService tripGuideService, SearchQuotaService searchQuota) {
        this.tripGuideService = tripGuideService;
        this.searchQuota = searchQuota;
    }

    @GetMapping("/guide")
    public String guide(@RequestParam(required = false) String cities,
            HttpSession session, Model model) {

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

        User user = (User) session.getAttribute("user");
        if (!searchQuota.allow(session, user, "guide:" + String.join(",", names))) {
            return "redirect:/pricing?limit=search";
        }

        model.addAttribute("guide", tripGuideService.build(names));
        return "guide";
    }
}
