package com.zainab.roamSafe.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // Old data loader logic removed as part of schema refactoring.
        // SeedController is now the primary way to populate the database.
    }
}