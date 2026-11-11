package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers how {@link AppServer} reads the readiness of the verified application: only the {@code READY} answer
 * passes, and an application that does not answer fails the check in time instead of blocking the build.
 *
 * @author Yury Molchan
 */
class AppServerTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final CountDownLatch release = new CountDownLatch(1);
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/ready", exchange -> answer(exchange, 200, "READY"));
        server.createContext("/failed", exchange -> answer(exchange, 200, "FAILED"));
        server.createContext("/unavailable", exchange -> answer(exchange, 503, "READY"));
        server.createContext("/silent", exchange -> awaitRelease());
        server.start();
    }

    @AfterEach
    void stopServer() {
        release.countDown();
        server.stop(0);
    }

    @Test
    void passesWhenTheApplicationIsReady() {
        assertDoesNotThrow(() -> AppServer.checkReadiness(uri("/ready"), TIMEOUT));
    }

    @Test
    void failsWhenTheApplicationIsNotReady() {
        var error = assertThrows(IOException.class, () -> AppServer.checkReadiness(uri("/failed"), TIMEOUT));

        assertEquals("Server startup failure: FAILED", error.getMessage());
    }

    @Test
    void failsOnAnErrorStatusWhateverTheBodySays() {
        var error = assertThrows(IOException.class, () -> AppServer.checkReadiness(uri("/unavailable"), TIMEOUT));

        assertEquals("Server startup failure: HTTP status 503", error.getMessage());
    }

    @Test
    void failsInTimeWhenTheApplicationDoesNotAnswer() {
        assertTimeoutPreemptively(TIMEOUT,
                () -> assertThrows(SocketTimeoutException.class,
                        () -> AppServer.checkReadiness(uri("/silent"), Duration.ofMillis(200))));
    }

    private URI uri(String path) {
        var address = server.getAddress();
        return URI.create("http://" + address.getHostString() + ":" + address.getPort() + path);
    }

    private static void answer(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /**
     * Keeps the request unanswered until the test is over.
     */
    private void awaitRelease() {
        try {
            release.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
