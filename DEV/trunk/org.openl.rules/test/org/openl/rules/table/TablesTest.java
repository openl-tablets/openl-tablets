package org.openl.rules.table;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.FileSourceCodeModule;

/**
 * @author snshor
 * 
 */
public class TablesTest extends TestCase {

    public void testSplitter() throws Exception {

        FileSourceCodeModule source = new FileSourceCodeModule("./test/rules/Test2.xls", null);
        XlsWorkbookSourceCodeModule wbSrc = new XlsWorkbookSourceCodeModule(source);

        Workbook wb = wbSrc.getWorkbook();

        int nsheets = wb.getNumberOfSheets();

        for (int i = 0; i < nsheets; i++) {

            Sheet sheet = wb.getSheetAt(i);
            String name = wb.getSheetName(i);

            XlsSheetSourceCodeModule sheetSrc = new XlsSheetSourceCodeModule(sheet, name, wbSrc);

            XlsSheetGridModel xsGrid = new XlsSheetGridModel(sheetSrc);

            IGridTable[] tables = new GridSplitter(xsGrid).split();

            Assert.assertEquals(17, xsGrid.getNumberOfMergedRegions());
            Assert.assertEquals(6, tables.length);

            IGridTable lt = LogicalTableHelper.logicalTable(tables[5]);

            subtestRegion(lt.rows(1));

            Assert.assertEquals(6, lt.getGridHeight());
            Assert.assertEquals(1, lt.getGridWidth());

            IGridTable row1 = lt.getRow(0);

            Assert.assertEquals(1, row1.getGridHeight());
            Assert.assertEquals(1, row1.getGridWidth());

            Assert.assertEquals(2, row1.getGridTable().getGridHeight());
            Assert.assertEquals(4, row1.getGridTable().getGridWidth());

            IGridTable row2 = lt.getRow(1);

            Assert.assertEquals(1, row2.getGridHeight());
            Assert.assertEquals(2, row2.getGridWidth());

            Assert.assertEquals(2, row2.getGridTable().getGridHeight());
            Assert.assertEquals(4, row2.getGridTable().getGridWidth());

            IGridTable col22 = row2.columns(1, 1);

            Assert.assertEquals(2, col22.getGridHeight());
            Assert.assertEquals(1, col22.getGridWidth());

            Assert.assertEquals(2, col22.getGridTable().getGridHeight());
            Assert.assertEquals(3, col22.getGridTable().getGridWidth());

            IGridTable row222 = col22.rows(1, 1);

            Assert.assertEquals(1, row222.getGridHeight());
            Assert.assertEquals(3, row222.getGridWidth());

            IGridTable invRow2 = LogicalTableHelper.logicalTable(new TransposedGridTable(row2.getGridTable()));

            Assert.assertEquals(2, invRow2.getGridHeight());
            Assert.assertEquals(1, invRow2.getGridWidth());

            IGridTable invCol22 = invRow2.getRow(1);

            Assert.assertEquals(1, invCol22.getGridHeight());
            Assert.assertEquals(2, invCol22.getGridWidth());

            IGridTable invRow222 = invCol22.columns(1, 1);

            Assert.assertEquals(3, invRow222.getGridHeight());
            Assert.assertEquals(1, invRow222.getGridWidth());

            assertEquals(2, tables[0].getRegion().getRight());
            assertEquals(4, tables[0].getRegion().getBottom());

            assertEquals(7, tables[1].getRegion().getTop());
            assertEquals(1, tables[1].getRegion().getLeft());

            assertEquals(28, tables[3].getRegion().getBottom());
            assertEquals(4, tables[3].getRegion().getRight());

            assertEquals(35, tables[4].getRegion().getBottom());
            assertEquals(1, tables[4].getRegion().getLeft());
        }
    }

    private void subtestRegion(IGridTable testHeader1) throws Exception {
        
        IGridTable bb = testHeader1.getRegion(1, 0, 1, 1);

        Assert.assertEquals(2, bb.getGridHeight());
        Assert.assertEquals(5, testHeader1.getRegion(1, 3, 1, 2).getGridHeight());
        Assert.assertEquals(5, testHeader1.transpose().getRegion(3, 1, 2, 1).getGridWidth());
    }
}
