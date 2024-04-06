package com.zainab.globeTrotter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GlobeTrotterApplication {
	private static final Logger log = LoggerFactory.getLogger(GlobeTrotterApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(GlobeTrotterApplication.class, args);
		log.info("Application started successfully");
	}

}
