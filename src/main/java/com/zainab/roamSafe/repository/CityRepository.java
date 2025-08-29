package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.City;
import com.zainab.roamSafe.model.Scam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    City findByName(String name);
    
    City findByNameAndCountry(String name, String country);
    
    @Query("SELECT s FROM Scam s JOIN s.cities c WHERE LOWER(c.name) = LOWER(:cityName)")
    List<Scam> findScamsByCityName(@Param("cityName") String cityName);
}