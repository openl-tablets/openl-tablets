package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import org.openl.itest.core.JettyServer;

public class WebSocketTest {

    @Test
    public void smoke() throws Exception {
        try (var httpClient = JettyServer.get()
                .withProfile("multi")
                .start()) {
            httpClient.test("test-resources-socket/auth-admin");

            AtomicReference<Throwable> failure = new AtomicReference<>();
            AtomicReference<String> notification = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            StompSessionHandler handler = new StompSessionHandlerAdapter() {

                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    session.subscribe("/topic/public/notification.txt", new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return String.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            try {
                                notification.set((String) payload);
                            } finally {
                                latch.countDown();
                            }
                        }
                    });

                    session.subscribe("/user/queue/errors", new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return String.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            failure.set(new AssertionError(payload));
                        }
                    });

                    session.send("/app/admin/notification.txt", "Hello, World!");
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    failure.set(new Exception(headers.get("message").getFirst()));
                }

                @Override
                public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
                    failure.set(ex);
                }

                @Override
                public void handleTransportError(StompSession session, Throwable ex) {
                    failure.set(ex);
                }
            };

            httpClient.startWebSocket(handler);

            if (!latch.await(10, TimeUnit.SECONDS)) {
                if (failure.get() != null) {
                    throw new AssertionError("", failure.get());
                } else {
                    fail("Notification is not received");
                }
            } else if (failure.get() != null) {
                throw new AssertionError("", failure.get());
            } else {
                assertEquals("Hello, World!", notification.get(), "WebSocket message mismatch");
            }
        }
    }

}
