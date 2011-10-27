package org.openl.rules.calc;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.testmethod.TestUnitsResults;

public class TestingSpreadsheetTest extends BaseOpenlBuilderHelper {
    private static String __src = "test/rules/calc1/TestingSpreadsheet.xlsx";

    public TestingSpreadsheetTest() {
        super(__src);
    }
    
    @Test
    public void test() throws ClassNotFoundException {
        assertNotNull(getJavaWrapper().getOpenClass());
        TestUnitsResults res = (TestUnitsResults)invokeMethod("TestSprTestAll");
        assertEquals(2, res.getNumberOfTestUnits());
        assertEquals(0, res.getNumberOfFailures());
    }
}
