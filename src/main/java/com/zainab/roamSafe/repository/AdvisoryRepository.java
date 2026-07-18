package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.Advisory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvisoryRepository extends JpaRepository<Advisory, Long> {

    List<Advisory> findByCountryName(String countryName);

    Optional<Advisory> findByCountryNameAndSource(String countryName, String source);

    List<Advisory> findBySeverityIn(List<String> severities);
}
