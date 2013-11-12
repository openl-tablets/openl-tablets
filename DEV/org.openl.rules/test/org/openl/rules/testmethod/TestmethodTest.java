package org.openl.rules.testmethod;

import java.io.File;

import org.junit.Test;
import static  org.junit.Assert.*;
import org.openl.rules.TestHelper;

public class TestmethodTest {

    private static final String FILE_NAME = "test/rules/testmethod/UserExceptionTest.xlsx";

    public interface ITest {
        TestUnitsResults driverRiskTest1TestAll();
        TestUnitsResults driverRiskTest2TestAll();
    }
    
    @Test
    public void testUserExceptionSupport1() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);
        
        ITest instance = testHelper.getInstance();
        TestUnitsResults result = instance.driverRiskTest1TestAll();
        assertEquals(0, result.getNumberOfFailures());
    }
    
    @Test
    public void testUserExceptionSupport2() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);
        
        ITest instance = testHelper.getInstance();
        TestUnitsResults result = instance.driverRiskTest2TestAll();
        assertEquals(0, result.getNumberOfFailures());
    }

}
