package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.service.SearchQuotaService;
import com.zainab.roamSafe.service.TripReviewService;
import jakarta.servlet.http.HttpSession;
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
    private final SearchQuotaService searchQuota;

    public ReviewController(TripReviewService tripReviewService, SearchQuotaService searchQuota) {
        this.tripReviewService = tripReviewService;
        this.searchQuota = searchQuota;
    }

    @GetMapping("/review")
    public String form() {
        return "review";
    }

    @PostMapping("/review")
    public String review(@RequestParam String itinerary, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (!searchQuota.allow(session, user, "review:" + itinerary.hashCode())) {
            return "redirect:/pricing?limit=search";
        }
        model.addAttribute("review", tripReviewService.review(itinerary));
        return "review";
    }
}
