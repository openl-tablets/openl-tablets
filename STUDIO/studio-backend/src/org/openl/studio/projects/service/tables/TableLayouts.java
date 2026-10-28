package org.openl.studio.projects.service.tables;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.TableLayout;

/**
 * How a table is laid out on its sheet, as the compiler read it.
 *
 * <p>Answered for a test table, which is the one a screen has something to do with it: a test table is a table
 * of cases, and a reader refers to them by number — the third case, the one that failed. The numbers are
 * nowhere in the workbook. They follow from where the data begins, from which way the table is written and from
 * the lines each case takes, all of which the compiler knows and the cells do not.
 *
 * <p>Read from the compiled table rather than counted off its rows, so it holds however the table is written:
 * data down the rows, data across the columns, and a table of a single line.
 *
 * @author Vladyslav Pikus
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class TableLayouts {

    /**
     * How the given table is laid out, or {@code null} where nothing about it is worth saying.
     *
     * <p>Answered only for a test table for now — no other screen asks how a table is laid out. Nothing is
     * answered for one the compiler could not build, and for one whose cases carry identifiers of their own:
     * those name the cases already, and a number beside a name is one more thing to read and nothing more to
     * know.
     *
     * @param model the compiled module the table is read through
     * @param table the table being asked about
     */
    public static @Nullable TableLayout of(ProjectModel model, IOpenLTable table) {
        var uri = table.getUri();
        var method = model.isProjectCompilationCompleted()
                ? model.getMethod(uri)
                : model.getOpenedModuleMethod(uri);
        if (!(method instanceof TestSuiteMethod suite) || carriesItsOwnIdentifiers(suite)) {
            return null;
        }
        var node = suite.getBoundNode();
        var data = node == null ? null : node.getTable();
        if (data == null || data.getNumberOfRows() == 0) {
            return null;
        }
        // A line of data is a row of the table as the compiler reads it, which is a row of the sheet where the
        // table is written the usual way round and a column of it where the table is transposed.
        var downTheRows = data.getHeaderTable().isNormalOrientation();
        var grid = table.getGridTable();
        var origin = downTheRows ? grid.getGridRow(0, 0) : grid.getGridColumn(0, 0);
        var end = origin + (downTheRows ? grid.getHeight() : grid.getWidth());
        var starts = new int[data.getNumberOfRows()];
        for (var i = 0; i < starts.length; i++) {
            var cell = data.getRowTable(i).getCell(0, 0);
            starts[i] = downTheRows ? cell.getAbsoluteRow() : cell.getAbsoluteColumn();
        }
        return TableLayout.builder()
                .transposed(!downTheRows)
                .firstDataLine(starts[0] - origin)
                .caseLines(caseLinesOf(starts, end))
                .build();
    }

    /**
     * The number of lines each case takes, from where the cases begin to the end of the table, or {@code null}
     * where every case takes one line.
     */
    private static @Nullable List<Integer> caseLinesOf(int[] starts, int end) {
        var lines = new ArrayList<Integer>(starts.length);
        for (var i = 0; i < starts.length; i++) {
            lines.add((i + 1 < starts.length ? starts[i + 1] : end) - starts[i]);
        }
        return lines.stream().allMatch(line -> line == 1) ? null : lines;
    }

    /** Whether the table names its own cases, which is the table's business and not the screen's. */
    private static boolean carriesItsOwnIdentifiers(TestSuiteMethod suite) {
        try {
            var tests = suite.getTests();
            return tests.length == 0 || tests[0].hasId();
        } catch (Exception | LinkageError broken) {
            log.debug("Cannot read the cases of test table '{}'.", suite.getName(), broken);
            return true;
        }
    }
}
