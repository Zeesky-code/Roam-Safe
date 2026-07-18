package com.zainab.roamSafe.service;

import org.springframework.stereotype.Service;

/**
 * Deprecated. This service previously inserted hardcoded, fabricated "News"
 * alerts on an hourly schedule, which violated the product's core promise that
 * RoamSafe "summarizes, but never invents." The fabrication has been removed.
 *
 * Real, sourced intelligence now comes from {@link AdvisoryIngestionService}
 * (government travel advisories) and the Wikivoyage import pipeline. This class
 * is retained only so existing callers keep compiling; it performs no work.
 */
@Service
public class NewsIngestionService {

    /**
     * No-op. Kept for backward compatibility with the seed/admin trigger.
     * Real ingestion lives in {@link AdvisoryIngestionService}.
     */
    public void ingestFreshScamData() {
        System.out.println("[news] Mock news ingestion is disabled. "
                + "Use AdvisoryIngestionService for real, sourced data.");
    }
}
