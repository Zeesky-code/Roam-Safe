package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.EmergencyNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmergencyNumberRepository extends JpaRepository<EmergencyNumber, Long> {

    Optional<EmergencyNumber> findByCountryIgnoreCase(String country);
}
