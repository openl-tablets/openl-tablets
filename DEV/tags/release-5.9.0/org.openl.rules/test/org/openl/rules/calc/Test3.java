package org.openl.rules.calc;

import static junit.framework.Assert.assertEquals;
import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class Test3 {
    interface ITestCalc {
        SpreadsheetResult calc3();
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/calc1/calc3-1.xls");
        TestHelper<ITestCalc> testHelper;
        testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc3();

        assertEquals(4, result.getHeight());
        assertEquals(4, result.getWidth());

        assertEquals("Col1", result.getColumnName(0));
        assertEquals("Col2", result.getColumnName(1));
        assertEquals("Col3", result.getColumnName(2));

        assertEquals("Row1", result.getRowName(0));
        assertEquals("Row2", result.getRowName(1));
        assertEquals("Row3", result.getRowName(2));

        assertEquals("A", result.getValue(0, 0).toString());
        assertEquals("B", result.getValue(0, 1).toString());
//        assertEquals("C", result.getValue(0, 2).toString());

        // TODO consider update implementation
        // nulls if no row/column has no Name is not right
        // at least let them be String(s)
        assertEquals("D", result.getValue(1, 0).toString());
    
        assertEquals("17.0", result.getValue(3, 2).toString());
        assertEquals("41.0", result.getValue(3, 3).toString());

    }
}
