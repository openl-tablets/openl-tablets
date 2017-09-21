package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestHelper;

/*
 * @author PTarasevich
 */


public class TestDataAccessFieldTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataAccessFieldTest.xlsx";

    public interface ITestDataAccessField {
        TestUnitsResults returnPolicyTest();
        TestUnitsResults returnPolicyQuoteDateTest();
        TestUnitsResults returnBrokerDiscountTest();
    }

    @Before
    public void before() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
    }

    @Test
    public void returnPolicyTestPK() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDataAccessField> testHelper = new TestHelper<ITestDataAccessField>(xlsFile, ITestDataAccessField.class);

        ITestDataAccessField instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnPolicyTest();
        assertEquals(2, result.getNumberOfFailures());
    }

    @Test
    public void returnPolicyTestQuoteDate() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDataAccessField> testHelper = new TestHelper<ITestDataAccessField>(xlsFile, ITestDataAccessField.class);

        ITestDataAccessField instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnPolicyQuoteDateTest();
        assertEquals(0, result.getNumberOfFailures());
    }

    @Test
    public void returnBrokerDiscountTest() {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITestDataAccessField> testHelper = new TestHelper<ITestDataAccessField>(xlsFile, ITestDataAccessField.class);

        ITestDataAccessField instance = testHelper.getInstance();
        TestUnitsResults result = instance.returnBrokerDiscountTest();
        assertEquals(2, result.getNumberOfFailures());
    }

}
