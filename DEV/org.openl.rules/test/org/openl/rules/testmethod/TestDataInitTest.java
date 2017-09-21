package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestHelper;

/*
 * @author PTarasevich
 */

public class TestDataInitTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataArrayInitTest.xlsx";

    public interface ITestDataInit {
        TestUnitsResults returnAddressArryTest();
    }

    @Before
    public void before() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
    }

    @Test
    public void returnAddressArryTest() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDataInit> testHelper = new TestHelper<ITestDataInit>(xlsFile, ITestDataInit.class);

        ITestDataInit instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnAddressArryTest();
        assertEquals(3, result.getNumberOfFailures());
    }
}
