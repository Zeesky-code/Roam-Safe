package com.zainab.roamSafe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class WaitlistRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    public WaitlistRequest() {}

    public WaitlistRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}