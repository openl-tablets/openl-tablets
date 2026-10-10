package org.openl.studio.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * Starts fake MCP servers with the Node.js runtime found on the PATH; the tests that need it are skipped without one.
 */
class McpServerProcessTest {

    private static final String NODE = "node";
    private static final String STUDIO_URL = "http://127.0.0.1:8080";

    /** Answers the health check as openl-mcp, with the environment the server was started with. */
    private static final String HEALTHY = """
            const http = require("node:http");
            const env = { PORT: process.env.PORT, HOST: process.env.HOST, OPENL_BASE_URL: process.env.OPENL_BASE_URL };
            http.createServer((req, res) => {
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ status: "ok", service: "openl-mcp", env }));
            }).listen(Number(process.env.PORT), process.env.HOST, () => {
              console.log("listening");
              console.error("a warning");
            });
            """;

    @TempDir
    Path webapp;
    private MockServletContext servletContext;
    private int port;
    private McpServerProcess server;

    @BeforeEach
    void setUp() throws IOException {
        servletContext = new MockServletContext("file:" + webapp, new FileSystemResourceLoader());
        servletContext.setContextPath("/studio");
        try (var socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void runsTheServerOnTheLoopbackWithTheAddressOfStudio() throws Exception {
        assumeTrue(nodeIsAvailable());
        bundle(HEALTHY);
        server = server(true, NODE, "multi");

        server.start();

        assertTrue(server.isRunning());
        var env = new ObjectMapper().readTree(health()).path("env");
        assertEquals(Map.of("PORT", String.valueOf(port), "HOST", "127.0.0.1", "OPENL_BASE_URL",
                STUDIO_URL + "/studio"), new ObjectMapper().convertValue(env, Map.class));

        server.stop();

        assertFalse(server.isRunning());
        assertThrows(ConnectException.class, this::health);
    }

    @Test
    void stopsTheServerWhenTheContextIsRefreshed() throws Exception {
        assumeTrue(nodeIsAvailable());
        bundle(HEALTHY);
        server = server(true, NODE, "single");
        server.start();
        assertTrue(server.isRunning());

        server.destroy();

        assertFalse(server.isRunning());
        assertThrows(ConnectException.class, this::health);
    }

    @Test
    void staysOffWhenTheServerExits() throws Exception {
        assumeTrue(nodeIsAvailable());
        bundle("process.exit(3);");
        server = server(true, NODE, "multi");

        server.start();

        assertFalse(server.isRunning());
    }

    @Test
    void staysOffWhenThePortIsTaken() throws Exception {
        bundle(HEALTHY);
        server = server(true, NODE, "multi");

        try (var taken = new ServerSocket(port, 0, InetAddress.getLoopbackAddress())) {
            server.start();
        }

        assertFalse(server.isRunning());
    }

    @Test
    void staysOffWithoutNodeJs() throws Exception {
        bundle(HEALTHY);
        server = server(true, webapp.resolve("no-node").toString(), "multi");

        server.start();

        assertFalse(server.isRunning());
    }

    @Test
    void staysOffWithoutTheBundle() {
        server = server(true, NODE, "multi");

        server.start();

        assertFalse(server.isRunning());
    }

    @Test
    void staysOffWhenDisabled() throws Exception {
        bundle(HEALTHY);
        server = server(false, NODE, "multi");

        server.start();

        assertFalse(server.isRunning());
    }

    private McpServerProcess server(boolean enabled, String node, String userMode) {
        return new McpServerProcess(enabled, node, port, STUDIO_URL, userMode, servletContext);
    }

    private void bundle(String script) throws IOException {
        var entry = webapp.resolve("WEB-INF/mcp/dist/index.js");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, script);
    }

    private String health() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/health")).build();
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
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
