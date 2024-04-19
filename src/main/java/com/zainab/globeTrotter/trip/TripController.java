package com.zainab.globeTrotter.trip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Controller
public class TripController {

    @Autowired
    private TripService service;

    @GetMapping("/createTrip")
    public String createTrip(Model model) {
        model.addAttribute("newTrip", new Trip());
        return "createTrip";
    }

    @PostMapping("/createTrip")
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip){
        Trip t = service.createTrip(trip);
        return new ResponseEntity<>(t, HttpStatus.CREATED);
    }

    @GetMapping("/featuredTrips")
    public String showFeaturedTrips(Model model) {
        List<Trip> featuredTrips = service.findFeaturedTrips();
        model.addAttribute("featuredTrips", featuredTrips);
        return "featuredTrips";
    }
}
