package com.zainab.roamSafe.controller;

import java.util.List;
import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.service.ScamService;
import com.zainab.roamSafe.service.DestinationService;
import com.zainab.roamSafe.service.CountryLookup;
import com.zainab.roamSafe.repository.AdvisoryRepository;
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
    private final DestinationService destinationService;
    private final AdvisoryRepository advisoryRepository;

    public ScamController(ScamService scamService, DestinationService destinationService,
            AdvisoryRepository advisoryRepository) {
        this.scamService = scamService;
        this.destinationService = destinationService;
        this.advisoryRepository = advisoryRepository;
    }

    @GetMapping
    public String showScamsPage(@RequestParam(required = false) String city,
            Model model,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = user != null;
        boolean isPro = isLoggedIn && user.isPro();
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isPro", isPro);

        if (city != null && !city.isEmpty()) {
            // --- Destination report for one city ---
            List<ScamReport> allScams = scamService.getReportsByCity(city);
            int totalScams = allScams.size();

            // Stats/score use the full list; the card list is paywalled.
            model.addAttribute("destination", destinationService.build(city, allScams));

            // Official government advisories for this city's country (real, sourced).
            CountryLookup.forCity(city).ifPresent(country -> {
                model.addAttribute("country", country.name());
                List<com.zainab.roamSafe.model.Advisory> advisories = advisoryRepository
                        .findByCountryName(country.name());
                if (!advisories.isEmpty()) {
                    model.addAttribute("advisories", advisories);
                }
            });

            List<ScamReport> visible = allScams;
            if (!isPro && totalScams > 3) {
                visible = allScams.subList(0, 3);
                model.addAttribute(isLoggedIn ? "showUpgradePrompt" : "showLoginPrompt", true);
            }
            model.addAttribute("scams", visible);
            model.addAttribute("totalScams", totalScams);
            model.addAttribute("selectedCity", city);
            return "destination";
        }

        // --- Scam library (no city) ---
        model.addAttribute("topCities", scamService.getTopCities(8));
        model.addAttribute("recentReports", scamService.getRecentReports(5));
        model.addAttribute("libraryReports", scamService.getRecentReports(12));
        model.addAttribute("selectedCity", null);
        return "scams";
    }
}
