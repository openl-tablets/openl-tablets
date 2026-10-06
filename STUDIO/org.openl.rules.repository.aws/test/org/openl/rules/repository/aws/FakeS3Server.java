package org.openl.rules.repository.aws;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A bucket with versioning that answers just enough of the S3 protocol for {@link S3Repository} and records every
 * request it receives, so a test can check what is sent over the wire.
 *
 * <p>The bucket is always empty, whatever is written to it.
 *
 * @author Yury Molchan
 */
final class FakeS3Server implements AutoCloseable {

    static final String BUCKET = "openl-test";

    private static final String HOST = "127.0.0.1";

    private static final String NAMESPACE = "http://s3.amazonaws.com/doc/2006-03-01/";

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    FakeS3Server() throws IOException {
        server = HttpServer.create(new InetSocketAddress(HOST, 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    String endpoint() {
        return "http://" + HOST + ":" + server.getAddress().getPort();
    }

    List<Request> requests() {
        return List.copyOf(requests);
    }

    List<Request> requests(String method) {
        return requests().stream().filter(request -> request.method().equals(method)).toList();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            var uri = exchange.getRequestURI();
            var query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
            var request = new Request(exchange.getRequestMethod(), uri.getPath(), query, exchange.getRequestHeaders());
            exchange.getRequestBody().readAllBytes();
            requests.add(request);
            respond(exchange, request);
        }
    }

    private static void respond(HttpExchange exchange, Request request) throws IOException {
        switch (request.method()) {
            case "HEAD" -> send(exchange, 200, null);
            case "GET" -> respondToGet(exchange, request);
            case "PUT" -> respondToPut(exchange, request);
            default -> send(exchange, 501, "<Error><Code>NotImplemented</Code></Error>");
        }
    }

    private static void respondToGet(HttpExchange exchange, Request request) throws IOException {
        if (request.query().contains("versioning")) {
            send(exchange, 200, "<VersioningConfiguration xmlns=\"" + NAMESPACE + "\"><Status>Enabled</Status>"
                    + "</VersioningConfiguration>");
        } else if (request.query().contains("versions")) {
            send(exchange, 200, "<ListVersionsResult xmlns=\"" + NAMESPACE + "\"><Name>" + BUCKET + "</Name>"
                    + "<MaxKeys>1000</MaxKeys><IsTruncated>false</IsTruncated></ListVersionsResult>");
        } else {
            send(exchange, 501, "<Error><Code>NotImplemented</Code></Error>");
        }
    }

    private static void respondToPut(HttpExchange exchange, Request request) throws IOException {
        exchange.getResponseHeaders().set("ETag", "\"etag\"");
        exchange.getResponseHeaders().set("x-amz-version-id", "version-1");
        if (request.header("x-amz-copy-source") == null) {
            send(exchange, 200, null);
        } else {
            send(exchange, 200, "<CopyObjectResult><ETag>\"etag\"</ETag>"
                    + "<LastModified>2026-01-01T00:00:00.000Z</LastModified></CopyObjectResult>");
        }
    }

    private static void send(HttpExchange exchange, int status, String xml) throws IOException {
        if (xml == null) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        var body = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/xml");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    /**
     * A request as it arrived.
     *
     * @param method  the HTTP method
     * @param path    the path, starting with the bucket name
     * @param query   the raw query, which is empty if there is none
     * @param headers the request headers
     */
    record Request(String method, String path, String query, Headers headers) {

        String header(String name) {
            return headers.getFirst(name);
        }
    }
}
