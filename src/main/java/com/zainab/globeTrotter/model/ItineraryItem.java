package com.zainab.globeTrotter.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document("itineraryItem")
public class ItineraryItem {

    @Id
    private ObjectId id;
    private String name;
    private String description;
    private LocalDate date;

    public ItineraryItem(String name, String description, LocalDate date) {
        super();
        this.name = name;
        this.description = description;
        this.date = date;
    }
}
