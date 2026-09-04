package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

/**
 * End-to-end test of the name a change ping carries, over a real WebSocket.
 *
 * <p>A ping tells the user's sessions that their workspace changed, and every session answers it by
 * re-reading the workspace. Naming the client behind the change lets the session that made it skip
 * that re-read, and it must never let a session skip a change made by anyone else — an integration
 * or an MCP server calling the REST API under the same credentials is a different client of the
 * same user.
 *
 * <p>Both scenarios write a file, which is the case with nothing on the request thread to publish
 * the change: the write reaches the sockets through the workspace files watcher, from the watcher's
 * own thread, and is named after the client that was writing just before. The per-project ping names
 * the file it touched, so each scenario waits for its own ping.
 */
class WebSocketChangeOriginTest {

    private static final String SETUP_RESOURCES = "test-resources-socket/projects-multi";
    private static final String MUTATIONS = "test-resources-socket/change-origin/";
    // Base64 of "admin:admin" — the design repo administrator from application.properties.
    private static final String ADMIN_BASIC = "Basic YWRtaW46YWRtaW4=";
    private static final String PROJECT = "WebSocketChangeOrigin";
    /** The id the browser tab of the scenario puts on its own request. */
    private static final String TAB = "tab-1";
    /**
     * How long a topic must stay silent to count as settled. The server coalesces a burst into one
     * ping per second, and the files watcher takes a moment to see a write, so a step that started
     * at once would be answered by the ping of the step before it.
     */
    private static final long SILENCE_MS = 3_000;

    /** When the topic last said anything, so a step can wait for the previous one to be over. */
    private static final AtomicLong lastPingAt = new AtomicLong();

    @AutoClose
    private static final HttpClient client = JettyServer.get().start();

    /** The body of a per-project change ping: {@code {"files": [...], "origins": [...]}}. */
    record ChangePing(List<String> files, List<String> origins) {
        ChangePing {
            files = files == null ? List.of() : files;
            origins = origins == null ? List.of() : origins;
        }
    }

    /** Just enough of the status response to learn the encoded project id the topics are keyed by. */
    record ProjectRef(String projectId) {
    }

    @Test
    void names_the_client_behind_a_change_and_never_lets_a_tab_skip_another_client_s() throws Exception {
        client.localEnv.put("PROJECT", PROJECT);
        client.test(SETUP_RESOURCES);
        var project = client.getForObject("/rest/projects/" + PROJECT + "/status", ProjectRef.class, 200,
                "Authorization", ADMIN_BASIC);
        // The server URL-encodes the project id in the destination, so mirror that here.
        var topic = "/user/topic/projects/"
                + URLEncoder.encode(project.projectId(), StandardCharsets.UTF_8) + "/changed";

        try (var stomp = new StompTester(client, client.getWebSocketURL("/rest/ws"),
                Map.of("Authorization", ADMIN_BASIC))) {
            // One subscription for the whole test that only notes the traffic, so a step can tell
            // the previous one is over. Its predicate never matches, so it never settles.
            stomp.awaitMatching(topic, ChangePing.class, ping -> {
                lastPingAt.set(System.currentTimeMillis());
                return false;
            });
            settle();

            // The user's own browser tab writes a file, naming itself on the request.
            var ownWrite = awaitPing(stomp, topic, "named-by-the-client.txt");
            client.test(MUTATIONS + "named");
            var own = ownWrite.get(30, TimeUnit.SECONDS);

            // The tab is named, so it recognises the ping as the echo of its own action and does not
            // re-read what it has just read. Named, not sole: the clients that wrote within the
            // server's recent-writer window are named as well — the setup of this test among them —
            // and for those the same ping is somebody else's change. Asserting the sole name would
            // only hold by waiting that window out, which ties the test to its length.
            assertTrue(own.origins().contains(TAB), "The client that wrote must be named, got: " + own.origins());

            settle();

            // Another client of the same user — an integration, an MCP server — writes without
            // naming itself.
            var foreignWrite = awaitPing(stomp, topic, "written-by-another-client.txt");
            client.test(MUTATIONS + "unnamed");
            var foreign = foreignWrite.get(30, TimeUnit.SECONDS);

            // It is named after its own session, never after the tab: a client reads a ping as its
            // own echo only when its id is the only name on it, so the tab still re-reads.
            assertFalse(foreign.origins().isEmpty(), "A change made through a request must be named");
            assertTrue(foreign.origins().stream().anyMatch(origin -> !TAB.equals(origin)),
                    "Another client's change must not read as the tab's own echo, got: " + foreign.origins());
        }
    }

    private static CompletableFuture<ChangePing> awaitPing(StompTester stomp, String topic, String file) {
        return stomp.awaitMatching(topic, ChangePing.class, ping -> ping.files().contains(file));
    }

    /** Waits until the topic has been silent long enough for the previous step to be over. */
    private static void settle() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollDelay(SILENCE_MS, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> System.currentTimeMillis() - lastPingAt.get() >= SILENCE_MS);
    }
}
