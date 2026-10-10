package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openxmlformats.schemas.drawingml.x2006.main.ThemeDocument;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * Covers a cell filled or its text coloured with a theme colour of Excel: the theme colour itself in a workbook of a
 * theme, the colour Office draws it in elsewhere.
 *
 * @author Yury Molchan
 */
class XlsSheetGridModelThemeColoursTest {

    /** Blue, Accent 1, Lighter 60%. */
    private static final ThemedColor LIGHT_BLUE = new ThemedColor(4, 600);

    /** The colour Office draws Blue, Accent 1, Lighter 60% in. */
    private static final short[] LIGHT_BLUE_RGB = {0xB4, 0xC6, 0xE7};

    /** Black, Text 1, Lighter 50%. */
    private static final ThemedColor GREY = new ThemedColor(1, 500);

    @TempDir
    Path dir;

    @Test
    void fillsACellOfAWorkbookOfAThemeWithTheThemeColour() throws IOException {
        var grid = gridOf(themed(new XSSFWorkbook()), "themed.xlsx");

        grid.setCellFillColor(0, 0, LIGHT_BLUE);

        var style = (XSSFCellStyle) grid.getSheetToWrite().getRow(0).getCell(0).getCellStyle();
        assertEquals(FillPatternType.SOLID_FOREGROUND, style.getFillPattern());
        assertEquals(LIGHT_BLUE, ThemedColor.of(style.getFillForegroundColorColor()));
    }

    @Test
    void coloursTheTextOfACellOfAWorkbookOfAThemeWithTheThemeColourAndKeepsItsFont() throws IOException {
        var workbook = themed(new XSSFWorkbook());
        var bold = workbook.createFont();
        bold.setBold(true);
        var style = workbook.createCellStyle();
        style.setFont(bold);
        workbook.getSheetAt(0).getRow(0).getCell(0).setCellStyle(style);
        var grid = gridOf(workbook, "bold.xlsx");

        grid.setCellFontColor(0, 0, GREY);

        var font = (XSSFFont) grid.getSheetToWrite().getWorkbook()
                .getFontAt(grid.getSheetToWrite().getRow(0).getCell(0).getCellStyle().getFontIndex());
        assertEquals(GREY, ThemedColor.of(font.getXSSFColor()));
        assertTrue(font.getBold(), "Only the colour of the font changes");
    }

    @Test
    void writesTheColourOfficeDrawsIntoAWorkbookWithoutATheme() throws IOException {
        var grid = gridOf(sheeted(new XSSFWorkbook()), "plain.xlsx");

        grid.setCellFillColor(0, 0, LIGHT_BLUE);
        grid.setCellFontColor(0, 0, LIGHT_BLUE);

        var cell = grid.getSheetToWrite().getRow(0).getCell(0);
        var fill = (XSSFColor) cell.getCellStyle().getFillForegroundColorColor();
        assertFalse(fill.isThemed(), "No theme to draw the colour from");
        assertArrayEquals(LIGHT_BLUE_RGB, PoiExcelHelper.toRgb(fill));
        var font = (XSSFFont) grid.getSheetToWrite().getWorkbook().getFontAt(cell.getCellStyle().getFontIndex());
        assertArrayEquals(LIGHT_BLUE_RGB, PoiExcelHelper.toRgb(font.getXSSFColor()));
    }

    @Test
    void writesTheColourOfficeDrawsIntoAnXlsWorkbook() throws IOException {
        var grid = gridOf(sheeted(new HSSFWorkbook()), "old.xls");

        grid.setCellFillColor(0, 0, LIGHT_BLUE);
        grid.setCellFontColor(0, 0, LIGHT_BLUE);

        var workbook = (HSSFWorkbook) grid.getSheetToWrite().getWorkbook();
        var style = grid.getSheetToWrite().getRow(0).getCell(0).getCellStyle();
        assertArrayEquals(LIGHT_BLUE_RGB, PoiExcelHelper.toRgb(style.getFillForegroundColor(), workbook));
        var font = workbook.getFontAt(style.getFontIndex());
        assertArrayEquals(LIGHT_BLUE_RGB, PoiExcelHelper.toRgb(font.getColor(), workbook));
    }

    @Test
    void tellsAWorkbookOfAThemeFromOneWithout() throws IOException {
        try (var plain = new XSSFWorkbook(); var themed = themed(new XSSFWorkbook()); var old = new HSSFWorkbook()) {
            assertFalse(PoiExcelHelper.hasTheme(plain));
            assertTrue(PoiExcelHelper.hasTheme(themed));
            assertFalse(PoiExcelHelper.hasTheme(old));
        }
    }

    /** A workbook with the theme of Office and a cell to colour. */
    private static XSSFWorkbook themed(XSSFWorkbook workbook) throws IOException {
        var document = ThemeDocument.Factory.newInstance();
        var scheme = document.addNewTheme().addNewThemeElements().addNewClrScheme();
        scheme.setName("Office");
        scheme.addNewDk1().addNewSrgbClr().setVal(rgb(0x000000));
        scheme.addNewLt1().addNewSrgbClr().setVal(rgb(0xFFFFFF));
        scheme.addNewDk2().addNewSrgbClr().setVal(rgb(0x44546A));
        scheme.addNewLt2().addNewSrgbClr().setVal(rgb(0xE7E6E6));
        scheme.addNewAccent1().addNewSrgbClr().setVal(rgb(0x4472C4));
        scheme.addNewAccent2().addNewSrgbClr().setVal(rgb(0xED7D31));
        scheme.addNewAccent3().addNewSrgbClr().setVal(rgb(0xA5A5A5));
        scheme.addNewAccent4().addNewSrgbClr().setVal(rgb(0xFFC000));
        scheme.addNewAccent5().addNewSrgbClr().setVal(rgb(0x5B9BD5));
        scheme.addNewAccent6().addNewSrgbClr().setVal(rgb(0x70AD47));
        scheme.addNewHlink().addNewSrgbClr().setVal(rgb(0x0563C1));
        scheme.addNewFolHlink().addNewSrgbClr().setVal(rgb(0x954F72));
        var styles = workbook.getStylesSource();
        styles.ensureThemesTable();
        styles.getTheme().readFrom(document.newInputStream());
        return sheeted(workbook);
    }

    private static byte[] rgb(int colour) {
        return new byte[]{(byte) (colour >> 16), (byte) (colour >> 8), (byte) colour};
    }

    /** A workbook with a cell to colour. */
    private static <W extends Workbook> W sheeted(W workbook) {
        workbook.createSheet("Sheet").createRow(0).createCell(0).setCellValue("Colour");
        return workbook;
    }

    /** The grid of the first sheet of a workbook, as read from its file. */
    private XlsSheetGridModel gridOf(Workbook workbook, String name) throws IOException {
        var file = dir.resolve(name);
        try (workbook; var out = Files.newOutputStream(file)) {
            workbook.write(out);
        }
        var source = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(file.toUri().toURL()));
        return new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, source));
    }
}
