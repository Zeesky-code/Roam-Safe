package com.zainab.roamSafe.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The relevance filter is the only thing standing between a world-news keyword
 * search and a city's safety page, so it is tested against real headlines that
 * the live API actually returned for these queries - including the ones that
 * made the unfiltered version unusable.
 */
class GdeltRelevanceTest {

    @Test
    void keepsHeadlinesAboutADisruptionInTheCity() {
        assertTrue(GdeltIngestionService.isRelevant("Paris",
                "Wildfires near Paris force evacuations , disrupt train lines and motorways"));
        assertTrue(GdeltIngestionService.isRelevant("Paris",
                "Homes evacuated as huge fire rages in Fontainebleau forest near Paris"));
        assertTrue(GdeltIngestionService.isRelevant("Barcelona",
                "Barcelona metro strike to close three lines on Friday"));
    }

    @Test
    void rejectsWorldNewsThatMerelyMentionsTheWords() {
        // Real GDELT results for a Paris disruption query. Without the
        // city-in-headline test these were published as Paris travel incidents.
        assertFalse(GdeltIngestionService.isRelevant("Paris",
                "Climate strike named Collin word of the year 2019"));
        assertFalse(GdeltIngestionService.isRelevant("Paris",
                "Doctors rally in France demanding release of detained Gaza hospital chief"));
        assertFalse(GdeltIngestionService.isRelevant("Paris",
                "Franco - German defense cooperation under strain as Macron , Merz meet"));
    }

    @Test
    void rejectsCityMentionsWithNoDisruption() {
        assertFalse(GdeltIngestionService.isRelevant("Paris",
                "Paris fashion week draws record crowds"));
        assertFalse(GdeltIngestionService.isRelevant("Tokyo",
                "Tokyo named world's best city for food"));
    }

    @Test
    void handlesMissingInputWithoutThrowing() {
        assertFalse(GdeltIngestionService.isRelevant("Paris", null));
        assertFalse(GdeltIngestionService.isRelevant(null, "Paris strike"));
    }
}
