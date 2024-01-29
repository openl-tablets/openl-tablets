package org.openl.rules.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * @author snshor
 *
 */
public class TablesTest {

    @Test
    public void testSplitter() throws Exception {

        URLSourceCodeModule source = new URLSourceCodeModule("./test/rules/Test2.xls");
        XlsWorkbookSourceCodeModule wbSrc = new XlsWorkbookSourceCodeModule(source);

        Workbook wb = wbSrc.getWorkbook();

        int nsheets = wb.getNumberOfSheets();

        for (int i = 0; i < nsheets; i++) {

            XlsSheetSourceCodeModule sheetSrc = new XlsSheetSourceCodeModule(i, wbSrc);

            XlsSheetGridModel xsGrid = new XlsSheetGridModel(sheetSrc);

            IGridTable[] tables = xsGrid.getTables();

            assertEquals(17, xsGrid.getNumberOfMergedRegions());
            assertEquals(6, tables.length);

            ILogicalTable lt = LogicalTableHelper.logicalTable(tables[5]);

            subtestRegion(lt.getRows(1));

            assertEquals(6, lt.getHeight());
            assertEquals(1, lt.getWidth());

            ILogicalTable row1 = lt.getRow(0);

            assertEquals(1, row1.getHeight());
            assertEquals(1, row1.getWidth());

            assertEquals(2, row1.getSource().getHeight());
            assertEquals(4, row1.getSource().getWidth());

            ILogicalTable row2 = lt.getRow(1);

            assertEquals(1, row2.getHeight());
            assertEquals(2, row2.getWidth());

            assertEquals(2, row2.getSource().getHeight());
            assertEquals(4, row2.getSource().getWidth());

            ILogicalTable col22 = row2.getColumns(1, 1);

            assertEquals(2, col22.getHeight());
            assertEquals(1, col22.getWidth());

            assertEquals(2, col22.getSource().getHeight());
            assertEquals(3, col22.getSource().getWidth());

            ILogicalTable row222 = col22.getRows(1, 1);

            assertEquals(1, row222.getHeight());
            assertEquals(3, row222.getWidth());

            ILogicalTable invRow2 = LogicalTableHelper.logicalTable(new TransposedGridTable(row2.getSource()));

            assertEquals(2, invRow2.getHeight());
            assertEquals(1, invRow2.getWidth());

            ILogicalTable invCol22 = invRow2.getRow(1);

            assertEquals(1, invCol22.getHeight());
            assertEquals(2, invCol22.getWidth());

            ILogicalTable invRow222 = invCol22.getColumns(1, 1);

            assertEquals(3, invRow222.getHeight());
            assertEquals(1, invRow222.getWidth());

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

    private void subtestRegion(ILogicalTable testHeader1) throws Exception {

        ILogicalTable bb = testHeader1.getSubtable(1, 0, 1, 1);

        assertEquals(2, bb.getHeight());
        assertEquals(5, testHeader1.getSubtable(1, 3, 1, 2).getHeight());
        assertEquals(5, testHeader1.transpose().getSubtable(3, 1, 2, 1).getWidth());
    }
}
