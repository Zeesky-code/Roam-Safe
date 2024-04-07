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
    private Destination destination;
    private LocalDate date;

    public ItineraryItem(String name, String description, LocalDate date, Destination destination) {
        super();
        this.name = name;
        this.description = description;
        this.destination = destination;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
