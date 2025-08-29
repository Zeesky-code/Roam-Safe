package com.zainab.roamSafe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "google.sheets.webhook.url=http://localhost:8080/test-webhook"
})
class RoamSafeApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
    }
}