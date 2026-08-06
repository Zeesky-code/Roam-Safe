package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.CoworkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoworkingSpaceRepository extends JpaRepository<CoworkingSpace, Long> {

    List<CoworkingSpace> findByCityNameIgnoreCaseOrderByName(String cityName);

    Optional<CoworkingSpace> findByOsmId(String osmId);
}
