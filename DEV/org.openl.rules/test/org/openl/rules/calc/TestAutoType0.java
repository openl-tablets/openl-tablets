package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class TestAutoType0 {
    @Test
    public void test1() {
        ITestCalc test = TestUtils.create("test/rules/calc/autotype/autotype-0.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc3();

        assertEquals(4, result.getHeight());
        assertEquals(4, result.getWidth());

        assertEquals("Col1", result.getColumnName(0));
        assertEquals("Col2", result.getColumnName(1));
        assertEquals("Col3", result.getColumnName(2));
        assertEquals("Col4", result.getColumnName(3));

        assertEquals("Row1", result.getRowName(0));
        assertEquals("Row2", result.getRowName(1));
        assertEquals("Row3", result.getRowName(2));
        assertEquals("Row4", result.getRowName(3));

        assertEquals("A", result.getValue(0, 0));
        assertEquals("B", result.getValue(0, 1));
        assertEquals(1.0, result.getValue(0, 2));
        assertEquals(7.0, result.getValue(0, 3));

        assertEquals("D", result.getValue(1, 0));
        assertEquals("E", result.getValue(1, 1));
        assertEquals(7.0, result.getValue(1, 2));
        assertEquals(8.0, result.getValue(1, 3));

        assertEquals("G", result.getValue(2, 0));
        assertEquals("H", result.getValue(2, 1));
        assertEquals(9.0, result.getValue(2, 2));
        assertEquals(9.0, result.getValue(2, 3));

        assertEquals("G", result.getValue(3, 0));
        assertEquals("H", result.getValue(3, 1));
        assertEquals(17.0, result.getValue(3, 2));
        assertEquals(41.0, result.getValue(3, 3));

    }

    public interface ITestCalc {
        SpreadsheetResult calc3();
    }
}
