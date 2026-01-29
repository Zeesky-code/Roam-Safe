package com.zainab.roamSafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.repository.ScamReportRepository;

@Controller
public class SubmissionController {

    private final ScamReportRepository scamReportRepository;

    public SubmissionController(ScamReportRepository scamReportRepository) {
        this.scamReportRepository = scamReportRepository;
    }

    @GetMapping("/submit")
    public String showForm(Model model) {
        model.addAttribute("scamReport", new ScamReport());
        return "submit";
    }

    @PostMapping("/submit")
    public String handleSubmit(@ModelAttribute ScamReport scamReport, Model model) {
        scamReportRepository.save(scamReport);
        model.addAttribute("success", true);
        return "submit";
    }
}
