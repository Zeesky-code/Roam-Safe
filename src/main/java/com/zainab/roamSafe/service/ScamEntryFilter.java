package com.zainab.roamSafe.service;

import java.util.regex.Pattern;

/**
 * Keeps non-scams out of the scam library at import time.
 *
 * A one-off cleanup removed entries that were never scams - natural disasters
 * ("Earthquake Risk" rated severity 9), pure emergency-contact info ("Emergency
 * Number 112") and vague "General Safety in X" platitudes - which had polluted
 * the data and dragged the safety scores. This guard stops the same entries
 * coming back on a re-import, so the cleanup is durable rather than a one-time
 * sweep.
 *
 * It errs toward keeping: anything whose title names a scam mechanism (a scam,
 * overcharging, theft, fraud) is always allowed, even when it also mentions
 * weather ("Taxi Overcharging during Typhoons" is a real scam). Only titles that
 * are unambiguously not scams are rejected.
 */
public final class ScamEntryFilter {

    private ScamEntryFilter() {
    }

    /** Words that mark a real scam or crime, which always overrides a reject. */
    private static final Pattern SCAM_SIGNAL = Pattern.compile(
            "scam|fraud|overcharg|overpric|rip.?off|cheat|deception|two.tier|short.?change|"
                    + "switch|trap|ruse|trick|theft|robbery|pickpocket|snatch|counterfeit|con\\b",
            Pattern.CASE_INSENSITIVE);

    /** Titles that are not scams: disasters, pure emergency-info, vague safety. */
    private static final Pattern NOT_A_SCAM = Pattern.compile(
            "earthquake|typhoon|volcan|tsunami|monsoon|flooding|slippery pavement|"
                    + "^general safety|"
                    + "emergency (number|services contact|help points|social services|response strategy|features)|"
                    + "^emergency services$",
            Pattern.CASE_INSENSITIVE);

    /**
     * True when a title belongs in the scam library. A scam signal wins; only an
     * otherwise clearly non-scam title is rejected.
     */
    public static boolean isScamEntry(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String t = title.trim();
        if (SCAM_SIGNAL.matcher(t).find()) {
            return true;
        }
        return !NOT_A_SCAM.matcher(t).find();
    }
}
