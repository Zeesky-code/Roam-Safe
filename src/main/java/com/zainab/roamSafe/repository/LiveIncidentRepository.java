package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.LiveIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LiveIncidentRepository extends JpaRepository<LiveIncident, Long> {

    Optional<LiveIncident> findBySourceUrl(String sourceUrl);

    List<LiveIncident> findByCityNameIgnoreCaseOrderByPublishedAtDesc(String cityName);

    List<LiveIncident> findTop20ByOrderByPublishedAtDesc();

    /** Stories older than the cutoff, for pruning: a strike last month isn't news. */
    List<LiveIncident> findByPublishedAtBefore(LocalDateTime cutoff);
}
