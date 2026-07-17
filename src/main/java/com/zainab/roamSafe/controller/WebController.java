package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.repository.ScamReportRepository;
import com.zainab.roamSafe.service.LandingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class WebController {

    private final ScamReportRepository scamReportRepository;
    private final LandingService landingService;

    public WebController(ScamReportRepository scamReportRepository, LandingService landingService) {
        this.scamReportRepository = scamReportRepository;
        this.landingService = landingService;
    }

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("stats", landingService.stats());
        model.addAttribute("briefing", landingService.heroBriefing());
        model.addAttribute("intel", landingService.intel());
        model.addAttribute("scamPreview", landingService.scamPreview(2));
        model.addAttribute("scamTotal", scamReportRepository.countByStatus(
                com.zainab.roamSafe.model.ScamReportStatus.APPROVED));
        // paletteItems supplied globally by GlobalModelAdvice
        model.addAttribute("popularCities", List.of("Tokyo", "Bali", "Mexico City", "Lisbon", "Bangkok", "Barcelona"));
        return "landing";
    }

    @GetMapping("/map")
    public String map() {
        return "map";
    }
}