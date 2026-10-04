package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableTextRun;
import org.openl.studio.projects.model.tables.TableThemeView;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.read.RawTableRead;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Covers the look a theme gives each kind of decision table: a Rules table written either way round, a SimpleRules,
 * a SmartRules, a SimpleLookup and a SmartLookup table.
 */
class DecisionThemeLayoutTest {

    private static final String SHEET = "Rules";
    private static final String THEME = "default";

    /** The looks the default theme gives the places of a decision table. */
    private static final String WHITE = "#ffffff";
    private static final String MUTED = "#808080";
    private static final String TITLE = "#d0cece";
    private static final String HORIZONTAL = "#b4c6e7";
    private static final String RETURN_TITLE = "#b4c6e7";
    private static final String RETURN = "#ddebf7";

    /** A Rules table naming its rules, with a value of its first condition merged over the first two of them. */
    private static final String GREET = "Greet";
    private static final int GREET_ROW = 1;

    /**
     * A Rules table written the other way round, the kind, the code and the title of a column in each row, with a
     * value of its first condition merged over the first two rules.
     */
    private static final String GREET_ROUND = "GreetRound";
    private static final int GREET_ROUND_ROW = 11;

    /** A SimpleRules table whose first rule is written over two rows of the sheet. */
    private static final String GREETING = "Greeting";
    private static final int GREETING_ROW = 16;

    /** A SmartRules table naming its rules in its first column and returning a value of two fields. */
    private static final String HELLO = "Hello";
    private static final int HELLO_ROW = 23;

    /** A SimpleLookup table returning its values under one horizontal condition. */
    private static final String RATE = "Rate";
    private static final int RATE_ROW = 29;

    /** A SmartLookup table returning its values under two horizontal conditions. */
    private static final String FACTOR = "Factor";
    private static final int FACTOR_ROW = 36;

    /** A Rules table returning a type no table declares, so it does not compile. */
    private static final int BROKEN_ROW = 48;

    /** A theme giving each kind of decision table a look of its own. */
    private static final TableThemeService KINDS =
            new TableThemeService("classpath*:test-table-themes/decision-kinds.yaml");
    private static final String KINDS_THEME = "decision-kinds";

    private final TableThemeService service = new TableThemeService();

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, DecisionThemeLayoutTest::fillSheet);
    }

    @Test
    void offersEveryThemeForEveryKindOfDecisionTable() {
        for (var name : List.of(GREET, GREET_ROUND, GREETING, HELLO, RATE, FACTOR)) {
            assertEquals(List.of("default", "green"),
                    service.getThemes(table(name)).stream().map(TableThemeView::id).toList(), name);
        }
    }

    @Test
    void givesTheCodeOfARulesTableTheCodeLook() {
        var layout = layoutOf(GREET);

        // The kinds of the columns, their expressions and their parameters are muted and closed by one line.
        for (var row = GREET_ROW + 1; row <= GREET_ROW + 3; row++) {
            assertEquals(MUTED, layout.at(row, 2).style().color());
        }
        assertEquals(ThemeLineStyle.NONE, bottom(layout.at(GREET_ROW + 1, 2)), "No line between the rows of code");
        assertEquals(ThemeLineStyle.NONE, bottom(layout.at(GREET_ROW + 2, 4)));
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(GREET_ROW + 3, 1)));
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(GREET_ROW + 3, 4)));
    }

    @Test
    void givesTheConditionsAndTheReturnOfARulesTableTheirLooks() {
        var layout = layoutOf(GREET);

        // The column naming the rules is titled as the conditions are, then comes the title of what is returned.
        var titles = GREET_ROW + 4;
        for (var column = 1; column <= 3; column++) {
            assertEquals(TITLE, layout.at(titles, column).style().background());
            assertEquals(Boolean.TRUE, layout.at(titles, column).style().bold());
        }
        assertEquals(RETURN_TITLE, layout.at(titles, 4).style().background());
        // The names of the rules and the values of the conditions, a line between their columns, then the returns.
        var rule = GREET_ROW + 5;
        assertEquals(WHITE, layout.at(rule, 1).style().background());
        assertEquals(ThemeHorizontalAlign.CENTER, layout.at(rule, 1).style().align());
        assertEquals(ThemeLineStyle.THIN, layout.at(rule, 3).style().border().right().style());
        assertEquals(RETURN, layout.at(rule, 4).style().background());
        assertEquals(ThemeLineStyle.NONE, layout.at(rule, 4).style().border().right().style());
    }

    @Test
    void setsAGroupOfRulesApartWithALine() {
        var layout = layoutOf(GREET);

        // The first condition is merged over the first two rules: a line goes over them and over the rule after them.
        for (var column = 1; column <= 4; column++) {
            assertEquals(ThemeLineStyle.THIN, top(layout.at(GREET_ROW + 5, column)));
            assertEquals(ThemeLineStyle.THIN, top(layout.at(GREET_ROW + 7, column)));
        }
        assertEquals(ThemeLineStyle.NONE, top(layout.at(GREET_ROW + 6, 1)), "No line inside the group");
        assertEquals(ThemeLineStyle.NONE, top(layout.at(GREET_ROW + 6, 4)));
    }

    @Test
    void givesTheRulesOfATransposedRulesTableTheLookOfTheirPlace() {
        var layout = layoutOf(GREET_ROUND);
        var day = GREET_ROUND_ROW + 1;
        var result = GREET_ROUND_ROW + 3;

        // Each column is written in a row: its kind, its code and its parameters, its title, then a value of each rule.
        for (var column = 1; column <= 3; column++) {
            assertEquals(MUTED, layout.at(day, column).style().color());
        }
        assertEquals(TITLE, layout.at(day, 4).style().background());
        assertEquals(RETURN_TITLE, layout.at(result, 4).style().background());
        assertEquals(WHITE, layout.at(day + 1, 5).style().background());
        assertEquals(RETURN, layout.at(result, 6).style().background());
        // The last row as written closes the table.
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(result, 5)));
    }

    @Test
    void turnsTheLinesOfATransposedRulesTableWithItsRowsAndColumns() {
        var layout = layoutOf(GREET_ROUND);
        var day = GREET_ROUND_ROW + 1;
        var hour = GREET_ROUND_ROW + 2;

        // The line that closes the code stands on the right of its last column.
        assertEquals(ThemeLineStyle.THIN, right(layout.at(hour, 3)));
        assertEquals(ThemeLineStyle.NONE, right(layout.at(hour, 2)));
        // The lines between the conditions run between their rows.
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(day, 4)));
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(hour, 7)));
        assertEquals(ThemeLineStyle.NONE, right(layout.at(hour, 6)));
        // The first condition is merged over the first two rules: lines on the left of them and of the rule after them.
        for (var row = day; row <= GREET_ROUND_ROW + 3; row++) {
            assertEquals(ThemeLineStyle.THIN, left(layout.at(row, 5)));
            assertEquals(ThemeLineStyle.THIN, left(layout.at(row, 7)));
        }
        assertEquals(ThemeLineStyle.NONE, left(layout.at(hour, 6)), "No line inside the group");
        assertEquals(ThemeLineStyle.NONE, top(layout.at(hour, 5)), "The line over a group turns with the table");
    }

    @Test
    void givesASimpleRulesTableTheLookOfARulesTable() {
        var layout = layoutOf(GREETING);

        assertEquals(TITLE, layout.at(GREETING_ROW + 1, 1).style().background());
        assertEquals(RETURN_TITLE, layout.at(GREETING_ROW + 1, 2).style().background());
        assertEquals(WHITE, layout.at(GREETING_ROW + 2, 1).style().background());
        assertEquals(RETURN, layout.at(GREETING_ROW + 2, 2).style().background());
        // A rule written over two rows, with every value of it merged over both, is one rule, not a group.
        assertEquals(ThemeLineStyle.NONE, top(layout.at(GREETING_ROW + 2, 1)));
        assertEquals(ThemeLineStyle.NONE, top(layout.at(GREETING_ROW + 4, 2)));
    }

    @Test
    void givesTheColumnASmartRulesTableNamesItsRulesInTheLookOfACondition() {
        var layout = layoutOf(HELLO);
        var titles = HELLO_ROW + 1;

        assertEquals(TITLE, layout.at(titles, 1).style().background());
        assertEquals(TITLE, layout.at(titles, 2).style().background());
        // A value of two fields is returned in two columns.
        assertEquals(RETURN_TITLE, layout.at(titles, 3).style().background());
        assertEquals(RETURN_TITLE, layout.at(titles, 4).style().background());
        assertEquals(WHITE, layout.at(titles + 1, 1).style().background());
        assertEquals(ThemeLineStyle.THIN, layout.at(titles + 1, 1).style().border().right().style());
        assertEquals(RETURN, layout.at(titles + 1, 3).style().background());
        assertEquals(RETURN, layout.at(titles + 1, 4).style().background());
    }

    @Test
    void givesTheHorizontalConditionOfALookupTheHorizontalLook() {
        var layout = layoutOf(RATE);

        // The titles of the vertical conditions stand beside the values of the horizontal one.
        assertEquals(TITLE, layout.at(RATE_ROW + 1, 1).style().background());
        assertEquals(TITLE, layout.at(RATE_ROW + 1, 2).style().background());
        var horizontal = layout.at(RATE_ROW + 1, 3).style();
        assertEquals(HORIZONTAL, horizontal.background());
        assertEquals(Boolean.TRUE, horizontal.bold());
        assertEquals(ThemeLineStyle.THIN, horizontal.border().bottom().style());
        // The values the lookup returns stand where the conditions meet.
        assertEquals(WHITE, layout.at(RATE_ROW + 2, 2).style().background());
        assertEquals(RETURN, layout.at(RATE_ROW + 2, 3).style().background());
        assertEquals(RETURN, layout.at(RATE_ROW + 4, 4).style().background());
        // The first vertical condition is merged over two rules, which makes them a group.
        assertEquals(ThemeLineStyle.THIN, top(layout.at(RATE_ROW + 2, 3)));
        assertEquals(ThemeLineStyle.NONE, top(layout.at(RATE_ROW + 3, 3)));
        assertEquals(ThemeLineStyle.THIN, top(layout.at(RATE_ROW + 4, 3)));
    }

    @Test
    void givesEveryRowOfHorizontalConditionsTheHorizontalLook() {
        var layout = layoutOf(FACTOR);

        // The title of the vertical condition is merged down over both rows of horizontal conditions.
        assertEquals(TITLE, layout.at(FACTOR_ROW + 1, 1).style().background());
        assertEquals(TITLE, layout.at(FACTOR_ROW + 2, 1).style().background());
        for (var column = 2; column <= 5; column++) {
            assertEquals(HORIZONTAL, layout.at(FACTOR_ROW + 1, column).style().background());
            assertEquals(HORIZONTAL, layout.at(FACTOR_ROW + 2, column).style().background());
            assertEquals(RETURN, layout.at(FACTOR_ROW + 3, column).style().background());
        }
        assertEquals(WHITE, layout.at(FACTOR_ROW + 3, 1).style().background());
    }

    @Test
    void givesEachKindOfDecisionTableTheLookTheThemeWritesForIt() {
        assertEquals("#ddebf7", returnOf(GREET, GREET_ROW + 5, 4));
        assertEquals("#ddebf7", returnOf(GREETING, GREETING_ROW + 2, 2), "A SimpleRules looks like a Rules table");
        assertEquals("#e2efda", returnOf(HELLO, HELLO_ROW + 2, 3), "A SmartRules changes what it returns");
        assertEquals(Boolean.TRUE, KINDS.layoutOf(table(HELLO), KINDS_THEME).at(HELLO_ROW + 1, 2).style().bold(),
                "and keeps the rest of the look of a Rules table");
        assertEquals("#fff2cc", returnOf(RATE, RATE_ROW + 2, 3), "A SimpleLookup writes a look of its own");
        assertNull(KINDS.layoutOf(table(RATE), KINDS_THEME).at(RATE_ROW + 1, 1).style().bold());
        assertNull(returnOf(FACTOR, FACTOR_ROW + 3, 2), "A SmartLookup the theme writes nothing for takes the base");
    }

    @Test
    void givesADecisionTableThatDidNotCompileTheBaseAlone() {
        var layout = service.layoutOf(tableAt(BROKEN_ROW), THEME);

        // The header is signed as the header of every table is; the body takes the look every cell starts from.
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(BROKEN_ROW, 1)));
        assertEquals(Boolean.FALSE, layout.at(BROKEN_ROW + 1, 1).style().bold());
        assertEquals(WHITE, layout.at(BROKEN_ROW + 2, 2).style().background());
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(BROKEN_ROW + 2, 2)), "The last row closes the table");
    }

    @Test
    void writesTheLookOfADecisionTableIntoTheWorkbook() throws IOException {
        service.writer(THEME).writeAll(List.of(table(GREET)), Map.of());

        var written = read(GREET);
        assertEquals(List.of("Rules", " ", "String", " ", GREET, " ", "( String day, Integer hour )"),
                written.getFirst().getFirst().runs().stream().map(RawTableTextRun::text).toList());
        assertEquals(MUTED, written.get(1).get(1).style().color());
        assertEquals(TITLE, written.get(4).get(1).style().background());
        assertEquals(RETURN, written.get(5).get(3).style().background());
        try (var in = Files.newInputStream(dir.resolve(SHEET + ".xlsx")); var workbook = new XSSFWorkbook(in)) {
            var first = workbook.getSheet(SHEET).getRow(GREET_ROW + 5).getCell(1);
            assertEquals(BorderStyle.THIN, first.getCellStyle().getBorderTop(), "The line over the group");
        }
    }

    private IOpenLTable table(String name) {
        return TableTestProjects.table(TableTestProjects.projectModel(dir), name);
    }

    /** The table whose header stands in the given row: a table that did not compile has no name to find it by. */
    private IOpenLTable tableAt(int row) {
        return TableTestProjects.projectModel(dir).getAllTableSyntaxNodes().stream()
                .filter(node -> node.getGridTable().getRegion().getTop() == row)
                .map(TableSyntaxNodeAdapter::new)
                .findFirst()
                .orElseThrow();
    }

    private ThemedTable layoutOf(String name) {
        return service.layoutOf(table(name), THEME);
    }

    /** The fill the theme giving each kind a look of its own gives a cell of a table. */
    private @Nullable String returnOf(String name, int row, int column) {
        return KINDS.layoutOf(table(name), KINDS_THEME).at(row, column).style().background();
    }

    /** The cells of a table as the workbook now holds them, with their styles. */
    private List<List<RawTableCell>> read(String name) {
        return new RawTableReader().read(table(name), RawTableRead.builder().withStyles(true).build()).source;
    }

    private static ThemeLineStyle top(ThemedTable.ThemedCell cell) {
        return cell.style().border().top().style();
    }

    private static ThemeLineStyle bottom(ThemedTable.ThemedCell cell) {
        return cell.style().border().bottom().style();
    }

    private static ThemeLineStyle left(ThemedTable.ThemedCell cell) {
        return cell.style().border().left().style();
    }

    private static ThemeLineStyle right(ThemedTable.ThemedCell cell) {
        return cell.style().border().right().style();
    }

    /**
     * A Rules table written each way round, a SimpleRules, a SmartRules, a SimpleLookup and a SmartLookup table, the
     * Datatype the SmartRules returns, and a Rules table that does not compile.
     */
    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, GREET_ROW, 1, "Rules String " + GREET + " ( String day, Integer hour )");
        merge(sheet, GREET_ROW, GREET_ROW, 1, 4);
        TableTestProjects.row(sheet, GREET_ROW + 1, 1, "RULE", "C1", "C2", "RET1");
        TableTestProjects.row(sheet, GREET_ROW + 2, 1, null, "day == dayName", "hour < limit", "greeting");
        TableTestProjects.row(sheet, GREET_ROW + 3, 1, null, "String dayName", "Integer limit", "String greeting");
        TableTestProjects.row(sheet, GREET_ROW + 4, 1, "Rule", "Day", "Before", "Greeting");
        TableTestProjects.row(sheet, GREET_ROW + 5, 1, "R1", "Weekday", "12", "Good Morning");
        TableTestProjects.row(sheet, GREET_ROW + 6, 1, "R2", null, "24", "Good Evening");
        merge(sheet, GREET_ROW + 5, GREET_ROW + 6, 2, 2);
        TableTestProjects.row(sheet, GREET_ROW + 7, 1, "R3", "Weekend", "24", "Rest");

        TableTestProjects.row(sheet, GREET_ROUND_ROW, 1,
                "Rules String " + GREET_ROUND + " ( String day, Integer hour )");
        merge(sheet, GREET_ROUND_ROW, GREET_ROUND_ROW, 1, 7);
        TableTestProjects.row(sheet, GREET_ROUND_ROW + 1, 1, "C1", "day == dayName", "String dayName", "Day",
                "Weekday", null, "Weekend");
        merge(sheet, GREET_ROUND_ROW + 1, GREET_ROUND_ROW + 1, 5, 6);
        TableTestProjects.row(sheet, GREET_ROUND_ROW + 2, 1, "C2", "hour < limit", "Integer limit", "Before", "12",
                "24", "24");
        TableTestProjects.row(sheet, GREET_ROUND_ROW + 3, 1, "RET1", "greeting", "String greeting", "Greeting",
                "Good Morning", "Good Evening", "Rest");

        TableTestProjects.row(sheet, GREETING_ROW, 1, "SimpleRules String " + GREETING + " ( Integer hour )");
        merge(sheet, GREETING_ROW, GREETING_ROW, 1, 2);
        TableTestProjects.row(sheet, GREETING_ROW + 1, 1, "Hour", "Greeting");
        TableTestProjects.row(sheet, GREETING_ROW + 2, 1, "< 12", "Good Morning");
        TableTestProjects.row(sheet, GREETING_ROW + 3, 1, null, null);
        merge(sheet, GREETING_ROW + 2, GREETING_ROW + 3, 1, 1);
        merge(sheet, GREETING_ROW + 2, GREETING_ROW + 3, 2, 2);
        TableTestProjects.row(sheet, GREETING_ROW + 4, 1, ">= 12", "Good Day");

        TableTestProjects.row(sheet, HELLO_ROW, 1, "SmartRules Reply " + HELLO + " ( Integer hour )");
        merge(sheet, HELLO_ROW, HELLO_ROW, 1, 4);
        TableTestProjects.row(sheet, HELLO_ROW + 1, 1, "Rule", "Hour", "Text", "Rank");
        TableTestProjects.row(sheet, HELLO_ROW + 2, 1, "Morning", "< 12", "Good Morning", "1");
        TableTestProjects.row(sheet, HELLO_ROW + 3, 1, "Day", ">= 12", "Good Day", "2");

        TableTestProjects.row(sheet, RATE_ROW, 1,
                "SimpleLookup Double " + RATE + " ( String band, String day, Integer year )");
        merge(sheet, RATE_ROW, RATE_ROW, 1, 4);
        TableTestProjects.row(sheet, RATE_ROW + 1, 1, "Band", "Day", "1", "2");
        TableTestProjects.row(sheet, RATE_ROW + 2, 1, "A", "Weekday", "1.1", "1.2");
        TableTestProjects.row(sheet, RATE_ROW + 3, 1, null, "Weekend", "1.3", "1.4");
        merge(sheet, RATE_ROW + 2, RATE_ROW + 3, 1, 1);
        TableTestProjects.row(sheet, RATE_ROW + 4, 1, "B", "Weekday", "2.1", "2.2");

        TableTestProjects.row(sheet, FACTOR_ROW, 1,
                "SmartLookup Double " + FACTOR + " ( String band, Integer year, String day )");
        merge(sheet, FACTOR_ROW, FACTOR_ROW, 1, 5);
        TableTestProjects.row(sheet, FACTOR_ROW + 1, 1, "Band", "1", null, "2", null);
        TableTestProjects.row(sheet, FACTOR_ROW + 2, 1, null, "Weekday", "Weekend", "Weekday", "Weekend");
        merge(sheet, FACTOR_ROW + 1, FACTOR_ROW + 2, 1, 1);
        merge(sheet, FACTOR_ROW + 1, FACTOR_ROW + 1, 2, 3);
        merge(sheet, FACTOR_ROW + 1, FACTOR_ROW + 1, 4, 5);
        TableTestProjects.row(sheet, FACTOR_ROW + 3, 1, "A", "1.1", "1.2", "1.3", "1.4");
        TableTestProjects.row(sheet, FACTOR_ROW + 4, 1, "B", "2.1", "2.2", "2.3", "2.4");

        TableTestProjects.row(sheet, 43, 1, "Datatype Reply");
        TableTestProjects.row(sheet, 44, 1, "String", "text");
        TableTestProjects.row(sheet, 45, 1, "Integer", "rank");

        TableTestProjects.row(sheet, BROKEN_ROW, 1, "Rules Nothing Broken ( Integer hour )");
        merge(sheet, BROKEN_ROW, BROKEN_ROW, 1, 2);
        TableTestProjects.row(sheet, BROKEN_ROW + 1, 1, "C1", "RET1");
        TableTestProjects.row(sheet, BROKEN_ROW + 2, 1, "hour < 12", "greeting");
    }

    private static void merge(Sheet sheet, int top, int bottom, int left, int right) {
        sheet.addMergedRegion(new CellRangeAddress(top, bottom, left, right));
    }
}
