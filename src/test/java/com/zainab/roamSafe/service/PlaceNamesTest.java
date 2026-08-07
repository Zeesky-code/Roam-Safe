package com.zainab.roamSafe.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the place-name extraction that street intelligence and the
 * neighborhood breakdown depend on. This was a real bug source: the importer
 * had crammed several places into one field and mislabelled headings, roles and
 * currencies as districts, so the product once offered "Taxi drivers" as a safer
 * area to go to. These tests pin both directions - real places are kept and
 * split apart, non-places are rejected.
 */
class PlaceNamesTest {

    @Test
    void splitsMultiplePlacesInOneValue() {
        // The value that hid coverage: a search for La Rambla matched none of it.
        List<String> out = PlaceNames.extract("La Rambla, Raval, public transport, train and bus stations");
        assertTrue(out.contains("La Rambla"), "should recover La Rambla");
        assertTrue(out.contains("Raval"), "should recover Raval");
        assertFalse(out.contains("public transport"), "topic, not a place");
        assertFalse(out.stream().anyMatch(p -> p.contains("train and bus")), "topic, not a place");
    }

    @Test
    void keepsRealDistrictNames() {
        assertEquals(List.of("Taksim"), PlaceNames.extract("Taksim"));
        assertEquals(List.of("Sultanahmet"), PlaceNames.extract("Sultanahmet"));
        assertEquals(List.of("Gothic Quarter", "El Raval"), PlaceNames.extract("Gothic Quarter and El Raval"));
        assertTrue(PlaceNames.extract("Blue Mosque area").contains("Blue Mosque area"));
    }

    @Test
    void rejectsSectionHeadingsAndTopics() {
        assertTrue(PlaceNames.extract("Stay safe").isEmpty());
        assertTrue(PlaceNames.extract("Crime").isEmpty());
        assertTrue(PlaceNames.extract("ATMs").isEmpty());
        assertTrue(PlaceNames.extract("the Old Town").isEmpty());
        assertTrue(PlaceNames.extract("Theft in public transit").isEmpty());
    }

    @Test
    void rejectsRolesAndThingsNotPlaces() {
        // The exact false positives that reached the UI as "safer areas".
        assertTrue(PlaceNames.extract("Taxi drivers").isEmpty());
        assertTrue(PlaceNames.extract("Shoe shine scam").isEmpty());
        assertTrue(PlaceNames.extract("Street vendors").isEmpty());
        assertTrue(PlaceNames.extract("Fake police officers").isEmpty());
        assertTrue(PlaceNames.extract("Overpricing").isEmpty());
        assertTrue(PlaceNames.extract("Lira").isEmpty(), "a currency is not a place");
    }

    @Test
    void keepsRealPlacesThatLookLikeActivitiesOrCurrencies() {
        // The '-ing' rule must not eat real cities; substring rules must not eat
        // real districts.
        assertEquals(List.of("Beijing"), PlaceNames.extract("Beijing"));
        assertEquals(List.of("Nanjing"), PlaceNames.extract("Nanjing"));
    }

    @Test
    void handlesNullAndBlank() {
        assertTrue(PlaceNames.extract(null).isEmpty());
        assertTrue(PlaceNames.extract("   ").isEmpty());
        assertFalse(PlaceNames.isRealPlace("Stay safe"));
        assertTrue(PlaceNames.isRealPlace("Taksim"));
    }
}
