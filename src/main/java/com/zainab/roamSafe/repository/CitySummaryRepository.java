package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.CitySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CitySummaryRepository extends JpaRepository<CitySummary, Long> {
    Optional<CitySummary> findByCityIgnoreCase(String city);
}
