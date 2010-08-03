package org.openl.rules.testmethod;

import java.io.File;

import org.junit.Test;
import static  org.junit.Assert.*;
import org.openl.rules.TestHelper;

public class TestmethodTest {

    private static final String FILE_NAME = "test/rules/testmethod/UserExceptionTest.xlsx";

    public interface ITest {
        TestResult driverRiskTestTestAll();
    }
    
    @Test
    public void testUserExceptionSupport() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);
        
        ITest instance = testHelper.getInstance();
        TestResult result = instance.driverRiskTestTestAll();
        assertEquals(0, result.getNumberOfFailures());
    }
}
