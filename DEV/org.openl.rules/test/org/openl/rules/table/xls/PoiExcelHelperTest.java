package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FillPatternType;
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
}
