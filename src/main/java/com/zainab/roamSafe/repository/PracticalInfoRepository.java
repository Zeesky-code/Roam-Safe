package com.zainab.roamSafe.repository;

import com.zainab.roamSafe.model.PracticalInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticalInfoRepository extends JpaRepository<PracticalInfo, Long> {

    List<PracticalInfo> findByCityNameIgnoreCase(String cityName);

    Optional<PracticalInfo> findByCityNameIgnoreCaseAndTopic(String cityName, String topic);
}
