package org.openl.rules.calc;

import static junit.framework.Assert.assertEquals;
import java.io.File;

import junit.framework.AssertionFailedError;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;

public class Test0 {
    @Test
    public void test1() {
        File xlsFile = new File("test/rules/calc0/calc0-1.xls");
        TestHelper<ITestCalc> testHelper;
        testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc();
        assertEquals(2, result.height());
        assertEquals(3, result.width());

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                // Note, implied type can be changed, for now it is DoubleValue
                DoubleValue i = new DoubleValue(y * 3 + x);
                Object r = result.getValue(y, x);
                if (i.compareTo((DoubleValue)r) != 0) {
                    throw new AssertionFailedError(String.format("<%s> != <%s>", i, r));
                }
            }
        }
    }

    @Test
    public void test2() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/calc0/calc0-2.xls");
                TestHelper<ITestCalc> testHelper;
                testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);
            }
        }, "Table has no body!");
    }

    @Test
    public void test3() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/calc0/calc0-3.xls");
                TestHelper<ITestCalc> testHelper;
                testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);
            }
        }, "Spreadsheet must have at least 2x2 cells!");
    }

    @Test
    public void test4() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/calc0/calc0-4.xls");
                TestHelper<ITestCalc> testHelper;
                testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);
            }
        }, "Table has no body!", "merge header cell");
    }

    interface ITestCalc {
        SpreadsheetResult calc();
    }
}
