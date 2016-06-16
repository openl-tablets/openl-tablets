package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;

import java.io.File;

import junit.framework.AssertionFailedError;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;

public class Test0 {
    public interface ITestCalc {
        SpreadsheetResult calc();
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/calc0/calc0-1.xls");
        TestHelper<ITestCalc> testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc();
        assertEquals(2, result.getHeight());
        assertEquals(3, result.getWidth());

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                // Note, implied type can be changed, for now it is DoubleValue
                DoubleValue i = new DoubleValue(y * 3 + x);
                Object r = result.getValue(y, x);
                if (i.compareTo((DoubleValue) r) != 0) {
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
                new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);
            }
        }, "Table has no body!");
    }

    @Test
    public void test3() {
        File xlsFile = new File("test/rules/calc0/calc0-3.xls");
        TestHelper<ITestCalc> testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc();
        assertEquals(0, result.getHeight());
        assertEquals(0, result.getWidth());
    }

    @Test
    public void test4() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/calc0/calc0-4.xls");
                new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);
            }
        }, "Table has no body!", "merge header cell");
    }

    @Test
    public void test5() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/calc0/calc0-5.xls");
                new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);
            }
        }, "Col1 has already been defined", "cell=E4");
    }

    @Test
    public void test6() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/calc0/calc0-6.xls");
                new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);
            }
        }, "Row1 has already been defined", "cell=B7");
    }
}
