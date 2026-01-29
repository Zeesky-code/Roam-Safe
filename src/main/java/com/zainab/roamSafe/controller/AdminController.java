package com.zainab.roamSafe.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;
import com.zainab.roamSafe.service.ScamService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ScamReportRepository scamReportRepository;
    @Autowired
    private ScamService scamService;

    public AdminController(ScamReportRepository scamReportRepository) {
        this.scamReportRepository = scamReportRepository;
    }

    @GetMapping("/submissions")
    public String viewSubmissions(Model model) {
        List<ScamReport> unreviewed = scamReportRepository.findByStatus(ScamReportStatus.PENDING);
        model.addAttribute("submissions", unreviewed);
        return "admin-submissions";
    }

    @PostMapping("/submissions/{id}/review")
    public String markAsReviewed(@PathVariable Long id) {
        Optional<ScamReport> optional = scamReportRepository.findById(id);
        optional.ifPresent(report -> {
            report.setStatus(ScamReportStatus.APPROVED);
            scamReportRepository.save(report);
        });
        return "redirect:/admin/submissions";
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("total", scamService.getTotalScams());
        model.addAttribute("approved", scamService.getApprovedScams());
        model.addAttribute("pending", scamService.getPendingScams());
        model.addAttribute("topCities", scamService.getTopCities(5));
        return "analytics";
    }

}
