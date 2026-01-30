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
import java.util.Collections;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${roamsafe.api.key:roamsafe-secret-key-123}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only checking /api/v1 paths
        if (request.getRequestURI().startsWith("/api/v1")) {
            String requestApiKey = request.getHeader("X-API-KEY");

            if (validApiKey.equals(requestApiKey)) {
                // Determine privilege level (Partner)
                // In production, we'd lookup the key in DB to see which partner it is
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "TripDesk-Partner", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTNER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // Invalid or missing key
                // Continue chain, but context is null, so security config will reject if
                // configured
            }
        }

        filterChain.doFilter(request, response);
    }
}
