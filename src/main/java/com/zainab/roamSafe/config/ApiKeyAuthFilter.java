package com.zainab.roamSafe.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Header-key authentication for the two machine-facing namespaces.
 *
 * /api/v1 is the partner read API. /api/admin is the ingestion and maintenance
 * surface the data pipeline scripts call, and it is a different trust level
 * entirely: those endpoints write, delete and re-approve safety data, so they
 * get their own key rather than sharing the partner one.
 *
 * Neither key has a default. An unset key authenticates nobody, so a deployment
 * that forgot to configure one is locked rather than open on a value that is
 * printed in this file's history. That is the failure mode we want: a missing
 * key should stop the ingestion scripts, not admit the internet.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-KEY";

    /** Partner read access to /api/v1. */
    @Value("${roamsafe.api.key:}")
    private String partnerApiKey;

    /** Write access to /api/admin — ingestion, seeding, destructive maintenance. */
    @Value("${roamsafe.admin.key:}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String presented = request.getHeader(HEADER);

        // Admin first: /api/admin must never be satisfiable by a partner key.
        //
        // The role is ADMIN_API rather than ADMIN, so this namespace is reachable
        // only by presenting the key - a signed-in admin's browser session does
        // not carry it. That is what keeps the CSRF exemption on /api/** safe
        // here: with no ambient credential a browser can be tricked into
        // replaying, a cross-site POST to the destructive endpoints has nothing
        // to authenticate with.
        if (uri.startsWith("/api/admin")) {
            if (matches(adminApiKey, presented)) {
                authenticate("roamsafe-admin-key", "ROLE_ADMIN_API");
            }
        } else if (uri.startsWith("/api/v1")) {
            if (matches(partnerApiKey, presented)) {
                authenticate("TripDesk-Partner", "ROLE_PARTNER");
            }
        }
        // No match leaves the context empty, and SecurityConfig rejects it.

        filterChain.doFilter(request, response);
    }

    private static void authenticate(String principal, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Constant-time comparison, and never a match on an unconfigured key.
     *
     * The unconfigured check has to come first: without it, a blank configured
     * key would be matched by a request that simply omits the header.
     */
    private static boolean matches(String configured, String presented) {
        if (configured == null || configured.isBlank() || presented == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                configured.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}
