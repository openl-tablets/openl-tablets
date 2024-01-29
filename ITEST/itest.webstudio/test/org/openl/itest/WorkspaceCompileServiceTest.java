package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class WorkspaceCompileServiceTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeAll
    public static void setUp() throws Exception {
        server = JettyServer.start("simple");
        client = server.client();
    }

    @Test
    @Timeout(value = 15_000, unit = TimeUnit.MILLISECONDS)
    public void compile() {
        // Initialize WebStudio.
        client.send("workspace-compile/empty.get");
        // Init project compilation.
        client.send("workspace-compile/project.get");

        CompileProgress compileProgress;
        do {
            compileProgress = client.getForObject("/web/compile/progress/-1/-1", CompileProgress.class, 200);
        } while (!compileProgress.compilationCompleted);
        assertEquals(2, compileProgress.modulesCount);
        assertEquals(4, compileProgress.errorsCount);
        assertEquals("new", compileProgress.dataType);
        assertEquals(0, compileProgress.warningsCount);
        assertEquals(2, compileProgress.modulesCompiled);

        client.send("workspace-compile/module.get");
        client.send("workspace-compile/tests.get");
        client.send("workspace-compile/table.tests.get");
        client.send("workspace-compile/table.errors.module.get");
        TableErrorInfo tableErrorInfo = client.getForObject("/web/compile/table/8e514ef161e2f50d730dde1fdc4fb4ac",
            TableErrorInfo.class, 200);
        assertEquals("#local/Sample/Main/table", tableErrorInfo.tableUrl);
        assertEquals(1, tableErrorInfo.errors.length);
        assertEquals(true, tableErrorInfo.errors[0].hasStacktrace);
        assertEquals("B3", tableErrorInfo.errors[0].errorCell);
        assertEquals("There can be only one active table.", tableErrorInfo.errors[0].summary);
        assertEquals("ERROR", tableErrorInfo.errors[0].severity);
        String projectError = client.getForObject("/web/message/" + tableErrorInfo.errors[0].id + "/stacktrace",
            String.class, 200);
        assertTrue(projectError.startsWith("Error: There can be only one active table."));
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.stop();
    }

    private static class CompileProgress {

        public int modulesCount;
        public boolean compilationCompleted;
        public int errorsCount;
        public String dataType;
        public int warningsCount;
        public int modulesCompiled;
    }

    public static class TableErrorInfo {
        public String tableUrl;
        public TableError[] errors;
    }

    public static class TableError {

        public int id;
        public String summary;
        public boolean hasStacktrace;
        public String errorCell;
        public String severity;

    }
}
