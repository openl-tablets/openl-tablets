package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

class WebSocketTest {

    // Base64 of "admin:admin" — the administrator from application.properties.
    private static final String ADMIN_BASIC = "Basic YWRtaW46YWRtaW4=";

    @Test
    void echoes_payload_back_on_topic() throws Exception {
        try (var httpClient = JettyServer.get().start()) {
            // The handshake carries its own credentials, the way any API client authenticates.
            var restWs = httpClient.getWebSocketURL("/rest/ws");
            try (var stomp = new StompTester(httpClient, restWs, Map.of("Authorization", ADMIN_BASIC))) {
                var received = stomp.awaitFirst("/topic/public/notification.txt", String.class);
                stomp.send("/app/admin/notification.txt", "Hello, World!");
                assertEquals("Hello, World!", received.get(10, TimeUnit.SECONDS), "WebSocket message mismatch");
            }
        }
    }
}
