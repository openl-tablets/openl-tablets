package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class Test0 {
    @Test
    public void test1() {

        ITestCalc test = TestUtils.create("test/rules/calc0/calc0-1.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc();
        assertEquals(2, result.getHeight());
        assertEquals(3, result.getWidth());

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                Double a = y * 3.0 + x;
                Object r = result.getValue(y, x);
                assertEquals(a, r);
            }
        }
    }

    @Test
    public void test2() {
        TestUtils.assertEx("test/rules/calc0/calc0-2.xls", "Table has no body.");
    }

    @Test
    public void test3() {

        ITestCalc test = TestUtils.create("test/rules/calc0/calc0-3.xls", ITestCalc.class);
        SpreadsheetResult result = test.calc();
        assertEquals(0, result.getHeight());
        assertEquals(0, result.getWidth());
    }

    @Test
    public void test4() {
        TestUtils.assertEx("test/rules/calc0/calc0-4.xls", "Table has no body.", "merge header cell");
    }

    @Test
    public void test5() {
        TestUtils.assertEx("test/rules/calc0/calc0-5.xls", "'Col1' is already defined.", "cell=E4");
    }

    @Test
    public void test6() {
        TestUtils.assertEx("test/rules/calc0/calc0-6.xls", "'Row1' is already defined.", "cell=B7");
    }

    public interface ITestCalc {
        SpreadsheetResult calc();
    }
}
