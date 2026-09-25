package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.junitpioneer.jupiter.StdErr;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

/**
 * Verifies the message a run of request files fails with: every failed request, each with the assertion it failed or
 * its exception.
 *
 * @author Yury Molchan
 */
class HttpClientTest {

    private static final String FOLDER = "test-resources/failure-report";

    @AutoClose
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final CountDownLatch slowResponse = new CountDownLatch(1);
    /** The client side port of every request to {@code /json}, which tells the connection it came through. */
    private final List<Integer> clientPorts = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(executor);
        server.createContext("/json", exchange -> {
            clientPorts.add(exchange.getRemoteAddress().getPort());
            respond(exchange, "{\"name\":\"foo\"}");
        });
        server.createContext("/slow", exchange -> {
            try {
                slowResponse.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        slowResponse.countDown();
        server.stop(0);
    }

    // The read timeout bounds every request, not only the slow one, so it leaves a cold first request room on a loaded
    // runner. The deliberate failures print to the captured streams and save their bodies away from target/responses.
    @Test
    @StdIo
    @SetSystemProperty(key = "http.timeout.read", value = "3000")
    @SetSystemProperty(key = "server.responses", value = "target/failure-report/")
    void failedRequestsAreListedWithTheAssertionTheyFailed(StdOut out, StdErr err) throws Exception {
        try (var client = new HttpClient(JettyServer.get(), baseURL())) {
            var error = assertThrows(AssertionError.class, () -> client.test(FOLDER));

            assertLinesMatch(List.of(
                    "Failed 6 of 7 requests:",
                    "    " + request("020-status") + ": Status code ==> expected: <201> but was: <200>",
                    "    " + request("030-header")
                            + ": Header Content-Type ==> expected: <text/plain> but was: <application/json>",
                    "    " + request("040-body") + ": Body > name ==> expected: <\"bar\"> but was: <\"foo\">",
                    "    " + Pattern.quote(request("050-timeout")) + ": Timeout ==> no response in \\d+ ms",
                    "    " + request("060-error")
                            + ": java.lang.IllegalArgumentException: Undefined environment variable: UNDEFINED",
                    "    " + request("070-unreadable") + ": java.lang.IllegalStateException: Cannot read "
                            + Path.of(FOLDER, "070-unreadable.resp")
                            + " (caused by java.io.IOException: Unexpected size of the body.)"
            ), String.valueOf(error.getMessage()).lines().toList());
        }
        // Every failure is reported as it happens, and a response is shown for a failed check of it only
        assertEquals(6, Stream.of(out.capturedLines()).filter(line -> line.contains("FAIL")).count());
        assertEquals(3, Stream.of(err.capturedLines()).filter("{\"name\":\"foo\"}"::equals).count());
    }

    @Test
    void requestsShareOneConnection() throws Exception {
        try (var client = new HttpClient(JettyServer.get(), baseURL())) {
            client.send("failure-report/010-ok");
            client.send("failure-report/010-ok");
        }

        assertEquals(2, clientPorts.size());
        assertEquals(clientPorts.getFirst(), clientPorts.getLast(), "Both requests must come from one connection");
    }

    @Test
    void closeStopsTheClientThread() throws Exception {
        var open = openClients();
        List<Thread> started;
        try (var client = new HttpClient(JettyServer.get(), baseURL())) {
            // Other clients may start meanwhile, so only a thread this client started counts
            started = openClients().stream().filter(thread -> !open.contains(thread)).toList();
            client.send("failure-report/010-ok");
        }

        assertFalse(started.isEmpty(), "The client runs a selector thread");
        for (var thread : started) {
            thread.join(Duration.ofSeconds(5));
        }
        var leaked = started.stream().filter(Thread::isAlive).toList();
        assertTrue(leaked.isEmpty(), () -> "Selector threads left open: " + leaked);
    }

    @Test
    void assertionIsDescribedByItsMessage() {
        var timeout = new AssertionError("Timeout ==> no response in 5 ms", new HttpTimeoutException("timed out"));

        assertEquals("Timeout ==> no response in 5 ms", HttpClient.describe(timeout));
    }

    @Test
    void wrapperIsSkippedAndTheRootCauseAdded() {
        var decoding = new RuntimeException("Failed to decode GZIP input", new ZipException("Not in GZIP format"));

        assertEquals("java.lang.RuntimeException: Failed to decode GZIP input"
                        + " (caused by java.util.zip.ZipException: Not in GZIP format)",
                HttpClient.describe(new RuntimeException(decoding)));
    }

    @Test
    void rootCauseIsTheDeepestOne() {
        var broken = new IOException("Broken", new ZipException("Not in GZIP"));
        var reading = new IllegalStateException("Cannot read", broken);

        assertEquals("java.lang.IllegalStateException: Cannot read"
                        + " (caused by java.util.zip.ZipException: Not in GZIP)",
                HttpClient.describe(reading));
    }

    @Test
    void rootCauseTheMessageTellsIsNotRepeated() {
        var reading = new IllegalStateException("Cannot read: Not in GZIP", new ZipException("Not in GZIP"));

        assertEquals("java.lang.IllegalStateException: Cannot read: Not in GZIP", HttpClient.describe(reading));
    }

    private static String request(String name) {
        return Path.of(FOLDER, name + ".req").toString();
    }

    private URI baseURL() {
        return URI.create("http://localhost:" + server.getAddress().getPort());
    }

    /** Finds the JDK HTTP clients still open, by the selector thread each of them runs. */
    private static Set<Thread> openClients() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().matches("HttpClient-\\d+-SelectorManager"))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        var body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
