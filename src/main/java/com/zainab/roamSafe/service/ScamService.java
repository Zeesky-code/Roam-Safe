package com.zainab.roamSafe.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.zainab.roamSafe.repository.ScamRepository;
import com.zainab.roamSafe.repository.SubmittedScamRepository;

@Service
public class ScamService {
    private final ScamRepository scamRepository;
    private final SubmittedScamRepository submittedScamRepository;

    public ScamService(ScamRepository scamRepository, SubmittedScamRepository submittedScamRepository) {
        this.scamRepository = scamRepository;
        this.submittedScamRepository = submittedScamRepository;
    }

    public long getTotalScams() {
        return scamRepository.count();
    }
    
    public long getApprovedScams() {
        return scamRepository.count(); // All scams in main table are approved
    }
    
    public long getPendingScams() {
        return submittedScamRepository.countByReviewed(false); // Count unreviewed submissions
    }
    
    public List<Object[]> getTopCities(int limit) {
        return scamRepository.findTopCities(PageRequest.of(0, limit));
    }
}