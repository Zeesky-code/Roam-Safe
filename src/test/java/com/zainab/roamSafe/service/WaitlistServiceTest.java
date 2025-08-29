package com.zainab.roamSafe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WaitlistServiceTest {

    private WaitlistService waitlistService;

    @BeforeEach
    void setUp() {
        waitlistService = new WaitlistService();
        // Set a test webhook URL to avoid null pointer exceptions
        ReflectionTestUtils.setField(waitlistService, "googleSheetsWebhookUrl", "http://test-webhook.com");
    }

    @Test
    void testGetWaitlistCountInitiallyZero() {
        assertThat(waitlistService.getWaitlistCount()).isEqualTo(0);
    }

    @Test
    void testClearCache() {
        // Clear cache should work even when empty
        waitlistService.clearCache();
        assertThat(waitlistService.getWaitlistCount()).isEqualTo(0);
    }

    @Test
    void testWaitlistServiceCreation() {
        // Test that service can be created without errors
        WaitlistService service = new WaitlistService();
        assertThat(service).isNotNull();
        assertThat(service.getWaitlistCount()).isEqualTo(0);
    }
}