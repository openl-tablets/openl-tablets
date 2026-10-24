package org.openl.studio.telemetry.rest.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.studio.telemetry.OtelEnvironment;
import org.openl.studio.telemetry.rest.controller.TelemetryController.TelemetryView;

class TelemetryControllerTest {

    private record Received(String path, String contentType, String auth, String body) {
    }

    private HttpServer collector;
    private final CompletableFuture<Received> received = new CompletableFuture<>();

    @BeforeEach
    void setUp() throws Exception {
        collector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        collector.createContext("/", exchange -> {
            var headers = exchange.getRequestHeaders();
            received.complete(new Received(exchange.getRequestURI().getPath(), headers.getFirst("Content-Type"),
                    headers.getFirst("Authorization"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            var answer = "{\"partialSuccess\":{}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        collector.start();
    }

    @AfterEach
    void tearDown() {
        collector.stop(0);
    }

    private TelemetryController controller(Map<String, String> env) {
        return new TelemetryController(new OtelEnvironment(env), HttpClient.newHttpClient());
    }

    /** The data of a post. */
    private static InputStream data(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private String collectorUrl() {
        return "http://127.0.0.1:" + collector.getAddress().getPort();
    }

    @Test
    void tellsThePagesWhatToSend() {
        assertEquals(new TelemetryView("OpenL-ui", Map.of(), false, false), controller(Map.of()).telemetry());
        assertEquals(new TelemetryView("Studio-ui", Map.of("env", "prod"), true, false),
                controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl(), "OTEL_SERVICE_NAME", "Studio",
                        "OTEL_RESOURCE_ATTRIBUTES", "env=prod", "OTEL_METRICS_EXPORTER", "none")).telemetry());
    }

    @Test
    void passesTheDataOfThePagesOnToTheCollector() throws Exception {
        var controller = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl(),
                "OTEL_EXPORTER_OTLP_HEADERS", "Authorization=Bearer secret"));

        var answer = controller.relay("traces", data("{\"resourceSpans\":[]}"));

        assertEquals(200, answer.getStatusCode().value());
        assertEquals("application/json", answer.getHeaders().getFirst("Content-Type"));
        assertArrayEquals("{\"partialSuccess\":{}}".getBytes(StandardCharsets.UTF_8), answer.getBody());
        assertEquals(new Received("/v1/traces", "application/json", "Bearer secret", "{\"resourceSpans\":[]}"),
                received.get());
    }

    @Test
    void sendsTheDataOfAPageAsTheServiceOfThePages() throws Exception {
        var controller = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl()));

        controller.relay("metrics", data("""
                {"resourceMetrics":[
                  {"resource":{"attributes":[{"key":"service.name","value":{"stringValue":"OpenL"}},
                    {"key":"env","value":{"stringValue":"prod"}}]},"scopeMetrics":[]},
                  {"scopeMetrics":[]}]}"""));

        assertEquals(new ObjectMapper().readTree("""
                {"resourceMetrics":[
                  {"resource":{"attributes":[{"key":"env","value":{"stringValue":"prod"}},
                    {"key":"service.name","value":{"stringValue":"OpenL-ui"}}]},"scopeMetrics":[]},
                  {"scopeMetrics":[],
                    "resource":{"attributes":[{"key":"service.name","value":{"stringValue":"OpenL-ui"}}]}}]}"""),
                new ObjectMapper().readTree(received.get().body()));
    }

    @Test
    void passesOnTheResourcesOfTheSignalUnderTheirJsonNameOnly() throws Exception {
        var controller = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl()));

        // A collector reads the proto name too: the service name the page wrote would reach it
        controller.relay("traces", data("""
                {"resource_spans":[{"resource":{"attributes":[
                  {"key":"service.name","value":{"stringValue":"OpenL"}}]}}],"extra":1}"""));

        assertEquals("{\"resourceSpans\":[]}", received.get().body());
    }

    @Test
    void refusesJsonThatIsNotAnObject() throws Exception {
        var controller = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl()));

        assertEquals(400, controller.relay("traces", data("{\"resourceSpans\":")).getStatusCode()
                .value());
        assertEquals(400, controller.relay("traces", data("[]")).getStatusCode().value());
        assertEquals(400, controller.relay("traces", data("{\"resourceSpans\":[{\"resource\":1}]}"))
                .getStatusCode().value());
        assertFalse(received.isDone(), "nothing reaches the collector");
    }

    @Test
    void leavesOutAHeaderTheClientRefusesToSend() throws Exception {
        var controller = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl(),
                "OTEL_EXPORTER_OTLP_HEADERS", "Host=collector.example,Authorization=Bearer secret"));

        var answer = controller.relay("traces", data("{}"));

        assertEquals(200, answer.getStatusCode().value());
        assertEquals("Bearer secret", received.get().auth());
    }

    @Test
    void sendsNoSignalWhoseEndpointTheClientCannotCall() {
        var noUri = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl(),
                "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "http://collector example/v1/traces"));
        var noScheme = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl(),
                "OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "collector:4318"));

        assertEquals(new TelemetryView("OpenL-ui", Map.of(), false, true), noUri.telemetry());
        assertEquals(new TelemetryView("OpenL-ui", Map.of(), true, false), noScheme.telemetry());
    }

    @Test
    void refusesDataLargerThanAPageSends() throws Exception {
        var controller = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl()));

        var answer = controller.relay("traces", new ByteArrayInputStream(new byte[TelemetryController.MAX_BODY + 1]));

        assertEquals(413, answer.getStatusCode().value());
        assertFalse(received.isDone(), "nothing reaches the collector");
    }

    @Test
    void refusesASignalThatIsNotSent() throws Exception {
        var controller = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", collectorUrl(),
                "OTEL_METRICS_EXPORTER", "none"));

        assertEquals(404, controller.relay("metrics", data("")).getStatusCode().value());
        assertEquals(404, controller.relay("logs", data("")).getStatusCode().value());
        assertEquals(404, controller(Map.of()).relay("traces", data("")).getStatusCode()
                .value());
    }

    @Test
    void answersBadGatewayWhenTheCollectorIsUnreachable() throws Exception {
        var url = collectorUrl();
        collector.stop(0);

        var answer = controller(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", url)).relay("traces", data("{}"));

        assertEquals(502, answer.getStatusCode().value());
    }
}
