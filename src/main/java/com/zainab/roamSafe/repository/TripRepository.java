package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserIdOrderByStartDateAsc(Long userId);

    /** Trips that haven't finished yet, soonest first. */
    List<Trip> findByUserIdAndEndDateGreaterThanEqualOrderByStartDateAsc(Long userId, LocalDate today);
}
