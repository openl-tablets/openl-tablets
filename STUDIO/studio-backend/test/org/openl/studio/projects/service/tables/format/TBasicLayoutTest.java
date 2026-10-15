package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the roles of a TBasic table: an algorithm written in steps, nested by the indent of their operations.
 */
class TBasicLayoutTest {

    private static final String SHEET = "Algorithms";

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

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, TBasicLayoutTest::fillSheet);
    }

    @Test
    void readsTheRolesOfATBasicTable() {
        var roles = Roles.of(factorial());

        assertEquals(RawTableRole.CODE, roles.roleAt(FACTORIAL_ROW + 1, OPERATION), "The ids of the columns");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(FACTORIAL_ROW + 2, LABEL));
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(FACTORIAL_ROW + 2, ACTION));
        assertEquals(RawTableRole.KEY, roles.roleAt(SUBROUTINE_STEP + 1, LABEL), "A label");
        assertEquals(RawTableRole.VALUE, roles.roleAt(FIRST_STEP, DESCRIPTION));
        assertEquals(RawTableValueType.STRING, roles.typeAt(FIRST_STEP, DESCRIPTION), "A description is text");
        assertEquals(RawTableRole.VALUE, roles.roleAt(FIRST_STEP, ACTION));
        assertEquals(RawTableValueType.CODE, roles.typeAt(FIRST_STEP, ACTION), "What a step runs is code");
        assertEquals(RawTableValueType.CODE, roles.typeAt(LOOP_STEP, CONDITION), "So is its condition");
        assertEquals(RawTableValueType.CODE, roles.typeAt(LOOP_STEP, OPERATION), "And its operation");
        assertEquals(RawTableRole.RESULT, roles.roleAt(RETURN_STEP, OPERATION), "A step that returns");
        assertEquals(RawTableRole.RESULT, roles.roleAt(RETURN_STEP, LABEL));
        assertEquals(RawTableValueType.CODE, roles.typeAt(RETURN_STEP, CONDITION));
        assertEquals(RawTableRole.SECTION, roles.roleAt(SUBROUTINE_STEP, LABEL), "A step starting a subroutine");
        assertEquals(RawTableRole.SECTION, roles.roleAt(SUBROUTINE_STEP, ACTION));
        assertEquals(RawTableRole.VALUE, roles.roleAt(LAST_STEP, OPERATION));
    }

    private IOpenLTable factorial() {
        return TableTestProjects.tableAt(dir, FACTORIAL_ROW);
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
