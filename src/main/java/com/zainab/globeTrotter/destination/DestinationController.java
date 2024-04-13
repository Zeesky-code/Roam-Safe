package com.zainab.globeTrotter.destination;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class DestinationController {

    @Autowired
    private DestinationService service;

    @PostMapping("/addDestination")
    public ResponseEntity<Destination> createDestination(@RequestBody Destination destination){
        Destination d = service.saveDestination(destination);
        return new ResponseEntity<>(d, HttpStatus.CREATED);
    }

    @PutMapping("/updateDestination")
    public ResponseEntity<Destination> updateDestination(@RequestBody Destination destination){
        Destination d = service.updateDestination(destination);
        return new ResponseEntity<>(d, HttpStatus.CREATED);
    }

    @GetMapping("/findAllDestinations")
    public ResponseEntity<List<Destination>> getDestinations(){
        List<Destination> destinations = service.getDestinations();
        return new ResponseEntity<>(destinations, HttpStatus.OK);
    }

    @DeleteMapping("/deleteDestination")
    public ResponseEntity<String> deleteDestination(@RequestParam(name = "id") String id) {
        String message = service.deleteDestination(id);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}
