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
    private final com.zainab.roamSafe.service.EmergencyNumberService emergencyNumberService;
    private final com.zainab.roamSafe.service.CityCountryResolver cityCountryResolver;
    private final com.zainab.roamSafe.service.ScamLookupService scamLookupService;
    private final com.zainab.roamSafe.service.SearchQuotaService searchQuota;
    private final com.zainab.roamSafe.repository.LiveIncidentRepository liveIncidentRepository;
    private final com.zainab.roamSafe.repository.PracticalInfoRepository practicalInfoRepository;

    public ScamController(ScamService scamService, DestinationService destinationService,
            AdvisoryRepository advisoryRepository,
            com.zainab.roamSafe.service.EmergencyNumberService emergencyNumberService,
            com.zainab.roamSafe.service.CityCountryResolver cityCountryResolver,
            com.zainab.roamSafe.service.ScamLookupService scamLookupService,
            com.zainab.roamSafe.service.SearchQuotaService searchQuota,
            com.zainab.roamSafe.repository.LiveIncidentRepository liveIncidentRepository,
            com.zainab.roamSafe.repository.PracticalInfoRepository practicalInfoRepository) {
        this.practicalInfoRepository = practicalInfoRepository;
        this.liveIncidentRepository = liveIncidentRepository;
        this.scamLookupService = scamLookupService;
        this.searchQuota = searchQuota;
        this.scamService = scamService;
        this.destinationService = destinationService;
        this.advisoryRepository = advisoryRepository;
        this.emergencyNumberService = emergencyNumberService;
        this.cityCountryResolver = cityCountryResolver;
    }

    @GetMapping
    public String showScamsPage(@RequestParam(required = false) String city,
            @RequestParam(required = false) String q,
            Model model,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = user != null;
        boolean isPro = isLoggedIn && user.isPro();
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isPro", isPro);

        // Named-scam lookup: "bracelet scam Paris". Handled before the city
        // branch so a query mentioning a city still searches the scam, rather
        // than falling through to that city's whole report list.
        if (q != null && !q.isBlank()) {
            if (!searchQuota.allow(session, user, "scam:" + q)) {
                return "redirect:/pricing?limit=search";
            }
            model.addAttribute("lookup", scamLookupService.lookup(q));
            model.addAttribute("searchesLeft", searchQuota.remaining(session, user));
            return "scam-lookup";
        }

        if (city != null && !city.isEmpty()) {
            if (!searchQuota.allow(session, user, "city:" + city)) {
                return "redirect:/pricing?limit=search";
            }
            model.addAttribute("searchesLeft", searchQuota.remaining(session, user));
            // --- Destination report for one city ---
            List<ScamReport> allScams = scamService.getReportsByCity(city);
            int totalScams = allScams.size();

            // Stats/score use the full list; the card list is paywalled.
            model.addAttribute("destination", destinationService.build(city, allScams));

            // Country: prefer the value resolved onto the city record, falling back
            // to the older hardcoded lookup for anything not yet backfilled.
            String countryName = cityCountryResolver.countryFor(city)
                    .orElseGet(() -> CountryLookup.forCity(city)
                            .map(com.zainab.roamSafe.service.CountryLookup.Country::name)
                            .orElse(null));

            if (countryName != null) {
                model.addAttribute("country", countryName);

                // Official government advisories for this country (real, sourced).
                List<com.zainab.roamSafe.model.Advisory> advisories = advisoryRepository
                        .findByCountryName(countryName);
                if (!advisories.isEmpty()) {
                    model.addAttribute("advisories", advisories);
                }

                // Emergency numbers, from the structured dataset. Absent rather
                // than guessed when the country isn't covered.
                emergencyNumberService.forCountry(countryName)
                        .ifPresent(numbers -> model.addAttribute("emergency", numbers));
            }

            // Current news mentions for this city. Attributed and unverified -
            // shown as third-party reporting, never as a RoamSafe finding.
            var incidents = liveIncidentRepository
                    .findByCityNameIgnoreCaseOrderByPublishedAtDesc(city);
            if (!incidents.isEmpty()) {
                model.addAttribute("incidents", incidents);
            }

            // Arrival and practical info, verbatim excerpts with attribution.
            var practical = practicalInfoRepository.findByCityNameIgnoreCase(city);
            if (!practical.isEmpty()) {
                model.addAttribute("practical", practical);
            }

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
