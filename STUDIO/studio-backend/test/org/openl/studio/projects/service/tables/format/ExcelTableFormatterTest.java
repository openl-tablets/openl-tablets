package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.ArrayList;
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
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.RawTableGroups;
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
    private static final ThemedColor DK2 = new ThemedColor(3, 0);
    private static final ThemedColor ACCENT1 = new ThemedColor(4, 0);

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
            assertEquals("40% - Accent1", namedStyleAt(workbook, RATE_ROW + 2, 3).getName());
            assertEquals("Input", namedStyleAt(workbook, RATE_ROW + 3, 2).getName());
            assertEquals("Output", namedStyleAt(workbook, RATE_ROW + 3, 3).getName());
            assertEquals("Calculation", namedStyleAt(workbook, FACTORIAL_ROW + 1, OPERATION).getName());
            assertEquals("Heading 4", namedStyleAt(workbook, SUBROUTINE_STEP + 1, LABEL).getName());
            assertEquals("Heading 2", namedStyleAt(workbook, SUBROUTINE_STEP, LABEL).getName());
            assertEquals("Total", namedStyleAt(workbook, RETURN_STEP, ACTION).getName());
            // Every role style is written, and each once.
            var named = workbook.getStylesSource().getCTStylesheet().getCellStyles();
            assertEquals(11, named.sizeOfCellStyleArray(), "Normal and the ten role styles");
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
            var signature = styleAt(workbook, RATE_ROW, 2);
            assertLine(BorderStyle.THIN, DK2, signature.getBorderTop(), signature.getTopBorderXSSFColor());
            assertLine(BorderStyle.THIN, DK2, signature.getBorderBottom(), signature.getBottomBorderXSSFColor());
            var properties = styleAt(workbook, RATE_ROW + 1, 3);
            assertLine(BorderStyle.THIN, new ThemedColor(2, -250), properties.getBorderBottom(),
                    properties.getBottomBorderXSSFColor());
            // The hour is merged over the first two rules: the rule after them starts a group.
            var group = styleAt(workbook, RATE_ROW + 5, 3);
            assertLine(BorderStyle.HAIR, new ThemedColor(2, -250), group.getBorderTop(), group.getTopBorderXSSFColor());
            assertEquals(BorderStyle.NONE, styleAt(workbook, RATE_ROW + 4, 3).getBorderTop(), "No line inside it");
            var closing = styleAt(workbook, RATE_ROW + 5, 1);
            assertLine(BorderStyle.THIN, DK2, closing.getBorderBottom(), closing.getBottomBorderXSSFColor());
            assertEquals(BorderStyle.NONE, closing.getBorderRight(), "No vertical lines");

            var result = styleAt(workbook, RETURN_STEP, ACTION);
            assertLine(BorderStyle.THIN, ACCENT1, result.getBorderTop(), result.getTopBorderXSSFColor());
            assertLine(BorderStyle.DOUBLE, ACCENT1, result.getBorderBottom(), result.getBottomBorderXSSFColor());
            var section = styleAt(workbook, SUBROUTINE_STEP, ACTION);
            assertLine(BorderStyle.THIN, new ThemedColor(2, -250), section.getBorderBottom(),
                    section.getBottomBorderXSSFColor());
            var last = styleAt(workbook, LAST_STEP, ACTION);
            assertLine(BorderStyle.THIN, DK2, last.getBorderBottom(), last.getBottomBorderXSSFColor());
        }
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
            new ExcelTableFormatter().format(((XlsSheetGridModel) grid.getGrid()).getSheetToWrite(), grid.getRegion(),
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
            var named = styles.getCTStylesheet().addNewCellStyles();
            var normal = named.addNewCellStyle();
            normal.setName("Normal");
            normal.setXfId(0);
            normal.setBuiltinId(0);
            var entry = named.addNewCellStyle();
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
        return workbook.getStylesSource().getCTStylesheet().getCellStyles().getCellStyleList().stream()
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
