package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.TableThemeView;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the look a theme gives a ColumnMatch table: a decision tree that checks its arguments row by row, under the
 * rows that give what it returns or scores.
 */
class ColumnMatchThemeLayoutTest {

    private static final String SHEET = "Matches";
    private static final String THEME = "standard";

    /** The looks the standard theme gives the places of a ColumnMatch table. */
    private static final String WHITE = "#ffffff";
    private static final String MUTED = "#808080";
    private static final String TITLE = "#bfbfbf";
    private static final String RETURN_TITLE = "#b4c6e7";
    private static final String RETURNED = "#ddebf7";

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

    private final TableThemeService service = new TableThemeService();

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, ColumnMatchThemeLayoutTest::fillSheet);
    }

    @Test
    void offersEveryThemeForAColumnMatchTable() {
        assertEquals(List.of("green", "standard"),
                service.getThemes(tableAt(APPROVAL_ROW)).stream().map(TableThemeView::id).toList());
    }

    @Test
    void signsTheAlgorithmWithTheTypeTheTableReturns() {
        var header = "ColumnMatch <MATCH> String NeedApprovalOf (String area, Integer money)";
        var runs = layoutAt(APPROVAL_ROW).at(APPROVAL_ROW, NAMES).runs(header);

        assertEquals("<MATCH> String", header.substring(runs.get(2).start(), runs.get(2).end()));
        assertEquals(MUTED, runs.get(2).style().color().rgb());
        assertEquals(Boolean.TRUE, runs.get(4).style().bold(), "The name of the table is bold");
    }

    @Test
    void givesTheIdsTheLookOfCodeAndTheTitlesTheLookOfTheTitlesOfARulesTable() {
        var layout = layoutAt(APPROVAL_ROW);

        for (var column = NAMES; column <= LAST_VALUE; column++) {
            var id = layout.at(APPROVAL_ROW + 1, column).style();
            assertEquals(MUTED, id.color().rgb(), "column " + column);
            assertEquals(ThemeLineStyle.THIN, id.border().bottom().style(), "A line closes the ids");
            assertEquals(TITLE, layout.at(APPROVAL_ROW + 2, column).style().background().rgb(), "column " + column);
        }
    }

    @Test
    void givesTheRowOfReturnValuesTheLookOfTheReturnsOfARulesTable() {
        var layout = layoutAt(APPROVAL_ROW);

        assertEquals(RETURN_TITLE, layout.at(RETURN_VALUES, NAMES).style().background().rgb());
        assertEquals(RETURN_TITLE, layout.at(RETURN_VALUES, OPERATION).style().background().rgb());
        // Every value the table returns, with a line between them.
        for (var column = VALUES; column <= LAST_VALUE; column++) {
            var returned = layout.at(RETURN_VALUES, column).style();
            assertEquals(RETURNED, returned.background().rgb(), "column " + column);
            assertEquals(ThemeLineStyle.THIN, returned.border().right().style(), "column " + column);
        }
    }

    @Test
    void setsTheNamesApartFromWhatTheConditionsCheckThemWithAndAgainst() {
        var layout = layoutAt(APPROVAL_ROW);

        var name = layout.at(HARDWARE + 1, NAMES).style();
        assertEquals(WHITE, name.background().rgb());
        assertEquals(ThemeHorizontalAlign.LEFT, name.align(), "An indent shows in a text aligned to the left");
        assertEquals(ThemeLineStyle.THIN, name.border().right().style(), "A line after the names");
        for (var column = OPERATION; column <= LAST_VALUE; column++) {
            var value = layout.at(HARDWARE + 1, column).style();
            assertEquals(ThemeHorizontalAlign.CENTER, value.align(), "column " + column);
            assertEquals(ThemeLineStyle.THIN, value.border().right().style(), "column " + column);
        }
    }

    @Test
    void setsEveryGroupOfConditionsCheckedTogetherApart() {
        var layout = layoutAt(APPROVAL_ROW);

        for (var column = NAMES; column <= LAST_VALUE; column++) {
            // A line over the first row of each group, the second one also standing after the first group.
            assertEquals(ThemeLineStyle.THIN, top(layout.at(HARDWARE, column)), "column " + column);
            assertEquals(ThemeLineStyle.NONE, top(layout.at(HARDWARE + 1, column)), "column " + column);
            assertEquals(ThemeLineStyle.THIN, top(layout.at(SOFTWARE, column)), "column " + column);
            assertEquals(ThemeLineStyle.THIN, layout.at(APPROVAL_END, column).style().border().bottom().style(),
                    "The last row closes the table");
        }
    }

    @Test
    void givesTheThreeRowsOfAWeightedTableTheLookOfTheReturns() {
        var layout = layoutAt(RATING_ROW);

        for (var row = RATING_RETURNS; row < AGE; row++) {
            assertEquals(RETURN_TITLE, layout.at(row, NAMES).style().background().rgb(), "row " + row);
            assertEquals(RETURN_TITLE, layout.at(row, WEIGHT).style().background().rgb(), "row " + row);
            assertEquals(RETURNED, layout.at(row, RATING_VALUES).style().background().rgb(), "row " + row);
        }
        assertEquals(WHITE, layout.at(AGE, NAMES).style().background().rgb(), "The first condition");
        assertEquals(WHITE, layout.at(AGE, RATING_VALUES).style().background().rgb());
        assertEquals(ThemeLineStyle.NONE, top(layout.at(AGE + 1, NAMES)), "No condition is indented: no group");
    }

    @Test
    void keepsTheIndentOfTheNamesInTheWorkbook() {
        service.writer(THEME).writeAll(List.of(tableAt(APPROVAL_ROW)), Map.of());

        // The rows read start at the header, and the columns at the names.
        var written = read(APPROVAL_ROW);
        var money = written.get(HARDWARE + 1 - APPROVAL_ROW).getFirst();
        assertEquals("money", money.value(), "The theme changes no text");
        assertEquals(1, money.style().indent(), "The condition stays in the group");
        assertEquals(RETURNED, written.get(RETURN_VALUES - APPROVAL_ROW).get(VALUES - NAMES).style().background());
    }

    private IOpenLTable tableAt(int row) {
        return TableTestProjects.tableAt(dir, row);
    }

    private ThemedTable layoutAt(int row) {
        return service.layoutOf(tableAt(row), THEME);
    }

    /** The cells of a table as the workbook now holds them, with their styles. */
    private List<List<RawTableCell>> read(int row) {
        return TableTestProjects.styledSource(tableAt(row));
    }

    private static ThemeLineStyle top(ThemedTable.ThemedCell cell) {
        return cell.style().border().top().style();
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
