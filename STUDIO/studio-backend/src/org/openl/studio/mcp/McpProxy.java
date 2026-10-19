package org.openl.studio.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Passes a request of an MCP client on to the MCP server and streams the answer back as the server writes it.
 *
 * <p>Each exchange runs on a virtual thread of its own, so neither a thread of the servlet container nor a lock of
 * OpenL Studio is held while the server works or an event stream stays open. An exchange has no time limit, and it
 * stops when the client goes away.
 *
 * <p>The server calls OpenL Studio at the address in the {@code OpenL-Base-Url} header and learns the address of
 * OpenL Studio the client opened from the {@code OpenL-Public-Url} header. A request is answered with {@code 502}
 * when the server cannot be reached.
 *
 * @author Yury Molchan
 */
@Slf4j
final class McpProxy implements AutoCloseable {

    /** The header that tells the server the address to call OpenL Studio at. */
    private static final String BASE_URL = "OpenL-Base-Url";

    /** The header that tells the server the address of OpenL Studio the client opened. */
    private static final String PUBLIC_URL = "OpenL-Public-Url";

    /**
     * The headers of a connection rather than of a request or an answer, and the ones the HTTP client and the
     * container set themselves. They are passed on in neither direction.
     */
    private static final Set<String> NOT_PASSED = Set.of("connection", "content-length", "date", "expect", "host",
            "keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade");

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            // The server listens on the loopback interface, which no proxy stands in front of
            .proxy(HttpClient.Builder.NO_PROXY)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Sends the request to the server at the given address and streams its answer back. The exchange runs on a
     * thread of its own, so the thread of the container is free while the server works.
     */
    void proxy(HttpServletRequest request, HttpServletResponse response, URI server) {
        var toServer = toServer(request, server);
        var async = request.startAsync();
        async.setTimeout(0);
        var exchange = Thread.ofVirtual().name("mcp-proxy").unstarted(() -> {
            try {
                exchange(toServer, response);
            } finally {
                async.complete();
            }
        });
        async.addListener(new StopWhenClientIsGone(exchange));
        exchange.start();
    }

    private void exchange(HttpRequest toServer, HttpServletResponse response) {
        try {
            var answer = client.send(toServer, BodyHandlers.ofInputStream());
            response.setStatus(answer.statusCode());
            answer.headers().map().forEach((name, values) -> {
                if (passes(name)) {
                    values.forEach(value -> response.addHeader(name, value));
                }
            });
            try (var body = answer.body()) {
                stream(body, response);
            }
        } catch (IOException e) {
            log.debug("The MCP request ended early: {}", e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Copies the answer as it comes, so that each event of a stream reaches the client when the server writes it. */
    private static void stream(InputStream body, HttpServletResponse response) throws IOException {
        var out = response.getOutputStream();
        response.flushBuffer();
        var buffer = new byte[8192];
        int read;
        while ((read = body.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
            // Flushed whenever the bytes read so far are all there is, so an event reaches the client when written
            if (body.available() == 0) {
                out.flush();
            }
        }
    }

    private static HttpRequest toServer(HttpServletRequest request, URI server) {
        var path = request.getRequestURI().substring(request.getContextPath().length());
        var query = request.getQueryString();
        var builder = HttpRequest.newBuilder(server.resolve(query == null ? path : path + "?" + query))
                .method(request.getMethod(), body(request));
        for (var name : Collections.list(request.getHeaderNames())) {
            if (passes(name)) {
                Collections.list(request.getHeaders(name)).forEach(value -> builder.header(name, value));
            }
        }
        return builder.setHeader(BASE_URL, baseUrl(request))
                .setHeader(PUBLIC_URL, publicUrl(request))
                .build();
    }

    /** The body of the request as it arrives, or none for a request that carries none. */
    private static BodyPublisher body(HttpServletRequest request) {
        var carriesBody = request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null;
        return carriesBody ? BodyPublishers.ofInputStream(() -> input(request)) : BodyPublishers.noBody();
    }

    private static InputStream input(HttpServletRequest request) {
        try {
            return request.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean passes(String header) {
        return !NOT_PASSED.contains(header.toLowerCase(Locale.ROOT));
    }

    /**
     * The address the server calls OpenL Studio at: the address and the port of the connector the request came in
     * through, which the server shares the host with, and the context path. The call goes over HTTP, so a connector
     * that speaks only HTTPS cannot be called back.
     */
    static String baseUrl(HttpServletRequest request) {
        try {
            return new URI("http", null, request.getLocalAddr(), request.getLocalPort(),
                    request.getServletContext().getContextPath(), null, null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("No address of OpenL Studio for " + request.getLocalAddr(), e);
        }
    }

    /**
     * The address of OpenL Studio the client opened, ending with a slash: the address of the request without the
     * part below the context path. A reverse proxy in front of OpenL Studio is taken into account.
     */
    static String publicUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request).path("/").toUriString();
    }

    /** Stops the exchange with the server when the client is gone: nobody is left to read the answer. */
    private record StopWhenClientIsGone(Thread exchange) implements AsyncListener {

        @Override
        public void onError(AsyncEvent event) {
            exchange.interrupt();
        }

        @Override
        public void onComplete(AsyncEvent event) {
            // The exchange is over
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            // A request to the server has no time limit
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            // Started once only
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
