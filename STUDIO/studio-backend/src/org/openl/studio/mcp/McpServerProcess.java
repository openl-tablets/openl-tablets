package org.openl.studio.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import jakarta.servlet.ServletContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Runs the MCP server built into OpenL Studio.
 *
 * <p>The server is the openl-mcp Node.js program the war carries in {@code WEB-INF/mcp}. It listens on the loopback
 * interface at {@code mcp.port} and calls the OpenL Studio REST API at {@code mcp.studio-url}. The servlet container
 * serves it at {@code /mcp} through a reverse proxy, which the Docker image configures.
 *
 * <p>The server starts with the Spring context and stops with it. A change of the settings refreshes the context, so
 * the server restarts with the new settings. It is optional: when it is disabled, when Node.js or the bundle is
 * missing, or when it fails to start, OpenL Studio runs without it and logs a warning.
 *
 * <p>Each line the server writes goes to the {@code org.openl.studio.mcp.node} logger: standard output as INFO,
 * standard error as WARN.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
@RequiredArgsConstructor
class McpServerProcess implements SmartLifecycle, DisposableBean {

    private static final Logger NODE_LOG = LoggerFactory.getLogger("org.openl.studio.mcp.node");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BUNDLE = "/WEB-INF/mcp";
    private static final String ENTRY = "dist/index.js";
    private static final String SERVICE = "openl-mcp";
    private static final String LOOPBACK = "127.0.0.1";
    private static final Duration START_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final long STOP_TIMEOUT_SECONDS = 10;

    @Value("${mcp.enabled}")
    private final boolean enabled;
    @Value("${mcp.node}")
    private final String node;
    @Value("${mcp.port}")
    private final int port;
    @Value("${mcp.studio-url}")
    private final String studioUrl;
    @Value("${user.mode}")
    private final String userMode;
    private final ServletContext servletContext;

    private volatile @Nullable Process process;

    @Override
    public void start() {
        if (!enabled) {
            log.info("The built-in MCP server is disabled.");
            return;
        }
        var home = servletContext.getRealPath(BUNDLE);
        if (home == null || !Files.isRegularFile(Path.of(home, ENTRY))) {
            log.warn("The built-in MCP server is not available: the war carries no {}/{}.", BUNDLE, ENTRY);
            return;
        }
        try {
            requireFreePort();
            var started = launch(Path.of(home));
            process = started;
            awaitHealthy(started);
            log.info("The built-in MCP server is running at /mcp on port {}.", port);
            if ("single".equals(userMode)) {
                log.warn("The built-in MCP server serves /mcp without authentication: user.mode is single.");
            }
        } catch (IOException e) {
            stop();
            log.warn("The built-in MCP server is not available: {}", e.getMessage());
        } catch (InterruptedException e) {
            stop();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        var running = process;
        process = null;
        if (running == null || !running.isAlive()) {
            return;
        }
        running.descendants().forEach(ProcessHandle::destroy);
        running.destroy();
        try {
            if (!running.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                running.destroyForcibly();
            }
        } catch (InterruptedException e) {
            running.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        log.info("The built-in MCP server is stopped.");
    }

    /**
     * Stops the server when the bean is destroyed. A refresh of the context destroys its beans without stopping
     * them first.
     */
    @Override
    public void destroy() {
        stop();
    }

    @Override
    public boolean isRunning() {
        var running = process;
        return running != null && running.isAlive();
    }

    /**
     * Fails when the port is taken. Another server there, even another openl-mcp, would answer the health check in
     * place of the one this bean starts.
     */
    private void requireFreePort() throws IOException {
        try (var socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName(LOOPBACK), port));
        } catch (IOException e) {
            throw new IOException("port " + port + " is in use. Set mcp.port to a free port.", e);
        }
    }

    private Process launch(Path home) throws IOException {
        var builder = new ProcessBuilder(node, home.resolve(ENTRY).toString(), "--http").directory(home.toFile());
        var env = builder.environment();
        env.put("PORT", String.valueOf(port));
        env.put("HOST", LOOPBACK);
        env.put("OPENL_BASE_URL", studioUrl + servletContext.getContextPath());
        var started = builder.start();
        pipe(started.getInputStream(), NODE_LOG::info);
        pipe(started.getErrorStream(), NODE_LOG::warn);
        return started;
    }

    private static void pipe(InputStream output, Consumer<String> logger) {
        Thread.ofVirtual().name("openl-mcp-output").start(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(output, StandardCharsets.UTF_8))) {
                reader.lines().forEach(logger);
            } catch (IOException | UncheckedIOException e) {
                // The process is gone and its output with it
            }
        });
    }

    /**
     * Waits until the server answers its health check as openl-mcp.
     */
    private void awaitHealthy(Process started) throws IOException, InterruptedException {
        var deadline = Instant.now().plus(START_TIMEOUT);
        var request = HttpRequest.newBuilder(URI.create("http://" + LOOPBACK + ":" + port + "/health"))
                .timeout(POLL_INTERVAL.multipliedBy(5))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            while (!isHealthy(client, request)) {
                if (!started.isAlive()) {
                    throw new IOException("the server exited with code " + started.exitValue() + ".");
                }
                if (Instant.now().isAfter(deadline)) {
                    throw new IOException("the server did not answer in " + START_TIMEOUT.toSeconds() + " seconds.");
                }
                Thread.sleep(POLL_INTERVAL);
            }
        }
    }

    private static boolean isHealthy(HttpClient client, HttpRequest request) throws InterruptedException {
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200
                    && SERVICE.equals(JSON.readTree(response.body()).path("service").asText());
        } catch (IOException e) {
            return false;
        }
    }
}
