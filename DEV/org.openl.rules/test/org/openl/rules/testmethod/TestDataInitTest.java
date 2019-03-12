package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestUtils;

/*
 * @author PTarasevich
 */

public class TestDataInitTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataArrayInitTest.xlsx";

    @Before
    public void before() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
    }

    @Test
    public void returnAddressArryTest() {
        ITestDataInit instance = TestUtils.create(FILE_NAME, ITestDataInit.class);
        TestUnitsResults result = instance.returnAddressArryTest();
        assertEquals(3, result.getNumberOfFailures());
    }

    public interface ITestDataInit {
        TestUnitsResults returnAddressArryTest();
    }
}
