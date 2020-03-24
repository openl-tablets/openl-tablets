package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestUtils;

/*
 * @author PTarasevich
 */

public class TestDataAccessFieldTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataAccessFieldTest.xlsx";

    private static String csr;

    @BeforeClass
    public static void before() {
        csr = System.getProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "");
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
    }

    @AfterClass
    public static void after() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, csr);
    }

    @Test
    public void returnPolicyTestPK() {
        ITestDataAccessField instance = TestUtils.create(FILE_NAME, ITestDataAccessField.class);
        TestUnitsResults result = instance.returnPolicyTest();
        assertEquals(2, result.getNumberOfFailures());
    }

    @Test
    public void returnPolicyTestQuoteDate() {
        ITestDataAccessField instance = TestUtils.create(FILE_NAME, ITestDataAccessField.class);
        TestUnitsResults result = instance.returnPolicyQuoteDateTest();
        assertEquals(0, result.getNumberOfFailures());
    }

    @Test
    public void returnBrokerDiscountTest() {
        ITestDataAccessField instance = TestUtils.create(FILE_NAME, ITestDataAccessField.class);
        TestUnitsResults result = instance.returnBrokerDiscountTest();
        assertEquals(2, result.getNumberOfFailures());
    }

    public interface ITestDataAccessField {
        TestUnitsResults returnPolicyTest();

        TestUnitsResults returnPolicyQuoteDateTest();

        TestUnitsResults returnBrokerDiscountTest();
    }

}
