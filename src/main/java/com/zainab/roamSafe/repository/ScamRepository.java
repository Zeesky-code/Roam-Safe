package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.Scam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScamRepository extends JpaRepository<Scam, Long> {
    @Query("SELECT s FROM Scam s JOIN s.cities c WHERE LOWER(c.name) = LOWER(:cityName)")
    List<Scam> findByCityName(@Param("cityName") String cityName);
} 
