package com.zainab.roamSafe.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Reports which database this instance uses, and wakes it once at startup.
 *
 * Both halves exist because of the same incident. The database is Neon, which
 * suspends its compute after a few minutes with no connections from anyone -
 * that state is shared, not per-client - and waking it takes seconds. Deployed
 * instances always hit a suspended database, because nothing else is touching
 * it between deploys, while local runs never did, because testing kept it
 * permanently awake. The result was a connection timeout that only ever
 * appeared in production.
 *
 * So: log the host being used (never the password), and open one connection
 * after startup so the wake happens on our time rather than on the first
 * visitor's. Also shouts if the URL is localhost, which means DB_URL wasn't set
 * and the instance is pointed at a database inside its own container.
 */
@Component
@Profile("prod")
public class DatasourceCheck {

    private final DataSource dataSource;

    @Value("${spring.datasource.url:}")
    private String url;

    @Value("${spring.datasource.username:}")
    private String username;

    public DatasourceCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void report() {
        String host = hostOf(url);
        System.out.println("[db] Configured target: " + host
                + " as " + (username.isBlank() ? "<unset>" : username));

        if (host.contains("localhost") || host.contains("127.0.0.1")) {
            System.out.println("""
                    [db] *** DB_URL is not set. ***
                    [db] This instance is pointed at a database inside its own container,
                    [db] which almost certainly does not exist. Set DB_URL, DB_USER and
                    [db] DB_PASS for THIS environment and redeploy - variables scoped to
                    [db] one environment are not visible in another.""");
        }
    }

    /**
     * Opens one connection after startup, on a background thread.
     *
     * Off the boot path deliberately: waking a suspended database can take
     * longer than a platform's health-check window, and a deploy must not fail
     * because the database was asleep. Liveness and data-availability are
     * separate questions - /health answers the first without touching the
     * database, /health/db answers the second.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Thread.ofVirtual().name("db-warmup").start(() -> {
            long started = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("select 1");
                System.out.println("[db] Connection established in "
                        + (System.currentTimeMillis() - started) + " ms.");
            } catch (Exception e) {
                // Not fatal: the app still serves pages that need no data, and
                // staying up means /health/db can be asked what went wrong.
                System.out.println("[db] *** Could not reach the database after "
                        + (System.currentTimeMillis() - started) + " ms: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " *** Check /health/db for details.");
            }
        });
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
