package com.zainab.roamSafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoamSafeApplication {

	public static void main(String[] args) {
		// Load .env file if it exists (for local development)
		try {
			java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
			if (java.nio.file.Files.exists(envPath)) {
				java.util.List<String> lines = java.nio.file.Files.readAllLines(envPath);
				for (String line : lines) {
					if (line.contains("=") && !line.startsWith("#")) {
						String[] parts = line.split("=", 2);
						if (System.getProperty(parts[0].trim()) == null) {
							System.setProperty(parts[0].trim(), parts[1].trim());
						}
					}
				}
				System.out.println("Loaded environment variables from .env file");
			}
		} catch (Exception e) {
			System.out.println("No .env file found or error reading it. Proceeding with environment variables.");
		}
		SpringApplication.run(RoamSafeApplication.class, args);
	}

}
