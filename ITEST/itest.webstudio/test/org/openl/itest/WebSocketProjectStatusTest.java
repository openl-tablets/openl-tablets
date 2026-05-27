package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

/**
 * End-to-end WebSocket test for the project compilation status stream in multi-user mode.
 *
 * <p>Flow exercised per scenario:
 * <ol>
 *     <li>OpenL Studio starts in the {@code multi} profile (multi-user mode).</li>
 *     <li>A project is created in the design repository (see {@code test-resources-socket/projects-multi}).</li>
 *     <li>{@code GET /projects/{id}/status} reports {@code idle} — nothing compiled yet.</li>
 *     <li>The client subscribes to the per-user status topic over a WebSocket endpoint.</li>
 *     <li>{@code GET /projects/{id}/tables} initializes project compilation.</li>
 *     <li>Compilation progress is pushed to the subscriber over the WebSocket.</li>
 * </ol>
 *
 * <p>Two endpoints are covered, proving the session is replicated to the STOMP principal in both cases:
 * <ul>
 *     <li><b>web-cookie</b> — {@code /web/ws}, authenticated by the session cookie (no Authorization header);</li>
 *     <li><b>rest-basic</b> — {@code /rest/ws}, authenticated by an {@code Authorization: Basic} header
 *     (cookie + basic), as a third-party client outside the main UI.</li>
 * </ul>
 * The per-user destination ({@code /user/topic/projects/{id}/status}) is only delivered when the WebSocket
 * principal matches the user that triggered compilation, so receiving a frame confirms the replication.
 *
 * <p>One server is started for the whole class ({@link AutoClose}). Each scenario provisions its own project
 * (a distinct {@code {PROJECT}} substituted into the setup requests) so it can observe a fresh {@code idle ->
 * compiled} transition on the shared server.
 */
class WebSocketProjectStatusTest {

    private static final String SETUP_RESOURCES = "test-resources-socket/projects-multi";
    // Base64 of "admin:admin" — the design repo administrator from application-multi.properties.
    private static final String ADMIN_BASIC = "Basic YWRtaW46YWRtaW4=";
    // Compile states that mean the compile cycle has finished (progress was streamed to completion).
    private static final Set<String> TERMINAL_STATES = Set.of("ok", "warnings", "errors");

    @AutoClose
    private static final HttpClient client = JettyServer.get().withProfile("multi").start();

    record Scenario(String name,
                    String httpPrefix,
                    String wsPath,
                    String project,
                    String[] requestHeaders,
                    Map<String, String> handshakeHeaders) {

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Minimal projection of {@code ProjectStatusViewModel}. {@code projectId} is the encoded
     * {@code ProjectIdModel} (base64 of {@code repo:name}); {@code compileState} is the lowercase
     * {@code CompileState}.
     */
    record StatusUpdate(String projectId, String compileState) {
    }

    static Scenario[] scenarios() {
        return new Scenario[]{
                // Native UI: the /web/ws handshake is permitted anonymously and the security context
                // comes from the session cookie established by the login step.
                new Scenario("web-cookie", "/web", "/web/ws", "WebSocketCompilationWeb", new String[0], Map.of()),
                // Third-party client: the /rest/ws handshake is authenticated by the Authorization header
                // through the /rest/** security chain; the cookie is sent as well (cookie + basic auth).
                new Scenario("rest-basic", "/rest", "/rest/ws", "WebSocketCompilationRest",
                        new String[]{"Authorization", ADMIN_BASIC},
                        Map.of("Authorization", ADMIN_BASIC))
        };
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("scenarios")
    void streams_compilation_progress(Scenario s) throws Exception {
        // 1-2. Log in as admin and create + open this scenario's project (the {PROJECT} placeholder in
        //      the setup requests is resolved from localEnv).
        client.localEnv.put("PROJECT", s.project());
        client.test(SETUP_RESOURCES);

        var statusUrl = s.httpPrefix() + "/projects/" + s.project() + "/status";
        var tablesUrl = s.httpPrefix() + "/projects/" + s.project() + "/tables";

        // 3. Status is idle before any compilation is triggered. Reuse the encoded project id
        //    from the response so the WebSocket topic always matches what the server publishes.
        var initial = client.getForObject(statusUrl, StatusUpdate.class, 200, s.requestHeaders());
        assertEquals("idle", initial.compileState(), "Project must be idle before compilation");

        // 4. Subscribe to the per-user project status topic over the chosen WebSocket endpoint.
        //    The server URL-encodes the project id in the destination, so mirror that here.
        try (var stomp = new StompTester(client, client.getWebSocketURL(s.wsPath()), s.handshakeHeaders())) {
            var statusTopic = "/user/topic/projects/"
                    + URLEncoder.encode(initial.projectId(), StandardCharsets.UTF_8) + "/status";
            var compiled = stomp.awaitMatching(statusTopic, StatusUpdate.class,
                    u -> TERMINAL_STATES.contains(u.compileState()));

            // 5. Reading the tables initializes compilation (blocks until compiled).
            client.getForObject(tablesUrl, String.class, 200, s.requestHeaders());

            // 6. Compilation progress is pushed to the subscriber over the WebSocket.
            var terminal = compiled.get(30, TimeUnit.SECONDS);
            assertTrue(TERMINAL_STATES.contains(terminal.compileState()),
                    "Expected a terminal compile state over WebSocket, got: " + terminal.compileState());
        }
    }
}
