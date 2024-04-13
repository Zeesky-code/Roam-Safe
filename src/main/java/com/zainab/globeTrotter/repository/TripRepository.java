package com.zainab.globeTrotter.repository;

import com.zainab.globeTrotter.model.Trip;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TripRepository extends MongoRepository<Trip, ObjectId> {
}
