package com.zainab.roamSafe.controller;

import java.util.List;
import com.zainab.roamSafe.model.Scam;
import com.zainab.roamSafe.repository.ScamRepository;
import com.zainab.roamSafe.model.City;
import com.zainab.roamSafe.repository.CityRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class ScamController {

    @Autowired
    private ScamRepository scamRepository;
    
    @Autowired
    private CityRepository cityRepository;

    @GetMapping
    public String showScamsPage() {
        return "scams";
    }

    @GetMapping("/scams")
    @ResponseBody
    public List<Scam> getScams(@RequestParam String city) {
        System.out.println("Searching for scams in city: " + city);
        List<Scam> scams = scamRepository.findByCityName(city);
        System.out.println("Found " + scams.size() + " scams for " + city);
        return scams;
    }

    @GetMapping("/cities")
    public ResponseEntity<List<City>> getAllCities() {
        List<City> cities = cityRepository.findAll();
        return ResponseEntity.ok(cities);
    }
}
