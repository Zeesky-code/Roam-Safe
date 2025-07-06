package com.zainab.roamSafe.controller;

import java.util.List;
import java.util.stream.Collectors;
import com.zainab.roamSafe.model.Scam;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/scams")
public class ScamController {

    private final List<Scam> scams = List.of(
        new Scam("istanbul", "Taxi Scam", "Driver 'forgets' to start the meter."),
        new Scam("paris", "Petition Scam", "Teens pretend to collect signatures."),
        new Scam("bangkok", "Closed Temple", "Tuk-tuk driver says temple is closed.")
    );

    @GetMapping
    public String showScamsPage() {
        return "scams";
    }

    @GetMapping("/api")
    @ResponseBody
    public List<Scam> getScams(@RequestParam String city) {
        return scams.stream()
            .filter(scam -> scam.getCity().equalsIgnoreCase(city))
            .collect(Collectors.toList());
    }


}
