package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.testmethod.TestUnitsResults;

public class ArrayIndexOnSpreadsheetResultTest {

    private static final String FILE_NAME = "test/rules/binding/ArrayIndexOnSpreadsheetResult.xlsx";

    @Test
    public void testUserExceptionSupport1() {
        ITest instance = TestUtils.create(FILE_NAME, ITest.class);
        TestUnitsResults result = instance.findEmployeeClassPremiumTest();
        assertEquals(0, result.getNumberOfFailures(), "Number of failures");
    }

    public interface ITest {
        TestUnitsResults findEmployeeClassPremiumTest();
    }
}
