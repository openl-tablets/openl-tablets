package org.openl.studio.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import jakarta.servlet.ServletContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import org.openl.info.OpenLVersion;
import org.openl.studio.telemetry.OtelEnvironment;
import org.openl.studio.telemetry.OtelEnvironment.Signal;

/**
 * Runs the MCP server built into OpenL Studio.
 *
 * <p>The server is the studio-mcp Node.js program the war carries in {@code WEB-INF/mcp}. It listens on a free port of
 * the loopback interface and calls the OpenL Studio REST API at the address {@link McpProxyServlet}, which serves it at
 * {@code /mcp}, names with every request.
 *
 * <p>The server takes the port when it starts and names it in the line it logs once it listens, which ends with
 * {@code listening on <host>, port <port>}. OpenL Studio reads the port from that line.
 *
 * <p>Node.js runs with a young generation of 1 MB per semi-space. The default one grows to tens of megabytes, which the
 * few short-lived objects of the server never fill. The smaller one saves about a quarter of the memory of the server,
 * at the cost of more frequent minor garbage collections.
 *
 * <p>The server starts with the Spring context and stops with it. A change of the settings refreshes the context, so
 * the server restarts with the new settings. It is optional: when it is disabled, when Node.js or the bundle is
 * missing, or when it fails to start, OpenL Studio runs without it and logs a warning.
 *
 * <p>Each line the server writes goes to the {@code org.openl.studio.mcp.node} logger: standard output as INFO,
 * standard error as WARN. A platform thread of its own reads each stream. The read of the output of a process blocks
 * a thread for as long as the server runs. A virtual thread doing it holds one of the few carriers of the
 * virtual-thread scheduler, which has one per processor by default. On a machine of two processors, two readers hold
 * both carriers. No exchange of {@link McpProxy} ever runs.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
@RequiredArgsConstructor
class McpServerProcess implements SmartLifecycle, DisposableBean {

    private static final Logger NODE_LOG = LoggerFactory.getLogger("org.openl.studio.mcp.node");
    /** The end of the line the server logs once it listens, with the port it took. */
    private static final Pattern LISTENING = Pattern.compile("listening on .+, port (\\d+)$");
    private static final String BUNDLE = "/WEB-INF/mcp";
    private static final String ENTRY = "index.mjs";
    /** The V8 option of the young generation of 1 MB per semi-space. */
    private static final String SMALL_YOUNG_GENERATION = "--max-semi-space-size=1";
    private static final String LOOPBACK = "127.0.0.1";
    private static final long START_TIMEOUT_SECONDS = 20;
    private static final long STOP_TIMEOUT_SECONDS = 10;

    @Value("${mcp.enabled}")
    private final boolean enabled;
    @Value("${mcp.node}")
    private final String node;
    private final ServletContext servletContext;

    /** The running server, once it is started. */
    private final AtomicReference<@Nullable Process> process = new AtomicReference<>();
    /** The port the server listens on, once it names it. */
    private volatile int port;

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
            var listening = new CompletableFuture<Integer>();
            var started = launch(Path.of(home), listening);
            process.set(started);
            port = awaitPort(started, listening);
            log.info("The built-in MCP server is running at /mcp on port {}.", port);
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
        var running = process.getAndSet(null);
        port = 0;
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

    /** The address of the server while it runs and listens, else {@code null}. */
    @Nullable URI address() {
        var listening = port;
        return isRunning() && listening > 0 ? URI.create("http://" + LOOPBACK + ":" + listening) : null;
    }

    @Override
    public boolean isRunning() {
        var running = process.get();
        return running != null && running.isAlive();
    }

    /** Starts the server, and completes {@code listening} with the port the server names once it listens. */
    private Process launch(Path home, CompletableFuture<Integer> listening) throws IOException {
        var builder = new ProcessBuilder(node, SMALL_YOUNG_GENERATION, home.resolve(ENTRY).toString())
                .directory(home.toFile());
        configure(builder.environment());
        var started = builder.start();
        pipe(started.inputReader(StandardCharsets.UTF_8), line -> {
            NODE_LOG.info(line);
            var matcher = LISTENING.matcher(line);
            if (matcher.find()) {
                listening.complete(Integer.parseInt(matcher.group(1)));
            }
        });
        pipe(started.errorReader(StandardCharsets.UTF_8), NODE_LOG::warn);
        return started;
    }

    /**
     * Turns the environment of OpenL Studio into the one of the server: the interface it listens on, any free port
     * there, and the version of Studio it reports.
     *
     * <p>{@code NODE_USE_ENV_PROXY} is removed: with it, Node.js would send the calls to Studio through the proxy of the
     * environment instead of to the loopback interface.
     *
     * <p>The server sends OpenTelemetry data when OpenL Studio sends traces or metrics ({@code OTEL_SDK_DISABLED}), as
     * the service of OpenL Studio with the suffix {@code -mcp} ({@code OTEL_SERVICE_NAME}). The other {@code OTEL_*}
     * variables reach it as they are.
     */
    void configure(Map<String, String> env) {
        env.remove("NODE_USE_ENV_PROXY");
        env.put("PORT", "0");
        env.put("HOST", LOOPBACK);
        env.put("OPENL_VERSION", OpenLVersion.getVersion());
        var otel = new OtelEnvironment(env);
        env.put("OTEL_SDK_DISABLED", String.valueOf(!otel.sends(Signal.TRACES) && !otel.sends(Signal.METRICS)));
        env.put("OTEL_SERVICE_NAME", otel.serviceName("-mcp"));
    }

    private static void pipe(BufferedReader output, Consumer<String> logger) {
        // Not a virtual thread: the read blocks natively for as long as the server runs and holds a carrier of the
        // virtual-thread scheduler, which has one per processor by default. Two readers on two carriers starve every
        // exchange of McpProxy. A daemon thread does not keep the JVM alive after the context is closed.
        Thread.ofPlatform().daemon().name("studio-mcp-output").start(() -> {
            try (output) {
                output.lines().forEach(logger);
            } catch (IOException | UncheckedIOException e) {
                // The process is gone and its output with it
            }
        });
    }

    /** Waits until the server names the port it listens on. */
    private static int awaitPort(Process started, CompletableFuture<Integer> listening)
            throws IOException, InterruptedException {
        started.onExit().thenRun(() -> listening.completeExceptionally(
                new IOException("the server exited with code " + started.exitValue() + ".")));
        try {
            return listening.get(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof IOException cause ? cause : new IOException(e.getCause());
        } catch (TimeoutException e) {
            throw new IOException("the server did not start in " + START_TIMEOUT_SECONDS + " seconds.", e);
        }
    }
}
