package com.zainab.roamSafe.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parsing tests over real Wikivoyage markup. These excerpts are shown to
 * travellers as arrival instructions, so stray image captions and template
 * fragments are not cosmetic problems - they read as directions.
 */
class PracticalInfoParsingTest {

    @Test
    void stripsImageEmbedsThatWouldReadAsInstructions() {
        String wiki = "[[File:Istanbul Airport.jpg|thumb|Istanbul Airport, 2nd busiest in Europe]]\n"
                + "* By metro – The M11 line connects the airport to the city.";
        String out = PracticalInfoService.stripMarkup(wiki);
        assertFalse(out.contains("thumb"), "image caption leaked into the text");
        assertFalse(out.contains("File:"));
        assertTrue(out.contains("M11"));
    }

    @Test
    void keepsTheSubstanceOfAnInstruction() {
        String wiki = "* '''By bus''' – [[Havaist]] runs to {{km|20}} from the airport "
                + "until 01:00. See [https://havaist.example Havaist timetable].";
        String out = PracticalInfoService.stripMarkup(wiki);
        assertTrue(out.contains("Havaist"));
        assertTrue(out.contains("01:00"), "a time qualifier must survive stripping");
        assertFalse(out.contains("'''"));
        assertFalse(out.contains("http"));
    }

    @Test
    void extractsASectionUpToTheNextHeadingOfSameLevel() {
        String wiki = "== Get in ==\nintro\n=== By plane ===\nairport detail here\n"
                + "=== By train ===\ntrain detail\n== Get around ==\nlocal transport";
        String plane = PracticalInfoService.extractSection(wiki, "By plane");
        assertNotNull(plane);
        assertTrue(plane.contains("airport detail here"));
        assertFalse(plane.contains("train detail"), "bled into the following section");
        assertFalse(plane.contains("local transport"));
    }

    @Test
    void returnsNullForAMissingSection() {
        assertNull(PracticalInfoService.extractSection("== Get in ==\ntext", "Connect"));
    }

    @Test
    void removesBareThumbCaptionsThatReadAsInstructions() {
        // Real Chiang Mai markup: the caption survived the bracket strip and
        // "thumb|A songthaew serves as a bus" appeared as transport guidance.
        String out = PracticalInfoService.stripMarkup(
                "r 400 baht per 3 days.\n\nthumb|A songthaew serves as a bus or a taxi.\n\nThe traditional way");
        assertFalse(out.contains("thumb"));
        assertTrue(out.contains("traditional way"));
    }

    @Test
    void rendersSubHeadingsAsPlainLabelsNotBullets() {
        String out = PracticalInfoService.stripMarkup("* item\n===Markets===\n* another");
        assertFalse(out.contains("==="), "wiki heading syntax leaked");
        assertFalse(out.contains("• ==="), "heading collected a bullet");
        assertTrue(out.contains("Markets"), "the heading text is useful and should survive");
    }

    @Test
    void collapsesRepeatedBulletMarkers() {
        String out = PracticalInfoService.stripMarkup("**** deeply nested item");
        assertFalse(out.contains("• • •"));
        assertTrue(out.contains("deeply nested item"));
    }
}
