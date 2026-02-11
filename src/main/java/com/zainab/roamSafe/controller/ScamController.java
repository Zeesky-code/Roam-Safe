package com.zainab.roamSafe.controller;

import java.util.List;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.service.ScamService;
import com.zainab.roamSafe.model.User;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/scams")
public class ScamController {

    private final ScamService scamService;

    public ScamController(ScamService scamService) {
        this.scamService = scamService;
    }

    @GetMapping
    public String showScamsPage(@RequestParam(required = false) String city,
            Model model,
            HttpSession session) {

        // Always pass user login status
        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = user != null;
        model.addAttribute("isLoggedIn", isLoggedIn);

        if (city != null && !city.isEmpty()) {
            List<ScamReport> scams = scamService.getReportsByCity(city);

            int totalScams = scams.size();

            if (!isLoggedIn && scams.size() > 3) {
                // Show only first 3 scams for non-logged-in users
                scams = scams.subList(0, 3);
                model.addAttribute("showLoginPrompt", true);
                model.addAttribute("totalScams", totalScams);
            }

            model.addAttribute("scams", scams);
            model.addAttribute("selectedCity", city);
        } else {
            // Dashboard mode
            List<Object[]> topCities = scamService.getTopCities(6);
            List<ScamReport> recentReports = scamService.getRecentReports(5);

            model.addAttribute("topCities", topCities);
            model.addAttribute("recentReports", recentReports);
            model.addAttribute("selectedCity", null);
        }

        return "scams";
    }
}
