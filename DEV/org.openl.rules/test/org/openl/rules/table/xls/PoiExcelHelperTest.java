package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class PoiExcelHelperTest {

    private static final short[] WHITE = {255, 255, 255};
    private static final short[] DARK_BLUE = {31, 78, 120};

    @Test
    void createsTheCellAndItsRowOnceAndReusesThemAfterwards() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var sheet = workbook.createSheet();

            var cell = PoiExcelHelper.getOrCreateCell(2, 3, sheet);

            assertEquals(3, cell.getRowIndex());
            assertEquals(2, cell.getColumnIndex());
            assertSame(cell, PoiExcelHelper.getOrCreateCell(2, 3, sheet));
        }
    }

    @Test
    void clonesTheStyleOfACellIntoANewOne() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var style = workbook.createCellStyle();
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);

            var copy = PoiExcelHelper.cloneStyleFrom(cell);

            assertNotSame(style, copy);
            assertEquals(FillPatternType.SOLID_FOREGROUND, copy.getFillPattern());
        }
    }

    @Test
    void changesOneFontSettingAndKeepsTheOthers() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var font = workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 14);
            var style = workbook.createCellStyle();
            style.setFont(font);
            cell.setCellStyle(style);

            PoiExcelHelper.setCellFontBold(cell, true);
            PoiExcelHelper.setCellFontItalic(cell, true);
            PoiExcelHelper.setCellFontUnderline(cell, Font.U_SINGLE);

            var changed = PoiExcelHelper.getCellFont(cell);
            assertTrue(changed.getBold());
            assertTrue(changed.getItalic());
            assertEquals(Font.U_SINGLE, changed.getUnderline());
            assertEquals("Arial", changed.getFontName());
            assertEquals(14, changed.getFontHeightInPoints());
            assertFalse(font.getBold());
        }
    }

    @Test
    void reusesTheWorkbookFontWithTheSameSettings() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            var first = row.createCell(0);
            var second = row.createCell(1);

            PoiExcelHelper.setCellFontBold(first, true);
            PoiExcelHelper.setCellFontBold(second, true);

            assertEquals(first.getCellStyle().getFontIndex(), second.getCellStyle().getFontIndex());
        }
    }

    @Test
    void keepsTheColourOfTheTextWhenAnXlsxFontOfAnotherColourHasTheSameSettings() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            // A white bold font, as the dark header of a workbook has, of the same face as the font of the cell.
            fontOf(workbook, WHITE).setBold(true);
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var style = workbook.createCellStyle();
            style.setFont(fontOf(workbook, DARK_BLUE));
            cell.setCellStyle(style);

            PoiExcelHelper.setCellFontBold(cell, true);

            var bold = PoiExcelHelper.getCellFont(cell);
            assertTrue(bold.getBold());
            assertArrayEquals(DARK_BLUE, PoiExcelHelper.getFontColor(bold, workbook));
        }
    }

    @Test
    void reusesTheXlsxFontOfTheSameColour() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var style = workbook.createCellStyle();
            style.setFont(fontOf(workbook, DARK_BLUE));
            var row = workbook.createSheet().createRow(0);
            var first = row.createCell(0);
            var second = row.createCell(1);
            first.setCellStyle(style);
            second.setCellStyle(style);
            var fonts = workbook.getNumberOfFonts();

            PoiExcelHelper.setCellFontBold(first, true);
            PoiExcelHelper.setCellFontBold(second, true);

            assertEquals(first.getCellStyle().getFontIndex(), second.getCellStyle().getFontIndex());
            assertEquals(fonts + 1, workbook.getNumberOfFonts());
        }
    }

    @Test
    void listsTheFontsOfAnXlsWorkbookPastTheFontItHasNot() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var added = workbook.createFont();
            var last = workbook.createFont();

            var indexes = PoiExcelHelper.getFonts(workbook).stream().map(Font::getIndex).toList();

            // An .xls workbook has no font 4: the fonts it adds are numbered past it.
            assertEquals(workbook.getNumberOfFonts(), indexes.size());
            assertFalse(indexes.contains(4));
            assertTrue(indexes.containsAll(List.of(added.getIndex(), last.getIndex())));
        }
    }

    @Test
    void readsTheColourOfAnXlsxFontWithoutWritingIntoTheFont() throws IOException {
        try (var workbook = themedWorkbook()) {
            // White, Background 1: a font coloured by the theme alone, with no RGB of its own.
            var font = workbook.createFont();
            var written = font.getCTFont().addNewColor();
            written.setTheme(0);

            var read = PoiExcelHelper.getFontColor(font, workbook);
            var attributes = PoiExcelHelper.FontAttributes.of(font, workbook);

            assertArrayEquals(WHITE, read);
            assertEquals(0xFFFFFF, attributes.color());
            // POI writes the RGB of a theme colour into the colour it reads, so a saved workbook would keep it.
            assertFalse(written.isSetRgb(), "Reading the colour writes nothing into the font");
            // A font read without its workbook, as the SAX reader reads one, resolves its theme colour itself.
            assertArrayEquals(read, PoiExcelHelper.getFontColor(font, null));
        }
    }

    /** A workbook whose theme holds the first background and text as Excel writes them: the colours of a window. */
    private static XSSFWorkbook themedWorkbook() throws IOException {
        var workbook = new XSSFWorkbook();
        workbook.getStylesSource().ensureThemesTable();
        workbook.getStylesSource().getTheme().readFrom(new ByteArrayInputStream("""
                <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Test">
                  <a:themeElements>
                    <a:clrScheme name="Test">
                      <a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1>
                      <a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1>
                    </a:clrScheme>
                  </a:themeElements>
                </a:theme>
                """.getBytes(StandardCharsets.UTF_8)));
        return workbook;
    }

    /** A Calibri font of 11 points in the given colour. */
    private static Font fontOf(XSSFWorkbook workbook, short[] color) {
        var font = workbook.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        PoiExcelHelper.setFontColor(font, color, workbook);
        return font;
    }

    @Test
    void ignoresAMissingCell() {
        PoiExcelHelper.setCellFontBold(null, true);
        PoiExcelHelper.setCellFontItalic(null, true);
        PoiExcelHelper.setCellFontUnderline(null, Font.U_SINGLE);
        assertNull(PoiExcelHelper.getCellFont(null));
    }
}
