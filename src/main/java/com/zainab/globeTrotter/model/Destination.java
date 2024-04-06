package com.zainab.globeTrotter.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document("destination")
public class Destination {
    @Id
    private ObjectId id;
    private String name;
    private String description;
    private List<String> images;

    public Destination(String name, String description, List<String> images){
        this.name = name;
        this.description = description;
        this.images = images;
    }
}
