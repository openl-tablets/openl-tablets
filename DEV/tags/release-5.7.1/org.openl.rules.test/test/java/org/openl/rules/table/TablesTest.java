/*
 * Created on Oct 29, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import java.net.URL;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.XlsUtil;
import org.openl.source.impl.FileSourceCodeModule;

/**
 * @author snshor
 * 
 */
public class TablesTest extends TestCase {

    public void testSplitter() throws Exception {

        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2.xls");

        FileSourceCodeModule source = new FileSourceCodeModule(url.getPath(), null);
        XlsWorkbookSourceCodeModule wbSrc = new XlsWorkbookSourceCodeModule(source);

        Workbook wb = wbSrc.getWorkbook();

        int nsheets = wb.getNumberOfSheets();

        for (int i = 0; i < nsheets; i++) {

            Sheet sheet = wb.getSheetAt(i);
            String name = wb.getSheetName(i);
            
            XlsSheetSourceCodeModule sheetSrc = new XlsSheetSourceCodeModule(sheet, name, wbSrc);

            XlsSheetGridModel xsGrid = new XlsSheetGridModel(sheetSrc);

            IGridTable[] tables = new GridSplitter(xsGrid).split();

            if (name.equals("ABC")) {
                for (int j = 0; j < tables.length; j++) {
                    System.out.println(XlsUtil.xlsCellPresentation(tables[j].getGridColumn(0, 0), tables[j].getGridRow(
                            0, 0)));
                }

                Assert.assertEquals(12, xsGrid.getNumberOfMergedRegions());
                Assert.assertEquals(6, tables.length);

                ILogicalTable lt = LogicalTableHelper.logicalTable(tables[5]);

                subtestRegion(lt.rows(1));

                Assert.assertEquals(6, lt.getLogicalHeight());
                Assert.assertEquals(1, lt.getLogicalWidth());

                ILogicalTable row1 = lt.getLogicalRow(0);

                Assert.assertEquals(1, row1.getLogicalHeight());
                Assert.assertEquals(1, row1.getLogicalWidth());

                Assert.assertEquals(2, row1.getGridTable().getLogicalHeight());
                Assert.assertEquals(4, row1.getGridTable().getLogicalWidth());

                ILogicalTable row2 = lt.getLogicalRow(1);

                Assert.assertEquals(1, row2.getLogicalHeight());
                Assert.assertEquals(2, row2.getLogicalWidth());

                Assert.assertEquals(2, row2.getGridTable().getLogicalHeight());
                Assert.assertEquals(4, row2.getGridTable().getLogicalWidth());

                ILogicalTable col22 = row2.columns(1, 1);

                Assert.assertEquals(2, col22.getLogicalHeight());
                Assert.assertEquals(1, col22.getLogicalWidth());

                Assert.assertEquals(2, col22.getGridTable().getLogicalHeight());
                Assert.assertEquals(3, col22.getGridTable().getLogicalWidth());

                ILogicalTable row222 = col22.rows(1, 1);

                Assert.assertEquals(1, row222.getLogicalHeight());
                Assert.assertEquals(3, row222.getLogicalWidth());

                ILogicalTable invRow2 = LogicalTableHelper.logicalTable(new TransposedGridTable(row2.getGridTable()));

                Assert.assertEquals(2, invRow2.getLogicalHeight());
                Assert.assertEquals(1, invRow2.getLogicalWidth());

                ILogicalTable invCol22 = invRow2.getLogicalRow(1);

                Assert.assertEquals(1, invCol22.getLogicalHeight());
                Assert.assertEquals(2, invCol22.getLogicalWidth());

                ILogicalTable invRow222 = invCol22.columns(1, 1);

                Assert.assertEquals(3, invRow222.getLogicalHeight());
                Assert.assertEquals(1, invRow222.getLogicalWidth());

            }
        }
    }

    private void subtestRegion(ILogicalTable testHeader1) throws Exception {
        
        ILogicalTable bb = testHeader1.getLogicalRegion(1, 0, 1, 1);

        Assert.assertEquals(2, bb.getLogicalHeight());
        Assert.assertEquals(5, testHeader1.getLogicalRegion(1, 3, 1, 2).getLogicalHeight());
        Assert.assertEquals(5, testHeader1.transpose().getLogicalRegion(3, 1, 2, 1).getLogicalWidth());
    }
}
