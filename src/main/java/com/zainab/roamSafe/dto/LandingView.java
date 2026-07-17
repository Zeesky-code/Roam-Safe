package com.zainab.roamSafe.dto;

import java.util.List;

/**
 * View data for the landing page.
 *
 * Everything here is derived from real rows in the database — report counts,
 * computed safety scores and actual traveler reports. The design handoff ships
 * illustrative figures (2.4M reports, 840 sources, sample testimonials); those
 * are deliberately not reproduced, per the product principle that RoamSafe
 * "summarizes, but never invents".
 */
public final class LandingView {

    private LandingView() {
    }

    /** A headline number on the trust strip. */
    public record Stat(String value, String label) {
    }

    /** One risk meter inside the briefing card. */
    public record Meter(String label, int value, String severity) {
    }

    /** The hero briefing card: a real city's computed safety score. */
    public record Briefing(
            String city,
            int score,
            int confidencePct,
            int reports,
            String updated,
            List<Meter> meters) {
    }

    /**
     * A card in the "live intel" grid.
     *
     * {@code tint} is a CSS gradient stand-in rather than a photo: the handoff
     * uses {@code intelTint(city)} gradients for the same slot, and we cannot
     * map an arbitrary city from the database to correct destination
     * photography without mislabelling it.
     */
    public record Intel(
            String city,
            int score,
            String scoreColor,
            int confidencePct,
            int reports,
            String note,
            String tint,
            List<String> tags) {
    }

    /** A floating alert card over the hero panel. */
    public record Alert(
            String city,
            String title,
            String severity,
            String time) {
    }

    /** A command-palette entry. */
    public record PaletteItem(
            String group,
            String title,
            String subtitle,
            String href) {
    }
}
