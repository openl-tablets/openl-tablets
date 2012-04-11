package org.openl.rules.table;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.FileSourceCodeModule;


public class SimpleLogicalTableTest {
    
    private XlsSheetSourceCodeModule getXlsGrid() {
        FileSourceCodeModule source = new FileSourceCodeModule("./test/rules/SimpleLogicalTableTest.xls", null);
        XlsWorkbookSourceCodeModule wbSrc = new XlsWorkbookSourceCodeModule(source);

        Workbook wb = wbSrc.getWorkbook();

        Sheet sheet = wb.getSheetAt(0);
        String name = wb.getSheetName(0);

        XlsSheetSourceCodeModule sheetSrc = new XlsSheetSourceCodeModule(sheet, name, wbSrc);
        return sheetSrc;
    }
    
    @Test
    public void testSimpleLogicalTable() throws Exception {

        XlsSheetSourceCodeModule sheetSrc = getXlsGrid();

        XlsSheetGridModel xsGrid = new XlsSheetGridModel(sheetSrc);

        IGridTable[] tables = xsGrid.getTables();

        Assert.assertEquals(2, xsGrid.getNumberOfMergedRegions());
        Assert.assertEquals(1, tables.length);

        ILogicalTable logicalTable = LogicalTableHelper.logicalTable(tables[0]);
        
        assertTrue(logicalTable instanceof SimpleLogicalTable);
        
        assertEquals(5, logicalTable.getWidth());
        
        ILogicalTable subTable = logicalTable.getSubtable(0, 1, logicalTable.getWidth(), logicalTable.getHeight()-1);
        
        assertTrue(subTable instanceof SimpleLogicalTable);
        
        // this is not correct behaviour for ILogicalTable see SimpleLogicalTable docs for info
        assertEquals(5, subTable.getWidth());
    }

}
