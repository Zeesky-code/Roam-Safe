package com.zainab.globeTrotter.destination;

import com.zainab.globeTrotter.destination.Destination;
import com.zainab.globeTrotter.destination.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DestinationService {
    @Autowired
    private DestinationRepository repo;

    public List<Destination> getDestinations(){ return repo.findAll();}

    public Destination saveDestination(Destination destination){ return repo.save(destination);}

    public Destination updateDestination(Destination destination){ return repo.save(destination);}

    public String deleteDestination(String id){
        repo.deleteById(new ObjectId(id));
        return "Destination Deleted Successfully";
    }
    public Destination getDestinationByName(String name) {
        return repo.findByName(name);
    }
}
