package org.openl.studio.projects.service.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.webstudio.web.Props;
import org.openl.studio.projects.service.tables.TableTestProjects;

class TestsExecutorServiceImplTest {

    @TempDir
    Path dir;

    private Environment previousEnvironment;

    @BeforeEach
    void setUp() {
        previousEnvironment = Props.getEnvironment();
        Props.setEnvironment(new MockEnvironment());
    }

    @AfterEach
    void tearDown() {
        Props.setEnvironment(previousEnvironment);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reportsOnlyTheTestTableThatCouldNotCompileAsNotRun(boolean currentOpenedModule) throws Exception {
        var model = TableTestProjects.projectModel(dir, "Tests", sheet -> {
            write(sheet, 1, "Method int Calc(int x)");
            write(sheet, 2, "return x * 2;");
            write(sheet, 4, "Test Calc CalcTest");
            write(sheet, 5, "x", "_res_");
            write(sheet, 6, "X", "Result");
            write(sheet, 7, 1, 2);
            write(sheet, 8, "not a number", 4);
            write(sheet, 10, "Test Missing MissingTest");
            write(sheet, 11, "x", "_res_");
            write(sheet, 12, "X", "Result");
            write(sheet, 13, 1, 2);
        });

        var run = new TestsExecutorServiceImpl()
                .runAll(mock(ProjectTestsExecutionProgressListener.class), model, currentOpenedModule)
                .get();

        assertEquals(1, run.tables().size());
        assertEquals(List.of("MissingTest"), run.notRun());
    }

    private static void write(Sheet sheet, int row, Object... values) {
        var sheetRow = sheet.createRow(row);
        for (var i = 0; i < values.length; i++) {
            var cell = sheetRow.createCell(i + 1);
            if (values[i] instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else {
                cell.setCellValue((String) values[i]);
            }
        }
    }
}
