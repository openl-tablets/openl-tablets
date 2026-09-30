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
 * <p>The {@code /ws} endpoint is served by the httpBasic security chain that serves {@code /rest/**} too
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

    // Base64 of "admin:admin" — the design repo administrator from application.properties.
    private static final String VALID_BASIC = "Basic YWRtaW46YWRtaW4=";
    private static final String WRONG_BASIC = "Basic "
            + Base64.getEncoder().encodeToString("admin:wrong-password".getBytes(StandardCharsets.UTF_8));

    @AutoClose
    private static final HttpClient client = JettyServer.get().start();

    @Test
    void ws_rejects_handshake_without_credentials() {
        var webSocket = client.getWebSocketBaseURL();
        assertThrows(AssertionError.class,
                () -> new StompTester(client, webSocket, Map.of()),
                "/ws must reject a handshake without credentials or cookies");
    }

    @Test
    void ws_rejects_handshake_with_invalid_credentials() {
        var webSocket = client.getWebSocketBaseURL();
        var headers = Map.of("Authorization", WRONG_BASIC);
        assertThrows(AssertionError.class,
                () -> new StompTester(client, webSocket, headers),
                "/ws must reject a handshake with invalid credentials");
    }

    @Test
    void ws_accepts_handshake_with_valid_credentials() {
        // Positive control: valid Basic credentials authenticate the handshake; the connection
        // is established (construction blocks until connected and throws otherwise).
        try (var stomp = new StompTester(client, client.getWebSocketBaseURL(), Map.of("Authorization", VALID_BASIC))) {
            // Connected successfully — nothing else to assert.
        }
    }

    @Test
    void ws_answers_a_plain_request_with_the_handshake_refusal() {
        // A request that asks for no upgrade reaches the handshake handler, which refuses it with 400. Together
        // with the test below, it tells the handler at /ws from the not-found answer under /rest.
        client.getForObject("/ws", String.class, 400, "Authorization", VALID_BASIC);
    }

    @Test
    void rest_prefix_does_not_serve_the_handshake() {
        // The handshake has an address of its own. Valid credentials pass the /rest/** chain, so the 404 is the
        // dispatcher's: the handshake handler would answer 400, and a failed authentication 401.
        client.getForObject("/rest/ws", String.class, 404, "Authorization", VALID_BASIC);

        // A real upgrade is refused as well.
        var underRest = client.getWebSocketBaseURL().resolve("/rest/ws");
        var headers = Map.of("Authorization", VALID_BASIC);
        assertThrows(AssertionError.class,
                () -> new StompTester(client, underRest, headers),
                "/rest/ws must not serve the handshake");
    }
}
