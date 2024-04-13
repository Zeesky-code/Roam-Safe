package com.zainab.globeTrotter.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Getter
@Setter
@Document("trip")
public class Trip {
    @Id
    private ObjectId id;
    private String name;
    private List<Destination> destinations;
    private List<ItineraryItem> itinerary;

    public Trip(String name,  List<Destination> destinations ,List<ItineraryItem> itinerary){
        this.name = name;
        this.destinations = destinations;
        this.itinerary = itinerary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }

    public void setDestination(Destination destination) {
        this.destinations.add(destination);
    }

    public List<ItineraryItem> getItinerary() {
        return itinerary;
    }

    public void setItinerary(ItineraryItem itinerary) {
        this.itinerary.add(itinerary);
    }
    public void removeDestination(Destination destination){
        this.destinations.remove(destination);
    }

    public void removeItineraryItem(ItineraryItem itineraryItem){
        this.itinerary.remove(itineraryItem);
    }
}
