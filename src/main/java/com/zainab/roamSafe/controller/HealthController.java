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

    private final javax.sql.DataSource dataSource;

    @org.springframework.beans.factory.annotation.Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public HealthController(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping({ "/health", "/healthz", "/ping", "/_health" })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Readiness: does this instance actually reach its database?
     *
     * Separate from liveness on purpose. Liveness must stay dependency-free so a
     * platform probe can't be failed by a slow database, but that leaves no way
     * to tell "the app is up" from "the app is up and useless". This answers
     * that in one request from outside the box, which is otherwise guesswork
     * when all you have is a 503 from someone else's router.
     *
     * Reports the host it tried, never the credentials, so it can be shared in a
     * support thread safely.
     */
    @GetMapping("/health/db")
    public ResponseEntity<java.util.Map<String, Object>> database() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("target", hostOf(datasourceUrl));

        long started = System.currentTimeMillis();
        try (java.sql.Connection connection = dataSource.getConnection();
                java.sql.Statement statement = connection.createStatement()) {
            statement.execute("select 1");
            body.put("database", "reachable");
            body.put("tookMs", System.currentTimeMillis() - started);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("database", "unreachable");
            body.put("tookMs", System.currentTimeMillis() - started);
            body.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            if (String.valueOf(datasourceUrl).contains("localhost")) {
                body.put("likelyCause",
                        "DB_URL is not set on this environment, so the app fell back to localhost. "
                                + "Set DB_URL, DB_USER and DB_PASS and redeploy.");
            }
            return ResponseEntity.status(503).body(body);
        }
    }

    /** Host and database from a JDBC URL, with any credentials stripped. */
    private static String hostOf(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "<no url configured>";
        }
        String stripped = jdbcUrl.replaceAll("(?i)(password|user)=[^&]*", "$1=***");
        int start = stripped.indexOf("://");
        return start < 0 ? stripped : stripped.substring(start + 3);
    }
}
