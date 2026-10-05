package com.zainab.roamSafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;

@Controller
public class SubmissionController {

    private final ScamReportRepository scamReportRepository;

    public SubmissionController(ScamReportRepository scamReportRepository) {
        this.scamReportRepository = scamReportRepository;
    }

    /**
     * Only the fields the public form shows may be bound.
     *
     * Binding the whole entity let anyone POST status=APPROVED to publish a
     * report without review, or id=<existing> to overwrite someone else's.
     */
    @InitBinder("scamReport")
    void allowOnlyFormFields(WebDataBinder binder) {
        binder.setAllowedFields("name", "city", "neighborhood", "category", "scamType",
                "description", "safetyZone", "safetyRating", "preventionTips", "isNightTimeIncident");
    }

    @GetMapping("/submit")
    public String showForm(Model model) {
        model.addAttribute("scamReport", new ScamReport());
        return "submit";
    }

    @PostMapping("/submit")
    public String handleSubmit(@ModelAttribute ScamReport scamReport, Model model) {
        // Belt and braces alongside the binder: a public submission is always a
        // new row awaiting review.
        scamReport.setId(null);
        scamReport.setStatus(ScamReportStatus.PENDING);
        // severity_score is required but the form never asked for it, so every
        // public submission failed on insert. Derive it from the submitter's own
        // 1 (dangerous) - 5 (very safe) rating rather than inventing one; an
        // admin reviews it before anything is published.
        scamReport.setSeverityScore(severityFromRating(scamReport.getSafetyRating()));
        scamReportRepository.save(scamReport);
        model.addAttribute("success", true);
        return "submit";
    }

    /** 1 (dangerous) maps to 10, 5 (very safe) to 2; an unrated report sits mid-scale. */
    static int severityFromRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            return 5;
        }
        return (6 - rating) * 2;
    }
}
