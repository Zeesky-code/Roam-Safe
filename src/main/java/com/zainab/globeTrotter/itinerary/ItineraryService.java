package com.zainab.globeTrotter.itinerary;

import com.zainab.globeTrotter.destination.Destination;
import com.zainab.globeTrotter.destination.DestinationService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItineraryService {
    @Autowired
    private ItemRepository repo;

    @Autowired
    private DestinationService destinationService;

    public List<ItineraryItem> getItineraryItems(){
        return repo.findAll();
    }

    public ItineraryItem saveItineraryItem(ItineraryItem item) {
        Destination destination = item.getDestination();
        Optional<Destination> existingDestination = Optional.ofNullable(destinationService.getDestinationByName(destination.getName()));
        item.setDestination(existingDestination.orElseGet(() -> destinationService.saveDestination(destination)));
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
