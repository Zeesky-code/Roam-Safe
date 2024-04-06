package com.zainab.globeTrotter.repository;

import com.zainab.globeTrotter.model.ItineraryItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends MongoRepository<ItineraryItem, String> {

}
