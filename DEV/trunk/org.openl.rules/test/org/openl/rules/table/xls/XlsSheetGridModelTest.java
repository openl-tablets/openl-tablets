package org.openl.rules.table.xls;

import static org.junit.Assert.*;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.IGridRegion;
import org.openl.source.impl.FileSourceCodeModule;

public class XlsSheetGridModelTest {
    
    private static XlsSheetGridModel xsGrid;
    
    @Before
    public void before() {
        FileSourceCodeModule source = new FileSourceCodeModule("./test/rules/XlsSheetGridModelTest.xls", null);
        XlsWorkbookSourceCodeModule wbSrc = new XlsWorkbookSourceCodeModule(source);

        Workbook wb = wbSrc.getWorkbook();

        Sheet sheet = wb.getSheetAt(0);
        String name = wb.getSheetName(0);

        XlsSheetSourceCodeModule sheetSrc = new XlsSheetSourceCodeModule(sheet, name, wbSrc);

        xsGrid = new XlsSheetGridModel(sheetSrc);
    }

    @Test
    public void testConversions() {
        _testCell("A1", 0, 0);
        _testCell("AA1", 26, 0);
        _testCell("AB1", 27, 0);
    }

    private void _testCell(String cell, int col, int row) {
        Assert.assertEquals(XlsSheetGridModel.getColumn(cell), col);
        Assert.assertEquals(XlsSheetGridModel.getRow(cell), row);
        
        Assert.assertEquals(cell, XlsUtil.xlsCellPresentation(col, row));   
    }
    
    @Test
    public void testCellsFromMergedRegions() {
        XlsCell cell = xsGrid.getCell(2, 2);        
        assertEquals("Rules void hello1(int hour)", cell.getStringValue());
        
        XlsCell cell1 = xsGrid.getCell(4, 2);        
        assertEquals("Rules void hello1(int hour)", cell1.getStringValue());
        
        XlsCell cell2 = xsGrid.getCell(2, 8);        
        assertEquals("R20", cell2.getStringValue());
        
        XlsCell cell3 = xsGrid.getCell(2, 9);        
        assertEquals("R20", cell3.getStringValue());
    }
    
    @Test
    public void testMergedRegionsNumber() {
        int mergedRegions = xsGrid.getNumberOfMergedRegions();
        assertEquals(13, mergedRegions);
    }
    
    @Test
    public void testColumnIndexes() {
        int maxColumnIndex = xsGrid.getMaxColumnIndex(2);
        assertEquals(8, maxColumnIndex);
        
        int minColumnIndex = xsGrid.getMinColumnIndex(2);
        assertEquals(1, minColumnIndex);
    }
    
    @Test
    public void testGetRegion() {
        assertTrue(xsGrid.isPartOfTheMergedRegion(3, 2));
        IGridRegion gridRegion = xsGrid.getRegionContaining(3, 2);
        
        assertEquals(2,gridRegion.getTop());
        assertEquals(2,gridRegion.getBottom());
        
        assertEquals(2,gridRegion.getLeft());
        assertEquals(7,gridRegion.getRight());
    }
    
    @Test
    public void testIsEmpty() {
        assertTrue(xsGrid.isEmpty(4, 12)); // trully empty cell
        
        assertTrue(xsGrid.isEmpty(4, 11)); // second cell from the merged region. 
                                           // is it right behaviour?
        
        assertFalse(xsGrid.isEmpty(3, 11));// trully not emty cell.
    }

}
