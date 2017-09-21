package org.openl.rules.calc;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.testmethod.TestUnitsResults;

public class TestingSpreadsheetTest extends BaseOpenlBuilderHelper {
    private static String SRC = "test/rules/calc1/TestingSpreadsheet.xlsx";

    public TestingSpreadsheetTest() {
    }

    @Before
    public void before() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
    }

    @After
    public void after() {
        // set to default 'false' to avoid impact on other tests
        //
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "false");
    }

    @Test
    public void testingSpreadsheet() throws ClassNotFoundException {
        build(SRC);
        assertNotNull(getCompiledOpenClass().getOpenClass());
        TestUnitsResults res = (TestUnitsResults)invokeMethod("TestSpr");
        assertEquals(1, res.getNumberOfTestUnits());
        assertEquals(0, res.getNumberOfFailures());
    }

    @Test
    public void testingChainCall() throws ClassNotFoundException {
        build(SRC);
        assertNotNull(getCompiledOpenClass().getOpenClass());
        TestUnitsResults res = (TestUnitsResults)invokeMethod("testing1");
        assertEquals(1, res.getNumberOfTestUnits());
        assertEquals(0, res.getNumberOfFailures());
    }

    @Test
    public void testingSpreadsheetResultCall() throws ClassNotFoundException {
        build(SRC);
        assertNotNull(getCompiledOpenClass().getOpenClass());
        TestUnitsResults res = (TestUnitsResults)invokeMethod("testing2");
        assertEquals(1, res.getNumberOfTestUnits());
        assertEquals(0, res.getNumberOfFailures());
    }

    @Test
    public void testOldCellAccess() throws ClassNotFoundException {
        build(SRC);
        assertNotNull(getCompiledOpenClass().getOpenClass());
        TestUnitsResults res = (TestUnitsResults)invokeMethod("method4TestTest");
        assertEquals(1, res.getNumberOfTestUnits());
        assertEquals(0, res.getNumberOfFailures());
    }
}
