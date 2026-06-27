package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

/**
 * End-to-end test for parallel multi-project editing in multi-user mode.
 *
 * <p>Proves the core guarantee: editing one project does not clobber another opened project's compiled state.
 * Two projects are created and opened in the same session; both compile to {@code ok}; a table is then deleted
 * in the first project; the second project's compile status must remain {@code ok} (with the previous
 * single-model architecture, any edit reset the whole session, leaving the other project {@code idle}). It also
 * checks the edit took effect in the edited project only.
 */
class MultiProjectEditingTest {

    private static final String SETUP_RESOURCES = "test-resources-socket/projects-multi";
    // Base64 of "admin:admin" — the design repo administrator from application-multi.properties.
    private static final String ADMIN_BASIC = "Basic YWRtaW46YWRtaW4=";
    // Compile states that mean a project has been (re)compiled — i.e. it is not idle/reset.
    private static final Set<String> COMPILED_STATES = Set.of("ok", "warnings", "errors");

    @AutoClose
    private static final HttpClient client = JettyServer.get().withProfile("multi").start();

    /** Minimal projection of {@code ProjectStatusViewModel}; unknown fields are ignored. */
    record Status(String compileState) {
    }

    @Test
    void editingOneProjectDoesNotResetAnother() {
        createAndOpen("ParallelEditA");
        createAndOpen("ParallelEditB");
        var idA = projectId("ParallelEditA");
        var idB = projectId("ParallelEditB");

        // Reading the tables initializes compilation and blocks until compiled.
        var tablesA = awaitTables(idA);
        var tablesB = awaitTables(idB);
        assertEquals("ok", status(idA).compileState(), "project A must compile");
        assertEquals("ok", status(idB).compileState(), "project B must compile");

        int countABefore = tablesA.get("content").size();
        int countBBefore = tablesB.get("content").size();
        assertTrue(countABefore > 0, "project A must have a table to edit");
        var tableId = tablesA.get("content").get(0).get("id").asText();

        // Edit project A: delete one of its tables. With the old single-model architecture this reset the whole
        // session; now it only invalidates A (and its dependents).
        client.delete("/rest/projects/" + idA + "/tables/" + tableId, 204, "Authorization", ADMIN_BASIC);

        // The core guarantee: editing A must not reset B's compiled state.
        assertEquals("ok", status(idB).compileState(), "editing project A must not reset project B's compilation");

        // The edit took effect in A only.
        assertEquals(countABefore - 1, awaitTables(idA).get("content").size(),
                "the deleted table must be gone from project A");
        assertEquals(countBBefore, awaitTables(idB).get("content").size(),
                "project B's tables must be unchanged");
    }

    @Test
    void editingDependencyRecompilesDependent() {
        // Sample2 depends on Sample1 (declared in Sample2's rules.xml).
        client.test("test-resources-multi-deps");
        var idSample1 = projectId("Sample1");
        var idSample2 = projectId("Sample2");

        // Both compile. Sample2 reuses Sample1's compiled modules from the shared dependency manager rather
        // than recompiling them.
        var tables1 = awaitTables(idSample1);
        var count2Before = awaitTables(idSample2).get("content").size();
        assertEquals("ok", status(idSample1).compileState(), "dependency project must compile");
        assertEquals("ok", status(idSample2).compileState(), "dependent project must compile");
        assertTrue(tables1.get("content").size() > 0, "dependency must have a table to edit");
        var tableId = tables1.get("content").get(0).get("id").asText();

        // Edit the dependency (Sample1). Its open dependents must be eagerly recompiled; with the old
        // architecture this reset the whole session, leaving the dependent idle.
        client.delete("/rest/projects/" + idSample1 + "/tables/" + tableId, 204, "Authorization", ADMIN_BASIC);

        // The dependent was recompiled (against the changed dependency), not left reset/idle. Block on its
        // tables first so the eager background recompile has completed.
        awaitTables(idSample2);
        assertTrue(COMPILED_STATES.contains(status(idSample2).compileState()),
                "editing the dependency must eagerly recompile the dependent, not reset it");
        assertEquals(count2Before, awaitTables(idSample2).get("content").size(),
                "the dependent's own tables are unchanged");
    }

    private void createAndOpen(String project) {
        client.localEnv.put("PROJECT", project);
        client.test(SETUP_RESOURCES);
    }

    private static String projectId(String project) {
        return Base64.getEncoder().encodeToString(("design:" + project).getBytes(StandardCharsets.UTF_8));
    }

    private Status status(String projectId) {
        return client.getForObject("/rest/projects/" + projectId + "/status", Status.class, 200,
                "Authorization", ADMIN_BASIC);
    }

    private JsonNode awaitTables(String projectId) {
        return client.getForObject("/rest/projects/" + projectId + "/tables", JsonNode.class, 200,
                "Authorization", ADMIN_BASIC);
    }
}
