package com.zainab.globeTrotter.trip;

import com.zainab.globeTrotter.trip.Trip;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TripRepository extends MongoRepository<Trip, ObjectId> {
}
