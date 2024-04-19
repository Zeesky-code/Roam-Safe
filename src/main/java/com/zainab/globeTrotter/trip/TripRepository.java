package com.zainab.globeTrotter.trip;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TripRepository extends MongoRepository<Trip, ObjectId> {
    List<Trip> findByIsFeatured(boolean isFeatured);
}
