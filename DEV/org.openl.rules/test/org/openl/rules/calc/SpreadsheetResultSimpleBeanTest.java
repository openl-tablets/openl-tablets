package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;

public class SpreadsheetResultSimpleBeanTest {
    private static final String SCR = "test/rules/calc1/SpreadsheetResult_SimpleBean_Test.xls";

    public interface ITestCalc {
        DoubleValue calc();
    }

    @Test
    public void test1() {
        ITestCalc test = TestUtils.create(SCR, ITestCalc.class);
        DoubleValue result = test.calc();
        assertEquals(375.0, result.getValue(), 1e-8);
    }

}
