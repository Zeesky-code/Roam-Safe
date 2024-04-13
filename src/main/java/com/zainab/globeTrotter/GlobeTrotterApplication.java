package com.zainab.globeTrotter;

import com.zainab.globeTrotter.itinerary.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class GlobeTrotterApplication {
	private static final Logger log = LoggerFactory.getLogger(GlobeTrotterApplication.class);

	@Autowired
	ItemRepository itineraryItemRepo;

	public static void main(String[] args) {
		SpringApplication.run(GlobeTrotterApplication.class, args);
		log.info("Application started successfully");
	}

}
