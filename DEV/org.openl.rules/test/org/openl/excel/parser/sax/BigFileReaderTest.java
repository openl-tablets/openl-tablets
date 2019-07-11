package org.openl.excel.parser.sax;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.ExcelReaderFactory;
import org.openl.excel.parser.FolderUtils;
import org.openl.excel.parser.MergedCell;
import org.openl.excel.parser.SheetDescriptor;

public class BigFileReaderTest {
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
}
