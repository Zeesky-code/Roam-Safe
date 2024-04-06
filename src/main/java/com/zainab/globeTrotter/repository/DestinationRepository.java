package com.zainab.globeTrotter.repository;

import com.zainab.globeTrotter.model.Destination;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DestinationRepository extends MongoRepository<Destination, ObjectId> {
}
