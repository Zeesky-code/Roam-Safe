package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.LiveIncident;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void rejectsRetrospectives() {
        // Live on the Mexico City page on 2026-10-05, three times over.
        assertFalse(GdeltIngestionService.isRelevant("Mexico City",
                "Today in History : October 2 , hundreds massacred at Mexico City student protest"));
    }

    @Test
    void showsASyndicatedStoryOnce() {
        String headline = "Kashmiri community holds major protest in London over killings in PoJK";
        var shown = GdeltIngestionService.current(List.of(
                new LiveIncident("London", headline, "https://a.example/1", "a.example", null),
                new LiveIncident("London", headline + " ", "https://b.example/1", "b.example", null),
                new LiveIncident("London", "All train lines CLOSED amid emergency London incident",
                        "https://c.example/1", "c.example", null)));
        assertEquals(2, shown.size());
        assertEquals("a.example", shown.get(0).getSourceDomain());
    }

    @Test
    void handlesMissingInputWithoutThrowing() {
        assertFalse(GdeltIngestionService.isRelevant("Paris", null));
        assertFalse(GdeltIngestionService.isRelevant(null, "Paris strike"));
    }
}
