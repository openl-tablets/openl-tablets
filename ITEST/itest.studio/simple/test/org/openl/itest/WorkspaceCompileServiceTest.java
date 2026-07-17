package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

class WorkspaceCompileServiceTest {

    @AutoClose
    private static final HttpClient client = startServerWithSeededWorkspace();

    // base64("local:Sample") — keep in sync with the project name/repository used by project.get.
    private static final String PROJECT_ID = "bG9jYWw6U2FtcGxl";
    private static final String STATUS_TOPIC = "/user/topic/projects/" + PROJECT_ID + "/status";

    /**
     * Starts the server on a workspace seeded by this test: the Sample project files together with
     * the metainfo registry record which marks the project as a local one.
     */
    private static HttpClient startServerWithSeededWorkspace() {
        try {
            Path userDir = Path.of("target", "compile-workspace", "jdoe");
            seedLocalProject(userDir, "Sample", Path.of("test-resources", "workspace-compile", "Sample.zip"));
            return JettyServer.get()
                    .withInitParam("user.workspace.home", "target/compile-workspace")
                    .start();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot seed the test workspace", e);
        }
    }

    private static void seedLocalProject(Path userDir, String projectName, Path zip) throws IOException {
        Path projectDir = userDir.resolve(projectName).normalize();
        try (var stream = new ZipInputStream(Files.newInputStream(zip))) {
            for (var entry = stream.getNextEntry(); entry != null; entry = stream.getNextEntry()) {
                Path file = projectDir.resolve(entry.getName()).normalize();
                if (!file.startsWith(projectDir)) {
                    throw new IOException("Zip entry escapes the project folder: " + entry.getName());
                }
                Files.createDirectories(file.getParent());
                Files.copy(stream, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        // The registry record makes the folder a registered local project: folders without a record
        // are deleted at the first workspace load.
        Path metainfo = userDir.resolve(".metainfo");
        Files.createDirectories(metainfo);
        Files.writeString(metainfo.resolve(projectName + ".properties"), "format-version=1\nrepository-id=local\n");
    }

    @Test
    @Timeout(value = 15_000, unit = TimeUnit.MILLISECONDS)
    void compile() throws Exception {
        // Initialize OpenL Studio (also creates the session cookie used by STOMP).
        client.send("workspace-compile/empty.get");

        try (var stomp = new StompTester(client)) {
            // Subscribe BEFORE triggering compilation so we don't miss the terminal event.
            var terminal = stomp.awaitMatching(STATUS_TOPIC, ProjectStatus.class,
                    status -> "errors".equals(status.compileState));

            // Trigger project compilation.
            client.send("workspace-compile/project.get");

            ProjectStatus status = terminal.get(10, TimeUnit.SECONDS);
            assertEquals("errors", status.compileState());
            assertEquals(2, status.compilation().modules().total());
            assertEquals(2, status.compilation().modules().compiled());
            assertEquals(4, status.compilation().messages().errors());
            assertEquals(0, status.compilation().messages().warnings());
        }

        client.send("workspace-compile/table.tests.get");
        client.send("workspace-compile/table.errors.module.get");
        TableErrorInfo tableErrorInfo = client.getForObject("/web/compile/table/8e514ef161e2f50d730dde1fdc4fb4ac",
                TableErrorInfo.class, 200);
        assertEquals("#local/Sample/Main/table", tableErrorInfo.tableUrl());
        assertEquals(1, tableErrorInfo.errors().length);
        TableError firstError = tableErrorInfo.errors()[0];
        assertTrue(firstError.hasStacktrace());
        assertEquals("B3", firstError.errorCell());
        assertEquals("There can be only one active table.", firstError.summary());
        assertEquals("ERROR", firstError.severity());
        String projectError = client.getForObject("/web/message/" + firstError.id() + "/stacktrace",
                String.class, 200);
        assertTrue(projectError.startsWith("Error: There can be only one active table."));
    }

    public record ProjectStatus(String compileState, Compilation compilation) {
    }

    public record Compilation(Modules modules, Messages messages) {
    }

    public record Modules(int total, int compiled) {
    }

    public record Messages(int errors, int warnings) {
    }

    public record TableErrorInfo(String tableUrl, TableError[] errors) {
    }

    public record TableError(int id, String summary, boolean hasStacktrace, String errorCell, String severity) {
    }
}
