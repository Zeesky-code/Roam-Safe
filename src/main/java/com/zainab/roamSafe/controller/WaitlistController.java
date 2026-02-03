package com.zainab.roamSafe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.zainab.roamSafe.service.WaitlistService;
import com.zainab.roamSafe.dto.WaitlistRequest;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/")
public class WaitlistController {

    @Autowired
    private WaitlistService waitlistService;

    @PostMapping("/waitlist")
    public String addToWaitlist(@Valid @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        try {
            if (!isValidEmail(email)) {
                redirectAttributes.addFlashAttribute("error", "Please enter a valid email address");
                return "redirect:/";
            }

            boolean success = waitlistService.addToWaitlist(email);

            if (success) {
                redirectAttributes.addFlashAttribute("success",
                        "Thanks! You're on the waitlist. We'll notify you first! ✨");
            } else {
                redirectAttributes.addFlashAttribute("error", "You're already on our waitlist!");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Something went wrong. Please try again.");
            e.printStackTrace();
        }

        return "redirect:/";
    }

    // REST API endpoint (optional - for AJAX calls)
    @PostMapping("/api/waitlist")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToWaitlistAPI(@Valid @RequestBody WaitlistRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!isValidEmail(request.getEmail())) {
                response.put("success", false);
                response.put("message", "Please enter a valid email address");
                return ResponseEntity.badRequest().body(response);
            }

            boolean success = waitlistService.addToWaitlist(request.getEmail());

            if (success) {
                response.put("success", true);
                response.put("message", "Thanks! You're on the waitlist. We'll notify you first! ✨");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "You're already on our waitlist!");
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Something went wrong. Please try again.");
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/waitlist/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getWaitlistCount() {
        Map<String, Object> response = new HashMap<>();
        try {
            int count = waitlistService.getWaitlistCount();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Unable to get waitlist count");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}