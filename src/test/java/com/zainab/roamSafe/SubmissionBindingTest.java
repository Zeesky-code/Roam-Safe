package com.zainab.roamSafe;

import com.zainab.roamSafe.model.ScamReport;
import com.zainab.roamSafe.model.ScamReportStatus;
import com.zainab.roamSafe.repository.ScamReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The public submission form must only ever create a new report awaiting review.
 * It used to bind the whole entity, so a hand-built POST could publish itself
 * (status=APPROVED) or overwrite an existing report (id=...).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionBindingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScamReportRepository repository;

    @Test
    void submissionCannotSetItsOwnStatusOrOverwriteAnotherReport() throws Exception {
        ScamReport existing = new ScamReport();
        existing.setName("Original report");
        existing.setCity("Lisbon");
        existing.setStatus(ScamReportStatus.APPROVED);
        existing.setSeverityScore(5);
        existing.setDescription("text");
        existing.setScamType("Gold ring");
        existing = repository.save(existing);

        mockMvc.perform(post("/submit").with(csrf())
                .param("name", "Injected")
                .param("city", "Lisbon")
                .param("description", "text")
                .param("scamType", "Bracelet")
                .param("safetyRating", "1")
                .param("status", "APPROVED")
                .param("id", String.valueOf(existing.getId())))
                .andExpect(status().isOk());

        assertEquals("Original report", repository.findById(existing.getId()).orElseThrow().getName());
        ScamReport injected = repository.findAll().stream()
                .filter(r -> "Injected".equals(r.getName())).findFirst().orElseThrow();
        assertEquals(ScamReportStatus.PENDING, injected.getStatus());
        assertEquals(10, injected.getSeverityScore(), "rated 1 (dangerous) by the submitter");
        assertTrue(!injected.getId().equals(existing.getId()));
    }
}
