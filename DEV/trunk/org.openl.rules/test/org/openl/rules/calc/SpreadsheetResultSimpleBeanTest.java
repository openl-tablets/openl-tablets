package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;

public class SpreadsheetResultSimpleBeanTest {
    private static final String SCR = "test/rules/calc1/SpreadsheetResult_SimpleBean_Test.xls";

    public interface ITestCalc {
        DoubleValue calc();
        SpreadsheetResult calc1(int a, int b);
    }

    @Test
    public void test1() {
        File xlsFile = new File(SCR);
        
        TestHelper<ITestCalc> testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        DoubleValue result = test.calc();
        assertEquals(375.0, result.getValue(), 1e-8);

        SpreadsheetResult res = test.calc1(20, 30);

        Object o1 = res.getValue(0, 0);
        Object o2 = res.getValue(0, 1);

        DoubleValue v1 = (DoubleValue) o1;
        assertEquals(20.0, v1.doubleValue(), 1e-8);

        DoubleValue v2 = (DoubleValue) o2;
        assertEquals(30.0, v2.doubleValue(), 1e-8);
    }

}
