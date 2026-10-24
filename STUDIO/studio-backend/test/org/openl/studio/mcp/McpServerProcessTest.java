package org.openl.studio.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;

import org.openl.info.OpenLVersion;

/**
 * Starts fake MCP servers with the Node.js runtime found on the PATH; the tests that need it are skipped without one.
 */
class McpServerProcessTest {

    private static final String NODE = "node";

    /**
     * Names the port it took the way studio-mcp does, and answers with the environment and the Node.js options it was
     * started with.
     */
    private static final String LISTENING = """
            import { createServer } from "node:http";
            const env = { PORT: process.env.PORT, HOST: process.env.HOST };
            const server = createServer((req, res) => {
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ env, execArgv: process.execArgv }));
            }).listen(Number(process.env.PORT), process.env.HOST, () => {
              console.log("studio-mcp 1.0 listening on " + process.env.HOST + ", port " + server.address().port);
              console.error("a warning");
            });
            """;

    @TempDir
    Path webapp;
    private MockServletContext servletContext;
    private McpServerProcess server;

    @BeforeEach
    void setUp() {
        servletContext = new MockServletContext("file:" + webapp, new FileSystemResourceLoader());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void runsTheServerOnAFreePortOfTheLoopback() throws Exception {
        assumeTrue(nodeIsAvailable());
        bundle(LISTENING);
        server = server(true, NODE);

        server.start();

        assertTrue(server.isRunning());
        var address = server.address();
        assertNotNull(address);
        assertEquals("127.0.0.1", address.getHost());
        var answer = new ObjectMapper().readTree(get(address));
        assertEquals(Map.of("PORT", "0", "HOST", "127.0.0.1"),
                new ObjectMapper().convertValue(answer.path("env"), Map.class));
        assertEquals(List.of("--max-semi-space-size=1"),
                new ObjectMapper().convertValue(answer.path("execArgv"), List.class));

        server.stop();

        assertFalse(server.isRunning());
        assertNull(server.address());
        assertThrows(ConnectException.class, () -> get(address));
    }

    @Test
    void keepsNodeJsFromSendingTheCallsToStudioThroughAProxy() {
        var env = new HashMap<>(Map.of("NODE_USE_ENV_PROXY", "1", "HTTP_PROXY", "http://proxy:3128"));

        server(true, NODE).configure(env);

        assertFalse(env.containsKey("NODE_USE_ENV_PROXY"));
        assertEquals("http://proxy:3128", env.get("HTTP_PROXY"));
    }

    @Test
    void passesTheVersionOfStudioTheServerReports() {
        var env = new HashMap<String, String>();

        server(true, NODE).configure(env);

        assertEquals(OpenLVersion.getVersion(), env.get("OPENL_VERSION"));
    }

    @Test
    void sendsTelemetryAsAServiceOfItsOwnWhenStudioDoes() {
        var env = new HashMap<>(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318",
                "OTEL_SERVICE_NAME", "Studio"));

        server(true, NODE).configure(env);

        assertEquals("false", env.get("OTEL_SDK_DISABLED"));
        assertEquals("Studio-mcp", env.get("OTEL_SERVICE_NAME"));
        assertEquals("http://collector:4318", env.get("OTEL_EXPORTER_OTLP_ENDPOINT"));
    }

    @Test
    void sendsNoTelemetryWhenStudioDoesNot() {
        var env = new HashMap<>(Map.of("OTEL_JAVAAGENT_ENABLED", "false",
                "OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318"));

        server(true, NODE).configure(env);

        assertEquals("true", env.get("OTEL_SDK_DISABLED"));
        assertEquals("OpenL-mcp", env.get("OTEL_SERVICE_NAME"));
    }

    @Test
    void sendsNoTelemetryWhenStudioSendsNoTracesOrMetrics() {
        // LOGGING_FORMAT=otel of the Docker image without a collector: the agent is on for the logs alone
        var env = new HashMap<>(Map.of("OTEL_JAVAAGENT_ENABLED", "true", "OTEL_TRACES_EXPORTER", "none",
                "OTEL_METRICS_EXPORTER", "none"));

        server(true, NODE).configure(env);

        assertEquals("true", env.get("OTEL_SDK_DISABLED"));
    }

    @Test
    void stopsTheServerWhenTheContextIsRefreshed() throws Exception {
        assumeTrue(nodeIsAvailable());
        bundle(LISTENING);
        server = server(true, NODE);
        server.start();
        var address = server.address();
        assertNotNull(address);

        server.destroy();

        assertFalse(server.isRunning());
        assertThrows(ConnectException.class, () -> get(address));
    }

    @Test
    void staysOffWhenTheServerExits() throws Exception {
        assumeTrue(nodeIsAvailable());
        bundle("process.exit(3);");
        server = server(true, NODE);

        server.start();

        assertFalse(server.isRunning());
    }

    @Test
    void staysOffWithoutNodeJs() throws Exception {
        bundle(LISTENING);
        server = server(true, webapp.resolve("no-node").toString());

        server.start();

        assertFalse(server.isRunning());
    }

    @Test
    void staysOffWithoutTheBundle() {
        server = server(true, NODE);

        server.start();

        assertFalse(server.isRunning());
    }

    @Test
    void staysOffWhenDisabled() throws Exception {
        bundle(LISTENING);
        server = server(false, NODE);

        server.start();

        assertFalse(server.isRunning());
    }

    private McpServerProcess server(boolean enabled, String node) {
        return new McpServerProcess(enabled, node, servletContext);
    }

    private void bundle(String script) throws IOException {
        var entry = webapp.resolve("WEB-INF/mcp/index.mjs");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, script);
    }

    private static String get(URI address) throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(address).build(), HttpResponse.BodyHandlers.ofString()).body();
        }
    }

    private static boolean nodeIsAvailable() {
        try {
            return new ProcessBuilder(NODE, "--version").start().waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
