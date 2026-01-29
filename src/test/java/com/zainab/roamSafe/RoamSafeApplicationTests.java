package com.zainab.roamSafe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "google.sheets.webhook.url=https://script.google.com/macros/s/AKfycbwXzASbmHoB0rFyw7DaFva1TfVzkN7dE9sLGEnMgpe2K5HOq9JlTbo_BvSHX7Uif3Nl/exec",
        "GEMINI_API_KEY=test-key"
})
class RoamSafeApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
    }
}