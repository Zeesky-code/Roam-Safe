package com.zainab.globeTrotter.controller;

import com.zainab.globeTrotter.model.ItineraryItem;
import com.zainab.globeTrotter.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ItineraryController {

    @Autowired
    private ItemRepository repo;

    @PostMapping("/addItem")
    public String saveItem(@RequestBody ItineraryItem item){
        repo.save(item);
        return "Itinerary Item Added Successfully";
    }

    @GetMapping("/findAllItems")
    public List<ItineraryItem> getItineraryItems(){
        return repo.findAll();
    }

    @DeleteMapping("/delete/{id}")
    public String deleteItem(@PathVariable int id){
        repo.deleteById(String.valueOf(id));
        return "Itinerary Item Deleted Successfully";
    }
}
