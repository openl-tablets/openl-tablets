package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;
import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class TestAutoType2 {
    public interface ITestCalc {
        SpreadsheetResult calc3();
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/calc/autotype/autotype-2.xls");
        TestHelper<ITestCalc> testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc3();

        assertEquals(2, result.getHeight());
        assertEquals(4, result.getWidth());

        assertEquals("Col1", result.getColumnName(0));
        assertEquals("Col2", result.getColumnName(1));
        assertEquals("Col3", result.getColumnName(2));

        assertEquals("Row1", result.getRowName(0));
        assertEquals("Row2", result.getRowName(1));

        assertEquals("G", result.getValue(0, 0).toString());
        assertEquals("H", result.getValue(0, 1).toString());
        // assertEquals("C", result.getValue(0, 2).toString());

        // TODO consider update implementation
        // nulls if no row/column has no Name is not right
        // at least let them be String(s)

        assertEquals(30, result.getValue(1, 2));
        assertEquals(34, result.getValue(1, 3));

    }
}
