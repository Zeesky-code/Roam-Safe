package com.zainab.globeTrotter.trip;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TripRepository extends MongoRepository<Trip, ObjectId> {
}
