package org.openl.excel.parser.sax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.ExcelReaderFactory;
import org.openl.excel.parser.FolderUtils;
import org.openl.excel.parser.MergedCell;
import org.openl.excel.parser.SheetDescriptor;

public class SheetOptimizationTest {
    @Test
    public void readFileWithBigSheets() {
        ExcelReader reader = ExcelReaderFactory.sequentialFactory()
            .create(FolderUtils.getResourcesFolder() + "big-sheet.xlsx");
        List<? extends SheetDescriptor> sheets = reader.getSheets();
        assertEquals(1, sheets.size());

        Object[][] firstSheet = reader.getCells(sheets.get(0));
        assertEquals(3, firstSheet.length);
        assertEquals(2, firstSheet[0].length);
        assertEquals("Environment", firstSheet[0][0]);
        assertEquals(MergedCell.MERGE_WITH_LEFT, firstSheet[0][1]);
        assertEquals("dependency", firstSheet[1][0]);
        assertEquals("datatypes", firstSheet[1][1]);
    }

    @Test
    public void mergedCells() {
        ExcelReader reader = ExcelReaderFactory.sequentialFactory()
            .create(FolderUtils.getResourcesFolder() + "sheet-optimization.xlsx");
        List<? extends SheetDescriptor> sheets = reader.getSheets();
        assertEquals(3, sheets.size());

        Object[][] sheet1 = reader.getCells(sheets.get(0));
        assertEquals(18, sheet1.length);
        assertEquals(6, sheet1[0].length);

        Object[][] sheet2 = reader.getCells(sheets.get(1));
        assertEquals(0, sheet2.length);

        Object[][] sheet3 = reader.getCells(sheets.get(2));
        assertEquals(10, sheet3.length);
        assertEquals(20, sheet3[0].length);
        assertEquals("S3", sheet3[5][17]);
        assertEquals(MergedCell.MERGE_WITH_LEFT, sheet3[5][19]);
        assertNull(sheet3[9][19]);
    }
}
