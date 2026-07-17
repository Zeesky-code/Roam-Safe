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

    private static final int TRIP_PASS_DAYS = 14;

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
     * Creates a Stripe Checkout Session for the recurring Nomad subscription and
     * redirects the user to the hosted payment page. Nomad is $10/mo, or the
     * same billed yearly at a 25% discount.
     */
    @GetMapping("/subscribe")
    public String createCheckoutSession(
            @RequestParam(name = "billing", defaultValue = "monthly") String billing,
            HttpSession httpSession) throws StripeException {
        User user = (User) httpSession.getAttribute("user");

        if (user == null) {
            return "redirect:/login?redirect=/pricing";
        }

        // Already pro? Redirect back.
        if (user.isPro()) {
            return "redirect:/pricing?already_pro=true";
        }

        boolean yearly = "yearly".equals(billing);
        long monthlyCents = 1000L; // $10.00
        long unitAmount = yearly ? Math.round(monthlyCents * 12 * 0.75) : monthlyCents;
        SessionCreateParams.LineItem.PriceData.Recurring.Interval interval = yearly
                ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR
                : SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH;

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
                                                .setUnitAmount(unitAmount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("RoamSafe Nomad")
                                                                .setDescription(
                                                                        "Unlimited reports & cities, real-time alerts, neighborhood scores")
                                                                .build())
                                                .setRecurring(
                                                        SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                .setInterval(interval)
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("user_id", user.getId().toString())
                .putMetadata("plan", "subscription")
                .putMetadata("tier", "nomad")
                .putMetadata("billing", yearly ? "yearly" : "monthly")
                .build();

        Session session = Session.create(params);
        return "redirect:" + session.getUrl();
    }

    /**
     * Creates a one-time Stripe Checkout Session for a Trip Pass — a fixed
     * window of Pro access with no recurring billing, for travelers who only
     * need it for a single trip.
     */
    @GetMapping("/trip-pass")
    public String createTripPassCheckoutSession(HttpSession httpSession) throws StripeException {
        User user = (User) httpSession.getAttribute("user");

        if (user == null) {
            return "redirect:/login?redirect=/pricing";
        }

        if (user.isPro()) {
            return "redirect:/pricing?already_pro=true";
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(user.getEmail())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(300L) // $3.00
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("RoamSafe Trip Pass")
                                                                .setDescription(
                                                                        TRIP_PASS_DAYS
                                                                                + " days of full Pro access for one trip — no subscription")
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("user_id", user.getId().toString())
                .putMetadata("plan", "trip_pass")
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

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "invoice.paid" -> handleInvoicePaid(event);
            case "customer.subscription.deleted" -> handleSubscriptionCanceled(event);
            default -> {
            }
        }

        return ResponseEntity.ok("Received");
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            return;
        }

        String userId = session.getMetadata().get("user_id");
        String plan = session.getMetadata().getOrDefault("plan", "subscription");
        if (userId == null) {
            return;
        }

        userRepository.findById(Long.parseLong(userId)).ifPresent(user -> {
            user.setPro(true);
            user.setProPlan(plan);
            user.setStripeCustomerId(session.getCustomer());
            LocalDateTime expiry = "trip_pass".equals(plan)
                    ? LocalDateTime.now().plusDays(TRIP_PASS_DAYS)
                    : LocalDateTime.now().plusMonths(1);
            user.setSubscriptionExpiry(expiry);
            userRepository.save(user);
            System.out.println("[STRIPE] User " + user.getEmail() + " upgraded to Pro (" + plan + ")!");
        });
    }

    /**
     * Recurring subscriptions bill monthly; each successful invoice extends
     * access by another month so Pro doesn't lapse between billing cycles.
     * Trip Pass purchases are one-time payments and never raise invoices, so
     * this only ever applies to subscription customers.
     */
    private void handleInvoicePaid(Event event) {
        com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (invoice == null || invoice.getCustomer() == null) {
            return;
        }

        userRepository.findByStripeCustomerId(invoice.getCustomer()).ifPresent(user -> {
            user.setPro(true);
            user.setProPlan("subscription");
            user.setSubscriptionExpiry(LocalDateTime.now().plusMonths(1));
            userRepository.save(user);
            System.out.println("[STRIPE] User " + user.getEmail() + " subscription renewed!");
        });
    }

    private void handleSubscriptionCanceled(Event event) {
        com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (subscription == null || subscription.getCustomer() == null) {
            return;
        }

        userRepository.findByStripeCustomerId(subscription.getCustomer()).ifPresent(user -> {
            // Don't cut off a still-valid Trip Pass just because the user also
            // once had (and canceled) a subscription on the same customer id.
            if ("subscription".equals(user.getProPlan())) {
                user.setPro(false);
                userRepository.save(user);
                System.out.println("[STRIPE] User " + user.getEmail() + " subscription canceled.");
            }
        });
    }
}
