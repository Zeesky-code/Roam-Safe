package com.zainab.roamSafe.itinerary;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ItemRepository extends MongoRepository<ItineraryItem, ObjectId> {

}
