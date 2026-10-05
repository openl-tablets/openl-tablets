package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers writing the colours a theme makes of the theme colours of Excel: as those theme colours into a workbook
 * whose theme colours are the ones of the theme, so Excel offers them in its palette, and as {@code #rrggbb} into any
 * other workbook, whose theme stays as it is.
 */
class ExcelThemeColoursTest {

    private static final String SHEET = "Model";
    private static final String THEME = "standard";
    private static final String GREEN = "green";

    /** Where the Datatype the tests theme stands, and its cells: the type and the name of a field. */
    private static final int TYPE_ROW = 2;
    private static final int TYPE = 1;
    private static final int NAME = 2;

    /** Blue, Accent 5, Lighter 80%, the fill of a field name, and White, Background 1, Darker 50%, a muted text. */
    private static final ThemedColor FIELD_NAME = new ThemedColor(8, 800);
    private static final ThemedColor MUTED = new ThemedColor(0, -500);
    private static final String FIELD_NAME_RGB = "#ddebf7";
    private static final String MUTED_RGB = "#808080";

    /** The theme colours the standard theme makes its colours of, in the order a workbook numbers them. */
    private static final String[] OFFICE = {"FFFFFF", "000000", "E7E6E6", "44546A", "4472C4", "ED7D31", "A5A5A5",
            "FFC000", "5B9BD5", "70AD47", "0563C1", "954F72"};

    /** The theme colours of another theme of Excel, Trek, whose accents are browns. */
    private static final String[] TREK = {"FFFFFF", "000000", "FBEEC9", "4E3B30", "F0A22E", "A5644E", "B58B80",
            "C3986D", "A19574", "C17529", "AD1F1F", "FFC42F"};

    private final TableThemeService service = new TableThemeService();

    @TempDir
    Path dir;

    @Test
    void tellsTheThemeColoursOfAWorkbookByTheirColoursWhateverTheirName() throws IOException {
        var colours = service.theme(THEME).themeColors();
        try (var office = workbookOf(OFFICE); var trek = workbookOf(TREK); var unthemed = new XSSFWorkbook();
             var xls = new HSSFWorkbook()) {
            assertTrue(colours.areThoseOf(office), "Office 2013 - 2022, which the workbook names Test");
            assertFalse(colours.areThoseOf(trek), "Another theme, though its first text and background are alike");
            assertFalse(colours.areThoseOf(unthemed), "A workbook without a theme");
            assertFalse(colours.areThoseOf(xls), "An .xls workbook");
        }
    }

    @Test
    void writesTheColoursOfAProjectAsTheThemeColoursOfAWorkbookOfThem() throws IOException {
        writeProject(OFFICE, null);

        service.writer(THEME).writeAll(List.of(person()), Map.of());

        try (var workbook = workbook()) {
            // Every colour the theme makes of them is written as the theme colour it is, a font colour with its tint.
            assertEquals(FIELD_NAME, ThemedColor.of(fillOf(workbook, TYPE_ROW, NAME)));
            assertEquals(MUTED, ThemedColor.of(fontColourOf(workbook, TYPE_ROW, TYPE)));
        }
        // The workbook draws them as the theme does.
        var field = TableTestProjects.styledSource(person()).get(1);
        assertEquals(FIELD_NAME_RGB, field.get(NAME - 1).style().background());
        assertEquals(MUTED_RGB, field.get(TYPE - 1).style().color());
    }

    @Test
    void writesTheColoursAsRrggbbIntoAWorkbookOfOtherThemeColoursAndKeepsItsTheme() throws IOException {
        writeProject(TREK, null);

        service.writer(THEME).writeAll(List.of(person()), Map.of());

        try (var workbook = workbook()) {
            assertEquals(TREK[4], hex(workbook.getStylesSource().getTheme().getThemeColor(4)), "The theme stays");
            var fill = fillOf(workbook, TYPE_ROW, NAME);
            assertNull(ThemedColor.of(fill), "Trek has another fifth accent");
            assertEquals(FIELD_NAME_RGB, rgbOf(fill));
            // A colour of a theme colour the workbook has alike is written as #rrggbb all the same.
            var muted = fontColourOf(workbook, TYPE_ROW, TYPE);
            assertNull(ThemedColor.of(muted), "Every colour of the theme is written one way");
            assertEquals(MUTED_RGB, rgbOf(muted));
        }
    }

    @Test
    void writesTheThemeColourOverTheSameColourWrittenAsRrggbb() throws IOException {
        // The workbook has the theme colours, and the field name is filled with its colour as #rrggbb.
        writeProject(OFFICE, FIELD_NAME_RGB);

        service.writer(THEME).writeAll(List.of(person()), Map.of());

        try (var workbook = workbook()) {
            assertEquals(FIELD_NAME, ThemedColor.of(fillOf(workbook, TYPE_ROW, NAME)),
                    "A cell holding the colour the other way round has not the look");
        }
    }

    @Test
    void writingTheThemeColoursAgainAddsNoStylesOrFonts() throws IOException {
        writeProject(OFFICE, null);
        service.writer(THEME).writeAll(List.of(person()), Map.of());
        int fonts;
        int styles;
        try (var workbook = workbook()) {
            fonts = workbook.getNumberOfFonts();
            styles = workbook.getNumCellStyles();
        }

        service.writer(THEME).writeAll(List.of(person()), Map.of());

        try (var workbook = workbook()) {
            assertEquals(fonts, workbook.getNumberOfFonts());
            assertEquals(styles, workbook.getNumCellStyles());
        }
    }

    @Test
    void writesALineOfAThemeColourAsTheThemeColour() throws IOException {
        // The green theme draws its lines in Green, Accent 6, Darker 25%.
        writeProject(OFFICE, null);
        service.writer(GREEN).writeAll(List.of(person()), Map.of());
        int styles;
        try (var workbook = workbook()) {
            var line = styleOf(workbook, 1, TYPE).getBorderColor(BorderSide.BOTTOM);
            assertEquals(new ThemedColor(9, -250), ThemedColor.of(line), "The line under the header");
            styles = workbook.getNumCellStyles();
        }

        service.writer(GREEN).writeAll(List.of(person()), Map.of());

        try (var workbook = workbook()) {
            assertEquals(styles, workbook.getNumCellStyles(), "A line of the theme colour has the look");
        }
    }

    @Test
    void writesOneTableOfAWorkbookOfTheThemeColoursInTheThemeColours() throws IOException {
        writeProject(OFFICE, null);

        var workbook = writeOneTable();

        assertEquals(FIELD_NAME, ThemedColor.of(fillOf(workbook, TYPE_ROW, NAME)));
        assertEquals(MUTED, ThemedColor.of(fontColourOf(workbook, TYPE_ROW, TYPE)));
    }

    @Test
    void writesOneTableOfAWorkbookOfOtherThemeColoursInRrggbb() throws IOException {
        writeProject(TREK, null);

        var workbook = writeOneTable();

        assertEquals(TREK[4], hex(workbook.getStylesSource().getTheme().getThemeColor(4)), "The theme stays");
        assertEquals(FIELD_NAME_RGB, rgbOf(fillOf(workbook, TYPE_ROW, NAME)));
        assertNull(ThemedColor.of(fillOf(workbook, TYPE_ROW, NAME)));
    }

    @Test
    void writesTheColoursOfAWorkbookWithoutAThemeInRrggbb() throws IOException {
        // A workbook a program wrote, rather than Excel, has no theme.
        TableTestProjects.projectModel(dir, SHEET, ExcelThemeColoursTest::fillSheet);

        service.writer(THEME).writeAll(List.of(person()), Map.of());

        try (var workbook = workbook()) {
            assertNull(workbook.getStylesSource().getTheme(), "No theme is made for the workbook");
            var fill = fillOf(workbook, TYPE_ROW, NAME);
            assertFalse(fill.isThemed());
            assertEquals(FIELD_NAME_RGB, rgbOf(fill));
        }
    }

    /** Writes the theme into the Datatype alone, as an edit of the table does, and answers its workbook. */
    private XSSFWorkbook writeOneTable() {
        var table = person();
        var grid = GridTableUtils.getOriginalTable(table.getGridTable());
        grid.edit();
        try {
            assertTrue(service.writer(THEME).write(table, grid, TableMoves.NONE));
            return (XSSFWorkbook) ((XlsSheetGridModel) grid.getGrid()).getSheetToWrite().getWorkbook();
        } finally {
            grid.stopEditing();
        }
    }

    /**
     * Writes a project of one workbook holding a Datatype, its theme of the given colours.
     *
     * @param colours  the theme colours, in the order a workbook numbers them
     * @param nameFill the fill of the field name as {@code #rrggbb}, or {@code null} for none
     */
    private void writeProject(String[] colours, @Nullable String nameFill) throws IOException {
        try (var workbook = workbookOf(colours)) {
            var sheet = workbook.createSheet(SHEET);
            fillSheet(sheet);
            if (nameFill != null) {
                var style = workbook.createCellStyle();
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFillForegroundColor(PoiExcelHelper.getColor(PoiExcelHelper.toRgb(nameFill), workbook));
                sheet.getRow(TYPE_ROW).getCell(NAME).setCellStyle(style);
            }
            try (OutputStream out = Files.newOutputStream(dir.resolve(SHEET + ".xlsx"))) {
                workbook.write(out);
            }
        }
    }

    /** A workbook whose theme has the given theme colours. */
    private static XSSFWorkbook workbookOf(String[] colours) throws IOException {
        var workbook = new XSSFWorkbook();
        workbook.getStylesSource().ensureThemesTable();
        workbook.getStylesSource().getTheme().readFrom(new ByteArrayInputStream(theme(colours)));
        return workbook;
    }

    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, 1, 1, "Datatype Person");
        TableTestProjects.row(sheet, TYPE_ROW, 1, "String", "name");
        TableTestProjects.row(sheet, 3, 1, "Integer", "age");
    }

    /**
     * A theme of Excel of the given theme colours, its fonts those of Trek. The first text and background are the
     * colours of a window, as Excel writes them.
     */
    private static byte[] theme(String[] colours) {
        String[] slots = {"lt1", "dk1", "lt2", "dk2", "accent1", "accent2", "accent3", "accent4", "accent5",
                "accent6", "hlink", "folHlink"};
        var scheme = new StringBuilder("<a:dk1><a:sysClr val=\"windowText\" lastClr=\"%s\"/></a:dk1>"
                .formatted(colours[1]))
                .append("<a:lt1><a:sysClr val=\"window\" lastClr=\"%s\"/></a:lt1>".formatted(colours[0]));
        // A workbook numbers the first background before the first text; its theme holds the text first.
        for (var at : new int[]{3, 2, 4, 5, 6, 7, 8, 9, 10, 11}) {
            scheme.append("<a:%1$s><a:srgbClr val=\"%2$s\"/></a:%1$s>".formatted(slots[at], colours[at]));
        }
        return """
                <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Test">
                  <a:themeElements>
                    <a:clrScheme name="Test">%s</a:clrScheme>
                    <a:fontScheme name="Trek">
                      <a:majorFont><a:latin typeface="Franklin Gothic Medium"/><a:ea typeface=""/><a:cs typeface=""/>
                      </a:majorFont>
                      <a:minorFont><a:latin typeface="Franklin Gothic Book"/><a:ea typeface=""/><a:cs typeface=""/>
                      </a:minorFont>
                    </a:fontScheme>
                  </a:themeElements>
                </a:theme>
                """.formatted(scheme).getBytes(StandardCharsets.UTF_8);
    }

    private IOpenLTable person() {
        return TableTestProjects.table(TableTestProjects.projectModel(dir), "Person");
    }

    private XSSFWorkbook workbook() throws IOException {
        try (InputStream in = Files.newInputStream(dir.resolve(SHEET + ".xlsx"))) {
            return new XSSFWorkbook(in);
        }
    }

    private static XSSFColor fillOf(XSSFWorkbook workbook, int row, int column) {
        return styleOf(workbook, row, column).getFillForegroundXSSFColor();
    }

    private static XSSFColor fontColourOf(XSSFWorkbook workbook, int row, int column) {
        return styleOf(workbook, row, column).getFont().getXSSFColor();
    }

    private static XSSFCellStyle styleOf(XSSFWorkbook workbook, int row, int column) {
        return workbook.getSheet(SHEET).getRow(row).getCell(column).getCellStyle();
    }

    private static String rgbOf(XSSFColor color) {
        return "#%06x".formatted(PoiExcelHelper.toRgbValue(PoiExcelHelper.toRgb(color)));
    }

    private static String hex(XSSFColor color) {
        return "%06X".formatted(PoiExcelHelper.toRgbValue(PoiExcelHelper.toRgb(color)));
    }
}
