package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

class WebSocketTest {

    record Scenario(String profile,
                    String httpTestResource,
                    String subscribeTopic,
                    String sendDestination,
                    String payload) {
    }

    static Scenario[] scenarios() {
        return new Scenario[]{
                new Scenario(
                        "multi",
                        "test-resources-socket/auth-admin",
                        "/topic/public/notification.txt",
                        "/app/admin/notification.txt",
                        "Hello, World!"
                ),
                new Scenario(
                        "simple",
                        "test-resources-socket/no-auth",
                        "/topic/public/notification.txt",
                        "/app/admin/notification.txt",
                        "Hello, World!"
                )
        };
    }

    @ParameterizedTest(name = "{index} => profile={0}")
    @MethodSource("scenarios")
    void echoes_payload_back_on_topic(Scenario s) throws Exception {
        try (var httpClient = JettyServer.get().withProfile(s.profile).start()) {
            httpClient.test(s.httpTestResource);

            try (var stomp = new StompTester(httpClient)) {
                var received = stomp.awaitFirst(s.subscribeTopic, String.class);
                stomp.send(s.sendDestination, s.payload);
                assertEquals(s.payload, received.get(10, TimeUnit.SECONDS), "WebSocket message mismatch");
            }
        }
    }
}
