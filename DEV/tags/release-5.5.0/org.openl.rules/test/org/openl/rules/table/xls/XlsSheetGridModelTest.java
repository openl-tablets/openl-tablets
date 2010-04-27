package org.openl.rules.table.xls;

import org.junit.Assert;
import org.junit.Test;

public class XlsSheetGridModelTest {

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
    
    

}
