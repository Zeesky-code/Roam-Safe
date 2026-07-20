package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.PoliceCrimeStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PoliceCrimeStatRepository extends JpaRepository<PoliceCrimeStat, Long> {

    Optional<PoliceCrimeStat> findByCityNameAndMonthAndCategory(String cityName, String month, String category);

    List<PoliceCrimeStat> findByCityNameIgnoreCase(String cityName);

    /** Distinct cities that have official police data, for coverage reporting. */
    @Query("select distinct p.cityName from PoliceCrimeStat p order by p.cityName")
    List<String> findDistinctCities();

    @Query("select distinct p.month from PoliceCrimeStat p where lower(p.cityName) = lower(:city) order by p.month")
    List<String> findMonthsForCity(String city);
}
