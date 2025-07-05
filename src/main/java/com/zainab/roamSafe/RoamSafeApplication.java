package com.zainab.roamSafe;

import com.zainab.roamSafe.itinerary.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class RoamSafeApplication {
	private static final Logger log = LoggerFactory.getLogger(RoamSafeApplication.class);

	@Autowired
	ItemRepository itineraryItemRepo;

	public static void main(String[] args) {
		SpringApplication.run(RoamSafeApplication.class, args);
		log.info("Application started successfully");
	}

}
