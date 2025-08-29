package com.zainab.roamSafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.zainab.roamSafe.model.SubmittedScam;
import com.zainab.roamSafe.repository.SubmittedScamRepository;

@Controller
public class SubmissionController {

    private final SubmittedScamRepository submittedScamRepository;

    public SubmissionController(SubmittedScamRepository submittedScamRepository) {
        this.submittedScamRepository = submittedScamRepository;
    }

    @GetMapping("/submit")
    public String showForm(Model model) {
        model.addAttribute("submittedScam", new SubmittedScam());
        return "submit";
    }

    @PostMapping("/submit")
    public String handleSubmit(@ModelAttribute SubmittedScam submittedScam, Model model) {
        submittedScamRepository.save(submittedScam);
        model.addAttribute("success", true);
        return "submit";
    }
}
