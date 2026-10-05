package com.zainab.roamSafe.controller;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** People type routes in whatever shape comes naturally; each should read the same. */
class GuideRouteParsingTest {

    private static final List<String> ROUTE = List.of("Paris", "Brussels", "Berlin");

    @Test
    void acceptsCommasArrowsAndThen() {
        assertEquals(ROUTE, GuideController.parse("Paris, Brussels, Berlin"));
        assertEquals(ROUTE, GuideController.parse("Paris -> Brussels -> Berlin"));
        assertEquals(ROUTE, GuideController.parse("Paris → Brussels > Berlin"));
        assertEquals(ROUTE, GuideController.parse("Paris then Brussels then Berlin"));
        assertEquals(ROUTE, GuideController.parse("Paris\nBrussels\n\nBerlin"));
    }

    @Test
    void keepsMultiWordCities() {
        assertEquals(List.of("Mexico City", "Buenos Aires"), GuideController.parse("Mexico City, Buenos Aires"));
    }

    @Test
    void emptyInputMeansNoStops() {
        assertTrue(GuideController.parse(null).isEmpty());
        assertTrue(GuideController.parse("  , ,").isEmpty());
    }
}
