package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.repository.AdvisoryRepository;
import com.zainab.roamSafe.service.CityCountryResolver;
import com.zainab.roamSafe.service.EmergencyNumberService;
import com.zainab.roamSafe.service.StreetIntelligenceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * Street-level intelligence for a named place.
 *
 * /street?q=Rambla searches; adding &city= pins it to one profile, which is
 * needed because the same name occurs in several cities.
 */
@Controller
public class StreetController {

    private final StreetIntelligenceService streetService;
    private final CityCountryResolver cityCountryResolver;
    private final AdvisoryRepository advisoryRepository;
    private final EmergencyNumberService emergencyNumberService;
    private final com.zainab.roamSafe.service.SearchQuotaService searchQuota;

    public StreetController(StreetIntelligenceService streetService,
            CityCountryResolver cityCountryResolver,
            AdvisoryRepository advisoryRepository,
            EmergencyNumberService emergencyNumberService,
            com.zainab.roamSafe.service.SearchQuotaService searchQuota) {
        this.searchQuota = searchQuota;
        this.streetService = streetService;
        this.cityCountryResolver = cityCountryResolver;
        this.advisoryRepository = advisoryRepository;
        this.emergencyNumberService = emergencyNumberService;
    }

    @GetMapping("/street")
    public String street(@RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            HttpSession session,
            Model model) {

        model.addAttribute("query", q);
        var user = (com.zainab.roamSafe.model.User) session.getAttribute("user");
        if (q != null && !q.isBlank() && !searchQuota.allow(session, user, "street:" + q + "|" + city)) {
            return "redirect:/pricing?limit=search";
        }
        model.addAttribute("searchesLeft", searchQuota.remaining(session, user));

        if (q == null || q.isBlank()) {
            model.addAttribute("matches", List.of());
            return "street";
        }

        List<StreetIntelligenceService.Match> matches = streetService.search(q);
        model.addAttribute("matches", matches);

        // Pin to one place when the caller named a city, or when the search is
        // unambiguous. Otherwise show the list and let the user choose rather
        // than guessing which Old Town they meant.
        StreetIntelligenceService.Match target = null;
        if (city != null && !city.isBlank()) {
            target = matches.stream()
                    .filter(m -> m.city().equalsIgnoreCase(city))
                    .findFirst().orElse(null);
        } else if (matches.size() == 1) {
            target = matches.get(0);
        }

        if (target != null) {
            String country = cityCountryResolver.countryFor(target.city()).orElse(null);
            streetService.profile(target.city(), target.place(), country)
                    .ifPresent(profile -> {
                        model.addAttribute("profile", profile);
                        if (country != null) {
                            var advisories = advisoryRepository.findByCountryName(country);
                            if (!advisories.isEmpty()) {
                                model.addAttribute("advisory", advisories.get(0));
                            }
                            emergencyNumberService.forCountry(country)
                                    .ifPresent(n -> model.addAttribute("emergency", n));
                        }
                    });
        }
        return "street";
    }
}
