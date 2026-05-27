package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

/**
 * Verifies that WebSocket handshake authentication is actually enforced in multi-user mode.
 *
 * <p>The {@code /rest/ws} endpoint is served by the {@code /rest/**} httpBasic security chain
 * ({@code org.openl.studio.security.FormBasedAuthenticationConfig}), so an unauthenticated or
 * wrongly-authenticated handshake is answered with {@code 401} and the WebSocket connection fails.
 * A successful connection with valid credentials is the positive control proving the failures are
 * caused by authentication, not by connectivity.
 *
 * <p>No login is performed here, so no session cookie exists — the handshake is authenticated solely
 * by the {@code Authorization} header (or rejected when it is missing/invalid). One server is shared by
 * the whole class ({@link AutoClose}); each test only attempts an independent handshake, so no per-test
 * state leaks between them.
 */
class WebSocketAuthTest {

    private static final String REST_WS = "/rest/ws";
    // Base64 of "admin:admin" — the design repo administrator from application-multi.properties.
    private static final String VALID_BASIC = "Basic YWRtaW46YWRtaW4=";
    private static final String WRONG_BASIC = "Basic "
            + Base64.getEncoder().encodeToString("admin:wrong-password".getBytes(StandardCharsets.UTF_8));

    @AutoClose
    private static final HttpClient client = JettyServer.get().withProfile("multi").start();

    @Test
    void rest_ws_rejects_handshake_without_credentials() {
        assertThrows(AssertionError.class,
                () -> new StompTester(client, client.getWebSocketURL(REST_WS), Map.of()),
                "/rest/ws must reject a handshake without credentials or cookies");
    }

    @Test
    void rest_ws_rejects_handshake_with_invalid_credentials() {
        assertThrows(AssertionError.class,
                () -> new StompTester(client, client.getWebSocketURL(REST_WS), Map.of("Authorization", WRONG_BASIC)),
                "/rest/ws must reject a handshake with invalid credentials");
    }

    @Test
    void rest_ws_accepts_handshake_with_valid_credentials() {
        // Positive control: valid Basic credentials authenticate the handshake; the connection
        // is established (construction blocks until connected and throws otherwise).
        try (var stomp = new StompTester(client, client.getWebSocketURL(REST_WS), Map.of("Authorization", VALID_BASIC))) {
            // Connected successfully — nothing else to assert.
        }
    }
}
