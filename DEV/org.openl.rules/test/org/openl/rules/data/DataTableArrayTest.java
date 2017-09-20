package org.openl.rules.data;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestHelper;
import org.openl.rules.testmethod.TestUnitsResults;

/*
 * @author PTarasevich
 */

public class DataTableArrayTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataAccessFieldTest.xlsx";

    public interface ITestDataTableArray {
        TestUnitsResults returnVehicleArryTest();
        TestUnitsResults returnAddressArryTest();
    }

    @Before
    public void before() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
    }

    @Test
    public void vehicleArryTest() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDataTableArray> testHelper = new TestHelper<ITestDataTableArray>(xlsFile, ITestDataTableArray.class);

        ITestDataTableArray instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnVehicleArryTest();
        assertEquals(2, result.getNumberOfFailures());
    }

    @Test
    public void addressArryTest() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDataTableArray> testHelper = new TestHelper<ITestDataTableArray>(xlsFile, ITestDataTableArray.class);

        ITestDataTableArray instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnAddressArryTest();
        assertEquals(3, result.getNumberOfFailures());
    }
}
