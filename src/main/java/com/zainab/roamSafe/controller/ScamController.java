package com.zainab.roamSafe.controller;

import java.util.List;
import com.zainab.roamSafe.model.Scam;
import com.zainab.roamSafe.repository.ScamRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/scams")
public class ScamController {

    @Autowired
    private ScamRepository scamRepository;

    @GetMapping
    public String showScamsPage() {
        return "scams";
    }

    @GetMapping("/api")
    @ResponseBody
    public List<Scam> getScams(@RequestParam String city) {
        System.out.println("Searching for scams in city: " + city);
        List<Scam> scams = scamRepository.findByCityName(city);
        System.out.println("Found " + scams.size() + " scams for " + city);
        return scams;
    }
}
