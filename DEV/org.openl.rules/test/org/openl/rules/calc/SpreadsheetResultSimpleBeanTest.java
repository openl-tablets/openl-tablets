package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class SpreadsheetResultSimpleBeanTest {
    private static final String SCR = "test/rules/calc1/SpreadsheetResult_SimpleBean_Test.xls";

    @Test
    public void test1() {
        ITestCalc test = TestUtils.create(SCR, ITestCalc.class);
        Double result = test.calc();
        assertEquals(375.0, result, 1e-8);
    }

    public interface ITestCalc {
        Double calc();
    }

}
