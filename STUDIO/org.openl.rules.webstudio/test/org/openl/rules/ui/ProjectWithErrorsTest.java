package org.openl.rules.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openl.config.ConfigurationManager;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.resolving.ProjectResolver;
import org.springframework.mock.env.MockEnvironment;

@RunWith(Parameterized.class)
public class ProjectWithErrorsTest extends AbstractWorkbookGeneratingTest {
    private static final String SHEET_NAME = "Test";
    private static final String MAIN_MODULE_FILE_NAME = "MainModule.xls";
    private ProjectModel pm;

    @Parameterized.Parameter
    public boolean singleModuleMode;

    @Parameterized.Parameters(name = "singleModuleMode: {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] { { true }, { false } });
    }

    @Before
    public void init() throws Exception {
        createMainModule();

        WebStudio ws = mock(WebStudio.class);
        when(ws.getSystemConfigManager()).thenReturn(new ConfigurationManager(null));
        when(ws.getProjectResolver()).thenReturn(ProjectResolver.instance());
        when(ws.isChangeableModuleMode()).thenReturn(true);

        pm = new ProjectModel(ws, new MockEnvironment());
        pm.setModuleInfo(getModules().get(0));
        if (singleModuleMode) {
            pm.useSingleModuleMode();
        } else {
            pm.useMultiModuleMode();
        }
    }

    @Test
    public void testTypesAndTestMethodsCount() throws Exception {
        assertEquals(singleModuleMode, pm.isSingleModuleMode());
        assertTrue(pm.getCompiledOpenClass().hasErrors());

        assertEquals(1, pm.getAllTestMethods().length);
        assertEquals(1, pm.getCompiledOpenClass().getOpenClassWithErrors().getTypes().size());
    }

    @Test
    public void testSingleModuleModeNotChangedAfterReset() throws Exception {
        assertEquals(singleModuleMode, pm.isSingleModuleMode());
        pm.reset(ReloadType.FORCED);
        assertEquals(singleModuleMode, pm.isSingleModuleMode());
    }

    private void createMainModule() throws IOException {
        Workbook book = new HSSFWorkbook();
        Sheet sheet = book.createSheet(SHEET_NAME);

        // Correct tables
        String expenseDatatypeTable[][] = { { "Datatype Expense" }, { "String", "area" } };

        String greetingMethodTable[][] = { { "Method String getGreeting(String name)" },
                { "Return \"Hi, \" + name;" } };
        String greetingTestTable[][] = { { "Test getGreeting getGreetingTest" },
                { "name", "_res_" },
                { "Name", "Result" },
                { "John", "Hi, John" } };

        // Tables with errors
        String incorrectDatatypeTable[][] = { { "Datatype Incorrect" }, { "NotExistedType", "area" } };
        String incorrectTestTable[][] = { { "Test notExistedMethod getGreetingTest" },
                { "name", "_res_" },
                { "Name", "Result" },
                { "John", "Hi, John" } };

        createTable(sheet, expenseDatatypeTable);
        createTable(sheet, incorrectDatatypeTable);
        createTable(sheet, greetingMethodTable);
        createTable(sheet, greetingTestTable);
        createTable(sheet, incorrectTestTable);
        writeBook(book, MAIN_MODULE_FILE_NAME);
    }
}
