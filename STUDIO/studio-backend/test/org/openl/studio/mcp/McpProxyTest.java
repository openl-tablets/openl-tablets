package org.openl.studio.mcp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import jakarta.servlet.AsyncEvent;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/**
 * Passes requests to a fake MCP server on the loopback interface.
 */
class McpProxyTest {

    private final McpProxy proxy = new McpProxy();
    private final BlockingQueue<Received> received = new LinkedBlockingQueue<>();
    private final CountDownLatch released = new CountDownLatch(1);
    private HttpServer server;

    private record Received(String method, String uri, Headers headers, String body) {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        released.countDown();
        server.stop(0);
        proxy.close();
    }

    @Test
    void passesTheRequestWithTheAddressOfStudioTheClientOpened() throws Exception {
        serve(exchange -> answer(exchange, "application/json", "{\"result\":{}}"));
        var secret = McpCalls.newSecret();
        var request = request("POST");
        request.setQueryString("trace=1");
        request.addHeader("Authorization", "Token openl_pat_a.b");
        request.addHeader("Mcp-Session-Id", "s1");
        request.addHeader("OpenL-Public-Url", "https://attacker.example.com/");
        request.addHeader("OpenL-Client-Address", "198.51.100.66");
        request.addHeader("OpenL-Mcp-Secret", "guessed");
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("Connection", "keep-alive");
        request.setContentType("application/json");
        request.setContent("{\"jsonrpc\":\"2.0\"}".getBytes(UTF_8));

        var response = proxy(request);

        var sent = received.poll(10, SECONDS);
        assertEquals("POST", sent.method());
        assertEquals("/mcp?trace=1", sent.uri());
        assertEquals("Token openl_pat_a.b", sent.headers().getFirst("Authorization"));
        assertEquals("s1", sent.headers().getFirst("Mcp-Session-Id"));
        assertEquals("https://studio.example.com/studio/", sent.headers().getFirst("OpenL-Public-Url"));
        assertEquals(1, sent.headers().get("OpenL-Public-Url").size());
        assertEquals(List.of("203.0.113.7"), sent.headers().get("OpenL-Client-Address"),
                "The address the request came from, never the one the client named");
        assertEquals(List.of(secret), sent.headers().get("OpenL-Mcp-Secret"), "The secret of the run of the server");
        assertEquals("http://10.0.0.5:8080/studio", sent.headers().getFirst("OpenL-Base-Url"));
        assertEquals("{\"jsonrpc\":\"2.0\"}", sent.body());
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("abc", response.getHeader("Mcp-Session-Id"));
        assertNull(response.getHeader("Date"));
        assertEquals("{\"result\":{}}", response.getContentAsString());
    }

    @Test
    void streamsEachEventWhenTheServerWritesIt() throws Exception {
        serve(exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (var body = exchange.getResponseBody()) {
                body.write("data: 1\n\n".getBytes(UTF_8));
                body.flush();
                waitFor(released);
                body.write("data: 2\n\n".getBytes(UTF_8));
            }
        });
        var request = request("GET");

        var response = proxy(request, false);

        await().atMost(10, SECONDS).until(() -> "data: 1\n\n".equals(response.getContentAsString()));
        assertTrue(request.isAsyncStarted());
        released.countDown();
        await().atMost(10, SECONDS).until(() -> !request.isAsyncStarted());
        assertEquals("data: 1\n\ndata: 2\n\n", response.getContentAsString());
        assertEquals("text/event-stream", response.getContentType());
        assertNull(response.getHeader("Transfer-Encoding"));
        var sent = received.poll(10, SECONDS);
        assertEquals("GET", sent.method());
        assertEquals("", sent.body());
    }

    @Test
    void answersBadGatewayWhenTheServerCannotBeReached() throws Exception {
        int closed;
        try (var socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            closed = socket.getLocalPort();
        }
        var request = request("POST");
        var response = new MockHttpServletResponse();

        proxy.proxy(request, response, URI.create("http://127.0.0.1:" + closed));

        await().atMost(10, SECONDS).until(() -> !request.isAsyncStarted());
        assertEquals(502, response.getStatus());
    }

    @Test
    void stopsTheExchangeWhenTheClientIsGone() throws Exception {
        serve(exchange -> {
            waitFor(released);
            answer(exchange, "application/json", "{}");
        });
        var request = request("POST");

        proxy(request, false);
        assertNotNull(received.poll(10, SECONDS));
        var async = (MockAsyncContext) request.getAsyncContext();
        for (var listener : async.getListeners()) {
            listener.onError(new AsyncEvent(async, new IOException("The client is gone")));
        }

        await().atMost(10, SECONDS).until(() -> !request.isAsyncStarted());
    }

    @Test
    void callsStudioBackWhereTheRequestCameIn() {
        var request = request("GET");
        request.setLocalAddr("0:0:0:0:0:0:0:1");

        assertEquals("http://[0:0:0:0:0:0:0:1]:8080/studio", McpProxy.baseUrl(request));
    }

    @Test
    void takesTheAddressOfStudioWithoutItsContextPath() {
        var request = new MockHttpServletRequest("GET", "/mcp/");
        request.setServerPort(8080);

        assertEquals("http://localhost:8080/", McpProxy.publicUrl(request));
    }

    private static MockHttpServletRequest request(String method) {
        var request = new MockHttpServletRequest(method, "/studio/mcp");
        request.setContextPath("/studio");
        request.setServletPath("/mcp");
        request.setScheme("https");
        request.setServerName("studio.example.com");
        request.setServerPort(443);
        // The connector behind the reverse proxy in front of OpenL Studio
        request.setLocalAddr("10.0.0.5");
        request.setLocalPort(8080);
        ((MockServletContext) request.getServletContext()).setContextPath("/studio");
        request.setAsyncSupported(true);
        return request;
    }

    private MockHttpServletResponse proxy(MockHttpServletRequest request) {
        return proxy(request, true);
    }

    private MockHttpServletResponse proxy(MockHttpServletRequest request, boolean awaitEnd) {
        var response = new MockHttpServletResponse();
        proxy.proxy(request, response, URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
        if (awaitEnd) {
            await().atMost(10, SECONDS).until(() -> !request.isAsyncStarted());
        }
        return response;
    }

    private void serve(HttpHandler handler) {
        server.createContext("/", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            received.add(new Received(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                    exchange.getRequestHeaders(), body));
            handler.handle(exchange);
        });
    }

    private static void answer(HttpExchange exchange, String contentType, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.getResponseHeaders().add("Mcp-Session-Id", "abc");
        var bytes = body.getBytes(UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void waitFor(CountDownLatch latch) {
        try {
            latch.await(10, SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
