package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class TestmethodTest {

    private static final String FILE_NAME = "test/rules/testmethod/UserExceptionTest.xlsx";

    @Test
    public void testUserExceptionSupport1() {
        ITest instance = TestUtils.create(FILE_NAME, ITest.class);
        TestUnitsResults result = instance.driverRiskTest1();
        assertEquals(0, result.getNumberOfFailures());
    }

    @Test
    public void testUserExceptionSupport2() {
        ITest instance = TestUtils.create(FILE_NAME, ITest.class);
        TestUnitsResults result = instance.driverRiskTest2();
        assertEquals(0, result.getNumberOfFailures());
    }

    public interface ITest {
        TestUnitsResults driverRiskTest1();

        TestUnitsResults driverRiskTest2();
    }

}
