package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.RawTableBorderLineStyle;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableHorizontalAlign;
import org.openl.studio.projects.model.tables.RawTableStyleSource;
import org.openl.studio.projects.model.tables.RawTableTextRun;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableThemeView;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.read.RawTableRead;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Covers the table theme end to end: the theme file, the look each cell gets, the look drawn over a table on the
 * screen, and the look written into the workbook.
 */
class TableThemeTest {

    private static final String SHEET = "Model";

    /** The most cell styles an .xls workbook holds, as POI counts them. */
    private static final int XLS_STYLES = 4030;
    private static final String HEADER_BACKGROUND = "#b4c6e7";
    private static final String NAME_BACKGROUND = "#ddebf7";
    /** The fill of a cell the theme fills no other way. */
    private static final String WHITE = "#ffffff";
    /** The fill the workbook gives the values of the Vocabulary. */
    private static final String GREY = "#c0c0c0";
    private static final String PERCENT = "0.00%";

    private static final String THEME = "default";

    /** A Datatype whose header is not merged over the table, written bold in 10.5 points. */
    private static final String ACCOUNT = "Account";
    private static final int ACCOUNT_ROW = 24;
    private static final short HALF_POINT_HEIGHT = 210;

    /** A Datatype written transposed: a field in each column, with its type, name and default one under another. */
    private static final String POINT = "Point";
    private static final int POINT_ROW = 29;

    /** A transposed Datatype that names its rows in its first column. */
    private static final String ITEM = "Item";
    private static final int ITEM_ROW = 35;

    /** A Spreadsheet whose properties take two rows of the sheet, returning the step named RETURN. */
    private static final String RATED = "Rated";
    private static final int RATED_ROW = 48;

    /** A Spreadsheet returning a value with no step named RETURN: its last step is the one returned. */
    private static final String GRADED = "Graded";
    private static final int GRADED_ROW = 56;

    /** A Spreadsheet returning the whole table. */
    private static final String PLAIN = "Plain";
    private static final int PLAIN_ROW = 62;

    /** A Spreadsheet returning the value of a column named RETURN. */
    private static final String RATIO = "Ratio";
    private static final int RATIO_ROW = 68;

    /** A Spreadsheet returning the whole table under the full name of its type. */
    private static final String QUALIFIED = "Qualified";
    private static final int QUALIFIED_ROW = 73;

    /** A Spreadsheet returning nothing, with a step named RETURN all the same. */
    private static final String NOTHING = "Nothing";
    private static final int NOTHING_ROW = 78;

    /** A Spreadsheet whose last step is written over two rows of the sheet: its name is merged down over both. */
    private static final String TALL = "Tall";
    private static final int TALL_ROW = 114;
    private static final String VALUE_BACKGROUND = "#ddebf7";

    /** A Data table naming its IDs in {@code _PK_}, with a column taken from another Data table and an empty value. */
    private static final String TEAMS = "teams";
    private static final int TEAMS_ROW = 89;

    /** A Data table written transposed: a field in each row and the values of one row in each column. */
    private static final String CREW = "crew";
    private static final int CREW_ROW = 97;

    /** A Test table filling its input from a Data table by the IDs of its rows. */
    private static final String RATED_TEST = "RatedTest";
    private static final int RATED_TEST_ROW = 102;

    /** A Run table filling its input from a Data table by the IDs of its rows. */
    private static final String RATED_RUN = "RatedRun";
    private static final int RATED_RUN_ROW = 120;

    /** A Method table, which no theme styles. */
    private static final String ANSWER = "Answer";

    /** The fills the default theme gives an ID and a value that is not filled. */
    private static final String ID_BACKGROUND = "#fff2cc";
    private static final String EMPTY_BACKGROUND = "#f2f2f2";
    private static final String MUTED = "#808080";

    /** A Spreadsheet with steps marked for its result and a heading that splits its steps into sections. */
    private static final String PREMIUM = "Premium";
    private static final int PREMIUM_ROW = 40;
    private static final String PREMIUM_HEADER = "Spreadsheet SpreadsheetResult Premium ( Person person )";
    private static final String STEP_TITLE_BACKGROUND = "#d0cece";
    private static final String TITLE_BACKGROUND = "#b4c6e7";

    private final TableThemeService service = new TableThemeService();

    /** A theme that names no font attribute of a cell, so a cell keeps the font it has in the workbook. */
    private static final TableThemeService EXTENDED =
            new TableThemeService("classpath*:test-table-themes/extended-header.yaml");
    private static final String EXTENDED_THEME = "extended-header";

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, TableThemeTest::fillSheet);
    }

    @Test
    void appliesToTheKindsOfTableEveryThemeStyles() {
        var model = TableTestProjects.projectModel(dir);

        assertNotNull(service.layoutOf(TableTestProjects.table(model, "Person"), THEME));
        assertNotNull(service.layoutOf(TableTestProjects.table(model, "Code"), THEME));
        assertNotNull(service.layoutOf(TableTestProjects.table(model, PREMIUM), THEME));
        assertNotNull(service.layoutOf(TableTestProjects.table(model, "people"), THEME));
        assertNotNull(service.layoutOf(TableTestProjects.table(model, RATED_TEST), THEME));
        assertNotNull(service.layoutOf(TableTestProjects.table(model, RATED_RUN), THEME));
        assertNull(service.layoutOf(TableTestProjects.table(model, ANSWER), THEME));
    }

    @Test
    void offersEveryThemeForATableOfAKindThemesStyle() {
        var model = TableTestProjects.projectModel(dir);
        var extension = new TableThemeService("classpath*:test-table-themes/datatype-extension.yaml");

        assertEquals(List.of("default", "green"), ids(service.getThemes(TableTestProjects.table(model, "Code"))));
        assertEquals(List.of("default", "green"), ids(service.getThemes(TableTestProjects.table(model, PREMIUM))));
        // A theme writing nothing for a kind of table other than a Datatype styles them all the same.
        assertEquals(List.of("datatype-extension"), ids(extension.getThemes(TableTestProjects.table(model, "Code"))));
        assertEquals(List.of("datatype-extension"), ids(extension.getThemes(TableTestProjects.table(model, PREMIUM))));
        assertEquals(List.of("default", "green"), ids(service.getThemes(TableTestProjects.table(model, "people"))));
        assertTrue(service.getThemes(TableTestProjects.table(model, ANSWER)).isEmpty(), "No theme styles a Method");
    }

    @Test
    void stylesAKindTheThemeWritesNothingForWithTheBase() {
        var model = TableTestProjects.projectModel(dir);
        var extension = new TableThemeService("classpath*:test-table-themes/datatype-extension.yaml");
        var code = TableTestProjects.table(model, "Code");

        var layout = extension.layoutOf(code, "datatype-extension");
        assertEquals(Boolean.TRUE, layout.at(8, 1).style().italic(), "The Vocabulary takes the base");
        assertEquals(ThemeLineStyle.MEDIUM, layout.at(9, 1).style().border().bottom().style());
        var grid = GridTableUtils.getOriginalTable(code.getGridTable());
        grid.edit();
        try {
            assertTrue(extension.writer("datatype-extension").write(code, grid, TableMoves.NONE));
        } finally {
            grid.stopEditing();
        }
        var people = extension.layoutOf(TableTestProjects.table(model, "people"), "datatype-extension");
        assertEquals(Boolean.TRUE, people.at(14, 1).style().italic(), "A Data table takes the base as well");
        assertNull(extension.layoutOf(TableTestProjects.table(model, ANSWER), "datatype-extension"));
    }

    @Test
    void givesEachPlaceOfADatatypeItsLook() {
        var layout = service.layoutOf(person(), THEME);

        // B2 is the header, merged over B2:D2; every cell of the merge is themed.
        assertEquals(HEADER_BACKGROUND, layout.at(1, 1).style().background());
        assertEquals(HEADER_BACKGROUND, layout.at(1, 3).style().background());
        assertNotNull(layout.at(1, 1).header(), "The cell holding the header text formats it in pieces");
        assertNull(layout.at(1, 3).header());
        // The name column, the values beside it, and the line closing the last row.
        assertEquals(NAME_BACKGROUND, layout.at(2, 2).style().background());
        assertEquals(ThemeHorizontalAlign.CENTER, layout.at(2, 3).style().align());
        assertFalse(lineBelow(layout.at(2, 1)), "A row other than the last is not closed");
        assertEquals(ThemeLineStyle.THIN, layout.at(4, 1).style().border().bottom().style());
        assertEquals("Franklin Gothic Book", layout.at(3, 1).style().fontFamily());
        // A cell outside the table is not reached.
        assertNull(layout.at(1, 5));
    }

    @Test
    void givesTheTitleRowOfADatatypeTheTitleLook() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), "Titled"), THEME);

        assertEquals(Boolean.TRUE, layout.at(20, 1).style().bold());
        assertEquals(WHITE, layout.at(20, 2).style().background(), "The titles are not the field names");
        assertEquals(NAME_BACKGROUND, layout.at(21, 2).style().background());
    }

    @Test
    void givesTheFieldsOfATransposedDatatypeTheLookOfTheirPlace() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), POINT), THEME);

        // Each field runs down a column, so each place runs across the table: the types, the names, the defaults.
        for (var column = 1; column <= 3; column++) {
            assertEquals(WHITE, layout.at(POINT_ROW + 1, column).style().background(), "A type is not a name");
            assertEquals(NAME_BACKGROUND, layout.at(POINT_ROW + 2, column).style().background());
            assertEquals(ThemeHorizontalAlign.CENTER, layout.at(POINT_ROW + 3, column).style().align());
            // The last row closes the table, as it closes an upright one.
            assertFalse(lineBelow(layout.at(POINT_ROW + 2, column)), "Only the last row is closed");
            assertEquals(ThemeLineStyle.THIN, layout.at(POINT_ROW + 3, column).style().border().bottom().style());
        }

        write(tables(TableTestProjects.projectModel(dir), POINT));

        var written = read(POINT);
        assertEquals(NAME_BACKGROUND, written.get(2).get(1).style().background());
        assertNull(written.get(3).get(1).style().background(), "A default is not a name");
    }

    @Test
    void givesTheTitleColumnOfATransposedDatatypeTheTitleLook() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), ITEM), THEME);

        assertEquals(Boolean.TRUE, layout.at(ITEM_ROW + 1, 1).style().bold());
        assertEquals(Boolean.TRUE, layout.at(ITEM_ROW + 2, 1).style().bold());
        assertEquals(WHITE, layout.at(ITEM_ROW + 2, 1).style().background(), "The titles are not the field names");
        assertEquals(Boolean.FALSE, layout.at(ITEM_ROW + 1, 2).style().bold(), "A type is not a title");
        assertEquals(NAME_BACKGROUND, layout.at(ITEM_ROW + 2, 2).style().background());
        assertEquals(NAME_BACKGROUND, layout.at(ITEM_ROW + 2, 3).style().background());
    }

    @Test
    void givesEachPlaceOfASpreadsheetItsLook() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), PREMIUM), THEME);

        // The header is signed as every kind of table is, across the table, its text formatted in pieces. Only a
        // Datatype fills it: a Spreadsheet keeps it white, as any cell the theme fills no other way.
        assertEquals(WHITE, layout.at(PREMIUM_ROW, 1).style().background());
        assertEquals(ThemeLineStyle.THIN, layout.at(PREMIUM_ROW, 2).style().border().bottom().style());
        assertNotNull(layout.at(PREMIUM_ROW, 1).header());
        // The row naming the columns, with the title of the column of steps set apart.
        var stepTitle = layout.at(PREMIUM_ROW + 1, 1).style();
        assertEquals(STEP_TITLE_BACKGROUND, stepTitle.background());
        assertEquals(Boolean.TRUE, stepTitle.bold());
        assertEquals(ThemeHorizontalAlign.CENTER, stepTitle.align());
        assertEquals(TITLE_BACKGROUND, layout.at(PREMIUM_ROW + 1, 2).style().background());
        // A step marked for the result is bold; the others keep what the workbook says.
        assertEquals(Boolean.TRUE, layout.at(PREMIUM_ROW + 2, 1).style().bold());
        assertEquals(Boolean.FALSE, layout.at(PREMIUM_ROW + 4, 1).style().bold(), "A step that is not marked");
        assertEquals(Boolean.TRUE, layout.at(PREMIUM_ROW + 5, 1).style().bold(), "Marked before the type it declares");
        assertEquals(NAME_BACKGROUND, layout.at(PREMIUM_ROW + 2, 2).style().background());
        assertEquals(Boolean.FALSE, layout.at(PREMIUM_ROW + 2, 2).style().bold(),
                "The value of a marked step is not marked");
        // The last row closes the table.
        assertFalse(lineBelow(layout.at(PREMIUM_ROW + 4, 2)));
        assertEquals(ThemeLineStyle.THIN, layout.at(PREMIUM_ROW + 5, 1).style().border().bottom().style());
        assertEquals(ThemeLineStyle.THIN, layout.at(PREMIUM_ROW + 5, 2).style().border().bottom().style());
    }

    @Test
    void givesAStepMergedAcrossItsRowTheSectionLook() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), PREMIUM), THEME);

        // The heading is merged over the values of its row: every cell of it is the section, none a value.
        for (var column = 1; column <= 2; column++) {
            var section = layout.at(PREMIUM_ROW + 3, column).style();
            assertEquals(TITLE_BACKGROUND, section.background());
            assertEquals(Boolean.TRUE, section.bold());
            assertEquals(Boolean.TRUE, section.italic());
            assertEquals(ThemeHorizontalAlign.CENTER, section.align());
            assertEquals(ThemeLineStyle.THIN, section.border().top().style());
        }
        assertEquals(WHITE, layout.at(PREMIUM_ROW + 4, 1).style().background(), "A step under the heading is a step");
    }

    @Test
    void writesTheLookOfASpreadsheetIntoTheWorkbook() {
        write(tables(TableTestProjects.projectModel(dir), PREMIUM));

        var written = read(PREMIUM);
        assertEquals(List.of("Spreadsheet", " ", "SpreadsheetResult", " ", PREMIUM, " ", "( Person person )"),
                texts(written.getFirst().getFirst().runs()));
        assertEquals(Boolean.TRUE, written.getFirst().getFirst().runs().get(4).style().bold());
        assertEquals("#808080", written.getFirst().getFirst().runs().get(6).style().color());
        assertEquals(STEP_TITLE_BACKGROUND, written.get(1).getFirst().style().background());
        assertEquals(Boolean.TRUE, written.get(2).getFirst().style().bold());
        assertEquals(Boolean.TRUE, written.get(3).getFirst().style().italic());
        assertEquals(TITLE_BACKGROUND, written.get(3).getFirst().style().background());
        var step = written.get(4).getFirst().style();
        assertTrue(step == null || step.bold() == null, "A step that is not marked is not bold");
        assertEquals(NAME_BACKGROUND, written.get(4).get(1).style().background());
    }

    @Test
    void takesAwayTheLinesTheWorkbookDrawsInsideTheTable() {
        assertNotNull(bottomOf(read(PREMIUM).get(4).getFirst().style()), "The workbook draws a line under the step");

        var table = premium();
        var themed = readThemed(table, service.layoutOf(table, THEME)).source;
        assertNull(bottomOf(themed.get(4).getFirst().style()), "The theme draws no line there");

        write(tables(TableTestProjects.projectModel(dir), PREMIUM));
        assertNull(bottomOf(read(PREMIUM).get(4).getFirst().style()), "Nor does the workbook it is written into");
    }

    @Test
    void fillsWhiteACellTheThemeFillsNoOtherWay() {
        assertEquals(GREY, read("Code").get(1).getFirst().style().background(), "The workbook fills the values grey");

        var code = TableTestProjects.table(TableTestProjects.projectModel(dir), "Code");
        var themed = readThemed(code, service.layoutOf(code, THEME)).source;
        // White is reported as no fill, as the style of a cell reports it.
        assertNull(themed.get(1).getFirst().style().background(), "The theme draws them white");

        write(tables(TableTestProjects.projectModel(dir), "Code"));
        assertNull(read("Code").get(1).getFirst().style().background(), "And writes them white");
    }

    @Test
    void drawsTheLineOverASectionByTheCellsAboveIt() {
        var table = premium();
        var themed = readThemed(table, service.layoutOf(table, THEME)).source;

        // The screen draws the side of the upper cell over the side of the lower one, as it draws a workbook: the
        // line over the heading is drawn by the step above it, all across the heading.
        for (var column = 0; column < 2; column++) {
            var line = bottomOf(themed.get(2).get(column).style());
            assertEquals(RawTableBorderLineStyle.SOLID, line.style());
        }
        assertNull(topOf(themed.get(3).getFirst().style()), "The heading draws no line of its own");
        // The header keeps its top: no cell of the table is above it.
        assertNotNull(topOf(themed.getFirst().getFirst().style()));
    }

    @Test
    void drawsTheLineOverASectionUnderTheRowsReadByTheLastOfThem() {
        var table = premium();
        var layout = service.layoutOf(table, THEME);

        // A window ending right above the heading: its last step draws the line over the heading.
        var window = new RawTableReader().read(table, RawTableRead.builder()
                .startRow(0)
                .maxRows(3)
                .withStyles(true)
                .theme(layout)
                .build()).source;
        for (var column = 0; column < 2; column++) {
            var line = bottomOf(window.get(2).get(column).style());
            assertEquals(RawTableBorderLineStyle.SOLID, line.style());
        }
        // The next window starts with the heading and draws the line itself: no row of it is above the heading.
        var next = new RawTableReader().read(table, RawTableRead.builder()
                .startRow(3)
                .maxRows(3)
                .withStyles(true)
                .theme(layout)
                .build()).source;
        assertNotNull(topOf(next.getFirst().getFirst().style()));
    }

    @Test
    void closesThePropertiesOfATableWithOneLine() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), RATED), THEME);

        // The properties take two rows of the sheet and are one section of the table: one line closes them.
        assertFalse(lineBelow(layout.at(RATED_ROW + 1, 2)), "No line between two properties");
        assertTrue(lineBelow(layout.at(RATED_ROW + 2, 2)));
        assertTrue(lineBelow(layout.at(RATED_ROW + 2, 3)));
        assertTrue(lineBelow(layout.at(RATED_ROW + 1, 1)), "The keyword merged over both properties reaches the line");
        assertEquals("Franklin Gothic Book", layout.at(RATED_ROW + 1, 3).style().fontFamily(),
                "The properties start from the look of the whole table");
    }

    @Test
    void givesTheStepASpreadsheetReturnsTheResultLook() {
        var model = TableTestProjects.projectModel(dir);
        var rated = service.layoutOf(TableTestProjects.table(model, RATED), THEME);
        var graded = service.layoutOf(TableTestProjects.table(model, GRADED), THEME);
        var plain = service.layoutOf(TableTestProjects.table(model, PLAIN), THEME);
        var ratio = service.layoutOf(TableTestProjects.table(model, RATIO), THEME);
        var qualified = service.layoutOf(TableTestProjects.table(model, QUALIFIED), THEME);
        var nothing = service.layoutOf(TableTestProjects.table(model, NOTHING), THEME);

        assertEquals(Boolean.TRUE, rated.at(RATED_ROW + 4, 1).style().bold(), "The step named RETURN is returned");
        assertEquals(Boolean.FALSE, rated.at(RATED_ROW + 5, 1).style().bold(),
                "The last step is not, where a step is named RETURN");
        assertEquals(Boolean.TRUE, graded.at(GRADED_ROW + 3, 1).style().bold(), "With none named so, the last one is");
        assertEquals(Boolean.FALSE, graded.at(GRADED_ROW + 2, 1).style().bold());
        assertEquals(Boolean.FALSE, rated.at(RATED_ROW + 4, 2).style().bold(), "The value of the step is not the step");
        assertEquals(Boolean.FALSE, plain.at(PLAIN_ROW + 3, 1).style().bold(),
                "A Spreadsheet returning the whole table returns no step");
        assertEquals(Boolean.FALSE, ratio.at(RATIO_ROW + 2, 1).style().bold(),
                "A column named RETURN is returned in place of a step");
        assertEquals(Boolean.FALSE, qualified.at(QUALIFIED_ROW + 2, 1).style().bold(),
                "The full name returns the table");
        assertEquals(Boolean.FALSE, nothing.at(NOTHING_ROW + 2, 1).style().bold(),
                "A void Spreadsheet returns no step");
    }

    @Test
    void splitsTheHeaderOfASpreadsheetIntoKeywordTypeNameAndParameters() {
        var theme = service.theme(THEME);
        var header = theme.lookOf(theme.spreadsheet()).header();
        var cell = ThemeStyle.NONE.with(header.style());

        assertEquals(List.of("Spreadsheet", " ", "SpreadsheetResult", " ", PREMIUM, " ", "( Person person )"),
                pieces(PREMIUM_HEADER, cell, header));
        assertEquals(List.of("Spreadsheet", " ", "SpreadsheetResult", " ", "Plan", "( Plan plan )"),
                pieces("Spreadsheet SpreadsheetResult Plan( Plan plan )", cell, header));
        assertEquals(List.of("Spreadsheet", "  ", "Gender[]", " ", "AllGenders", "()"),
                pieces("Spreadsheet  Gender[] AllGenders()", cell, header));
        // A header may leave out the type it returns, or name no parameters.
        assertEquals(List.of("Spreadsheet", " ", PREMIUM, "()"), pieces("Spreadsheet Premium()", cell, header));
        assertEquals(List.of("Calc", " ", "Double", " ", "Rate"), pieces("Calc Double Rate", cell, header));
        assertEquals("SpreadsheetResult", HeaderRuns.returnType(PREMIUM_HEADER));
        assertEquals("", HeaderRuns.returnType("Spreadsheet Premium()"));
        assertEquals("org.openl.rules.calc.SpreadsheetResult",
                HeaderRuns.returnType("Spreadsheet org.openl.rules.calc.SpreadsheetResult Premium()"));

        var runs = HeaderRuns.split(PREMIUM_HEADER, cell, header);
        assertEquals("#808080", runs.get(0).style().color());
        assertEquals("#808080", runs.get(2).style().color());
        assertEquals(Boolean.TRUE, runs.get(4).style().bold());
        assertEquals("#808080", runs.get(6).style().color());
    }

    @Test
    void splitsTheHeaderIntoKeywordNameAndType() {
        var theme = service.theme(THEME);
        var header = theme.lookOf(theme.datatype()).header();
        var cell = ThemeStyle.NONE.with(header.style());

        assertEquals(List.of("Datatype", " ", "Code", "<String>"), pieces("Datatype Code<String>", cell, header));
        assertEquals(List.of("Datatype", " ", "Child", " ", "extends Parent"),
                pieces("Datatype Child extends Parent", cell, header));
        assertEquals(List.of(), pieces("   ", cell, header));

        var runs = HeaderRuns.split("Datatype Code <String>", cell, header);
        assertEquals(Boolean.TRUE, runs.get(2).style().bold());
        assertEquals("#808080", runs.get(0).style().color());
        assertEquals("#808080", runs.get(4).style().color());
    }

    @Test
    void drawsTheThemeInTheStyleOfEachCellAReadNamesItFor() {
        var table = person();
        var plain = new RawTableReader().read(table, RawTableRead.builder().withStyles(true).build());
        var themed = readThemed(table, service.layoutOf(table, THEME));

        var header = themed.source.getFirst().getFirst();
        assertEquals(RawTableStyleSource.THEME, header.style().source(), "The style is the look of the theme");
        assertNull(Optional.ofNullable(plain.source.get(1).getFirst().style())
                .map(RawTableCellStyle::source)
                .orElse(null), "A style of the workbook names no source");
        assertEquals(HEADER_BACKGROUND, header.style().background());
        assertEquals(RawTableHorizontalAlign.CENTER, header.style().align());
        assertEquals("Franklin Gothic Book", header.style().fontFamily());
        assertEquals(List.of("Datatype", " ", "Person"), texts(header.runs()));
        assertEquals(Boolean.TRUE, header.runs().get(2).style().bold());
        assertEquals(RawTableStyleSource.THEME, header.runs().get(2).style().source(), "So does a piece of its text");
        assertEquals(NAME_BACKGROUND, themed.source.get(1).get(1).style().background());
        assertNotEquals(NAME_BACKGROUND, Optional.ofNullable(plain.source.get(1).get(1).style())
                .map(RawTableCellStyle::background)
                .orElse(null), "No theme is read unless asked for");
    }

    @Test
    void reachesAcrossAHeaderThatIsNotMergedOverTheTable() {
        var table = account();
        var themed = readThemed(table, service.layoutOf(table, THEME));
        // The header holds its text in its first cell only; the cells beside it are the header row all the same.
        assertEquals(HEADER_BACKGROUND, themed.source.getFirst().get(2).style().background());

        write(tables(TableTestProjects.projectModel(dir), ACCOUNT));

        assertEquals(HEADER_BACKGROUND, read(ACCOUNT).getFirst().get(2).style().background());
    }

    @Test
    void startsEachPieceOfTheHeaderFromTheFontOfTheCell() {
        var table = account();
        var themed = readThemed(table, EXTENDED.layoutOf(table, EXTENDED_THEME));
        // The header is bold in the workbook, and the theme says nothing of bold for the keyword: it stays bold, on
        // the screen as in the workbook the theme is written into.
        var drawn = themed.source.getFirst().getFirst().runs().getFirst().style();
        assertEquals(Boolean.TRUE, drawn.bold());
        assertEquals("#548235", drawn.color());

        EXTENDED.writer(EXTENDED_THEME).writeAll(tables(TableTestProjects.projectModel(dir), ACCOUNT), Map.of());

        var written = read(ACCOUNT).getFirst().getFirst().runs().getFirst().style();
        assertEquals(drawn.bold(), written.bold());
        assertEquals(drawn.color(), written.color());
    }

    @Test
    void writesAnyOtherTextInTheFontOfItsCell() throws IOException {
        // A field name formatted in pieces of its own, as an author marks a part of it.
        try (var workbook = workbook()) {
            var cell = workbook.getSheet(SHEET).getRow(2).getCell(2);
            var red = workbook.createFont();
            red.setColor(IndexedColors.RED.getIndex());
            var marked = workbook.getCreationHelper().createRichTextString("name");
            marked.applyFont(0, 2, red);
            cell.setCellValue(marked);
            try (var out = Files.newOutputStream(dir.resolve(SHEET + ".xlsx"))) {
                workbook.write(out);
            }
        }
        assertNotNull(read("Person").get(1).get(1).runs(), "The workbook formats the name in pieces");

        // A theme naming nothing of the font of the cell keeps the pieces, on the screen as in the workbook.
        var person = TableTestProjects.table(TableTestProjects.projectModel(dir), "Person");
        var kept = readThemed(person, EXTENDED.layoutOf(person, EXTENDED_THEME)).source.get(1).get(1);
        assertEquals(List.of("na", "me"), texts(kept.runs()));
        var alone = new RawTableReader().read(person, RawTableRead.builder()
                .theme(EXTENDED.layoutOf(person, EXTENDED_THEME))
                .build()).source.get(1).get(1);
        assertEquals(List.of("na", "me"), texts(alone.runs()), "Kept by a read of the theme alone");
        var drawn = readThemed(person, service.layoutOf(person, THEME)).source.get(1).get(1);
        assertNull(drawn.runs(), "A theme naming the font draws the text in it");
        EXTENDED.writer(EXTENDED_THEME).writeAll(tables(TableTestProjects.projectModel(dir), "Person"), Map.of());
        assertNotNull(read("Person").get(1).get(1).runs());

        write(tables(TableTestProjects.projectModel(dir), "Person"));

        var name = read("Person").get(1).get(1);
        assertEquals("name", name.value(), "The text stays as it is");
        assertNull(name.runs(), "The theme writes it in the font of its cell");
    }

    @Test
    void keepsTheSizeOfAFontOfHalfAPointTheThemeSaysNothingOf() throws IOException {
        // The theme names no size, so the header keeps the 10.5 points it is written in.
        EXTENDED.writer(EXTENDED_THEME).writeAll(tables(TableTestProjects.projectModel(dir), ACCOUNT), Map.of());

        try (var workbook = workbook()) {
            var header = cell(workbook.getSheet(SHEET), ACCOUNT_ROW, 1);
            assertEquals(HALF_POINT_HEIGHT, header.getCellStyle().getFont().getFontHeight());
        }
    }

    @Test
    void writesTheThemeIntoTheWorkbook() throws IOException {
        write(tables(TableTestProjects.projectModel(dir), "Person", "Code"));

        var person = TableTestProjects.table(TableTestProjects.projectModel(dir), "Person");
        var source = new RawTableReader().read(person, RawTableRead.builder().withStyles(true).build()).source;
        var header = source.getFirst().getFirst();
        assertEquals(HEADER_BACKGROUND, header.style().background());
        assertEquals(RawTableHorizontalAlign.CENTER, header.style().align());
        assertEquals("Datatype Person", header.value(), "The header keeps its text");
        assertEquals(List.of("Datatype", " ", "Person"), texts(header.runs()));
        assertEquals(Boolean.TRUE, header.runs().get(2).style().bold());
        assertEquals("#808080", header.runs().getFirst().style().color());
        assertEquals(NAME_BACKGROUND, source.get(1).get(1).style().background());
        assertNotNull(source.get(3).get(0).style().border().bottom(), "The last row is closed");

        try (var workbook = workbook()) {
            var sheet = workbook.getSheet(SHEET);
            assertEquals(PERCENT, cell(sheet, 3, 3).getCellStyle().getDataFormatString(),
                    "A cell keeps its number format");
            assertEquals(0.5, cell(sheet, 3, 3).getNumericCellValue());
            assertEquals("Franklin Gothic Book", cell(sheet, 2, 1).getCellStyle().getFont().getFontName());
            var foreign = cell(sheet, 1, 5).getCellStyle();
            assertEquals(IndexedColors.RED.getIndex(), foreign.getFillForegroundColor(),
                    "A cell outside the table is left as it is");
            assertEquals(BorderStyle.NONE, foreign.getBorderBottom());
        }
    }

    @Test
    void formatsAHeaderWrittenIntoTheCellItself(@TempDir Path inline) throws IOException {
        // A streaming workbook writes each text into its cell rather than among the shared strings, as tools other
        // than Excel do.
        try (var workbook = new SXSSFWorkbook(); var out = Files.newOutputStream(inline.resolve("Inline.xlsx"))) {
            var sheet = workbook.createSheet("Inline");
            sheet.createRow(1).createCell(1).setCellValue("Datatype Person");
            var field = sheet.createRow(2);
            field.createCell(1).setCellValue("String");
            field.createCell(2).setCellValue("name");
            workbook.write(out);
        }

        write(List.of(TableTestProjects.table(TableTestProjects.projectModel(inline), "Person")));

        var header = new RawTableReader().read(TableTestProjects.table(TableTestProjects.projectModel(inline),
                "Person"), RawTableRead.builder().withStyles(true).build()).source.getFirst().getFirst();
        assertEquals("Datatype Person", header.value());
        assertEquals(List.of("Datatype", " ", "Person"), texts(header.runs()));
    }

    @Test
    void writingTheThemeAgainAddsNoFonts() throws IOException {
        write(tables(TableTestProjects.projectModel(dir), "Person", "Code"));
        int fonts;
        int styles;
        try (var workbook = workbook()) {
            fonts = workbook.getNumberOfFonts();
            styles = workbook.getNumCellStyles();
        }

        write(tables(TableTestProjects.projectModel(dir), "Person", "Code"));

        try (var workbook = workbook()) {
            assertEquals(fonts, workbook.getNumberOfFonts());
            assertEquals(styles, workbook.getNumCellStyles());
        }
    }

    @Test
    void refusesAThemeAnXlsWorkbookHasNoRoomForTheStylesOf(@TempDir Path xls) throws IOException {
        // The workbook holds as many cell styles as an .xls file can.
        writeXlsPerson(xls, XLS_STYLES);
        var person = List.of(TableTestProjects.table(TableTestProjects.projectModel(xls), "Person"));

        var refused = assertThrows(BadRequestException.class, () -> write(person));

        assertEquals("openl.error.400.table.theme.styles.full.message", refused.getErrorCode());
        try (var workbook = xlsWorkbook(xls)) {
            assertEquals(XLS_STYLES, workbook.getNumCellStyles(), "Nothing of the theme is written");
        }
    }

    @Test
    void tellsAnXlsxWorkbookWithoutRoomForTheStylesNoFormatToBeSavedAs() throws IOException {
        // An .xlsx file holds 64,000 cell styles, too many for a test to write: the message alone is checked.
        try (var xlsx = new XSSFWorkbook(); var xls = new HSSFWorkbook()) {
            assertEquals("table.theme.styles.full.xlsx.message", ThemeExcelWriter.stylesFullMessage(xlsx));
            assertEquals("table.theme.styles.full.message", ThemeExcelWriter.stylesFullMessage(xls));
        }
    }

    @Test
    void writingTheThemeAgainIntoAnXlsWorkbookAddsNothing(@TempDir Path xls) throws IOException {
        // The palette of an .xls workbook has no room for another colour, so a colour of the theme is written as the
        // nearest one it holds. That colour is the look the next write finds.
        writeXlsPerson(xls, 0);
        write(List.of(TableTestProjects.table(TableTestProjects.projectModel(xls), "Person")));
        int fonts;
        int styles;
        try (var workbook = xlsWorkbook(xls)) {
            fonts = workbook.getNumberOfFonts();
            styles = workbook.getNumCellStyles();
        }

        write(List.of(TableTestProjects.table(TableTestProjects.projectModel(xls), "Person")));

        try (var workbook = xlsWorkbook(xls)) {
            assertEquals(fonts, workbook.getNumberOfFonts());
            assertEquals(styles, workbook.getNumCellStyles());
        }
    }

    @Test
    void notesTheEditOnEveryTableItThemes() {
        var written = service.writer(THEME).writeAll(List.of(person()), Map.of("modifiedBy", "admin"));

        // The note is written onto the table as any edit of it writes it.
        var source = read("Person");
        assertEquals("properties", source.get(1).getFirst().value());
        assertEquals("modifiedBy", source.get(1).get(1).value());
        assertEquals("admin", source.get(1).get(2).value());
        assertEquals(List.of(person().getSyntaxNode().getId()), written, "Named by where the table stands");
    }

    @Test
    void themesThePropertiesTheNoteLaysDown() {
        service.writer(THEME).writeAll(List.of(person()), Map.of("modifiedBy", "admin"));

        // The table had no properties: the note lays them down after the theme, and they take the theme too.
        var source = read("Person");
        source.get(1).forEach(cell -> assertNotNull(bottomOf(cell.style()), "The properties are closed by a line"));
        assertNull(source.get(1).get(1).style().background(), "A property takes no look of the field under it");
        assertEquals(NAME_BACKGROUND, source.get(2).get(1).style().background(), "The fields keep their look");
    }

    @Test
    void writesNoCellPastTheEdgeOfTheTableAnEmptyMergeReaches() throws IOException {
        // A region of empty cells does not widen the table, so it may be merged past the edge of the table.
        var wide = dir.resolve("wide");
        TableTestProjects.projectModel(wide, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Wide", null, null);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 3));
            TableTestProjects.row(sheet, 2, 1, "String", "name", "Bob");
            TableTestProjects.row(sheet, 3, 1, "Integer", "age");
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 5));
        });

        service.writer(THEME).writeAll(List.of(TableTestProjects.table(TableTestProjects.projectModel(wide), "Wide")),
                Map.of());

        try (InputStream in = Files.newInputStream(wide.resolve(SHEET + ".xlsx"));
             var workbook = new XSSFWorkbook(in)) {
            var row = workbook.getSheet(SHEET).getRow(3);
            assertNotNull(row.getCell(3), "The cell holding the merge stands in the table");
            assertNull(row.getCell(4), "A cell past the edge of the table");
            assertNull(row.getCell(5));
        }
    }

    @Test
    void writesNothingIntoATableTheThemeDoesNotApplyTo() throws IOException {
        var before = Files.readAllBytes(dir.resolve(SHEET + ".xlsx"));

        write(List.of(TableTestProjects.table(TableTestProjects.projectModel(dir), ANSWER)));

        assertArrayEquals(before, Files.readAllBytes(dir.resolve(SHEET + ".xlsx")),
                "No workbook is saved when no table is themed");
    }

    @Test
    void themesEveryRowOfTheSheetAStepTakes() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), TALL), THEME);

        // The step takes two rows of the sheet: its name is merged down over both, its value is written in each.
        assertEquals(VALUE_BACKGROUND, layout.at(TALL_ROW + 2, 2).style().background());
        assertEquals(VALUE_BACKGROUND, layout.at(TALL_ROW + 3, 2).style().background());
        // Only the row of the sheet that ends the table is closed.
        assertFalse(lineBelow(layout.at(TALL_ROW + 2, 2)));
        assertTrue(lineBelow(layout.at(TALL_ROW + 3, 2)));
        assertTrue(lineBelow(layout.at(TALL_ROW + 3, 1)), "The name merged down to the end of the table");
    }

    @Test
    void givesEachPlaceOfADataTableItsLook() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), TEAMS), THEME);

        // The header of a Data table reaches across its values, so its text starts at the left.
        assertEquals(ThemeHorizontalAlign.LEFT, layout.at(TEAMS_ROW, 1).style().align());
        // The field names and the table a field takes its values from are muted, the titles filled.
        assertEquals(MUTED, layout.at(TEAMS_ROW + 1, 2).style().color());
        assertEquals(MUTED, layout.at(TEAMS_ROW + 2, 3).style().color());
        assertEquals(NAME_BACKGROUND, layout.at(TEAMS_ROW + 3, 2).style().background());
        assertEquals(Boolean.TRUE, layout.at(TEAMS_ROW + 3, 2).style().bold());
        // The IDs of the rows are named by _PK_; the values of the other columns are not IDs.
        assertEquals(ID_BACKGROUND, layout.at(TEAMS_ROW + 4, 1).style().background());
        assertEquals(Boolean.TRUE, layout.at(TEAMS_ROW + 4, 1).style().bold());
        assertEquals(WHITE, layout.at(TEAMS_ROW + 4, 2).style().background());
        assertEquals(ThemeHorizontalAlign.CENTER, layout.at(TEAMS_ROW + 4, 2).style().align());
        assertEquals(WHITE, layout.at(TEAMS_ROW + 4, 3).style().background(), "A reference is not an ID of the table");
        // A value that is not filled, and the line closing the last row.
        assertEquals(EMPTY_BACKGROUND, layout.at(TEAMS_ROW + 5, 2).style().background());
        assertFalse(lineBelow(layout.at(TEAMS_ROW + 4, 1)), "A row other than the last is not closed");
        assertTrue(lineBelow(layout.at(TEAMS_ROW + 5, 3)));
    }

    @Test
    void givesTheFieldsOfATransposedDataTableTheLookOfTheirPlace() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), CREW), THEME);

        // Each field runs across a row: its name, its title, then a value of each row of the table.
        assertEquals(MUTED, layout.at(CREW_ROW + 2, 1).style().color());
        assertEquals(NAME_BACKGROUND, layout.at(CREW_ROW + 2, 2).style().background());
        // The first field names the rows of the table.
        assertEquals(ID_BACKGROUND, layout.at(CREW_ROW + 1, 3).style().background());
        assertEquals(ID_BACKGROUND, layout.at(CREW_ROW + 1, 4).style().background());
        assertEquals(WHITE, layout.at(CREW_ROW + 2, 3).style().background());
        assertEquals(EMPTY_BACKGROUND, layout.at(CREW_ROW + 2, 4).style().background());
        // The last row as written closes the table.
        assertFalse(lineBelow(layout.at(CREW_ROW + 1, 3)));
        assertTrue(lineBelow(layout.at(CREW_ROW + 2, 3)));
    }

    @Test
    void givesTheValuesATestTakesFromADataTableTheIdLook() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), RATED_TEST), THEME);

        assertEquals(MUTED, layout.at(RATED_TEST_ROW + 2, 1).style().color(), "The row naming the Data table");
        assertEquals(NAME_BACKGROUND, layout.at(RATED_TEST_ROW + 3, 1).style().background());
        // The person is filled from the Data table by its ID; the expected result is not.
        assertEquals(ID_BACKGROUND, layout.at(RATED_TEST_ROW + 4, 1).style().background());
        assertEquals(WHITE, layout.at(RATED_TEST_ROW + 4, 2).style().background());
    }

    @Test
    void givesARunTableTheLookOfATestTable() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), RATED_RUN), THEME);

        assertEquals(MUTED, layout.at(RATED_RUN_ROW + 1, 1).style().color(), "The row naming the field");
        assertEquals(MUTED, layout.at(RATED_RUN_ROW + 2, 1).style().color(), "The row naming the Data table");
        assertEquals(NAME_BACKGROUND, layout.at(RATED_RUN_ROW + 3, 1).style().background());
        // The person is filled from the Data table by its ID, and its row closes the table.
        assertEquals(ID_BACKGROUND, layout.at(RATED_RUN_ROW + 4, 1).style().background());
        assertTrue(lineBelow(layout.at(RATED_RUN_ROW + 4, 1)));

        // The header names the method the table runs, then the name of the table.
        var theme = service.theme(THEME);
        var header = theme.lookOf(theme.run()).header();
        assertEquals(List.of("Run", " ", RATED, " ", RATED_RUN),
                pieces("Run " + RATED + " " + RATED_RUN, ThemeStyle.NONE.with(header.style()), header));
    }

    @Test
    void writesTheThemeIntoADataTable() {
        write(tables(TableTestProjects.projectModel(dir), TEAMS));

        var written = read(TEAMS);
        assertEquals(ID_BACKGROUND, written.get(4).getFirst().style().background());
        assertEquals(EMPTY_BACKGROUND, written.get(5).get(1).style().background());
        assertEquals(MUTED, written.get(1).get(1).style().color());
    }

    /** The table read with the styles of its cells and the look a theme gives each of them. */
    private static RawTableView readThemed(IOpenLTable table, ThemedTable theme) {
        return new RawTableReader().read(table, RawTableRead.builder().withStyles(true).theme(theme).build());
    }

    private IOpenLTable person() {
        return TableTestProjects.table(TableTestProjects.projectModel(dir), "Person");
    }

    private IOpenLTable premium() {
        return TableTestProjects.table(TableTestProjects.projectModel(dir), PREMIUM);
    }

    private IOpenLTable account() {
        return TableTestProjects.table(TableTestProjects.projectModel(dir), ACCOUNT);
    }

    /** The cells of a table as the workbook now holds them, with their styles. */
    private List<List<RawTableCell>> read(String name) {
        var table = TableTestProjects.table(TableTestProjects.projectModel(dir), name);
        return new RawTableReader().read(table, RawTableRead.builder().withStyles(true).build()).source;
    }

    private List<String> write(List<IOpenLTable> tables) {
        return write(tables, THEME);
    }

    /** Writes a theme into tables and saves their workbooks, the way a project is themed where no edit is noted. */
    private List<String> write(List<IOpenLTable> tables, String theme) {
        return service.writer(theme).writeAll(tables, Map.of());
    }

    /** Tables of one model: tables read through two models would each save a workbook of their own. */
    private static List<IOpenLTable> tables(ProjectModel model, String... names) {
        return Arrays.stream(names).map(name -> TableTestProjects.table(model, name)).toList();
    }

    private XSSFWorkbook workbook() throws IOException {
        try (InputStream in = Files.newInputStream(dir.resolve(SHEET + ".xlsx"))) {
            return new XSSFWorkbook(in);
        }
    }

    /** Writes a project of one .xls workbook holding the Datatype Person, with at least so many cell styles. */
    private static void writeXlsPerson(Path project, int styles) throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var sheet = workbook.createSheet(SHEET);
            TableTestProjects.row(sheet, 1, 1, "Datatype Person");
            TableTestProjects.row(sheet, 2, 1, "String", "name");
            while (workbook.getNumCellStyles() < styles) {
                workbook.createCellStyle();
            }
            try (OutputStream out = Files.newOutputStream(project.resolve(SHEET + ".xls"))) {
                workbook.write(out);
            }
        }
    }

    private static HSSFWorkbook xlsWorkbook(Path project) throws IOException {
        try (InputStream in = Files.newInputStream(project.resolve(SHEET + ".xls"))) {
            return new HSSFWorkbook(in);
        }
    }

    private static XSSFCell cell(Sheet sheet, int row, int column) {
        return (XSSFCell) sheet.getRow(row).getCell(column);
    }

    /** Whether the theme draws a line under a cell. */
    private static boolean lineBelow(ThemedTable.ThemedCell cell) {
        var border = cell.style().border();
        return border != null && border.bottom() != null && border.bottom().isLine();
    }

    private static @Nullable RawTableCellBorderSide bottomOf(@Nullable RawTableCellStyle style) {
        return style == null || style.border() == null ? null : style.border().bottom();
    }

    private static @Nullable RawTableCellBorderSide topOf(@Nullable RawTableCellStyle style) {
        return style == null || style.border() == null ? null : style.border().top();
    }

    private static List<String> pieces(String text, ThemeStyle cell, TableTheme.Header header) {
        return HeaderRuns.split(text, cell, header).stream()
                .map(run -> text.substring(run.start(), run.end()))
                .toList();
    }

    private static List<String> ids(List<TableThemeView> themes) {
        return themes.stream().map(TableThemeView::id).toList();
    }

    private static List<String> texts(List<RawTableTextRun> runs) {
        return runs.stream().map(RawTableTextRun::text).toList();
    }

    /**
     * A Datatype with a percentage, a Vocabulary, a Datatype naming its columns, a Data table, a cell outside
     * every table filled red, a Datatype whose header is not merged over it, two Datatypes written transposed, seven
     * Spreadsheets, two more Data tables, one of them transposed, a Test table and a Method table.
     */
    private static void fillSheet(Sheet sheet) {
        var workbook = sheet.getWorkbook();
        TableTestProjects.row(sheet, 1, 1, "Datatype Person", null, null);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 3));
        var foreign = workbook.createCellStyle();
        foreign.setFillForegroundColor(IndexedColors.RED.getIndex());
        foreign.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        var note = sheet.getRow(1).createCell(5);
        note.setCellValue("note");
        note.setCellStyle(foreign);
        TableTestProjects.row(sheet, 2, 1, "String", "name", "Bob");
        TableTestProjects.row(sheet, 3, 1, "Double", "rate");
        var percent = workbook.createCellStyle();
        percent.setDataFormat(workbook.createDataFormat().getFormat(PERCENT));
        var rate = sheet.getRow(3).createCell(3);
        rate.setCellValue(0.5);
        rate.setCellStyle(percent);
        TableTestProjects.row(sheet, 4, 1, "Integer", "age");

        TableTestProjects.row(sheet, 7, 1, "Datatype Code <String>");
        TableTestProjects.row(sheet, 8, 1, "A");
        TableTestProjects.row(sheet, 9, 1, "B");
        var grey = workbook.createCellStyle();
        grey.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        grey.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sheet.getRow(8).getCell(1).setCellStyle(grey);
        sheet.getRow(9).getCell(1).setCellStyle(grey);

        TableTestProjects.row(sheet, 12, 1, "Data Person people");
        TableTestProjects.row(sheet, 13, 1, "name");
        TableTestProjects.row(sheet, 14, 1, "Name");
        TableTestProjects.row(sheet, 15, 1, "Ann");

        TableTestProjects.row(sheet, 19, 1, "Datatype Titled");
        TableTestProjects.row(sheet, 20, 1, "Type", "Name", "Description");
        TableTestProjects.row(sheet, 21, 1, "String", "code", "The code");

        var headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeight(HALF_POINT_HEIGHT);
        var header = workbook.createCellStyle();
        header.setFont(headerFont);
        TableTestProjects.row(sheet, ACCOUNT_ROW, 1, "Datatype " + ACCOUNT);
        sheet.getRow(ACCOUNT_ROW).getCell(1).setCellStyle(header);
        TableTestProjects.row(sheet, ACCOUNT_ROW + 1, 1, "String", "id", "A-1");
        TableTestProjects.row(sheet, ACCOUNT_ROW + 2, 1, "Integer", "balance", "0");

        TableTestProjects.row(sheet, POINT_ROW, 1, "Datatype " + POINT);
        TableTestProjects.row(sheet, POINT_ROW + 1, 1, "Integer", "Integer", "String");
        TableTestProjects.row(sheet, POINT_ROW + 2, 1, "x", "y", "label");
        TableTestProjects.row(sheet, POINT_ROW + 3, 1, "1", "2", "origin");

        TableTestProjects.row(sheet, ITEM_ROW, 1, "Datatype " + ITEM);
        TableTestProjects.row(sheet, ITEM_ROW + 1, 1, "Type", "String", "Integer");
        TableTestProjects.row(sheet, ITEM_ROW + 2, 1, "Name", "code", "count");

        TableTestProjects.row(sheet, PREMIUM_ROW, 1, PREMIUM_HEADER);
        sheet.addMergedRegion(new CellRangeAddress(PREMIUM_ROW, PREMIUM_ROW, 1, 2));
        TableTestProjects.row(sheet, PREMIUM_ROW + 1, 1, "Step", "Formula");
        TableTestProjects.row(sheet, PREMIUM_ROW + 2, 1, "Base*", "= 10");
        TableTestProjects.row(sheet, PREMIUM_ROW + 3, 1, "Rates and Totals");
        sheet.addMergedRegion(new CellRangeAddress(PREMIUM_ROW + 3, PREMIUM_ROW + 3, 1, 2));
        TableTestProjects.row(sheet, PREMIUM_ROW + 4, 1, "Rate : Double", "= $Base * 2");
        TableTestProjects.row(sheet, PREMIUM_ROW + 5, 1, "Total* : Double", "= $Rate");
        // A line the author draws between two steps, inside the table.
        var groupLine = workbook.createCellStyle();
        groupLine.setBorderBottom(BorderStyle.THIN);
        sheet.getRow(PREMIUM_ROW + 4).getCell(1).setCellStyle(groupLine);
        sheet.getRow(PREMIUM_ROW + 4).getCell(2).setCellStyle(groupLine);

        TableTestProjects.row(sheet, RATED_ROW, 1, "Spreadsheet Double " + RATED + " ( Person person )");
        sheet.addMergedRegion(new CellRangeAddress(RATED_ROW, RATED_ROW, 1, 3));
        TableTestProjects.row(sheet, RATED_ROW + 1, 1, "properties", "description", "A rated premium");
        TableTestProjects.row(sheet, RATED_ROW + 2, 1, null, "category", "Rating");
        sheet.addMergedRegion(new CellRangeAddress(RATED_ROW + 1, RATED_ROW + 2, 1, 1));
        TableTestProjects.row(sheet, RATED_ROW + 3, 1, "Step", "Formula");
        TableTestProjects.row(sheet, RATED_ROW + 4, 1, "RETURN", "= $Base");
        TableTestProjects.row(sheet, RATED_ROW + 5, 1, "Base", "= 1.5");

        TableTestProjects.row(sheet, GRADED_ROW, 1, "Spreadsheet Double " + GRADED + " ( Person person )");
        sheet.addMergedRegion(new CellRangeAddress(GRADED_ROW, GRADED_ROW, 1, 2));
        TableTestProjects.row(sheet, GRADED_ROW + 1, 1, "Steps", "Formula");
        TableTestProjects.row(sheet, GRADED_ROW + 2, 1, "Premium", "= 10");
        TableTestProjects.row(sheet, GRADED_ROW + 3, 1, "Graded", "= $Premium / 2");

        TableTestProjects.row(sheet, PLAIN_ROW, 1, "Spreadsheet SpreadsheetResult " + PLAIN + " ( Person person )");
        sheet.addMergedRegion(new CellRangeAddress(PLAIN_ROW, PLAIN_ROW, 1, 2));
        TableTestProjects.row(sheet, PLAIN_ROW + 1, 1, "Step", "Formula");
        TableTestProjects.row(sheet, PLAIN_ROW + 2, 1, "First", "= 1");
        TableTestProjects.row(sheet, PLAIN_ROW + 3, 1, "Last", "= 2");

        TableTestProjects.row(sheet, RATIO_ROW, 1, "Spreadsheet Double " + RATIO + " ( Person person )");
        sheet.addMergedRegion(new CellRangeAddress(RATIO_ROW, RATIO_ROW, 1, 3));
        TableTestProjects.row(sheet, RATIO_ROW + 1, 1, "Step", "Value", "RETURN");
        TableTestProjects.row(sheet, RATIO_ROW + 2, 1, "Base", "= 2", "= $Value$Base * 2");

        TableTestProjects.row(sheet, QUALIFIED_ROW, 1,
                "Spreadsheet org.openl.rules.calc.SpreadsheetResult " + QUALIFIED + " ( Person person )");
        sheet.addMergedRegion(new CellRangeAddress(QUALIFIED_ROW, QUALIFIED_ROW, 1, 2));
        TableTestProjects.row(sheet, QUALIFIED_ROW + 1, 1, "Step", "Formula");
        TableTestProjects.row(sheet, QUALIFIED_ROW + 2, 1, "Last", "= 2");

        TableTestProjects.row(sheet, NOTHING_ROW, 1, "Spreadsheet void " + NOTHING + " ( Person person )");
        sheet.addMergedRegion(new CellRangeAddress(NOTHING_ROW, NOTHING_ROW, 1, 2));
        TableTestProjects.row(sheet, NOTHING_ROW + 1, 1, "Step", "Formula");
        TableTestProjects.row(sheet, NOTHING_ROW + 2, 1, "RETURN", "= 2");

        TableTestProjects.row(sheet, 84, 1, "Datatype Team");
        TableTestProjects.row(sheet, 85, 1, "String", "code");
        TableTestProjects.row(sheet, 86, 1, "Person", "lead");

        TableTestProjects.row(sheet, TEAMS_ROW, 1, "Data Team " + TEAMS);
        TableTestProjects.row(sheet, TEAMS_ROW + 1, 1, "_PK_", "code", "lead");
        TableTestProjects.row(sheet, TEAMS_ROW + 2, 1, null, null, ">people");
        TableTestProjects.row(sheet, TEAMS_ROW + 3, 1, "ID", "Code", "Lead");
        TableTestProjects.row(sheet, TEAMS_ROW + 4, 1, "T1", "Red", "Ann");
        TableTestProjects.row(sheet, TEAMS_ROW + 5, 1, "T2", null, "Ann");

        // The titles name no field, so the compiler reads the fields down the first column.
        TableTestProjects.row(sheet, CREW_ROW, 1, "Data Person " + CREW);
        TableTestProjects.row(sheet, CREW_ROW + 1, 1, "name", "Person Name", "Bob", "Eve");
        TableTestProjects.row(sheet, CREW_ROW + 2, 1, "age", "Years", "30", null);

        TableTestProjects.row(sheet, RATED_TEST_ROW, 1, "Test " + RATED + " " + RATED_TEST);
        TableTestProjects.row(sheet, RATED_TEST_ROW + 1, 1, "person", "_res_");
        TableTestProjects.row(sheet, RATED_TEST_ROW + 2, 1, ">people", null);
        TableTestProjects.row(sheet, RATED_TEST_ROW + 3, 1, "Insured", "Premium");
        TableTestProjects.row(sheet, RATED_TEST_ROW + 4, 1, "Ann", "1.5");

        TableTestProjects.row(sheet, 109, 1, "Method Integer " + ANSWER + "()");
        TableTestProjects.row(sheet, 110, 1, "return 42;");

        TableTestProjects.row(sheet, TALL_ROW, 1, "Spreadsheet SpreadsheetResult " + TALL + " ( Person person )");
        sheet.addMergedRegion(new CellRangeAddress(TALL_ROW, TALL_ROW, 1, 2));
        TableTestProjects.row(sheet, TALL_ROW + 1, 1, "Step", "Formula");
        TableTestProjects.row(sheet, TALL_ROW + 2, 1, "Base", "= 1");
        TableTestProjects.row(sheet, TALL_ROW + 3, 1, null, "the base rate");
        sheet.addMergedRegion(new CellRangeAddress(TALL_ROW + 2, TALL_ROW + 3, 1, 1));

        TableTestProjects.row(sheet, RATED_RUN_ROW, 1, "Run " + RATED + " " + RATED_RUN);
        TableTestProjects.row(sheet, RATED_RUN_ROW + 1, 1, "person");
        TableTestProjects.row(sheet, RATED_RUN_ROW + 2, 1, ">people");
        TableTestProjects.row(sheet, RATED_RUN_ROW + 3, 1, "Insured");
        TableTestProjects.row(sheet, RATED_RUN_ROW + 4, 1, "Ann");
    }
}
