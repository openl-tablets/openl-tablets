package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.read.RawTableRead;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Covers the look a theme gives a TBasic table: an algorithm written in steps, nested by the indent of their
 * operations.
 */
class TBasicThemeLayoutTest {

    private static final String SHEET = "Algorithms";

    /**
     * A theme that gives a TBasic table a look of its own, as the shipped themes do not: they draw it in the General
     * format of the standard.
     */
    private static final TableThemeService LOOKS = TestThemes.everyKind();

    /** The looks that theme gives the places of a TBasic table. */
    private static final String WHITE = "#ffffff";
    private static final String MUTED = "#808080";
    private static final String TITLE = "#b4c6e7";
    private static final String LABEL_TITLE = "#bfbfbf";
    private static final String CODE = "#ddebf7";

    /**
     * The factorial of a number, as the tutorial writes it: the steps in the main algorithm, then a subroutine the loop
     * calls by its label.
     */
    private static final int FACTORIAL_ROW = 1;
    private static final String FACTORIAL = "TBasic Integer Factorial (Integer n)";
    private static final int LABEL = 1;
    private static final int DESCRIPTION = 2;
    private static final int OPERATION = 3;
    private static final int CONDITION = 4;
    private static final int ACTION = 5;
    private static final int FIRST_STEP = FACTORIAL_ROW + 3;
    private static final int LOOP_STEP = FIRST_STEP + 2;
    private static final int RETURN_STEP = FIRST_STEP + 4;
    private static final int SUBROUTINE_STEP = FIRST_STEP + 5;
    private static final int LAST_STEP = FIRST_STEP + 7;

    /** A theme that gives the conditions of a TBasic table a look of their own, which the shipped themes do not. */
    private static final TableThemeService CONDITIONS =
            TestThemes.of("tbasic-condition.yaml");
    private static final String CONDITION_FILL = "#fff2cc";

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, TBasicThemeLayoutTest::fillSheet);
    }

    @Test
    void offersEveryThemeForATBasicTable() {
        assertTrue(ThemeLayouts.styles(factorial()));
    }

    @Test
    void signsATBasicTableAsASpreadsheet() {
        var runs = layout().at(FACTORIAL_ROW, LABEL).runs(FACTORIAL);

        assertEquals(List.of("TBasic", " ", "Integer", " ", "Factorial", " ", "(Integer n)"),
                runs.stream().map(run -> FACTORIAL.substring(run.start(), run.end())).toList());
        assertEquals(MUTED, runs.getFirst().style().color().rgb());
        assertEquals(Boolean.TRUE, runs.get(4).style().bold(), "The name of the table is bold");
    }

    @Test
    void givesTheIdsOfTheColumnsTheLookOfCodeAndTheTitlesTheLookOfASpreadsheet() {
        var layout = layout();

        for (var column = LABEL; column <= ACTION; column++) {
            var id = layout.at(FACTORIAL_ROW + 1, column).style();
            assertEquals(MUTED, id.color().rgb(), "column " + column);
            assertEquals(ThemeLineStyle.THIN, id.border().bottom().style(), "A line closes the ids");
            var title = layout.at(FACTORIAL_ROW + 2, column).style();
            assertEquals(Boolean.TRUE, title.bold(), "column " + column);
            assertEquals(column == LABEL ? LABEL_TITLE : TITLE, title.background().rgb(), "column " + column);
        }
    }

    @Test
    void fillsWhatAStepRunsAndLeavesTheRestOfItAsEveryCellStarts() {
        var layout = layout();

        assertEquals(CODE, layout.at(FIRST_STEP, ACTION).style().background().rgb(), "The action of a step");
        assertEquals(Boolean.FALSE, layout.at(FIRST_STEP, ACTION).style().bold());
        assertEquals(WHITE, layout.at(FIRST_STEP, CONDITION).style().background().rgb(),
                "The themes fill no condition");
        assertEquals(WHITE, layout.at(FIRST_STEP, LABEL).style().background().rgb(), "The label of a step");
        assertEquals(WHITE, layout.at(FIRST_STEP, DESCRIPTION).style().background().rgb());
        var operation = layout.at(LOOP_STEP + 1, OPERATION).style();
        assertEquals(WHITE, operation.background().rgb());
        assertEquals(ThemeHorizontalAlign.LEFT, operation.align(), "An indent shows in a text aligned to the left");
    }

    @Test
    void givesTheConditionsTheLookAThemeWritesForThem() {
        var layout = CONDITIONS.layoutOf(factorial());

        for (var row = FIRST_STEP; row <= LAST_STEP; row++) {
            assertEquals(CONDITION_FILL, layout.at(row, CONDITION).style().background().rgb(), "row " + row);
            assertNull(layout.at(row, ACTION).style().background(), "The theme writes nothing for the actions");
        }
    }

    @Test
    void headsASubroutineAsASectionAndMakesAStepThatReturnsBold() {
        var layout = layout();

        for (var column = LABEL; column <= ACTION; column++) {
            var subroutine = layout.at(SUBROUTINE_STEP, column).style();
            assertEquals(Boolean.TRUE, subroutine.italic(), "Every cell of the step that starts a subroutine");
            assertEquals(ThemeLineStyle.THIN, subroutine.border().top().style(), "A line sets the subroutine apart");
            assertEquals(Boolean.TRUE, layout.at(RETURN_STEP, column).style().bold(), "Every cell of the step");
            assertEquals(Boolean.FALSE, layout.at(LOOP_STEP, column).style().bold(), "A step that does not return");
        }
        assertEquals(CODE, layout.at(RETURN_STEP, ACTION).style().background().rgb(), "Bold over its own look");
    }

    @Test
    void closesTheLastStep() {
        var layout = layout();

        assertEquals(ThemeLineStyle.THIN, layout.at(LAST_STEP, ACTION).style().border().bottom().style());
        assertEquals(ThemeLineStyle.NONE, layout.at(LAST_STEP - 1, ACTION).style().border().bottom().style());
    }

    @Test
    void keepsTheIndentOfTheOperationsInTheWorkbook() {
        LOOKS.writer().format(factorial());

        var written = read();
        // The rows of the body follow the header: the ids, the titles, then the steps.
        var nested = written.get(LOOP_STEP + 1 - FACTORIAL_ROW).get(OPERATION - LABEL);
        assertEquals("SET", nested.value(), "The theme changes no text");
        assertEquals(1, nested.style().indent(), "The level of the step stays as it is");
        assertEquals(CODE, written.get(FIRST_STEP - FACTORIAL_ROW).get(ACTION - LABEL).style().background());
    }

    @Test
    void keepsTheIndentOfTheOperationsOnTheScreen() {
        var drawn = new RawTableReader().read(factorial(), RawTableRead.builder()
                .theme(LOOKS.layoutOf(factorial()))
                .build());

        // The rows of the body follow the header: the ids, the titles, then the steps.
        var nested = drawn.source.get(LOOP_STEP + 1 - FACTORIAL_ROW).get(OPERATION - LABEL);
        assertEquals(1, nested.style().indent(), "The indent nests the step, in the look of the theme");
    }

    private IOpenLTable factorial() {
        return TableTestProjects.tableAt(dir, FACTORIAL_ROW);
    }

    private ThemedTable layout() {
        return LOOKS.layoutOf(factorial());
    }

    /** The cells of the table as the workbook now holds them, with their styles. */
    private List<List<RawTableCell>> read() {
        return TableTestProjects.styledSource(factorial());
    }

    /** The factorial of a number, its steps nested by the indent of their operations. */
    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, FACTORIAL_ROW, LABEL, FACTORIAL);
        merge(sheet, FACTORIAL_ROW, FACTORIAL_ROW, LABEL, ACTION);
        TableTestProjects.row(sheet, FACTORIAL_ROW + 1, LABEL, "Label", "Description", "Operation", "Condition",
                "Action");
        TableTestProjects.row(sheet, FACTORIAL_ROW + 2, LABEL, "Name", "Description", "Operation", "Condition",
                "Action");
        TableTestProjects.row(sheet, FIRST_STEP, LABEL, null, "the result", "VAR", "factorial", "1");
        TableTestProjects.row(sheet, FIRST_STEP + 1, LABEL, null, "the multiplier", "VAR", "i", "1");
        TableTestProjects.row(sheet, LOOP_STEP, LABEL, null, null, "WHILE", "i <= n", null);
        TableTestProjects.row(sheet, LOOP_STEP + 1, LABEL, null, null, "SET", null, "Calculation");
        indent(sheet, LOOP_STEP + 1, OPERATION);
        TableTestProjects.row(sheet, RETURN_STEP, LABEL, null, null, "RETURN", "factorial", null);
        TableTestProjects.row(sheet, SUBROUTINE_STEP, LABEL, "Calculation", "multiplies by i", "SUB", null, null);
        TableTestProjects.row(sheet, SUBROUTINE_STEP + 1, LABEL, null, null, "SET", null, "factorial *= i");
        indent(sheet, SUBROUTINE_STEP + 1, OPERATION);
        TableTestProjects.row(sheet, LAST_STEP, LABEL, null, "the next multiplier", "SET", null, "i += 1");
        indent(sheet, LAST_STEP, OPERATION);
    }

    /** Indents the text of a cell by one level, which nests the step under the one above. */
    private static void indent(Sheet sheet, int row, int column) {
        var style = sheet.getWorkbook().createCellStyle();
        style.setIndention((short) 1);
        sheet.getRow(row).getCell(column).setCellStyle(style);
    }
}
