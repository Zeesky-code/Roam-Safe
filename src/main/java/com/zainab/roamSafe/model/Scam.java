package com.zainab.roamSafe.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Scam {
    private String city;
    private String title;
    private String description;


    public String getCity() { 
        return city;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
} 