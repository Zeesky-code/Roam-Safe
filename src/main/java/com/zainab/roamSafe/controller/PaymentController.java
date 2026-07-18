package com.zainab.roamSafe.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.repository.UserRepository;
import com.zainab.roamSafe.service.BachsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checkout + webhook handling via Bachs (https://bachs.io), which settles to
 * Nigerian bank accounts (Stripe cannot). Two plans: a one-time Trip Pass and a
 * recurring Nomad subscription, each mapped to a pre-created Bachs product.
 */
@Controller
public class PaymentController {

    private static final int TRIP_PASS_DAYS = 14;

    @Value("${bachs.product.trip-pass}")
    private String tripPassProduct;

    @Value("${bachs.product.nomad}")
    private String nomadProduct;

    private final UserRepository userRepository;
    private final BachsService bachs;

    public PaymentController(UserRepository userRepository, BachsService bachs) {
        this.userRepository = userRepository;
        this.bachs = bachs;
    }

    /** Recurring Nomad subscription checkout ($10/mo). */
    @GetMapping("/subscribe")
    public String subscribe(HttpSession httpSession) {
        return startCheckout(httpSession, nomadProduct, "nomad");
    }

    /** One-time Trip Pass checkout ($3, 14 days). */
    @GetMapping("/trip-pass")
    public String tripPass(HttpSession httpSession) {
        return startCheckout(httpSession, tripPassProduct, "trip_pass");
    }

    private String startCheckout(HttpSession httpSession, String productId, String plan) {
        User user = (User) httpSession.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/pricing";
        }
        if (user.isPro()) {
            return "redirect:/pricing?already_pro=true";
        }

        // Reuse the customer we created for this user, or make one.
        String customerId = user.getBachsCustomerId();
        if (customerId == null || customerId.isBlank()) {
            customerId = bachs.createCustomer(user.getEmail(), user.getFullName());
            if (customerId == null) {
                return "redirect:/pricing?canceled=true";
            }
            user.setBachsCustomerId(customerId);
            userRepository.save(user);
        }

        String url = bachs.createCheckoutUrl(customerId, productId, Map.of(
                "user_id", user.getId().toString(),
                "plan", plan));
        if (url == null) {
            return "redirect:/pricing?canceled=true";
        }
        return "redirect:" + url;
    }

    /**
     * Bachs webhook. Grants/renews/revokes Pro from real payment events. We
     * match the app user by the metadata user_id we set at checkout, falling
     * back to the Bachs customer id.
     */
    @PostMapping("/api/bachs/webhook")
    @ResponseBody
    public ResponseEntity<String> webhook(HttpServletRequest request) {
        String body;
        try {
            body = request.getReader().lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("cannot read body");
        }

        // Bachs signs with HMAC and (per the dashboard) sends a "Bachs-Signature"
        // header; the community client used "X-Bachs-Signature". Accept both.
        String signature = firstNonNull(request.getHeader("Bachs-Signature"),
                request.getHeader("X-Bachs-Signature"));
        String timestamp = firstNonNull(request.getHeader("Bachs-Timestamp"),
                request.getHeader("X-Bachs-Timestamp"));
        if (!bachs.verifyWebhook(body, timestamp, signature)) {
            return ResponseEntity.status(400).body("invalid signature");
        }

        JsonNode root;
        try {
            root = bachs.parse(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("bad json");
        }

        String type = root.path("type").asText("").toLowerCase();
        JsonNode data = root.path("data");
        LocalDateTime now = LocalDateTime.now();

        // Subscription lifecycle (Customer Subscription Created/Updated/Deleted).
        if (type.contains("subscription")) {
            if (type.contains("delete") || type.contains("cancel")) {
                revoke(data);
            } else {
                grant(data, "nomad", now.plusMonths(1));
            }
        }
        // Subscription renewal (Invoice Paid) → extend another month.
        else if (type.contains("invoice") && type.contains("paid")) {
            grant(data, "nomad", now.plusMonths(1));
        }
        // A successful payment (Collection Succeeded / Conversion Completed) →
        // grant per the checkout metadata: Trip Pass is one-time, Nomad monthly.
        else if (isSuccess(type, data)) {
            String plan = data.path("metadata").path("plan").asText("trip_pass");
            LocalDateTime expiry = "nomad".equals(plan) ? now.plusMonths(1) : now.plusDays(TRIP_PASS_DAYS);
            grant(data, plan, expiry);
        } else {
            System.out.println("[bachs] Unhandled webhook type: " + type);
        }

        return ResponseEntity.ok("ok");
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    /** Matches Collection Succeeded, Conversion Completed, *.paid, *.succeeded, *.completed. */
    private static boolean isSuccess(String type, JsonNode data) {
        if (type.contains("succeed") || type.contains("complete") || type.contains("paid")) {
            return true;
        }
        String status = data.path("status").asText("");
        return status.equals("succeeded") || status.equals("completed") || status.equals("paid");
    }

    /** Grant/extend Pro for the user referenced by the event metadata/customer. */
    private void grant(JsonNode data, String plan, LocalDateTime expiry) {
        findUser(data).ifPresent(user -> {
            user.setPro(true);
            user.setProPlan(plan);
            user.setSubscriptionExpiry(expiry);
            String customerId = data.path("customer").path("customer_id").asText(null);
            if (customerId != null && !customerId.isBlank()) {
                user.setBachsCustomerId(customerId);
            }
            userRepository.save(user);
            System.out.println("[bachs] " + user.getEmail() + " -> Pro (" + plan + ")");
        });
    }

    private void revoke(JsonNode data) {
        findUser(data).ifPresent(user -> {
            // Don't cut off a still-valid Trip Pass because a subscription ended.
            if ("nomad".equals(user.getProPlan()) || "subscription".equals(user.getProPlan())) {
                user.setPro(false);
                userRepository.save(user);
                System.out.println("[bachs] " + user.getEmail() + " subscription canceled");
            }
        });
    }

    private java.util.Optional<User> findUser(JsonNode data) {
        String userId = data.path("metadata").path("user_id").asText(null);
        if (userId != null && !userId.isBlank()) {
            try {
                java.util.Optional<User> u = userRepository.findById(Long.parseLong(userId));
                if (u.isPresent()) {
                    return u;
                }
            } catch (NumberFormatException ignored) {
                // fall through to customer-id lookup
            }
        }
        String customerId = data.path("customer").path("customer_id").asText(null);
        if (customerId != null && !customerId.isBlank()) {
            return userRepository.findByBachsCustomerId(customerId);
        }
        return java.util.Optional.empty();
    }
}
