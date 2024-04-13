package com.zainab.globeTrotter.trip;

import com.zainab.globeTrotter.destination.Destination;
import com.zainab.globeTrotter.destination.DestinationRepository;
import com.zainab.globeTrotter.destination.DestinationService;
import com.zainab.globeTrotter.itinerary.ItemRepository;
import com.zainab.globeTrotter.itinerary.ItineraryItem;
import com.zainab.globeTrotter.itinerary.ItineraryService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final ItemRepository itineraryItemRepository;
    @Autowired
    private ItineraryService itineraryService;
    @Autowired
    private DestinationService destinationService;

    @Autowired
    public TripService(TripRepository tripRepository, DestinationRepository destinationRepository, ItemRepository itineraryItemRepository) {
        this.tripRepository = tripRepository;
        this.destinationRepository = destinationRepository;
        this.itineraryItemRepository = itineraryItemRepository;
    }
    public Trip createTrip(Trip trip) {
        List<Destination> destinations = trip.getDestinations();
        List<ItineraryItem> itinerary = trip.getItinerary();
        if (!destinations.isEmpty()) {
            List<Destination> destinationsToSave = new ArrayList<>(destinations);
            for (Destination destinationName : destinationsToSave) {
                Optional<Destination> destinationOptional = Optional.ofNullable(destinationService.getDestinationByName(destinationName.getName()));
                Destination destination = destinationOptional.orElseGet(() -> destinationService.saveDestination(destinationName));
                trip.removeDestination(destinationName);
                trip.setDestination(destination);
            }
        }
        if (!itinerary.isEmpty()) {
            List<ItineraryItem> itineraryItemsToSave = new ArrayList<>(itinerary);
            for (ItineraryItem itineraryItem : itineraryItemsToSave) {
                trip.removeItineraryItem(itineraryItem);
                trip.setItinerary(itineraryService.saveItineraryItem(itineraryItem));
            }
        }
        return tripRepository.save(trip);
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Trip getTripById(String id) {
        return tripRepository.findById(new ObjectId(id)).orElse(null);
    }
    public String deleteTrip(String id){
        tripRepository.deleteById(new ObjectId(id));
        return "Trip Deleted Successfully";
    }
    public Trip updateTrip( Trip updatedTrip) {
        return tripRepository.save(updatedTrip);
    }
}
