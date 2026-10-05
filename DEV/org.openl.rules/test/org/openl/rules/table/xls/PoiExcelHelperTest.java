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
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    void holdsAColourTheXlsPaletteHasNoRoomForAsTheNearestOneItHas() throws IOException {
        try (var xls = new HSSFWorkbook(); var xlsx = new XSSFWorkbook()) {
            // The palette of an .xls workbook is full: a colour it lacks is held as the nearest one it has.
            var nearest = xls.getCustomPalette().findSimilarColor(0xB4, 0xC6, 0xE7).getTriplet();
            assertArrayEquals(nearest, PoiExcelHelper.toStoredRgb("#b4c6e7", xls));
            assertArrayEquals(new short[]{0x80, 0x80, 0x80}, PoiExcelHelper.toStoredRgb("#808080", xls));
            // An .xlsx workbook holds every colour as it is.
            assertArrayEquals(new short[]{0xB4, 0xC6, 0xE7}, PoiExcelHelper.toStoredRgb("#b4c6e7", xlsx));
            assertEquals(0xB4C6E7, PoiExcelHelper.toRgbValue(new short[]{0xB4, 0xC6, 0xE7}));
        }
    }

    /**
     * The colours the palette of Excel offers for the theme colours Office 2013 - 2022, each with the tint Excel writes
     * for it: a colour made lighter or darker draws as Excel draws it.
     */
    @ParameterizedTest(name = "{0} {1} gives {2}")
    @CsvSource({
            // White, Background 1, and Black, Text 1: darker and lighter only.
            "#ffffff, -0.0499893185216834, #f2f2f2", "#ffffff, -0.149998474074526, #d9d9d9",
            "#ffffff, -0.249977111117893, #bfbfbf", "#ffffff, -0.349986266670736, #a6a6a6",
            "#ffffff, -0.499984740745262, #808080",
            "#000000, 0.499984740745262, #808080", "#000000, 0.349986266670736, #595959",
            "#000000, 0.249977111117893, #404040", "#000000, 0.149998474074526, #262626",
            "#000000, 0.0499893185216834, #0d0d0d",
            // Background 2.
            "#e7e6e6, -0.0999786370433668, #d0cece", "#e7e6e6, -0.249977111117893, #aeaaaa",
            "#e7e6e6, -0.499984740745262, #757171", "#e7e6e6, -0.749992370372631, #3a3838",
            "#e7e6e6, -0.899990844447157, #161616",
            // Text 2 and the accents: Lighter 80%, 60% and 40%, then Darker 25% and 50%.
            "#44546a, 0.799981688894314, #d6dce4", "#44546a, 0.599993896298105, #acb9ca",
            "#44546a, 0.399975585192419, #8497b0", "#44546a, -0.249977111117893, #333f4f",
            "#44546a, -0.499984740745262, #222b35",
            "#4472c4, 0.799981688894314, #d9e1f2", "#4472c4, 0.599993896298105, #b4c6e7",
            "#4472c4, 0.399975585192419, #8ea9db", "#4472c4, -0.249977111117893, #305496",
            "#4472c4, -0.499984740745262, #203764",
            "#ed7d31, 0.799981688894314, #fce4d6", "#ed7d31, 0.599993896298105, #f8cbad",
            "#ed7d31, 0.399975585192419, #f4b084", "#ed7d31, -0.249977111117893, #c65911",
            "#ed7d31, -0.499984740745262, #833c0c",
            "#a5a5a5, 0.799981688894314, #ededed", "#a5a5a5, 0.599993896298105, #dbdbdb",
            "#a5a5a5, 0.399975585192419, #c9c9c9", "#a5a5a5, -0.249977111117893, #7b7b7b",
            "#a5a5a5, -0.499984740745262, #525252",
            "#ffc000, 0.799981688894314, #fff2cc", "#ffc000, 0.599993896298105, #ffe699",
            "#ffc000, 0.399975585192419, #ffd966", "#ffc000, -0.249977111117893, #bf8f00",
            "#ffc000, -0.499984740745262, #806000",
            "#5b9bd5, 0.799981688894314, #ddebf7", "#5b9bd5, 0.599993896298105, #bdd7ee",
            "#5b9bd5, 0.399975585192419, #9bc2e6", "#5b9bd5, -0.249977111117893, #2f75b5",
            "#5b9bd5, -0.499984740745262, #1f4e78",
            "#70ad47, 0.799981688894314, #e2efda", "#70ad47, 0.599993896298105, #c6e0b4",
            "#70ad47, 0.399975585192419, #a9d08e", "#70ad47, -0.249977111117893, #548235",
            "#70ad47, -0.499984740745262, #375623"})
    void tintsAColourAsExcelDrawsIt(String colour, double tint, String drawn) {
        assertArrayEquals(PoiExcelHelper.toRgb(drawn), PoiExcelHelper.applyTint(PoiExcelHelper.toRgb(colour), tint));
    }

    @Test
    void keepsAColourATintOfLessThanAThousandthLeavesAsItIs() {
        var accent = PoiExcelHelper.toRgb("#4472c4");

        assertArrayEquals(accent, PoiExcelHelper.applyTint(accent, 0));
        assertArrayEquals(accent, PoiExcelHelper.applyTint(accent, 0.0004));
        // A whole tint makes white and black.
        assertArrayEquals(WHITE, PoiExcelHelper.applyTint(accent, 1));
        assertArrayEquals(new short[]{0, 0, 0}, PoiExcelHelper.applyTint(accent, -1));
    }

    @Test
    void readsATintedColourOfAnXlsxCellAsExcelDrawsIt() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var color = new XSSFColor(workbook.getStylesSource().getIndexedColors());
            color.setRGB(new byte[]{0x5B, (byte) 0x9B, (byte) 0xD5});
            color.setTint(0.79998168889431442);
            var style = workbook.createCellStyle();
            style.setFillForegroundColor(color);

            var drawn = PoiExcelHelper.toRgb(style.getFillForegroundColorColor());

            assertArrayEquals(PoiExcelHelper.toRgb("#ddebf7"), drawn);
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
