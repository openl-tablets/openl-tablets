package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

/*
 * @author PTarasevich
 */


public class TestDataAccessFieldTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataAccessFieldTest.xlsx";

    public interface ITest {
        TestUnitsResults returnPolicyTestTestAll();
        TestUnitsResults returnPolicyQuoteDateTestTestAll();
        TestUnitsResults returnBrokerDiscountTestTestAll();
    }

    @Test
    public void returnPolicyTestPK() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);

        ITest instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnPolicyTestTestAll();
        assertEquals(2, result.getNumberOfFailures());
    }

    @Test
    public void returnPolicyTestQuoteDate() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);

        ITest instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnPolicyQuoteDateTestTestAll();
        assertEquals(0, result.getNumberOfFailures());
    }

    @Test
    public void returnBrokerDiscountTest() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);

        ITest instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnBrokerDiscountTestTestAll();
        assertEquals(2, result.getNumberOfFailures());
    }
}
