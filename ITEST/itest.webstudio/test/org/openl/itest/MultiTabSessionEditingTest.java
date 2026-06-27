package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

/**
 * End-to-end test for per-tab project selection in the legacy JSF UI.
 *
 * <p>Two projects are opened in one HTTP session (as two browser tabs would). The legacy per-tab REST endpoint
 * {@code /web/compile/table/{id}} resolves its model from the session-global selection, which historically meant
 * a second tab clobbered the first. Each request now carries its tab's identity ({@code tabRepositoryId} /
 * {@code tabProject}); the server resolves that tab's own compiled model, so each tab sees only its own
 * project's tables. The decisive assertion is isolation: project A's table id resolves under tab A but not under
 * tab B, with everything else (session, endpoint, table id) held constant.
 */
class MultiTabSessionEditingTest {

    private static final String SETUP_RESOURCES = "test-resources-socket/projects-multi";
    // Base64 of "admin:admin" — the design repo administrator from application-multi.properties.
    private static final String ADMIN_BASIC = "Basic YWRtaW46YWRtaW4=";

    @AutoClose
    private static final HttpClient client = JettyServer.get().withProfile("multi").start();

    @Test
    void legacyCompileEndpointResolvesEachTabsOwnModel() {
        createAndOpen("MultiTabA");
        createAndOpen("MultiTabB");
        var tableIdA = firstTableId(projectId("MultiTabA"));
        var tableIdB = firstTableId(projectId("MultiTabB"));

        // Each tab's identity resolves THAT tab's project model, concurrently, in one session.
        assertTrue(tableFound(tableIdA, "MultiTabA"), "tab A must resolve project A and find A's table");
        assertTrue(tableFound(tableIdB, "MultiTabB"), "tab B must resolve project B and find B's table");

        // Isolation: the same request under tab B does NOT see project A's table — the cross-tab clobber is
        // gone. With a session-global selection both calls would have returned the same model.
        assertFalse(tableFound(tableIdA, "MultiTabB"), "tab B must not see project A's table");
    }

    private boolean tableFound(String tableId, String tabProject) {
        String url = "/web/compile/table/" + tableId
                + "?tabRepositoryId=design&tabProject=" + tabProject;
        JsonNode info = client.getForObject(url, JsonNode.class, 200, "Authorization", ADMIN_BASIC);
        JsonNode tableUrl = info.get("tableUrl");
        return tableUrl != null && !tableUrl.isNull();
    }

    private void createAndOpen(String project) {
        client.localEnv.put("PROJECT", project);
        client.test(SETUP_RESOURCES);
    }

    private static String projectId(String project) {
        return Base64.getEncoder().encodeToString(("design:" + project).getBytes(StandardCharsets.UTF_8));
    }

    private String firstTableId(String projectId) {
        JsonNode tables = client.getForObject("/rest/projects/" + projectId + "/tables", JsonNode.class, 200,
                "Authorization", ADMIN_BASIC);
        return tables.get("content").get(0).get("id").asText();
    }
}
