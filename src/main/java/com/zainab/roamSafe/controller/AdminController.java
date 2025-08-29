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

import com.zainab.roamSafe.model.SubmittedScam;
import com.zainab.roamSafe.repository.SubmittedScamRepository;
import com.zainab.roamSafe.service.ScamService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SubmittedScamRepository submittedScamRepository;
    @Autowired
    private ScamService scamService;

    public AdminController(SubmittedScamRepository submittedScamRepository) {
        this.submittedScamRepository = submittedScamRepository;
    }

    @GetMapping("/submissions")
    public String viewSubmissions(Model model) {
        List<SubmittedScam> unreviewed = submittedScamRepository.findByReviewedFalse();
        model.addAttribute("submissions", unreviewed);
        return "admin-submissions";
    }

    @PostMapping("/submissions/{id}/review")
    public String markAsReviewed(@PathVariable Long id) {
        Optional<SubmittedScam> optional = submittedScamRepository.findById(id);
        optional.ifPresent(sub -> {
            sub.setReviewed(true);
            submittedScamRepository.save(sub);
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
