package org.openl.rules.table;

import java.net.URL;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.FileSourceCodeModule;

public class LookupTest extends TestCase {

    public void testMergeBounds() throws Exception {

        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/TestLookup.xls");

        FileSourceCodeModule source = new FileSourceCodeModule(url.getPath(), null);
        XlsWorkbookSourceCodeModule wbsrc = new XlsWorkbookSourceCodeModule(source);
        Workbook wb = wbsrc.getWorkbook();

        int nsheets = wb.getNumberOfSheets();

        for (int i = 0; i < nsheets; i++) {

            Sheet sheet = wb.getSheetAt(i);
            String name = wb.getSheetName(i);

            XlsSheetSourceCodeModule sheetSrc = new XlsSheetSourceCodeModule(sheet, name, wbsrc);
            XlsSheetGridModel xsGrid = new XlsSheetGridModel(sheetSrc);

            IGridTable[] tables = new GridSplitter(xsGrid).split();

            if (name.equals("Sheet1")) {
                testSheet1(tables);
            }
        }
    }

    private void testSheet1(IGridTable[] tables) {
       
        Assert.assertEquals(2, tables.length);

        IGridTable lookupTable = OffSetGridTableHelper.offSetTable(tables[0]);
        IGridTable t1 = lookupTable.rows(1);
        IGridTable lookupRow1 = t1.getRow(0);
        IGridTable t2 = t1.rows(1);
        IGridTable lookupColumn1 = t2.getColumn(0);
        IGridTable body = OffSetGridTableHelper.mergeBounds(lookupColumn1, lookupRow1);

        Assert.assertEquals(5, body.getGridHeight());
        Assert.assertEquals(3, body.getGridWidth());

        Assert.assertEquals("1", body.getRegion(0, 0, 1, 1).getGridTable().getCell(0, 0).getStringValue());
        Assert.assertEquals("7", body.getRegion(0, 2, 1, 1).getGridTable().getCell(0, 0).getStringValue());
        Assert.assertEquals("15", body.getRegion(2, 4, 1, 1).getGridTable().getCell(0, 0).getStringValue());
    }
}
