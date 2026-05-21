package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.StompTester;

public class WorkspaceCompileServiceTest {

    @AutoClose
    private static final HttpClient client = JettyServer.get().withProfile("simple").start();

    // base64("local:Sample") — keep in sync with the project name/repository used by project.get.
    private static final String PROJECT_ID = "bG9jYWw6U2FtcGxl";
    private static final String STATUS_TOPIC = "/user/topic/projects/" + PROJECT_ID + "/status";

    @Test
    @Timeout(value = 15_000, unit = TimeUnit.MILLISECONDS)
    public void compile() throws Exception {
        // Initialize OpenL Studio (also creates the session cookie used by STOMP).
        client.send("workspace-compile/empty.get");

        try (var stomp = new StompTester(client)) {
            // Subscribe BEFORE triggering compilation so we don't miss the terminal event.
            var terminal = stomp.awaitMatching(STATUS_TOPIC, ProjectStatus.class,
                    status -> !"compiling".equals(status.compileState));

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
