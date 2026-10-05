package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.service.TripReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Itinerary review. Paste a plan, get findings backed by real reports.
 */
@Controller
public class ReviewController {

    private final TripReviewService tripReviewService;

    public ReviewController(TripReviewService tripReviewService) {
        this.tripReviewService = tripReviewService;
    }

    @GetMapping("/review")
    public String form() {
        return "review";
    }

    @PostMapping("/review")
    public String review(@RequestParam String itinerary, Model model) {
        model.addAttribute("review", tripReviewService.review(itinerary));
        return "review";
    }
}
