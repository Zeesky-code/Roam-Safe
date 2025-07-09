package com.zainab.roamSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoamSafeApplication {
	private static final Logger log = LoggerFactory.getLogger(RoamSafeApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(RoamSafeApplication.class, args);
		log.info("Application started successfully");
	}

}
