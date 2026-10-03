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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.RawTableCell;
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

    private final TableThemeService service = new TableThemeService();

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, TableThemeTest::fillSheet);
    }

    @Test
    void appliesToDatatypesAndVocabulariesOnly() {
        var model = TableTestProjects.projectModel(dir);

        assertNotNull(service.layoutOf(TableTestProjects.table(model, "Person"), THEME));
        assertNotNull(service.layoutOf(TableTestProjects.table(model, "Code"), THEME));
        assertNull(service.layoutOf(TableTestProjects.table(model, "people"), THEME));
    }

    @Test
    void offersForATableTheThemesWithALookForItsKind() {
        var model = TableTestProjects.projectModel(dir);
        var datatypeOnly = new TableThemeService("classpath*:test-table-themes/datatype-only.yaml");

        assertEquals(List.of("default", "green"), ids(service.getThemes(TableTestProjects.table(model, "Code"))));
        assertEquals(List.of("datatype-only"), ids(datatypeOnly.getThemes(TableTestProjects.table(model, "Person"))));
        assertTrue(datatypeOnly.getThemes(TableTestProjects.table(model, "Code")).isEmpty(),
                "The theme has no look for a Vocabulary");
        assertTrue(service.getThemes(TableTestProjects.table(model, "people")).isEmpty(), "No theme styles Data");
    }

    @Test
    void leavesATableOfAKindTheThemeNamesNoLookFor() {
        var model = TableTestProjects.projectModel(dir);
        var datatypeOnly = new TableThemeService("classpath*:test-table-themes/datatype-only.yaml");
        var code = TableTestProjects.table(model, "Code");

        assertNull(datatypeOnly.layoutOf(code, "datatype-only"));
        var grid = GridTableUtils.getOriginalTable(code.getGridTable());
        grid.edit();
        try {
            assertFalse(datatypeOnly.writer("datatype-only").write(code, grid));
        } finally {
            grid.stopEditing();
        }
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
        assertNull(layout.at(2, 1).style().border(), "A row other than the last is not closed");
        assertEquals(ThemeLineStyle.THIN, layout.at(4, 1).style().border().bottom().style());
        assertEquals("Franklin Gothic Book", layout.at(3, 1).style().fontFamily());
        // A cell outside the table is not reached.
        assertNull(layout.at(1, 5));
    }

    @Test
    void givesTheTitleRowOfADatatypeTheTitleLook() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), "Titled"), THEME);

        assertEquals(Boolean.TRUE, layout.at(20, 1).style().bold());
        assertNull(layout.at(20, 2).style().background(), "The titles are not the field names");
        assertEquals(NAME_BACKGROUND, layout.at(21, 2).style().background());
    }

    @Test
    void givesTheFieldsOfATransposedDatatypeTheLookOfTheirPlace() {
        var layout = service.layoutOf(TableTestProjects.table(TableTestProjects.projectModel(dir), POINT), THEME);

        // Each field runs down a column, so each place runs across the table: the types, the names, the defaults.
        for (var column = 1; column <= 3; column++) {
            assertNull(layout.at(POINT_ROW + 1, column).style().background(), "A type is not a name");
            assertEquals(NAME_BACKGROUND, layout.at(POINT_ROW + 2, column).style().background());
            assertEquals(ThemeHorizontalAlign.CENTER, layout.at(POINT_ROW + 3, column).style().align());
            // The last row closes the table, as it closes an upright one.
            assertNull(layout.at(POINT_ROW + 2, column).style().border(), "Only the last row is closed");
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
        assertNull(layout.at(ITEM_ROW + 2, 1).style().background(), "The titles are not the field names");
        assertNull(layout.at(ITEM_ROW + 1, 2).style().bold(), "A type is not a title");
        assertEquals(NAME_BACKGROUND, layout.at(ITEM_ROW + 2, 2).style().background());
        assertEquals(NAME_BACKGROUND, layout.at(ITEM_ROW + 2, 3).style().background());
    }

    @Test
    void splitsTheHeaderIntoKeywordNameAndType() {
        var header = service.theme(THEME).lookOf(false).header();
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
        var themed = readThemed(table, service.layoutOf(table, THEME));
        // The header is bold in the workbook, and the theme says nothing of bold for the keyword: it stays bold, on
        // the screen as in the workbook the theme is written into.
        var drawn = themed.source.getFirst().getFirst().runs().getFirst().style();
        assertEquals(Boolean.TRUE, drawn.bold());
        assertEquals("#808080", drawn.color());

        write(tables(TableTestProjects.projectModel(dir), ACCOUNT));

        var written = read(ACCOUNT).getFirst().getFirst().runs().getFirst().style();
        assertEquals(drawn.bold(), written.bold());
        assertEquals(drawn.color(), written.color());
    }

    @Test
    void keepsTheSizeOfAFontOfHalfAPointTheThemeSaysNothingOf() throws IOException {
        // Green names no size, so the header keeps the 10.5 points it is written in.
        write(tables(TableTestProjects.projectModel(dir), ACCOUNT), "green");

        try (var workbook = workbook()) {
            var header = cell(workbook.getSheet(SHEET), ACCOUNT_ROW, 1);
            assertEquals(HALF_POINT_HEIGHT, header.getCellStyle().getFont().getFontHeight());
        }
    }

    @Test
    void writesTheThemeIntoTheWorkbook() throws IOException {
        var model = TableTestProjects.projectModel(dir);
        var written = write(tables(model, "Person", "Code"));

        assertEquals(2, written.size());
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

        var written = write(List.of(TableTestProjects.table(TableTestProjects.projectModel(dir), "people")));

        assertTrue(written.isEmpty());
        assertArrayEquals(before, Files.readAllBytes(dir.resolve(SHEET + ".xlsx")),
                "No workbook is saved when no table is themed");
    }

    /** The table read with the styles of its cells and the look a theme gives each of them. */
    private static RawTableView readThemed(IOpenLTable table, ThemedTable theme) {
        return new RawTableReader().read(table, RawTableRead.builder().withStyles(true).theme(theme).build());
    }

    private IOpenLTable person() {
        return TableTestProjects.table(TableTestProjects.projectModel(dir), "Person");
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
     * every table filled red, a Datatype whose header is not merged over it, and two Datatypes written transposed.
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
    }
}
