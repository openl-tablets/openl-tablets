package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import org.openl.itest.core.JettyServer;

public class WebSocketTest {

    record Scenario(String profile,
                    String httpTestResource,
                    String subscribeTopic,
                    String errorQueue,
                    String sendDestination,
                    String payload,
                    String expected) {
    }

    static Scenario[] scenarios() {
        return new Scenario[]{
                new Scenario(
                        "multi",
                        "test-resources-socket/auth-admin",
                        "/topic/public/notification.txt",
                        "/user/queue/errors",
                        "/app/admin/notification.txt",
                        "Hello, World!",
                        "Hello, World!"
                ),
                new Scenario(
                        "simple",
                        "test-resources-socket/no-auth",
                        "/topic/public/notification.txt",
                        "/user/queue/errors",
                        "/app/admin/notification.txt",
                        "Hello, World!",
                        "Hello, World!"
                )
        };
    }

    @ParameterizedTest(name = "{index} => profile={0}")
    @MethodSource("scenarios")
    void smoke_all(Scenario s) throws Exception {
        try (var httpClient = JettyServer.get()
                .withProfile(s.profile)
                .start()) {

            httpClient.test(s.httpTestResource);

            CompletableFuture<String> result = new CompletableFuture<>();

            StompSessionHandler handler = new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    // Success subscription
                    session.subscribe(s.subscribeTopic, new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return String.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            if (!result.isDone()) {
                                result.complete((String) payload);
                            }
                        }
                    });

                    // Error subscription (complete exceptionally)
                    session.subscribe(s.errorQueue, new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return String.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            if (!result.isDone()) {
                                result.completeExceptionally(
                                        new AssertionError("Error queue payload: " + payload));
                            }
                        }
                    });

                    // Send after subs are active
                    session.send(s.sendDestination, s.payload);
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // Broker ERROR frame
                    if (!result.isDone()) {
                        String msg = headers.getFirst("message");
                        result.completeExceptionally(new AssertionError("BROKER ERROR: " + msg));
                    }
                }

                @Override
                public void handleException(StompSession sess, StompCommand cmd, StompHeaders h, byte[] body, Throwable ex) {
                    if (!result.isDone()) {
                        result.completeExceptionally(ex);
                    }
                }

                @Override
                public void handleTransportError(StompSession session, Throwable ex) {
                    if (!result.isDone()) {
                        result.completeExceptionally(ex);
                    }
                }
            };

            httpClient.startWebSocket(handler);

            String received;
            try {
                received = result.orTimeout(10, TimeUnit.SECONDS).join();
            } catch (Exception e) {
                throw new AssertionError("""
                        WebSocket test failed.
                          profile      : %s
                          subscribe    : %s
                          errorQueue   : %s
                          sendDest     : %s
                          payloadSent  : %s
                        """.formatted(
                        s.profile, s.subscribeTopic, s.errorQueue, s.sendDestination, s.payload
                ), e);
            }

            assertEquals(s.expected, received, "WebSocket message mismatch");
        }
    }

}
