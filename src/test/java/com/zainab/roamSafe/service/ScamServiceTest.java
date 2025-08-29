package com.zainab.roamSafe.service;

import com.zainab.roamSafe.repository.ScamRepository;
import com.zainab.roamSafe.repository.SubmittedScamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScamServiceTest {

    @Mock
    private ScamRepository scamRepository;

    @Mock
    private SubmittedScamRepository submittedScamRepository;

    @InjectMocks
    private ScamService scamService;

    @Test
    void testGetTotalScams() {
        // Given
        when(scamRepository.count()).thenReturn(150L);

        // When
        long totalScams = scamService.getTotalScams();

        // Then
        assertThat(totalScams).isEqualTo(150L);
    }

    @Test
    void testGetApprovedScams() {
        // Given
        when(scamRepository.count()).thenReturn(120L);

        // When
        long approvedScams = scamService.getApprovedScams();

        // Then
        assertThat(approvedScams).isEqualTo(120L);
    }

    @Test
    void testGetPendingScams() {
        // Given
        when(submittedScamRepository.countByReviewed(false)).thenReturn(25L);

        // When
        long pendingScams = scamService.getPendingScams();

        // Then
        assertThat(pendingScams).isEqualTo(25L);
    }

    @Test
    void testGetTopCities() {
        // Given
        List<Object[]> mockTopCities = Arrays.asList(
            new Object[]{"Istanbul", 45L},
            new Object[]{"Paris", 32L},
            new Object[]{"London", 28L}
        );
        when(scamRepository.findTopCities(any(PageRequest.class))).thenReturn(mockTopCities);

        // When
        List<Object[]> topCities = scamService.getTopCities(3);

        // Then
        assertThat(topCities).hasSize(3);
        assertThat(topCities.get(0)[0]).isEqualTo("Istanbul");
        assertThat(topCities.get(0)[1]).isEqualTo(45L);
        assertThat(topCities.get(1)[0]).isEqualTo("Paris");
        assertThat(topCities.get(1)[1]).isEqualTo(32L);
        assertThat(topCities.get(2)[0]).isEqualTo("London");
        assertThat(topCities.get(2)[1]).isEqualTo(28L);
    }

    @Test
    void testGetTopCitiesWithLimit() {
        // Given
        List<Object[]> mockTopCities = Arrays.asList(
            new Object[]{"Istanbul", 45L},
            new Object[]{"Paris", 32L}
        );
        when(scamRepository.findTopCities(PageRequest.of(0, 2))).thenReturn(mockTopCities);

        // When
        List<Object[]> topCities = scamService.getTopCities(2);

        // Then
        assertThat(topCities).hasSize(2);
    }

    @Test
    void testGetTopCitiesEmpty() {
        // Given
        when(scamRepository.findTopCities(any(PageRequest.class))).thenReturn(Arrays.asList());

        // When
        List<Object[]> topCities = scamService.getTopCities(5);

        // Then
        assertThat(topCities).isEmpty();
    }
}