package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.testmethod.TestUnitsResults;

public class ArrayIndexOnSpreadsheetResultTest {

    private static final String FILE_NAME = "test/rules/binding/ArrayIndexOnSpreadsheetResult.xlsx";

    public interface ITest {
        TestUnitsResults findEmployeeClassPremiumTest();
    }
    
    @Test
    public void testUserExceptionSupport1() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);
        
        ITest instance = testHelper.getInstance();
        TestUnitsResults result = instance.findEmployeeClassPremiumTest();
        assertEquals("Number of failures", 0, result.getNumberOfFailures());
    }
}
