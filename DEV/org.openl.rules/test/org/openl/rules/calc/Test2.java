package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

public class Test2 {
    @Test
    public void test1() {
        ITestCalc test = TestUtils.create("test/rules/calc1/calc2-1.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc2();

        assertEquals(3, result.getHeight());
        assertEquals(2, result.getWidth());

        assertEquals("Col1", result.getColumnName(0));

        assertEquals("Col2", result.getColumnName(1));

        assertEquals("Row1", result.getRowName(0));

        assertEquals("Row2", result.getRowName(1));

        assertEquals("Row3", result.getRowName(2));

        assertEquals("A", result.getValue(0, 0));

        assertEquals("C", result.getValue(0, 1));

        assertEquals("G", result.getValue(1, 0));
        assertEquals("I", result.getValue(1, 1));

        assertEquals(0, result.getValue(2, 0));
        assertEquals(0, result.getValue(2, 1));
    }

    public interface ITestCalc {
        SpreadsheetResult calc2();
    }

}
