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
            Model model,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isPro", isLoggedIn && user.isPro());

        if ("true".equals(success)) {
            model.addAttribute("successMessage", "Welcome to Traveler Pro! Your subscription is now active.");
        }
        if ("true".equals(canceled)) {
            model.addAttribute("cancelMessage", "Payment was canceled. You can try again anytime.");
        }
        if ("true".equals(alreadyPro)) {
            model.addAttribute("infoMessage", "You're already a Pro member!");
        }

        return "pricing";
    }
}
