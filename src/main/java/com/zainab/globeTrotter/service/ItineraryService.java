package com.zainab.globeTrotter.service;

import com.zainab.globeTrotter.model.Destination;
import com.zainab.globeTrotter.model.ItineraryItem;
import com.zainab.globeTrotter.repository.ItemRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItineraryService {
    @Autowired
    private ItemRepository repo;

    @Autowired
    private DestinationService destinationService;

    public List<ItineraryItem> getItineraryItems(){
        return repo.findAll();
    }
    public ItineraryItem saveItineraryItem(ItineraryItem item){
        Destination destination = item.getDestination();
        Destination existingDestination = destinationService.getDestinationByName(destination.getName());
        if(existingDestination == null){
            destination = destinationService.saveDestination(destination);
            item.setDestination(destination);
        }
        return repo.save(item);
    }

    public ItineraryItem updateItineraryItem(ItineraryItem item){
        return repo.save(item);
    }

    public String deleteItineraryItem(String id){
        repo.deleteById(new ObjectId(id));
        return "Itinerary Item Deleted Successfully";
    }
    public ItineraryItem getItineraryItemById(String id) {
        return repo.findById(new ObjectId(id)).orElse(null);
    }
}
