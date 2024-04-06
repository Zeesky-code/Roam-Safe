package com.zainab.globeTrotter.controller;

import com.zainab.globeTrotter.model.ItineraryItem;
import com.zainab.globeTrotter.repository.ItemRepository;
import com.zainab.globeTrotter.service.ItineraryService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ItineraryController {

    @Autowired
    private ItineraryService service;

    @PostMapping("/addItem")
    public ResponseEntity<ItineraryItem> createItineraryItem(@RequestBody ItineraryItem item){
        ItineraryItem i = service.saveItineraryItem(item);
        return new ResponseEntity<>(i, HttpStatus.CREATED);
    }

    @PutMapping("/updateItem")
    public ResponseEntity<ItineraryItem> updateItineraryItem(@RequestBody ItineraryItem item){
        ItineraryItem i = service.updateItineraryItem(item);
        return new ResponseEntity<>(i, HttpStatus.CREATED);
    }

    @GetMapping("/findAllItems")
    public ResponseEntity<List<ItineraryItem>> getItineraryItems(){
        List<ItineraryItem> students = service.getItineraryItems();
        return new ResponseEntity<>(students, HttpStatus.OK);
    }



    @DeleteMapping("/student")
    public ResponseEntity<String> deleteItineraryItem(@RequestParam(name = "id") String id) {
        String message = service.deleteItineraryItem(id);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}
