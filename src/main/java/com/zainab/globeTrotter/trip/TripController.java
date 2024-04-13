package com.zainab.globeTrotter.trip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripController {

    @Autowired
    private TripService service;

    @GetMapping("/createTrip")
    public String createTrip(){
        return "createTrip";
    }

    @PostMapping("/createTrip")
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip){
        Trip t = service.createTrip(trip);
        return new ResponseEntity<>(t, HttpStatus.CREATED);
    }
}
