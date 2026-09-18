package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

/**
 * End-to-end WebSocket test for the project compilation status stream in multi-user mode.
 *
 * <p>Flow exercised:
 * <ol>
 *     <li>OpenL Studio starts in multi-user mode.</li>
 *     <li>A project is created in the design repository (see {@code test-resources-socket/projects-multi}).</li>
 *     <li>{@code GET /projects/{id}/status} reports {@code idle} — nothing compiled yet.</li>
 *     <li>The client subscribes to the per-user status topic over {@code /rest/ws}, authenticated by an
 *     {@code Authorization: Basic} header the way any API client is.</li>
 *     <li>{@code GET /projects/{id}/tables} initializes project compilation.</li>
 *     <li>Compilation progress is pushed to the subscriber over the WebSocket.</li>
 * </ol>
 *
 * <p>The per-user destination ({@code /user/topic/projects/{id}/status}) is only delivered when the WebSocket
 * principal matches the user that triggered compilation, so receiving a frame confirms the handshake's
 * credentials became the STOMP principal.
 */
class WebSocketProjectStatusTest {

    private static final String SETUP_RESOURCES = "test-resources-socket/projects-multi";
    private static final String PROJECT = "WebSocketCompilationRest";
    // Base64 of "admin:admin" — the design repo administrator from application.properties.
    private static final String ADMIN_BASIC = "Basic YWRtaW46YWRtaW4=";
    // Compile states that mean the compile cycle has finished (progress was streamed to completion).
    private static final Set<String> TERMINAL_STATES = Set.of("ok", "warnings", "errors");

    @AutoClose
    private static final HttpClient client = JettyServer.get().start();

    /**
     * Minimal projection of {@code ProjectStatusViewModel}. {@code projectId} is the encoded
     * {@code ProjectIdModel} (base64 of {@code repo:name}); {@code compileState} is the lowercase
     * {@code CompileState}.
     */
    record StatusUpdate(String projectId, String compileState) {
    }

    @Test
    void streams_compilation_progress() throws Exception {
        // 1-2. Create and open the project (the {PROJECT} placeholder in the setup requests is resolved
        //      from localEnv).
        client.localEnv.put("PROJECT", PROJECT);
        client.test(SETUP_RESOURCES);

        var statusUrl = "/rest/projects/" + PROJECT + "/status";
        var tablesUrl = "/rest/projects/" + PROJECT + "/tables";

        // 3. Status is idle before any compilation is triggered. Reuse the encoded project id
        //    from the response so the WebSocket topic always matches what the server publishes.
        var initial = client.getForObject(statusUrl, StatusUpdate.class, 200, "Authorization", ADMIN_BASIC);
        assertEquals("idle", initial.compileState(), "Project must be idle before compilation");

        // 4. Subscribe to the per-user project status topic. The server URL-encodes the project id in
        //    the destination, so mirror that here.
        try (var stomp = new StompTester(client, client.getWebSocketBaseURL(),
                Map.of("Authorization", ADMIN_BASIC))) {
            var statusTopic = "/user/topic/projects/"
                    + URLEncoder.encode(initial.projectId(), StandardCharsets.UTF_8) + "/status";
            var compiled = stomp.awaitMatching(statusTopic, StatusUpdate.class,
                    u -> TERMINAL_STATES.contains(u.compileState()));

            // 5. Reading the tables initializes compilation (blocks until compiled).
            client.getForObject(tablesUrl, String.class, 200, "Authorization", ADMIN_BASIC);

            // 6. Compilation progress is pushed to the subscriber over the WebSocket.
            var terminal = compiled.get(30, TimeUnit.SECONDS);
            assertTrue(TERMINAL_STATES.contains(terminal.compileState()),
                    "Expected a terminal compile state over WebSocket, got: " + terminal.compileState());
        }
    }
}
