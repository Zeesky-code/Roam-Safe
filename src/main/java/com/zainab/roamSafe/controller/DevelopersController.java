package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.service.DestinationService;
import com.zainab.roamSafe.service.ScamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class DevelopersController {

    private final ScamService scamService;
    private final DestinationService destinationService;

    public DevelopersController(ScamService scamService, DestinationService destinationService) {
        this.scamService = scamService;
        this.destinationService = destinationService;
    }

    @GetMapping("/developers")
    public String developers() {
        return "developers";
    }

    /**
     * Trip planner. When a city is supplied we produce a real briefing from that
     * city's actual reports + computed score. AI day-by-day itineraries are a
     * roadmap feature; the page is honest about that rather than faking one.
     */
    @GetMapping("/planner")
    public String planner(@RequestParam(required = false) String city, Model model) {
        if (city != null && !city.isBlank()) {
            List<ScamReport> reports = scamService.getReportsByCity(city);
            model.addAttribute("city", city);
            model.addAttribute("destination", destinationService.build(city, reports));
            model.addAttribute("topScams", reports.stream().limit(3).toList());
            model.addAttribute("hasBriefing", true);
        } else {
            model.addAttribute("hasBriefing", false);
        }
        return "planner";
    }
}
