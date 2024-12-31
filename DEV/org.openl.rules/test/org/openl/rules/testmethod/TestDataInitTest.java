package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

/*
 * @author PTarasevich
 */

public class TestDataInitTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataArrayInitTest.xlsx";

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
