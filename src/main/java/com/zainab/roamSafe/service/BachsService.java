package com.zainab.roamSafe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the Bachs payments API (https://api.bachs.io/v1).
 *
 * Bachs is a merchant-of-record style processor built for African internet
 * businesses; unlike Stripe it settles to Nigerian bank accounts. Flow:
 * find/create a customer, create a checkout session for a pre-created product,
 * redirect the user to the returned checkout_url, then confirm via webhook.
 */
@Service
public class BachsService {

    private static final String PRODUCTION_URL = "https://api.bachs.io/v1";
    private static final String SANDBOX_URL = "https://sandbox-api.bachs.io/v1";

    @Value("${bachs.api.url:}")
    private String configuredUrl;

    @Value("${bachs.api.key}")
    private String apiKey;

    @Value("${bachs.webhook.secret:}")
    private String webhookSecret;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Resolve the API host from the key prefix (sandbox keys go to the sandbox
     * host) unless an explicit bachs.api.url override is set. Mirrors the
     * official Bachs client, so switching between a live and a sandbox key needs
     * no other config change.
     */
    private String apiUrl() {
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        return apiKey != null && apiKey.startsWith("sk_sandbox_") ? SANDBOX_URL : PRODUCTION_URL;
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(apiKey);
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("User-Agent", "RoamSafe/1.0");
        return h;
    }

    /** Create a Bachs customer for this email; returns the customer_id. */
    public String createCustomer(String email, String name) {
        Map<String, Object> body = Map.of("email", email, "name", name == null ? email : name);
        ResponseEntity<JsonNode> resp = rest.exchange(apiUrl() + "/customers", HttpMethod.POST,
                new HttpEntity<>(body, headers()), JsonNode.class);
        JsonNode b = resp.getBody();
        return b != null ? b.path("customer_id").asText(null) : null;
    }

    /**
     * Create a checkout session for one product and return the hosted
     * checkout_url to redirect the buyer to. Metadata is echoed back on the
     * webhook, so we stash the app user id and plan there.
     */
    public String createCheckoutUrl(String customerId, String productId, Map<String, String> metadata) {
        Map<String, Object> body = Map.of(
                "customer", Map.of("customer_id", customerId),
                "product_cart", List.of(Map.of("product_id", productId, "quantity", 1)),
                "metadata", metadata);
        ResponseEntity<JsonNode> resp = rest.exchange(apiUrl() + "/checkout-sessions", HttpMethod.POST,
                new HttpEntity<>(body, headers()), JsonNode.class);
        JsonNode b = resp.getBody();
        return b != null ? b.path("checkout_url").asText(null) : null;
    }

    /**
     * Verify a Bachs webhook signature:
     * {@code HMAC-SHA256(timestamp + "." + rawBody, webhookSecret)} compared to
     * the X-Bachs-Signature header, within a 300s timestamp tolerance.
     * Returns true when no secret is configured (local dev).
     */
    public boolean verifyWebhook(String rawBody, String timestamp, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true; // dev: signature checking disabled
        }
        if (timestamp == null || signature == null) {
            return false;
        }
        try {
            long ts = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() / 1000 - ts) > 300) {
                return false;
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
            StringBuilder expected = new StringBuilder();
            for (byte x : digest) {
                expected.append(String.format("%02x", x));
            }
            return constantTimeEquals(expected.toString(), signature);
        } catch (Exception e) {
            return false;
        }
    }

    public JsonNode parse(String json) throws Exception {
        return mapper.readTree(json);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
