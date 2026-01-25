package com.zainab.roamSafe.controller;

import java.util.List;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import com.zainab.roamSafe.model.User;

@Controller
@RequestMapping("/scams")
public class ScamController {

    @Autowired
    private ScamReportRepository scamReportRepository;

    @GetMapping
    public String showScamsPage(@RequestParam(required = false) String city,
            Model model,
            HttpSession session) {
        if (city != null && !city.isEmpty()) {
            // Query from the new scam_reports table (populated by LLM seeder)
            List<ScamReport> scams = scamReportRepository
                    .findByCityIgnoreCaseAndStatusOrderBySeverityScoreDesc(city, ScamReportStatus.APPROVED);

            // Check if user is logged in
            User user = (User) session.getAttribute("user");
            boolean isLoggedIn = user != null;

            int totalScams = scams.size();

            if (!isLoggedIn && scams.size() > 3) {
                // Show only first 3 scams for non-logged-in users
                scams = scams.subList(0, 3);
                model.addAttribute("showLoginPrompt", true);
                model.addAttribute("totalScams", totalScams);
            }

            model.addAttribute("scams", scams);
            model.addAttribute("selectedCity", city);
            model.addAttribute("isLoggedIn", isLoggedIn);
        }

        return "scams";
    }
}
