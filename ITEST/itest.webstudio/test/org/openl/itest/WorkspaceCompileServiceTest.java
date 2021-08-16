package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.springframework.http.HttpStatus;

public class WorkspaceCompileServiceTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.startWithWebXml("wcs");
        client = server.client();
    }

    @Ignore
    //Ignored because tableId generated based on absolute path to the xlsx file.
    //Uncomment this test when tableId will use relative path.
    @Test(timeout = 15_000)
    public void compile() {
        // Initialize WebStudio.
        client.send("workspace-compile/empty.get");
        // Init project compilation.
        client.send("workspace-compile/project.get");

        CompileProgress compileProgress;
        do {
            compileProgress = client.getForObject("/web/compile/progress/-1/-1", CompileProgress.class, HttpStatus.OK);
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
        TableErrorInfo tableErrorInfo = client.getForObject("/web/compile/table/4471a9b6968ca6fa141b5eeef129082e",
            TableErrorInfo.class, HttpStatus.OK);
        assertEquals("#local/Sample/Main/table", tableErrorInfo.tableUrl);
        assertEquals(1, tableErrorInfo.errors.length);
        assertEquals(true, tableErrorInfo.errors[0].hasStacktrace);
        assertEquals("B3", tableErrorInfo.errors[0].errorCell);
        assertEquals("There can be only one active table.", tableErrorInfo.errors[0].summary);
        assertEquals("ERROR", tableErrorInfo.errors[0].severity);
        String projectError = client.getForObject("/web/compile/error/" + tableErrorInfo.errors[0].id + "/false",
            String.class, HttpStatus.OK);
        assertTrue(projectError.startsWith("Error: There can be only one active table."));
        String moduleError = client.getForObject("/web/compile/error/" + tableErrorInfo.errors[0].id + "/true",
            String.class, HttpStatus.NO_CONTENT);
        assertNull(moduleError);
    }

    @AfterClass
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
