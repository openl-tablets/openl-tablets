package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.rules.testmethod.TestUnitsResults;

public class ArrayIndexOnSpreadsheetResultTest {

    private static final String FILE_NAME = "test/rules/binding/ArrayIndexOnSpreadsheetResult.xlsx";

    @Test
    public void testUserExceptionSupport1() {
        ITest instance = TestUtils.create(FILE_NAME, ITest.class);
        TestUnitsResults result = instance.findEmployeeClassPremiumTest();
        assertEquals("Number of failures", 0, result.getNumberOfFailures());
    }

    public interface ITest {
        TestUnitsResults findEmployeeClassPremiumTest();
    }
}
