package com.zainab.roamSafe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness endpoints for platform health checks (Brimble, Railway, Fly, ...).
 *
 * Deliberately does no database or network work: a health probe should answer
 * instantly and only report that the process is up and serving. Hitting "/"
 * instead makes the probe depend on the database being reachable and on a full
 * page render, which is slow on a cold start and fails the check even when the
 * app is healthy.
 *
 * Several common probe paths are mapped because platforms differ in which one
 * they call.
 */
@RestController
public class HealthController {

    @GetMapping({ "/health", "/healthz", "/ping", "/_health" })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
