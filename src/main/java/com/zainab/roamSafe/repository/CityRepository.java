package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.City;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    City findByName(String name);

    City findByNameAndCountry(String name, String country);

}