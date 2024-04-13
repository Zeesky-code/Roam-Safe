package com.zainab.globeTrotter.destination;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DestinationRepository extends MongoRepository<Destination, ObjectId> {
    Destination findByName(String name);
}
