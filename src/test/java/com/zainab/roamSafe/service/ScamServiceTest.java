package com.zainab.roamSafe.service;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScamServiceTest {

    @Mock
    private ScamReportRepository scamReportRepository;

    @InjectMocks
    private ScamService scamService;

    @Test
    void testGetTotalScams() {
        // Given
        when(scamReportRepository.count()).thenReturn(150L);

        // When
        long totalScams = scamService.getTotalScams();

        // Then
        assertThat(totalScams).isEqualTo(150L);
    }

    @Test
    void testGetApprovedScams() {
        // Given
        List<ScamReport> mockReports = Collections.nCopies(120, new ScamReport());
        when(scamReportRepository.findByStatus(ScamReportStatus.APPROVED)).thenReturn(mockReports);

        // When
        long approvedScams = scamService.getApprovedScams();

        // Then
        assertThat(approvedScams).isEqualTo(120L);
    }

    @Test
    void testGetPendingScams() {
        // Given
        List<ScamReport> mockReports = Collections.nCopies(25, new ScamReport());
        when(scamReportRepository.findByStatus(ScamReportStatus.PENDING)).thenReturn(mockReports);

        // When
        long pendingScams = scamService.getPendingScams();

        // Then
        assertThat(pendingScams).isEqualTo(25L);
    }

    @Test
    void testGetTopCities() {
        // Given
        // Since we stubbed getTopCities to return empty list in the service for now:

        // When
        List<Object[]> topCities = scamService.getTopCities(3);

        // Then
        assertThat(topCities).isEmpty();
    }
}