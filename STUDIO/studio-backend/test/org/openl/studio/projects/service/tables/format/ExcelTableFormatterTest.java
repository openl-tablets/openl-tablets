package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FontScheme;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyles;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.write.TableWriter;

/**
 * Covers the OpenL table style written into a workbook: the built-in styles of the roles, the theme colours and the
 * theme fonts, the lines at the edges of the regions, what is kept from each cell, and a second formatting adding
 * nothing.
 *
 * @author Yury Molchan
 */
class ExcelTableFormatterTest {

    private static final String SHEET = "Rules";

    /** A SmartRules table with properties, a number format on its rates, and its first two rules grouped by hour. */
    private static final int RATE_ROW = 1;
    private static final String RATE_HEADER = "SmartRules Double Rate(Integer hour, String day)";

    /** The factorial of a number, its steps nested by the indent of their operations. */
    private static final int FACTORIAL_ROW = 10;
    private static final int LABEL = 1;
    private static final int OPERATION = 3;
    private static final int ACTION = 5;
    private static final int FIRST_STEP = FACTORIAL_ROW + 3;
    private static final int NESTED_STEP = FIRST_STEP + 3;
    private static final int RETURN_STEP = FIRST_STEP + 4;
    private static final int SUBROUTINE_STEP = FIRST_STEP + 5;
    private static final int LAST_STEP = FIRST_STEP + 7;

    private static final ThemedColor TEXT = new ThemedColor(1, 0);
    private static final ThemedColor MUTED = new ThemedColor(1, 500);
    private static final ThemedColor LT2 = new ThemedColor(2, 0);

    /** The named styles Excel leaves in a workbook whose sheets were copied from other workbooks. */
    private static final int MANY_STYLES = 20_000;

    @TempDir
    Path dir;

    @Test
    void linksEachCellToTheBuiltInStyleOfItsRole() throws IOException {
        try (var workbook = formatted(ExcelTableFormatterTest::fillSheet)) {
            assertEquals("Title", namedStyleAt(workbook, RATE_ROW, 1).getName());
            assertEquals(15, namedStyleAt(workbook, RATE_ROW, 1).getBuiltinId());
            assertTrue(namedStyleAt(workbook, RATE_ROW, 1).getCustomBuiltin(), "The style is redefined");
            assertEquals("Explanatory Text", namedStyleAt(workbook, RATE_ROW + 1, 2).getName());
            assertEquals("Heading 3", namedStyleAt(workbook, RATE_ROW + 2, 1).getName());
            assertEquals("Heading 2", namedStyleAt(workbook, RATE_ROW + 2, 3).getName());
            assertEquals("Input", namedStyleAt(workbook, RATE_ROW + 3, 2).getName());
            assertEquals("Output", namedStyleAt(workbook, RATE_ROW + 3, 3).getName());
            assertEquals("Calculation", namedStyleAt(workbook, FACTORIAL_ROW + 1, OPERATION).getName());
            assertEquals("Heading 4", namedStyleAt(workbook, SUBROUTINE_STEP + 1, LABEL).getName());
            assertEquals("Heading 3", namedStyleAt(workbook, SUBROUTINE_STEP, LABEL).getName(),
                    "A heading takes the style of a title");
            assertEquals("Total", namedStyleAt(workbook, RETURN_STEP, ACTION).getName());
            // Every role style is written, and each once.
            var named = workbook.getStylesSource().getCTStylesheet().getCellStyles();
            assertEquals(10, named.sizeOfCellStyleArray(), "Normal and the nine role styles");
            assertEquals(named.sizeOfCellStyleArray(), named.getCount());
            assertStylesAreValid(workbook);
        }
    }

    @Test
    void writesThemeColoursAndThemeFontsOnly() throws IOException {
        try (var workbook = formatted(ExcelTableFormatterTest::fillSheet)) {
            var title = styleAt(workbook, RATE_ROW + 2, 1);
            assertEquals(FillPatternType.SOLID_FOREGROUND, title.getFillPattern());
            assertEquals(LT2, ThemedColor.of(title.getFillForegroundColorColor()));
            assertEquals(new ThemedColor(4, 600), ThemedColor.of(styleAt(workbook, RATE_ROW + 2, 3)
                    .getFillForegroundColorColor()));
            assertEquals(new ThemedColor(4, 800), ThemedColor.of(styleAt(workbook, RATE_ROW + 3, 3)
                    .getFillForegroundColorColor()));
            assertEquals(FillPatternType.NO_FILL, styleAt(workbook, RATE_ROW + 3, 2).getFillPattern(),
                    "A value has no fill, not a white one");

            var heading = title.getFont();
            assertEquals(FontScheme.MAJOR, heading.getScheme());
            assertEquals("Calibri Light", heading.getFontName(), "The face of the Office theme the workbook gets");
            assertTrue(heading.getBold());
            assertEquals(TEXT, colourOf(heading));
            var value = styleAt(workbook, RATE_ROW + 3, 2).getFont();
            assertEquals(FontScheme.MINOR, value.getScheme());
            assertEquals("Calibri", value.getFontName());
            assertEquals(11, value.getFontHeightInPoints(), "The size of the Normal style");
            var properties = styleAt(workbook, RATE_ROW + 1, 2).getFont();
            assertTrue(properties.getItalic());
            assertEquals(MUTED, colourOf(properties));
            assertEquals(MUTED, colourOf(styleAt(workbook, FACTORIAL_ROW + 1, OPERATION).getFont()), "Code is muted");
        }
    }

    @Test
    void drawsTheLinesAtTheEdgesOfTheRegions() throws IOException {
        try (var workbook = formatted(ExcelTableFormatterTest::fillSheet)) {
            // Every line is drawn in the darkest text colour of the theme.
            var signature = styleAt(workbook, RATE_ROW, 2);
            assertLine(BorderStyle.THIN, TEXT, signature.getBorderTop(), signature.getTopBorderXSSFColor());
            assertLine(BorderStyle.THIN, TEXT, signature.getBorderBottom(), signature.getBottomBorderXSSFColor());
            // The properties and the code draw a line above their first row and below their last one.
            var properties = styleAt(workbook, RATE_ROW + 1, 3);
            assertLine(BorderStyle.THIN, TEXT, properties.getBorderTop(),
                    properties.getTopBorderXSSFColor());
            assertLine(BorderStyle.THIN, TEXT, properties.getBorderBottom(),
                    properties.getBottomBorderXSSFColor());
            var code = styleAt(workbook, FACTORIAL_ROW + 1, OPERATION);
            assertLine(BorderStyle.THIN, TEXT, code.getBorderTop(), code.getTopBorderXSSFColor());
            assertLine(BorderStyle.THIN, TEXT, code.getBorderBottom(), code.getBottomBorderXSSFColor());
            // The titles and a section draw a line above their first row only.
            var titles = styleAt(workbook, RATE_ROW + 2, 3);
            assertLine(BorderStyle.THIN, TEXT, titles.getBorderTop(),
                    titles.getTopBorderXSSFColor());
            assertEquals(BorderStyle.NONE, titles.getBorderBottom(), "No line below the titles");
            assertEquals(BorderStyle.NONE, styleAt(workbook, RATE_ROW + 3, 2).getBorderTop(), "No line above values");
            // The hour is merged over the first two rules: the rule after them starts a group.
            var group = styleAt(workbook, RATE_ROW + 5, 3);
            assertLine(BorderStyle.HAIR, TEXT, group.getBorderTop(), group.getTopBorderXSSFColor());
            assertEquals(BorderStyle.NONE, styleAt(workbook, RATE_ROW + 4, 3).getBorderTop(), "No line inside it");
            var closing = styleAt(workbook, RATE_ROW + 5, 1);
            assertLine(BorderStyle.THIN, TEXT, closing.getBorderBottom(), closing.getBottomBorderXSSFColor());
            assertEquals(BorderStyle.NONE, closing.getBorderRight(), "No vertical lines");

            var result = styleAt(workbook, RETURN_STEP, ACTION);
            assertLine(BorderStyle.THIN, TEXT, result.getBorderTop(), result.getTopBorderXSSFColor());
            assertLine(BorderStyle.DOUBLE, TEXT, result.getBorderBottom(), result.getBottomBorderXSSFColor());
            var section = styleAt(workbook, SUBROUTINE_STEP, ACTION);
            assertLine(BorderStyle.THIN, TEXT, section.getBorderTop(),
                    section.getTopBorderXSSFColor());
            assertEquals(BorderStyle.NONE, section.getBorderBottom());
            assertEquals(HorizontalAlignment.CENTER, section.getAlignment(), "A heading is centred");
            var last = styleAt(workbook, LAST_STEP, ACTION);
            assertLine(BorderStyle.THIN, TEXT, last.getBorderBottom(), last.getBottomBorderXSSFColor());
        }
    }

    @Test
    void drawsALineAboveTheFirstRowOfEachRegionOfTheTitlesOfWhatATableGives() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(SHEET);
            row(sheet, 0, 0, "SimpleLookup Double Fee(String plan, String term, String size)");
            row(sheet, 1, 0, "Plan", "Annual", "Monthly");
            row(sheet, 2, 0, null, "Small", "Large");
            row(sheet, 3, 0, "Basic", "1", "2");
            var table = new GridRegion(0, 0, 3, 2);
            // A lookup lays out a region for each row of its horizontal values: each row is set apart.
            new ExcelTableFormatter().format(sheet, table, lookupRegions(List.of(
                    new RawTableRegion(RawTableRole.OUTPUT_TITLE, null, 1, 1, 1, 2),
                    new RawTableRegion(RawTableRole.OUTPUT_TITLE, null, 2, 1, 2, 2))));
            for (var at = 1; at <= 2; at++) {
                var title = styleAt(workbook, at, 2);
                assertLine(BorderStyle.THIN, TEXT, title.getBorderTop(), title.getTopBorderXSSFColor());
            }
            assertEquals(BorderStyle.NONE, styleAt(workbook, 2, 0).getBorderTop(), "A title of what it takes");

            // One region of titles over two rows, as a transposed table lays it out, has one line, above it.
            new ExcelTableFormatter().format(sheet, table, lookupRegions(List.of(
                    new RawTableRegion(RawTableRole.OUTPUT_TITLE, null, 1, 1, 2, 2))));
            assertEquals(BorderStyle.NONE, styleAt(workbook, 2, 2).getBorderTop());
        }
    }

    @Test
    void mergesTheHeaderIntoOneCellAcrossTheTable() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(SHEET);
            row(sheet, 0, 0, "SimpleLookup Double Fee(String plan, String term, String size)");
            row(sheet, 3, 0, "Basic", "1", "2");
            var table = new GridRegion(0, 0, 3, 2);

            new ExcelTableFormatter().format(sheet, table, lookupRegions(List.of(
                    new RawTableRegion(RawTableRole.OUTPUT_TITLE, null, 1, 1, 2, 2))));

            assertEquals(List.of(new CellRangeAddress(0, 0, 0, 2)), sheet.getMergedRegions());
        }
    }

    @Test
    void widensAHeaderMergedOverAPartOfTheRowAndLeavesOneWithAValueBesideIt() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(SHEET);
            row(sheet, 0, 0, "SimpleLookup Double Fee(String plan, String term, String size)");
            merge(sheet, 0, 0, 0, 1);
            var table = new GridRegion(0, 0, 3, 2);
            var regions = lookupRegions(List.of(new RawTableRegion(RawTableRole.OUTPUT_TITLE, null, 1, 1, 2, 2)));

            new ExcelTableFormatter().format(sheet, table, regions);
            assertEquals(List.of(new CellRangeAddress(0, 0, 0, 2)), sheet.getMergedRegions(), "The whole row");

            var other = workbook.createSheet("Other");
            row(other, 0, 0, "SimpleLookup Double Fee(String plan, String term, String size)", null, "note");
            new ExcelTableFormatter().format(other, table, regions);
            assertTrue(other.getMergedRegions().isEmpty(), "A value beside the header is kept");
        }
    }

    @Test
    void drawsTheLineOfAGroupColumnFromTheTitlesDown() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(SHEET);
            row(sheet, 0, 0, "SimpleLookup Double Fee(String plan, String term)");
            row(sheet, 1, 0, "properties", "description", "Fees");
            row(sheet, 2, 0, "Plan", "Basic", "Gold");
            row(sheet, 3, 0, "A", "1", "2");
            var regions = new TableRegions(List.of(
                    new RawTableRegion(RawTableRole.SIGNATURE, null, 0, 0, 0, 2),
                    new RawTableRegion(RawTableRole.PROPERTIES, null, 1, 0, 1, 2),
                    new RawTableRegion(RawTableRole.INPUT_TITLE, null, 2, 0, 2, 0),
                    new RawTableRegion(RawTableRole.OUTPUT_TITLE, null, 2, 1, 2, 2),
                    new RawTableRegion(RawTableRole.VALUE, null, 3, 0, 3, 0),
                    new RawTableRegion(RawTableRole.OUTPUT, null, 3, 1, 3, 2)),
                    new RawTableGroups(List.of(), List.of(2)), List.of(), Map.of());

            new ExcelTableFormatter().format(sheet, new GridRegion(0, 0, 3, 2), regions);

            assertEquals(BorderStyle.NONE, styleAt(workbook, 0, 2).getBorderLeft(), "Not across the header");
            assertEquals(BorderStyle.NONE, styleAt(workbook, 1, 2).getBorderLeft(), "Nor across the properties");
            assertEquals(BorderStyle.HAIR, styleAt(workbook, 2, 2).getBorderLeft(), "From the titles");
            assertEquals(BorderStyle.HAIR, styleAt(workbook, 3, 2).getBorderLeft(), "Down the values");
        }
    }

    /** The regions of a lookup of two rows of horizontal values, which take the given titles. */
    private static TableRegions lookupRegions(List<RawTableRegion> titles) {
        var regions = new ArrayList<RawTableRegion>();
        regions.add(new RawTableRegion(RawTableRole.SIGNATURE, null, 0, 0, 0, 2));
        regions.add(new RawTableRegion(RawTableRole.INPUT_TITLE, null, 1, 0, 2, 0));
        regions.addAll(titles);
        regions.add(new RawTableRegion(RawTableRole.VALUE, null, 3, 0, 3, 0));
        regions.add(new RawTableRegion(RawTableRole.OUTPUT, null, 3, 1, 3, 2));
        return new TableRegions(regions, RawTableGroups.NONE, List.of(), Map.of());
    }

    @Test
    void keepsTheNumberFormatAndTheIndentOfACell() throws IOException {
        try (var workbook = formatted(ExcelTableFormatterTest::fillSheet)) {
            assertEquals("0.00", styleAt(workbook, RATE_ROW + 3, 3).getDataFormatString());
            var nested = styleAt(workbook, NESTED_STEP, OPERATION);
            assertEquals(1, nested.getIndention(), "The level of the step stays as it is");
            assertEquals(HorizontalAlignment.LEFT, nested.getAlignment(), "An indent shows in a text aligned left");
            assertEquals(HorizontalAlignment.GENERAL, styleAt(workbook, FIRST_STEP, OPERATION).getAlignment());
            assertEquals(HorizontalAlignment.CENTER, styleAt(workbook, RATE_ROW + 2, 2).getAlignment(), "A title");
            assertEquals(VerticalAlignment.CENTER, styleAt(workbook, RATE_ROW + 3, 1).getVerticalAlignment());
            assertEquals("SET", workbook.getSheet(SHEET).getRow(NESTED_STEP).getCell(OPERATION).getStringCellValue(),
                    "No value changes");
        }
    }

    @Test
    void writesTheSignatureInItsPiecesAndEveryOtherTextPlain() throws IOException {
        try (var workbook = formatted(ExcelTableFormatterTest::fillSheet)) {
            var header = workbook.getSheet(SHEET).getRow(RATE_ROW).getCell(1).getRichStringCellValue();
            assertEquals(RATE_HEADER, header.getString());
            var keyword = fontOfRun(header, 0);
            assertEquals(MUTED, colourOf(keyword));
            assertEquals(FontScheme.MAJOR, keyword.getScheme());
            var name = fontOfRun(header, RATE_HEADER.indexOf("Rate"));
            assertTrue(name.getBold(), "The name of the table is bold");
            assertEquals(TEXT, colourOf(name));
            assertEquals(TEXT, colourOf(fontOfRun(header, RATE_HEADER.indexOf("hour"))), "A parameter name");
            assertEquals(MUTED, colourOf(fontOfRun(header, RATE_HEADER.indexOf("Integer"))), "A parameter type");

            var rich = workbook.getSheet(SHEET).getRow(RATE_ROW + 3).getCell(2).getRichStringCellValue();
            assertEquals("Weekday", rich.getString());
            assertEquals(0, rich.numFormattingRuns(), "Rich text in a value is written plain");
        }
    }

    @Test
    void addsNothingWhenATableIsFormattedAgain() throws IOException {
        try (var once = formatted(ExcelTableFormatterTest::fillSheet)) {
            var counts = countsOf(once);
            format(TableTestProjects.projectModel(dir));
            try (var twice = read()) {
                assertEquals(counts, countsOf(twice));
                assertStylesAreValid(twice);
            }
        }
    }

    @Test
    void formatsTheGivenLinesAloneAndLeavesTheOtherCellsAsTheyAre() throws IOException {
        var table = TableTestProjects.table(TableTestProjects.projectModel(dir, SHEET,
                ExcelTableFormatterTest::fillSheet), "Rate");
        var grid = GridTableUtils.getOriginalTable(table.getGridTable());
        grid.edit();
        try {
            var lines = new ExcelTableFormatter.Lines(false, new TreeSet<>(Set.of(3)), new TreeSet<>(Set.of(2)));
            var model = (XlsSheetGridModel) grid.getGrid();
            new ExcelTableFormatter().format(model.getSheetToWrite(), model, grid.getRegion(),
                    TableRegionLayout.compiledRegionsOf(table), lines);
            TableWriter.saveWorkbook((XlsSheetGridModel) grid.getGrid());
        } finally {
            grid.stopEditing();
        }

        try (var workbook = read()) {
            assertEquals("Input", namedStyleAt(workbook, RATE_ROW + 3, 2).getName(), "A cell of the row");
            assertEquals("Output", namedStyleAt(workbook, RATE_ROW + 5, 3).getName(), "A cell of the column");
            assertEquals(0, styleAt(workbook, RATE_ROW + 5, 2).getCoreXf().getXfId(), "A cell of neither");
            assertTrue(workbook.getSheet(SHEET).getRow(RATE_ROW).getCell(1).getRichStringCellValue()
                    .numFormattingRuns() > 1, "The header is written in its pieces every time");
        }
    }

    @Test
    void readsTheFontsOfTheThemeOfTheWorkbook() throws IOException {
        try (var workbook = formatted(sheet -> {
            fillSheet(sheet);
            var styles = ((XSSFWorkbook) sheet.getWorkbook()).getStylesSource();
            styles.ensureThemesTable();
            try (var office = Objects.requireNonNull(WorkbookStyles.class.getResourceAsStream("office-theme.xml"))) {
                var theme = new String(office.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("\"Calibri Light\"", "\"Georgia\"")
                        .replace("\"Calibri\"", "\"Verdana\"");
                styles.getTheme().readFrom(new ByteArrayInputStream(theme.getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        })) {
            assertEquals("Georgia", styleAt(workbook, RATE_ROW + 2, 1).getFont().getFontName());
            assertEquals("Verdana", styleAt(workbook, RATE_ROW + 3, 2).getFont().getFontName());
        }
    }

    @Test
    void givesAWorkbookWithNoThemeTheOfficeTheme() throws IOException {
        try (var workbook = formatted(ExcelTableFormatterTest::fillSheet)) {
            var theme = workbook.getTheme();
            assertNotNull(theme, "A workbook POI writes has no theme");
            assertEquals("4472C4", theme.getThemeColor(4).getARGBHex().substring(2), "Accent 1 of the Office theme");
        }
    }

    @Test
    void takesTheFontSizeOfTheNormalStyle() throws IOException {
        try (var workbook = formatted(sheet -> {
            sheet.getWorkbook().getFontAt(0).setFontHeightInPoints((short) 10);
            fillSheet(sheet);
        })) {
            assertEquals(10, styleAt(workbook, RATE_ROW + 2, 1).getFont().getFontHeightInPoints());
            assertEquals(10, styleAt(workbook, RATE_ROW + 3, 2).getFont().getFontHeightInPoints());
        }
    }

    @Test
    void redefinesABuiltInStyleTheWorkbookNamesInPlace() throws IOException {
        try (var workbook = formatted(sheet -> {
            fillSheet(sheet);
            // An Input style as Office defines it, orange and bordered, as Excel adds it once a user applies it.
            var styles = ((XSSFWorkbook) sheet.getWorkbook()).getStylesSource();
            var input = CTXf.Factory.newInstance();
            input.setFontId(0);
            input.setFillId(0);
            input.setBorderId(0);
            var at = styles.putCellStyleXf(input) - 1;
            var entry = namedStyles(sheet).addNewCellStyle();
            entry.setName("Input");
            entry.setXfId(at);
            entry.setBuiltinId(20);
        })) {
            var inputs = workbook.getStylesSource().getCTStylesheet().getCellStyles().getCellStyleList().stream()
                    .filter(named -> "Input".equals(named.getName()))
                    .toList();
            assertEquals(1, inputs.size(), "The style is redefined, not added again");
            var definition = workbook.getStylesSource().getCellStyleXfAt((int) inputs.getFirst().getXfId());
            var font = workbook.getStylesSource().getFontAt((int) definition.getFontId());
            assertEquals(FontScheme.MINOR, font.getScheme());
        }
    }

    /**
     * A workbook naming tens of thousands of styles, as Excel leaves one whose sheets were copied from other workbooks,
     * is formatted at once. The role styles are looked for among them whether the workbook names them or not.
     */
    @Test
    void formatsAWorkbookNamingManyStylesAtOnce() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            fillSheet(sheet);
            var named = namedStyles(sheet);
            for (var at = 0; at < MANY_STYLES; at++) {
                var copied = named.addNewCellStyle();
                copied.setName("Copied style " + at);
                copied.setXfId(0);
            }
        });

        assertTimeout(Duration.ofSeconds(5), () -> format(model), "The role styles are added after the others");
        var again = TableTestProjects.projectModel(dir);
        assertTimeout(Duration.ofSeconds(5), () -> format(again), "The role styles are found after the others");

        try (var workbook = read()) {
            assertEquals("Title", namedStyleAt(workbook, RATE_ROW, 1).getName());
            assertEquals("Input", namedStyleAt(workbook, RATE_ROW + 3, 2).getName());
            assertEquals(1 + MANY_STYLES + 9,
                    workbook.getStylesSource().getCTStylesheet().getCellStyles().sizeOfCellStyleArray(),
                    "Normal, the copied styles and the nine role styles, each once");
        }
    }

    /** Gives a workbook the list of named styles, naming its Normal style, as Excel writes the list. */
    private static CTCellStyles namedStyles(Sheet sheet) {
        var named = ((XSSFWorkbook) sheet.getWorkbook()).getStylesSource().getCTStylesheet().addNewCellStyles();
        var normal = named.addNewCellStyle();
        normal.setName("Normal");
        normal.setXfId(0);
        normal.setBuiltinId(0);
        return named;
    }

    @Test
    void leavesATableOfAnXlsWorkbookAsItIs() throws IOException {
        var xls = dir.resolve("Rules.xls");
        try (var workbook = new HSSFWorkbook(); OutputStream out = Files.newOutputStream(xls)) {
            fillSheet(workbook.createSheet(SHEET));
            workbook.write(out);
        }
        var before = Files.readAllBytes(xls);
        var table = TableTestProjects.table(TableTestProjects.projectModel(dir), "Rate");
        var grid = GridTableUtils.getOriginalTable(table.getGridTable());
        grid.edit();
        try {
            assertFalse(new ExcelTableFormatter().format(table, grid));
        } finally {
            grid.stopEditing();
        }
        assertArrayEquals(before, Files.readAllBytes(xls), "Nothing is written");
    }

    @Test
    void savesATableUnformattedWhenItsFormattingFails() {
        var grid = mock(IGridTable.class);
        when(grid.getGrid()).thenThrow(new IllegalStateException("The sheet cannot be read"));
        var layout = RegionShift.of(new TableRegions(List.of(), RawTableGroups.NONE, List.of(), Map.of()), 1, 1);

        assertNull(assertDoesNotThrow(() -> ExcelTableFormatter.formatOnSave(grid, layout, false)));
    }

    /** Writes the project, formats every table of it and reads the workbook back. */
    private XSSFWorkbook formatted(Consumer<Sheet> content) throws IOException {
        format(TableTestProjects.projectModel(dir, SHEET, content));
        return read();
    }

    /** Formats every table of a project with one formatter, saving the workbook after each. */
    private static void format(ProjectModel model) {
        var formatter = new ExcelTableFormatter();
        for (var node : model.getAllTableSyntaxNodes()) {
            IOpenLTable table = new TableSyntaxNodeAdapter(node);
            var grid = GridTableUtils.getOriginalTable(table.getGridTable());
            grid.edit();
            try {
                if (formatter.format(table, grid)) {
                    TableWriter.saveWorkbook((XlsSheetGridModel) grid.getGrid());
                }
            } finally {
                grid.stopEditing();
            }
        }
    }

    private XSSFWorkbook read() throws IOException {
        try (var in = Files.newInputStream(dir.resolve(SHEET + ".xlsx"))) {
            return new XSSFWorkbook(in);
        }
    }

    private static XSSFCellStyle styleAt(XSSFWorkbook workbook, int row, int column) {
        return workbook.getSheet(SHEET).getRow(row).getCell(column).getCellStyle();
    }

    /** The named style a cell links. */
    private static CTCellStyle namedStyleAt(XSSFWorkbook workbook, int row, int column) {
        var xfId = styleAt(workbook, row, column).getCoreXf().getXfId();
        return Arrays.stream(workbook.getStylesSource().getCTStylesheet().getCellStyles().getCellStyleArray())
                .filter(named -> named.getXfId() == xfId)
                .findFirst()
                .orElseThrow();
    }

    /** Fails naming what a spreadsheet application would refuse in the style table of a workbook. */
    private static void assertStylesAreValid(XSSFWorkbook workbook) {
        var complaints = new ArrayList<XmlError>();
        var valid = workbook.getStylesSource().getCTStylesheet()
                .validate(new XmlOptions().setErrorListener(complaints));
        assertTrue(valid, () -> complaints.stream().map(String::valueOf).collect(Collectors.joining("; ")));
    }

    private static ThemedColor colourOf(XSSFFont font) {
        return ThemedColor.of(font.getXSSFColor());
    }

    private static XSSFFont fontOfRun(XSSFRichTextString text, int at) {
        for (var run = 0; run < text.numFormattingRuns(); run++) {
            var start = text.getIndexOfFormattingRun(run);
            if (start <= at && at < start + text.getLengthOfFormattingRun(run)) {
                return text.getFontOfFormattingRun(run);
            }
        }
        throw new AssertionError("No run holds the character at " + at);
    }

    private static void assertLine(BorderStyle style, ThemedColor colour, BorderStyle actual, XSSFColor actualColour) {
        assertEquals(style, actual);
        assertEquals(colour, ThemedColor.of(actualColour));
    }

    /** How many cell formats, cell styles, named styles, fonts, fills and borders a workbook holds. */
    private static List<Integer> countsOf(XSSFWorkbook workbook) {
        var styles = workbook.getStylesSource();
        return List.of(styles.getNumCellStyles(), styles._getStyleXfsSize(),
                styles.getCTStylesheet().getCellStyles().sizeOfCellStyleArray(), styles.getFonts().size(),
                styles.getFills().size(), styles.getBorders().size());
    }

    /** A SmartRules table with properties, and a TBasic table with a subroutine and a step that returns. */
    private static void fillSheet(Sheet sheet) {
        row(sheet, RATE_ROW, 1, RATE_HEADER);
        merge(sheet, RATE_ROW, RATE_ROW, 1, 3);
        row(sheet, RATE_ROW + 1, 1, "properties", "description", "Rates by the hour");
        row(sheet, RATE_ROW + 2, 1, "Hour", "Day", "Rate");
        row(sheet, RATE_ROW + 3, 1, "< 12", "Weekday", "1.5");
        row(sheet, RATE_ROW + 4, 1, null, "Weekend", "2");
        merge(sheet, RATE_ROW + 3, RATE_ROW + 4, 1, 1);
        row(sheet, RATE_ROW + 5, 1, ">= 12", "Weekday", "1");
        var workbook = sheet.getWorkbook();
        var rate = workbook.createCellStyle();
        rate.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        for (var at = RATE_ROW + 3; at <= RATE_ROW + 5; at++) {
            sheet.getRow(at).getCell(3).setCellStyle(rate);
        }
        if (workbook instanceof XSSFWorkbook xssf) {
            var rich = new XSSFRichTextString("Weekday");
            var bold = xssf.createFont();
            bold.setBold(true);
            rich.applyFont(0, 4, bold);
            sheet.getRow(RATE_ROW + 3).getCell(2).setCellValue(rich);
        }

        row(sheet, FACTORIAL_ROW, LABEL, "TBasic Integer Factorial (Integer n)");
        merge(sheet, FACTORIAL_ROW, FACTORIAL_ROW, LABEL, ACTION);
        row(sheet, FACTORIAL_ROW + 1, LABEL, "Label", "Description", "Operation", "Condition", "Action");
        row(sheet, FACTORIAL_ROW + 2, LABEL, "Name", "Description", "Operation", "Condition", "Action");
        row(sheet, FIRST_STEP, LABEL, null, "the result", "VAR", "factorial", "1");
        row(sheet, FIRST_STEP + 1, LABEL, null, "the multiplier", "VAR", "i", "1");
        row(sheet, FIRST_STEP + 2, LABEL, null, null, "WHILE", "i <= n", null);
        row(sheet, NESTED_STEP, LABEL, null, null, "SET", null, "Calculation");
        indent(sheet, NESTED_STEP, OPERATION);
        row(sheet, RETURN_STEP, LABEL, null, null, "RETURN", "factorial", null);
        row(sheet, SUBROUTINE_STEP, LABEL, "Calculation", "multiplies by i", "SUB", null, null);
        row(sheet, SUBROUTINE_STEP + 1, LABEL, null, null, "SET", null, "factorial *= i");
        indent(sheet, SUBROUTINE_STEP + 1, OPERATION);
        row(sheet, LAST_STEP, LABEL, null, "the next multiplier", "SET", null, "i += 1");
        indent(sheet, LAST_STEP, OPERATION);
    }

    /** Indents the text of a cell by one level, which nests the step under the one above. */
    private static void indent(Sheet sheet, int row, int column) {
        var style = sheet.getWorkbook().createCellStyle();
        style.setIndention((short) 1);
        sheet.getRow(row).getCell(column).setCellStyle(style);
    }
}
