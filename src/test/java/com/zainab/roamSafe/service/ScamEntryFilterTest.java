package com.zainab.roamSafe.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the scam/non-scam classification so the data cleanup stays durable:
 * a re-import must not be able to reintroduce the disaster/emergency-info/vague
 * entries that were removed, and must never drop a real scam.
 */
class ScamEntryFilterTest {

    @Test
    void keepsRealScamsEvenWhenTheyMentionWeatherOrEmergencies() {
        assertTrue(ScamEntryFilter.isScamEntry("Taxi Overcharging during Typhoons/Rainstorms"));
        assertTrue(ScamEntryFilter.isScamEntry("Emergency Services Disruption & Scam Calls"));
        assertTrue(ScamEntryFilter.isScamEntry("Friendship Bracelet Scam"));
        assertTrue(ScamEntryFilter.isScamEntry("Metro Pickpockets"));
        assertTrue(ScamEntryFilter.isScamEntry("Currency Exchange Rip-Off"));
    }

    @Test
    void rejectsDisastersEmergencyInfoAndVagueSafety() {
        assertFalse(ScamEntryFilter.isScamEntry("Earthquake Risk"));
        assertFalse(ScamEntryFilter.isScamEntry("Typhoon Signal 9/10 Dangers"));
        assertFalse(ScamEntryFilter.isScamEntry("Monsoon Season Flooding"));
        assertFalse(ScamEntryFilter.isScamEntry("Emergency Number 112"));
        assertFalse(ScamEntryFilter.isScamEntry("Emergency Services Contact"));
        assertFalse(ScamEntryFilter.isScamEntry("General Safety in Tokyo"));
    }

    @Test
    void rejectsBlankOrNullTitles() {
        assertFalse(ScamEntryFilter.isScamEntry(null));
        assertFalse(ScamEntryFilter.isScamEntry("   "));
    }
}
