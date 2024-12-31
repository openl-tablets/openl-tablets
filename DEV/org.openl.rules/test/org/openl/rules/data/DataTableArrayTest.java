package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.testmethod.TestUnitsResults;

/*
 * @author PTarasevich
 */

public class DataTableArrayTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataAccessFieldTest.xlsx";

    @Test
    public void vehicleArryTest() {
        ITestDataTableArray instance = TestUtils.create(FILE_NAME, ITestDataTableArray.class);
        TestUnitsResults result = instance.returnVehicleArryTest();
        assertEquals(2, result.getNumberOfFailures());
    }

    @Test
    public void addressArryTest() {
        ITestDataTableArray instance = TestUtils.create(FILE_NAME, ITestDataTableArray.class);
        TestUnitsResults result = instance.returnAddressArryTest();
        assertEquals(3, result.getNumberOfFailures());
    }

    public interface ITestDataTableArray {
        TestUnitsResults returnVehicleArryTest();

        TestUnitsResults returnAddressArryTest();
    }
}
