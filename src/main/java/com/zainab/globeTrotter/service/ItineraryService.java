package com.zainab.globeTrotter.service;

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

    public List<ItineraryItem> getItineraryItems(){
        return repo.findAll();
    }
    public ItineraryItem saveItineraryItem(ItineraryItem item){
        return repo.save(item);
    }

    public ItineraryItem updateItineraryItem(ItineraryItem item){
        return repo.save(item);
    }

    public String deleteItineraryItem(String id){
        repo.deleteById(new ObjectId(id));
        return "Itinerary Item Deleted Successfully";
    }
}
