package org.openl.studio.telemetry.rest.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.studio.telemetry.OtelEnvironment;
import org.openl.studio.telemetry.OtelEnvironment.Signal;

/**
 * The OpenTelemetry of the pages of OpenL Studio: what the browser sends, and the way to the collector.
 *
 * <p>The browser reads what to send and the name of its service, then posts its spans and metrics in OTLP/HTTP here.
 * OpenL Studio passes them on to the collector the environment names, with the headers the environment gives it. So
 * the collector needs to be reachable from OpenL Studio only, and its credentials never reach the browser.
 *
 * <p>The environment does not change while OpenL Studio runs, so it is read once. A signal whose endpoint the HTTP
 * client cannot call is not sent, and a header the HTTP client refuses to set, such as {@code Host}, is left out; both
 * are logged.
 *
 * <p>The endpoints serve the pages only, so the REST API documentation leaves them out.
 *
 * @author Yury Molchan
 */
@Slf4j
@Hidden
@RestController
@RequestMapping("/telemetry")
public class TelemetryController implements DisposableBean {

    /**
     * The largest data a page posts at once, in bytes: a batch of the browser SDK is far smaller, and a larger post is
     * refused before it fills the memory.
     */
    static final int MAX_BODY = 4 * 1024 * 1024;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Where the pages post their data, below the REST API: a post there changes nothing. */
    public static final String RELAY_PATH = "/telemetry/v1/";

    /** What the pages of OpenL Studio send, and the name of the service they send it as. */
    public record TelemetryView(String serviceName, Map<String, String> attributes, boolean traces, boolean metrics) {
    }

    /** Where and how a signal is sent to the collector. */
    private record Export(URI endpoint, Map<String, String> headers, Duration timeout) {
    }

    private final TelemetryView view;
    /** The signals the environment sends, with the way of each to the collector. */
    private final Map<Signal, Export> exports = new EnumMap<>(Signal.class);
    private final HttpClient client;

    public TelemetryController() {
        this(OtelEnvironment.current(), HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    TelemetryController(OtelEnvironment otel, HttpClient client) {
        this.client = client;
        for (var signal : Signal.values()) {
            var export = otel.exports(signal) ? exportOf(otel, signal) : null;
            if (export != null) {
                exports.put(signal, export);
            }
        }
        view = new TelemetryView(otel.serviceName("-ui"), otel.resourceAttributes(),
                exports.containsKey(Signal.TRACES), exports.containsKey(Signal.METRICS));
    }

    /**
     * The way of the signal to the collector, or {@code null} when the HTTP client cannot call its endpoint: not a
     * URI, a scheme but {@code http} and {@code https}, no host.
     */
    private static @Nullable Export exportOf(OtelEnvironment otel, Signal signal) {
        try {
            var endpoint = otel.endpoint(signal);
            // Refused once now rather than on every post of the pages
            HttpRequest.newBuilder(endpoint);
            return new Export(endpoint, validHeaders(otel.headers(signal)), otel.timeout(signal));
        } catch (IllegalArgumentException e) {
            log.warn("The {} of the pages are not sent: the HTTP client cannot call the OpenTelemetry endpoint. {}",
                    signal.id(), e.getMessage());
            return null;
        }
    }

    /** The headers the HTTP client can send: a restricted or a malformed one is left out. */
    private static Map<String, String> validHeaders(Map<String, String> headers) {
        var valid = new LinkedHashMap<String, String>();
        headers.forEach((name, value) -> {
            try {
                HttpRequest.newBuilder().header(name, value);
                valid.put(name, value);
            } catch (IllegalArgumentException e) {
                log.warn("The OpenTelemetry header '{}' is not sent with the data of the pages: {}", name,
                        e.getMessage());
            }
        });
        return valid;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public TelemetryView telemetry() {
        return view;
    }

    /**
     * Passes the OTLP/HTTP data of the browser on to the collector and answers with the answer of the collector:
     * {@code 404} for a signal that is not sent, {@code 413} for data larger than {@value #MAX_BODY} bytes, which is
     * not read further, {@code 400} for JSON that is not an object, {@code 502} when the collector cannot be reached.
     * Data that is not JSON is answered {@code 415} before it reaches the method.
     *
     * <p>Every resource of the data is named after the service of the pages, whatever a page wrote: a signed-in user
     * cannot send data in the name of another service, such as the backend.
     */
    @PostMapping(value = "/v1/{signal}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> relay(@PathVariable("signal") String signal, InputStream data)
            throws IOException, InterruptedException {
        var sent = Signal.of(signal);
        var export = sent == null ? null : exports.get(sent);
        if (export == null) {
            return ResponseEntity.notFound().build();
        }
        var read = data.readNBytes(MAX_BODY + 1);
        if (read.length > MAX_BODY) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        var body = ofThePages(read, sent);
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        var request = HttpRequest.newBuilder(export.endpoint())
                .timeout(export.timeout())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(BodyPublishers.ofByteArray(body));
        export.headers().forEach(request::header);
        try {
            var answer = client.send(request.build(), BodyHandlers.ofByteArray());
            var response = ResponseEntity.status(answer.statusCode());
            answer.headers().firstValue(HttpHeaders.CONTENT_TYPE).ifPresent(type -> response.header(
                    HttpHeaders.CONTENT_TYPE, type));
            return response.body(answer.body());
        } catch (IOException e) {
            log.debug("The collector did not receive the {} of the browser: {}", signal, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    /**
     * The OTLP/JSON data with the {@code service.name} of each of its resources set to the service of the pages, or
     * {@code null} when it is not a JSON object.
     *
     * <p>Only the resources of the signal under their JSON name, {@code resourceSpans} or {@code resourceMetrics}, are
     * passed on. Any other field is left out: a collector also reads the resources under their proto name, such as
     * {@code resource_spans}, which would pass on a service name the page wrote.
     */
    private byte @Nullable [] ofThePages(byte[] data, Signal signal) {
        var field = signal == Signal.TRACES ? "resourceSpans" : "resourceMetrics";
        try {
            if (!(JSON.readTree(data) instanceof ObjectNode root)) {
                return null;
            }
            var resources = root.path(field);
            for (var resource : resources) {
                if (resource instanceof ObjectNode owned) {
                    var described = owned.withObjectProperty("resource");
                    var attributes = JSON.createArrayNode();
                    described.path("attributes").valueStream()
                            .filter(attribute -> !OtelEnvironment.SERVICE_NAME.equals(attribute.path("key").asText()))
                            .forEach(attributes::add);
                    attributes.addObject().put("key", OtelEnvironment.SERVICE_NAME).putObject("value")
                            .put("stringValue", view.serviceName());
                    described.set("attributes", attributes);
                }
            }
            var relayed = JSON.createObjectNode();
            relayed.set(field, resources.isMissingNode() ? JSON.createArrayNode() : resources);
            return JSON.writeValueAsBytes(relayed);
        } catch (IOException | UnsupportedOperationException e) {
            // Not JSON, or a resource that is not an object
            return null;
        }
    }

    /**
     * Closes the connections to the collector when a refresh of the context replaces the controller, without waiting
     * for a collector that does not answer.
     */
    @Override
    public void destroy() {
        client.shutdownNow();
    }
}
