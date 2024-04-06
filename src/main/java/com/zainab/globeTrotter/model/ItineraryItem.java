package com.zainab.globeTrotter.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document("itineraryItem")
public class ItineraryItem {

    @Id
    private String id;
    private String name;
    private String description;
    private LocalDate date;

    public ItineraryItem(String id, String name, String description, LocalDate date) {
        super();
        this.id = id;
        this.name = name;
        this.description = description;
        this.date = date;
    }
}
