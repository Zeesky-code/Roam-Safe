package com.zainab.roamSafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class PricingController {

    @GetMapping("/pricing")
    public String showPricingPage(Model model) {
        return "pricing";
    }
}
