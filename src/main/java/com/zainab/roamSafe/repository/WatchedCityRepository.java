package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.WatchedCity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchedCityRepository extends JpaRepository<WatchedCity, Long> {

    List<WatchedCity> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<WatchedCity> findByUserIdAndCityNameIgnoreCase(Long userId, String cityName);

    void deleteByUserIdAndCityNameIgnoreCase(Long userId, String cityName);
}
