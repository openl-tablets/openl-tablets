package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the roles of a ColumnMatch table: a decision tree that checks its arguments row by row, under the rows that
 * give what it returns or scores.
 */
class ColumnMatchLayoutTest {

    private static final String SHEET = "Matches";

    private static final int NAMES = 1;
    private static final int OPERATION = 2;
    private static final int VALUES = 3;
    private static final int LAST_VALUE = 4;

    /**
     * Who approves an expense, as the tutorial matches it: each area of expenses and the least money that needs the
     * approval, the money indented under its area.
     */
    private static final int APPROVAL_ROW = 1;
    private static final int RETURN_VALUES = APPROVAL_ROW + 3;
    private static final int HARDWARE = RETURN_VALUES + 1;
    private static final int SOFTWARE = HARDWARE + 2;
    private static final int APPROVAL_END = SOFTWARE + 1;

    /** A driver rating that weighs the score of each attribute: three rows give what the table returns and scores. */
    private static final int RATING_ROW = 12;
    private static final int WEIGHT = 3;
    private static final int RATING_VALUES = 4;
    private static final int RATING_RETURNS = RATING_ROW + 3;
    private static final int AGE = RATING_RETURNS + 3;

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, ColumnMatchLayoutTest::fillSheet);
    }

    @Test
    void readsTheRolesOfAColumnMatchTable() {
        var roles = Roles.of(tableAt(APPROVAL_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(APPROVAL_ROW + 1, NAMES), "The ids of the columns");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(APPROVAL_ROW + 2, VALUES));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(RETURN_VALUES, NAMES), "A row of what it returns");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(RETURN_VALUES, LAST_VALUE), "What it returns");
        assertEquals(RawTableRole.KEY, roles.roleAt(HARDWARE, NAMES), "The name a condition checks");
        assertEquals(RawTableRole.VALUE, roles.roleAt(HARDWARE, OPERATION));
        assertEquals(RawTableRole.VALUE, roles.roleAt(HARDWARE + 1, VALUES));
        // Each condition indented under another makes a group with it: a line over each group.
        assertEquals(new RawTableGroups(List.of(HARDWARE - APPROVAL_ROW, SOFTWARE - APPROVAL_ROW), List.of()),
                roles.regions().groups());

        var rating = Roles.of(tableAt(RATING_ROW));
        assertEquals(RawTableRole.OUTPUT_TITLE, rating.roleAt(RATING_RETURNS + 1, NAMES), "The total score row");
        assertEquals(RawTableRole.OUTPUT, rating.roleAt(RATING_RETURNS + 2, RATING_VALUES), "A score");
        assertEquals(RawTableRole.VALUE, rating.roleAt(AGE, WEIGHT), "A weight");
        assertEquals(RawTableRole.KEY, rating.roleAt(AGE + 1, NAMES));
        assertEquals(RawTableGroups.NONE, rating.regions().groups(), "No condition is indented: no group");
    }

    private IOpenLTable tableAt(int row) {
        return TableTestProjects.tableAt(dir, row);
    }

    /** A matched table with groups of conditions, and a weighted one without. */
    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, APPROVAL_ROW, NAMES,
                "ColumnMatch <MATCH> String NeedApprovalOf (String area, Integer money)");
        merge(sheet, APPROVAL_ROW, APPROVAL_ROW, NAMES, LAST_VALUE);
        TableTestProjects.row(sheet, APPROVAL_ROW + 1, NAMES, "names", "operation", "values", null);
        merge(sheet, APPROVAL_ROW + 1, APPROVAL_ROW + 1, VALUES, LAST_VALUE);
        TableTestProjects.row(sheet, APPROVAL_ROW + 2, NAMES, "Expenses", "Condition", "Need Approval Of", null);
        merge(sheet, APPROVAL_ROW + 2, APPROVAL_ROW + 2, VALUES, LAST_VALUE);
        TableTestProjects.row(sheet, RETURN_VALUES, NAMES, "Return Values", null, "CAO", "Team Lead");
        TableTestProjects.row(sheet, HARDWARE, NAMES, "area", "match", "Hardware", "Hardware");
        TableTestProjects.row(sheet, HARDWARE + 1, NAMES, "money", "min", "100000", "5000");
        indent(sheet, HARDWARE + 1, NAMES);
        TableTestProjects.row(sheet, SOFTWARE, NAMES, "area", "match", "Software", "Software");
        TableTestProjects.row(sheet, APPROVAL_END, NAMES, "money", "min", "20000", "1000");
        indent(sheet, APPROVAL_END, NAMES);

        TableTestProjects.row(sheet, RATING_ROW, NAMES,
                "ColumnMatch <WEIGHTED> String Rating (Integer age, String gender)");
        merge(sheet, RATING_ROW, RATING_ROW, NAMES, RATING_VALUES + 1);
        TableTestProjects.row(sheet, RATING_ROW + 1, NAMES, "names", "operation", "weight", "values", null);
        merge(sheet, RATING_ROW + 1, RATING_ROW + 1, RATING_VALUES, RATING_VALUES + 1);
        TableTestProjects.row(sheet, RATING_ROW + 2, NAMES, "Attribute", "Condition", "Weight", "Rating", null);
        merge(sheet, RATING_ROW + 2, RATING_ROW + 2, RATING_VALUES, RATING_VALUES + 1);
        TableTestProjects.row(sheet, RATING_RETURNS, NAMES, "Return Values", null, null, "High", "Low");
        TableTestProjects.row(sheet, RATING_RETURNS + 1, NAMES, "Total Score", "match", null, "50+", "< 50");
        TableTestProjects.row(sheet, RATING_RETURNS + 2, NAMES, "Score", null, null, "10", "1");
        TableTestProjects.row(sheet, AGE, NAMES, "age", "min", "5", "40", "16");
        TableTestProjects.row(sheet, AGE + 1, NAMES, "gender", "match", "5", "Male", "Female");
    }

    /** Indents the text of a cell by one level, which puts the condition into the group above. */
    private static void indent(Sheet sheet, int row, int column) {
        var style = sheet.getWorkbook().createCellStyle();
        style.setIndention((short) 1);
        sheet.getRow(row).getCell(column).setCellStyle(style);
    }
}
