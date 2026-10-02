package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.junit.jupiter.api.Test;

class PoiExcelHelperTest {

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
    void ignoresAMissingCell() {
        PoiExcelHelper.setCellFontBold(null, true);
        PoiExcelHelper.setCellFontItalic(null, true);
        PoiExcelHelper.setCellFontUnderline(null, Font.U_SINGLE);
        assertNull(PoiExcelHelper.getCellFont(null));
    }
}
