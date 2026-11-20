package org.openl.studio.projects.service.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.Props;
import org.openl.studio.projects.service.tables.TableModules;
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
                .runAll(mock(ProjectTestsExecutionProgressListener.class), model, TableModules.none(),
                        currentOpenedModule)
                .get();

        assertEquals(1, run.tables().size());
        assertEquals(List.of("MissingTest"), run.notRun());
    }

    @Test
    void runsNothingOnceItsThreadIsAskedToStop() throws Exception {
        var model = twoTestTables();
        var listener = mock(ProjectTestsExecutionProgressListener.class);
        Thread.currentThread().interrupt();
        try {
            var run = new TestsExecutorServiceImpl().runAll(listener, model, TableModules.none(), false).get();

            assertEquals(0, run.tables().size());
            verify(listener).onStatusChanged(TestExecutionStatus.INTERRUPTED);
            verify(listener, never()).onStatusChanged(TestExecutionStatus.COMPLETED);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void stopsOnceTheProjectIsCompiledAgain() throws Exception {
        var model = twoTestTables();
        var listener = mock(ProjectTestsExecutionProgressListener.class);
        // The project is compiled again once the first test table has run: the second one tests nothing it is now.
        doAnswer(call -> {
            model.reset(ReloadType.SINGLE, model.getModuleInfo());
            awaitCompiled(model);
            return null;
        }).when(listener).onTestUnitExecuted(any());

        var run = new TestsExecutorServiceImpl().runAll(listener, model, TableModules.none(), false).get();

        assertEquals(1, run.tables().size());
        verify(listener).onStatusChanged(TestExecutionStatus.INTERRUPTED);
        verify(listener, never()).onStatusChanged(TestExecutionStatus.COMPLETED);
    }

    @Test
    void reportsTheCasesOfATestTableThatRanBeforeItsThreadWasAskedToStop() throws Exception {
        var model = twoTestTables();
        var listener = mock(ProjectTestsExecutionProgressListener.class);
        var table = TableTestProjects.table(model, "CalcTest");
        Thread.currentThread().interrupt();
        try {
            var run = new TestsExecutorServiceImpl()
                    .runSingle(listener, model, TableModules.none(), table, null, false)
                    .get();

            assertEquals(1, run.tables().size());
            assertEquals(0, run.tables().getFirst().getNumberOfTestUnits());
            verify(listener).onStatusChanged(TestExecutionStatus.INTERRUPTED);
        } finally {
            Thread.interrupted();
        }
    }

    /** A run keeps each case the ranges name the way a run keeps it, and runs no other case. */
    @Test
    void keepsTheCasesTheRangesNameTheWayARunKeepsThem() throws Exception {
        var model = TableTestProjects.projectModel(dir, "Tests", sheet -> {
            write(sheet, 1, "Method int Calc(int x)");
            write(sheet, 2, "return x * 2;");
            write(sheet, 4, "Test Calc CalcTest");
            write(sheet, 5, "x", "_res_");
            write(sheet, 6, "X", "Result");
            write(sheet, 7, 1, 2);
            write(sheet, 8, 2, 4);
            write(sheet, 9, 3, 7);
        });
        awaitCompiled(model);
        var table = TableTestProjects.table(model, "CalcTest");

        var run = new TestsExecutorServiceImpl()
                .runSingle(mock(ProjectTestsExecutionProgressListener.class), model, TableModules.none(), table,
                        "2-3", false)
                .get();

        var results = run.tables().getFirst();
        var units = results.getTestUnits();
        assertTrue(units.stream().allMatch(RetainedTestUnit.class::isInstance));
        assertEquals(List.of(4, 6), units.stream().map(ITestUnit::getActualResult).toList());
        // The third case expects 7, so it fails, and the run counts it.
        assertEquals(1, results.getNumberOfFailures());
    }

    private ProjectModel twoTestTables() throws Exception {
        var model = TableTestProjects.projectModel(dir, "Tests", sheet -> {
            write(sheet, 1, "Method int Calc(int x)");
            write(sheet, 2, "return x * 2;");
            write(sheet, 4, "Test Calc CalcTest");
            write(sheet, 5, "x", "_res_");
            write(sheet, 6, "X", "Result");
            write(sheet, 7, 1, 2);
            write(sheet, 9, "Test Calc CalcAgainTest");
            write(sheet, 10, "x", "_res_");
            write(sheet, 11, "X", "Result");
            write(sheet, 12, 2, 4);
        });
        awaitCompiled(model);
        return model;
    }

    private static void awaitCompiled(ProjectModel model) {
        try {
            model.getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
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
