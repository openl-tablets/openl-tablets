package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;
import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.openl.rules.TestHelper;

public class Test1 {
    public interface ITestCalc {
        SpreadsheetResult calc1(int a, int b);
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/calc1/calc1-1.xls");
        TestHelper<ITestCalc> testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc1(10, 20);

        Object o1 = result.getValue(0, 0);
        Object o2 = result.getValue(0, 1);

        Number v1 = (Number) o1;
        assertEquals(10.0, v1.doubleValue(), 1e-8);

        Number v2 = (Number) o2;
        assertEquals(20.0, v2.doubleValue(), 1e-8);
    }

    @Test
    public void test2() {
        File xlsFile = new File("test/rules/calc1/calc1-2.xls");
        TestHelper<ITestCalc> testHelper;
        testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc1(10, 20);

        Object o1 = result.getValue(0, 2);
        Number v1 = (Number) o1;
        assertEquals(30.0, v1.doubleValue(), 1e-8);
    }

    @Test
    public void test3() {
        File xlsFile = new File("test/rules/calc1/calc1-3.xls");
        TestHelper<ITestCalc> testHelper;
        testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc1(10, 20);

        assertEquals(10, result.getValue(0, 0));
        assertEquals(20, result.getValue(0, 1));
        assertEquals(30, result.getValue(0, 2));
    }

    @Test
    public void test4() {
        File xlsFile = new File("test/rules/calc1/calc1-4.xls");
        TestHelper<ITestCalc> testHelper;
        testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc1(10, 20);

        assertEquals(10, result.getValue(0, 0));
        assertEquals(20L, result.getValue(0, 1));
        // FIX ME, returns 'long' when 'double' expected
        // assertEquals(30.0, result.getValue(0, 2));

        assertEquals(11, result.getValue(1, 0));
        assertEquals(22L, result.getValue(1, 1));
        assertEquals(90.0, result.getValue(1, 2));
    }

    @Ignore
    public void test4a() {
        File xlsFile = new File("test/rules/calc1/calc1-4.xls");
        TestHelper<ITestCalc> testHelper;
        testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc1(10, 20);

        assertEquals(30.0, result.getValue(0, 2));
    }
}
