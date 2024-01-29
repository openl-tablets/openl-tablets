package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestUtils;

/*
 * @author PTarasevich
 */

public class TestDataInitTest {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataArrayInitTest.xlsx";

    private static String csr;

    @BeforeAll
    public static void before() {
        csr = System.getProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "");
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
    }

    @AfterAll
    public static void after() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, csr);
    }

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
