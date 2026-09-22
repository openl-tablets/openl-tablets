package org.openl.excel.parser.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EventReaderMergedRegionTest {

    @Test
    void mergedRegionAboveTheFirstCellIsSkipped(@TempDir Path dir) throws IOException {
        var file = dir.resolve("merged.xls");
        try (var workbook = new HSSFWorkbook()) {
            var sheet = workbook.createSheet("Main");
            sheet.createRow(2).createCell(1).setCellValue("x");
            // Excel records the used range as the sheet dimensions, POI records the range from A1.
            sheet.getSheet().setDimensions(2, (short) 1, 3, (short) 2);
            // The region starts two rows above and one column left of the only cell with a value.
            sheet.addMergedRegion(new CellRangeAddress(0, 3, 0, 0));
            try (var out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }

        var reader = new EventReader(file.toString());
        try {
            var cells = reader.getCells(reader.getSheets().getFirst());
            assertEquals(1, cells.length);
            assertEquals(1, cells[0].length);
            assertEquals("x", cells[0][0]);
        } finally {
            reader.close();
        }
    }
}
