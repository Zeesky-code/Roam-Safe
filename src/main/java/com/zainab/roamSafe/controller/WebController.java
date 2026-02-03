package com.zainab.roamSafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String landing() {
        return "landing";
    }

    @GetMapping("/map")
    public String map() {
        return "map";
    }
}