package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class Test2 {
    @Test
    public void test1() {
        ITestCalc test = TestUtils.create("test/rules/calc1/calc2-1.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc2();

        assertEquals(5, result.getHeight());
        assertEquals(3, result.getWidth());

        assertEquals("Col1", result.getColumnName(0));
        assertNull(result.getColumnName(1));
        assertEquals("Col2", result.getColumnName(2));

        assertEquals("Row1", result.getRowName(0));
        assertNull(result.getRowName(1));
        assertEquals("Row2", result.getRowName(2));
        assertNull(result.getRowName(3));
        assertEquals("Row3", result.getRowName(4));

        assertEquals("A", result.getValue(0, 0));
        assertNull(result.getValue(0, 1));// ?
        assertEquals("C", result.getValue(0, 2));

        // TODO consider update implementation
        // nulls if no row/column has no Name is not right
        // at least let them be String(s)
        assertNull(result.getValue(1, 0));
        assertNull(result.getValue(1, 1));
        assertNull(result.getValue(1, 2));
        assertNull(result.getValue(3, 0));
        assertNull(result.getValue(3, 1));
        assertNull(result.getValue(3, 2));

        assertEquals("G", result.getValue(2, 0));
        assertNull(result.getValue(2, 1));
        assertEquals("I", result.getValue(2, 2));

        assertEquals(0.0, result.getValue(4, 0));
        assertNull(result.getValue(4, 1));
        assertEquals(0.0, result.getValue(4, 2));
    }

    public interface ITestCalc {
        SpreadsheetResult calc2();
    }

}
