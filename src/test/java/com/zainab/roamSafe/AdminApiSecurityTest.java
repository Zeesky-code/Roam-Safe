package com.zainab.roamSafe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The ingestion namespace must never be anonymously reachable.
 *
 * /api/admin/** was permitAll, which left POST /api/admin/seed/clear - a
 * delete-everything on reports and cities - open to anyone who found the path,
 * and POST /api/admin/seed/bulk able to inject auto-approved safety reports
 * into a product whose whole claim is that its data is traceable. These tests
 * exist so that rule cannot quietly regress.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "roamsafe.admin.key=test-admin-key",
        "roamsafe.api.key=test-partner-key"
})
class AdminApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void destructiveSeedEndpointRejectsAnonymousCallers() throws Exception {
        mockMvc.perform(post("/api/admin/seed/clear"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkImportRejectsAnonymousCallers() throws Exception {
        mockMvc.perform(post("/api/admin/seed/bulk")
                .contentType("application/json")
                .content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutatingGetEndpointsRejectAnonymousCallers() throws Exception {
        // Several maintenance endpoints are GETs that write. They are behind the
        // same gate, and a bare GET is the easiest thing for a crawler to hit.
        mockMvc.perform(get("/api/admin/seed/merge-cities"))
                .andExpect(status().isForbidden());
    }

    @Test
    void partnerKeyCannotReachTheAdminNamespace() throws Exception {
        mockMvc.perform(post("/api/admin/seed/clear")
                .header("X-API-KEY", "test-partner-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongAdminKeyIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/seed/clear")
                .header("X-API-KEY", "not-the-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void partnerApiStillRequiresItsKey() throws Exception {
        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isForbidden());
    }

    /**
     * The gate has to still let the ingestion pipeline in. Locking the endpoints
     * so thoroughly that scripts/upload_chunked.py can no longer reach them
     * would be a quieter failure than leaving them open, and easy to miss until
     * the next scrape.
     *
     * Safe to run destructively: the test profile uses an in-memory database.
     */
    @Test
    void correctAdminKeyIsAdmitted() throws Exception {
        mockMvc.perform(post("/api/admin/seed/clear")
                .header("X-API-KEY", "test-admin-key"))
                .andExpect(status().isOk());
    }

    /**
     * The map page reads its markers from a same-origin endpoint, so it no
     * longer has to ship an API key to every visitor. That endpoint has to stay
     * open, or the map silently renders empty.
     */
    @Test
    void mapSignalsArePubliclyReadable() throws Exception {
        mockMvc.perform(get("/map/signals"))
                .andExpect(status().isOk());
    }
}
