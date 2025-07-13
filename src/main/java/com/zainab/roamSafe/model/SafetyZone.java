package com.zainab.roamSafe.model;

public enum SafetyZone {
    GREEN("🟢 Safe - Walked alone at midnight, felt safe"),
    YELLOW("🟡 Caution - Daylight only, busy streets preferred"),
    RED("🔴 Avoid - Multiple harassment reports, avoid completely"),
    UNKNOWN("⚪ Unknown - No safety data available");
    
    private final String description;
    
    SafetyZone(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getEmoji() {
        return description.split(" ")[0];
    }
} 