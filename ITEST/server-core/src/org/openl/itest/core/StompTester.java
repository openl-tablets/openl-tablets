package org.openl.itest.core;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Test helper for STOMP-over-WebSocket assertions against the itest Jetty server.
 *
 * <p>Construction blocks until the STOMP session is connected, so callers can immediately
 * subscribe / send afterwards. The user-scoped error queue ({@code /user/queue/errors})
 * is auto-subscribed; any frame received there fails every pending {@code awaitMatching}
 * future with the broker's error message.
 *
 * <p>Closing the tester disconnects the session.
 */
public final class StompTester implements AutoCloseable {

    private static final ObjectMapper JSON = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    private static final long DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;

    private final WebSocketStompClient client;
    private final StompSession session;
    private final List<CompletableFuture<?>> pendingAwaits = new ArrayList<>();

    public StompTester(HttpClient http) {
        this(http, http.getWebSocketBaseURL(), Map.of(), DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public StompTester(HttpClient http, long connectTimeout, TimeUnit unit) {
        this(http, http.getWebSocketBaseURL(), Map.of(), connectTimeout, unit);
    }

    /**
     * Connects to a specific STOMP endpoint with extra handshake headers.
     *
     * <p>The session cookie established by previous HTTP calls is always sent when present, so
     * cookie-authenticated {@code /web/ws} sessions keep working. {@code handshakeHeaders} adds
     * headers on top of it — e.g. {@code Authorization: Basic ...} for the {@code /rest/ws}
     * endpoint, which is authenticated by the {@code /rest/**} security chain rather than the
     * session.
     *
     * @param http             established HTTP client (provides the base URL and session cookie)
     * @param wsUrl            the {@code ws://...} endpoint to connect to, see {@link HttpClient#getWebSocketURL(String)}
     * @param handshakeHeaders extra HTTP headers to send during the WebSocket handshake
     */
    public StompTester(HttpClient http, URI wsUrl, Map<String, String> handshakeHeaders) {
        this(http, wsUrl, handshakeHeaders, DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private StompTester(HttpClient http, URI wsUrl, Map<String, String> handshakeHeaders, long connectTimeout, TimeUnit unit) {
        this.client = new WebSocketStompClient(new StandardWebSocketClient());
        // Always decode the frame body as a UTF-8 string regardless of the broker's
        // declared content type — JSON payloads carry `application/json` and the stock
        // StringMessageConverter rejects them. We hand a raw String to the frame
        // handler and let callers parse JSON themselves.
        this.client.setMessageConverter(new RawStringMessageConverter());
        var headers = new WebSocketHttpHeaders();
        var cookie = http.getCookie();
        if (cookie != null) {
            headers.add("Cookie", cookie);
        }
        handshakeHeaders.forEach(headers::add);
        var connectFuture = new CompletableFuture<StompSession>();
        client.connectAsync(wsUrl, headers, new StompHeaders(),
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession s, StompHeaders connected) {
                        connectFuture.complete(s);
                    }

                    @Override
                    public void handleTransportError(StompSession s, Throwable ex) {
                        if (!connectFuture.isDone()) {
                            connectFuture.completeExceptionally(ex);
                        }
                        failPending(ex);
                    }

                    @Override
                    public void handleException(StompSession s, StompCommand cmd,
                                                StompHeaders h, byte[] body, Throwable ex) {
                        failPending(ex);
                    }

                    @Override
                    public void handleFrame(StompHeaders frameHeaders, Object payload) {
                        // STOMP-level ERROR frames land here; surface as test failures.
                        var msg = frameHeaders.getFirst("message");
                        failPending(new AssertionError("STOMP broker error: " + msg));
                    }
                });
        try {
            this.session = connectFuture.get(connectTimeout, unit);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        } catch (TimeoutException te) {
            throw new AssertionError("STOMP connect timed out after %d %s".formatted(connectTimeout, unit), te);
        } catch (Exception e) {
            throw new AssertionError("STOMP connect failed", e);
        }
        // Auto-subscribe to the per-user error queue so broker errors fail pending awaits.
        session.subscribe("/user/queue/errors", new StringFrameHandler(payload ->
                failPending(new AssertionError("Error queue payload: " + payload))));
    }

    /**
     * Subscribe to {@code topic}, deserialize each STOMP frame body to {@code type}, and
     * return a future that completes with the first payload for which {@code condition}
     * returns true. The subscription is cancelled as soon as the future settles.
     */
    public <T> CompletableFuture<T> awaitMatching(String topic, Class<T> type, Predicate<? super T> condition) {
        var result = new CompletableFuture<T>();
        synchronized (pendingAwaits) {
            pendingAwaits.add(result);
        }
        var subscription = session.subscribe(topic, new StringFrameHandler(body -> {
            try {
                T payload = type == String.class
                        ? type.cast(body)
                        : JSON.readValue(body, type);
                if (condition.test(payload)) {
                    result.complete(payload);
                }
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        }));
        result.whenComplete((ignored, ex) -> {
            synchronized (pendingAwaits) {
                pendingAwaits.remove(result);
            }
            try {
                subscription.unsubscribe();
            } catch (Exception ignore) {
                // Disconnect in flight — ignore.
            }
        });
        return result;
    }

    /** Convenience: {@code awaitMatching} with a true-for-everything predicate. */
    public <T> CompletableFuture<T> awaitFirst(String topic, Class<T> type) {
        return awaitMatching(topic, type, payload -> true);
    }

    public void send(String destination, Object payload) {
        session.send(destination, payload);
    }

    @Override
    public void close() {
        try {
            session.disconnect();
        } catch (Exception ignore) {
            // Best-effort.
        }
        client.stop();
    }

    private void failPending(Throwable ex) {
        List<CompletableFuture<?>> snapshot;
        synchronized (pendingAwaits) {
            snapshot = new ArrayList<>(pendingAwaits);
        }
        for (var future : snapshot) {
            future.completeExceptionally(ex);
        }
    }

    private record StringFrameHandler(java.util.function.Consumer<String> consumer) implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            consumer.accept((String) payload);
        }
    }

    /**
     * Content-type-agnostic converter: incoming frame body → UTF-8 String, outgoing
     * String → UTF-8 bytes. Other payload classes are passed through unchanged so
     * {@link StompSession#send(String, Object)} can serialize arbitrary types via
     * Spring's default machinery if needed.
     */
    private static final class RawStringMessageConverter implements MessageConverter {
        @Override
        @Nullable
        public Object fromMessage(Message<?> message, Class<?> targetClass) {
            if (targetClass != String.class) {
                return null;
            }
            Object payload = message.getPayload();
            if (payload instanceof byte[] bytes) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return payload instanceof String ? payload : null;
        }

        @Override
        @Nullable
        public Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
            if (payload instanceof String str) {
                return new org.springframework.messaging.support.GenericMessage<>(
                        str.getBytes(StandardCharsets.UTF_8), headers);
            }
            if (payload instanceof byte[]) {
                return new org.springframework.messaging.support.GenericMessage<>(payload, headers);
            }
            return null;
        }
    }
}
