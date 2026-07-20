package com.zainab.roamSafe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "google.sheets.webhook.url=https://script.google.com/macros/s/AKfycbwXzASbmHoB0rFyw7DaFva1TfVzkN7dE9sLGEnMgpe2K5HOq9JlTbo_BvSHX7Uif3Nl/exec",
        "GEMINI_API_KEY=test-key"
})
class RoamSafeApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the whole bean graph wires up. This is the cheapest guard
        // against a broken constructor or a missing dependency after adding a
        // service, and it now runs against an in-memory database so it doesn't
        // depend on a Postgres being installed.
    }
}