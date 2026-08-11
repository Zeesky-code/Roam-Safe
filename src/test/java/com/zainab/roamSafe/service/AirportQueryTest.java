package com.zainab.roamSafe.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * US4 (airport arrival): a search for "Istanbul Airport" has to reach the
 * Istanbul destination page and its arrival tab, not fall through to an empty
 * "no reports" state. These tests pin the normalization in both directions -
 * airport queries collapse to their city, real cities are left untouched.
 */
class AirportQueryTest {

    @Test
    void recognizesAirportQueries() {
        assertTrue(AirportQuery.isAirport("Istanbul Airport"));
        assertTrue(AirportQuery.isAirport("barcelona airport"));
        assertTrue(AirportQuery.isAirport("Rome International Airport"));
    }

    @Test
    void leavesPlainCitiesAlone() {
        assertFalse(AirportQuery.isAirport("Istanbul"));
        assertFalse(AirportQuery.isAirport("Barcelona"));
        assertEquals("Istanbul", AirportQuery.toCity("Istanbul"));
        // "Airport" as a district name mid-string is not a trailing airport query.
        assertFalse(AirportQuery.isAirport("Airport Road"));
    }

    @Test
    void stripsToBaseCity() {
        assertEquals("Istanbul", AirportQuery.toCity("Istanbul Airport"));
        // Casing is preserved as typed; the city lookup itself is case-insensitive.
        assertEquals("barcelona", AirportQuery.toCity("barcelona airport"));
        assertEquals("Rome", AirportQuery.toCity("Rome International Airport"));
        assertEquals("Istanbul", AirportQuery.toCity("  Istanbul   Airport "));
    }

    @Test
    void handlesNullAndEmptySafely() {
        assertFalse(AirportQuery.isAirport(null));
        assertEquals("Airport", AirportQuery.toCity("Airport")); // nothing left to strip
    }
}
