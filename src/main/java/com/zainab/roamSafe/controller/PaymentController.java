package com.zainab.roamSafe.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class PaymentController {

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @Value("${stripe.webhook.secret:}")
    private String endpointSecret;

    private final UserRepository userRepository;

    public PaymentController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a Stripe Checkout Session and redirects the user to the hosted
     * payment page.
     */
    @GetMapping("/subscribe")
    public String createCheckoutSession(HttpSession httpSession) throws StripeException {
        User user = (User) httpSession.getAttribute("user");

        if (user == null) {
            return "redirect:/login?redirect=/pricing";
        }

        // Already pro? Redirect back.
        if (user.isPro()) {
            return "redirect:/pricing?already_pro=true";
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(user.getEmail())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(500L) // $5.00
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("RoamSafe Traveler Pro")
                                                                .setDescription(
                                                                        "Unlimited reports, email alerts, verified badge")
                                                                .build())
                                                .setRecurring(
                                                        SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                .setInterval(
                                                                        SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("user_id", user.getId().toString())
                .build();

        Session session = Session.create(params);
        return "redirect:" + session.getUrl();
    }

    /**
     * Stripe Webhook endpoint. Listens for checkout.session.completed events
     * to activate user subscriptions.
     */
    @PostMapping("/api/stripe/webhook")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) {
        String payload;
        try {
            payload = request.getReader().lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Unable to read request body");
        }

        String sigHeader = request.getHeader("Stripe-Signature");

        Event event;
        try {
            if (endpointSecret != null && !endpointSecret.isEmpty()) {
                event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            } else {
                // For development without webhook secret verification
                event = com.stripe.net.ApiResource.GSON.fromJson(payload, Event.class);
            }
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (session != null) {
                String userId = session.getMetadata().get("user_id");
                if (userId != null) {
                    Optional<User> optUser = userRepository.findById(Long.parseLong(userId));
                    optUser.ifPresent(user -> {
                        user.setPro(true);
                        user.setStripeCustomerId(session.getCustomer());
                        user.setSubscriptionExpiry(LocalDateTime.now().plusMonths(1));
                        userRepository.save(user);
                        System.out.println("[STRIPE] User " + user.getEmail() + " upgraded to Pro!");
                    });
                }
            }
        }

        return ResponseEntity.ok("Received");
    }
}
