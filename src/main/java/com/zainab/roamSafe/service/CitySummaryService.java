package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.CitySummary;
import com.zainab.roamSafe.repository.CitySummaryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CitySummaryService {

    private final CitySummaryRepository citySummaryRepository;

    public CitySummaryService(CitySummaryRepository citySummaryRepository) {
        this.citySummaryRepository = citySummaryRepository;
    }

    public CitySummary getSummaryForCity(String cityName) {
        Optional<CitySummary> summary = citySummaryRepository.findByCityIgnoreCase(cityName);
        if (summary.isPresent()) {
            return summary.get();
        } else {
            // Return a placeholder or empty summary if not found
            // In production, might trigger Generation here
            return new CitySummary(cityName,
                    "No summary available for this city yet. Check back soon or view individual reports below.");
        }
    }
}
