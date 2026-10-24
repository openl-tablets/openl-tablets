package org.openl.studio.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.studio.telemetry.OtelEnvironment.Signal;

class OtelEnvironmentTest {

    @Test
    void isOnWhenAnExporterIsDefinedOrTheAgentIsEnabled() {
        assertFalse(new OtelEnvironment(Map.of()).enabled());
        assertTrue(new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", "http://c:4318")).enabled());
        assertTrue(new OtelEnvironment(Map.of("OTEL_JAVAAGENT_ENABLED", "TRUE")).enabled());
        assertFalse(new OtelEnvironment(Map.of("OTEL_JAVAAGENT_ENABLED", "false",
                "OTEL_EXPORTER_OTLP_ENDPOINT", "http://c:4318")).enabled());
        assertFalse(new OtelEnvironment(Map.of("OTEL_SDK_DISABLED", "true",
                "OTEL_EXPORTER_OTLP_ENDPOINT", "http://c:4318")).enabled());
    }

    @Test
    void namesTheServiceOfAComponent() {
        assertEquals("OpenL-ui", new OtelEnvironment(Map.of()).serviceName("-ui"));
        assertEquals("Studio-ui", new OtelEnvironment(Map.of("OTEL_SERVICE_NAME", " Studio ")).serviceName("-ui"));
        assertEquals("Rules-ui", new OtelEnvironment(Map.of("OTEL_RESOURCE_ATTRIBUTES", "service.name=Rules"))
                .serviceName("-ui"));
    }

    @Test
    void readsTheResourceAttributesButTheNameOfTheService() {
        var otel = new OtelEnvironment(Map.of("OTEL_RESOURCE_ATTRIBUTES",
                "service.name=Rules, deployment.environment = prod,team=a%20b,broken"));

        assertEquals(Map.of("deployment.environment", "prod", "team", "a b"), otel.resourceAttributes());
    }

    @Test
    void sendsASignalWhoseExporterIsNotNone() {
        assertTrue(new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")).sends(Signal.TRACES));
        assertFalse(new OtelEnvironment(Map.of("OTEL_JAVAAGENT_ENABLED", "true", "OTEL_METRICS_EXPORTER", "none"))
                .sends(Signal.METRICS));
        assertFalse(new OtelEnvironment(Map.of("OTEL_TRACES_EXPORTER", "otlp")).sends(Signal.TRACES));
    }

    @Test
    void exportsASignalOverOtlpHttpOnly() {
        var endpoint = Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", "http://c:4318");
        assertTrue(new OtelEnvironment(endpoint).exports(Signal.TRACES));
        assertFalse(new OtelEnvironment(Map.of()).exports(Signal.TRACES));
        assertFalse(new OtelEnvironment(Map.of("OTEL_JAVAAGENT_ENABLED", "true", "OTEL_TRACES_EXPORTER", "none"))
                .exports(Signal.TRACES));
        assertTrue(new OtelEnvironment(Map.of("OTEL_JAVAAGENT_ENABLED", "true", "OTEL_METRICS_EXPORTER",
                "logging, otlp")).exports(Signal.METRICS));
        var grpc = new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc",
                "OTEL_EXPORTER_OTLP_METRICS_PROTOCOL", "http/json"));
        assertFalse(grpc.exports(Signal.TRACES));
        assertTrue(grpc.exports(Signal.METRICS));
    }

    @Test
    void findsTheEndpointOfASignal() {
        assertEquals(URI.create("http://localhost:4318/v1/traces"),
                new OtelEnvironment(Map.of()).endpoint(Signal.TRACES));
        var otel = new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", "https://c:4318/otlp/",
                "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "https://t:4318/spans"));
        assertEquals(URI.create("https://t:4318/spans"), otel.endpoint(Signal.TRACES));
        assertEquals(URI.create("https://c:4318/otlp/v1/metrics"), otel.endpoint(Signal.METRICS));
    }

    @Test
    void readsTheHeadersAndTheTimeoutOfASignal() {
        var otel = new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_HEADERS", "Authorization=Basic a+b=,x-team=one",
                "OTEL_EXPORTER_OTLP_METRICS_HEADERS", "x-team=two", "OTEL_EXPORTER_OTLP_METRICS_TIMEOUT", "500"));

        assertEquals(Map.of("Authorization", "Basic a+b=", "x-team", "one"), otel.headers(Signal.TRACES));
        assertEquals(Map.of("Authorization", "Basic a+b=", "x-team", "two"), otel.headers(Signal.METRICS));
        assertEquals(Duration.ofSeconds(10), otel.timeout(Signal.TRACES));
        assertEquals(Duration.ofMillis(500), otel.timeout(Signal.METRICS));
    }

    @Test
    void readsATimeoutWithAUnitAndToleratesAMalformedOne() {
        assertEquals(Duration.ofSeconds(3), new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT", "3s"))
                .timeout(Signal.TRACES));
        assertEquals(Duration.ofMinutes(1), new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT", "1m"))
                .timeout(Signal.TRACES));
        assertEquals(Duration.ofSeconds(10), new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT", "soon"))
                .timeout(Signal.TRACES));
        assertEquals(Duration.ofSeconds(10), new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT", "0"))
                .timeout(Signal.TRACES));
        // Longer than a duration holds, and more than a long holds
        assertEquals(Duration.ofSeconds(10), new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT",
                Long.MAX_VALUE + "d")).timeout(Signal.TRACES));
        assertEquals(Duration.ofSeconds(10), new OtelEnvironment(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT",
                "99999999999999999999")).timeout(Signal.TRACES));
    }

    @Test
    void keepsAValueWhosePercentEncodingIsMalformed() {
        var otel = new OtelEnvironment(Map.of("OTEL_RESOURCE_ATTRIBUTES", "service.name=Rules%,note=100%"));

        assertEquals("Rules%-mcp", otel.serviceName("-mcp"));
        assertEquals(Map.of("note", "100%"), otel.resourceAttributes());
    }
}
