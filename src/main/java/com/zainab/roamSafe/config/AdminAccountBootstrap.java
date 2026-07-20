package com.zainab.roamSafe.config;

import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.model.UserRole;
import com.zainab.roamSafe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Creates a standing admin account from the environment on startup.
 *
 * Credentials are read from ADMIN_EMAIL and ADMIN_PASSWORD and never live in the
 * repository: this is a public repo, and a default admin password committed to
 * it is a working key to every deployment that forgot to change it. With the
 * variables unset the bootstrap does nothing at all, so an environment that
 * hasn't opted in has no admin account rather than a guessable one.
 *
 * Re-running is safe. An existing account is promoted to ADMIN and its password
 * reset to the configured value, which doubles as the way to recover access
 * after a forgotten password.
 */
@Component
public class AdminAccountBootstrap {

    /** Shortest password this will accept, to stop a trivial one being set. */
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    public AdminAccountBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAdminAccount() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return; // Not configured: deliberately leaves no admin account.
        }
        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            System.out.println("[admin] ADMIN_PASSWORD is shorter than " + MIN_PASSWORD_LENGTH
                    + " characters; refusing to create the admin account.");
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        Optional<User> existing = userRepository.findByEmail(email);

        User user = existing.orElseGet(() -> {
            User fresh = new User();
            fresh.setEmail(email);
            fresh.setFirstName("Admin");
            fresh.setLastName("");
            fresh.setCreatedAt(LocalDateTime.now());
            return fresh;
        });

        user.setPassword(passwordEncoder.encode(adminPassword));
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        user.setEmailVerified(true);
        // Give the test account the paid experience, so admin sign-in exercises
        // the same views a Nomad subscriber sees rather than the paywalled ones.
        user.setPro(true);
        user.setProPlan("nomad");
        user.setSubscriptionExpiry(LocalDateTime.now().plusYears(10));

        userRepository.save(user);
        System.out.println("[admin] Admin account ready: " + email
                + (existing.isPresent() ? " (updated)" : " (created)"));
    }
}
