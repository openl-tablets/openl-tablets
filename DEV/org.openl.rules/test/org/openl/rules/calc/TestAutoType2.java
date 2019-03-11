package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class TestAutoType2 {
    @Test
    public void test1() {
        ITestCalc test = TestUtils.create("test/rules/calc/autotype/autotype-2.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc3();

        assertEquals(2, result.getHeight());
        assertEquals(4, result.getWidth());

        assertEquals("Col1", result.getColumnName(0));
        assertEquals("Col2", result.getColumnName(1));
        assertEquals("Col3", result.getColumnName(2));
        assertEquals("Col4", result.getColumnName(3));

        assertEquals("Row1", result.getRowName(0));
        assertEquals("Row2", result.getRowName(1));

        assertEquals("G", result.getValue(0, 0));
        assertEquals("H", result.getValue(0, 1));
        assertEquals(9.0, result.getValue(0, 2));
        assertEquals(9.0, result.getValue(0, 3));

        assertEquals("G", result.getValue(1, 0));
        assertEquals(11, result.getValue(1, 1));
        assertEquals(30, result.getValue(1, 2));
        assertEquals(34, result.getValue(1, 3));

    }

    public interface ITestCalc {
        SpreadsheetResult calc3();
    }
}
