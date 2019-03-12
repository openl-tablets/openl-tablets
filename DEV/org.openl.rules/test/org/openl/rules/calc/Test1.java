package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class Test1 {
    @Test
    public void test1() {
        ITestCalc test = TestUtils.create("test/rules/calc1/calc.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc1(10, 20);

        assertEquals(10, result.getValue(0, 0));
        assertEquals(20, result.getValue(0, 1));
        assertEquals(30, result.getValue(0, 2));
    }

    @Test
    public void test2() {
        ITestCalc test = TestUtils.create("test/rules/calc1/calc.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc2(10, 20);

        assertEquals(10, result.getValue(0, 0));
        assertEquals(20, result.getValue(0, 1));
        assertEquals(30, result.getValue(0, 2));
    }

    @Test
    public void test3() {
        ITestCalc test = TestUtils.create("test/rules/calc1/calc.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc3(10, 20);

        assertEquals(10, result.getValue(0, 0));
        assertEquals(20L, result.getValue(0, 1));
        assertEquals(30.0, result.getValue(0, 2));

        assertEquals(11, result.getValue(1, 0));
        assertEquals(22L, result.getValue(1, 1));
        assertEquals(90.0, result.getValue(1, 2));
    }

    public interface ITestCalc {
        SpreadsheetResult calc1(int a, int b);
        SpreadsheetResult calc2(int a, int b);
        SpreadsheetResult calc3(int a, int b);
    }
}
