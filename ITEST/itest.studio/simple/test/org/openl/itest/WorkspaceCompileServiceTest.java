package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

class WorkspaceCompileServiceTest {

    @AutoClose
    private static final HttpClient client = startServerWithSeededWorkspace();

    // The id of the local project in the workspace folder Sample: the 72-bit SHA-256 of "v1␟local␟Sample".
    private static final String PROJECT_ID = "4SCA27CWRT781";
    private static final String STATUS_TOPIC = "/user/topic/projects/" + PROJECT_ID + "/status";

    /**
     * Starts the server on a workspace seeded by this test: the Sample project files together with
     * the metainfo registry record which marks the project as a local one.
     */
    private static HttpClient startServerWithSeededWorkspace() {
        try {
            Path userDir = Path.of("target", "compile-workspace", "jdoe");
            LocalProjectFixture.seed(userDir, "Sample", Path.of("test-resources", "workspace-compile", "Sample.zip"));
            return JettyServer.get()
                    .withInitParam("user.workspace.home", "target/compile-workspace")
                    .start();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot seed the test workspace", e);
        }
    }

    @Test
    @Timeout(value = 15_000, unit = TimeUnit.MILLISECONDS)
    void compile() throws Exception {
        // Single-user mode authenticates every request as the one user, so the handshake needs no session.
        try (var stomp = new StompTester(client)) {
            // Subscribe BEFORE triggering compilation so we don't miss the terminal event.
            var terminal = stomp.awaitMatching(STATUS_TOPIC, ProjectStatus.class,
                    status -> "errors".equals(status.compileState));

            // Trigger project compilation.
            client.send("workspace-compile/compile-module.post");

            ProjectStatus status = terminal.get(10, TimeUnit.SECONDS);
            assertEquals("errors", status.compileState());
            assertEquals(2, status.compilation().modules().total());
            assertEquals(2, status.compilation().modules().compiled());
            assertEquals(4, status.compilation().messages().errors());
            assertEquals(0, status.compilation().messages().warnings());
        }
    }

    public record ProjectStatus(String compileState, Compilation compilation) {
    }

    public record Compilation(Modules modules, Messages messages) {
    }

    public record Modules(int total, int compiled) {
    }

    public record Messages(int errors, int warnings) {
    }
}
