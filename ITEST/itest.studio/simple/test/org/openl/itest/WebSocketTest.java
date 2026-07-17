package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

class WebSocketTest {

    @Test
    void echoes_payload_back_on_topic() throws Exception {
        try (var httpClient = JettyServer.get().start()) {
            httpClient.test("test-resources-socket/no-auth");

            try (var stomp = new StompTester(httpClient)) {
                var received = stomp.awaitFirst("/topic/public/notification.txt", String.class);
                stomp.send("/app/admin/notification.txt", "Hello, World!");
                assertEquals("Hello, World!", received.get(10, TimeUnit.SECONDS), "WebSocket message mismatch");
            }
        }
    }
}
