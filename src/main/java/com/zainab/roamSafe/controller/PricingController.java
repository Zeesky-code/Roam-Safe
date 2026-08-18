package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
public class PricingController {

    @GetMapping("/pricing")
    public String showPricingPage(
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String canceled,
            @RequestParam(name = "already_pro", required = false) String alreadyPro,
            @RequestParam(required = false) String locked,
            Model model,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isPro", isLoggedIn && user.isPro());

        // Arriving from a gated feature: name the feature, so the page answers
        // "why am I here" rather than showing a generic wall.
        if (locked != null && !locked.isBlank()) {
            model.addAttribute("lockedFeature", switch (locked) {
                case "street" -> "Street intelligence";
                case "compare" -> "Destination comparison";
                case "trip" -> "The multi-city trip briefing";
                case "guide" -> "The multi-stop guide";
                case "review" -> "Itinerary review";
                default -> "That feature";
            });
        }

        if ("true".equals(success)) {
            model.addAttribute("successMessage", "Welcome to Nomad! Your subscription is now active.");
        }
        if ("true".equals(canceled)) {
            model.addAttribute("cancelMessage", "Payment was canceled. You can try again anytime.");
        }
        if ("true".equals(alreadyPro)) {
            model.addAttribute("infoMessage", "You're already a Pro member!");
        }

        model.addAttribute("proPlan", isLoggedIn ? user.getProPlan() : null);
        return "pricing";
    }
}
