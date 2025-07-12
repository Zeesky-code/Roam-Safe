package com.zainab.roamSafe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zainab.roamSafe.model.SubmittedScam;

public interface SubmittedScamRepository extends JpaRepository<SubmittedScam, Long> {
    List<SubmittedScam> findByReviewedFalse();
    long countByReviewed(boolean reviewed);
}
