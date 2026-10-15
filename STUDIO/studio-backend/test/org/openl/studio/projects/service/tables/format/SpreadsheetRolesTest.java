package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.studio.projects.model.tables.RawTableRole.INPUT_TITLE;
import static org.openl.studio.projects.model.tables.RawTableRole.KEY;
import static org.openl.studio.projects.model.tables.RawTableRole.OUTPUT;
import static org.openl.studio.projects.model.tables.RawTableRole.OUTPUT_TITLE;
import static org.openl.studio.projects.model.tables.RawTableRole.PROPERTIES;
import static org.openl.studio.projects.model.tables.RawTableRole.RESULT;
import static org.openl.studio.projects.model.tables.RawTableRole.SECTION;
import static org.openl.studio.projects.model.tables.RawTableRole.SIGNATURE;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the roles of the cells of a Spreadsheet table, above all which cells it returns: the cells the compiler
 * returns are its result.
 *
 * @author Yury Molchan
 */
class SpreadsheetRolesTest {

    private static final String SHEET = "Calculations";

    /** A Spreadsheet returning a single value from its RETURN step, under a heading and with properties. */
    private static final int RETURNED_STEP = 1;
    /** A Spreadsheet returning a single value from its RETURN column. */
    private static final int RETURNED_COLUMN = 11;
    /** A Spreadsheet returning a single value from its last step, none of its steps named RETURN. */
    private static final int LAST_STEP = 17;
    /** A SpreadsheetResult returning two adjacent steps and one apart, marked with *. */
    private static final int MARKED_STEPS = 23;
    /** A SpreadsheetResult returning its steps under the one column marked with *. */
    private static final int MARKED_COLUMN = 31;
    /** A SpreadsheetResult returning the step and the column marked with *. */
    private static final int MARKED_BOTH = 37;
    /** A SpreadsheetResult with nothing marked * and a step left out of the result with ~. */
    private static final int NOTHING_MARKED = 43;
    /** A Spreadsheet returning nothing. */
    private static final int RETURNS_VOID = 49;

    @TempDir
    Path dir;

    private ProjectModel model;

    @BeforeEach
    void writeProject() throws IOException {
        model = TableTestProjects.projectModel(dir, SHEET, SpreadsheetRolesTest::fillSheet);
    }

    @Test
    void returnsTheStepNamedReturnOfASingleValue() {
        var roles = rolesAt(RETURNED_STEP);

        assertEquals(SIGNATURE, roles.roleAt(RETURNED_STEP, 2));
        assertEquals(PROPERTIES, roles.roleAt(RETURNED_STEP + 1, 3));
        assertEquals(INPUT_TITLE, roles.roleAt(RETURNED_STEP + 2, 1), "The title of the column of steps");
        assertEquals(OUTPUT_TITLE, roles.roleAt(RETURNED_STEP + 2, 2), "A title of a column");
        assertEquals(KEY, roles.roleAt(RETURNED_STEP + 3, 1), "The name of a step");
        assertEquals(OUTPUT, roles.roleAt(RETURNED_STEP + 3, 2));
        assertEquals(SECTION, roles.roleAt(RETURNED_STEP + 4, 1), "A step merged across its row heads a section");
        assertEquals(SECTION, roles.roleAt(RETURNED_STEP + 4, 3));
        assertEquals(RESULT, roles.roleAt(RETURNED_STEP + 6, 1), "The name of the step returned");
        assertEquals(RESULT, roles.roleAt(RETURNED_STEP + 6, 3), "Every cell of the step returned");
        assertEquals(OUTPUT, roles.roleAt(RETURNED_STEP + 7, 2), "A step after the one returned");
        assertEquals(RawTableValueType.CODE, roles.typeAt(RETURNED_STEP + 3, 2), "A formula is code");
        assertEquals(RawTableValueType.NUMBER, roles.typeAt(RETURNED_STEP + 7, 2), "A number of a step");
    }

    @Test
    void returnsTheColumnNamedReturnOfASingleValue() {
        var roles = rolesAt(RETURNED_COLUMN);

        assertEquals(OUTPUT_TITLE, roles.roleAt(RETURNED_COLUMN + 1, 2));
        assertEquals(RESULT, roles.roleAt(RETURNED_COLUMN + 1, 3), "The title of the column returned");
        assertEquals(KEY, roles.roleAt(RETURNED_COLUMN + 2, 1), "A step name is no part of the column");
        assertEquals(OUTPUT, roles.roleAt(RETURNED_COLUMN + 3, 2));
        assertEquals(RESULT, roles.roleAt(RETURNED_COLUMN + 3, 3));
    }

    @Test
    void returnsTheLastStepOfASingleValueNamingNoReturn() {
        var roles = rolesAt(LAST_STEP);

        assertEquals(KEY, roles.roleAt(LAST_STEP + 2, 1));
        assertEquals(OUTPUT, roles.roleAt(LAST_STEP + 2, 2));
        assertEquals(RESULT, roles.roleAt(LAST_STEP + 3, 1));
        assertEquals(RESULT, roles.roleAt(LAST_STEP + 3, 2));
    }

    @Test
    void returnsTheStepsMarkedOfASpreadsheetResult() {
        var roles = rolesAt(MARKED_STEPS);

        assertEquals(OUTPUT_TITLE, roles.roleAt(MARKED_STEPS + 1, 2), "No column is marked");
        assertEquals(KEY, roles.roleAt(MARKED_STEPS + 2, 1));
        assertEquals(RESULT, roles.roleAt(MARKED_STEPS + 3, 1));
        assertEquals(RESULT, roles.roleAt(MARKED_STEPS + 4, 2));
        assertEquals(OUTPUT, roles.roleAt(MARKED_STEPS + 5, 2));
        assertEquals(RESULT, roles.roleAt(MARKED_STEPS + 6, 2), "A marked step apart from the others");
        // The two adjacent steps are one region of each column, the step apart another.
        var results = roles.regions().regions().stream().filter(region -> region.role() == RESULT).toList();
        assertTrue(results.contains(new RawTableRegion(RESULT, null, 3, 0, 4, 0)), String.valueOf(results));
        assertTrue(results.contains(new RawTableRegion(RESULT, null, 6, 0, 6, 0)), String.valueOf(results));
    }

    @Test
    void returnsEveryStepUnderTheColumnMarkedOfASpreadsheetResult() {
        var roles = rolesAt(MARKED_COLUMN);

        assertEquals(OUTPUT_TITLE, roles.roleAt(MARKED_COLUMN + 1, 2));
        assertEquals(RESULT, roles.roleAt(MARKED_COLUMN + 1, 3), "The title of the column marked");
        assertEquals(KEY, roles.roleAt(MARKED_COLUMN + 2, 1), "No step is marked");
        assertEquals(OUTPUT, roles.roleAt(MARKED_COLUMN + 2, 2));
        assertEquals(RESULT, roles.roleAt(MARKED_COLUMN + 2, 3));
        assertEquals(RESULT, roles.roleAt(MARKED_COLUMN + 3, 3));
    }

    @Test
    void returnsTheStepsTimesTheColumnsMarkedOfASpreadsheetResult() {
        var roles = rolesAt(MARKED_BOTH);

        assertEquals(RESULT, roles.roleAt(MARKED_BOTH + 1, 3));
        assertEquals(RESULT, roles.roleAt(MARKED_BOTH + 3, 1));
        assertEquals(RESULT, roles.roleAt(MARKED_BOTH + 3, 3), "The marked step under the marked column");
        assertEquals(OUTPUT, roles.roleAt(MARKED_BOTH + 3, 2), "The marked step under another column");
        assertEquals(OUTPUT, roles.roleAt(MARKED_BOTH + 2, 3), "Another step under the marked column");
    }

    @Test
    void returnsNoCellOfASpreadsheetResultMarkingNothing() {
        var roles = rolesAt(NOTHING_MARKED);

        assertEquals(KEY, roles.roleAt(NOTHING_MARKED + 2, 1));
        assertEquals(KEY, roles.roleAt(NOTHING_MARKED + 3, 1), "A step left out with ~ reads as any step");
        assertEquals(OUTPUT, roles.roleAt(NOTHING_MARKED + 4, 2));
        assertTrue(roles.regions().regions().stream().noneMatch(region -> region.role() == RESULT));
    }

    @Test
    void returnsNothingOfASpreadsheetReturningVoid() {
        var roles = rolesAt(RETURNS_VOID);

        assertEquals(OUTPUT, roles.roleAt(RETURNS_VOID + 2, 2));
        assertTrue(roles.regions().regions().stream().map(RawTableRegion::role).noneMatch(RawTableRole.RESULT::equals));
    }

    private Roles rolesAt(int row) {
        return Roles.of(model.getAllTableSyntaxNodes().stream()
                .filter(node -> node.getGridTable().getRegion().getTop() == row)
                .map(TableSyntaxNodeAdapter::new)
                .findFirst()
                .orElseThrow());
    }

    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, RETURNED_STEP, 1, "Spreadsheet Double Premium ( Integer age )");
        merge(sheet, RETURNED_STEP, 1, 3);
        TableTestProjects.row(sheet, RETURNED_STEP + 1, 1, "properties", "description", "A premium");
        TableTestProjects.row(sheet, RETURNED_STEP + 2, 1, "Step", "Value", "Note");
        TableTestProjects.row(sheet, RETURNED_STEP + 3, 1, "Base", "= age * 2", "base");
        TableTestProjects.row(sheet, RETURNED_STEP + 4, 1, "Adjustments");
        merge(sheet, RETURNED_STEP + 4, 1, 3);
        TableTestProjects.row(sheet, RETURNED_STEP + 5, 1, "Factor", "= $Base * 3", "factor");
        TableTestProjects.row(sheet, RETURNED_STEP + 6, 1, "RETURN", "= $Factor", "result");
        TableTestProjects.row(sheet, RETURNED_STEP + 7, 1, "After");
        sheet.getRow(RETURNED_STEP + 7).createCell(2).setCellValue(7);

        TableTestProjects.row(sheet, RETURNED_COLUMN, 1, "Spreadsheet Double Rate ( Integer age )");
        merge(sheet, RETURNED_COLUMN, 1, 3);
        TableTestProjects.row(sheet, RETURNED_COLUMN + 1, 1, "Step", "Value", "RETURN");
        TableTestProjects.row(sheet, RETURNED_COLUMN + 2, 1, "Base", "= age", "");
        TableTestProjects.row(sheet, RETURNED_COLUMN + 3, 1, "Total", "= $Value$Base * 2", "= $Value$Total");

        TableTestProjects.row(sheet, LAST_STEP, 1, "Spreadsheet Double Total ( Integer age )");
        merge(sheet, LAST_STEP, 1, 2);
        TableTestProjects.row(sheet, LAST_STEP + 1, 1, "Step", "Value");
        TableTestProjects.row(sheet, LAST_STEP + 2, 1, "Base", "= age");
        TableTestProjects.row(sheet, LAST_STEP + 3, 1, "Total", "= $Base * 2");

        TableTestProjects.row(sheet, MARKED_STEPS, 1, "Spreadsheet SpreadsheetResult Quote ( Integer age )");
        merge(sheet, MARKED_STEPS, 1, 2);
        TableTestProjects.row(sheet, MARKED_STEPS + 1, 1, "Step", "Value");
        TableTestProjects.row(sheet, MARKED_STEPS + 2, 1, "Base", "= age");
        TableTestProjects.row(sheet, MARKED_STEPS + 3, 1, "Premium*", "= $Base * 2");
        TableTestProjects.row(sheet, MARKED_STEPS + 4, 1, "Tax*", "= $Premium / 10");
        TableTestProjects.row(sheet, MARKED_STEPS + 5, 1, "Audit", "= \"checked\"");
        TableTestProjects.row(sheet, MARKED_STEPS + 6, 1, "Fee*", "= 5");

        TableTestProjects.row(sheet, MARKED_COLUMN, 1, "Spreadsheet SpreadsheetResult Plan ( Integer age )");
        merge(sheet, MARKED_COLUMN, 1, 3);
        TableTestProjects.row(sheet, MARKED_COLUMN + 1, 1, "Step", "Draft", "Final*");
        TableTestProjects.row(sheet, MARKED_COLUMN + 2, 1, "Base", "= age", "= age + 1");
        TableTestProjects.row(sheet, MARKED_COLUMN + 3, 1, "Total", "= age * 2", "= age * 3");

        TableTestProjects.row(sheet, MARKED_BOTH, 1, "Spreadsheet SpreadsheetResult Offer ( Integer age )");
        merge(sheet, MARKED_BOTH, 1, 3);
        TableTestProjects.row(sheet, MARKED_BOTH + 1, 1, "Step", "Draft", "Final*");
        TableTestProjects.row(sheet, MARKED_BOTH + 2, 1, "Base", "= age", "= age + 1");
        TableTestProjects.row(sheet, MARKED_BOTH + 3, 1, "Total*", "= age * 2", "= age * 3");

        TableTestProjects.row(sheet, NOTHING_MARKED, 1, "Spreadsheet SpreadsheetResult Steps ( Integer age )");
        merge(sheet, NOTHING_MARKED, 1, 2);
        TableTestProjects.row(sheet, NOTHING_MARKED + 1, 1, "Step", "Value");
        TableTestProjects.row(sheet, NOTHING_MARKED + 2, 1, "Base", "= age");
        TableTestProjects.row(sheet, NOTHING_MARKED + 3, 1, "Scratch~", "= age + 1");
        TableTestProjects.row(sheet, NOTHING_MARKED + 4, 1, "Total", "= $Base * 2");

        TableTestProjects.row(sheet, RETURNS_VOID, 1, "Spreadsheet void Nothing ( Integer age )");
        merge(sheet, RETURNS_VOID, 1, 2);
        TableTestProjects.row(sheet, RETURNS_VOID + 1, 1, "Step", "Value");
        TableTestProjects.row(sheet, RETURNS_VOID + 2, 1, "Base", "= age");
    }

    private static void merge(Sheet sheet, int row, int left, int right) {
        sheet.addMergedRegion(new CellRangeAddress(row, row, left, right));
    }
}
