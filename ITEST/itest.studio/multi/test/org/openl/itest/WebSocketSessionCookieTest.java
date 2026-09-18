package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

/**
 * Proves the browser's own way onto the WebSocket: a session cookie and nothing else.
 *
 * <p>Every other STOMP test authenticates its handshake with an {@code Authorization} header, the way an
 * integration does. The user interface never sends one — it signs in through the form and rides the session
 * from then on. That is the only path left now that {@code /web/ws}, which used to admit an anonymous
 * handshake, is gone (EPBDS-16690), so it is worth a test of its own.
 *
 * <p>The client is built fresh here rather than shared with {@link WebSocketAuthTest}: signing in stores a
 * session cookie on it, which would otherwise make that class's "rejects a handshake without credentials"
 * assertions pass or fail depending on the order the tests happened to run in.
 *
 * @author Yury Molchan
 */
class WebSocketSessionCookieTest {

    @Test
    void handshake_is_authenticated_by_the_session_cookie_alone() throws Exception {
        try (var client = JettyServer.get().start()) {
            client.send("005-login-admin");
            assertNotNull(client.getCookie(), "the form sign-in must establish a session cookie");

            // No Authorization header: the default endpoint with whatever cookie the client already holds.
            try (var stomp = new StompTester(client, client.getWebSocketBaseURL(), Map.of())) {
                // The constructor blocks until CONNECTED and throws otherwise, so reaching here is the assertion.
            }
        }
    }
}
