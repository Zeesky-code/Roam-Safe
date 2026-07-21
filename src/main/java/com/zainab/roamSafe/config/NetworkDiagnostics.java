package com.zainab.roamSafe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Answers, from inside the container, why the database is unreachable.
 *
 * The same image reaches Neon in 185ms on Render and times out after 10s on
 * Brimble, so the difference is in the network path and nothing observable from
 * a laptop will explain it. This resolves the database host, prints every
 * address it gets with its family, and opens a plain TCP connection to each one
 * separately - which distinguishes the possibilities that all look identical
 * through a JDBC timeout:
 *
 *   - every address refused quickly    -> egress blocked
 *   - IPv6 hangs, IPv4 connects        -> the JVM is picking IPv6 for an
 *                                         endpoint that only answers on IPv4
 *   - resolution fails                 -> container DNS
 *   - all connect fine                 -> the network is innocent and the
 *                                         problem is TLS or Postgres itself
 *
 * Runs off the startup path on a background thread, uses short timeouts, and
 * only opens and closes sockets - it sends nothing and cannot affect the app.
 * Enabled with NETWORK_DIAGNOSTICS=true so it stays off unless asked for.
 */
@Component
public class NetworkDiagnostics {

    /** Long enough to distinguish a hang from a refusal, short enough to end. */
    private static final int PROBE_TIMEOUT_MS = 8000;

    private static final Pattern HOST_PORT = Pattern.compile("//([^/:?]+)(?::(\\d+))?");

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Value("${network.diagnostics.enabled:false}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void probe() {
        if (!enabled) {
            return;
        }
        Thread.ofVirtual().name("net-diag").start(this::run);
    }

    private void run() {
        Matcher matcher = HOST_PORT.matcher(jdbcUrl == null ? "" : jdbcUrl);
        if (!matcher.find()) {
            System.out.println("[net] Could not parse a host out of the datasource URL.");
            return;
        }
        String host = matcher.group(1);
        int port = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 5432;

        System.out.println("[net] ---- network diagnostics ----");
        System.out.println("[net] target: " + host + ":" + port);
        System.out.println("[net] java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));

        InetAddress[] addresses;
        long dnsStart = System.currentTimeMillis();
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (Exception e) {
            System.out.println("[net] DNS FAILED after " + (System.currentTimeMillis() - dnsStart)
                    + " ms: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("[net] The container cannot resolve the database host. This is DNS,");
            System.out.println("[net] not the database and not the application.");
            return;
        }
        System.out.println("[net] DNS resolved " + addresses.length + " address(es) in "
                + (System.currentTimeMillis() - dnsStart) + " ms");

        int reachable = 0;
        for (InetAddress address : addresses) {
            String family = address instanceof java.net.Inet6Address ? "IPv6" : "IPv4";
            long started = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address, port), PROBE_TIMEOUT_MS);
                long took = System.currentTimeMillis() - started;
                reachable++;
                System.out.println("[net]   " + family + " " + address.getHostAddress()
                        + " -> CONNECTED in " + took + " ms");
            } catch (Exception e) {
                long took = System.currentTimeMillis() - started;
                // A timeout means packets vanished; a refusal means something
                // answered and said no. They point at different causes.
                String verdict = took >= PROBE_TIMEOUT_MS - 200 ? "TIMED OUT (no response)" : "FAILED";
                System.out.println("[net]   " + family + " " + address.getHostAddress()
                        + " -> " + verdict + " after " + took + " ms: " + e.getClass().getSimpleName());
            }
        }

        System.out.println("[net] " + reachable + " of " + addresses.length + " address(es) reachable.");
        if (reachable == 0) {
            System.out.println("[net] VERDICT: this container cannot open a TCP connection to the");
            System.out.println("[net] database on any resolved address. The application, the driver");
            System.out.println("[net] and the credentials are not involved - nothing got that far.");
        } else if (reachable < addresses.length) {
            System.out.println("[net] VERDICT: some addresses work and others do not. If the failing");
            System.out.println("[net] ones are IPv6, set -Djava.net.preferIPv4Stack=true.");
        } else {
            System.out.println("[net] VERDICT: TCP is fine to every address, so a JDBC failure here is");
            System.out.println("[net] TLS or Postgres-level, not connectivity.");
        }
        System.out.println("[net] ---- end ----");
    }
}
