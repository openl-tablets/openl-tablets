package org.openl.rules.table.xls;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.source.impl.URLSourceCodeModule;

public class XlsCellTest {
    
    private static XlsSheetGridModel xsGrid;
    
    @Before
    public void before() {
        URLSourceCodeModule source = new URLSourceCodeModule("./test/rules/XlsCellTest.xls");
        XlsWorkbookSourceCodeModule wbSrc = new XlsWorkbookSourceCodeModule(source);

        XlsSheetSourceCodeModule sheetSrc = new XlsSheetSourceCodeModule(0, wbSrc);

        xsGrid = new XlsSheetGridModel(sheetSrc);
    }
    
    @Test
    public void testStringMergedCell() {
        ICell cell = xsGrid.getCell(1, 3);
        assertEquals(1, cell.getAbsoluteColumn());
        assertEquals(1, cell.getColumn());
        assertEquals(3, cell.getAbsoluteRow());
        assertEquals(3, cell.getRow());
        
        // test region methods.
        IGridRegion gridRegion = cell.getAbsoluteRegion();
        assertEquals(3, gridRegion.getTop());
        assertEquals(4, gridRegion.getBottom());
        
        assertEquals(1, gridRegion.getLeft());
        assertEquals(2, gridRegion.getRight());
        
        gridRegion = cell.getRegion();
        assertEquals(3, gridRegion.getTop());
        assertEquals(4, gridRegion.getBottom());
        
        assertEquals(1, gridRegion.getLeft());
        assertEquals(2, gridRegion.getRight());
        
        assertEquals(2, cell.getWidth());
        assertEquals(2, cell.getHeight());
        
        // test data.
        assertNull(cell.getFormula());
        
        assertTrue(cell.getNativeBoolean());
        
        assertTrue(cell.hasNativeType());
        assertEquals(IGrid.CELL_TYPE_STRING, cell.getNativeType());
        assertEquals(IGrid.CELL_TYPE_STRING, cell.getType());
        
        assertEquals("hello everybody!", cell.getStringValue());
        
        try {
            cell.getNativeDate();  
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true); // can`t get numeric values from string cell
        }
        try {
            cell.getNativeNumber();
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true); // can`t get numeric values from string cell
        }
        
        ICellFont font = cell.getFont();
        assertEquals("Arial", font.getName());
        
        Object objectValue = cell.getObjectValue();
        assertTrue(objectValue instanceof String);
        assertEquals("hello everybody!", objectValue.toString());
    }
    
    @Test
    public void testDoubleMergedCell() {
        ICell cell = xsGrid.getCell(5, 8);
        assertEquals(5, cell.getAbsoluteColumn());
        assertEquals(5, cell.getColumn());
        assertEquals(8, cell.getAbsoluteRow());
        assertEquals(8, cell.getRow());
        
        // test region methods.
        IGridRegion gridRegion = cell.getAbsoluteRegion();
        assertEquals(7, gridRegion.getTop());
        assertEquals(9, gridRegion.getBottom());
        
        assertEquals(4, gridRegion.getLeft());
        assertEquals(7, gridRegion.getRight());
        
        gridRegion = cell.getRegion();
        assertEquals(7, gridRegion.getTop());
        assertEquals(9, gridRegion.getBottom());
        
        assertEquals(4, gridRegion.getLeft());
        assertEquals(7, gridRegion.getRight());
        
        assertEquals(1, cell.getWidth());
        assertEquals(1, cell.getHeight());
        
        // test data.
        assertNull(cell.getFormula());
        
        assertTrue(cell.getNativeBoolean());
        
        assertTrue(cell.hasNativeType());
        assertEquals(IGrid.CELL_TYPE_BLANK, cell.getNativeType());
        assertEquals(IGrid.CELL_TYPE_NUMERIC, cell.getType());
        
        assertEquals("123.343", cell.getStringValue());
        
        Date dateValue = cell.getNativeDate();   
        assertNull(dateValue);
        
        double doubleVaue = cell.getNativeNumber();
        assertEquals(0.0, doubleVaue, 0.001); // the native value of this cell in excel is 0. 
        
        ICellFont font = cell.getFont();
        assertEquals("Arial", font.getName());
        
        Object objectValue = cell.getObjectValue();
        assertTrue(objectValue instanceof Double);
        assertEquals(123.343, (Double) objectValue, 0.001); // the value will be taken from the top left
                                                                           // cell from the region 
    }
}
